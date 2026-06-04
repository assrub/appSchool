import datetime
from sqlalchemy import String, Integer, Boolean, Float, Text, DateTime, LargeBinary, JSON, UniqueConstraint, ForeignKey, Index
from sqlalchemy.orm import Mapped, mapped_column, relationship

from database import Base


def _utcnow():
    return datetime.datetime.utcnow()


class Subject(Base):
    __tablename__ = "subjects"

    id: Mapped[str] = mapped_column(String(50), primary_key=True)
    name: Mapped[str] = mapped_column(String(100), nullable=False)
    icon: Mapped[str] = mapped_column(String(50), default="")
    color: Mapped[str] = mapped_column(String(7), default="#4CAF50")
    sort_order: Mapped[int] = mapped_column(Integer, default=0)
    map_config: Mapped[dict | None] = mapped_column(JSON, nullable=True)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow)
    updated_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow, onupdate=_utcnow)

    topics: Mapped[list["Topic"]] = relationship(back_populates="subject", order_by="Topic.sort_order")


class Topic(Base):
    __tablename__ = "topics"

    id: Mapped[str] = mapped_column(String(100), primary_key=True)
    subject_id: Mapped[str] = mapped_column(String(50), ForeignKey("subjects.id"), nullable=False, index=True)
    name: Mapped[str] = mapped_column(String(200), nullable=False)
    icon: Mapped[str] = mapped_column(String(50), default="")
    difficulty: Mapped[int] = mapped_column(Integer, default=1)
    sort_order: Mapped[int] = mapped_column(Integer, default=0)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow)
    updated_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow, onupdate=_utcnow)

    subject: Mapped["Subject"] = relationship(back_populates="topics")
    theory: Mapped["TopicTheory | None"] = relationship(back_populates="topic", uselist=False)
    videos: Mapped[list["TheoryVideo"]] = relationship(back_populates="topic", order_by="TheoryVideo.sort_order")
    units: Mapped[list["ExerciseUnit"]] = relationship(back_populates="topic", order_by="ExerciseUnit.sort_order")


class TopicTheory(Base):
    __tablename__ = "topic_theory"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    topic_id: Mapped[str] = mapped_column(String(100), ForeignKey("topics.id"), unique=True, nullable=False)
    text: Mapped[str] = mapped_column(Text, nullable=False)
    table_headers: Mapped[list | None] = mapped_column(JSON, nullable=True)
    table_rows: Mapped[list | None] = mapped_column(JSON, nullable=True)
    tips: Mapped[list | None] = mapped_column(JSON, nullable=True)
    created_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow)
    updated_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow, onupdate=_utcnow)

    topic: Mapped["Topic"] = relationship(back_populates="theory")
    sections: Mapped[list["TheorySection"]] = relationship(back_populates="theory", order_by="TheorySection.sort_order")


class TheorySection(Base):
    __tablename__ = "theory_sections"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    theory_id: Mapped[int] = mapped_column(Integer, ForeignKey("topic_theory.id"), nullable=False)
    title: Mapped[str] = mapped_column(String(200), nullable=False)
    text: Mapped[str] = mapped_column(Text, nullable=False)
    examples: Mapped[list | None] = mapped_column(JSON, nullable=True)
    sort_order: Mapped[int] = mapped_column(Integer, default=0)

    theory: Mapped["TopicTheory"] = relationship(back_populates="sections")


class TheoryVideo(Base):
    __tablename__ = "theory_videos"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    topic_id: Mapped[str] = mapped_column(String(100), ForeignKey("topics.id"), nullable=False)
    title: Mapped[str] = mapped_column(String(200), nullable=False)
    url: Mapped[str] = mapped_column(String(500), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    sort_order: Mapped[int] = mapped_column(Integer, default=0)
    created_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow)

    topic: Mapped["Topic"] = relationship(back_populates="videos")


class ExerciseUnit(Base):
    __tablename__ = "exercise_units"

    id: Mapped[str] = mapped_column(String(100), primary_key=True)
    topic_id: Mapped[str] = mapped_column(String(100), ForeignKey("topics.id"), nullable=False)
    title: Mapped[str] = mapped_column(String(200), nullable=False)
    exercise_type: Mapped[str] = mapped_column(String(50), default="fill-blank")
    explanation: Mapped[str | None] = mapped_column(Text, nullable=True)
    input_mode: Mapped[str] = mapped_column(String(10), default="tap")
    is_locked: Mapped[bool] = mapped_column(Boolean, default=False)
    icon: Mapped[str] = mapped_column(String(50), default="")
    sound_correct_url: Mapped[str | None] = mapped_column(String(500), nullable=True)
    sound_incorrect_url: Mapped[str | None] = mapped_column(String(500), nullable=True)
    sort_order: Mapped[int] = mapped_column(Integer, default=0)
    created_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow)
    updated_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow, onupdate=_utcnow)

    topic: Mapped["Topic"] = relationship(back_populates="units")
    theory: Mapped["UnitTheory | None"] = relationship(back_populates="unit", uselist=False)
    blocks: Mapped[list["ExerciseBlock"]] = relationship(back_populates="unit", order_by="ExerciseBlock.sort_order")


