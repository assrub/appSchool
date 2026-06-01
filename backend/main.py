from contextlib import asynccontextmanager
import os

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from database import init_db
from routers import content, tts, translate, progress, dictionary
from routers import auth, admin

UPLOAD_DIR = os.path.join(os.path.dirname(__file__), "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)


@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    yield


app = FastAPI(title="AppEnglish API", version="1.0.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
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
app.mount("/uploads", StaticFiles(directory=UPLOAD_DIR), name="uploads")


@app.get("/")
async def root():
    return {"name": "AppEnglish API", "version": "1.0.0", "status": "ok"}


@app.get("/health")
async def health():
    return {"status": "healthy"}


@app.get("/api/v1/version")
async def version():
    return {
        "versionCode": 21,
        "versionName": "2.4.0",
        "apkUrl": "/uploads/app-release.apk",
    }
