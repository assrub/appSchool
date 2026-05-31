from pydantic import BaseModel


class TranslateRequest(BaseModel):
    text: str
    sourceLang: str = "en"
    targetLang: str = "es"


class TranslateResponse(BaseModel):
    text: str
    translation: str
    sourceLang: str
    targetLang: str
    confidence: float
