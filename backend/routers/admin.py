from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.orm import selectinload

from database import get_db
from dependencies import get_current_admin
from models import Subject, Topic, ExerciseUnit, ExerciseBlock, ExerciseItem
from schemas.admin import (
    SubjectCreate, SubjectUpdate, SubjectResponse as AdminSubjectResponse,
    TopicCreate, TopicUpdate, TopicResponse as AdminTopicResponse,
    MessageResponse,
    UnitCreate, UnitUpdate, UnitResponse,
    BlockCreate, BlockUpdate, BlockResponse,
    ItemCreate, ItemUpdate, ItemResponse,
)

router = APIRouter()


# ── Subjects ──────────────────────────────────────────────

@router.get("/subjects", response_model=list[AdminSubjectResponse])
async def list_subjects(
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    result = await db.execute(
        select(Subject).order_by(Subject.sort_order)
    )
    return [AdminSubjectResponse.model_validate(s) for s in result.scalars().all()]


@router.post("/subjects", response_model=AdminSubjectResponse, status_code=201)
async def create_subject(
    data: SubjectCreate,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    existing = await db.get(Subject, data.id)
    if existing:
        raise HTTPException(status_code=409, detail="Subject ID already exists")

    subject = Subject(**data.model_dump())
    db.add(subject)
    await db.commit()
    await db.refresh(subject)
    return AdminSubjectResponse.model_validate(subject)


@router.put("/subjects/{subject_id}", response_model=AdminSubjectResponse)
async def update_subject(
    subject_id: str,
    data: SubjectUpdate,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    subject = await db.get(Subject, subject_id)
    if not subject:
        raise HTTPException(status_code=404, detail="Subject not found")

    for key, value in data.model_dump(exclude_unset=True).items():
        setattr(subject, key, value)

    await db.commit()
    await db.refresh(subject)
    return AdminSubjectResponse.model_validate(subject)


@router.delete("/subjects/{subject_id}", response_model=MessageResponse)
async def delete_subject(
    subject_id: str,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    subject = await db.get(Subject, subject_id)
    if not subject:
        raise HTTPException(status_code=404, detail="Subject not found")

    subject.is_active = False
    await db.commit()
    return MessageResponse(message="Subject deactivated")


# ── Topics ────────────────────────────────────────────────

@router.get("/subjects/{subject_id}/topics", response_model=list[AdminTopicResponse])
async def list_topics(
    subject_id: str,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    result = await db.execute(
        select(Topic)
        .where(Topic.subject_id == subject_id, Topic.is_active == True)
        .order_by(Topic.sort_order)
    )
    return [AdminTopicResponse.model_validate(t) for t in result.scalars().all()]


@router.post("/topics", response_model=AdminTopicResponse, status_code=201)
async def create_topic(
    data: TopicCreate,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    existing = await db.get(Topic, data.id)
    if existing:
        raise HTTPException(status_code=409, detail="Topic ID already exists")

    topic = Topic(
        id=data.id,
        subject_id=data.subject_id,
        name=data.name,
        icon=data.icon or "",
        difficulty=data.difficulty,
        sort_order=data.sort_order,
    )
    db.add(topic)
    await db.commit()
    await db.refresh(topic)
    return AdminTopicResponse.model_validate(topic)


@router.get("/topics/{topic_id}", response_model=AdminTopicResponse)
async def get_topic(
    topic_id: str,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    topic = await db.get(Topic, topic_id)
    if not topic:
        raise HTTPException(status_code=404, detail="Topic not found")
    return AdminTopicResponse.model_validate(topic)


@router.put("/topics/{topic_id}", response_model=AdminTopicResponse)
async def update_topic(
    topic_id: str,
    data: TopicUpdate,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    topic = await db.get(Topic, topic_id)
    if not topic:
        raise HTTPException(status_code=404, detail="Topic not found")

    for key, value in data.model_dump(exclude_unset=True).items():
        setattr(topic, key, value)

    await db.commit()
    await db.refresh(topic)
    return AdminTopicResponse.model_validate(topic)


@router.delete("/topics/{topic_id}", response_model=MessageResponse)
async def delete_topic(
    topic_id: str,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    topic = await db.get(Topic, topic_id)
    if not topic:
        raise HTTPException(status_code=404, detail="Topic not found")

    topic.is_active = False
    await db.commit()
    return MessageResponse(message="Topic deactivated")


# ── Exercise Units ───────────────────────────────────────

@router.get("/topics/{topic_id}/units", response_model=list[UnitResponse])
async def list_units(
    topic_id: str,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    result = await db.execute(
        select(ExerciseUnit)
        .where(ExerciseUnit.topic_id == topic_id)
        .order_by(ExerciseUnit.sort_order)
    )
    return [UnitResponse.model_validate(u) for u in result.scalars().all()]


@router.post("/units", response_model=UnitResponse, status_code=201)
async def create_unit(
    data: UnitCreate,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    existing = await db.get(ExerciseUnit, data.id)
    if existing:
        raise HTTPException(status_code=409, detail="Unit ID already exists")

    unit = ExerciseUnit(
        id=data.id,
        topic_id=data.topic_id,
        title=data.title,
        exercise_type=data.exercise_type,
        explanation=data.explanation or "",
        input_mode=data.input_mode,
        sort_order=data.sort_order,
    )
    db.add(unit)
    await db.commit()
    await db.refresh(unit)
    return UnitResponse.model_validate(unit)


@router.get("/units/{unit_id}", response_model=UnitResponse)
async def get_unit(
    unit_id: str,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    unit = await db.get(ExerciseUnit, unit_id)
    if not unit:
        raise HTTPException(status_code=404, detail="Unit not found")
    return UnitResponse.model_validate(unit)


@router.put("/units/{unit_id}", response_model=UnitResponse)
async def update_unit(
    unit_id: str,
    data: UnitUpdate,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    unit = await db.get(ExerciseUnit, unit_id)
    if not unit:
        raise HTTPException(status_code=404, detail="Unit not found")

    for key, value in data.model_dump(exclude_unset=True).items():
        setattr(unit, key, value)

    await db.commit()
    await db.refresh(unit)
    return UnitResponse.model_validate(unit)


@router.delete("/units/{unit_id}", response_model=MessageResponse)
async def delete_unit(
    unit_id: str,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    unit = await db.get(ExerciseUnit, unit_id)
    if not unit:
        raise HTTPException(status_code=404, detail="Unit not found")

    await db.delete(unit)
    await db.commit()
    return MessageResponse(message="Unit deleted")


@router.put("/units/{unit_id}/lock", response_model=UnitResponse)
async def lock_unit(unit_id: str, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    unit = await db.get(ExerciseUnit, unit_id)
    if not unit: raise HTTPException(status_code=404, detail="Unit not found")
    unit.is_locked = True
    await db.commit()
    await db.refresh(unit)
    return UnitResponse.model_validate(unit)


@router.put("/units/{unit_id}/unlock", response_model=UnitResponse)
async def unlock_unit(unit_id: str, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    unit = await db.get(ExerciseUnit, unit_id)
    if not unit: raise HTTPException(status_code=404, detail="Unit not found")
    unit.is_locked = False
    await db.commit()
    await db.refresh(unit)
    return UnitResponse.model_validate(unit)


@router.delete("/progress/{device_id}/{topic_id}/{unit_id}", response_model=MessageResponse)
async def reset_unit_progress(device_id: str, topic_id: str, unit_id: str, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    from sqlalchemy import delete as sqldelete
    from models import Progress, AnswerHistory
    await db.execute(sqldelete(Progress).where(Progress.device_id == device_id, Progress.topic_id == topic_id, Progress.unit_id == unit_id))
    await db.execute(sqldelete(AnswerHistory).where(AnswerHistory.device_id == device_id, AnswerHistory.topic_id == topic_id, AnswerHistory.unit_id == unit_id))
    await db.commit()
    return MessageResponse(message="Progress reset")


@router.post("/topics/{topic_id}/reset-all", response_model=MessageResponse)
async def reset_topic_progress(topic_id: str, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    from sqlalchemy import delete as sqldelete, update as sqlupdate
    from models import Progress, AnswerHistory
    await db.execute(sqldelete(Progress).where(Progress.topic_id == topic_id))
    await db.execute(sqldelete(AnswerHistory).where(AnswerHistory.topic_id == topic_id))
    await db.commit()
    return MessageResponse(message="All progress reset")


@router.put("/topics/{topic_id}/lock-all", response_model=MessageResponse)
async def lock_all_units(topic_id: str, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    from sqlalchemy import update as sqlupdate
    await db.execute(sqlupdate(ExerciseUnit).where(ExerciseUnit.topic_id == topic_id).values(is_locked=True))
    await db.commit()
    return MessageResponse(message="All units locked")


@router.put("/topics/{topic_id}/unlock-all", response_model=MessageResponse)
async def unlock_all_units(topic_id: str, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    from sqlalchemy import update as sqlupdate
    await db.execute(sqlupdate(ExerciseUnit).where(ExerciseUnit.topic_id == topic_id).values(is_locked=False))
    await db.commit()
    return MessageResponse(message="All units unlocked")


# ── Exercise Blocks ──────────────────────────────────────

@router.get("/units/{unit_id}/blocks", response_model=list[BlockResponse])
async def list_blocks(
    unit_id: str,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    result = await db.execute(
        select(ExerciseBlock)
        .where(ExerciseBlock.unit_id == unit_id)
        .order_by(ExerciseBlock.sort_order)
    )
    return [BlockResponse.model_validate(b) for b in result.scalars().all()]


@router.post("/blocks", response_model=BlockResponse, status_code=201)
async def create_block(
    data: BlockCreate,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    block = ExerciseBlock(
        unit_id=data.unit_id,
        title=data.title,
        sort_order=data.sort_order,
    )
    db.add(block)
    await db.commit()
    await db.refresh(block)
    return BlockResponse.model_validate(block)


@router.get("/blocks/{block_id}", response_model=BlockResponse)
async def get_block(
    block_id: int,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    block = await db.get(ExerciseBlock, block_id)
    if not block:
        raise HTTPException(status_code=404, detail="Block not found")
    return BlockResponse.model_validate(block)


@router.put("/blocks/{block_id}", response_model=BlockResponse)
async def update_block(
    block_id: int,
    data: BlockUpdate,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    block = await db.get(ExerciseBlock, block_id)
    if not block:
        raise HTTPException(status_code=404, detail="Block not found")

    for key, value in data.model_dump(exclude_unset=True).items():
        setattr(block, key, value)

    await db.commit()
    await db.refresh(block)
    return BlockResponse.model_validate(block)


@router.delete("/blocks/{block_id}", response_model=MessageResponse)
async def delete_block(
    block_id: int,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    block = await db.get(ExerciseBlock, block_id)
    if not block:
        raise HTTPException(status_code=404, detail="Block not found")

    await db.delete(block)
    await db.commit()
    return MessageResponse(message="Block deleted")


# ── Exercise Items ───────────────────────────────────────

@router.get("/blocks/{block_id}/items", response_model=list[ItemResponse])
async def list_items(
    block_id: int,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    result = await db.execute(
        select(ExerciseItem)
        .where(ExerciseItem.block_id == block_id)
        .order_by(ExerciseItem.sort_order)
    )
    return [ItemResponse.model_validate(i) for i in result.scalars().all()]


@router.post("/items", response_model=ItemResponse, status_code=201)
async def create_item(
    data: ItemCreate,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    item = ExerciseItem(
        block_id=data.block_id,
        item_type=data.item_type,
        sentence=data.sentence or "",
        answer=data.answer or "",
        hint=data.hint,
        question=data.question,
        options=data.options,
        words=data.words,
        correct_order=data.correct_order,
        audio_url=data.audio_url,
        pairs=data.pairs,
        is_correct_boolean=data.is_correct_boolean,
        sort_order=data.sort_order,
    )
    db.add(item)
    await db.commit()
    await db.refresh(item)
    return ItemResponse.model_validate(item)


@router.put("/items/{item_id}", response_model=ItemResponse)
async def update_item(
    item_id: int,
    data: ItemUpdate,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    item = await db.get(ExerciseItem, item_id)
    if not item:
        raise HTTPException(status_code=404, detail="Item not found")

    for key, value in data.model_dump(exclude_unset=True).items():
        setattr(item, key, value)

    await db.commit()
    await db.refresh(item)
    return ItemResponse.model_validate(item)


@router.delete("/items/{item_id}", response_model=MessageResponse)
async def delete_item(
    item_id: int,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    item = await db.get(ExerciseItem, item_id)
    if not item:
        raise HTTPException(status_code=404, detail="Item not found")

    await db.delete(item)
    await db.commit()
    return MessageResponse(message="Item deleted")
