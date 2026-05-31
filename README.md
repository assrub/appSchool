# AppSchool

App educativa para celular Android, pensada para aprendizaje escalable de múltiples materias.

## Estructura

```
appSchool/
├── backend/          # API REST (Python + FastAPI + Docker)
├── app-android/      # App Android (Kotlin + Jetpack Compose)
├── admin-panel/      # Panel de administración web (Vue.js + Docker)
├── PLAN.md           # Plan de desarrollo completo
├── BACKEND.md        # Especificación del backend
└── Ejercicios_*      # Archivos de ejercicios (verb to be)
```

## Stack

| Componente | Tecnología |
|-----------|-----------|
| App Android | Kotlin + Jetpack Compose + Hilt + Retrofit + Room |
| Backend API | Python 3.11 + FastAPI + SQLAlchemy + PostgreSQL |
| TTS | Edge TTS (Microsoft, gratuito) vía backend |
| Traducción | MyMemory API (gratuita) vía backend |
| Admin Panel | Vue.js 3 + Vuetify 3 (próxima fase) |

## Quick Start

### Backend

```bash
cd backend
pip install -r requirements.txt
uvicorn main:app --reload
```

### Android App

Abrir `app-android/` en Android Studio y build.

### VPS Deploy

```bash
cd backend
docker compose up -d --build
```

## Repositorio

https://github.com/assrub/appSchool.git
