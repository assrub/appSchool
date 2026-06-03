#!/usr/bin/env python3
"""Seed the database with content from JSON files. Uses upsert for idempotency."""

import asyncio
import json
import os
import sys

from database import async_session, init_db, engine
from sqlalchemy import select, text
from models import (
    Subject, Topic, TopicTheory, TheorySection, TheoryVideo,
    ExerciseUnit, UnitTheory, UnitTheorySection,
    ExerciseBlock, ExerciseItem, AdminUser, AppUser, UserSubject,
)
from services.auth_service import hash_password

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
CONTENT_DIR = os.path.join(BASE_DIR, "content")

# Check for --upsert flag (default behavior now)
UPSERT_MODE = "--upsert" in sys.argv or True  # Always use upsert


async def seed():
    await init_db()

    async with async_session() as db:

        # Ensure schema is up to date (add columns that may be new)
        migrations = [
            "ALTER TABLE exercise_units ADD COLUMN IF NOT EXISTS is_locked BOOLEAN DEFAULT FALSE",
            "ALTER TABLE exercise_units ADD COLUMN IF NOT EXISTS icon VARCHAR(50) DEFAULT ''",
            "ALTER TABLE exercise_units ADD COLUMN IF NOT EXISTS sound_correct_url VARCHAR(500)",
            "ALTER TABLE exercise_units ADD COLUMN IF NOT EXISTS sound_incorrect_url VARCHAR(500)",
            "ALTER TABLE exercise_blocks ADD COLUMN IF NOT EXISTS icon VARCHAR(50) DEFAULT ''",
            "ALTER TABLE exercise_blocks ADD COLUMN IF NOT EXISTS shuffle BOOLEAN DEFAULT FALSE",
            "ALTER TABLE exercise_items ADD COLUMN IF NOT EXISTS input_mode VARCHAR(10)",
        ]
        for m in migrations:
            try:
                await db.execute(text(m))
                await db.commit()
            except Exception:
                await db.rollback()

        # Create default admin user
        admin_check = await db.execute(select(AdminUser).where(AdminUser.username == "admin"))
        if not admin_check.scalar_one_or_none():
            admin = AdminUser(
                username="admin",
                password_hash=hash_password("admin123"),
            )
            db.add(admin)
            print("Created admin user: admin / admin123")

        # Create default app user (Fausti)
        app_user_check = await db.execute(select(AppUser).where(AppUser.username == "fausti"))
        if not app_user_check.scalar_one_or_none():
            app_user = AppUser(
                username="fausti",
                password_hash=hash_password("123"),
                display_name="Fausti",
            )
            db.add(app_user)
            print("Created app user: fausti / 123")

        await db.flush()

        # Load subjects.json
        subjects_path = os.path.join(CONTENT_DIR, "subjects.json")
        with open(subjects_path, "r") as f:
            subjects_data = json.load(f)

        # Assign fausti to all subjects (get user id first)
        fausti_check = await db.execute(select(AppUser).where(AppUser.username == "fausti"))
        fausti_user = fausti_check.scalar_one_or_none()
        if fausti_user:
            for subj_data in subjects_data.get("subjects", []):
                existing_assignment = await db.execute(
                    select(UserSubject).where(
                        UserSubject.user_id == fausti_user.id,
                        UserSubject.subject_id == subj_data["id"]
                    )
                )
                if not existing_assignment.scalar_one_or_none():
                    db.add(UserSubject(user_id=fausti_user.id, subject_id=subj_data["id"]))
            print("Assigned fausti to subjects")

        for subj_data in subjects_data.get("subjects", []):
            # Upsert subject
            existing_subject = await db.execute(select(Subject).where(Subject.id == subj_data["id"]))
            subject = existing_subject.scalar_one_or_none()
            if subject:
                subject.name = subj_data["name"]
                subject.icon = subj_data.get("icon", "")
                subject.color = subj_data.get("color", "#4CAF50")
                subject.sort_order = subj_data.get("sort_order", 0)
                subject.map_config = subj_data.get("mapConfig")
                subject.is_active = True
            else:
                subject = Subject(
                    id=subj_data["id"],
                    name=subj_data["name"],
                    icon=subj_data.get("icon", ""),
                    color=subj_data.get("color", "#4CAF50"),
                    sort_order=subj_data.get("sort_order", 0),
                    map_config=subj_data.get("mapConfig"),
                )
                db.add(subject)

            # Load topics for this subject
            subject_dir = os.path.join(CONTENT_DIR, subj_data["id"])
            if os.path.exists(subject_dir):
                for filename in sorted(os.listdir(subject_dir)):
                    if filename.endswith(".json"):
                        topic_path = os.path.join(subject_dir, filename)
                        with open(topic_path, "r") as f:
                            topic_data = json.load(f)

                        await _upsert_topic(db, topic_data, subj_data["id"])

        await db.commit()
        print("Database seeded successfully (upsert mode)!")


