from pydantic import BaseModel, Field


class TtsRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=5000)
    voice: str = "en-US-ChristopherNeural"
    rate: str = "+0%"
    pitch: str = "+0Hz"


class TtsVoice(BaseModel):
    id: str
    gender: str
    locale: str


class TtsVoicesResponse(BaseModel):
    voices: list[TtsVoice]
