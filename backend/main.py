from contextlib import asynccontextmanager
import os, sys, logging
from logging.handlers import RotatingFileHandler

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded

from database import init_db
from routers import content, tts, translate, progress, dictionary
from routers import auth, admin, ws
from config import CORS_ORIGINS

UPLOAD_DIR = os.path.join(os.path.dirname(__file__), "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)

LOG_DIR = os.path.join(os.path.dirname(__file__), "logs")
os.makedirs(LOG_DIR, exist_ok=True)

# Configure file logging
log_handler = RotatingFileHandler(
    os.path.join(LOG_DIR, "api.log"),
    maxBytes=5 * 1024 * 1024,
    backupCount=3,
)
log_handler.setFormatter(logging.Formatter("%(asctime)s %(levelname)s %(message)s"))
root_logger = logging.getLogger()
root_logger.setLevel(logging.INFO)
root_logger.addHandler(log_handler)

# Read version from version.properties in same directory (backend/)
_VERSION_CODE = 1
_VERSION_NAME = "1.0.0"
try:
    version_path = os.path.join(os.path.dirname(__file__), "version.properties")
    if os.path.exists(version_path):
        with open(version_path) as f:
            for line in f:
                line = line.strip()
                if line.startswith("versionCode="):
                    _VERSION_CODE = int(line.split("=")[1])
                elif line.startswith("versionName="):
                    _VERSION_NAME = line.split("=")[1]
except (ValueError, IOError) as e:
    logging.warning(f"Failed to read version.properties: {e}")

# Rate limiter
limiter = Limiter(key_func=get_remote_address)

@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    yield


app = FastAPI(title="AppEnglish API", version=_VERSION_NAME, lifespan=lifespan)
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

app.add_middleware(
    CORSMiddleware,
    allow_origins=CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(content.router, prefix="/api/v1/content", tags=["Content"])
app.include_router(tts.router, prefix="/api/v1/tts", tags=["TTS"])
app.include_router(translate.router, prefix="/api/v1/translate", tags=["Translate"])
app.include_router(progress.router, prefix="/api/v1/progress", tags=["Progress"])
app.include_router(dictionary.router, prefix="/api/v1/dictionary", tags=["Dictionary"])
app.include_router(auth.router, prefix="/api/v1/auth", tags=["Auth"])
app.include_router(admin.router, prefix="/api/v1/admin", tags=["Admin"])
app.include_router(ws.router, tags=["WebSocket"])
app.mount("/uploads", StaticFiles(directory=UPLOAD_DIR), name="uploads")


@app.get("/")
async def root():
    return {"name": "AppEnglish API", "version": _VERSION_NAME, "status": "ok"}


@app.get("/health")
async def health():
    return {"status": "healthy"}


@app.get("/api/v1/version")
async def version():
    return {
        "versionCode": _VERSION_CODE,
        "versionName": _VERSION_NAME,
        "apkUrl": "/uploads/app-release.apk",
    }
