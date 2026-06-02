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


class BlockProgressEntry(BaseModel):
    topicId: str
    unitId: str
    blockIndex: int
    completed: bool = False
    score: int = 0
    totalItems: int = 0
    completedAt: datetime | None = None


class ProgressSyncRequest(BaseModel):
    deviceId: str
    progress: list[ProgressEntry]
    blockProgress: list[BlockProgressEntry] = []


class ProgressSyncResponse(BaseModel):
    status: str
    syncedAt: datetime
    syncedCount: int


class ProgressUnitResponse(BaseModel):
    unitId: str
    completed: bool
    score: int
    totalItems: int = 0
    completedItems: int = 0
    testScore: int | None = None


class BlockProgressResponse(BaseModel):
    topicId: str
    unitId: str
    blockIndex: int
    completed: bool
    score: int
    totalItems: int
    completedAt: datetime | None = None


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
    blockProgress: list[BlockProgressResponse] = []
    lastSyncedAt: datetime | None = None
