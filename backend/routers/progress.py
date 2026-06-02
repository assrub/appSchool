from datetime import datetime, timezone
import json
import os

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from config import CONTENT_DIR
from database import get_db
from models import Progress, BlockProgress, AnswerHistory
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
    import traceback
    try:
        synced_count = 0
        now = datetime.utcnow()
        user_id = int(request.deviceId)

        for entry in request.progress:
            result = await db.execute(
                select(Progress).where(
                    Progress.user_id == user_id,
                    Progress.topic_id == entry.topicId,
                    Progress.unit_id == entry.unitId,
                ).order_by(Progress.id)
            )
            all_rows = result.scalars().all()
            existing = all_rows[0] if all_rows else None

            # Delete duplicates if any
            if len(all_rows) > 1:
                from sqlalchemy import delete as sqldelete
                duplicate_ids = [r.id for r in all_rows[1:]]
                await db.execute(sqldelete(Progress).where(Progress.id.in_(duplicate_ids)))

            if existing:
                existing.completed = entry.completed
                existing.score = entry.score
                existing.total_items = entry.totalItems
                existing.completed_items = entry.completedItems
                existing.test_score = entry.testScore
                if entry.completedAt:
                    if entry.completedAt.tzinfo is not None:
                        existing.completed_at = entry.completedAt.replace(tzinfo=None)
                    else:
                        existing.completed_at = entry.completedAt
            else:
                new_progress = Progress(
                    user_id=user_id,
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
                    BlockProgress.user_id == user_id,
                    BlockProgress.topic_id == bp_entry.topicId,
                    BlockProgress.unit_id == bp_entry.unitId,
                    BlockProgress.block_index == bp_entry.blockIndex,
                ).order_by(BlockProgress.id)
            )
            all_bp = result.scalars().all()
            existing_bp = all_bp[0] if all_bp else None

            if len(all_bp) > 1:
                from sqlalchemy import delete as sqldelete
                dup_ids = [r.id for r in all_bp[1:]]
                await db.execute(sqldelete(BlockProgress).where(BlockProgress.id.in_(dup_ids)))

            if existing_bp:
                if bp_entry.score > existing_bp.score:
                    existing_bp.score = bp_entry.score
                existing_bp.completed = bp_entry.completed
                existing_bp.total_items = bp_entry.totalItems
            if bp_entry.completedAt:
                if isinstance(bp_entry.completedAt, datetime):
                    if bp_entry.completedAt.tzinfo is not None:
                        existing_bp.completed_at = bp_entry.completedAt.replace(tzinfo=None)
                    else:
                        existing_bp.completed_at = bp_entry.completedAt
                else:
                    existing_bp.completed_at = bp_entry.completedAt
            else:
                new_bp = BlockProgress(
                    user_id=user_id,
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
        from routers.websocket_manager import ws_manager
        await ws_manager.broadcast({
            "type": "progress_synced",
            "userId": user_id,
            "syncedCount": synced_count,
            "timestamp": now.isoformat()
        })
        return ProgressSyncResponse(status="ok", syncedAt=now, syncedCount=synced_count)
    except Exception as e:
        import logging
        logging.error(f"Sync error: {e}\n{traceback.format_exc()}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/{device_id}", response_model=ProgressResponse)
async def get_progress(
    device_id: str,
    db: AsyncSession = Depends(get_db),
):
    uid = int(device_id)
    result = await db.execute(
        select(Progress).where(Progress.user_id == uid)
    )
    rows = result.scalars().all()

    block_rows = []
    try:
        bp_result = await db.execute(
            select(BlockProgress).where(BlockProgress.user_id == uid)
        )
        block_rows = bp_result.scalars().all()
    except Exception as e:
        import logging
        logging.error(f"BlockProgress query failed: {e}")

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


class AnswerEntry(BaseModel):
    topicId: str
    unitId: str
    givenAnswer: str
    correctAnswer: str
    isCorrect: bool


class AnswerBatchRequest(BaseModel):
    deviceId: str
    answers: list[AnswerEntry]


@router.post("/answer")
async def record_answers(
    request: AnswerBatchRequest,
    db: AsyncSession = Depends(get_db),
):
    now = datetime.utcnow()
    user_id = int(request.deviceId)
    count = 0
    for a in request.answers:
        entry = AnswerHistory(
            user_id=user_id,
            topic_id=a.topicId,
            unit_id=a.unitId,
            given_answer=a.givenAnswer,
            correct_answer=a.correctAnswer,
            is_correct=a.isCorrect,
            answered_at=now,
        )
        db.add(entry)
        count += 1
    await db.commit()

    from routers.websocket_manager import ws_manager
    await ws_manager.broadcast({
        "type": "answers_recorded",
        "userId": user_id,
        "count": count,
        "timestamp": now.isoformat()
    })

    return {"status": "ok", "recorded": count}
