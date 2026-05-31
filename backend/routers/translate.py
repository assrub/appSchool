from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from database import get_db
from schemas.translate import TranslateRequest, TranslateResponse
from services.translator_service import translate_text

router = APIRouter()


@router.post("", response_model=TranslateResponse)
async def translate(
    request: TranslateRequest,
    db: AsyncSession = Depends(get_db),
):
    result = await translate_text(
        text=request.text,
        source_lang=request.sourceLang,
        target_lang=request.targetLang,
        db=db,
    )
    return TranslateResponse(**result)
