# AppSchool — Plataforma Educativa de Inglés

Plataforma de aprendizaje con ejercicios interactivos, feedback por sonido, drag & drop, traducción en tiempo real, TTS y panel admin.

**Stack:** Python 3.11 (FastAPI) + PostgreSQL 16 + Android (Kotlin, Jetpack Compose) + Vue 3 (Vuetify)

**URL producción:**
- API: http://2.25.142.139:8000
- Admin Panel: http://2.25.142.139:9000

---

## 1. Introducción

AppSchool es una plataforma educativa diseñada para que un padre pueda asignar materias a sus hijos, ver su progreso en tiempo real, identificar debilidades y fortalezas, y crear contenido personalizado desde el panel admin.

**Funcionalidades principales:**
- 6 tipos de ejercicios (completar, opción múltiple, verdadero/falso, ordenar, emparejar, escuchar)
- Progreso offline-first con sincronización automática al backend
- Teoría con editor rich-text (tablas, imágenes, videos, emojis, tips)
- Traducción EN→ES de palabras y frases
- TTS (Text-to-Speech) para escuchar pronunciación
- Panel admin con dashboard, progreso en tiempo real, y explorador de DB
- Test final aleatorio por tema
- Ejercicio de rehacer errores

---

## 2. Stack Tecnológico

| Capa | Tecnología |
|------|-----------|
| Backend | Python 3.11, FastAPI, SQLAlchemy 2.0 (async), Alembic |
| Base de datos | PostgreSQL 16 (prod) / SQLite + aiosqlite (dev) |
| TTS | Microsoft Edge TTS (voces EN-US neural: Christopher, Jenny, Guy, Aria) |
| Traducción | MyMemory API (gratuita, con cache en BD) |
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
| WebSocket | FastAPI WebSocket para actualizaciones en tiempo real |

---

## 3. Arquitectura del Proyecto

