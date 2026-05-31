from pydantic import BaseModel
from datetime import datetime


class MessageResponse(BaseModel):
    message: str


class SubjectCreate(BaseModel):
    id: str
    name: str
    icon: str = ""
    color: str = "#4CAF50"
    sort_order: int = 0
    map_config: dict | None = None


class SubjectUpdate(BaseModel):
    name: str | None = None
    icon: str | None = None
    color: str | None = None
    sort_order: int | None = None
    map_config: dict | None = None
    is_active: bool | None = None


class SubjectResponse(BaseModel):
    id: str
    name: str
    icon: str
    color: str
    sort_order: int
    is_active: bool
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class TopicCreate(BaseModel):
    id: str
    subject_id: str
    name: str
    icon: str = ""
    difficulty: int = 1
    sort_order: int = 0


class TopicUpdate(BaseModel):
    name: str | None = None
    icon: str | None = None
    difficulty: int | None = None
    sort_order: int | None = None
    is_active: bool | None = None


class TopicResponse(BaseModel):
    id: str
    subject_id: str
    name: str
    icon: str
    difficulty: int
    sort_order: int
    is_active: bool
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


# ── Exercise Units ───────────────────────────────────────

class UnitCreate(BaseModel):
    id: str
    topic_id: str
    title: str
    exercise_type: str = "fill-blank"
    explanation: str | None = None
    input_mode: str = "tap"
    sort_order: int = 0


class UnitUpdate(BaseModel):
    title: str | None = None
    exercise_type: str | None = None
    explanation: str | None = None
    input_mode: str | None = None
    sort_order: int | None = None


class UnitResponse(BaseModel):
    id: str
    topic_id: str
    title: str
    exercise_type: str
    explanation: str | None
    input_mode: str
    sort_order: int
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


# ── Exercise Blocks ──────────────────────────────────────

class BlockCreate(BaseModel):
    unit_id: str
    title: str
    sort_order: int = 0


class BlockUpdate(BaseModel):
    title: str | None = None
    sort_order: int | None = None


class BlockResponse(BaseModel):
    id: int
    unit_id: str
    title: str
    sort_order: int

    model_config = {"from_attributes": True}


# ── Exercise Items ───────────────────────────────────────

class ItemCreate(BaseModel):
    block_id: int
    item_type: str = "fill-blank"
    sentence: str | None = None
    answer: str | None = None
    hint: str | None = None
    question: str | None = None
    options: list | None = None
    words: list | None = None
    correct_order: list | None = None
    audio_url: str | None = None
    pairs: list | None = None
    is_correct_boolean: bool | None = None
    sort_order: int = 0


class ItemUpdate(BaseModel):
    item_type: str | None = None
    sentence: str | None = None
    answer: str | None = None
    hint: str | None = None
    question: str | None = None
    options: list | None = None
    words: list | None = None
    correct_order: list | None = None
    audio_url: str | None = None
    pairs: list | None = None
    is_correct_boolean: bool | None = None
    sort_order: int | None = None


class ItemResponse(BaseModel):
    id: int
    block_id: int
    item_type: str
    sentence: str | None
    answer: str | None
    hint: str | None
    question: str | None
    options: list | None
    words: list | None
    correct_order: list | None
    audio_url: str | None
    pairs: list | None
    is_correct_boolean: bool | None
    sort_order: int

    model_config = {"from_attributes": True}
