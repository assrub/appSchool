from fastapi import APIRouter, Depends, Request
from sqlalchemy.ext.asyncio import AsyncSession
from slowapi import Limiter
from slowapi.util import get_remote_address

from database import get_db
from schemas.translate import TranslateRequest, TranslateResponse
from services.translator_service import translate_text

router = APIRouter()
limiter = Limiter(key_func=get_remote_address)


@router.post("", response_model=TranslateResponse)
@limiter.limit("60/minute")
async def translate(
    request: Request,
    body: TranslateRequest,
    db: AsyncSession = Depends(get_db),
):
    result = await translate_text(
        text=body.text,
        source_lang=body.sourceLang,
        target_lang=body.targetLang,
        db=db,
    )
    return TranslateResponse(**result)
