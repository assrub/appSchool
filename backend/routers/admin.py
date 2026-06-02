from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File
from sqlalchemy.ext.asyncio import AsyncSession
import os
import uuid
import datetime
import aiofiles
from sqlalchemy import select, update, delete, func, text
from sqlalchemy.orm import selectinload

from database import get_db
from dependencies import get_current_admin
from models import Subject, Topic, ExerciseUnit, ExerciseBlock, ExerciseItem, TopicTheory, TheorySection, UnitTheory, UnitTheorySection, TheoryVideo, BlockTheory, BlockTheorySection, Progress, BlockProgress, AnswerHistory, StudySession, DictionaryEntry, AppUser
from schemas.admin import (
    SubjectCreate, SubjectUpdate, SubjectResponse as AdminSubjectResponse,
    TopicCreate, TopicUpdate, TopicResponse as AdminTopicResponse,
    MessageResponse, ReorderRequest,
    UnitCreate, UnitUpdate, UnitResponse,
    BlockCreate, BlockUpdate, BlockResponse,
    ItemCreate, ItemUpdate, ItemResponse,
    TheorySaveRequest,
)

router = APIRouter()


# ── Subjects ──────────────────────────────────────────────

@router.get("/subjects", response_model=list[AdminSubjectResponse])
async def list_subjects(
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    result = await db.execute(
        select(Subject)
        .options(selectinload(Subject.topics))
        .order_by(Subject.sort_order)
    )
    subjects = result.scalars().all()
    return [AdminSubjectResponse(
        id=s.id, name=s.name, icon=s.icon, color=s.color,
        sort_order=s.sort_order, is_active=s.is_active,
        topics_count=sum(1 for t in s.topics if t.is_active),
        created_at=s.created_at, updated_at=s.updated_at
    ) for s in subjects]


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
        .options(selectinload(Topic.units))
        .order_by(Topic.sort_order)
    )
    topics = result.scalars().all()
    return [AdminTopicResponse(
        id=t.id, subject_id=t.subject_id, name=t.name, icon=t.icon,
        difficulty=t.difficulty, sort_order=t.sort_order, is_active=t.is_active,
        units_count=len(t.units), created_at=t.created_at, updated_at=t.updated_at
    ) for t in topics]


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
        .options(selectinload(ExerciseUnit.blocks).selectinload(ExerciseBlock.items))
        .order_by(ExerciseUnit.sort_order)
    )
    units = result.scalars().all()
    return [UnitResponse(
        id=u.id, topic_id=u.topic_id, title=u.title, exercise_type=u.exercise_type,
        explanation=u.explanation, input_mode=u.input_mode, is_locked=u.is_locked,
        icon=u.icon, sound_correct_url=u.sound_correct_url,
        sound_incorrect_url=u.sound_incorrect_url,
        sort_order=u.sort_order, blocks_count=len(u.blocks),
        items_count=sum(len(b.items) for b in u.blocks),
        created_at=u.created_at, updated_at=u.updated_at
    ) for u in units]


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