```
AppEnglish/
├── version.properties          # Versión única (Android + Backend)
├── app-android/                # App Android (Kotlin + Compose)
│   └── app/src/main/java/com/appenglish/
│       ├── AppEnglishApp.kt           # Application + Hilt + loadRemoteProgress()
│       ├── MainActivity.kt            # Single Activity + NavHost
│       ├── di/AppModule.kt            # Hilt DI: Retrofit, Room, Repos
│       ├── data/
│       │   ├── remote/api/            # Retrofit interfaces (ContentApi, ProgressApi, etc.)
│       │   ├── remote/dto/            # Data Transfer Objects
│       │   ├── local/
│       │   │   ├── dao/               # Room DAOs (ProgressDao, ContentCacheDao)
│       │   │   ├── entity/            # Room Entities (ProgressEntity, BlockProgressEntity, ContentCacheEntity)
│       │   │   └── AppDatabase.kt     # Room DB (migración 1→2→3)
│       │   └── repository/            # ContentRepository, ProgressRepository, DictionaryRepository
│       ├── domain/model/              # Models.kt (Subject, Topic, Unit, ExerciseBlock, ExerciseItem)
│       ├── ui/
│       │   ├── theme/                 # Color.kt, Theme.kt, Type.kt
│       │   ├── navigation/NavGraph.kt # Navigation routes + hideBottomBar
│       │   ├── screens/
│       │   │   ├── login/             # LoginScreen + LoginViewModel
│       │   │   ├── subjects/          # SubjectsScreen (lista de materias)
│       │   │   ├── topics/            # TopicsScreen (lista de temas)
│       │   │   ├── units/             # UnitsScreen (UNIDADES | TEORÍA tabs)
│       │   │   ├── blocks/            # BlocksScreen (BLOQUES | TEORÍA tabs + progreso por bloque)
│       │   │   ├── exercise/          # UnitExerciseScreen (ejercicios + drag & drop + FeedbackBar)
│       │   │   ├── test/              # FinalTestScreen (test final aleatorio)
│       │   │   ├── theory/            # TheoryScreen (teoría dedicada)
│       │   │   ├── dictionary/        # DictionaryScreen (diccionario personal)
│       │   │   ├── progress/          # ProgressScreen (estadísticas)
│       │   │   └── settings/          # SettingsScreen (versión, update, logout, reset progreso)
│       │   └── components/
│       │       ├── TheoryHtmlView.kt  # WebView para renderizar HTML de teoría
│       │       ├── TtsButton.kt       # Botón de Text-to-Speech
│       │       ├── TranslateableText.kt # Texto clicable con traducción popup
│       │       └── BottomNavBar.kt    # Barra de navegación inferior
│       └── util/
│           ├── ApiConfig.kt           # BASE_URL
│           └── UpdateManager.kt       # Actualización automática
├── backend/                    # FastAPI + PostgreSQL/SQLite
│   ├── main.py                # FastAPI app + WebSocket + logging
│   ├── routers/
│   │   ├── admin.py           # Endpoints admin (CRUD + analytics + sistema)
│   │   ├── content.py         # Endpoints de contenido (subjects, topics, test)
│   │   ├── progress.py        # Endpoints de progreso (sync, answer, get)
│   │   ├── auth.py            # Login/logout (JWT)
│   │   ├── tts.py             # Text-to-Speech
│   │   ├── translate.py       # Traducción EN→ES
│   │   ├── dictionary.py      # Diccionario personal
│   │   ├── ws.py              # WebSocket endpoint
│   │   └── websocket_manager.py # WebSocket broadcast manager
│   ├── models.py              # SQLAlchemy models (todas las tablas)
│   ├── schemas/               # Pydantic schemas (admin, content, progress, auth)
│   ├── database.py            # AsyncSession + init_db
│   ├── config.py              # Variables de entorno
│   ├── content/               # JSON files (subjects.json + topic files)
│   ├── uploads/               # Imágenes, videos, APK
│   │   └── app-release.apk    # APK servido al app
│   ├── logs/                  # Logs de la API (api.log)
│   │   └── api.log            # RotatingFileHandler
│   └── docker-compose.yml     # Docker services (api, admin, db)
├── admin-panel/                # Panel admin (Vue 3 + Vuetify + TipTap)
│   └── src/
│       ├── views/
│       │   ├── DashboardView.vue      # Dashboard con métricas
│       │   ├── SubjectsView.vue       # CRUD materias
│       │   ├── TopicsView.vue         # CRUD temas
│       │   ├── UnitsView.vue          # CRUD unidades
│       │   ├── BlocksView.vue         # CRUD bloques
│       │   ├── ItemsView.vue          # CRUD items de ejercicio
│       │   ├── ProgressView.vue       # Progreso detallado por usuario
│       │   ├── SystemView.vue         # Sistema: Logs, DB, SQL Console, Scripts, Health
│       │   ├── UsersView.vue          # Gestión de usuarios
│       │   ├── ScriptView.vue         # Scripts batch
│       │   ├── TheoryEditor.vue       # Editor de teoría (TipTap)
│       │   └── ContentEditorView.vue  # Editor inline de contenido
│       ├── composables/
│       │   └── useWebSocket.js        # Composable WebSocket con reconexión
│       ├── components/
│       │   ├── RichTextEditor.vue     # TipTap editor
│       │   ├── MobilePreview.vue      # Vista previa mobile
│       │   └── BottomNavBar.vue       # Barra de navegación
│       └── api/client.js              # Axios instance con JWT interceptor
├── docs/
│   └── DEBUGGING.md                   # Guía de debugging
└── tools/
    ├── bump-and-build.sh              # Script auto-version + compile + push
    ├── jdk/                           # JDK 17
    ├── android-sdk/                   # Android SDK
    └── node/                          # Node.js
```

---

## 4. Credenciales

### Admin Panel
- **URL:** http://2.25.142.139:9000
- **Usuario:** `admin`
- **Contraseña:** `admin123`

### Base de Datos (PostgreSQL en Docker)
- **Host:** `db` (desde dentro de Docker) / `2.25.142.139:5432` (desde fuera)
- **Usuario:** `user`
- **Contraseña:** `pass`
- **Base:** `appenglish`

