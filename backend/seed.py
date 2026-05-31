#!/usr/bin/env python3
"""Seed the database with content from JSON files."""

import asyncio
import json
import os

from database import async_session, init_db, engine
from sqlalchemy import select, text, delete as sqldelete
from models import (
    Subject, Topic, TopicTheory, TheorySection, TheoryVideo,
    ExerciseUnit, UnitTheory, UnitTheorySection,
    ExerciseBlock, ExerciseItem, AdminUser,
)
from services.auth_service import hash_password

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
CONTENT_DIR = os.path.join(BASE_DIR, "content")


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
            "ALTER TABLE exercise_items ADD COLUMN IF NOT EXISTS answers JSONB",
            "ALTER TABLE exercise_items ADD COLUMN IF NOT EXISTS input_mode VARCHAR(10)",
        ]
        for m in migrations:
            try:
                await db.execute(text(m))
            except Exception:
                pass
        await db.commit()

        # Remove old data and re-seed
        await db.execute(sqldelete(ExerciseItem))
        await db.execute(sqldelete(ExerciseBlock))
        await db.execute(sqldelete(UnitTheorySection))
        await db.execute(sqldelete(UnitTheory))
        await db.execute(sqldelete(ExerciseUnit))
        await db.execute(sqldelete(TheoryVideo))
        await db.execute(sqldelete(TheorySection))
        await db.execute(sqldelete(TopicTheory))
        await db.execute(sqldelete(Topic))
        await db.execute(sqldelete(Subject))
        await db.commit()

        # Create default admin user
        admin_check = await db.execute(select(AdminUser).where(AdminUser.username == "admin"))
        if not admin_check.scalar_one_or_none():
            admin = AdminUser(
                username="admin",
                password_hash=hash_password("admin123"),
            )
            db.add(admin)
            print("Created admin user: admin / admin123")

        # Load subjects.json
        subjects_path = os.path.join(CONTENT_DIR, "subjects.json")
        with open(subjects_path, "r") as f:
            subjects_data = json.load(f)

        for subj_data in subjects_data.get("subjects", []):
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

                        await _seed_topic(db, topic_data, subj_data["id"])

        await db.commit()
        print("Database seeded successfully!")


async def _seed_topic(db, topic_data: dict, subject_id: str):
    topic = Topic(
        id=topic_data["id"],
        subject_id=subject_id,
        name=topic_data["name"],
        icon=topic_data.get("icon", ""),
        difficulty=topic_data.get("difficulty", 1),
        sort_order=topic_data.get("order", 1),
    )
    db.add(topic)

    # Seed topic-level theory
    theory_data = topic_data.get("theory")
    if theory_data:
        theory = TopicTheory(
            topic_id=topic_data["id"],
            text=theory_data.get("text", ""),
            table_headers=theory_data.get("table", {}).get("headers"),
            table_rows=theory_data.get("table", {}).get("rows"),
            tips=theory_data.get("tips", []),
        )
        db.add(theory)
        await db.flush()

        # Seed theory sections
        for s_data in theory_data.get("sections", []):
            section = TheorySection(
                theory_id=theory.id,
                title=s_data["title"],
                text=s_data["text"],
                examples=s_data.get("examples", []),
                sort_order=0,
            )
            db.add(section)

    # Seed exercise units
    for unit_data in topic_data.get("units", []):
        unit = ExerciseUnit(
            id=unit_data["id"],
            topic_id=topic_data["id"],
            title=unit_data["title"],
            exercise_type=unit_data.get("exerciseType", "fill-blank"),
            explanation=unit_data.get("explanation", ""),
            input_mode=unit_data.get("inputMode", "tap"),
        )
        db.add(unit)

        # Seed unit-level theory
        unit_theory_data = unit_data.get("theory")
        if unit_theory_data:
            ut = UnitTheory(
                unit_id=unit_data["id"],
                text=unit_theory_data.get("text", ""),
                table_headers=unit_theory_data.get("table", {}).get("headers"),
                table_rows=unit_theory_data.get("table", {}).get("rows"),
                tips=unit_theory_data.get("tips", []),
            )
            db.add(ut)
            await db.flush()

            for s_data in unit_theory_data.get("sections", []):
                section = UnitTheorySection(
                    unit_theory_id=ut.id,
                    title=s_data["title"],
                    text=s_data["text"],
                    examples=s_data.get("examples", []),
                )
                db.add(section)

        # Seed exercise blocks and items
        for block_data in unit_data.get("blocks", []):
            block = ExerciseBlock(
                unit_id=unit_data["id"],
                title=block_data["title"],
            )
            db.add(block)
            await db.flush()

            for item_data in block_data.get("items", []):
                item = ExerciseItem(
                    block_id=block.id,
                    item_type=item_data.get("type", "fill-blank"),
                    sentence=item_data.get("sentence", ""),
                    answer=item_data.get("answer", ""),
                    hint=item_data.get("hint"),
                    question=item_data.get("question"),
                    options=item_data.get("options"),
                    words=item_data.get("words"),
                    correct_order=item_data.get("correctOrder"),
                    audio_url=item_data.get("audioUrl"),
                    pairs=item_data.get("pairs"),
                    is_correct_boolean=item_data.get("isCorrect"),
                )
                db.add(item)


if __name__ == "__main__":
    asyncio.run(seed())
