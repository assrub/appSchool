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
    is_locked: bool = False
    icon: str = ""
    sort_order: int = 0


class UnitUpdate(BaseModel):
    title: str | None = None
    exercise_type: str | None = None
    explanation: str | None = None
    input_mode: str | None = None
    is_locked: bool | None = None
    icon: str | None = None
    sound_correct_url: str | None = None
    sound_incorrect_url: str | None = None
    sort_order: int | None = None


class UnitResponse(BaseModel):
    id: str
    topic_id: str
    title: str
    exercise_type: str
    explanation: str | None
    input_mode: str
    is_locked: bool
    icon: str
    sound_correct_url: str | None
    sound_incorrect_url: str | None
    sort_order: int
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class BlockCreate(BaseModel):
    unit_id: str
    title: str
    icon: str = ""
    shuffle: bool = False
    sort_order: int = 0


class BlockUpdate(BaseModel):
    title: str | None = None
    icon: str | None = None
    shuffle: bool | None = None
    sort_order: int | None = None


class BlockResponse(BaseModel):
    id: int
    unit_id: str
    title: str
    icon: str
    shuffle: bool
    sort_order: int

    model_config = {"from_attributes": True}


class ItemCreate(BaseModel):
    block_id: int
    item_type: str = "fill-blank"
    sentence: str | None = None
    answer: str | None = None
    answers: list | None = None
    hint: str | None = None
    question: str | None = None
    options: list | None = None
    words: list | None = None
    correct_order: list | None = None
    audio_url: str | None = None
    pairs: list | None = None
    is_correct_boolean: bool | None = None
    input_mode: str | None = None
    sort_order: int = 0


class ItemUpdate(BaseModel):
    item_type: str | None = None
    sentence: str | None = None
    answer: str | None = None
    answers: list | None = None
    hint: str | None = None
    question: str | None = None
    options: list | None = None
    words: list | None = None
    correct_order: list | None = None
    audio_url: str | None = None
    pairs: list | None = None
    is_correct_boolean: bool | None = None
    input_mode: str | None = None
    sort_order: int | None = None


class ItemResponse(BaseModel):
    id: int
    block_id: int
    item_type: str
    sentence: str | None
    answer: str | None
    answers: list | None
    hint: str | None
    question: str | None
    options: list | None
    words: list | None
    correct_order: list | None
    audio_url: str | None
    pairs: list | None
    is_correct_boolean: bool | None
    input_mode: str | None
    sort_order: int

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
    input_mode: str | None
    sort_order: int

    model_config = {"from_attributes": True}


# ── Reorder ───────────────────────────────────────────────

class ReorderRequest(BaseModel):
    items: list[dict]  # [{"id":1,"sort_order":0}, {"id":2,"sort_order":1}]


# ── Theory ────────────────────────────────────────────────

class TheorySectionItem(BaseModel):
    title: str
    text: str
    examples: list[str] = []


class TheorySaveRequest(BaseModel):
    text: str = ""
    sections: list[TheorySectionItem] = []
    table_headers: list[str] | None = None
    table_rows: list[list[str]] | None = None
    tips: list[dict] | None = None
    blocks: list[dict] | None = None


# ── Videos ────────────────────────────────────────────────

class VideoCreate(BaseModel):
    title: str
    url: str
    description: str | None = None