### Credenciales de Producción (.env.production)
Las credenciales están en `backend/.env.production` (no se sube a GitHub, está en `.gitignore`):

```env
DATABASE_URL=postgresql+asyncpg://user:pass@db:5432/appenglish
SSH_HOST=2.25.142.139
SSH_USER=root
SSH_KEY_PATH=/root/.ssh/id_rsa
DOCKER_PROJECT_PATH=/docker/appSchool
```

**Para cambiar credenciales:**
1. Editar `backend/.env.production`
2. Cambiar contraseña en `docker-compose.yml` (service `db` y `api`)
3. Redeployar: `docker compose down && docker compose up -d --build`

---

## 5. Flujo de Actualización

```
┌──────────┐    git push     ┌──────────┐   git pull    ┌──────────┐
│  GitHub  │◄────────────────│ Maquina  │──────────────►│  VPS     │
│ (repo)   │                 │ local    │               │ (Docker) │
└──────────┘                 └──────────┘               └──────────┘
                                                             │
                                                       docker compose
                                                       down && up -d --build
                                                             │
                                                       ┌─────▼─────┐
                                                       │  FastAPI  │── GET /api/v1/version
                                                       │  :8000    │── GET /uploads/app-release.apk
                                                       └───────────┘
                                                             │
                                                       ┌─────▼─────────┐
                                                       │  App Android  │── UpdateManager.checkForUpdate()
                                                       │               │── Instala APK via FileProvider
                                                       └───────────────┘
```

### Flujo completo:

1. **Desarrollador** modifica código y ejecuta:
   ```bash
   ./tools/bump-and-build.sh --push
   ```
   Esto automáticamente:
   - Incrementa `versionCode` y `versionName` en `version.properties`
   - Compila el APK release
   - Copia el APK a `backend/uploads/app-release.apk`
   - Copia `version.properties` a `backend/version.properties`
   - Hace `git commit` y `git push` a GitHub

2. **VPS** (ejecutar en el servidor):
   ```bash
   cd /home/rubenmoreno/proyectos/AppEnglish
   git pull
   cd backend
   docker compose down && docker compose up -d --build
   ```
   Esto:
   - Descarga los cambios de GitHub
   - Reconstruye los contenedores Docker (api + admin + db)
   - El backend ahora sirve el nuevo `versionCode` y el nuevo APK

3. **App Android** (en el celular):
   - Abrir la app → Ajustes → "Buscar actualización"
   - La app llama a `GET /api/v1/version`
   - Si `serverVersionCode > currentVersionCode`, muestra diálogo de actualización
   - Descarga APK de `http://2.25.142.139:8000/uploads/app-release.apk`
   - Instala via FileProvider

---

## 6. Compilación

### Opción 1: Script automático (recomendado)

```bash
# Compilar + commit + push (todo junto)
./tools/bump-and-build.sh --push

# Solo compilar con bump de versión (sin commit/push)
./tools/bump-and-build.sh
```

El script:
1. Lee `version.properties` (actual: `versionCode=93`, `versionName=3.2.64`)
2. Incrementa `versionCode` (+1) y `versionName` (patch +1)
3. Compila el APK release con Gradle
4. Copia `app-release.apk` a `backend/uploads/`
5. Copia `version.properties` a `backend/`
6. Si `--push`: commit + push a GitHub

### Opción 2: Compilación manual

```bash
cd app-android
JAVA_HOME="$(pwd)/../tools/jdk" ANDROID_SDK_ROOT="$(pwd)/../tools/android-sdk" ./gradlew assembleRelease

# IMPORTANTE: copiar archivos antes de pushear
cp app/build/outputs/apk/release/app-release.apk ../backend/uploads/app-release.apk
cp ../version.properties ../backend/version.properties
```

### Estructura de version.properties

```
versionCode=93
versionName=3.2.64
```

- `versionCode`: entero, incrementa en cada build
- `versionName`: `major.minor.patch`, se incrementa automáticamente
- Tanto `build.gradle.kts` como `backend/main.py` leen de este archivo
- **No modificar manualmente** — usar el script

---

## 7. Backend (FastAPI)

### Arranque local

