# AppEnglish — Especificación del Backend

## Stack

- **Python 3.11** + **FastAPI** (async)
- **Edge TTS** para síntesis de voz (gratuito, calidad natural)
- **MyMemory API** para traducción EN→ES (gratuita, 5000 palabras/día)
- **PostgreSQL** para datos persistentes (contenido, progreso, diccionario)
- **Docker** para deploy en VPS

---

## 1. Estructura del Proyecto

```
backend/
├── main.py                    # FastAPI app entrypoint
├── config.py                  # Settings (DB URL, API keys, etc.)
├── database.py                # SQLAlchemy engine + session
├── models.py                  # ORM models
├── routers/
│   ├── content.py             # /api/v1/content/*
│   ├── tts.py                 # /api/v1/tts
│   ├── translate.py           # /api/v1/translate
│   └── progress.py            # /api/v1/progress/*
├── services/
│   ├── edge_tts_service.py    # Wrapper de edge-tts
│   └── translator_service.py  # MyMemory API + fallback + caché
├── schemas/
│   ├── content.py             # Pydantic schemas para contenido
│   ├── tts.py                 # Schemas para TTS
│   ├── translate.py           # Schemas para traducción
│   └── progress.py            # Schemas para progreso
├── content/                   # JSON files con el contenido educativo
│   ├── subjects.json          # Lista maestra de materias
│   └── english/
│       └── verb-to-be.json    # Contenido completo del verbo to be
├── requirements.txt
├── Dockerfile
└── docker-compose.yml         # FastAPI + PostgreSQL
```

---

## 2. Modelos de Base de Datos

### Subject (materia)
```sql
CREATE TABLE subjects (
    id          VARCHAR(50) PRIMARY KEY,   -- "english", "math"
    name        VARCHAR(100) NOT NULL,     -- "Inglés", "Matemática"
    icon        VARCHAR(50),               -- "📚"
    color       VARCHAR(7),                -- "#4CAF50"
    map_config  JSONB,                     -- Config del mapa animado (futuro)
    created_at  TIMESTAMP DEFAULT NOW()
);
```

### Topic (tema)
```sql
CREATE TABLE topics (
    id          VARCHAR(100) PRIMARY KEY,  -- "verb-to-be"
    subject_id  VARCHAR(50) REFERENCES subjects(id),
    name        VARCHAR(200) NOT NULL,     -- "Verbo To Be"
    sort_order  INTEGER DEFAULT 0,
    difficulty  INTEGER DEFAULT 1,         -- 1 a 5
    icon        VARCHAR(50),
    is_active   BOOLEAN DEFAULT TRUE,
    content     JSONB NOT NULL,            -- Teoría, tabla, tips, units completas
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);
```

### Progress (progreso del alumno)
```sql
CREATE TABLE progress (
    id              SERIAL PRIMARY KEY,
    device_id       VARCHAR(100) NOT NULL,  -- ID único del dispositivo
    topic_id        VARCHAR(100) REFERENCES topics(id),
    unit_id         VARCHAR(100),
    completed       BOOLEAN DEFAULT FALSE,
    score           INTEGER DEFAULT 0,      -- Puntos obtenidos
    total_items     INTEGER DEFAULT 0,      -- Total de ítems
    completed_items INTEGER DEFAULT 0,      -- Ítems completados
    test_score      INTEGER,                -- Puntuación del test final
    started_at      TIMESTAMP DEFAULT NOW(),
    completed_at    TIMESTAMP,
    UNIQUE(device_id, topic_id, unit_id)
);
```

### DictionaryEntry (diccionario personal)
```sql
CREATE TABLE dictionary_entries (
    id              SERIAL PRIMARY KEY,
    device_id       VARCHAR(100) NOT NULL,
    word            VARCHAR(255) NOT NULL,
    translation     VARCHAR(500) NOT NULL,
    source_lang     VARCHAR(10) DEFAULT 'en',
    target_lang     VARCHAR(10) DEFAULT 'es',
    times_looked_up INTEGER DEFAULT 1,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE(device_id, word)
);
```

