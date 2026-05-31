from pydantic import BaseModel
from datetime import datetime


class TipDto(BaseModel):
    emoji: str
    text: str


class TableDto(BaseModel):
    headers: list[str]
    rows: list[list[str]]


class TheoryDto(BaseModel):
    text: str
    table: TableDto | None = None
    tips: list[TipDto] = []


class TheorySectionDto(BaseModel):
    title: str
    text: str
    examples: list[str] = []


class UnitTheoryDto(BaseModel):
    text: str
    sections: list[TheorySectionDto] = []
    table: TableDto | None = None
    tips: list[TipDto] = []


class ExerciseItemDto(BaseModel):
    sentence: str
    answer: str
    hint: str | None = None


class ExerciseBlockDto(BaseModel):
    title: str
    items: list[ExerciseItemDto]


class UnitProgress(BaseModel):
    completedItems: int = 0
    totalItems: int = 0


class Unit(BaseModel):
    id: str
    title: str
    exerciseType: str
    explanation: str
    isLocked: bool = False
    progress: UnitProgress | None = None
    theory: UnitTheoryDto | None = None
    blocks: list[ExerciseBlockDto]


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


class TestConfigDto(BaseModel):
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
    theory: TheoryDto
    units: list[Unit]
    testConfig: TestConfigDto


class TestQuestion(BaseModel):
    id: str
    unitId: str
    sentence: str
    answer: str
    hint: str | None = None


class TestResponse(BaseModel):
    topicId: str
    questions: list[TestQuestion]