class UnitTheory(Base):
    __tablename__ = "unit_theory"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    unit_id: Mapped[str] = mapped_column(String(100), ForeignKey("exercise_units.id"), unique=True, nullable=False)
    text: Mapped[str] = mapped_column(Text, nullable=False)
    table_headers: Mapped[list | None] = mapped_column(JSON, nullable=True)
    table_rows: Mapped[list | None] = mapped_column(JSON, nullable=True)
    tips: Mapped[list | None] = mapped_column(JSON, nullable=True)
    created_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow)
    updated_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow, onupdate=_utcnow)

    unit: Mapped["ExerciseUnit"] = relationship(back_populates="theory")
    sections: Mapped[list["UnitTheorySection"]] = relationship(back_populates="unit_theory", order_by="UnitTheorySection.sort_order")


class UnitTheorySection(Base):
    __tablename__ = "unit_theory_sections"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    unit_theory_id: Mapped[int] = mapped_column(Integer, ForeignKey("unit_theory.id"), nullable=False)
    title: Mapped[str] = mapped_column(String(200), nullable=False)
    text: Mapped[str] = mapped_column(Text, nullable=False)
    examples: Mapped[list | None] = mapped_column(JSON, nullable=True)
    sort_order: Mapped[int] = mapped_column(Integer, default=0)

    unit_theory: Mapped["UnitTheory"] = relationship(back_populates="sections")


class ExerciseBlock(Base):
    __tablename__ = "exercise_blocks"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    unit_id: Mapped[str] = mapped_column(String(100), ForeignKey("exercise_units.id"), nullable=False)
    title: Mapped[str] = mapped_column(String(300), nullable=False)
    icon: Mapped[str] = mapped_column(String(50), default="")
    shuffle: Mapped[bool] = mapped_column(Boolean, default=False)
    sort_order: Mapped[int] = mapped_column(Integer, default=0)

    unit: Mapped["ExerciseUnit"] = relationship(back_populates="blocks")
    items: Mapped[list["ExerciseItem"]] = relationship(back_populates="block", order_by="ExerciseItem.sort_order")
    theory: Mapped["BlockTheory | None"] = relationship(back_populates="block", uselist=False)


class BlockTheory(Base):
    __tablename__ = "block_theory"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    block_id: Mapped[int] = mapped_column(Integer, ForeignKey("exercise_blocks.id"), unique=True, nullable=False)
    text: Mapped[str] = mapped_column(Text, nullable=False)
    table_headers: Mapped[list | None] = mapped_column(JSON, nullable=True)
    table_rows: Mapped[list | None] = mapped_column(JSON, nullable=True)
    tips: Mapped[list | None] = mapped_column(JSON, nullable=True)
    created_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow)
    updated_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow, onupdate=_utcnow)

    block: Mapped["ExerciseBlock"] = relationship(back_populates="theory")
    sections: Mapped[list["BlockTheorySection"]] = relationship(back_populates="block_theory", order_by="BlockTheorySection.sort_order")


class BlockTheorySection(Base):
    __tablename__ = "block_theory_sections"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    block_theory_id: Mapped[int] = mapped_column(Integer, ForeignKey("block_theory.id"), nullable=False)
    title: Mapped[str] = mapped_column(String(200), nullable=False)
    text: Mapped[str] = mapped_column(Text, nullable=False)
    examples: Mapped[list | None] = mapped_column(JSON, nullable=True)
    sort_order: Mapped[int] = mapped_column(Integer, default=0)

    block_theory: Mapped["BlockTheory"] = relationship(back_populates="sections")