```bash
cd backend
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

### Docker (producción)

```bash
cd backend
docker compose down && docker compose up -d --build
```

### Contenedores Docker

| Contenedor | Puerto | Descripción |
|------------|--------|-------------|
| `api` | 8000 | FastAPI + WebSocket |
| `admin` | 9000 | Panel admin (Vue + Nginx) |
| `db` | 5432 | PostgreSQL 16 |

### Variables de entorno

```env
DATABASE_URL=sqlite+aiosqlite:///./appenglish.db     # dev
DATABASE_URL=postgresql+asyncpg://user:pass@db:5432/appenglish  # prod
TTS_DEFAULT_VOICE=en-US-ChristopherNeural
MYMEMORY_EMAIL=                                       # opcional
```

### Logging

La API escribe logs en `backend/logs/api.log` via `RotatingFileHandler`:
- Tamaño máximo: 5MB
- Backups: 3 archivos
- Los logs se pueden ver desde Admin Panel → Sistema → Logs

### WebSocket

El backend tiene un WebSocket endpoint en `ws://2.25.142.139:8000/ws/progress`:
- Broadcast cuando se sincroniza progreso (`progress_synced`)
- Broadcast cuando se registran respuestas (`answers_recorded`)
- Broadcast cuando se resetea progreso (`progress_reset`)
- El admin panel se conecta automáticamente y actualiza en tiempo real

---

## 8. Modelos de Datos (Backend)

### Tablas principales

| Tabla | Descripción | Campos clave |
|-------|-------------|-------------|
| `subjects` | Materias (ej: Inglés) | `id`, `name`, `icon`, `color`, `is_active` |
| `topics` | Temas (ej: Verbo To Be) | `id`, `subject_id`, `name`, `difficulty`, `icon` |
| `exercise_units` | Unidades (ej: Afirmativo) | `id`, `topic_id`, `title`, `exercise_type`, `input_mode`, `is_locked` |
| `exercise_blocks` | Bloques de ejercicios | `id`, `unit_id`, `title`, `shuffle`, `sort_order` |
| `exercise_items` | Items individuales | `id`, `block_id`, `sentence`, `answer`, `item_type`, `options`, `words`, `pairs` |
| `topic_theory` | Teoría del tema | `id`, `topic_id`, `text`, `table_headers`, `table_rows`, `tips` |
| `theory_sections` | Secciones de teoría del tema | `id`, `theory_id`, `title`, `text`, `examples`, `sort_order` |
| `unit_theory` | Teoría de la unidad | `id`, `unit_id`, `text`, `table_headers`, `table_rows`, `tips` |
| `unit_theory_sections` | Secciones de teoría de unidad | `id`, `unit_theory_id`, `title`, `text`, `examples`, `sort_order` |
| `block_theory` | Teoría del bloque | `id`, `block_id`, `text`, `table_headers`, `table_rows`, `tips` |
| `block_theory_sections` | Secciones de teoría de bloque | `id`, `block_theory_id`, `title`, `text`, `examples`, `sort_order` |
| `progress` | Progreso por usuario/unidad | `id`, `user_id`, `topic_id`, `unit_id`, `completed`, `score`, `total_items`, `completed_items`, `test_score`, `completed_at` |
| `block_progress` | Progreso por usuario/bloque | `id`, `user_id`, `topic_id`, `unit_id`, `block_index`, `completed`, `score`, `total_items`, `completed_at` |
| `answer_history` | Historial de respuestas | `id`, `user_id`, `topic_id`, `unit_id`, `given_answer`, `correct_answer`, `is_correct`, `answered_at` |
| `app_users` | Usuarios alumnos (Android) | `id`, `username`, `password_hash`, `display_name`, `is_active` |
| `admin_users` | Usuarios admin (panel web) | `id`, `username`, `password_hash` |
| `user_subjects` | Asignación usuario ↔ materia | `user_id`, `subject_id` |
| `dictionary_entries` | Diccionario personal | `id`, `user_id`, `word`, `translation` |
| `translation_cache` | Cache de traducciones | `source_text`, `translated_text` |
| `tts_cache` | Cache de audio TTS | `text`, `voice`, `audio_path` |
| `study_sessions` | Sesiones de estudio | `id`, `user_id`, `started_at`, `ended_at`, `exercises_attempted` |
| `content_cache` | Cache de contenido (Android) | `cache_key`, `json_data`, `cached_at` |

