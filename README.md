# AppSchool - Plataforma Educativa de Ingles

Plataforma de aprendizaje con ejercicios interactivos, feedback por sonido, drag & drop, traduccion en tiempo real, TTS y panel admin.

**Stack:** Python 3.11 (FastAPI) + PostgreSQL/SQLite + Android (Kotlin, Jetpack Compose) + Vue 3 (Vuetify)

**URL produccion:** http://2.25.142.139:9000 (admin) | http://2.25.142.139:8000 (API)

---

## Flujo de Actualizacion Automatica

```
┌──────────┐    clona/copia    ┌──────────┐   git push    ┌──────────┐
│  GitHub  │◄────────────────│  VPS      │◄──────────── │ Maquina  │
│ (repo)   │   git pull       │ (Docker) │ commit+push  │ local    │
└──────────┘                  └──────────┘              └──────────┘
                                   │
                             docker compose
                             down && up -d --build
                                   │
                             ┌─────▼─────┐
                             │  FastAPI  │─ GET /api/v1/version
                             │  :8000    │─ GET /uploads/app-release.apk
                             └───────────┘
                                   │
                             ┌─────▼─────────┐
                             │  App Android  │─ UpdateManager.checkForUpdate()
                             │               │─ Instala APK via FileProvider
                             └───────────────┘
```

1. Desarrollador compila con `./tools/bump-and-build.sh --push` (auto-incrementa version)
2. `git push` sube el codigo + APK a GitHub
3. En el VPS: `git pull` descarga los cambios
4. `docker compose down && docker compose up -d --build` reconstruye los contenedores
5. Backend sirve `GET /api/v1/version` con el nuevo `versionCode`
6. App Android (Settings > "Buscar actualizacion") compara versiones
7. Si `serverVersion > currentVersion`, descarga e instala el nuevo APK

---

## Auto-Version (tools/bump-and-build.sh)

El script lee `version.properties` (archivo unico para version), incrementa `versionCode`, recalcula `versionName`, compila el APK y opcionalmente commitea/pushea.

```bash
# Solo compilar con bump automatico (sin commit/push)
./tools/bump-and-build.sh

# Compilar + commit + push
./tools/bump-and-build.sh --push
```

**Estructura de version.properties:**
```
versionCode=29
versionName=3.2.0
```

Tanto `build.gradle.kts` como `backend/main.py` leen de este archivo. **No hay que modificar version manualmente en ningun lado.**

---

## Arquitectura del Proyecto

```
AppEnglish/
├── version.properties          # Version unica (Android + Backend)
├── app-android/                # App Android (Kotlin + Compose)
├── backend/                    # FastAPI + PostgreSQL/SQLite
├── admin-panel/                # Panel admin (Vue 3 + Vuetify + TipTap)
├── tools/                      # JDK, Android SDK, Node.js + scripts
├── PLAN.md                     # Plan original del proyecto
├── DEPLOY.md                   # Guia de deploy
└── README.md                   # Este archivo
```

---

## Backend (FastAPI)

### Arranque local (desarrollo)