class ExerciseItem(Base):
    __tablename__ = "exercise_items"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    block_id: Mapped[int] = mapped_column(Integer, ForeignKey("exercise_blocks.id"), nullable=False)
    item_type: Mapped[str] = mapped_column(String(30), default="fill-blank")
    sentence: Mapped[str] = mapped_column(String(500), nullable=False)
    answer: Mapped[str] = mapped_column(String(200), nullable=False)
    answers: Mapped[list | None] = mapped_column(JSON, nullable=True)
    hint: Mapped[str | None] = mapped_column(String(300), nullable=True)
    question: Mapped[str | None] = mapped_column(String(500), nullable=True)
    options: Mapped[list | None] = mapped_column(JSON, nullable=True)
    input_mode: Mapped[str | None] = mapped_column(String(10), nullable=True)
    words: Mapped[list | None] = mapped_column(JSON, nullable=True)
    correct_order: Mapped[list | None] = mapped_column(JSON, nullable=True)
    audio_url: Mapped[str | None] = mapped_column(String(500), nullable=True)
    pairs: Mapped[list | None] = mapped_column(JSON, nullable=True)
    is_correct_boolean: Mapped[bool | None] = mapped_column(Boolean, nullable=True)
    sort_order: Mapped[int] = mapped_column(Integer, default=0)

    block: Mapped["ExerciseBlock"] = relationship(back_populates="items")


class AnswerHistory(Base):
    __tablename__ = "answer_history"
    __table_args__ = (
        Index("ix_answer_history_user_topic", "user_id", "topic_id"),
        Index("ix_answer_history_user_topic_unit", "user_id", "topic_id", "unit_id"),
    )

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("app_users.id"), nullable=False, index=True)
    topic_id: Mapped[str] = mapped_column(String(100), nullable=False)
    unit_id: Mapped[str] = mapped_column(String(100), nullable=False)
    exercise_item_id: Mapped[int | None] = mapped_column(Integer, nullable=True)
    given_answer: Mapped[str] = mapped_column(String(200), nullable=False)
    correct_answer: Mapped[str] = mapped_column(String(200), nullable=False)
    is_correct: Mapped[bool] = mapped_column(Boolean, nullable=False)
    attempt_number: Mapped[int] = mapped_column(Integer, default=1)
    answered_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow)


class Progress(Base):
    __tablename__ = "progress"
    __table_args__ = (
        UniqueConstraint("user_id", "topic_id", "unit_id", name="uq_progress_user_topic_unit"),
        Index("ix_progress_user_topic", "user_id", "topic_id"),
    )

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("app_users.id"), nullable=False, index=True)
    topic_id: Mapped[str] = mapped_column(String(100), nullable=False)
    unit_id: Mapped[str] = mapped_column(String(100), nullable=False)
    completed: Mapped[bool] = mapped_column(Boolean, default=False)
    score: Mapped[int] = mapped_column(Integer, default=0)
    total_items: Mapped[int] = mapped_column(Integer, default=0)
    completed_items: Mapped[int] = mapped_column(Integer, default=0)
    test_score: Mapped[int | None] = mapped_column(Integer, nullable=True)
    redo_data: Mapped[dict | None] = mapped_column(JSON, nullable=True)
    started_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow)
    completed_at: Mapped[datetime.datetime | None] = mapped_column(DateTime, nullable=True)
    # New pedagogical metrics
    accuracy: Mapped[float] = mapped_column(Float, default=0.0)  # % correct on first attempt
    mastery: Mapped[float] = mapped_column(Float, default=0.0)  # % mastered (first correct OR retried correct)
    status: Mapped[str] = mapped_column(String(20), default="not_started")  # not_started, in_progress, completed, mastered
    items_attempted: Mapped[int] = mapped_column(Integer, default=0)  # distinct items answered
    items_mastered: Mapped[int] = mapped_column(Integer, default=0)  # items mastered
    items_correct_first: Mapped[int] = mapped_column(Integer, default=0)  # correct on first attempt
    time_spent_seconds: Mapped[int] = mapped_column(Integer, default=0)  # total time in seconds
    last_activity_at: Mapped[datetime.datetime | None] = mapped_column(DateTime, nullable=True)
    last_reset_at: Mapped[datetime.datetime | None] = mapped_column(DateTime, nullable=True)


class BlockProgress(Base):
    __tablename__ = "block_progress"
    __table_args__ = (
        UniqueConstraint("user_id", "topic_id", "unit_id", "block_index", name="uq_block_progress_user_topic_unit_block"),
        Index("ix_block_progress_user_topic_unit", "user_id", "topic_id", "unit_id"),
    )

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("app_users.id"), nullable=False, index=True)
    topic_id: Mapped[str] = mapped_column(String(100), nullable=False)
    unit_id: Mapped[str] = mapped_column(String(100), nullable=False)
    block_index: Mapped[int] = mapped_column(Integer, nullable=False)
    completed: Mapped[bool] = mapped_column(Boolean, default=False)
    score: Mapped[int] = mapped_column(Integer, default=0)
    total_items: Mapped[int] = mapped_column(Integer, default=0)
    completed_items: Mapped[int] = mapped_column(Integer, default=0)
    completed_at: Mapped[datetime.datetime | None] = mapped_column(DateTime, nullable=True)


