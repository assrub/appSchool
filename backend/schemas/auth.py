from pydantic import BaseModel


class LoginRequest(BaseModel):
    username: str
    password: str


AppLoginRequest = LoginRequest  # Same fields, just different endpoint


class LoginResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    username: str


class RefreshResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"


class AppLoginResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    userId: int
    username: str
    displayName: str