```bash
cd backend
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

### Docker (produccion)

```bash
cd backend
docker compose down && docker compose up -d --build
```

Esto levanta 3 contenedores:
- **api** (puerto 8000): FastAPI
- **admin** (puerto 9000): Panel admin (Vue + Nginx)
- **db**: PostgreSQL 16

### Base de Datos

En desarrollo usa SQLite (`appenglish.db`). En produccion usa PostgreSQL. Las tablas se crean automaticamente al iniciar.

### Tablas Principales

| Tabla | Descripcion |
|-------|------------|
| `subjects` | Materias (ej: Ingles) |
| `topics` | Temas (ej: Verbo To Be) |
| `topic_theory` / `theory_sections` | Teoria del tema |
| `theory_videos` | Videos de YouTube asociados |
| `exercise_units` | Unidades (ej: Afirmativo) |
| `unit_theory` / `unit_theory_sections` | Teoria de la unidad |
| `exercise_blocks` | Bloques de ejercicios |
| `block_theory` / `block_theory_sections` | Teoria del bloque |
| `exercise_items` | Items individuales de ejercicio |
| `progress` | Progreso por usuario/unidad |
| `answer_history` | Historial de respuestas |
| `app_users` | Usuarios alumnos (Android) |
| `admin_users` | Usuarios admin (panel web) |
| `user_subjects` | Asignacion usuario <-> materia |
| `dictionary_entries` | Diccionario personal |
| `translation_cache` | Cache de traducciones |
| `tts_cache` | Cache de audio TTS |
| `study_sessions` | Sesiones de estudio |
| `audio_files` | Archivos de audio TTS |

---

## API Endpoints

**Base URL:** http://2.25.142.139:8000/api/v1/

### Publicos (sin auth)

| Metodo | Ruta | Descripcion |
|--------|------|------------|
| GET | `/version` | Version (versionCode, versionName, apkUrl) |
| GET | `/content/subjects` | Materias con temas y progreso |
| GET | `/content/topics/{id}` | Tema completo con teoria, unidades, bloques, items |
| GET | `/content/topics/{id}/test` | Test final aleatorio |
| POST | `/auth/app/login` | Login de alumno (username/password -> JWT) |
| GET | `/auth/app/me` | Datos del alumno autenticado |
| POST | `/progress/sync` | Sincronizar progreso |
| GET | `/progress/{device_id}` | Progreso del alumno |
| POST | `/tts` | Texto a voz (Edge TTS) |
| GET | `/tts/voices` | Voces disponibles |
| POST | `/translate` | Traduccion EN->ES (MyMemory + cache) |
| POST | `/dictionary` | Agregar palabra al diccionario |
| GET | `/dictionary/{device_id}` | Listar diccionario |
| DELETE | `/dictionary/{device_id}/{entry_id}` | Eliminar palabra |

### Admin (requiere JWT)

Panel admin en: http://2.25.142.139:9000

| Metodo | Ruta | Descripcion |
|--------|------|------------|
| GET/POST/PUT/DELETE | `/admin/subjects` | CRUD materias |
| GET/POST/PUT/DELETE | `/admin/topics` | CRUD temas |
| GET/PUT | `/admin/topics/{id}/theory` | Teoria del tema |
| GET/POST/PUT/DELETE | `/admin/topics/{id}/videos` | Videos del tema |
| GET/POST/PUT/DELETE | `/admin/units` | CRUD unidades |
| GET/PUT | `/admin/units/{id}/theory` | Teoria de la unidad |
| GET/POST/PUT/DELETE | `/admin/blocks` | CRUD bloques |
| GET/PUT | `/admin/blocks/{id}/theory` | Teoria del bloque |
| GET/POST/PUT/DELETE | `/admin/items` | CRUD items de ejercicio |
| PUT | `/admin/items-reorder` | Reordenar items |
| PUT | `/admin/blocks-reorder` | Reordenar bloques |
| PUT | `/admin/units-reorder` | Reordenar unidades |
| PUT | `/admin/topics-reorder` | Reordenar temas |
| GET/POST/PUT/DELETE | `/admin/users` | CRUD usuarios alumnos |
| GET | `/admin/users/progress-summary` | Resumen de progreso |
| GET | `/admin/progress/{user}/analytics` | Analiticas por usuario |
| DELETE | `/admin/progress/{user}/{topic}/{unit}` | Resetear progreso |
| POST/DELETE | `/admin/progress/{user}/{unit}/redo` | Marcar/desmarcar redo |
| POST | `/admin/upload/image` | Subir imagen (max 5MB) |
| POST | `/admin/upload/video` | Subir video (max 50MB) |
| DELETE | `/admin/upload/image/{filename}` | Eliminar imagen |
| GET | `/admin/export/{subject_id}` | Exportar materia como JSON |
| POST | `/admin/import` | Importar materia desde JSON |
| POST | `/admin/script` | Ejecutar script batch |
| GET | `/admin/script/template` | Template JSON + guia IA |
| PUT | `/admin/topics/{id}/lock-all` | Bloquear todas las unidades |
| PUT | `/admin/topics/{id}/unlock-all` | Desbloquear todas las unidades |
| POST | `/admin/topics/{id}/reset-all` | Resetear progreso del tema |

---

## App Android

### Compilar

```bash
cd app-android
JAVA_HOME="$(pwd)/../tools/jdk" ANDROID_SDK_ROOT="$(pwd)/../tools/android-sdk" ./gradlew assembleRelease
```

O usar el script automatico: `./tools/bump-and-build.sh --push`

### Arquitectura (MVVM + Repository Pattern)

```
app-android/app/src/main/java/com/appenglish/
├── AppEnglishApp.kt           # Application + Hilt
├── MainActivity.kt            # Single Activity + NavHost
├── di/AppModule.kt            # Hilt DI: Retrofit, Room, Repos
├── data/
│   ├── remote/api/            # Retrofit interfaces (ContentApi, ProgressApi, etc.)
│   ├── remote/dto/            # Data Transfer Objects
│   ├── local/
│   │   ├── dao/               # Room DAOs
│   │   ├── entity/            # Room Entities (ProgressEntity, BlockProgressEntity, etc.)
│   │   └── AppDatabase.kt     # Room DB (version 2, migration 1->2)
│   └── repository/            # ContentRepository, ProgressRepository, DictionaryRepository
├── domain/model/              # Models.kt (Subject, Topic, Unit, ExerciseBlock, ExerciseItem)
├── ui/
│   ├── theme/                 # Color.kt, Theme.kt, Type.kt
│   ├── navigation/NavGraph.kt # Navigation routes
│   ├── screens/
│   │   ├── login/             # LoginScreen + LoginViewModel
│   │   ├── subjects/          # SubjectsScreen (lista de materias)
│   │   ├── topics/            # TopicsScreen (lista de temas)
│   │   ├── units/             # UnitsScreen (UNIDADES | TEORIA tabs)
│   │   ├── blocks/            # BlocksScreen (BLOQUES | TEORIA tabs + barras progreso)
│   │   ├── exercise/          # UnitExerciseScreen (ejercicios + drag & drop)
│   │   ├── test/              # FinalTestScreen (test final aleatorio)
│   │   ├── theory/            # TheoryScreen (teoria dedicada)
│   │   ├── dictionary/        # DictionaryScreen (diccionario personal)
│   │   ├── progress/          # ProgressScreen (estadisticas)
│   │   └── settings/          # SettingsScreen (version, update, logout)
│   └── components/            # TtsButton, TranslateableText, TranslateableText (SoundHelper)
└── util/                      # ApiConfig, UpdateManager, Extensions
```

### Navegacion

```
home -> subjects -> topics -> units -> blocks -> exercise
                  -> test
                  -> theory
         -> dictionary
         -> progress
         -> settings