### TranslationCache (caché de traducciones para no re-consultar API)
```sql
CREATE TABLE translation_cache (
    id          SERIAL PRIMARY KEY,
    text_hash   VARCHAR(64) UNIQUE NOT NULL,  -- SHA256 del texto
    source_text TEXT NOT NULL,
    source_lang VARCHAR(10) NOT NULL,
    target_lang VARCHAR(10) NOT NULL,
    translation TEXT NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW()
);
```

### TtsCache (caché de audios generados)
```sql
CREATE TABLE tts_cache (
    id          SERIAL PRIMARY KEY,
    text_hash   VARCHAR(64) UNIQUE NOT NULL,  -- SHA256 del texto
    text        TEXT NOT NULL,
    voice       VARCHAR(100) NOT NULL,
    audio_data  BYTEA NOT NULL,               -- MP3 binario
    created_at  TIMESTAMP DEFAULT NOW()
);
```

---

## 3. Endpoints de la API

Base URL: `https://<vps-ip>:8000/api/v1`

### 3.1. Contenido

#### `GET /content/subjects`
Lista todas las materias disponibles.

**Response 200:**
```json
{
  "subjects": [
    {
      "id": "english",
      "name": "Inglés",
      "icon": "📚",
      "color": "#4CAF50",
      "topicsCount": 1,
      "topics": [
        {
          "id": "verb-to-be",
          "name": "Verbo To Be",
          "order": 1,
          "difficulty": 1,
          "icon": "📝",
          "isLocked": false,
          "progress": {
            "completedUnits": 0,
            "totalUnits": 4,
            "percentComplete": 0
          }
        }
      ]
    }
  ]
}
```

#### `GET /content/topics/{topicId}`
Contenido completo de un tema (teoría + ejercicios).

**Path param:** `topicId` = `"verb-to-be"`

**Query param (opcional):** `deviceId` — para devolver progreso embebido

**Response 200:**
```json
{
  "id": "verb-to-be",
  "name": "Verbo To Be",
  "subjectId": "english",
  "order": 1,
  "difficulty": 1,
  "icon": "📝",
  "theory": {
    "text": "En inglés usamos AM - IS - ARE para decir soy, estoy, es, eres, somos, están.",
    "table": {
      "headers": ["Si el sujeto es...", "Usa...", "Ejemplo"],
      "rows": [
        ["I (yo)", "am", "I am happy"],
        ["He / She / It (uno solo)", "is", "She is a doctor"],
        ["You / We / They (varios o 'tú')", "are", "We are friends"],
        ["Un nombre solo: Claudia", "is", "Claudia is smart"],
        ["Dos nombres: David and Dagui", "are", "They are outside"]
      ]
    },
    "tips": [
      {"emoji": "👑", "text": "AM → Solo YO. Como un rey solitario"},
      {"emoji": "👉", "text": "IS → Apuntas con el dedo a UNO → ¡eso IS!"},
      {"emoji": "🚢", "text": "ARE → Una ARmada de MUCHOS barcos"}
    ]
  },
  "units": [
    {
      "id": "affirmative",
      "title": "Afirmativo (am / is / are)",
      "exerciseType": "fill-blank",
      "explanation": "Completa con am, is o are según corresponda.",
      "progress": { "completedItems": 0, "totalItems": 68 },
      "blocks": [
        {
          "title": "PASO 1: Pronombres personales y familia cercana",
          "items": [
            { "sentence": "I ______ a happy student.", "answer": "am", "hint": "AM → Solo YO" },
            { "sentence": "You ______ my best friend.", "answer": "are", "hint": "ARE → Para you" },
            { "sentence": "He ______ in the office.", "answer": "is", "hint": "IS → Él es uno" }
          ]
        },
        {
          "title": "PASO 2: Entorno, mascotas y plurales mixtos",
          "items": [ ... ]
        }
      ]
    },
    {
      "id": "negative",
      "title": "Negativo (am not / isn't / aren't)",
      "exerciseType": "fill-blank",
      "explanation": "Para decir NO SOY, NO ESTOY... solo añade NOT.",
      "progress": { "completedItems": 0, "totalItems": 46 },
      "blocks": [ ... ]
    },
    {
      "id": "interrogative",
      "title": "Interrogativo (Am / Is / Are ...?)",
      "exerciseType": "fill-blank",
      "explanation": "Para hacer preguntas, cambias de lugar el verbo y el sujeto.",
      "progress": { "completedItems": 0, "totalItems": 48 },
      "blocks": [ ... ]
    },
    {
      "id": "short-answers",
      "title": "Respuestas cortas (Yes/No)",
      "exerciseType": "fill-blank",
      "explanation": "Responde sin repetir toda la frase.",
      "progress": { "completedItems": 0, "totalItems": 40 },
      "blocks": [ ... ]
    }
  ],
  "testConfig": {
    "totalQuestions": 20,
    "shuffle": true,
    "includeUnits": ["affirmative", "negative", "interrogative", "short-answers"]
  }
}
```

