#!/bin/sh
set -e

echo "Running database seed (upsert)..."
python seed.py --upsert

echo "Starting FastAPI server..."
exec uvicorn main:app --host 0.0.0.0 --port 8000