```

### Actualizacion Automatica (UpdateManager.kt)

1. El usuario toca "Buscar actualizacion" en Ajustes
2. La app llama a `GET /api/v1/version` del backend
3. Compara `versionCode` del servidor con el instalado (`PackageInfo.versionCode`)
4. Si hay nueva version, muestra dialogo "Nueva version disponible vX.Y.Z"
5. Descarga APK desde `/uploads/app-release.apk`
6. Instala via `FileProvider` (Android 7+) con permisos de instalacion

---

## Panel Admin

**Acceso:** http://2.25.142.139:9000

### Secciones
- **Dashboard**: Resumen general
- **Materias**: CRUD completo con asignacion de usuarios
- **Progreso**: Progreso detallado con analiticas (unidades debiles, errores comunes)
- **Scripts**: Ejecucion masiva de JSON batch + guia IA para crear teorias
- **Usuarios**: Gestion de alumnos (crear, editar, desactivar)

### Editor de Teoria (RichTextEditor con TipTap)
- Formato: negrita, cursiva, subrayado, tachado
- Titulos: H1, H2, H3
- Alineacion: izquierda, centro, derecha, justificada
- Listas: con vinetas y numeradas
- Citas (blockquote)
- Color de texto + resaltado
- Selector de fuente (8 familias) y tamano (12px-48px)
- Tablas custom (filas x columnas, con/sin encabezado)
- Emojis (picker modal con 50+ opciones)
- Links: insertar, editar, eliminar
- Imagenes: subir desde PC (drag & drop) o desde URL
- Videos: subir .mp4/.mov desde PC o embed de YouTube
- Tips y consejos integrados (dialog modal)
- Deshacer / Rehacer
- Autosave cada 30 segundos
- Vista previa mobile

---

## Stack Tecnologico

| Capa | Tecnologia |
|------|-----------|
| Backend | Python 3.11, FastAPI, SQLAlchemy 2.0 (async), Alembic |
| Base de datos | PostgreSQL 16 (prod) / SQLite + aiosqlite (dev) |
| TTS | Microsoft Edge TTS (voces EN-US neural: Christopher, Jenny, Guy, Aria) |
| Traduccion | MyMemory API (gratuita, con cache en BD) |
| Auth | JWT (python-jose + passlib) |
| App | Kotlin, Jetpack Compose, Material 3 |
| DI | Hilt (Dagger) |
| HTTP | Retrofit 2 + OkHttp 4 |
| BD local | Room (SQLite) con migraciones |
| Audio | MediaPlayer + ExoPlayer |
| Admin | Vue 3 (Composition API), Vuetify 3, Vue Router, Pinia, Axios |
| Editor | TipTap (StarterKit, Underline, TextStyle, Color, Highlight, Link, Image, Table, TextAlign, FontFamily, Youtube) |
| Infra | Docker, Docker Compose, Nginx (alpine) |
| CI/CD | Manual via `tools/bump-and-build.sh --push` |

---

## Dependencias Clave

### Backend (requirements.txt)
```
fastapi, uvicorn[standard], edge-tts, httpx,
sqlalchemy[asyncio], aiosqlite, asyncpg, psycopg2-binary,
alembic, pydantic, pydantic-settings, python-dotenv,
python-multipart, gTTS, aiofiles
```

### Android (build.gradle.kts)
```
Compose BOM 2024.02, Navigation Compose 2.7.7,
Hilt 2.50, Retrofit 2.9 + Gson, OkHttp 4.12,
Room 2.6.1, Lottie Compose 6.3, Coroutines 1.7.3,
Media3 ExoPlayer 1.2.1
```

### Admin (package.json)
```
Vue 3.4, Vuetify 3.5, Vue Router 4.3, Pinia 2.1, Axios 1.6,
@tiptap/vue-3, @tiptap/starter-kit, @tiptap/extension-underline,
@tiptap/extension-text-style, @tiptap/extension-font-family,
@tiptap/extension-color, @tiptap/extension-highlight,
@tiptap/extension-link, @tiptap/extension-image,
@tiptap/extension-table (+ cell, header, row),
@tiptap/extension-text-align, @tiptap/extension-youtube
```

---

## Flujo de Trabajo Tipico

### Para crear contenido nuevo

1. Ir al Panel Admin > Materias > seleccionar materia > Temas
2. Crear tema con `upsert_topic` via Scripts o formulario manual
3. Crear unidades (`upsert_unit`) con ejercicios en bloques
4. Editar teoria (topic/unit/block) via el Editor de Teoria (RichTextEditor)
5. Agregar tips, videos, imagenes desde la toolbar del editor
6. Asignar usuarios a la materia (Usuarios > asignar)

### Para desplegar actualizacion

1. `./tools/bump-and-build.sh --push` (local)
2. En VPS: `git pull && cd backend && docker compose down && docker compose up -d --build`
3. Los alumnos abren la app, van a Ajustes, tocan "Buscar actualizacion"

---

## Variables de Entorno

### Backend (.env)
```
DATABASE_URL=sqlite+aiosqlite:///./appenglish.db  # dev
DATABASE_URL=postgresql+asyncpg://user:pass@db:5432/appenglish  # prod (Docker)
TTS_DEFAULT_VOICE=en-US-ChristopherNeural
MYMEMORY_EMAIL=  # opcional, para aumentar rate limit
```

### Android (ApiConfig.kt)
```kotlin
const val BASE_URL = "http://2.25.142.139:8000/api/v1/"
```
