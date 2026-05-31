from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from database import get_db
from dependencies import get_current_admin
from models import AdminUser
from schemas.auth import LoginRequest, LoginResponse, RefreshResponse
from services.auth_service import verify_password, create_token

router = APIRouter()


@router.post("/login", response_model=LoginResponse)
async def login(request: LoginRequest, db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(AdminUser).where(AdminUser.username == request.username)
    )
    user = result.scalar_one_or_none()
    if user is None or not verify_password(request.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid username or password",
        )

    token = create_token(user.id, user.username)
    return LoginResponse(access_token=token, username=user.username)


@router.post("/refresh", response_model=RefreshResponse)
async def refresh_token(admin: dict = Depends(get_current_admin)):
    token = create_token(int(admin["sub"]), admin["username"])
    return RefreshResponse(access_token=token)
