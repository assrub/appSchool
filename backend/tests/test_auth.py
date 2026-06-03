import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_admin_login_success(client: AsyncClient, admin_user):
    response = await client.post("/api/v1/auth/login", json={
        "username": "testadmin",
        "password": "testpass123"
    })
    assert response.status_code == 200
    data = response.json()
    assert "access_token" in data
    assert data["username"] == "testadmin"
    assert data["token_type"] == "bearer"


@pytest.mark.asyncio
async def test_admin_login_wrong_password(client: AsyncClient, admin_user):
    response = await client.post("/api/v1/auth/login", json={
        "username": "testadmin",
        "password": "wrongpassword"
    })
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_admin_login_nonexistent_user(client: AsyncClient):
    response = await client.post("/api/v1/auth/login", json={
        "username": "nonexistent",
        "password": "testpass123"
    })
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_app_login_success(client: AsyncClient, app_user):
    response = await client.post("/api/v1/auth/app/login", json={
        "username": "teststudent",
        "password": "studentpass"
    })
    assert response.status_code == 200
    data = response.json()
    assert "access_token" in data
    assert data["userId"] == app_user.id
    assert data["displayName"] == "Test Student"


@pytest.mark.asyncio
async def test_app_login_inactive_user(client: AsyncClient, db_session, app_user):
    app_user.is_active = False
    db_session.add(app_user)
    await db_session.commit()

    response = await client.post("/api/v1/auth/app/login", json={
        "username": "teststudent",
        "password": "studentpass"
    })
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_app_me_endpoint(client: AsyncClient, app_headers, app_user):
    response = await client.get("/api/v1/auth/app/me", headers=app_headers)
    assert response.status_code == 200
    data = response.json()
    assert data["sub"] == str(app_user.id)


@pytest.mark.asyncio
async def test_app_me_without_token(client: AsyncClient):
    response = await client.get("/api/v1/auth/app/me")
    assert response.status_code == 403


@pytest.mark.asyncio
async def test_admin_endpoint_with_app_token(client: AsyncClient, app_headers):
    response = await client.get("/api/v1/admin/subjects", headers=app_headers)
    assert response.status_code == 403


@pytest.mark.asyncio
async def test_admin_endpoint_with_admin_token(client: AsyncClient, admin_headers):
    response = await client.get("/api/v1/admin/subjects", headers=admin_headers)
    assert response.status_code == 200


@pytest.mark.asyncio
async def test_token_refresh(client: AsyncClient, admin_headers):
    response = await client.post("/api/v1/auth/refresh", headers=admin_headers)
    assert response.status_code == 200
    data = response.json()
    assert "access_token" in data
