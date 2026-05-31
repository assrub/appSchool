from pydantic import BaseModel
from datetime import datetime


class ProgressEntry(BaseModel):
    topicId: str
    unitId: str
    completed: bool = False
    score: int = 0
    totalItems: int = 0
    completedItems: int = 0
    testScore: int | None = None
    completedAt: datetime | None = None


class ProgressSyncRequest(BaseModel):
    deviceId: str
    progress: list[ProgressEntry]


class ProgressSyncResponse(BaseModel):
    status: str
    syncedAt: datetime
    syncedCount: int


class ProgressUnitResponse(BaseModel):
    unitId: str
    completed: bool
    score: int


class ProgressTopicResponse(BaseModel):
    topicId: str
    units: list[ProgressUnitResponse]
    testScore: int | None = None


class ProgressSubjectResponse(BaseModel):
    subjectId: str
    topics: list[ProgressTopicResponse]


class ProgressResponse(BaseModel):
    deviceId: str
    subjects: list[ProgressSubjectResponse]
    lastSyncedAt: datetime | None = None