@router.delete("/progress/{user_id}/{topic_id}/{unit_id}", response_model=MessageResponse)
async def reset_unit_progress(user_id: int, topic_id: str, unit_id: str, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    from sqlalchemy import delete as sqldelete
    await db.execute(sqldelete(Progress).where(Progress.user_id == user_id, Progress.topic_id == topic_id, Progress.unit_id == unit_id))
    await db.execute(sqldelete(AnswerHistory).where(AnswerHistory.user_id == user_id, AnswerHistory.topic_id == topic_id, AnswerHistory.unit_id == unit_id))
    await db.commit()
    return MessageResponse(message="Progress reset for user")


@router.delete("/progress/{user_id}/reset-all", response_model=MessageResponse)
async def reset_all_user_progress(user_id: int, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    from sqlalchemy import delete as sqldelete
    await db.execute(sqldelete(Progress).where(Progress.user_id == user_id))
    await db.execute(sqldelete(AnswerHistory).where(AnswerHistory.user_id == user_id))
    await db.execute(sqldelete(BlockProgress).where(BlockProgress.user_id == user_id))
    from routers.websocket_manager import ws_manager
    await ws_manager.broadcast({
        "type": "progress_reset",
        "userId": user_id,
    })
    await db.commit()
    return MessageResponse(message="All progress reset for user")


@router.delete("/progress/{topic_id}/{unit_id}/reset-all-users", response_model=MessageResponse)
async def reset_all_users_progress(topic_id: str, unit_id: str, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    from sqlalchemy import delete as sqldelete
    await db.execute(sqldelete(Progress).where(Progress.topic_id == topic_id, Progress.unit_id == unit_id))
    await db.execute(sqldelete(AnswerHistory).where(AnswerHistory.topic_id == topic_id, AnswerHistory.unit_id == unit_id))
    await db.commit()
    return MessageResponse(message="Progress reset for all users")


@router.get("/users/progress-summary")
async def get_users_progress_summary(db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    users_result = await db.execute(select(AppUser).where(AppUser.is_active == True))
    users = users_result.scalars().all()
    result = []
    for user in users:
        progress_result = await db.execute(select(Progress).where(Progress.user_id == user.id))
        progress_rows = progress_result.scalars().all()
        total_units = len(progress_rows)
        completed_units = sum(1 for p in progress_rows if p.completed)
        result.append({
            "userId": user.id,
            "username": user.username,
            "displayName": user.display_name,
            "totalUnits": total_units,
            "completedUnits": completed_units,
            "percentComplete": round((completed_units / total_units * 100) if total_units > 0 else 0, 1),
        })
    return result


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
        .options(selectinload(ExerciseBlock.items))
        .order_by(ExerciseBlock.sort_order)
    )
    blocks = result.scalars().all()
    return [BlockResponse(
        id=b.id, unit_id=b.unit_id, title=b.title, icon=b.icon,
        shuffle=b.shuffle, sort_order=b.sort_order, items_count=len(b.items)
    ) for b in blocks]


@router.post("/blocks", response_model=BlockResponse, status_code=201)
async def create_block(
    data: BlockCreate,
    db: AsyncSession = Depends(get_db),
    admin: dict = Depends(get_current_admin),
):
    block = ExerciseBlock(
        unit_id=data.unit_id,
        title=data.title,
        icon=data.icon,
        shuffle=data.shuffle,
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
        input_mode=data.input_mode,
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


# ── Reorder ────────────────────────────────────────────────

@router.put("/items-reorder", response_model=MessageResponse)
async def reorder_items(data: ReorderRequest, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    for it in data.items:
        await db.execute(update(ExerciseItem).where(ExerciseItem.id == it["id"]).values(sort_order=it["sort_order"]))
    await db.commit()
    return MessageResponse(message="Reordered")


@router.put("/blocks-reorder", response_model=MessageResponse)
async def reorder_blocks(data: ReorderRequest, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    for it in data.items:
        await db.execute(update(ExerciseBlock).where(ExerciseBlock.id == it["id"]).values(sort_order=it["sort_order"]))
    await db.commit()
    return MessageResponse(message="Reordered")


@router.put("/units-reorder", response_model=MessageResponse)
async def reorder_units(data: ReorderRequest, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    for it in data.items:
        unit = await db.get(ExerciseUnit, it["id"])
        if unit:
            unit.sort_order = it["sort_order"]
    await db.commit()
    return MessageResponse(message="Reordered")


@router.put("/topics-reorder", response_model=MessageResponse)
async def reorder_topics(data: ReorderRequest, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    for it in data.items:
        await db.execute(update(Topic).where(Topic.id == it["id"]).values(sort_order=it["sort_order"]))
    await db.commit()
    return MessageResponse(message="Reordered")


# ── Theory (Topic) ─────────────────────────────────────────

@router.get("/topics/{topic_id}/theory", response_model=TheorySaveRequest)
async def get_topic_theory(topic_id: str, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    result = await db.execute(select(TopicTheory).where(TopicTheory.topic_id == topic_id).options(selectinload(TopicTheory.sections)))
    theory = result.scalar_one_or_none()
    if not theory:
        return TheorySaveRequest()
    import json
    blocks = None
    try:
        parsed = json.loads(theory.text)
        if isinstance(parsed, list): blocks = parsed
    except: pass
    return TheorySaveRequest(
        text=theory.text if not blocks else "",
        sections=[{"title": s.title, "text": s.text, "examples": s.examples or []} for s in (theory.sections or [])],
        table_headers=theory.table_headers,
        table_rows=theory.table_rows,
        tips=theory.tips,
        blocks=blocks,
    )


@router.put("/topics/{topic_id}/theory", response_model=MessageResponse)
async def save_topic_theory(topic_id: str, data: TheorySaveRequest, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    result = await db.execute(
        select(TopicTheory)
        .where(TopicTheory.topic_id == topic_id)
        .options(selectinload(TopicTheory.sections))
    )
    theory = result.scalar_one_or_none()
    if not theory:
        theory = TopicTheory(topic_id=topic_id, text="")
        db.add(theory)
        await db.flush()

    if data.blocks:
        import json
        theory.text = json.dumps(data.blocks, ensure_ascii=False)
    elif data.text:
        theory.text = data.text

    if data.sections:
        await db.execute(delete(TheorySection).where(TheorySection.theory_id == theory.id))
        for idx, s in enumerate(data.sections):
            db.add(TheorySection(theory_id=theory.id, title=s.title, text=s.text, examples=s.examples, sort_order=idx))

    if data.table_headers is not None:
        theory.table_headers = data.table_headers
    if data.table_rows is not None:
        theory.table_rows = data.table_rows
    if data.tips is not None:
        theory.tips = data.tips
    await db.commit()
    return MessageResponse(message="Theory saved")


# ── Theory (Unit) ──────────────────────────────────────────

@router.get("/units/{unit_id}/theory", response_model=TheorySaveRequest)
async def get_unit_theory(unit_id: str, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    result = await db.execute(select(UnitTheory).where(UnitTheory.unit_id == unit_id).options(selectinload(UnitTheory.sections)))
    theory = result.scalar_one_or_none()
    if not theory: return TheorySaveRequest()
    import json
    blocks = None
    try:
        parsed = json.loads(theory.text)
        if isinstance(parsed, list): blocks = parsed
    except: pass
    return TheorySaveRequest(text=theory.text if not blocks else "", sections=[{"title": s.title, "text": s.text, "examples": s.examples or []} for s in (theory.sections or [])], table_headers=theory.table_headers, table_rows=theory.table_rows, tips=theory.tips, blocks=blocks)


@router.put("/units/{unit_id}/theory", response_model=MessageResponse)
async def save_unit_theory(unit_id: str, data: TheorySaveRequest, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    result = await db.execute(
        select(UnitTheory)
        .where(UnitTheory.unit_id == unit_id)
        .options(selectinload(UnitTheory.sections))
    )
    theory = result.scalar_one_or_none()
    if not theory:
        theory = UnitTheory(unit_id=unit_id, text="")
        db.add(theory)
        await db.flush()

    if data.blocks:
        import json
        theory.text = json.dumps(data.blocks, ensure_ascii=False)
    elif data.text:
        theory.text = data.text

    if data.sections:
        await db.execute(delete(UnitTheorySection).where(UnitTheorySection.unit_theory_id == theory.id))
        for idx, s in enumerate(data.sections):
            db.add(UnitTheorySection(unit_theory_id=theory.id, title=s.title, text=s.text, examples=s.examples, sort_order=idx))

    if data.table_headers is not None:
        theory.table_headers = data.table_headers
    if data.table_rows is not None:
        theory.table_rows = data.table_rows
    if data.tips is not None:
        theory.tips = data.tips
    await db.commit()
    return MessageResponse(message="Theory saved")


# ── Theory (Block) ─────────────────────────────────────────

@router.get("/blocks/{block_id}/theory", response_model=TheorySaveRequest)
async def get_block_theory(block_id: int, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    result = await db.execute(select(BlockTheory).where(BlockTheory.block_id == block_id).options(selectinload(BlockTheory.sections)))
    theory = result.scalar_one_or_none()
    if not theory: return TheorySaveRequest()
    import json
    blocks = None
    try:
        parsed = json.loads(theory.text)
        if isinstance(parsed, list): blocks = parsed
    except: pass
    return TheorySaveRequest(text=theory.text if not blocks else "", sections=[{"title": s.title, "text": s.text, "examples": s.examples or []} for s in (theory.sections or [])], table_headers=theory.table_headers, table_rows=theory.table_rows, tips=theory.tips, blocks=blocks)


@router.put("/blocks/{block_id}/theory", response_model=MessageResponse)
async def save_block_theory(block_id: int, data: TheorySaveRequest, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    result = await db.execute(
        select(BlockTheory)
        .where(BlockTheory.block_id == block_id)
        .options(selectinload(BlockTheory.sections))
    )
    theory = result.scalar_one_or_none()
    if not theory:
        theory = BlockTheory(block_id=block_id, text="")
        db.add(theory)
        await db.flush()

    if data.blocks:
        import json
        theory.text = json.dumps(data.blocks, ensure_ascii=False)
    elif data.text:
        theory.text = data.text

    if data.sections:
        await db.execute(delete(BlockTheorySection).where(BlockTheorySection.block_theory_id == theory.id))
        for idx, s in enumerate(data.sections):
            db.add(BlockTheorySection(block_theory_id=theory.id, title=s.title, text=s.text, examples=s.examples, sort_order=idx))

    if data.table_headers is not None:
        theory.table_headers = data.table_headers
    if data.table_rows is not None:
        theory.table_rows = data.table_rows
    if data.tips is not None:
        theory.tips = data.tips
    await db.commit()
    return MessageResponse(message="Theory saved")


# ── Users (Children) ──────────────────────────────────────

from models import AppUser
from services.auth_service import hash_password

@router.get("/users")
async def list_users(db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    result = await db.execute(select(AppUser).order_by(AppUser.id))
    return [{"id": u.id, "username": u.username, "display_name": u.display_name, "is_active": u.is_active, "created_at": u.created_at.isoformat() if u.created_at else None} for u in result.scalars().all()]

@router.post("/users")
async def create_user(data: dict, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    user = AppUser(username=data["username"], display_name=data["display_name"], password_hash=hash_password(data["password"]))
    db.add(user); await db.commit(); await db.refresh(user)
    return {"id": user.id, "username": user.username, "display_name": user.display_name}

@router.put("/users/{user_id}")
async def update_user(user_id: int, data: dict, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    user = await db.get(AppUser, user_id)
    if not user: raise HTTPException(status_code=404)
    for k in ["username", "display_name"]:
        if k in data: setattr(user, k, data[k])
    if data.get("password"): user.password_hash = hash_password(data["password"])
    await db.commit()
    return {"id": user.id, "username": user.username, "display_name": user.display_name}

@router.delete("/users/{user_id}")
async def delete_user(user_id: int, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    user = await db.get(AppUser, user_id)
    if not user: raise HTTPException(status_code=404)
    user.is_active = False; await db.commit()
    return {"message": "User deactivated"}


# ── Script Engine ─────────────────────────────────────────

from pydantic import BaseModel as PydanticBase

class ScriptRequest(PydanticBase):
    version: str = "1.0"
    actions: list[dict]

SCRIPT_TEMPLATE = r"""# AppSchool — Guía Completa para Crear Contenido Educativo

## 📋 Estructura General del Script

Todo script debe tener esta estructura básica:

```json
{
  "version": "1.0",
  "actions": [ ... ]
}
```

Cada elemento en "actions" es una operación: crear/actualizar materia, tema, unidad, o eliminar.

---

## 🎓 1. CREAR MATERIA (upsert_subject)

La materia es el nivel más alto (ej: "Inglés", "Matemáticas").

```json
{
  "action": "upsert_subject",
  "id": "english",
  "name": "Inglés",
  "icon": "📚",
  "color": "#4CAF50"
}
```

| Campo | Descripción | Requerido |
|-------|-------------|-----------|
| id | Identificador único (sin espacios, minúsculas) | Sí |
| name | Nombre visible (ej: "Inglés") | Sí |
| icon | Emoji para mostrar (ej: 📚, 🌍, 🔢) | No |
| color | Color HEX para el tema (ej: #4CAF50) | No |

---

## 📖 2. CREAR TEMA (upsert_topic)

El tema agrupa unidades de contenido (ej: "Verbo To Be", "Past Simple").

```json
{
  "action": "upsert_topic",
  "id": "verb-to-be",
  "subject_id": "english",
  "name": "Verbo To Be",
  "difficulty": 1,
  "icon": "📝",
  "theory": {
    "blocks": [ ... ],
    "tips": [ ... ]
  }
}
```

| Campo | Descripción | Requerido |
|-------|-------------|-----------|
| id | Identificador único | Sí |
| subject_id | ID de la materia padre | Sí |
| name | Nombre visible | Sí |
| difficulty | 1 (fácil) a 5 (difícil) | No (default: 1) |
| icon | Emoji | No |
| theory | Teoría del tema (ver abajo) | No |

### 📚 TEORÍA DEL TEMA

La teoría del tema explica el CONCEPTO GENERAL. Se muestra cuando el usuario entra a los bloques de cualquier unidad de ese tema.

```json
"theory": {
  "blocks": [
    {
      "title": "Título de esta sección",
      "html": "<p>Contenido HTML con formato...</p>"
    }
  ],
  "tips": [
    { "emoji": "💡", "text": "Un consejo útil para recordar" }
  ]
}
```

#### Etiquetas HTML permitidas en "html":

| Etiqueta | Uso | Ejemplo |
|----------|-----|---------|
| `<p>` | Párrafo | `<p>Texto normal</p>` |
| `<b>` | Negrita | `<b>importante</b>` |
| `<i>` | Cursiva | `<i>énfasis</i>` |
| `<u>` | Subrayado | `<u>subrayado</u>` |
| `<h1>` | Título grande | `<h1>Título Principal</h1>` |
| `<h2>` | Subtítulo | `<h2>Subsección</h2>` |
| `<h3>` | Sub-subtítulo | `<h3>Detalle</h3>` |
| `<ul>` | Lista con puntos | `<ul><li>Item 1</li><li>Item 2</li></ul>` |
| `<ol>` | Lista numerada | `<ol><li>Paso 1</li><li>Paso 2</li></ol>` |
| `<li>` | Elemento de lista | `<li>contenido</li>` |
| `<table>` | Tabla | `<table>...</table>` |
| `<tr>` | Fila de tabla | `<tr>...</tr>` |
| `<th>` | Encabezado de columna | `<th>Nombre</th>` |
| `<td>` | Celda de datos | `<td>valor</td>` |
| `<blockquote>` | Cita destacada | `<blockquote>Frase célebre</blockquote>` |
| `<mark>` | Resaltado amarillo | `<mark>importante</mark>` |
| `<br>` | Salto de línea | `línea 1<br>línea 2` |

#### Ejemplo de teoría completa para un tema:

```json
"theory": {
  "blocks": [
    {
      "title": "¿Qué es el Present Simple?",
      "html": "<p>El <b>Present Simple</b> se usa para:</p><ul><li>🎯 Rutinas diarias</li><li>🌟 Hechos universales</li><li>💬 Preferencias y opiniones</li></ul><p>Ejemplos:<br><i>I wake up at 7am.</i><br><i>The sun rises in the east.</i></p>"
    },
    {
      "title": "Formación del Presente Simple",
      "html": "<table><tr><th>Sujeto</th><th>Verbo</th><th>Ejemplo</th></tr><tr><td>I / You / We / They</td><td>play</td><td>I play football</td></tr><tr><td>He / She / It</td><td>play + s</td><td>She plays tennis</td></tr></table>"
    },
    {
      "title": "⚠️ Cuidado con las excepciones",
      "html": "<p>Los verbos terminados en <b>-y</b> cambian a <b>-ies</b>:</p><ul><li>study → studies</li><li>fly → flies</li></ul><p>Los verbos terminados en <b>-ss, -x, -z, -ch, -sh</b> también add <b>-es</b>:</p><ul><li>kiss → kisses</li><li>watch → watches</li></ul>"
    }
  ],
  "tips": [
    { "emoji": "💡", "text": "Recordá: I/You/We/They = sin -s, He/She/It = con -s" },
    { "emoji": "🎯", "text": "Si la palabra termina en y preceded por consonante, cambia y por ies" }
  ]
}
```

---

## 📝 3. CREAR UNIDAD (upsert_unit)

La unidad contiene EJERCICIOS PRACTICOS sobre un aspecto específico del tema.

```json
{
  "action": "upsert_unit",
  "id": "present-simple-affirm",
  "topic_id": "present-simple",
  "title": "Afirmativo",
  "explanation": "Completá con la forma correcta del verbo",
  "theory": {
    "blocks": [ ... ],
    "tips": [ ... ]
  },
  "exercises": [ ... ]
}
```

| Campo | Descripción | Requerido |
|-------|-------------|-----------|
| id | Identificador único | Sí |
| topic_id | ID del tema padre | Sí |
| title | Nombre visible | Sí |
| explanation | Instrucciones para el alumno | No |
| theory | Teoría específica de esta unidad | No |
| exercises | Lista de ejercicios | Sí |

### 📚 TEORÍA DE LA UNIDAD

A diferencia de la teoría del TEMA (que es general), la teoría de la UNIDAD explica algo ESPECÍFICO de esta练习.

```json
"theory": {
  "blocks": [
    {
      "title": "Afirmativo: Oraciones afirmativas",
      "html": "<p>Para formar el afirmativo:</p><table><tr><th>Sujeto</th><th>Ser</th><th>Ejemplo</th></tr><tr><td>I</td><td>am</td><td>I am happy</td></tr><tr><td>He/She/It</td><td>is</td><td>She is tall</td></tr><tr><td>You/We/They</td><td>are</td><td>They are here</td></tr></table>"
    }
  ],
  "tips": [
    { "emoji": "💡", "text": "Recordá: I → am, He/She/It → is, You/We/They → are" }
  ]
}
```

---

## 🎯 4. TIPOS DE EJERCICIOS

### Estructura de ejercicios

Los ejercicios pueden organizarse en BLOQUES (para separar por dificultad/tema) o planos:

**Con bloques (recomendado para unidades grandes):**

```json
"exercises": [
  {
    "title": "PASO 1: Ejercicios básicos",
    "items": [ ... ]
  },
  {
    "title": "PASO 2: Ejercicios intermedios",
    "items": [ ... ]
  }
]
```

**Sin bloques (para unidades simples):**

```json
"exercises": [
  { "type": "fill-blank", ... },
  { "type": "fill-blank", ... }
]
```

---

### 4a. fill-blank — Completar el espacio

El ejercicio más común. El usuario completa un espacio en blanco.

```json
{
  "type": "fill-blank",
  "sentence": "I ______ a student.",
  "answer": "am",
  "answers": ["am", "Am"],
  "options": ["am", "is", "are"],
  "hint": "Para I usamos AM"
}
```

| Campo | Descripción | Requerido |
|-------|-------------|-----------|
| sentence | Oración con ______ para el blank | Sí |
| answer | Respuesta correcta | Sí |
| answers | Otras respuestas válidas | No |
| options | Opciones para botones (modo tap) | No |
| hint | Pista que ve el alumno | No |

**Regla para ______:** Usá exactamente 6 guiones bajos para el espacio a completar.

---

### 4b. multiple-choice — Elegir opción

Una pregunta con múltiples opciones.

```json
{
  "type": "multiple-choice",
  "question": "¿Cómo se dice 'Él juega al fútbol'?",
  "options": ["He play football", "He plays football", "He playing football"],
  "answer": "He plays football",
  "hint": "Con He/She/It el verbo termina en -s"
}
```

| Campo | Descripción | Requerido |
|-------|-------------|-----------|
| question | La pregunta a responder | Sí |
| options | Array de opciones (mínimo 2) | Sí |
| answer | Opción correcta (debe estar en options) | Sí |
| hint | Pista | No |

---

### 4c. reorder — Ordenar palabras

Mezclar las palabras y el usuario las ordena.

```json
{
  "type": "reorder",
  "words": ["am", "I", "happy"],
  "correct_order": ["I", "am", "happy"],
  "hint": "Primero va el pronombre"
}
```

| Campo | Descripción | Requerido |
|-------|-------------|-----------|
| words | Palabras desordenadas | Sí |
| correct_order | Orden correcto | Sí |
| hint | Pista | No |

---

### 4d. true-false — ¿Es correcta la oración?

Evaluar si una oración es verdadera o falsa.

```json
{
  "type": "true-false",
  "sentence": "I are happy",
  "is_correct": false,
  "answer": "I am happy"
}
```

| Campo | Descripción | Requerido |
|-------|-------------|-----------|
| sentence | Oración a evaluar | Sí |
| is_correct | true si es correcta, false si es incorrecta | Sí |
| answer | Corrección (solo si es falsa) | No |

---

### 4e. matching — Unir columnas

Relacionar elementos de dos columnas.

```json
{
  "type": "matching",
  "pairs": [
    { "left": "I", "right": "am" },
    { "left": "She", "right": "is" },
    { "left": "They", "right": "are" }
  ]
}
```

| Campo | Descripción | Requerido |
|-------|-------------|-----------|
| pairs | Array de pares {left, right} | Sí |

---

### 4f. listening — Escuchar y escribir

Escuchar un audio y escribir lo que se escucha.

```json
{
  "type": "listening",
  "sentence": "She is a doctor",
  "audio_url": "/audio/doctor.mp3",
  "answer": "She is a doctor"
}
```

| Campo | Descripción | Requerido |
|-------|-------------|-----------|
| sentence | Texto que se escucha | Sí |
| audio_url | URL del archivo de audio | Sí |
| answer | Lo que debe escribir el alumno | Sí |

---

## 🗑️ 5. ELIMINAR CONTENIDO

```json
{ "action": "delete_unit", "id": "present-simple-affirm" }
{ "action": "delete_topic", "id": "present-simple" }
{ "action": "delete_subject", "id": "english" }
```

---

## ✨ EJEMPLO COMPLETO: Inglés - Present Simple

```json
{
  "version": "1.0",
  "actions": [
    {
      "action": "upsert_subject",
      "id": "english",
      "name": "Inglés",
      "icon": "📚",
      "color": "#4CAF50"
    },
    {
      "action": "upsert_topic",
      "id": "present-simple",
      "subject_id": "english",
      "name": "Present Simple",
      "difficulty": 2,
      "icon": "🕐",
      "theory": {
        "blocks": [
          {
            "title": "¿Cuándo usamos el Present Simple?",
            "html": "<p>El <b>Present Simple</b> sirve para hablar de:</p><ul><li>🎯 <b>Rutinas diarias</b>: I wake up at 7am.</li><li>🌟 <b>Hechos generales</b>: The sun rises in the east.</li><li>💬 <b>Preferencias</b>: I like pizza.</li></ul>"
          },
          {
            "title": "Cómo formar oraciones",
            "html": "<h2>Afirmativo</h2><table><tr><th>Sujeto</th><th>Verbo</th><th>Ejemplo</th></tr><tr><td>I</td><td>am</td><td>I am happy</td></tr><tr><td>He/She/It</td><td>is</td><td>She is tall</td></tr><tr><td>You/We/They</td><td>are</td><td>They are here</td></tr></table><h2>Negativo</h2><p>Sujeto + <b>don't/doesn't</b> + verbo base</p><p>I <b>don't</b> like fish. / She <b>doesn't</b> play tennis.</p><h2>Pregunta</h2><p><b>Do/Does</b> + sujeto + verbo base?</p><p><b>Do</b> you speak English? / <b>Does</b> he live here?</p>"
          },
          {
            "title": "⚠️ Errores comunes",
            "html": "<p><mark>错误 comun:</mark> I are happy → ✅ Correcto: <b>I am happy</b></p><p><mark>错误 comun:</mark> She don't like → ✅ Correcto: <b>She doesn't like</b></p>"
          }
        ],
        "tips": [
          { "emoji": "💡", "text": "Recordá: I → am, You/We/They → are, He/She/It → is" },
          { "emoji": "🎯", "text": "Don't = Do not, Doesn't = Does not" }
        ]
      }
    },
    {
      "action": "upsert_unit",
      "id": "ps-affirm",
      "topic_id": "present-simple",
      "title": "Oraciones afirmativas",
      "explanation": "Completá con am, is o are",
      "theory": {
        "blocks": [
          {
            "title": "Afirmativo con el verbo TO BE",
            "html": "<p>Sujeto + <b>am/is/are</b> + complemento</p><table><tr><th>Yo</th><td>I <b>am</b> ('m)</td><td>I am happy / I'm happy</td></tr><tr><th>Él/Ella</th><td>He/She/It <b>is</b> ('s)</td><td>He is tall / He's tall</td></tr><tr><th>Nosotros</th><td>We/You/They <b>are</b> ('re)</td><td>They are here / They're here</td></tr></table>"
          }
        ],
        "tips": [
          { "emoji": "💡", "text": "'am, is, are' se pueden contractar: I'm, He's, She's, It's, You're, We're, They're" }
        ]
      },
      "exercises": [
        {
          "title": "PASO 1: Con I, You, We, They",
          "items": [
            { "type": "fill-blank", "sentence": "I ______ happy.", "answer": "am", "options": ["am", "is", "are"], "hint": "Usamos AM con I" },
            { "type": "fill-blank", "sentence": "They ______ at home.", "answer": "are", "options": ["am", "is", "are"], "hint": "Usamos ARE con They" },
            { "type": "multiple-choice", "question": "Seleccioná la oración correcta:", "options": ["I is tall", "I am tall", "I are tall"], "answer": "I am tall" },
            { "type": "reorder", "words": ["happy", "We", "are"], "correct_order": ["We", "are", "happy"], "hint": "We primero" }
          ]
        },
        {
          "title": "PASO 2: Con He, She, It",
          "items": [
            { "type": "fill-blank", "sentence": "She ______ a teacher.", "answer": "is", "options": ["am", "is", "are"], "hint": "Usamos IS con She" },
            { "type": "fill-blank", "sentence": "He ______ my brother.", "answer": "is", "options": ["am", "is", "are"], "hint": "Usamos IS con He" },
            { "type": "multiple-choice", "question": "¿Cómo se dice 'Él es alto'?", "options": ["He are tall", "He is tall", "He tall"], "answer": "He is tall" },
            { "type": "true-false", "sentence": "It is a cat.", "is_correct": true }
          ]
        }
      ]
    },
    {
      "action": "upsert_unit",
      "id": "ps-negat",
      "topic_id": "present-simple",
      "title": "Oraciones negativas",
      "explanation": "Completá con don't o doesn't",
      "theory": {
        "blocks": [
          {
            "title": "Negativo con verbos regulares",
            "html": "<p>Sujeto + <b>don't/doesn't</b> + verbo base</p><table><tr><th>Sujeto</th><th>No</th><th>Verbo</th><th>Ejemplo</th></tr><tr><td>I/You/We/They</td><td>don't</td><td>play</td><td>I don't play soccer</td></tr><tr><td>He/She/It</td><td>doesn't</td><td>play</td><td>She doesn't play tennis</td></tr></table><p><b>Nota:</b> Con He/She/It el verbo NO lleva -s</p>"
          }
        ],
        "tips": [
          { "emoji": "💡", "text": "Doesn't = Does not. Recordá: con he/she/it NO agregamos -s al verbo" }
        ]
      },
      "exercises": [
        {
          "title": "PASO 1: I, You, We, They",
          "items": [
            { "type": "fill-blank", "sentence": "I ______ like fish.", "answer": "don't", "options": ["don't", "doesn't"], "hint": "Con I usamos don't" },
            { "type": "fill-blank", "sentence": "They ______ play football.", "answer": "don't", "options": ["don't", "doesn't"], "hint": "Con They usamos don't" }
          ]
        },
        {
          "title": "PASO 2: He, She, It",
          "items": [
            { "type": "fill-blank", "sentence": "She ______ eat pizza.", "answer": "doesn't", "options": ["don't", "doesn't"], "hint": "Con She usamos doesn't" },
            { "type": "multiple-choice", "question": "¿Cómo se dice 'Él no juega al fútbol'?", "options": ["He don't play football", "He doesn't plays football", "He doesn't play football"], "answer": "He doesn't play football" }
          ]
        }
      ]
    }
  ]
}
```

---

## 🎨 Consejos para crear contenido EXCELENTE

### Para Materias y Temas:
1. **Usá emojis** en iconos (📚🕐📝🎯) — los chicos los adoran
2. **La teoría del TEMA** debe explicar el concepto general
3. **La teoría de la UNIDAD** debe explicar algo específico de esa práctica
4. **Empezá con dificultad 1** y aumentá gradualmente

### Para Teoría:
1. **Usá tablas** para comparar reglas (sujeto → verbo)
2. **Resaltá con `<mark>`** los errores comunes
3. **Usá `<blockquote>`** para citas o ejemplos importantes
4. **Poné tips útiles** con emojis (💡🎯⚠️)
5. **Máximo 3-4 bloques** de teoría por tema/unidad

### Para Ejercicios:
1. **Máximo 5 ejercicios por bloque** para mantener la atención
2. **Alterná tipos** de ejercicios (fill-blank, MC, reorder)
3. **Poné hints** útiles que guíen sin dar la respuesta
4. **Los primeros ejercicios** deben ser fáciles (dificultad 1)
5. **Usá ejemplos reales** de la vida del chico

### Estructura recomendada:
- **1 materia** = 2-4 temas
- **1 tema** = 3-6 unidades
- **1 unidad** = 2-3 bloques de 4-5 ejercicios

¡Así de simple! 🎓""".strip()

@router.post("/script")
async def execute_script(data: ScriptRequest, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    results = []
    for i, action in enumerate(data.actions):
        action_type = action.get("action", "")
        try:
            if action_type == "upsert_subject":
                await _upsert_subject(db, action)
                results.append({"index": i, "status": "ok", "action": action_type, "id": action.get("id")})
            elif action_type == "upsert_topic":
                await _upsert_topic(db, action)
                results.append({"index": i, "status": "ok", "action": action_type, "id": action.get("id")})
            elif action_type == "upsert_unit":
                await _upsert_unit(db, action)
                results.append({"index": i, "status": "ok", "action": action_type, "id": action.get("id")})
            elif action_type == "delete_unit":
                unit = await db.get(ExerciseUnit, action["id"])
                if unit: await db.delete(unit)
                results.append({"index": i, "status": "ok", "action": action_type, "id": action.get("id")})
            elif action_type == "delete_topic":
                topic = await db.get(Topic, action["id"])
                if topic: await db.delete(topic)
                results.append({"index": i, "status": "ok", "action": action_type, "id": action.get("id")})
            elif action_type == "delete_subject":
                subject = await db.get(Subject, action["id"])
                if subject: await db.delete(subject)
                results.append({"index": i, "status": "ok", "action": action_type, "id": action.get("id")})
            else:
                results.append({"index": i, "status": "error", "action": action_type, "error": f"Unknown action: {action_type}"})
        except Exception as e:
            results.append({"index": i, "status": "error", "action": action_type, "error": str(e)})
    await db.commit()
    return {"results": results}


THEORY_AI_GUIDE = r"""# Guía Completa para Crear Teorías en AppSchool

Esta guía te ayudará a crear contenido teórico de calidad para las diferentes secciones de la app: Temas (Topic), Unidades (Unit) y Bloques (Block).

================================================================================
1. TEORÍA DE TEMA (Topic Theory)
================================================================================

La teoría de TEMA es la explicación GENERAL del concepto. Se muestra cuando el
usuario entra a cualquier unidad de ese tema. Debe ser una introducción completa
pero accesible.

OBJETIVO: Que el alumno entienda el CONCEPTO GENERAL antes de practicar.

ESTRUCTURA RECOMENDADA:
{
  "blocks": [
    {
      "title": "¿Qué es el [CONCEPTO]?",
      "html": "<p>Explicación clara y simple del concepto...</p><p>Ejemplos concretos...</p>"
    },
    {
      "title": "📚 Reglas principales",
      "html": "<p>Lista de reglas o conceptos clave...</p><ul><li>Regla 1</li><li>Regla 2</li></ul>"
    },
    {
      "title": "💡 Ejemplos en contexto",
      "html": "<p>Ejemplos prácticos de la vida real...</p><blockquote>Frase ejemplo con el concepto</blockquote>"
    }
  ],
  "tips": [
    { "emoji": "💡", "text": "Tip memorable para recordar el concepto" },
    { "emoji": "🎯", "text": "Consejo práctico para aplicar" }
  ]
}

EJEMPLO REAL (Verbo To Be):
{
  "blocks": [
    {
      "title": "¿Qué es el Verbo To Be?",
      "html": "<p>El verbo <b>To Be</b> (ser/estar) es el más importante en inglés. Equivale a los verbos 'ser' y 'estar' en español.</p><p>Se usa para:</p><ul><li>🎯 Identificar personas y cosas: <i>I am a student</i> (Soy estudiante)</li><li>🌟 Describir estados: <i>She is happy</i> (Ella está feliz)</li><li>💬 Expresar ubicación: <i>They are at home</i> (Ellos están en casa)</li></ul>"
    },
    {
      "title": "📋 Formas del Verbo To Be",
      "html": "<table><tr><th>Sujeto</th><th>Forma</th><th>Contracción</th><th>Ejemplo</th></tr><tr><td>I</td><td>am</td><td>I'm</td><td>I am happy</td></tr><tr><td>He/She/It</td><td>is</td><td>He's/She's/It's</td><td>She is tall</td></tr><tr><td>You/We/They</td><td>are</td><td>You're/We're/They're</td><td>We are ready</td></tr></table>"
    },
    {
      "title": "⚠️ Errores comunes",
      "html": "<p><mark>错误 NO escribas 'yo soy' como 'I am soy'</mark> - En inglés solo se usa AM, nunca 'am soy'</p><p><mark>错误 NO uses 'is' con 'you'</mark> - You ALWAYS usa ARE, nunca 'you is'</p>"
    }
  ],
  "tips": [
    { "emoji": "👑", "text": "AM → Solo para YO (I). Como un rey que está solo en su trono" },
    { "emoji": "👉", "text": "IS → Para UNO solo (He, She, It). Apuntás con el dedo a una sola persona" },
    { "emoji": "🚢", "text": "ARE → Para MUCHOS (You, We, They). Una armada de barcos juntos" }
  ]
}

================================================================================
2. TEORÍA DE UNIDAD (Unit Theory)
================================================================================

La teoría de UNIDAD explica un aspecto ESPECÍFICO del tema. Mientras el tema
explica el concepto general, la unidad se enfoca en una variación o uso particular.

OBJETIVO: Detalle específico sobre AFIRMATIVO, NEGATIVO, INTERROGATIVO, etc.

CUÁNDO USAR: Cuando la unidad tiene matices diferentes que necesitan explicación
adicional más allá del concepto general.

ESTRUCTURA:
{
  "blocks": [
    {
      "title": "Cómo formar [AFIRMATIVO/NEGATIVO/INTERROGATIVO]",
      "html": "<p>Explicación específica de esta forma...</p>"
    },
    {
      "title": "📝 Estructura",
      "html": "<p>Estructura de la oración:</p><table><tr><th>Sujeto</th><th>Verbo</th><th>Complemento</th></tr><tr><td>I</td><td>am</td><td>a student</td></tr></table>"
    }
  ],
  "tips": [
    { "emoji": "💡", "text": "Tip específico de esta forma" }
  ]
}

EJEMPLO (Unidad Afirmativo del Verbo To Be):
{
  "blocks": [
    {
      "title": "Oraciones Afirmativas con To Be",
      "html": "<p>Para formar oraciones afirmativas, seguí esta estructura:</p><p><b>Sujeto + Forma del To Be + Complemento</b></p><p>Ejemplos:</p><ul><li>I am → I'm a teacher</li><li>He is → He's at work</li><li>They are → They're happy</li></ul>"
    },
    {
      "title": "⚡ Contracciones",
      "html": "<p>En conversación usamos contracciones:</p><table><tr><th>Completa</th><th>Contracción</th></tr><tr><td>I am</td><td>I'm</td></tr><tr><td>he is</td><td>he's</td></tr><tr><td>she is</td><td>she's</td></tr><tr><td>it is</td><td>it's</td></tr><tr><td>you are</td><td>you're</td></tr><tr><td>we are</td><td>we're</td></tr><tr><td>they are</td><td>they're</td></tr></table>"
    }
  ],
  "tips": [
    { "emoji": "💡", "text": "En inglés informal siempre se usan contracciones. En writing formal se puede usar la forma completa." }
  ]
}

================================================================================
3. TEORÍA DE BLOQUE (Block Theory)
================================================================================

La teoría de BLOQUE es muy específica - explica el CONTEXTO de los ejercicios
de ese bloque particular. Puede incluir reglas específicas, tips para entender
el patrón, o errores a evitar en esos ejercicios específicos.

OBJETIVO: Help the student understand the PATTERN or RULE for that specific set
of exercises.

CUÁNDO USAR: Ideal para bloques de ejercicios tricky, excepciones, o patrones
que requieren explicación específica.

ESTRUCTURA:
{
  "blocks": [
    {
      "title": "El patrón de [PATTERN NAME]",
      "html": "<p>Explicación del patrón que van a practicar...</p>"
    },
    {
      "title": "🔑 Cómo reconocerlo",
      "html": "<p>Qué buscar para identificar este patrón...</p>"
    }
  ],
  "tips": [
    { "emoji": "🎯", "text": "Tip específico para acertar estos ejercicios" }
  ]
}

EJEMPLO (Bloque de Third Person Singular - Present Simple):
{
  "blocks": [
    {
      "title": "El patrón de Third Person Singular",
      "html": "<p>Cuando el sujeto es <b>He, She o It</b>, el verbo CAMBIA:</p><p><b>verb + s / es</b></p><ul><li>play → plays</li><li>go → goes</li><li>study → studies</li></ul>"
    },
    {
      "title": "⚠️ Excepciones importantes",
      "html": "<p>Verbos que NO siguen la regla normal:</p><ul><li><b>have → has</b> (no 'haves')</li><li><b>be → is</b> (completamente diferente)</li></ul><p><mark>Estos hay que memorizarlos</mark></p>"
    }
  ],
  "tips": [
    { "emoji": "🔄", "text": "Si el sujeto es He/She/It, buscá si el verbo termina en s, x, ch, sh, o y → agregá 'es'" },
    { "emoji": "💡", "text": "Regla nemotécnica: 'Un solo personaje (He/She/It) necesita más esfuerzo para expresar acciones → agrega s/es'" }
  ]
}

================================================================================
4. ETIQUETAS HTML PERMITIDAS
================================================================================

En el campo 'html' de cada bloque podés usar:

TEXTO:
- <p>...</p> - Párrafo
- <b>...</b> - Negrita (important)
- <i>...</i> - Cursiva (emphasis)
- <u>...</u> - Subrayado
- <mark>...</mark> - Resaltado amarillo (para errores comunes)
- <br> - Salto de línea

LISTAS:
- <ul><li>...</li></ul> - Lista con viñetas
- <ol><li>...</li></ol> - Lista numerada

ENCABEZADOS:
- <h1>...</h1> - Título grande
- <h2>...</h2> - Subtítulo
- <h3>...</h3> - Sub-subtítulo

OTROS:
- <blockquote>...</blockquote> - Cita destacada
- <table>...</table> - Tabla (con <tr>, <th>, <td>)
- <a href="...">...</a> - Link

EJEMPLO COMPLETO:
"<h2>¿Qué es el Present Simple?</h2><p>El Present Simple se usa para:</p><ul><li>🎯 Rutinas diarias</li><li>🌟 Hechos universales</li></ul><table><tr><th>Sujeto</th><th>Verbo</th></tr><tr><td>I/You</td><td>play</td></tr></table><blockquote>Remember: Practice makes perfect!</blockquote>"

================================================================================
5. FORMATO DE TIPS
================================================================================

Los tips son frases cortas y memorables que ayudan al alumno a recordar
conceptos. Formato:

{
  "emoji": "🎯",  // Un emoji relacionado
  "text": "Frase corta y memorable que explique o recuerde el concepto"
}

REGLAS PARA BUENOS TIPS:
1. Ser BREVE - máximo 15-20 palabras
2. Ser MEMORABLE - usar analogías o imágenes mentales
3. Ser PRÁCTICO - indicar cómo aplicarlo

EJEMPLOS:
{ "emoji": "👑", "text": "AM → Solo para YO. Como un rey solo en su trono" }
{ "emoji": "👉", "text": "IS → Para UNO solo. Apuntás a UNA persona" }
{ "emoji": "🚢", "text": "ARE → Para MUCHOS. Una ARMADA de barcos juntos" }
{ "emoji": "🔄", "text": "Si termina en y precedida por consonante → cambia a ies" }

================================================================================
6. CÓMO USAR ESTA GUÍA CON UNA IA
================================================================================

Cuando le des esta guía a una IA (Claude, ChatGPT, etc.), seguí estos pasos:

1. COPIA toda esta guía (desde # Guía Completa hasta el final)

2. DECILE qué tipo de teoría necesitás crear:
   - "Creá la teoría para el Tema 'Past Simple' de Inglés"
   - "Creá la teoría para la Unidad 'Afirmativo' del Tema 'Verbo To Be'"
   - "Creá la teoría para el Bloque de ejercicios sobre 'Wh- Questions'"

3. PROVEÉ contexto adicional:
   - A qué año/curso va dirigido
   - Qué dificultades tiene el tema
   - Ejemplos específicos que quieras incluir

4. PEDILE el JSON estructurado según la sección correspondiente

5. REVISA que el HTML esté bien formado y los tips sean útiles

EJEMPLO DE PROMPT PARA IA:
---
Usando la guía adjunta, creá la teoría completa para el Tema 'Present
Continuous' de inglés para estudiantes de 2do año Secundario.

El tema debería incluir:
- Qué es el Present Continuous (acción happening ahora)
- Cuándo usarlo (acciones que están ocurriendo en este momento)
- Estructura: Sujeto + am/is/are + verbo-ing
- Ejercicios de práctica
---

================================================================================
7. RESUMEN RÁPIDO
================================================================================

| Tipo | Scope | Objetivo | Ideal para |
|------|-------|----------|------------|
| Topic | Concepto general | Entender el tema completo | Introducciones, overview |
| Unit | Aspecto específico | Dominar una forma (afirmativo, negativo...) | Variaciones y matices |
| Block | Detalle puntual | Entender patrones específicos | Errores comunes, tips |

PRO TIP: Empezá siempre con la teoría del Topic (general), después
agregá teoría a las Units que lo necesiten, y finalmente a los Blocks
que sean particularmente difíciles.
"""


@router.get("/script/template")
async def get_script_template(admin: dict = Depends(get_current_admin)):
    return {"template": SCRIPT_TEMPLATE, "aiGuide": THEORY_AI_GUIDE}


async def _upsert_subject(db: AsyncSession, data: dict):
    subject = await db.get(Subject, data["id"])
    if subject:
        for k in ["name", "icon", "color"]:
            if k in data: setattr(subject, k, data[k])
    else:
        db.add(Subject(id=data["id"], name=data["name"], icon=data.get("icon",""), color=data.get("color","#4CAF50")))


async def _upsert_topic(db: AsyncSession, data: dict):
    topic = await db.get(Topic, data["id"])
    if not topic:
        topic = Topic(id=data["id"], subject_id=data["subject_id"], name=data["name"], icon=data.get("icon",""), difficulty=data.get("difficulty",1))
        db.add(topic)
        await db.flush()
    else:
        for k in ["name", "icon", "difficulty", "subject_id"]:
            if k in data: setattr(topic, k, data[k])

    theory_data = data.get("theory", {})
    blocks_data = theory_data.get("blocks")
    tips_data = theory_data.get("tips")
    if blocks_data or tips_data:
        theory = (await db.execute(select(TopicTheory).where(TopicTheory.topic_id == data["id"]))).scalar_one_or_none()
        if not theory:
            theory = TopicTheory(topic_id=data["id"], text="")
            db.add(theory)
            await db.flush()
        import json
        if blocks_data:
            theory.text = json.dumps(blocks_data, ensure_ascii=False)
        if tips_data:
            theory.tips = tips_data

        await db.execute(delete(TheorySection).where(TheorySection.theory_id == theory.id))
        await db.execute(delete(TheoryVideo).where(TheoryVideo.topic_id == data["id"]))


async def _upsert_unit(db: AsyncSession, data: dict):
    unit = await db.get(ExerciseUnit, data["id"])
    if not unit:
        unit = ExerciseUnit(id=data["id"], topic_id=data["topic_id"], title=data.get("title", data["id"]), explanation=data.get("explanation",""))
        db.add(unit)
        await db.flush()
    else:
        for k in ["title", "explanation"]:
            if k in data: setattr(unit, k, data[k])

    exercises = data.get("exercises", [])
    if exercises:
        await db.execute(delete(ExerciseItem).where(ExerciseItem.block_id.in_(select(ExerciseBlock.id).where(ExerciseBlock.unit_id == data["id"]))))
        await db.execute(delete(ExerciseBlock).where(ExerciseBlock.unit_id == data["id"]))

        # exercises can be blocks with items, or flat list of items
        if isinstance(exercises[0], dict) and "items" in exercises[0]:
            for block_data in exercises:
                block = ExerciseBlock(unit_id=data["id"], title=block_data.get("title", ""))
                db.add(block)
                await db.flush()
                for item_data in block_data.get("items", []):
                    _add_item(db, block.id, item_data)
        else:
            block = ExerciseBlock(unit_id=data["id"], title="Ejercicios")
            db.add(block)
            await db.flush()
            for item_data in exercises:
                _add_item(db, block.id, item_data)

    # Unit theory
    theory_data = data.get("theory")
    if theory_data:
        blocks = theory_data.get("blocks")
        tips = theory_data.get("tips", [])
        if blocks or tips:
            existing = (await db.execute(select(UnitTheory).where(UnitTheory.unit_id == data["id"]))).scalar_one_or_none()
            if not existing:
                existing = UnitTheory(unit_id=data["id"], text="")
                db.add(existing)
                await db.flush()
            import json
            if blocks:
                existing.text = json.dumps(blocks, ensure_ascii=False)
            if tips:
                existing.tips = tips
            await db.execute(delete(UnitTheorySection).where(UnitTheorySection.theory_id == existing.id))


def _add_item(db, block_id: int, data: dict):
    item = ExerciseItem(
        block_id=block_id,
        item_type=data.get("type", "fill-blank"),
        sentence=data.get("sentence", ""),
        answer=data.get("answer", ""),
        answers=data.get("answers"),
        hint=data.get("hint"),
        question=data.get("question"),
        options=data.get("options"),
        words=data.get("words"),
        correct_order=data.get("correct_order"),
        audio_url=data.get("audio_url"),
        pairs=data.get("pairs"),
        is_correct_boolean=data.get("is_correct"),
        input_mode=data.get("input_mode"),
    )
    db.add(item)


# ── Export / Import ────────────────────────────────────────

import json as jsonlib, io
from fastapi.responses import StreamingResponse

async def _subject_to_dict(db, subject):
    topics_result = await db.execute(
        select(Topic).where(Topic.subject_id == subject.id, Topic.is_active == True)
            .options(selectinload(Topic.theory).selectinload(TopicTheory.sections),
                     selectinload(Topic.videos),
                     selectinload(Topic.units).selectinload(ExerciseUnit.theory).selectinload(UnitTheory.sections),
                     selectinload(Topic.units).selectinload(ExerciseUnit.blocks).selectinload(ExerciseBlock.items))
    )
    topics = []
    for topic in topics_result.scalars().all():
        t = {"id": topic.id, "name": topic.name, "icon": topic.icon or "", "difficulty": topic.difficulty, "sort_order": topic.sort_order}
        if topic.theory:
            t["theory"] = {
                "blocks": [{"title": s.title, "html": s.text, "examples": s.examples or []} for s in (topic.theory.sections or [])],
                "table_headers": topic.theory.table_headers, "table_rows": topic.theory.table_rows, "tips": topic.theory.tips,
            }
        t["units"] = []
        for unit in topic.units:
            u = {"id": unit.id, "title": unit.title, "input_mode": unit.input_mode, "explanation": unit.explanation or ""}
            if unit.theory:
                u["theory"] = {"text": unit.theory.text, "sections": [{"title": s.title, "text": s.text, "examples": s.examples or []} for s in (unit.theory.sections or [])], "table_headers": unit.theory.table_headers, "table_rows": unit.theory.table_rows, "tips": unit.theory.tips}
            u["blocks"] = []
            for block in unit.blocks:
                b = {"title": block.title, "items": []}
                for item in block.items:
                    b["items"].append({"type": item.item_type, "sentence": item.sentence, "answer": item.answer, "answers": item.answers, "hint": item.hint, "question": item.question, "options": item.options, "words": item.words, "correct_order": item.correct_order, "input_mode": item.input_mode})
                u["blocks"].append(b)
            t["units"].append(u)
        topics.append(t)
    return {"id": subject.id, "name": subject.name, "icon": subject.icon, "color": subject.color, "topics": topics}


@router.get("/export/{subject_id}")
async def export_subject(subject_id: str, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    subject = await db.get(Subject, subject_id)
    if not subject: raise HTTPException(status_code=404)
    data = await _subject_to_dict(db, subject)
    return data


@router.post("/import")
async def import_subject(data: dict, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    subject = await db.get(Subject, data["id"])
    if not subject:
        subject = Subject(id=data["id"], name=data["name"], icon=data.get("icon",""), color=data.get("color","#4CAF50"))
        db.add(subject); await db.flush()

    for topic_data in data.get("topics", []):
        topic = await db.get(Topic, topic_data["id"])
        if not topic:
            topic = Topic(id=topic_data["id"], subject_id=data["id"], name=topic_data["name"], icon=topic_data.get("icon",""), difficulty=topic_data.get("difficulty",1), sort_order=topic_data.get("sort_order",0))
            db.add(topic); await db.flush()

        await db.execute(delete(TheorySection).where(TheorySection.theory_id.in_(select(TopicTheory.id).where(TopicTheory.topic_id == topic_data["id"]))))
        theory = (await db.execute(select(TopicTheory).where(TopicTheory.topic_id == topic_data["id"]))).scalar_one_or_none()
        theory_data = topic_data.get("theory", {})
        if theory_data:
            if not theory: theory = TopicTheory(topic_id=topic_data["id"], text=""); db.add(theory); await db.flush()
            theory.table_headers = theory_data.get("table_headers"); theory.table_rows = theory_data.get("table_rows"); theory.tips = theory_data.get("tips")
            for b in theory_data.get("blocks", []): db.add(TheorySection(theory_id=theory.id, title=b.get("title",""), text=b.get("html",""), examples=b.get("examples",[])))

        await db.execute(delete(ExerciseItem).where(ExerciseItem.block_id.in_(select(ExerciseBlock.id).where(ExerciseBlock.unit_id.in_(select(ExerciseUnit.id).where(ExerciseUnit.topic_id == topic_data["id"]))))))
        await db.execute(delete(ExerciseBlock).where(ExerciseBlock.unit_id.in_(select(ExerciseUnit.id).where(ExerciseUnit.topic_id == topic_data["id"]))))
        await db.execute(delete(ExerciseUnit).where(ExerciseUnit.topic_id == topic_data["id"]))

        for unit_data in topic_data.get("units", []):
            unit = ExerciseUnit(id=unit_data["id"], topic_id=topic_data["id"], title=unit_data["title"], input_mode=unit_data.get("input_mode","tap"), explanation=unit_data.get("explanation",""))
            db.add(unit); await db.flush()
            ut_data = unit_data.get("theory", {})
            if ut_data:
                ut = UnitTheory(unit_id=unit_data["id"], text=ut_data.get("text",""), table_headers=ut_data.get("table_headers"), table_rows=ut_data.get("table_rows"), tips=ut_data.get("tips"))
                db.add(ut); await db.flush()
                for s in ut_data.get("sections", []): db.add(UnitTheorySection(unit_theory_id=ut.id, title=s.get("title",""), text=s.get("text",""), examples=s.get("examples",[])))
            for block_data in unit_data.get("blocks", []):
                block = ExerciseBlock(unit_id=unit_data["id"], title=block_data["title"])
                db.add(block); await db.flush()
                for item_data in block_data.get("items", []): _add_item(db, block.id, item_data)

    await db.commit()
    return {"status": "ok", "subject": data["id"]}


# ── User-Subject Assignment ────────────────────────────────

from models import UserSubject

@router.get("/subjects/{subject_id}/users")
async def get_subject_users(subject_id: str, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    result = await db.execute(select(UserSubject).where(UserSubject.subject_id == subject_id))
    return [r.user_id for r in result.scalars().all()]

@router.put("/subjects/{subject_id}/users")
async def assign_subject_users(subject_id: str, data: dict, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    await db.execute(delete(UserSubject).where(UserSubject.subject_id == subject_id))
    for uid in data.get("user_ids", []):
        db.add(UserSubject(user_id=uid, subject_id=subject_id))
    await db.commit()
    return {"status": "ok", "count": len(data.get("user_ids", []))}


# ── Analytics ──────────────────────────────────────────────

@router.get("/progress/dashboard-metrics")
async def get_dashboard_metrics(db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    from datetime import datetime, timedelta, timezone

    total_users = (await db.execute(select(func.count(AppUser.id)))).scalar() or 0
    total_progress = (await db.execute(select(func.count(Progress.id)))).scalar() or 0
    completed_units = (await db.execute(select(func.count(Progress.id)).where(Progress.completed == True))).scalar() or 0

    today = datetime.now(timezone.utc).replace(hour=0, minute=0, second=0, microsecond=0)
    active_today = (await db.execute(
        select(func.count(func.distinct(Progress.user_id)))
        .where(Progress.completed_at >= today)
    )).scalar() or 0

    avg_completion = 0
    if total_progress > 0:
        result = (await db.execute(
            select(func.avg(
                func.cast(Progress.completed_items, func.Float) / func.cast(Progress.total_items, func.Float) * 100
            )).where(Progress.total_items > 0)
        )).scalar()
        avg_completion = round(result or 0, 1)

    return {
        "totalUsers": total_users,
        "totalProgress": total_progress,
        "completedUnits": completed_units,
        "activeToday": active_today,
        "avgCompletion": avg_completion
    }


@router.get("/progress/{user_id}/analytics")
async def get_analytics(user_id: int, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    errors = (await db.execute(
        select(AnswerHistory)
        .where(AnswerHistory.user_id == user_id, AnswerHistory.is_correct == False)
        .order_by(AnswerHistory.answered_at.desc())
    )).scalars().all()
    all_answers = (await db.execute(
        select(AnswerHistory)
        .where(AnswerHistory.user_id == user_id)
        .order_by(AnswerHistory.answered_at.asc())
    )).scalars().all()

    # ── Weak units ──
    unit_data = {}
    for a in all_answers:
        key = a.unit_id
        if key not in unit_data:
            unit_data[key] = {"topicId": a.topic_id, "correct": 0, "wrong": 0, "total": 0}
        if a.is_correct:
            unit_data[key]["correct"] += 1
        else:
            unit_data[key]["wrong"] += 1
        unit_data[key]["total"] += 1
    weak_units = sorted(
        [{"unitId": k, "topicId": v["topicId"], "correct": v["correct"], "wrong": v["wrong"],
          "totalAttempts": v["total"], "errorRate": round(v["wrong"]/v["total"]*100, 1) if v["total"] > 0 else 0}
         for k, v in unit_data.items()],
        key=lambda x: -x["errorRate"]
    )

    # ── Common mistakes (with repeat count) ──
    mistake_map = {}
    for e in errors:
        key = f"{e.given_answer}→{e.correct_answer}"
        if key not in mistake_map:
            mistake_map[key] = {
                "givenAnswer": e.given_answer,
                "correctAnswer": e.correct_answer,
                "count": 0,
                "topicId": e.topic_id,
                "unitId": e.unit_id
            }
        mistake_map[key]["count"] += 1
    common_mistakes = sorted(mistake_map.values(), key=lambda x: -x["count"])[:10]

    # ── Overall ──
    total = len(all_answers)
    correct_count = sum(1 for a in all_answers if a.is_correct)
    accuracy = round(correct_count/total*100, 1) if total > 0 else 0

    # ── Per-exercise hardest exercises (with retry tracking) ──
    sentence_map = {}
    wrong_map = {}
    for e in errors:
        key = (e.topic_id, e.unit_id, e.correct_answer)
        wrong_key = key + (e.given_answer,)
        if key not in sentence_map:
            sentence_map[key] = {"topicId": e.topic_id, "unitId": e.unit_id,
                                 "correctAnswer": e.correct_answer, "totalAttempts": 0, "failedAttempts": 0,
                                 "commonWrongAnswers": [], "commonWrongCounts": []}
        sentence_map[key]["failedAttempts"] += 1
        if wrong_key not in wrong_map:
            wrong_map[wrong_key] = {"answer": e.given_answer, "count": 0}
        wrong_map[wrong_key]["count"] += 1
    for a in all_answers:
        key = (a.topic_id, a.unit_id, a.correct_answer)
        if key in sentence_map:
            sentence_map[key]["totalAttempts"] += 1
    for sk, sdata in sentence_map.items():
        exercise_wrongs = {k: v for k, v in wrong_map.items() if k[:3] == sk}
        sorted_wrongs = sorted(exercise_wrongs.values(), key=lambda x: -x["count"])[:3]
        sdata["commonWrongAnswers"] = [w["answer"] for w in sorted_wrongs]
        sdata["commonWrongCounts"] = [w["count"] for w in sorted_wrongs]
    hardest_exercises = sorted(sentence_map.values(), key=lambda x: -x["failedAttempts"])[:10]

    # ── Retry improvement tracking ──
    # Group answers by (topicId, unitId, correctAnswer) to see progress over time
    retry_improvement = []
    for (tid, uid, correct_answer), sdata in sorted(sentence_map.items(), key=lambda x: -x[1]["failedAttempts"]):
        if sdata["totalAttempts"] < 2:
            continue
        # Get the user's answers for this exercise in chronological order
        exercise_answers = [a for a in all_answers
                           if a.topic_id == tid and a.unit_id == uid and a.correct_answer == correct_answer]
        # Count consecutive wrong at start
        first_wrong_streak = 0
        for a in exercise_answers:
            if not a.is_correct:
                first_wrong_streak += 1
            else:
                break
        # Did they eventually get it right?
        eventually_correct = any(a.is_correct for a in exercise_answers)
        retry_improvement.append({
            "topicId": tid,
            "unitId": uid,
            "correctAnswer": correct_answer,
            "totalAttempts": sdata["totalAttempts"],
            "failedAttempts": sdata["failedAttempts"],
            "eventuallyCorrect": eventually_correct,
            "consecutiveWrongAtStart": first_wrong_streak,
        })

    # ── Strong units ──
    strong_units = sorted(weak_units, key=lambda x: x["errorRate"])[:5]

    return {
        "weakUnits": weak_units[:5],
        "strongUnits": strong_units[:5],
        "commonMistakes": common_mistakes,
        "overallAccuracy": accuracy,
        "totalAnswered": total,
        "totalCorrect": correct_count,
        "hardestExercises": hardest_exercises,
        "retryImprovement": retry_improvement[:10],
    }


@router.get("/progress/{user_id}/detail")
async def get_progress_detail(user_id: int, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    progress_rows = (await db.execute(select(Progress).where(Progress.user_id == user_id))).scalars().all()
    errors = (await db.execute(
    select(AnswerHistory)
    .where(AnswerHistory.user_id == user_id, AnswerHistory.is_correct == False)
    .order_by(AnswerHistory.answered_at.desc())
    .limit(15)
        )).scalars().all()
    sessions = (await db.execute(
        select(StudySession)
        .where(StudySession.user_id == user_id)
        .order_by(StudySession.started_at.desc())
        .limit(10)
    )).scalars().all()

    # Build progress map from existing records
    progress_map = {}
    for p in progress_rows:
        if p.topic_id not in progress_map:
            progress_map[p.topic_id] = {}
        if p.unit_id not in progress_map[p.topic_id]:
            progress_map[p.topic_id][p.unit_id] = p

    # Get all units from content tables (even without progress)
    all_units = await db.execute(
        select(ExerciseUnit)
        .options(
            selectinload(ExerciseUnit.blocks).selectinload(ExerciseBlock.items)
        )
        .select_from(ExerciseUnit)
        .join(Topic)
        .where(Topic.is_active == True)
    )
    content_units = all_units.scalars().all()

    # Get block progress for this user
    block_rows = (await db.execute(
        select(BlockProgress).where(BlockProgress.user_id == user_id)
    )).scalars().all()
    block_map = {}
    for bp in block_rows:
        key = (bp.topic_id, bp.unit_id, bp.block_index)
        block_map[key] = bp

    # Answer stats per unit (correct/wrong counts)
    all_answers = (await db.execute(
        select(AnswerHistory).where(AnswerHistory.user_id == user_id)
    )).scalars().all()
    unit_stats = {}
    for a in all_answers:
        key = (a.topic_id, a.unit_id)
        if key not in unit_stats:
            unit_stats[key] = {"correct": 0, "wrong": 0, "total": 0}
        if a.is_correct:
            unit_stats[key]["correct"] += 1
        else:
            unit_stats[key]["wrong"] += 1
        unit_stats[key]["total"] += 1

    # Group content units by topic with full info
    from collections import OrderedDict
    topic_data = OrderedDict()
    for cu in content_units:
        tid = cu.topic_id
        if tid not in topic_data:
            # Get topic name
            t_result = await db.execute(select(Topic).where(Topic.id == tid))
            t = t_result.scalar_one_or_none()
            topic_data[tid] = {
                "topicId": tid,
                "topicName": t.name if t else tid,
                "units": []
            }
        total_in_blocks = sum(len(b.items) for b in (cu.blocks or []))
        p = progress_map.get(tid, {}).get(cu.id)

        # Block progress for this unit
        unit_blocks = []
        for bi, b in enumerate(cu.blocks or []):
            bp_key = (tid, cu.id, bi)
            bp = block_map.get(bp_key)
            items_count = len(b.items)
            unit_blocks.append({
                "blockIndex": bi,
                "title": b.title,
                "score": bp.score if bp else 0,
                "totalItems": items_count,
                "completed": bp.completed if bp else False,
                "wrongCount": (items_count - bp.score) if bp and bp.completed else 0
            })

        stats = unit_stats.get((tid, cu.id), {"correct": 0, "wrong": 0, "total": 0})
        topic_data[tid]["units"].append({
            "unitId": cu.id,
            "topicId": tid,
            "title": cu.title,
            "completed": p.completed if p else False,
            "score": p.score if p else 0,
            "totalItems": total_in_blocks,
            "completedItems": p.completed_items if p else 0,
            "testScore": p.test_score if p else None,
            "completedAt": p.completed_at.isoformat() if p and p.completed_at else None,
            "correctCount": stats["correct"],
            "wrongCount": stats["wrong"],
            "totalAttempts": stats["total"],
            "blocks": unit_blocks
        })

    errors_data = [{
        "id": e.id,
        "topicId": e.topic_id,
        "unitId": e.unit_id,
        "givenAnswer": e.given_answer,
        "correctAnswer": e.correct_answer,
        "isCorrect": e.is_correct,
        "answeredAt": e.answered_at.isoformat() if e.answered_at else None
    } for e in errors]

    sessions_data = [{
        "topicId": s.topic_id,
        "startedAt": s.started_at.isoformat() if s.started_at else None,
        "endedAt": s.ended_at.isoformat() if s.ended_at else None,
        "exercisesAttempted": s.exercises_attempted,
        "exercisesCorrect": s.exercises_correct,
        "durationSeconds": s.duration_seconds
    } for s in sessions]

    return {
        "topics": list(topic_data.values()),
        "errors": errors_data,
        "sessions": sessions_data
    }


# ── Redo ───────────────────────────────────────────────────

@router.post("/progress/{user_id}/{topic_id}/{unit_id}/redo")
async def mark_redo(user_id: int, topic_id: str, unit_id: str, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    prog = (await db.execute(select(Progress).where(Progress.user_id == user_id, Progress.topic_id == topic_id, Progress.unit_id == unit_id))).scalar_one_or_none()
    if not prog:
        raise HTTPException(status_code=404, detail="Progress not found")
    if not prog.redo_data:
        prog.redo_data = {}
    prog.redo_data["marked"] = True
    await db.commit()
    from routers.websocket_manager import ws_manager
    await ws_manager.broadcast({"type": "redo_marked", "userId": user_id, "unitId": unit_id})
    return {"status": "ok", "unitId": unit_id}


@router.delete("/progress/{user_id}/{topic_id}/{unit_id}/redo")
async def unmark_redo(user_id: int, topic_id: str, unit_id: str, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    prog = (await db.execute(select(Progress).where(Progress.user_id == user_id, Progress.topic_id == topic_id, Progress.unit_id == unit_id))).scalar_one_or_none()
    if prog and prog.redo_data: prog.redo_data = {}
    await db.commit()
    return {"status": "ok"}


# ── System ─────────────────────────────────────────────────

from pydantic import BaseModel

class QueryRequest(BaseModel):
    query: str

@router.get("/system/health")
async def system_health(admin: dict = Depends(get_current_admin)):
    return {
        "api": "ok",
        "version_name": "3.2.51",
        "version_code": 80,
        "timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    }


@router.get("/system/logs")
async def system_logs(container: str = "api", lines: int = 100, admin: dict = Depends(get_current_admin)):
    log_path = os.path.join(os.path.dirname(__file__), "..", "logs", "api.log")
    try:
        if os.path.exists(log_path):
            with open(log_path, "r") as f:
                all_lines = f.readlines()
            tail = all_lines[-lines:] if len(all_lines) > lines else all_lines
            return {"logs": [l.rstrip() for l in tail], "container": container, "lines": len(tail)}
        else:
            return {"logs": [f"Log file not found at {log_path}"], "container": container, "lines": 0}
    except Exception as e:
        return {"logs": [f"Error: {str(e)}"], "container": container, "lines": 0}


@router.get("/system/db-tables")
async def system_db_tables(db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    from sqlalchemy import inspect as sa_inspect
    try:
        def get_tables(sync_session):
            inspector = sa_inspect(sync_session.get_bind())
            return inspector.get_table_names()
        tables_list = await db.run_sync(get_tables)
        result = []
        for tname in tables_list:
            count_result = await db.execute(text(f"SELECT COUNT(*) FROM {tname}"))
            row_count = count_result.scalar() or 0
            result.append({"name": tname, "rows": row_count})
        return {"tables": sorted(result, key=lambda t: t["name"])}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/system/db-query")
async def system_db_query(req: QueryRequest, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    import time
    query = req.query
    query_upper = query.strip().upper()
    if not query_upper.startswith("SELECT"):
        raise HTTPException(status_code=400, detail="Only SELECT queries are allowed")
    disallowed = ["INTO", "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "TRUNCATE", "CREATE"]
    for kw in disallowed:
        if kw in query_upper:
            raise HTTPException(status_code=400, detail=f"Keyword '{kw}' not allowed. Only read-only queries.")
    start = time.time()
    try:
        result = await db.execute(text(query))
        rows = result.fetchmany(200)
        columns = list(result.keys()) if result else []
        data = [list(row) for row in rows]
        for row in data:
            for i, val in enumerate(row):
                if isinstance(val, (datetime.datetime, datetime.date)):
                    row[i] = val.isoformat()
        elapsed = round(time.time() - start, 3)
        return {"columns": columns, "rows": data, "rowCount": len(data), "elapsed": elapsed}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/system/db-table/{table_name}")
async def system_db_table(table_name: str, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    import re
    if not re.match(r"^[a-zA-Z_][a-zA-Z0-9_]*$", table_name):
        raise HTTPException(status_code=400, detail="Invalid table name")
    try:
        from sqlalchemy import inspect as sa_inspect
        def get_columns(sync_session):
            inspector = sa_inspect(sync_session.get_bind())
            return inspector.get_columns(table_name)
        cols = await db.run_sync(get_columns)
        columns = [{"name": c["name"], "type": str(c["type"]), "nullable": "YES" if c.get("nullable") else "NO"} for c in cols]
        preview = await db.execute(text(f"SELECT * FROM {table_name} LIMIT 20"))
        preview_rows = [list(r) for r in preview.fetchall()]
        for row in preview_rows:
            for i, val in enumerate(row):
                if isinstance(val, (datetime.datetime, datetime.date)):
                    row[i] = val.isoformat()
        return {"tableName": table_name, "columns": columns, "preview": preview_rows}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


# ── Upload ────────────────────────────────────────────────────

IMAGES_DIR = os.path.join(os.path.dirname(__file__), "..", "uploads", "images")
os.makedirs(IMAGES_DIR, exist_ok=True)

ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"}
MAX_IMAGE_SIZE = 5 * 1024 * 1024  # 5MB


@router.post("/upload/image")
async def upload_image(
    file: UploadFile = File(...),
    admin: dict = Depends(get_current_admin),
):
    if file.content_type not in ALLOWED_IMAGE_TYPES:
        raise HTTPException(status_code=400, detail=f"Tipo de archivo no permitido: {file.content_type}. Solo se permiten: JPEG, PNG, GIF, WebP, SVG")

    if file.size and file.size > MAX_IMAGE_SIZE:
        raise HTTPException(status_code=400, detail="La imagen es demasiado grande. Máximo 5MB")

    ext = os.path.splitext(file.filename or "image")[1] or ".jpg"
    safe_filename = f"{uuid.uuid4().hex}{ext}"
    file_path = os.path.join(IMAGES_DIR, safe_filename)

    async with aiofiles.open(file_path, "wb") as out_file:
        content = await file.read()
        if len(content) > MAX_IMAGE_SIZE:
            raise HTTPException(status_code=400, detail="La imagen es demasiado grande. Máximo 5MB")
        await out_file.write(content)

    return {
        "url": f"/uploads/images/{safe_filename}",
        "filename": safe_filename,
        "contentType": file.content_type,
        "size": len(content)
    }


@router.delete("/upload/image/{filename}")
async def delete_image(
    filename: str,
    admin: dict = Depends(get_current_admin),
):
    file_path = os.path.join(IMAGES_DIR, filename)
    if os.path.exists(file_path):
        os.remove(file_path)
        return {"status": "ok", "deleted": filename}
    raise HTTPException(status_code=404, detail="Archivo no encontrado")


# ── Video Upload ─────────────────────────────────────────────

VIDEOS_DIR = os.path.join(os.path.dirname(__file__), "..", "uploads", "videos")
os.makedirs(VIDEOS_DIR, exist_ok=True)

ALLOWED_VIDEO_TYPES = {"video/mp4", "video/quicktime", "video/x-msvideo", "video/webm", "video/x-matroska"}
MAX_VIDEO_SIZE = 50 * 1024 * 1024  # 50MB


@router.post("/upload/video")
async def upload_video(
    file: UploadFile = File(...),
    admin: dict = Depends(get_current_admin),
):
    if file.content_type not in ALLOWED_VIDEO_TYPES:
        raise HTTPException(status_code=400, detail=f"Tipo de video no permitido: {file.content_type}. Solo se permiten: MP4, MOV, AVI, WebM, MKV")

    if file.size and file.size > MAX_VIDEO_SIZE:
        raise HTTPException(status_code=400, detail="El video es demasiado grande. Máximo 50MB")

    ext = os.path.splitext(file.filename or "video")[1] or ".mp4"
    safe_filename = f"{uuid.uuid4().hex}{ext}"
    file_path = os.path.join(VIDEOS_DIR, safe_filename)

    async with aiofiles.open(file_path, "wb") as out_file:
        content = await file.read()
        await out_file.write(content)

    return {
        "url": f"/uploads/videos/{safe_filename}",
        "filename": safe_filename,
        "contentType": file.content_type,
        "size": len(content)
    }