class ProgressEvent(Base):
    __tablename__ = "progress_events"
    __table_args__ = (
        Index("ix_progress_events_user", "user_id"),
        Index("ix_progress_events_user_topic_unit", "user_id", "topic_id", "unit_id"),
    )

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("app_users.id"), nullable=False, index=True)
    event_type: Mapped[str] = mapped_column(String(30), nullable=False)
    topic_id: Mapped[str] = mapped_column(String(100), nullable=False)
    unit_id: Mapped[str | None] = mapped_column(String(100), nullable=True)
    block_index: Mapped[int | None] = mapped_column(Integer, nullable=True)
    event_data: Mapped[dict | None] = mapped_column(JSON, nullable=True)
    created_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow)


class StudySession(Base):
    __tablename__ = "study_sessions"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("app_users.id"), nullable=False, index=True)
    topic_id: Mapped[str | None] = mapped_column(String(100), nullable=True)
    started_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow)
    ended_at: Mapped[datetime.datetime | None] = mapped_column(DateTime, nullable=True)
    exercises_attempted: Mapped[int] = mapped_column(Integer, default=0)
    exercises_correct: Mapped[int] = mapped_column(Integer, default=0)
    duration_seconds: Mapped[int | None] = mapped_column(Integer, nullable=True)


class DictionaryEntry(Base):
    __tablename__ = "dictionary_entries"
    __table_args__ = (
        UniqueConstraint("user_id", "word", name="uq_dict_user_word"),
    )

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("app_users.id"), nullable=False, index=True)
    word: Mapped[str] = mapped_column(String(255), nullable=False)
    translation: Mapped[str] = mapped_column(String(500), nullable=False)
    source_lang: Mapped[str] = mapped_column(String(10), default="en")
    target_lang: Mapped[str] = mapped_column(String(10), default="es")
    times_looked_up: Mapped[int] = mapped_column(Integer, default=1)
    created_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow)
    updated_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow, onupdate=_utcnow)


class TranslationCache(Base):
    __tablename__ = "translation_cache"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    text_hash: Mapped[str] = mapped_column(String(64), unique=True, nullable=False)
    source_text: Mapped[str] = mapped_column(Text, nullable=False)
    source_lang: Mapped[str] = mapped_column(String(10), nullable=False)
    target_lang: Mapped[str] = mapped_column(String(10), nullable=False)
    translation: Mapped[str] = mapped_column(Text, nullable=False)
    created_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow)


class TtsCache(Base):
    __tablename__ = "tts_cache"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    text_hash: Mapped[str] = mapped_column(String(64), unique=True, nullable=False)
    text: Mapped[str] = mapped_column(Text, nullable=False)
    voice: Mapped[str] = mapped_column(String(100), nullable=False)
    audio_data: Mapped[bytes] = mapped_column(LargeBinary, nullable=False)
    created_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow)


class AudioFile(Base):
    __tablename__ = "audio_files"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    filename: Mapped[str] = mapped_column(String(255), nullable=False)
    original_text: Mapped[str] = mapped_column(String(500), nullable=False)
    voice: Mapped[str | None] = mapped_column(String(100), nullable=True)
    file_path: Mapped[str] = mapped_column(String(500), nullable=False)
    duration_ms: Mapped[int | None] = mapped_column(Integer, nullable=True)
    created_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow)


class AdminUser(Base):
    __tablename__ = "admin_users"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    username: Mapped[str] = mapped_column(String(50), unique=True, nullable=False)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    created_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow)


class AppUser(Base):
    """Child/student user for login on the Android app."""
    __tablename__ = "app_users"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    username: Mapped[str] = mapped_column(String(50), unique=True, nullable=False)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    display_name: Mapped[str] = mapped_column(String(100), nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime.datetime] = mapped_column(DateTime, default=_utcnow)


class UserSubject(Base):
    """Which subjects a child user can access."""
    __tablename__ = "user_subjects"

    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("app_users.id"), primary_key=True)
    subject_id: Mapped[str] = mapped_column(String(50), ForeignKey("subjects.id"), primary_key=True)
