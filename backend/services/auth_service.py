import hashlib
import hmac
import time
import json
import os

from config import SECRET_KEY

TOKEN_EXPIRY_HOURS = 24


def hash_password(password: str) -> str:
    salt = os.urandom(16)
    key = hashlib.pbkdf2_hmac("sha256", password.encode(), salt, 100000)
    return salt.hex() + ":" + key.hex()


def verify_password(password: str, hashed: str) -> bool:
    salt_hex, key_hex = hashed.split(":")
    salt = bytes.fromhex(salt_hex)
    key = bytes.fromhex(key_hex)
    new_key = hashlib.pbkdf2_hmac("sha256", password.encode(), salt, 100000)
    return hmac.compare_digest(key, new_key)


def create_token(user_id: int, username: str, role: str = "app") -> str:
    header = {"alg": "HS256", "typ": "JWT"}
    now = int(time.time())
    payload = {
        "sub": str(user_id),
        "username": username,
        "role": role,
        "iat": now,
        "exp": now + TOKEN_EXPIRY_HOURS * 3600,
    }
    header_b64 = _b64url_encode(json.dumps(header))
    payload_b64 = _b64url_encode(json.dumps(payload))
    signature = _sign(f"{header_b64}.{payload_b64}")
    return f"{header_b64}.{payload_b64}.{signature}"


def verify_token(token: str) -> dict | None:
    try:
        parts = token.split(".")
        if len(parts) != 3:
            return None
        header_b64, payload_b64, signature = parts
        expected_sig = _sign(f"{header_b64}.{payload_b64}")
        if not hmac.compare_digest(signature, expected_sig):
            return None
        payload = json.loads(_b64url_decode(payload_b64))
        if payload.get("exp", 0) < time.time():
            return None
        return payload
    except Exception:
        return None


def _b64url_encode(data: str) -> str:
    import base64
    return base64.urlsafe_b64encode(data.encode()).decode().rstrip("=")


def _b64url_decode(data: str) -> str:
    import base64
    padding = 4 - len(data) % 4
    if padding != 4:
        data += "=" * padding
    return base64.urlsafe_b64decode(data).decode()


def _sign(data: str) -> str:
    return hmac.new(
        SECRET_KEY.encode(),
        data.encode(),
        hashlib.sha256,
    ).hexdigest()
