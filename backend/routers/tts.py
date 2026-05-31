from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import Response
from sqlalchemy.ext.asyncio import AsyncSession

from config import TTS_VOICES
from database import get_db
from schemas.tts import TtsRequest, TtsVoicesResponse, TtsVoice
from services.edge_tts_service import generate_tts

router = APIRouter()


@router.get("/voices", response_model=TtsVoicesResponse)
async def get_voices():
    voices = [TtsVoice(**v) for v in TTS_VOICES]
    return TtsVoicesResponse(voices=voices)


@router.post("")
async def text_to_speech(
    request: TtsRequest,
    db: AsyncSession = Depends(get_db),
):
    try:
        audio_data = await generate_tts(
            text=request.text,
            voice=request.voice,
            db=db,
        )
        return Response(
            content=audio_data,
            media_type="audio/mpeg",
            headers={
                "Content-Length": str(len(audio_data)),
            },
        )
    except Exception as e:
        raise HTTPException(
            status_code=503,
            detail=f"TTS service unavailable: {str(e)}",
        )