#### `GET /content/topics/{topicId}/test`
Genera un test final mezclado.

**Query params:**
- `deviceId` (string) — para no repetir preguntas ya falladas
- `count` (int, default 20) — cantidad de preguntas

**Response 200:**
```json
{
  "topicId": "verb-to-be",
  "questions": [
    {
      "id": "q_001",
      "unitId": "negative",
      "sentence": "She ______ sad today.",
      "answer": "isn't",
      "alternatives": ["am not", "isn't", "aren't"],
      "hint": "She → isn't"
    },
    ...
  ]
}
```

---

### 3.2. Text-to-Speech

#### `POST /tts`
Genera audio MP3 desde texto usando Edge TTS.

**Request:**
```json
{
  "text": "I am a happy student",
  "voice": "en-US-ChristopherNeural",
  "rate": "+0%",
  "pitch": "+0Hz"
}
```

**Response 200:** `audio/mpeg` (stream binario directamente)

**Response headers:**
```
Content-Type: audio/mpeg
Content-Length: 12345
X-Cache: HIT | MISS
```

**Caché:** El backend calcula SHA256 de `text + voice` y devuelve desde `tts_cache` si existe.

**Voces recomendadas:**
| Voice ID | Género | Estilo |
|----------|--------|--------|
| `en-US-ChristopherNeural` | Masculino | Natural, cálido |
| `en-US-JennyNeural` | Femenino | Clara, natural |
| `en-US-GuyNeural` | Masculino | Profesional |
| `en-US-AriaNeural` | Femenino | Expresiva, noticias |

#### `GET /tts/voices`
Lista las voces disponibles en Edge TTS para inglés.

**Response 200:**
```json
{
  "voices": [
    { "id": "en-US-ChristopherNeural", "gender": "Male", "locale": "en-US" },
    { "id": "en-US-JennyNeural", "gender": "Female", "locale": "en-US" },
    { "id": "en-US-GuyNeural", "gender": "Male", "locale": "en-US" },
    { "id": "en-US-AriaNeural", "gender": "Female", "locale": "en-US" },
    { "id": "en-GB-RyanNeural", "gender": "Male", "locale": "en-GB" },
    { "id": "en-GB-SoniaNeural", "gender": "Female", "locale": "en-GB" }
  ]
}
```

---

### 3.3. Traducción

#### `POST /translate`
Traduce texto usando MyMemory API con caché.

**Request:**
```json
{
  "text": "beautiful",
  "sourceLang": "en",
  "targetLang": "es"
}
```

**Response 200:**
```json
{
  "text": "beautiful",
  "translation": "hermosa",
  "sourceLang": "en",
  "targetLang": "es",
  "confidence": 0.98
}
```

**Caché:** Calcula SHA256 del texto + idiomas. Si existe en `translation_cache`, devuelve de ahí sin llamar a MyMemory.

**Rate limit:** 5000 palabras/día en MyMemory. El backend trackea uso diario.

---

### 3.4. Progreso y Sincronización

#### `POST /progress/sync`
Sincroniza el progreso local del dispositivo con el servidor (backup).

