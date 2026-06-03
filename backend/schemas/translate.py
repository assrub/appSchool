from pydantic import BaseModel, Field


class TranslateRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=500)
    sourceLang: str = "en"
    targetLang: str = "es"


class TranslateResponse(BaseModel):
    text: str
    translation: str
    sourceLang: str
    targetLang: str
    confidence: float
