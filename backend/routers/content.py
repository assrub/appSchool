import random

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.orm import selectinload

from database import get_db
from models import Subject, Topic, TopicTheory, TheorySection, TheoryVideo
from models import ExerciseUnit, UnitTheory, UnitTheorySection, ExerciseBlock, ExerciseItem, BlockTheory
from schemas.content import (
    SubjectsResponse, SubjectResponse, TopicResponse, TopicSummary, TopicProgress,
    TestResponse, TestQuestion,
    Unit, UnitProgress, UnitTheoryDto, TheorySectionDto,
    ExerciseBlockDto, ExerciseItemDto, TheoryDto, TableDto, TipDto, TestConfigDto, BlockTheoryDto,
)

router = APIRouter()


@router.get("/subjects", response_model=SubjectsResponse)
async def get_subjects(db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(Subject)
        .where(Subject.is_active == True)
        .order_by(Subject.sort_order)
        .options(
            selectinload(Subject.topics.and_(Topic.is_active == True)).selectinload(Topic.units)
        )
    )
    subjects = result.scalars().all()

    response_subjects = []
    for subj in subjects:
        topic_summaries = []
        for topic in subj.topics:
            if not topic.is_active:
                continue
            total_units = len(topic.units)
            topic_summaries.append(
                TopicSummary(
                    id=topic.id,
                    name=topic.name,
                    order=topic.sort_order,
                    difficulty=topic.difficulty,
                    icon=topic.icon,
                    isLocked=False,
                    progress=TopicProgress(totalUnits=total_units),
                )
            )
        response_subjects.append(
            SubjectResponse(
                id=subj.id,
                name=subj.name,
                icon=subj.icon,
                color=subj.color,
                topicsCount=len(topic_summaries),
                topics=topic_summaries,
            )
        )

    return SubjectsResponse(subjects=response_subjects)


async def _count_units_for_subject(db: AsyncSession, subject_id: str) -> int:
    from sqlalchemy import func
    result = await db.execute(
        select(func.count())
        .select_from(ExerciseUnit)
        .join(Topic)
        .where(Topic.subject_id == subject_id, Topic.is_active == True)
    )
    return result.scalar() or 0