**Request:**
```json
{
  "deviceId": "android-abc123",
  "progress": [
    {
      "topicId": "verb-to-be",
      "unitId": "affirmative",
      "completed": true,
      "score": 68,
      "totalItems": 68,
      "completedItems": 68,
      "testScore": null,
      "completedAt": "2026-05-30T15:30:00Z"
    },
    {
      "topicId": "verb-to-be",
      "unitId": "negative",
      "completed": false,
      "score": 20,
      "totalItems": 46,
      "completedItems": 20,
      "testScore": null,
      "completedAt": null
    }
  ]
}
```

**Response 200:**
```json
{
  "status": "ok",
  "syncedAt": "2026-05-30T15:31:00Z",
  "syncedCount": 2
}
```

#### `GET /progress/{deviceId}`
Obtiene el progreso guardado de un dispositivo (para restaurar).

**Response 200:**
```json
{
  "deviceId": "android-abc123",
  "subjects": [
    {
      "subjectId": "english",
      "topics": [
        {
          "topicId": "verb-to-be",
          "units": [
            { "unitId": "affirmative", "completed": true, "score": 68 },
            { "unitId": "negative", "completed": false, "score": 20 }
          ],
          "testScore": null
        }
      ]
    }
  ],
  "lastSyncedAt": "2026-05-30T15:31:00Z"
}
```

---

### 3.5. Diccionario

#### `POST /dictionary`
Guarda una palabra en el diccionario del alumno.

**Request:**
```json
{
  "deviceId": "android-abc123",
  "word": "beautiful",
  "translation": "hermosa",
  "sourceLang": "en",
  "targetLang": "es"
}
```

**Response 200:**
```json
{
  "id": 42,
  "word": "beautiful",
  "translation": "hermosa",
  "timesLookedUp": 1,
  "createdAt": "2026-05-30T15:30:00Z"
}
```

#### `GET /dictionary/{deviceId}`
Lista el diccionario del alumno.

**Query params:**
- `sortBy` = `"date"` | `"alphabetical"` | `"frequency"` (default: `"date"`)
- `order` = `"asc"` | `"desc"` (default: `"desc"`)
- `limit` (default: 50)
- `offset` (default: 0)

**Response 200:**
```json
{
  "entries": [
    {
      "id": 42,
      "word": "beautiful",
      "translation": "hermosa",
      "timesLookedUp": 3,
      "createdAt": "2026-05-30T15:30:00Z"
    },
    {
      "id": 41,
      "word": "library",
      "translation": "biblioteca",
      "timesLookedUp": 1,
      "createdAt": "2026-05-29T10:00:00Z"
    }
  ],
  "total": 15,
  "limit": 50,
  "offset": 0
}
```

#### `DELETE /dictionary/{deviceId}/{entryId}`
Elimina una entrada del diccionario.

**Response 200:**
```json
{ "status": "deleted" }
```

---

## 4. Servicio de Edge TTS

Implementación en `services/edge_tts_service.py`:

```python
import edge_tts
import hashlib
import io
from models import TtsCache
from sqlalchemy.ext.asyncio import AsyncSession

VOICES = {
    "male": "en-US-ChristopherNeural",
    "female": "en-US-JennyNeural",
}

async def generate_tts(text: str, voice: str, db: AsyncSession) -> bytes:
    text_hash = hashlib.sha256(f"{text}|{voice}".encode()).hexdigest()

    # Check cache
    cached = await db.get(TtsCache, text_hash)
    if cached:
        return cached.audio_data

    # Generate with Edge TTS
    communicate = edge_tts.Communicate(text, voice)
    audio_buffer = io.BytesIO()
    async for chunk in communicate.stream():
        if chunk["type"] == "audio":
            audio_buffer.write(chunk["data"])

    audio_data = audio_buffer.getvalue()

    # Save to cache
    db.add(TtsCache(text_hash=text_hash, text=text, voice=voice, audio_data=audio_data))
    await db.commit()

    return audio_data
```

---

## 5. Servicio de Traducción

Implementación en `services/translator_service.py`:

