import hashlib
import io
import logging
import traceback

import edge_tts
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from models import TtsCache

logger = logging.getLogger("appschool.tts")


async def generate_tts(
    text: str,
    voice: str,
    db: AsyncSession | None = None,
) -> bytes:
    text_hash = hashlib.sha256(f"{text}|{voice}".encode()).hexdigest()

    if db:
        result = await db.execute(
            select(TtsCache).where(TtsCache.text_hash == text_hash)
        )
        cached = result.scalar_one_or_none()
        if cached:
            return cached.audio_data

    try:
        communicate = edge_tts.Communicate(text, voice)
        audio_buffer = io.BytesIO()
        async for chunk in communicate.stream():
            if chunk.get("type") == "audio":
                audio_buffer.write(chunk.get("data", b""))

        audio_data = audio_buffer.getvalue()
    except Exception as e:
        # Log full exception with stacktrace to help diagnosis
        logger.exception("TTS generation failed for voice=%s", voice)
        # Also print traceback to stdout/stderr so Docker logs capture it
        print("TTS generation exception:\n", traceback.format_exc())

        # Fallback: try gTTS (offline-ish via Google Translate TTS) to return audio
        try:
            from gtts import gTTS

            logger.info("Attempting gTTS fallback for text length=%d", len(text))
            fp = io.BytesIO()
            tts = gTTS(text=text, lang="en")
            tts.write_to_fp(fp)
            audio_data = fp.getvalue()
            logger.info("gTTS fallback succeeded, returning mp3 data size=%d", len(audio_data))
            # try caching the fallback result if DB available
            if db:
                try:
                    cache_entry = TtsCache(
                        text_hash=text_hash,
                        text=text,
                        voice=voice,
                        audio_data=audio_data,
                    )
                    db.add(cache_entry)
                    await db.commit()
                except Exception:
                    logger.exception("Failed to cache gTTS fallback result for hash=%s", text_hash)

            return audio_data
        except Exception:
            logger.exception("gTTS fallback also failed")
            # If fallback fails, re-raise original exception to signal 503
            raise

    if db:
        try:
            cache_entry = TtsCache(
                text_hash=text_hash,
                text=text,
                voice=voice,
                audio_data=audio_data,
            )
            db.add(cache_entry)
            await db.commit()
        except Exception:
            logger.exception("Failed to cache TTS result for hash=%s", text_hash)

    return audio_data