### Diagrama de relaciones

```
subjects ──< topics ──< exercise_units ──< exercise_blocks ──< exercise_items
    │           │              │                 │
    │           │              │                 ├─ block_theory ──< block_theory_sections
    │           │              │
    │           │              └─ unit_theory ──< unit_theory_sections
    │           │
    │           └─ topic_theory ──< theory_sections
    │
    └─ app_users (vía user_subjects)

progress (user_id, topic_id, unit_id)
block_progress (user_id, topic_id, unit_id, block_index)
answer_history (user_id, topic_id, unit_id)
```

---

## 9. App Android

### Compilación

```bash
# Con script (recomendado)
./tools/bump-and-build.sh --push

# Manual
cd app-android
JAVA_HOME="$(pwd)/../tools/jdk" ANDROID_SDK_ROOT="$(pwd)/../tools/android-sdk" ./gradlew assembleRelease
```

### Arquitectura (MVVM + Repository Pattern)

```
ui/screens/          → ViewModels + Composables
data/repository/     → Lógica de negocio (API + Room)
data/remote/api/     → Retrofit interfaces
data/local/          → Room DAOs + Entities
domain/model/        → Data classes del dominio
```

### Pantallas y Navegación

```
Login → Subjects → Topics → Units → Blocks → Exercise
                                          → Test
                  → Dictionary
                  → Progress
                  → Settings
```

- **Login:** Autenticación JWT
- **Subjects:** Lista de materias asignadas
- **Topics:** Lista de temas con progreso
- **Units:** Pestañas UNIDADES / TEORÍA
- **Blocks:** Pestañas BLOQUES / TEORÍA con progreso por bloque
- **Exercise:** Ejercicios interactivos con feedback visual
- **Test:** Test final aleatorio (20 preguntas)
- **Dictionary:** Diccionario personal EN→ES
- **Progress:** Estadísticas del usuario
- **Settings:** Versión, actualización, logout, reset progreso

### Tipos de Ejercicios (6 tipos)

| Tipo | Descripción | UI |
|------|-------------|-----|
| `fill-blank` | Completar el hueco arrastrando la palabra | Drag & drop con words[] |
| `multiple-choice` | Seleccionar respuesta correcta | Tarjetas con opciones |
| `true-false` | Verdadero o falso | Dos botones |
| `reorder` | Ordenar palabras para formar oración | Drag & drop con words[] |
| `matching` | Unir pares (columna izq ↔ der) | Selección de pares |
| `listening` | Escuchar audio y escribir | TTS + campo de texto |

### Progreso Offline-First

El progreso se maneja con un patrón offline-first:

1. **Cada respuesta** → se guarda localmente en Room DB (`saveProgressLocal()`)
2. **Cada 3 segundos** → se sincroniza al backend (debounce)
3. **Al completar bloque** → sync completo con block progress + completedAt
4. **Al completar unidad** → sync con completed=true + block progress
5. **Al iniciar la app** → `loadRemoteProgress()` restaura progreso del backend

### Cache de Contenido

Las respuestas de la API se cachean en Room (`content_cache` table):
- Al cargar contenido, se guarda en caché
- Si el backend no responde, se carga desde caché
- La app funciona offline con datos cacheados

### Actualización Automática

1. Usuario toca "Buscar actualización" en Ajustes
2. App llama `GET /api/v1/version`
3. Compara `versionCode` del servidor con el instalado
4. Si hay nueva versión, muestra diálogo
5. Descarga APK de `http://2.25.142.139:8000/uploads/app-release.apk`
6. Instala via FileProvider (Android 7+)

---

## 10. Panel Admin

**URL:** http://2.25.142.139:9000

### Secciones

#### Dashboard
- Métricas: materias, temas, usuarios, activos hoy
- Métricas de progreso: unidades completadas, completitud promedio
- Vista previa mobile

