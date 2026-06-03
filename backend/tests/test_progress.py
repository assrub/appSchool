import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from models import Progress, BlockProgress, AnswerHistory


@pytest.mark.asyncio
async def test_sync_progress_creates_new(client: AsyncClient, app_headers, app_user):
    response = await client.post("/api/v1/progress/sync", headers=app_headers, json={
        "progress": [{
            "topicId": "verb-to-be",
            "unitId": "affirmative",
            "completed": False,
            "score": 3,
            "totalItems": 10,
            "completedItems": 5,
            "accuracy": 60.0,
            "mastery": 30.0,
            "status": "in_progress",
            "itemsAttempted": 5,
            "itemsMastered": 3,
            "itemsCorrectFirst": 3,
            "timeSpentSeconds": 120
        }],
        "blockProgress": [{
            "topicId": "verb-to-be",
            "unitId": "affirmative",
            "blockIndex": 0,
            "completed": True,
            "score": 3,
            "totalItems": 5
        }]
    })
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert data["syncedCount"] == 2


@pytest.mark.asyncio
async def test_sync_progress_updates_existing(client: AsyncClient, app_headers, app_user, db_session):
    # Create initial progress
    progress = Progress(
        user_id=app_user.id,
        topic_id="verb-to-be",
        unit_id="affirmative",
        score=2,
        total_items=10,
        completed_items=3,
    )
    db_session.add(progress)
    await db_session.commit()

    # Sync with higher score
    response = await client.post("/api/v1/progress/sync", headers=app_headers, json={
        "progress": [{
            "topicId": "verb-to-be",
            "unitId": "affirmative",
            "completed": False,
            "score": 5,
            "totalItems": 10,
            "completedItems": 6,
            "accuracy": 80.0,
            "mastery": 50.0,
            "status": "in_progress",
            "itemsAttempted": 6,
            "itemsMastered": 5,
            "itemsCorrectFirst": 5,
            "timeSpentSeconds": 180
        }]
    })
    assert response.status_code == 200

    # Verify update
    await db_session.refresh(progress)
    assert progress.score == 5
    assert progress.completed_items == 6
    assert progress.accuracy == 80.0


@pytest.mark.asyncio
async def test_sync_progress_block_score_max(client: AsyncClient, app_headers, app_user, db_session):
    # Create initial block progress with score 3
    bp = BlockProgress(
        user_id=app_user.id,
        topic_id="verb-to-be",
        unit_id="affirmative",
        block_index=0,
        score=3,
        total_items=5,
    )
    db_session.add(bp)
    await db_session.commit()

    # Sync with lower score - should keep higher
    response = await client.post("/api/v1/progress/sync", headers=app_headers, json={
        "blockProgress": [{
            "topicId": "verb-to-be",
            "unitId": "affirmative",
            "blockIndex": 0,
            "completed": True,
            "score": 2,
            "totalItems": 5
        }]
    })
    assert response.status_code == 200

    await db_session.refresh(bp)
    assert bp.score == 3  # Kept higher score


@pytest.mark.asyncio
async def test_get_progress(client: AsyncClient, app_headers, app_user, db_session):
    # Create progress
    progress = Progress(
        user_id=app_user.id,
        topic_id="verb-to-be",
        unit_id="affirmative",
        completed=True,
        score=8,
        total_items=10,
        completed_items=10,
        accuracy=80.0,
        mastery=80.0,
        status="completed",
    )
    db_session.add(progress)
    await db_session.commit()

    response = await client.get("/api/v1/progress/me", headers=app_headers)
    assert response.status_code == 200
    data = response.json()
    assert data["deviceId"] == str(app_user.id)
    assert len(data["subjects"]) > 0


@pytest.mark.asyncio
async def test_record_answers(client: AsyncClient, app_headers, app_user):
    response = await client.post("/api/v1/progress/answer", headers=app_headers, json={
        "answers": [
            {
                "topicId": "verb-to-be",
                "unitId": "affirmative",
                "givenAnswer": "am",
                "correctAnswer": "am",
                "isCorrect": True
            },
            {
                "topicId": "verb-to-be",
                "unitId": "affirmative",
                "givenAnswer": "is",
                "correctAnswer": "are",
                "isCorrect": False
            }
        ]
    })
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert data["recorded"] == 2


@pytest.mark.asyncio
async def test_sync_progress_without_auth(client: AsyncClient):
    response = await client.post("/api/v1/progress/sync", json={
        "progress": []
    })
    assert response.status_code == 403


@pytest.mark.asyncio
async def test_get_progress_without_auth(client: AsyncClient):
    response = await client.get("/api/v1/progress/me")
    assert response.status_code == 403
