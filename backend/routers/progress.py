from datetime import datetime, timezone
import json
import os

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from config import CONTENT_DIR
from database import get_db
from models import Progress, BlockProgress
from schemas.progress import (
    ProgressSyncRequest,
    ProgressSyncResponse,
    ProgressResponse,
    ProgressSubjectResponse,
    ProgressTopicResponse,
    ProgressUnitResponse,
    BlockProgressResponse,
)

router = APIRouter()


def _get_topic_subject_map() -> dict[str, str]:
    """Map topic_id -> subject_id from content files."""
    topic_map = {}
    subjects_path = os.path.join(CONTENT_DIR, "subjects.json")
    if not os.path.exists(subjects_path):
        return topic_map

    with open(subjects_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    for subject in data.get("subjects", []):
        subject_id = subject["id"]
        subject_dir = os.path.join(CONTENT_DIR, subject_id)
        if os.path.exists(subject_dir):
            for filename in os.listdir(subject_dir):
                if filename.endswith(".json"):
                    topic_id = filename.replace(".json", "")
                    topic_map[topic_id] = subject_id

    return topic_map


@router.post("/sync", response_model=ProgressSyncResponse)
async def sync_progress(
    request: ProgressSyncRequest,
    db: AsyncSession = Depends(get_db),
):
    synced_count = 0
    now = datetime.now(timezone.utc)

    for entry in request.progress:
        result = await db.execute(
            select(Progress).where(
                Progress.user_id == request.deviceId,
                Progress.topic_id == entry.topicId,
                Progress.unit_id == entry.unitId,
            )
        )
        existing = result.scalar_one_or_none()

        if existing:
            existing.completed = entry.completed
            existing.score = entry.score
            existing.total_items = entry.totalItems
            existing.completed_items = entry.completedItems
            existing.test_score = entry.testScore
            if entry.completedAt:
                existing.completed_at = entry.completedAt
        else:
            new_progress = Progress(
                user_id=request.deviceId,
                topic_id=entry.topicId,
                unit_id=entry.unitId,
                completed=entry.completed,
                score=entry.score,
                total_items=entry.totalItems,
                completed_items=entry.completedItems,
                test_score=entry.testScore,
                started_at=now,
                completed_at=entry.completedAt,
            )
            db.add(new_progress)

        synced_count += 1

    for bp_entry in request.blockProgress:
        result = await db.execute(
            select(BlockProgress).where(
                BlockProgress.user_id == request.deviceId,
                BlockProgress.topic_id == bp_entry.topicId,
                BlockProgress.unit_id == bp_entry.unitId,
                BlockProgress.block_index == bp_entry.blockIndex,
            )
        )
        existing_bp = result.scalar_one_or_none()

        if existing_bp:
            if bp_entry.score > existing_bp.score:
                existing_bp.score = bp_entry.score
            existing_bp.completed = bp_entry.completed
            existing_bp.total_items = bp_entry.totalItems
            if bp_entry.completedAt:
                existing_bp.completed_at = bp_entry.completedAt
        else:
            new_bp = BlockProgress(
                user_id=request.deviceId,
                topic_id=bp_entry.topicId,
                unit_id=bp_entry.unitId,
                block_index=bp_entry.blockIndex,
                completed=bp_entry.completed,
                score=bp_entry.score,
                total_items=bp_entry.totalItems,
                completed_at=bp_entry.completedAt,
            )
            db.add(new_bp)

        synced_count += 1

    await db.commit()

    return ProgressSyncResponse(
        status="ok",
        syncedAt=now,
        syncedCount=synced_count,
    )


@router.get("/{device_id}", response_model=ProgressResponse)
async def get_progress(
    device_id: str,
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Progress).where(Progress.user_id == device_id)
    )
    rows = result.scalars().all()

    bp_result = await db.execute(
        select(BlockProgress).where(BlockProgress.user_id == device_id)
    )
    block_rows = bp_result.scalars().all()

    topic_map = _get_topic_subject_map()
    subjects_map: dict[str, dict] = {}

    for row in rows:
        subject_id = topic_map.get(row.topic_id, "unknown")

        if subject_id not in subjects_map:
            subjects_map[subject_id] = {}

        if row.topic_id not in subjects_map[subject_id]:
            subjects_map[subject_id][row.topic_id] = []

        subjects_map[subject_id][row.topic_id].append(row)

    subjects = []
    for subject_id, topics_data in subjects_map.items():
        topics = []
        for topic_id, progress_rows in topics_data.items():
            units = []
            for p in progress_rows:
                units.append(
                    ProgressUnitResponse(
                        unitId=p.unit_id,
                        completed=p.completed,
                        score=p.score,
                        totalItems=p.total_items,
                        completedItems=p.completed_items,
                        testScore=p.test_score,
                    )
                )
            topics.append(
                ProgressTopicResponse(
                    topicId=topic_id,
                    units=units,
                    testScore=progress_rows[0].test_score if progress_rows else None,
                )
            )
        subjects.append(
            ProgressSubjectResponse(subjectId=subject_id, topics=topics)
        )

    block_progress = [
        BlockProgressResponse(
            topicId=bp.topic_id,
            unitId=bp.unit_id,
            blockIndex=bp.block_index,
            completed=bp.completed,
            score=bp.score,
            totalItems=bp.total_items,
            completedAt=bp.completed_at,
        )
        for bp in block_rows
    ]

    return ProgressResponse(deviceId=device_id, subjects=subjects, blockProgress=block_progress)
