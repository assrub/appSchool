import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from models import Subject, Topic, ExerciseUnit, ExerciseBlock, ExerciseItem


@pytest.mark.asyncio
async def test_list_subjects(client: AsyncClient, admin_headers, db_session):
    subject = Subject(id="english", name="English", icon="📚")
    db_session.add(subject)
    await db_session.commit()

    response = await client.get("/api/v1/admin/subjects", headers=admin_headers)
    assert response.status_code == 200
    data = response.json()
    assert len(data) >= 1
    assert any(s["id"] == "english" for s in data)


@pytest.mark.asyncio
async def test_create_subject(client: AsyncClient, admin_headers):
    response = await client.post("/api/v1/admin/subjects", headers=admin_headers, json={
        "id": "math",
        "name": "Mathematics",
        "icon": "🔢",
        "color": "#2196F3"
    })
    assert response.status_code == 201
    data = response.json()
    assert data["id"] == "math"
    assert data["name"] == "Mathematics"


@pytest.mark.asyncio
async def test_update_subject(client: AsyncClient, admin_headers, db_session):
    subject = Subject(id="english", name="English", icon="📚")
    db_session.add(subject)
    await db_session.commit()

    response = await client.put("/api/v1/admin/subjects/english", headers=admin_headers, json={
        "name": "English Updated",
        "icon": "📖"
    })
    assert response.status_code == 200
    data = response.json()
    assert data["name"] == "English Updated"


@pytest.mark.asyncio
async def test_delete_subject_soft(client: AsyncClient, admin_headers, db_session):
    subject = Subject(id="english", name="English", icon="📚")
    db_session.add(subject)
    await db_session.commit()

    response = await client.delete("/api/v1/admin/subjects/english", headers=admin_headers)
    assert response.status_code == 200

    # Verify soft delete
    await db_session.refresh(subject)
    assert subject.is_active == False


@pytest.mark.asyncio
async def test_admin_requires_auth(client: AsyncClient):
    response = await client.get("/api/v1/admin/subjects")
    assert response.status_code == 403


@pytest.mark.asyncio
async def test_admin_rejects_app_user(client: AsyncClient, app_headers):
    response = await client.get("/api/v1/admin/subjects", headers=app_headers)
    assert response.status_code == 403


@pytest.mark.asyncio
async def test_dashboard_metrics(client: AsyncClient, admin_headers, db_session):
    response = await client.get("/api/v1/admin/progress/dashboard-metrics", headers=admin_headers)
    assert response.status_code == 200
    data = response.json()
    assert "totalUsers" in data
    assert "totalProgress" in data


@pytest.mark.asyncio
async def test_system_health(client: AsyncClient, admin_headers):
    response = await client.get("/api/v1/admin/system/health", headers=admin_headers)
    assert response.status_code == 200
    data = response.json()
    assert "status" in data


@pytest.mark.asyncio
async def test_db_query_select_only(client: AsyncClient, admin_headers):
    response = await client.post("/api/v1/admin/system/db-query", headers=admin_headers, json={
        "query": "SELECT COUNT(*) FROM subjects"
    })
    assert response.status_code == 200


@pytest.mark.asyncio
async def test_db_query_rejects_insert(client: AsyncClient, admin_headers):
    response = await client.post("/api/v1/admin/system/db-query", headers=admin_headers, json={
        "query": "INSERT INTO subjects VALUES ('hack', 'Hacked')"
    })
    assert response.status_code == 400


@pytest.mark.asyncio
async def test_db_query_rejects_subquery_with_admin_users(client: AsyncClient, admin_headers):
    response = await client.post("/api/v1/admin/system/db-query", headers=admin_headers, json={
        "query": "SELECT * FROM subjects WHERE id = (SELECT password_hash FROM admin_users LIMIT 1)"
    })
    assert response.status_code == 400
