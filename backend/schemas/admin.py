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