#### Materias
- CRUD completo de materias, temas, unidades, bloques, items
- Asignación de usuarios a materias
- Reordenar con drag & drop

#### Progreso
- **Detalle por unidad:** jerarquía tema → unidad → bloque con ✅ aciertos y ❌ errores
- **Errores comunes:** respuestas incorrectas más frecuentes con conteo
- **Ejercicios más difíciles:** con badge "Crítico"/"Difícil"
- **Evolución:** ¿falló al inicio? ¿lo logró después?
- **Unidades débiles/fuertes:** con opción de rehacer

#### Sistema
- **Logs:** logs en vivo de Docker (pooling cada 5s)
- **DB Explorer:** lista tablas con row counts, schema, preview de datos
- **SQL Console:** ejecutar queries SELECT directas a PostgreSQL
- **Scripts:** editor de teoría batch
- **Health:** estado de la API, versión

#### Editor de Teoría (TipTap)
- Formato: negrita, cursiva, subrayado, tachado
- Títulos: H1, H2, H3
- Alineación: izquierda, centro, derecha, justificada
- Listas, citas, tablas, emojis, links, imágenes, videos
- Tips y consejos
- Autosave cada 30 segundos

---

## 11. API Endpoints

### Públicos (sin auth)

| Método | Ruta | Descripción |
|--------|------|------------|
| GET | `/api/v1/version` | Versión (versionCode, versionName, apkUrl) |
| GET | `/api/v1/content/subjects` | Materias con temas y progreso |
| GET | `/api/v1/content/topics/{id}` | Tema completo con teoría, unidades, bloques, items |
| GET | `/api/v1/content/topics/{id}/test` | Test final aleatorio |
| POST | `/api/v1/auth/app/login` | Login de alumno (username/password → JWT) |
| GET | `/api/v1/auth/app/me` | Datos del alumno autenticado |
| POST | `/api/v1/progress/sync` | Sincronizar progreso (unit + block progress) |
| GET | `/api/v1/progress/{device_id}` | Progreso del alumno |
| POST | `/api/v1/progress/answer` | Registrar respuestas individuales (AnswerHistory) |
| POST | `/api/v1/tts` | Texto a voz (Edge TTS) |
| GET | `/api/v1/tts/voices` | Voces disponibles |
| POST | `/api/v1/translate` | Traducción EN→ES (MyMemory + cache) |
| POST | `/api/v1/dictionary` | Agregar palabra al diccionario |
| GET | `/api/v1/dictionary/{device_id}` | Listar diccionario |
| DELETE | `/api/v1/dictionary/{device_id}/{entry_id}` | Eliminar palabra |

### Admin (requiere JWT)

