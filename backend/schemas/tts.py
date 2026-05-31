from pydantic import BaseModel


class TtsRequest(BaseModel):
    text: str
    voice: str = "en-US-ChristopherNeural"
    rate: str = "+0%"
    pitch: str = "+0Hz"


class TtsVoice(BaseModel):
    id: str
    gender: str
    locale: str


class TtsVoicesResponse(BaseModel):
    voices: list[TtsVoice]
