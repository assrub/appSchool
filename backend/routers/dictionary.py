from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, desc

from database import get_db
from models import DictionaryEntry
from schemas.dictionary import (
    DictionaryEntryRequest,
    DictionaryEntryResponse,
    DictionaryListResponse,
)

router = APIRouter()


@router.post("", response_model=DictionaryEntryResponse)
async def add_dictionary_entry(
    request: DictionaryEntryRequest,
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(DictionaryEntry).where(
            DictionaryEntry.device_id == request.deviceId,
            DictionaryEntry.word == request.word,
        )
    )
    existing = result.scalar_one_or_none()

    now = datetime.now(timezone.utc)

    if existing:
        existing.times_looked_up += 1
        existing.updated_at = now
        await db.commit()
        await db.refresh(existing)
        return DictionaryEntryResponse(
            id=existing.id,
            word=existing.word,
            translation=existing.translation,
            timesLookedUp=existing.times_looked_up,
            createdAt=existing.created_at,
        )

    entry = DictionaryEntry(
        device_id=request.deviceId,
        word=request.word,
        translation=request.translation,
        source_lang=request.sourceLang,
        target_lang=request.targetLang,
    )
    db.add(entry)
    await db.commit()
    await db.refresh(entry)

    return DictionaryEntryResponse(
        id=entry.id,
        word=entry.word,
        translation=entry.translation,
        timesLookedUp=entry.times_looked_up,
        createdAt=entry.created_at,
    )


@router.get("/{device_id}", response_model=DictionaryListResponse)
async def list_dictionary(
    device_id: str,
    sort_by: str = Query(default="date", pattern="^(date|alphabetical|frequency)$"),
    order: str = Query(default="desc", pattern="^(asc|desc)$"),
    limit: int = Query(default=50, ge=1, le=200),
    offset: int = Query(default=0, ge=0),
    db: AsyncSession = Depends(get_db),
):
    count_result = await db.execute(
        select(func.count()).select_from(DictionaryEntry).where(
            DictionaryEntry.device_id == device_id
        )
    )
    total = count_result.scalar()

    sort_column = DictionaryEntry.created_at
    if sort_by == "alphabetical":
        sort_column = DictionaryEntry.word
    elif sort_by == "frequency":
        sort_column = DictionaryEntry.times_looked_up

    if order == "desc":
        sort_column = desc(sort_column)

    result = await db.execute(
        select(DictionaryEntry)
        .where(DictionaryEntry.device_id == device_id)
        .order_by(sort_column)
        .offset(offset)
        .limit(limit)
    )
    rows = result.scalars().all()

    entries = [
        DictionaryEntryResponse(
            id=row.id,
            word=row.word,
            translation=row.translation,
            timesLookedUp=row.times_looked_up,
            createdAt=row.created_at,
        )
        for row in rows
    ]

    return DictionaryListResponse(
        entries=entries,
        total=total,
        limit=limit,
        offset=offset,
    )


@router.delete("/{device_id}/{entry_id}")
async def delete_dictionary_entry(
    device_id: str,
    entry_id: int,
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(DictionaryEntry).where(
            DictionaryEntry.id == entry_id,
            DictionaryEntry.device_id == device_id,
        )
    )
    entry = result.scalar_one_or_none()
    if not entry:
        raise HTTPException(status_code=404, detail="Entry not found")

    await db.delete(entry)
    await db.commit()
    return {"status": "deleted"}