| Método | Ruta | Descripción |
|--------|------|------------|
| GET/POST/PUT/DELETE | `/admin/subjects` | CRUD materias |
| GET/POST/PUT/DELETE | `/admin/topics` | CRUD temas |
| GET/PUT | `/admin/topics/{id}/theory` | Teoría del tema |
| GET/POST/PUT/DELETE | `/admin/units` | CRUD unidades |
| GET/PUT | `/admin/units/{id}/theory` | Teoría de la unidad |
| GET/POST/PUT/DELETE | `/admin/blocks` | CRUD bloques |
| GET/PUT | `/admin/blocks/{id}/theory` | Teoría del bloque |
| GET/POST/PUT/DELETE | `/admin/items` | CRUD items de ejercicio |
| PUT | `/admin/items-reorder` | Reordenar items |
| PUT | `/admin/blocks-reorder` | Reordenar bloques |
| PUT | `/admin/units-reorder` | Reordenar unidades |
| PUT | `/admin/topics-reorder` | Reordenar temas |
| GET/POST/PUT/DELETE | `/admin/users` | CRUD usuarios alumnos |
| GET | `/admin/users/progress-summary` | Resumen de progreso |
| GET | `/admin/progress/{user_id}/detail` | Detalle por unidad (topic→unit→block) |
| GET | `/admin/progress/{user_id}/analytics` | Analíticas (errores, ejercicios difíciles) |
| DELETE | `/admin/progress/{user_id}/{topic_id}/{unit_id}` | Resetear progreso |
| POST/DELETE | `/admin/progress/{user_id}/{topic_id}/{unit_id}/redo` | Marcar/desmarcar redo |
| GET | `/admin/progress/dashboard-metrics` | Métricas del dashboard |
| POST | `/admin/upload/image` | Subir imagen (max 5MB) |
| POST | `/admin/upload/video` | Subir video (max 50MB) |
| DELETE | `/admin/upload/image/{filename}` | Eliminar imagen |
| GET | `/admin/export/{subject_id}` | Exportar materia como JSON |
| POST | `/admin/import` | Importar materia desde JSON |
| POST | `/admin/script` | Ejecutar script batch |
| GET | `/admin/script/template` | Template JSON + guía IA |
| PUT | `/admin/topics/{id}/lock-all` | Bloquear todas las unidades |
| PUT | `/admin/topics/{id}/unlock-all` | Desbloquear todas las unidades |
| POST | `/admin/topics/{id}/reset-all` | Resetear progreso del tema |
| DELETE | `/admin/units/{id}?hard=true` | Eliminar unidad (hard delete) |
| DELETE | `/admin/blocks/{id}?hard=true` | Eliminar bloque (hard delete) |
| DELETE | `/admin/items/{id}?hard=true` | Eliminar item (hard delete) |

### Sistema (requiere JWT)

| Método | Ruta | Descripción |
|--------|------|------------|
| GET | `/admin/system/health` | Health check (API ok, versión) |
| GET | `/admin/system/logs?container=api&lines=100` | Logs de Docker |
| GET | `/admin/system/db-tables` | Tablas con row counts |
| POST | `/admin/system/db-query` | Ejecutar SELECT (solo lectura, max 200 rows) |
| GET | `/admin/system/db-table/{table}` | Schema + preview de tabla |

---

## 12. Debugging y Mantenimiento

### Ver logs de Docker desde Admin Panel

1. Ir a Admin Panel → **Sistema** → pestaña **Logs**
2. Seleccionar contenedor: `api`, `admin`, o `db`
3. Los logs se actualizan cada 5 segundos automáticamente
4. Botón "Refrescar" para actualización manual

### Consultar base de datos desde Admin Panel

1. Ir a Admin Panel → **Sistema** → pestaña **DB Explorer**
2. Ver lista de tablas con cantidad de filas
3. Click en una tabla para ver:
   - Schema (columnas, tipos, nullable)
   - Preview (primeros 20 registros)

### Ejecutar SQL desde Admin Panel

1. Ir a Admin Panel → **Sistema** → pestaña **SQL Console**
2. Escribir query SELECT (solo lectura, max 200 filas)
3. Click "Ejecutar"
4. Ejemplos útiles:
   ```sql
   SELECT * FROM progress WHERE user_id = 1;
   SELECT * FROM block_progress WHERE user_id = 1;
   SELECT * FROM answer_history WHERE user_id = 1 ORDER BY answered_at DESC LIMIT 10;
   SELECT * FROM app_users;
   SELECT * FROM subjects;
   ```

### Ver logs de Docker por SSH

```bash
# Logs del backend
cd /home/rubenmoreno/proyectos/AppEnglish/backend
docker compose logs api --tail=100

# Logs del admin panel
docker compose logs admin --tail=100

# Logs de PostgreSQL
docker compose logs db --tail=100

# Logs en tiempo real
docker compose logs -f api
```

### Acceder a PostgreSQL por SSH

```bash
# Conectar al contenedor de PostgreSQL
cd /home/rubenmoreno/proyectos/AppEnglish/backend
docker compose exec db psql -U user -d appenglish

# Queries útiles
\dt                                    -- Listar tablas
SELECT * FROM progress;                -- Ver progreso
SELECT * FROM answer_history LIMIT 10; -- Ver historial
SELECT * FROM app_users;               -- Ver usuarios
\q                                     -- Salir
```

### Endpoints de sistema (sin admin panel)