@router.get("/topics/{topic_id}", response_model=TopicResponse)
async def get_topic(topic_id: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(Topic)
        .where(Topic.id == topic_id, Topic.is_active == True)
        .options(
            selectinload(Topic.theory).selectinload(TopicTheory.sections),
            selectinload(Topic.videos),
            selectinload(Topic.units).selectinload(ExerciseUnit.theory).selectinload(UnitTheory.sections),
            selectinload(Topic.units).selectinload(ExerciseUnit.blocks).selectinload(ExerciseBlock.theory),
            selectinload(Topic.units).selectinload(ExerciseUnit.blocks).selectinload(ExerciseBlock.items),
            selectinload(Topic.subject),
        )
    )
    topic = result.scalar_one_or_none()
    if topic is None:
        raise HTTPException(status_code=404, detail="Topic not found")

    theory_dto = None
    if topic.theory:
        import json
        blocks = None
        try:
            parsed = json.loads(topic.theory.text or "")
            if isinstance(parsed, list):
                blocks = parsed
        except:
            pass
        theory_dto = TheoryDto(
            text=topic.theory.text if not blocks else "",
            blocks=blocks,
            table=TableDto(
                headers=topic.theory.table_headers or [],
                rows=topic.theory.table_rows or [],
            ) if topic.theory.table_headers else None,
            tips=[TipDto(emoji=t["emoji"], text=t["text"]) for t in (topic.theory.tips or [])],
        )

    units_dto = []
    for unit in topic.units:
        unit_theory_dto = None
        if unit.theory:
            import json
            blocks = None
            try:
                parsed = json.loads(unit.theory.text or "")
                if isinstance(parsed, list):
                    blocks = parsed
            except:
                pass

            unit_theory_dto = UnitTheoryDto(
                text=unit.theory.text if not blocks else "",
                sections=[
                    TheorySectionDto(title=s.title, text=s.text, examples=s.examples or [])
                    for s in (unit.theory.sections or [])
                ],
                table=TableDto(
                    headers=unit.theory.table_headers or [],
                    rows=unit.theory.table_rows or [],
                ) if unit.theory.table_headers else None,
                tips=[TipDto(emoji=t["emoji"], text=t["text"]) for t in (unit.theory.tips or [])],
                blocks=blocks,
            )

        blocks_dto = []
        for block in unit.blocks:
            items_dto = [
                ExerciseItemDto(
                    sentence=item.sentence,
                    answer=item.answer,
                    hint=item.hint,
                    itemType=item.item_type,
                    inputMode=item.input_mode,
                    answers=item.answers,
                    options=item.options,
                    question=item.question,
                    words=item.words,
                    correctOrder=item.correct_order,
                    audioUrl=item.audio_url,
                    pairs=item.pairs,
                    isCorrect=item.is_correct_boolean,
                )
                for item in block.items
            ]

            block_theory_dto = None
            if block.theory:
                import json
                blocks = None
                try:
                    parsed = json.loads(block.theory.text or "")
                    if isinstance(parsed, list):
                        blocks = parsed
                except:
                    pass
                block_theory_dto = BlockTheoryDto(
                    text=block.theory.text if not blocks else "",
                    sections=[
                        TheorySectionDto(title=s.title, text=s.text, examples=s.examples or [])
                        for s in (block.theory.sections or [])
                    ],
                    table=TableDto(
                        headers=block.theory.table_headers or [],
                        rows=block.theory.table_rows or [],
                    ) if block.theory.table_headers else None,
                    tips=[TipDto(emoji=t["emoji"], text=t["text"]) for t in (block.theory.tips or [])],
                    blocks=blocks,
                )

            blocks_dto.append(ExerciseBlockDto(title=block.title, items=items_dto, theory=block_theory_dto))

        total_items = sum(len(b.items) for b in unit.blocks)
        units_dto.append(
            Unit(
                id=unit.id,
                title=unit.title,
                exerciseType=unit.exercise_type,
                explanation=unit.explanation or "",
                inputMode=unit.input_mode,
                isLocked=unit.is_locked,
                soundCorrectUrl=unit.sound_correct_url,
                soundIncorrectUrl=unit.sound_incorrect_url,
                progress=UnitProgress(totalItems=total_items),
                theory=unit_theory_dto,
                blocks=blocks_dto,
            )
        )

    return TopicResponse(
        id=topic.id,
        name=topic.name,
        subjectId=topic.subject_id,
        order=topic.sort_order,
        difficulty=topic.difficulty,
        icon=topic.icon,
        theory=theory_dto or TheoryDto(text="", table=None, tips=[]),
        units=units_dto,
        testConfig=TestConfigDto(
            totalQuestions=20,
            shuffle=True,
            includeUnits=[u.id for u in topic.units],
        ),
    )


@router.get("/topics/{topic_id}/test", response_model=TestResponse)
async def get_test(
    topic_id: str,
    count: int = Query(default=20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Topic)
        .where(Topic.id == topic_id, Topic.is_active == True)
        .options(
            selectinload(Topic.units).selectinload(ExerciseUnit.blocks).selectinload(ExerciseBlock.items)
        )
    )
    topic = result.scalar_one_or_none()
    if topic is None:
        raise HTTPException(status_code=404, detail="Topic not found")

    all_items = []
    for unit in topic.units:
        for block in unit.blocks:
            for idx, item in enumerate(block.items):
                all_items.append({
                    "id": f"{unit.id}_b{idx}",
                    "unitId": unit.id,
                    "sentence": item.sentence,
                    "answer": item.answer,
                    "hint": item.hint,
                })

    if count > len(all_items):
        count = len(all_items)
    if count == 0:
        return TestResponse(topicId=topic_id, questions=[])

    selected = random.sample(all_items, count)
    questions = [TestQuestion(**q) for q in selected]
    return TestResponse(topicId=topic_id, questions=questions)
