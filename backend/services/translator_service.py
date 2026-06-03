import hashlib

import httpx
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from models import TranslationCache

MYMEMORY_URL = "https://api.mymemory.translated.net/get"


async def translate_text(
    text: str,
    source_lang: str = "en",
    target_lang: str = "es",
    db: AsyncSession | None = None,
) -> dict:
    text_hash = hashlib.sha256(
        f"{text}|{source_lang}|{target_lang}".encode()
    ).hexdigest()

    if db:
        try:
            result = await db.execute(
                select(TranslationCache).where(
                    TranslationCache.text_hash == text_hash
                )
            )
            cached = result.scalar_one_or_none()
            if cached:
                return {
                    "text": text,
                    "translation": cached.translation,
                    "sourceLang": source_lang,
                    "targetLang": target_lang,
                    "confidence": 1.0,
                }
        except Exception:
            pass  # Cache miss is not fatal

    try:
        async with httpx.AsyncClient() as client:
            params = {
                "q": text,
                "langpair": f"{source_lang}|{target_lang}",
            }
            response = await client.get(MYMEMORY_URL, params=params, timeout=10.0)
            data = response.json()

        if "responseData" not in data:
            return {
                "text": text,
                "translation": text,
                "sourceLang": source_lang,
                "targetLang": target_lang,
                "confidence": 0.0,
            }

        translation = data["responseData"].get("translatedText", text)
        confidence = data["responseData"].get("match", 0) * 100

        if db:
            try:
                cache_entry = TranslationCache(
                    text_hash=text_hash,
                    source_text=text,
                    source_lang=source_lang,
                    target_lang=target_lang,
                    translation=translation,
                )
                db.add(cache_entry)
                await db.commit()
            except Exception:
                pass  # Cache write failure is not fatal

        return {
            "text": text,
            "translation": translation,
            "sourceLang": source_lang,
            "targetLang": target_lang,
            "confidence": confidence,
        }
    except Exception as e:
        # Return original text if translation fails
        return {
            "text": text,
            "translation": text,
            "sourceLang": source_lang,
            "targetLang": target_lang,
            "confidence": 0.0,
        }