```bash
# Health check
curl http://2.25.142.139:8000/api/v1/admin/system/health

# Versión
curl http://2.25.142.139:8000/api/v1/version

# Logs (requiere token)
curl -H "Authorization: Bearer <token>" http://2.25.142.139:8000/api/v1/admin/system/logs

# DB tables (requiere token)
curl -H "Authorization: Bearer <token>" http://2.25.142.139:8000/api/v1/admin/system/db-tables
```

---

## 13. Despliegue en VPS

### Requisitos del VPS
- Docker + Docker Compose
- Puertos abiertos: 8000 (API), 9000 (Admin), 5432 (PostgreSQL)

### Pasos de despliegue

```bash
# 1. Clonar el repo (solo la primera vez)
cd /home/rubenmoreno/proyectos/AppEnglish
git clone https://github.com/assrub/appSchool.git .

# 2. Desplegar
cd backend
docker compose down && docker compose up -d --build

# 3. Verificar
curl http://localhost:8000/api/v1/version
```

### Actualizar después de cambios

```bash
cd /home/rubenmoreno/proyectos/AppEnglish
git pull
cd backend
docker compose down && docker compose up -d --build
```

**IMPORTANTE:** Siempre redeployar después de cambios en:
- Backend (archivos .py)
- Admin panel (archivos .vue)
- Docker (Dockerfile, docker-compose.yml)
- version.properties

### Estructura Docker

```yaml
# docker-compose.yml
services:
  api:
    build: .
    ports:
      - "8000:8000"
    environment:
      - DATABASE_URL=postgresql+asyncpg://user:pass@db:5432/appenglish
    depends_on:
      db:
        condition: service_healthy
    volumes:
      - ./content:/app/content
      - ./uploads:/app/uploads
      - ./logs:/app/logs

  admin:
    build: ../admin-panel
    ports:
      - "9000:80"
    depends_on:
      - api

  db:
    image: postgres:16-alpine
    environment:
      - POSTGRES_USER=user
      - POSTGRES_PASSWORD=pass
      - POSTGRES_DB=appenglish
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U user -d appenglish"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  pgdata:
```

---

## 14. Solución de Problemas

### La app no se actualiza

**Causa:** El APK no se copió a `backend/uploads/` o el VPS no se redeployó.

**Solución:**
```bash
# 1. Verificar que el APK existe
ls -la backend/uploads/app-release.apk

# 2. Verificar versión en el backend
curl http://2.25.142.139:8000/api/v1/version

# 3. Si la versión no coincide, redeployar
cd backend && docker compose down && docker compose up -d --build
```

### El progreso no se sincroniza

**Causa:** La app no pudo enviar datos al backend (sin conexión, error 500, etc.).

**Solución:**
1. Verificar que el backend esté corriendo: `curl http://2.25.142.139:8000/api/v1/version`
2. Verificar logs: Admin Panel → Sistema → Logs
3. Verificar datos: Admin Panel → Sistema → SQL Console → `SELECT * FROM progress`
4. La app guarda progreso localmente (Room) y sincroniza automáticamente cuando hay conexión

### El admin panel no muestra datos

**Causa:** No hay datos de progreso o el endpoint falla.

**Solución:**
1. Verificar que hay datos: SQL Console → `SELECT * FROM progress`
2. Verificar que el endpoint funciona: `curl -H "Authorization: Bearer <token>" http://2.25.142.139:8000/api/v1/admin/progress/1/detail`
3. Si no hay datos, el alumno necesita hacer ejercicios con la app

### Error 500 en la API

**Causa:** Error de Python en el backend.

**Solución:**
1. Ver logs: Admin Panel → Sistema → Logs
2. O por SSH: `docker compose logs api --tail=50`
3. Buscar el traceback y corregir el código
4. Redeployar: `docker compose down && docker compose up -d --build`

### Docker no construye

**Causa:** Error de compilación (Python, Node.js, etc.).

**Solución:**
1. Ver el error: `docker compose up -d --build`
2. Corregir el código
3. Volver a intentar

---

## 15. Dependencias Clave

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
