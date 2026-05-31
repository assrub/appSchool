from pydantic import BaseModel
from datetime import datetime


class Tip(BaseModel):
    emoji: str
    text: str


class TableData(BaseModel):
    headers: list[str]
    rows: list[list[str]]


class Theory(BaseModel):
    text: str
    table: TableData | None = None
    tips: list[Tip] = []


class ExerciseItem(BaseModel):
    sentence: str
    answer: str
    hint: str | None = None


class ExerciseBlock(BaseModel):
    title: str
    items: list[ExerciseItem]


class UnitProgress(BaseModel):
    completedItems: int = 0
    totalItems: int = 0

    @property
    def percent(self) -> float:
        if self.totalItems == 0:
            return 0.0
        return (self.completedItems / self.totalItems) * 100


class Unit(BaseModel):
    id: str
    title: str
    exerciseType: str
    explanation: str
    progress: UnitProgress | None = None
    blocks: list[ExerciseBlock]


class TopicProgress(BaseModel):
    completedUnits: int = 0
    totalUnits: int = 0
    percentComplete: float = 0.0


class TopicSummary(BaseModel):
    id: str
    name: str
    order: int
    difficulty: int
    icon: str
    isLocked: bool = False
    progress: TopicProgress | None = None


class SubjectResponse(BaseModel):
    id: str
    name: str
    icon: str
    color: str
    topicsCount: int
    topics: list[TopicSummary]


class SubjectsResponse(BaseModel):
    subjects: list[SubjectResponse]


class TestConfig(BaseModel):
    totalQuestions: int = 20
    shuffle: bool = True
    includeUnits: list[str]


class TopicResponse(BaseModel):
    id: str
    name: str
    subjectId: str
    order: int
    difficulty: int
    icon: str
    theory: Theory
    units: list[Unit]
    testConfig: TestConfig


class TestQuestion(BaseModel):
    id: str
    unitId: str
    sentence: str
    answer: str
    hint: str | None = None


class TestResponse(BaseModel):
    topicId: str
    questions: list[TestQuestion]
