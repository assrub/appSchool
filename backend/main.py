from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from database import init_db
from routers import content, tts, translate, progress, dictionary


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


@app.get("/")
async def root():
    return {"name": "AppEnglish API", "version": "1.0.0", "status": "ok"}


@app.get("/health")
async def health():
    return {"status": "healthy"}
