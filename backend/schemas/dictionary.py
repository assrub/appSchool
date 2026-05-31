from pydantic import BaseModel
from datetime import datetime


class DictionaryEntryRequest(BaseModel):
    deviceId: str
    word: str
    translation: str
    sourceLang: str = "en"
    targetLang: str = "es"


class DictionaryEntryResponse(BaseModel):
    id: int
    word: str
    translation: str
    timesLookedUp: int
    createdAt: datetime


class DictionaryListResponse(BaseModel):
    entries: list[DictionaryEntryResponse]
    total: int
    limit: int
    offset: int