async def _upsert_topic(db, topic_data: dict, subject_id: str):
    # Upsert topic
    existing_topic = await db.execute(select(Topic).where(Topic.id == topic_data["id"]))
    topic = existing_topic.scalar_one_or_none()
    if topic:
        topic.name = topic_data["name"]
        topic.subject_id = subject_id
        topic.icon = topic_data.get("icon", "")
        topic.difficulty = topic_data.get("difficulty", 1)
        topic.sort_order = topic_data.get("order", 1)
        topic.is_active = True
    else:
        topic = Topic(
            id=topic_data["id"],
            subject_id=subject_id,
            name=topic_data["name"],
            icon=topic_data.get("icon", ""),
            difficulty=topic_data.get("difficulty", 1),
            sort_order=topic_data.get("order", 1),
        )
        db.add(topic)
    await db.flush()

    # Upsert topic-level theory
    theory_data = topic_data.get("theory")
    if theory_data:
        existing_theory = await db.execute(select(TopicTheory).where(TopicTheory.topic_id == topic_data["id"]))
        theory = existing_theory.scalar_one_or_none()
        if theory:
            theory.text = theory_data.get("text", "")
            theory.table_headers = theory_data.get("table", {}).get("headers")
            theory.table_rows = theory_data.get("table", {}).get("rows")
            theory.tips = theory_data.get("tips", [])
        else:
            theory = TopicTheory(
                topic_id=topic_data["id"],
                text=theory_data.get("text", ""),
                table_headers=theory_data.get("table", {}).get("headers"),
                table_rows=theory_data.get("table", {}).get("rows"),
                tips=theory_data.get("tips", []),
            )
            db.add(theory)
        await db.flush()

        # Upsert theory sections - delete old ones and recreate
        existing_sections = await db.execute(
            select(TheorySection).where(TheorySection.theory_id == theory.id)
        )
        for old_section in existing_sections.scalars().all():
            await db.delete(old_section)

        for s_data in theory_data.get("sections", []):
            section = TheorySection(
                theory_id=theory.id,
                title=s_data["title"],
                text=s_data["text"],
                examples=s_data.get("examples", []),
                sort_order=0,
            )
            db.add(section)

    # Upsert exercise units
    for unit_data in topic_data.get("units", []):
        existing_unit = await db.execute(select(ExerciseUnit).where(ExerciseUnit.id == unit_data["id"]))
        unit = existing_unit.scalar_one_or_none()
        if unit:
            unit.title = unit_data["title"]
            unit.topic_id = topic_data["id"]
            unit.exercise_type = unit_data.get("exerciseType", "fill-blank")
            unit.explanation = unit_data.get("explanation", "")
            unit.input_mode = unit_data.get("inputMode", "tap")
        else:
            unit = ExerciseUnit(
                id=unit_data["id"],
                topic_id=topic_data["id"],
                title=unit_data["title"],
                exercise_type=unit_data.get("exerciseType", "fill-blank"),
                explanation=unit_data.get("explanation", ""),
                input_mode=unit_data.get("inputMode", "tap"),
            )
            db.add(unit)
        await db.flush()

        # Upsert unit-level theory
        unit_theory_data = unit_data.get("theory")
        if unit_theory_data:
            existing_ut = await db.execute(select(UnitTheory).where(UnitTheory.unit_id == unit_data["id"]))
            ut = existing_ut.scalar_one_or_none()
            if ut:
                ut.text = unit_theory_data.get("text", "")
                ut.table_headers = unit_theory_data.get("table", {}).get("headers")
                ut.table_rows = unit_theory_data.get("table", {}).get("rows")
                ut.tips = unit_theory_data.get("tips", [])
            else:
                ut = UnitTheory(
                    unit_id=unit_data["id"],
                    text=unit_theory_data.get("text", ""),
                    table_headers=unit_theory_data.get("table", {}).get("headers"),
                    table_rows=unit_theory_data.get("table", {}).get("rows"),
                    tips=unit_theory_data.get("tips", []),
                )
                db.add(ut)
            await db.flush()

            # Delete old sections and recreate
            existing_ut_sections = await db.execute(
                select(UnitTheorySection).where(UnitTheorySection.unit_theory_id == ut.id)
            )
            for old_s in existing_ut_sections.scalars().all():
                await db.delete(old_s)

            for s_data in unit_theory_data.get("sections", []):
                section = UnitTheorySection(
                    unit_theory_id=ut.id,
                    title=s_data["title"],
                    text=s_data["text"],
                    examples=s_data.get("examples", []),
                )
                db.add(section)

        # Upsert exercise blocks and items
        for block_idx, block_data in enumerate(unit_data.get("blocks", [])):
            # Try to find existing block by unit_id + sort_order
            existing_block = await db.execute(
                select(ExerciseBlock).where(
                    ExerciseBlock.unit_id == unit_data["id"],
                    ExerciseBlock.sort_order == block_idx
                ).order_by(ExerciseBlock.id)
            )
            all_blocks = existing_block.scalars().all()
            # Keep first, delete duplicates and their items first
            block = all_blocks[0] if all_blocks else None
            if len(all_blocks) > 1:
                for dup in all_blocks[1:]:
                    # Delete items of duplicate block first
                    dup_items = await db.execute(
                        select(ExerciseItem).where(ExerciseItem.block_id == dup.id)
                    )
                    for item in dup_items.scalars().all():
                        await db.delete(item)
                    await db.flush()
                    await db.delete(dup)
                await db.flush()

            if block:
                block.title = block_data["title"]
            else:
                block = ExerciseBlock(
                    unit_id=unit_data["id"],
                    title=block_data["title"],
                    sort_order=block_idx,
                )
                db.add(block)
            await db.flush()

            # Delete old items and recreate for this block
            existing_items = await db.execute(
                select(ExerciseItem).where(ExerciseItem.block_id == block.id)
            )
            for old_item in existing_items.scalars().all():
                await db.delete(old_item)

            for item_idx, item_data in enumerate(block_data.get("items", [])):
                item = ExerciseItem(
                    block_id=block.id,
                    item_type=item_data.get("type", "fill-blank"),
                    sentence=item_data.get("sentence", ""),
                    answer=item_data.get("answer", ""),
                    answers=item_data.get("answers"),
                    hint=item_data.get("hint"),
                    question=item_data.get("question"),
                    options=item_data.get("options"),
                    words=item_data.get("words"),
                    correct_order=item_data.get("correctOrder"),
                    audio_url=item_data.get("audioUrl"),
                    pairs=item_data.get("pairs"),
                    is_correct_boolean=item_data.get("isCorrect"),
                    input_mode=item_data.get("input_mode"),
                    sort_order=item_idx,
                )
                db.add(item)


if __name__ == "__main__":
    asyncio.run(seed())
