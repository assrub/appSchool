import json
import os
import random

from fastapi import APIRouter, HTTPException, Query

from config import CONTENT_DIR
from schemas.content import (
    SubjectsResponse,
    SubjectResponse,
    TopicResponse,
    TopicSummary,
    TopicProgress,
    TestResponse,
    TestQuestion,
)

router = APIRouter()


def _load_subjects() -> list[dict]:
    path = os.path.join(CONTENT_DIR, "subjects.json")
    if not os.path.exists(path):
        return []
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    return data.get("subjects", [])


def _load_topic_content(topic_id: str) -> dict | None:
    for subject_data in _load_subjects():
        subject_id = subject_data["id"]
        path = os.path.join(CONTENT_DIR, subject_id, f"{topic_id}.json")
        if os.path.exists(path):
            with open(path, "r", encoding="utf-8") as f:
                return json.load(f)
    return None


def _get_all_topics() -> list[dict]:
    topics = []
    for subject_data in _load_subjects():
        subject_id = subject_data["id"]
        subject_dir = os.path.join(CONTENT_DIR, subject_id)
        if os.path.exists(subject_dir):
            for filename in sorted(os.listdir(subject_dir)):
                if filename.endswith(".json"):
                    filepath = os.path.join(subject_dir, filename)
                    with open(filepath, "r", encoding="utf-8") as f:
                        topic = json.load(f)
                        topic["subjectId"] = subject_id
                        topics.append(topic)
    return topics


@router.get("/subjects", response_model=SubjectsResponse)
async def get_subjects():
    subjects_data = _load_subjects()
    all_topics = _get_all_topics()

    response_subjects = []
    for subj in subjects_data:
        subject_topics = [
            t for t in all_topics if t.get("subjectId") == subj["id"]
        ]
        subject_topics.sort(key=lambda t: t.get("order", 0))

        topic_summaries = []
        for topic in subject_topics:
            units = topic.get("units", [])
            total_units = len(units)
            topic_summaries.append(
                TopicSummary(
                    id=topic["id"],
                    name=topic["name"],
                    order=topic.get("order", 1),
                    difficulty=topic.get("difficulty", 1),
                    icon=topic.get("icon", ""),
                    isLocked=False,
                    progress=TopicProgress(
                        completedUnits=0,
                        totalUnits=total_units,
                        percentComplete=0.0,
                    ),
                )
            )

        response_subjects.append(
            SubjectResponse(
                id=subj["id"],
                name=subj["name"],
                icon=subj.get("icon", ""),
                color=subj.get("color", "#4CAF50"),
                topicsCount=len(topic_summaries),
                topics=topic_summaries,
            )
        )

    return SubjectsResponse(subjects=response_subjects)


@router.get("/topics/{topic_id}", response_model=TopicResponse)
async def get_topic(topic_id: str):
    topic = _load_topic_content(topic_id)
    if topic is None:
        raise HTTPException(status_code=404, detail="Topic not found")

    subject_id = topic.get("subjectId", "")
    return TopicResponse(**topic)


@router.get("/topics/{topic_id}/test", response_model=TestResponse)
async def get_test(
    topic_id: str,
    count: int = Query(default=20, ge=1, le=100),
):
    topic = _load_topic_content(topic_id)
    if topic is None:
        raise HTTPException(status_code=404, detail="Topic not found")

    all_items = []
    unit_ids_included = set()

    for unit in topic.get("units", []):
        unit_id = unit["id"]
        for block in unit.get("blocks", []):
            for idx, item in enumerate(block.get("items", [])):
                all_items.append({
                    "id": f"{unit_id}_b{idx}",
                    "unitId": unit_id,
                    "sentence": item["sentence"],
                    "answer": item["answer"],
                    "hint": item.get("hint"),
                })
                unit_ids_included.add(unit_id)

    if count > len(all_items):
        count = len(all_items)

    selected = random.sample(all_items, count)

    questions = [
        TestQuestion(**q) for q in selected
    ]

    return TestResponse(
        topicId=topic_id,
        questions=questions,
    )
