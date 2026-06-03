from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from slowapi import Limiter
from slowapi.util import get_remote_address

from database import get_db
from dependencies import get_current_admin, get_current_app_user
from models import AdminUser, AppUser
from schemas.auth import LoginRequest, LoginResponse, RefreshResponse, AppLoginResponse
from services.auth_service import verify_password, create_token

router = APIRouter()
limiter = Limiter(key_func=get_remote_address)


@router.post("/login", response_model=LoginResponse)
@limiter.limit("10/minute")
async def login(request: Request, body: LoginRequest, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(AdminUser).where(AdminUser.username == body.username)
    )
    user = result.scalar_one_or_none()
    if user is None or not verify_password(body.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid username or password",
        )

    token = create_token(user.id, user.username, role="admin")
    return LoginResponse(access_token=token, username=user.username)


@router.post("/app/login", response_model=AppLoginResponse)
@limiter.limit("20/minute")
async def app_login(request: Request, body: LoginRequest, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(AppUser).where(AppUser.username == body.username, AppUser.is_active == True)
    )
    user = result.scalar_one_or_none()
    if user is None or not verify_password(body.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid username or password",
        )

    token = create_token(user.id, user.username, role="app")
    return AppLoginResponse(
        access_token=token,
        userId=user.id,
        username=user.username,
        displayName=user.display_name,
    )


@router.get("/app/me")
async def app_me(user: dict = Depends(get_current_app_user)):
    return user


@router.post("/refresh", response_model=RefreshResponse)
async def refresh_token(admin: dict = Depends(get_current_admin)):
    token = create_token(int(admin["sub"]), admin["username"], role=admin.get("role", "admin"))
    return RefreshResponse(access_token=token)
