from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, update, delete
from sqlalchemy.orm import selectinload

from database import get_db
from dependencies import get_current_admin
from models import Subject, Topic, ExerciseUnit, ExerciseBlock, ExerciseItem, TopicTheory, TheorySection, UnitTheory, UnitTheorySection, TheoryVideo, Progress, AnswerHistory, StudySession, DictionaryEntry
from schemas.admin import (
    SubjectCreate, SubjectUpdate, SubjectResponse as AdminSubjectResponse,
    TopicCreate, TopicUpdate, TopicResponse as AdminTopicResponse,
    MessageResponse, ReorderRequest,
    UnitCreate, UnitUpdate, UnitResponse,
    BlockCreate, BlockUpdate, BlockResponse,
    ItemCreate, ItemUpdate, ItemResponse,
    TheorySaveRequest, VideoCreate,
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
    result = await db.execute(select(TopicTheory).where(TopicTheory.topic_id == topic_id))
    theory = result.scalar_one_or_none()
    if not theory:
        theory = TopicTheory(topic_id=topic_id, text="")
        db.add(theory)
        await db.flush()

    if data.blocks:
        import json
        theory.text = json.dumps(data.blocks, ensure_ascii=False)
    else:
        theory.text = data.text

    await db.execute(delete(TheorySection).where(TheorySection.theory_id == theory.id))
    for idx, s in enumerate(data.sections):
        db.add(TheorySection(theory_id=theory.id, title=s.title, text=s.text, examples=s.examples, sort_order=idx))

    theory.table_headers = data.table_headers
    theory.table_rows = data.table_rows
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
    result = await db.execute(select(UnitTheory).where(UnitTheory.unit_id == unit_id))
    theory = result.scalar_one_or_none()
    if not theory:
        theory = UnitTheory(unit_id=unit_id, text="")
        db.add(theory)
        await db.flush()

    if data.blocks:
        import json
        theory.text = json.dumps(data.blocks, ensure_ascii=False)
    else:
        theory.text = data.text

    await db.execute(delete(UnitTheorySection).where(UnitTheorySection.unit_theory_id == theory.id))
    for idx, s in enumerate(data.sections):
        db.add(UnitTheorySection(unit_theory_id=theory.id, title=s.title, text=s.text, examples=s.examples, sort_order=idx))

    theory.table_headers = data.table_headers
    theory.table_rows = data.table_rows
    theory.tips = data.tips
    await db.commit()
    return MessageResponse(message="Theory saved")


# ── Videos ─────────────────────────────────────────────────

@router.get("/topics/{topic_id}/videos", response_model=list[dict])
async def list_videos(topic_id: str, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    result = await db.execute(select(TheoryVideo).where(TheoryVideo.topic_id == topic_id).order_by(TheoryVideo.sort_order))
    return [{"id": v.id, "title": v.title, "url": v.url, "description": v.description, "sort_order": v.sort_order} for v in result.scalars().all()]


@router.post("/topics/{topic_id}/videos")
async def add_video(topic_id: str, data: VideoCreate, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    video = TheoryVideo(topic_id=topic_id, title=data.title, url=data.url, description=data.description)
    db.add(video)
    await db.commit()
    await db.refresh(video)
    return {"id": video.id, "title": video.title, "url": video.url}


@router.put("/videos/{video_id}")
async def update_video(video_id: int, data: VideoCreate, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    video = await db.get(TheoryVideo, video_id)
    if not video: raise HTTPException(status_code=404)
    video.title = data.title
    video.url = data.url
    video.description = data.description
    await db.commit()
    return {"id": video.id, "title": video.title, "url": video.url}


@router.delete("/videos/{video_id}", response_model=MessageResponse)
async def delete_video(video_id: int, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    video = await db.get(TheoryVideo, video_id)
    if not video: raise HTTPException(status_code=404)
    await db.delete(video)
    await db.commit()
    return MessageResponse(message="Video deleted")


# ── Uploads ────────────────────────────────────────────────

import os, uuid

@router.post("/upload")
async def upload_file(file: UploadFile = File(...), admin: dict = Depends(get_current_admin)):
    ext = os.path.splitext(file.filename or "file.bin")[1] or ".bin"
    filename = f"{uuid.uuid4().hex}{ext}"
    filepath = os.path.join("uploads", filename)
    os.makedirs("uploads", exist_ok=True)
    content = await file.read()
    with open(filepath, "wb") as f:
        f.write(content)
    return {"url": f"/uploads/{filename}"}


# ── Progress Detail ───────────────────────────────────────

@router.get("/progress/{device_id}/detail")
async def get_progress_detail(device_id: str, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    units_result = await db.execute(select(Progress).where(Progress.device_id == device_id))
    progress_rows = units_result.scalars().all()

    errors_result = await db.execute(
        select(AnswerHistory).where(AnswerHistory.device_id == device_id).order_by(AnswerHistory.answered_at.desc()).limit(50)
    )
    errors = [
        {"id": e.id, "topicId": e.topic_id, "unitId": e.unit_id, "givenAnswer": e.given_answer,
         "correctAnswer": e.correct_answer, "isCorrect": e.is_correct, "attemptNumber": e.attempt_number,
         "answeredAt": e.answered_at.isoformat() if e.answered_at else None}
        for e in errors_result.scalars().all()
    ]

    sessions_result = await db.execute(
        select(StudySession).where(StudySession.device_id == device_id).order_by(StudySession.started_at.desc()).limit(20)
    )
    sessions = [
        {"id": s.id, "topicId": s.topic_id, "startedAt": s.started_at.isoformat() if s.started_at else None,
         "endedAt": s.ended_at.isoformat() if s.ended_at else None, "exercisesAttempted": s.exercises_attempted,
         "exercisesCorrect": s.exercises_correct, "durationSeconds": s.duration_seconds}
        for s in sessions_result.scalars().all()
    ]

    return {
        "deviceId": device_id,
        "progress": [{"topicId": p.topic_id, "unitId": p.unit_id, "completed": p.completed, "score": p.score, "totalItems": p.total_items, "completedItems": p.completed_items, "testScore": p.test_score} for p in progress_rows],
        "errors": errors,
        "sessions": sessions,
    }


# ── Hard Delete ───────────────────────────────────────────

@router.delete("/subjects/{subject_id}/hard", response_model=MessageResponse)
async def hard_delete_subject(subject_id: str, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    subject = await db.get(Subject, subject_id)
    if not subject: raise HTTPException(status_code=404)
    await db.delete(subject)
    await db.commit()
    return MessageResponse(message="Subject deleted permanently")


@router.delete("/topics/{topic_id}/hard", response_model=MessageResponse)
async def hard_delete_topic(topic_id: str, db: AsyncSession = Depends(get_db), admin: dict = Depends(get_current_admin)):
    topic = await db.get(Topic, topic_id)
    if not topic: raise HTTPException(status_code=404)
    await db.delete(topic)
    await db.commit()
    return MessageResponse(message="Topic deleted permanently")


# ── Script Engine ─────────────────────────────────────────

from pydantic import BaseModel as PydanticBase

class ScriptRequest(PydanticBase):
    version: str = "1.0"
    actions: list[dict]

SCRIPT_TEMPLATE = r"""# AppSchool — Formato de Script para IA

## Estructura general

Envía un JSON con esta forma:

```json
{
  "version": "1.0",
  "actions": [ ... ]
}
```

Cada acción es un objeto con `"action"` (string) y los campos correspondientes.

---

## Acciones disponibles

### 1. upsert_subject — Crear o actualizar materia

```json
{
  "action": "upsert_subject",
  "id": "english",           // ID único (sin espacios, ej: math, science)
  "name": "Inglés",           // Nombre visible
  "icon": "📚",               // Emoji (opcional)
  "color": "#4CAF50"          // Color HEX (opcional)
}
```

### 2. upsert_topic — Crear o actualizar tema

```json
{
  "action": "upsert_topic",
  "id": "verb-to-be",         // ID único (sin espacios)
  "subject_id": "english",    // ID de la materia padre
  "name": "Verbo To Be",      // Nombre visible
  "difficulty": 1,            // 1 a 5 (opcional)
  "icon": "📝",               // Emoji (opcional)
  "theory": {                 // Teoría del tema (opcional)
    "blocks": [
      {
        "title": "¿Qué es el verbo to be?",
        "html": "<h2>El verbo más importante</h2><p>Sirve para decir <b>quién sos</b>, <b>cómo estás</b>...</p>"
      }
    ]
  }
}
```

### 3. upsert_unit — Crear o actualizar unidad de ejercicios

```json
{
  "action": "upsert_unit",
  "id": "affirmative",         // ID único
  "topic_id": "verb-to-be",    // ID del tema padre
  "title": "Afirmativo (am/is/are)",
  "input_mode": "tap",         // "tap" = tocar opciones, "type" = escribir
  "explanation": "Completa con am, is o are",
  "exercises": [ ... ]         // Lista de ejercicios (ver abajo)
}
```

### 4. delete_unit — Eliminar unidad
```json
{ "action": "delete_unit", "id": "affirmative" }
```

### 5. delete_topic — Eliminar tema
```json
{ "action": "delete_topic", "id": "verb-to-be" }
```

### 6. delete_subject — Eliminar materia
```json
{ "action": "delete_subject", "id": "english" }
```

---

## Tipos de ejercicios

Cada unidad puede tener una lista `"exercises"` con bloques de ejercicios:

```json
{
  "exercises": [
    {
      "title": "PASO 1: Pronombres",   // Título del bloque (opcional)
      "items": [ ... ]                    // Lista de ejercicios del bloque
    },
    {
      "title": "PASO 2: Familia",
      "items": [ ... ]
    }
  ]
}
```

Si no ponés bloques, los ejercicios van directamente en la unidad:

```json
{
  "exercises": [
    { "type": "fill-blank", "sentence": "I ___ happy.", "answer": "am" },
    { "type": "fill-blank", "sentence": "She ___ a doctor.", "answer": "is" }
  ]
}
```

### fill-blank — Completar el espacio

```json
{
  "type": "fill-blank",
  "sentence": "I ______ a happy student.",   // Usá ______ para el blank
  "answer": "am",                              // Respuesta correcta principal
  "answers": ["I am", "I'm"],                  // Otras respuestas válidas (opcional)
  "options": ["am", "is", "are"],              // Botones visibles en modo tap (opcional)
  "hint": "AM → Solo YO",                      // Pista (opcional)
  "input_mode": "tap"                          // "tap" o "type" (opcional, hereda de la unidad)
}
```

### multiple-choice — Elegir opción

```json
{
  "type": "multiple-choice",
  "question": "¿Cómo se dice 'yo soy'?",   // La pregunta
  "options": ["I am", "You are", "He is"],  // Opciones (mínimo 2)
  "answer": "I am"                           // La correcta (debe estar en options)
}
```

### reorder — Ordenar palabras

```json
{
  "type": "reorder",
  "words": ["am", "I", "happy"],             // Palabras desordenadas
  "correct_order": ["I", "am", "happy"],     // Orden correcto
  "hint": "Empieza con I"                     // Pista (opcional)
}
```

### listening — Escuchar y escribir

```json
{
  "type": "listening",
  "sentence": "She is a doctor",              // La frase que se escucha
  "audio_url": "/audio/doctor.mp3",           // URL del audio
  "answer": "She is a doctor"                  // Lo que debe escribir el alumno
}
```

### matching — Unir columnas

```json
{
  "type": "matching",
  "pairs": [
    {"left": "I", "right": "am"},
    {"left": "She", "right": "is"},
    {"left": "They", "right": "are"}
  ]
}
```

### true-false — ¿Es correcta la frase?

```json
{
  "type": "true-false",
  "sentence": "I are happy",           // La frase a evaluar
  "is_correct": false,                  // ¿Es correcta?
  "answer": "I am happy"               // Corrección (solo si es falsa)
}
```

---

## Ejemplo completo: Crear materia + tema + unidad con ejercicios

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
            "title": "¿Cuándo se usa?",
            "html": "<p>El <b>Present Simple</b> se usa para hablar de <u>rutinas</u> y <u>hechos</u>.</p><ul><li>I wake up at 7am.</li><li>The sun rises in the east.</li></ul>"
          },
          {
            "title": "Reglas",
            "html": "<table><tr><th>Sujeto</th><th>Verbo</th></tr><tr><td>I/You/We/They</td><td>play</td></tr><tr><td>He/She/It</td><td>plays</td></tr></table>"
          }
        ]
      }
    },
    {
      "action": "upsert_unit",
      "id": "present-simple-affirm",
      "topic_id": "present-simple",
      "title": "Afirmativo",
      "input_mode": "tap",
      "explanation": "Completá con la forma correcta del verbo",
      "exercises": [
        {
          "title": "PASO 1: Rutinas diarias",
          "items": [
            { "type": "fill-blank", "sentence": "I ______ up at 7am.", "answer": "wake", "hint": "wake up = despertarse" },
            { "type": "fill-blank", "sentence": "She ______ breakfast at 8am.", "answer": "has", "hint": "have → has con He/She/It" },
            { "type": "multiple-choice", "question": "¿Cómo se dice 'Él juega al fútbol'?", "options": ["He play football", "He plays football", "He playing football"], "answer": "He plays football" },
            { "type": "reorder", "words": ["to", "I", "school", "go"], "correct_order": ["I", "go", "to", "school"] }
          ]
        }
      ]
    }
  ]
}
```

---

## Tips para crear buen contenido

1. **Usá emojis** en iconos de materias y temas (los chicos los aman)
2. **Poné hints** en ejercicios difíciles para guiar sin dar la respuesta
3. **Alterná tipos** de ejercicios para no aburrir (fill-blank, mc, reorder, listening)
4. **Empezá fácil** (dificultad 1) y subí de a poco
5. **Usá tablas** en la teoría para comparar (sujeto vs verbo)
6. **Negritas y colores** en el HTML de teoría para resaltar lo importante
7. **Ejemplos reales** que el chico pueda relacionar con su vida diaria
8. **No más de 5 ejercicios por bloque** para mantener la atención
""".strip()

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


@router.get("/script/template")
async def get_script_template(admin: dict = Depends(get_current_admin)):
    return {"template": SCRIPT_TEMPLATE}


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

    theory_data = data.get("theory", {}).get("blocks")
    if theory_data:
        theory = (await db.execute(select(TopicTheory).where(TopicTheory.topic_id == data["id"]))).scalar_one_or_none()
        if not theory:
            theory = TopicTheory(topic_id=data["id"], text="")
            db.add(theory)
            await db.flush()
        import json
        theory.text = json.dumps(theory_data, ensure_ascii=False)

        await db.execute(delete(TheorySection).where(TheorySection.theory_id == theory.id))
        await db.execute(delete(TheoryVideo).where(TheoryVideo.topic_id == data["id"]))


async def _upsert_unit(db: AsyncSession, data: dict):
    unit = await db.get(ExerciseUnit, data["id"])
    if not unit:
        unit = ExerciseUnit(id=data["id"], topic_id=data["topic_id"], title=data["name"], input_mode=data.get("input_mode","tap"), explanation=data.get("explanation",""))
        db.add(unit)
        await db.flush()
    else:
        for k in ["title", "input_mode", "explanation"]:
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
