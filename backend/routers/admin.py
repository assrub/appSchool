from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.orm import selectinload

from database import get_db
from dependencies import get_current_admin
from models import Subject, Topic
from schemas.admin import (
    SubjectCreate, SubjectUpdate, SubjectResponse as AdminSubjectResponse,
    TopicCreate, TopicUpdate, TopicResponse as AdminTopicResponse,
    MessageResponse,
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
