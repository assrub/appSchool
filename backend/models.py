import datetime
from sqlalchemy import String, Integer, Boolean, Float, Text, DateTime, LargeBinary, JSON, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from database import Base


class Subject(Base):
    __tablename__ = "subjects"

    id: Mapped[str] = mapped_column(String(50), primary_key=True)
    name: Mapped[str] = mapped_column(String(100), nullable=False)
    icon: Mapped[str] = mapped_column(String(50), default="")
    color: Mapped[str] = mapped_column(String(7), default="#4CAF50")
    map_config: Mapped[dict | None] = mapped_column(JSON, nullable=True)
    created_at: Mapped[datetime.datetime] = mapped_column(
        DateTime, default=datetime.datetime.utcnow
    )


class Topic(Base):
    __tablename__ = "topics"

    id: Mapped[str] = mapped_column(String(100), primary_key=True)
    subject_id: Mapped[str] = mapped_column(String(50), nullable=False, index=True)
    name: Mapped[str] = mapped_column(String(200), nullable=False)
    sort_order: Mapped[int] = mapped_column(Integer, default=0)
    difficulty: Mapped[int] = mapped_column(Integer, default=1)
    icon: Mapped[str] = mapped_column(String(50), default="")
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    content: Mapped[dict] = mapped_column(JSON, nullable=False)
    created_at: Mapped[datetime.datetime] = mapped_column(
        DateTime, default=datetime.datetime.utcnow
    )
    updated_at: Mapped[datetime.datetime] = mapped_column(
        DateTime, default=datetime.datetime.utcnow, onupdate=datetime.datetime.utcnow
    )


class Progress(Base):
    __tablename__ = "progress"
    __table_args__ = (
        UniqueConstraint("device_id", "topic_id", "unit_id", name="uq_progress_device_topic_unit"),
    )

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    device_id: Mapped[str] = mapped_column(String(100), nullable=False, index=True)
    topic_id: Mapped[str] = mapped_column(String(100), nullable=False)
    unit_id: Mapped[str] = mapped_column(String(100), nullable=False)
    completed: Mapped[bool] = mapped_column(Boolean, default=False)
    score: Mapped[int] = mapped_column(Integer, default=0)
    total_items: Mapped[int] = mapped_column(Integer, default=0)
    completed_items: Mapped[int] = mapped_column(Integer, default=0)
    test_score: Mapped[int | None] = mapped_column(Integer, nullable=True)
    started_at: Mapped[datetime.datetime] = mapped_column(
        DateTime, default=datetime.datetime.utcnow
    )
    completed_at: Mapped[datetime.datetime | None] = mapped_column(DateTime, nullable=True)


class DictionaryEntry(Base):
    __tablename__ = "dictionary_entries"
    __table_args__ = (
        UniqueConstraint("device_id", "word", name="uq_dict_device_word"),
    )

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    device_id: Mapped[str] = mapped_column(String(100), nullable=False, index=True)
    word: Mapped[str] = mapped_column(String(255), nullable=False)
    translation: Mapped[str] = mapped_column(String(500), nullable=False)
    source_lang: Mapped[str] = mapped_column(String(10), default="en")
    target_lang: Mapped[str] = mapped_column(String(10), default="es")
    times_looked_up: Mapped[int] = mapped_column(Integer, default=1)
    created_at: Mapped[datetime.datetime] = mapped_column(
        DateTime, default=datetime.datetime.utcnow
    )
    updated_at: Mapped[datetime.datetime] = mapped_column(
        DateTime, default=datetime.datetime.utcnow, onupdate=datetime.datetime.utcnow
    )


class TranslationCache(Base):
    __tablename__ = "translation_cache"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    text_hash: Mapped[str] = mapped_column(String(64), unique=True, nullable=False)
    source_text: Mapped[str] = mapped_column(Text, nullable=False)
    source_lang: Mapped[str] = mapped_column(String(10), nullable=False)
    target_lang: Mapped[str] = mapped_column(String(10), nullable=False)
    translation: Mapped[str] = mapped_column(Text, nullable=False)
    created_at: Mapped[datetime.datetime] = mapped_column(
        DateTime, default=datetime.datetime.utcnow
    )


class TtsCache(Base):
    __tablename__ = "tts_cache"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    text_hash: Mapped[str] = mapped_column(String(64), unique=True, nullable=False)
    text: Mapped[str] = mapped_column(Text, nullable=False)
    voice: Mapped[str] = mapped_column(String(100), nullable=False)
    audio_data: Mapped[bytes] = mapped_column(LargeBinary, nullable=False)
    created_at: Mapped[datetime.datetime] = mapped_column(
        DateTime, default=datetime.datetime.utcnow
    )