```python
import httpx
import hashlib
from models import TranslationCache
from sqlalchemy.ext.asyncio import AsyncSession

MYMEMORY_URL = "https://api.mymemory.translated.net/get"

async def translate(
    text: str,
    source_lang: str = "en",
    target_lang: str = "es",
    db: AsyncSession = None
) -> dict:
    text_hash = hashlib.sha256(f"{text}|{source_lang}|{target_lang}".encode()).hexdigest()

    # Check cache
    if db:
        cached = await db.get(TranslationCache, text_hash)
        if cached:
            return {
                "text": text,
                "translation": cached.translation,
                "sourceLang": source_lang,
                "targetLang": target_lang,
                "confidence": 1.0
            }

    # Call MyMemory API
    async with httpx.AsyncClient() as client:
        response = await client.get(MYMEMORY_URL, params={
            "q": text,
            "langpair": f"{source_lang}|{target_lang}"
        })
        data = response.json()

    translation = data["responseData"]["translatedText"]
    confidence = data["responseData"].get("match", 0)

    # Save to cache
    if db:
        db.add(TranslationCache(
            text_hash=text_hash,
            source_text=text,
            source_lang=source_lang,
            target_lang=target_lang,
            translation=translation
        ))
        await db.commit()

    return {
        "text": text,
        "translation": translation,
        "sourceLang": source_lang,
        "targetLang": target_lang,
        "confidence": confidence
    }
```

---

## 6. Deploy en VPS

### Dockerfile
```dockerfile
FROM python:3.11-slim

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc libpq-dev && \
    rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

### docker-compose.yml
```yaml
version: "3.8"

services:
  api:
    build: .
    ports:
      - "8000:8000"
    environment:
      - DATABASE_URL=postgresql+asyncpg://user:pass@db:5432/appenglish
      - MYMEMORY_EMAIL=tu-email@gmail.com  # Opcional, aumenta rate limit
    depends_on:
      - db
    restart: unless-stopped

  db:
    image: postgres:16-alpine
    environment:
      - POSTGRES_USER=user
      - POSTGRES_PASSWORD=pass
      - POSTGRES_DB=appenglish
    volumes:
      - pgdata:/var/lib/postgresql/data
    restart: unless-stopped

volumes:
  pgdata:
```

### Comandos de deploy
```bash
# En el VPS
git clone <repo> /opt/appenglish
cd /opt/appenglish/backend
docker compose up -d --build

# Verificar
curl http://localhost:8000/api/v1/content/subjects
```

---

## 7. Seed de Datos Iniciales

### subjects.json
```json
{
  "subjects": [
    {
      "id": "english",
      "name": "Inglés",
      "icon": "📚",
      "color": "#4CAF50",
      "mapConfig": null
    }
  ]
}
```

### Contenido de verb-to-be.json

Archivo individual en `content/english/verb-to-be.json` con la estructura completa del tema (teoría + 4 unidades + testConfig) según lo especificado en el endpoint `GET /content/topics/{topicId}`.

Los ejercicios se cargan directamente desde los archivos `Ejercicios_1` a `Ejercicios_4` de la carpeta AppEnglish, parseando cada línea y extrayendo:
- Frase con `______` → `sentence`
- Respuesta correcta (am/is/are/am not/isn't/aren't según el tipo) → `answer`
- Contexto del bloque y paso → `block.title`

---

## 8. Roadmap del Backend

| Prioridad | Tarea | Estado |
|-----------|-------|--------|
| P0 | FastAPI base + endpoint `/content/topics/{id}` | MVP Día 1 |
| P0 | Endpoint `/tts` con Edge TTS + caché | MVP Día 1 |
| P0 | Endpoint `/translate` con MyMemory + caché | MVP Día 1 |
| P0 | Seed de datos: verbo to be completo | MVP Día 1 |
| P1 | Endpoint `/progress/sync` para backup | MVP Día 2 |
| P1 | Endpoint `/dictionary` CRUD | MVP Día 2 |
| P1 | PostgreSQL + migraciones | MVP Día 2 |
| P1 | Docker + deploy en VPS | MVP Día 2 |
| P2 | Endpoint `/content/topics/{id}/test` (generar test) | Día 2 |
| P2 | Admin panel simple para editar contenido | Fase 3 |
| P3 | Auth (deviceId → login con email) | Fase 4 |
| P3 | Rate limiting y monitoreo | Fase 4 |
