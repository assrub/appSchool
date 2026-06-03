import os
import secrets
from dotenv import load_dotenv

load_dotenv()

DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "sqlite+aiosqlite:///./appenglish.db",
)

SECRET_KEY = os.getenv("SECRET_KEY") or secrets.token_hex(32)

TTS_DEFAULT_VOICE = os.getenv("TTS_DEFAULT_VOICE", "en-US-ChristopherNeural")
TTS_DEFAULT_RATE = os.getenv("TTS_DEFAULT_RATE", "+0%")
TTS_DEFAULT_PITCH = os.getenv("TTS_DEFAULT_PITCH", "+0Hz")

MYMEMORY_EMAIL = os.getenv("MYMEMORY_EMAIL", None)

# CORS allowed origins (comma-separated)
CORS_ORIGINS = os.getenv("CORS_ORIGINS", "http://localhost:9000,http://2.25.142.139:9000").split(",")

CONTENT_DIR = os.path.join(os.path.dirname(__file__), "content")

TTS_VOICES = [
    {"id": "en-US-ChristopherNeural", "gender": "Male", "locale": "en-US"},
    {"id": "en-US-JennyNeural", "gender": "Female", "locale": "en-US"},
    {"id": "en-US-GuyNeural", "gender": "Male", "locale": "en-US"},
    {"id": "en-US-AriaNeural", "gender": "Female", "locale": "en-US"},
    {"id": "en-GB-RyanNeural", "gender": "Male", "locale": "en-GB"},
    {"id": "en-GB-SoniaNeural", "gender": "Female", "locale": "en-GB"},
]
