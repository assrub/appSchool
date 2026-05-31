# API Reference — AppSchool

Base URL: `http://TU_IP:8000/api/v1`

---

## Acceso al Admin Panel

El admin panel es una **app web separada** que corre en el puerto 9000:

```
http://TU_IP:9000
```

### Login

Al abrir `http://TU_IP:9000` ves la pantalla de login. Ingresás:

```
Usuario: admin
Contraseña: admin123
```

### Rutas del panel (una vez logueado)

| Ruta | Qué hacés |
|------|----------|
| `/` | Dashboard: ve cuántas materias y temas hay |
| `/subjects` | CRUD de materias (Inglés, Matemática...) |
| `/subjects/:id/topics` | CRUD de temas dentro de una materia |
| `/topics/:id/units` | CRUD de unidades + lock/unlock/reset |
| `/units/:id/blocks` | CRUD de bloques de ejercicios |
| `/blocks/:id/items` | Editor de ejercicios con formulario dinámico por tipo |

### Cómo editar ejercicios (flujo completo)

```
Dashboard → Materias → Inglés → Verbo To Be → Unidades → Afirmativo → Bloques → PASO 1 → Ejercicios
```

Ahí ves la tabla de ejercicios. Tocás el lápiz para editar, o el + para crear uno nuevo. El formulario cambia según el tipo de ejercicio que elijas.

### Controles parentales (en Unidades)

Cada unidad tiene botones:

| Botón | Acción |
|-------|--------|
| 🔒 / 🔓 | Bloquear/desbloquear la unidad (el nene no puede entrar) |
| 🔄 | Resetear el progreso (el nene la hace de cero) |
| 🗑 | Eliminar la unidad |

También hay botones globales arriba:
- **Bloquear todo** / **Desbloquear todo** / **Resetear todo**

---

## API Endpoints

### Públicos (los usa la app Android, sin autenticación)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/content/subjects` | Lista todas las materias activas con sus temas |
| `GET` | `/content/topics/{topicId}` | Contenido completo de un tema: teoría, unidades, ejercicios |
| `GET` | `/content/topics/{topicId}/test?count=20` | Genera un test final mezclando ejercicios de todas las unidades |
| `POST` | `/tts` | Convierte texto a voz (MP3). Body: `{"text":"hello","voice":"en-US-JennyNeural"}` |
| `GET` | `/tts/voices` | Lista las voces disponibles (Christopher, Jenny, etc.) |
| `POST` | `/translate` | Traduce EN→ES. Body: `{"text":"beautiful","sourceLang":"en","targetLang":"es"}` |
| `POST` | `/progress/sync` | Guarda el progreso del nene en el servidor (backup) |
| `GET` | `/progress/{deviceId}` | Recupera el progreso guardado de un dispositivo |
| `POST` | `/dictionary` | Agrega una palabra al diccionario personal |
| `GET` | `/dictionary/{deviceId}` | Lista el diccionario del nene |
| `DELETE` | `/dictionary/{deviceId}/{entryId}` | Borra una palabra del diccionario |

### Autenticación (admin panel)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/auth/login` | Login. Body: `{"username":"admin","password":"admin123"}` → devuelve JWT |
| `POST` | `/auth/refresh` | Refresca el token JWT antes de que expire |

Todos los endpoints admin requieren este header:
```
Authorization: Bearer <token_jwt>
```

### Admin: Materias (requiere JWT)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/admin/subjects` | Listar todas las materias |
| `POST` | `/admin/subjects` | Crear materia. Body: `{"id":"math","name":"Matemática","icon":"🔢","color":"#2196F3"}` |
| `PUT` | `/admin/subjects/{id}` | Editar materia |
| `DELETE` | `/admin/subjects/{id}` | Desactivar materia (soft delete) |

### Admin: Temas (requiere JWT)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/admin/subjects/{id}/topics` | Listar temas de una materia |
| `GET` | `/admin/topics/{id}` | Ver un tema individual |
| `POST` | `/admin/topics` | Crear tema. Body: `{"id":"verb-to-be","subject_id":"english","name":"Verbo To Be"}` |
| `PUT` | `/admin/topics/{id}` | Editar tema |
| `DELETE` | `/admin/topics/{id}` | Desactivar tema |

### Admin: Unidades de ejercicio (requiere JWT)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/admin/topics/{id}/units` | Listar unidades de un tema |
| `GET` | `/admin/units/{id}` | Ver una unidad |
| `POST` | `/admin/units` | Crear unidad. Body: `{"id":"affirmative","topic_id":"verb-to-be","title":"Afirmativo","exercise_type":"fill-blank","input_mode":"tap"}` |
| `PUT` | `/admin/units/{id}` | Editar unidad |
| `DELETE` | `/admin/units/{id}` | Eliminar unidad (borra bloques y ejercicios) |
| `PUT` | `/admin/units/{id}/lock` | Bloquear unidad (nene no puede entrar) |
| `PUT` | `/admin/units/{id}/unlock` | Desbloquear unidad |

### Admin: Bloques de ejercicios (requiere JWT)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/admin/units/{id}/blocks` | Listar bloques de una unidad |
| `GET` | `/admin/blocks/{id}` | Ver un bloque |
| `POST` | `/admin/blocks` | Crear bloque. Body: `{"unit_id":"affirmative","title":"PASO 1: Pronombres"}` |
| `PUT` | `/admin/blocks/{id}` | Editar bloque |
| `DELETE` | `/admin/blocks/{id}` | Eliminar bloque (borra ejercicios) |

### Admin: Ejercicios individuales (requiere JWT)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/admin/blocks/{id}/items` | Listar ejercicios de un bloque |
| `POST` | `/admin/items` | Crear ejercicio (ver tipos abajo) |
| `PUT` | `/admin/items/{id}` | Editar ejercicio |
| `DELETE` | `/admin/items/{id}` | Eliminar ejercicio |

### Admin: Progreso y controles (requiere JWT)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `DELETE` | `/admin/progress/{deviceId}/{topicId}/{unitId}` | Resetear progreso de una unidad específica |
| `POST` | `/admin/topics/{id}/reset-all` | Resetear TODO el progreso de un tema |
| `PUT` | `/admin/topics/{id}/lock-all` | Bloquear todas las unidades de un tema |
| `PUT` | `/admin/topics/{id}/unlock-all` | Desbloquear todas las unidades de un tema |

---

## Tipos de ejercicios y sus campos

Al crear un ejercicio (`POST /admin/items`), el campo `item_type` define qué campos enviar:

### `fill-blank` (completar el espacio)
```json
{
  "block_id": 1,
  "item_type": "fill-blank",
  "sentence": "I ______ a happy student.",
  "answer": "am",
  "hint": "AM → Solo YO"
}
```

### `multiple-choice` (elegir opción)
```json
{
  "block_id": 1,
  "item_type": "multiple-choice",
  "question": "¿Cómo se dice 'yo soy'?",
  "options": ["I am", "You are", "He is"],
  "answer": "I am"
}
```

### `reorder` (ordenar palabras)
```json
{
  "block_id": 1,
  "item_type": "reorder",
  "words": ["happy", "am", "I"],
  "correct_order": ["I", "am", "happy"],
  "hint": "Empieza con 'I'"
}
```

### `listening` (escuchar y escribir)
```json
{
  "block_id": 1,
  "item_type": "listening",
  "sentence": "She is a doctor",
  "audio_url": "/audio/verb-to-be/she-is-doctor.mp3",
  "answer": "She is a doctor"
}
```

### `matching` (unir columnas)
```json
{
  "block_id": 1,
  "item_type": "matching",
  "pairs": [
    {"left": "I", "right": "am"},
    {"left": "She", "right": "is"},
    {"left": "They", "right": "are"}
  ]
}
```

### `true-false` (verdadero o falso)
```json
{
  "block_id": 1,
  "item_type": "true-false",
  "sentence": "I are happy",
  "is_correct_boolean": false,
  "answer": "I am happy"
}
```

---

## Voces TTS disponibles

`GET /api/v1/tts/voices`

| ID | Género | Acento |
|----|--------|--------|
| `en-US-ChristopherNeural` | Hombre | USA |
| `en-US-JennyNeural` | Mujer | USA |
| `en-US-GuyNeural` | Hombre | USA |
| `en-US-AriaNeural` | Mujer | USA |
| `en-GB-RyanNeural` | Hombre | Británico |
| `en-GB-SoniaNeural` | Mujer | Británico |

---

## Ejemplos con curl

```bash
# Login (obtener token)
curl -X POST http://TU_IP:8000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Listar materias (admin)
curl http://TU_IP:8000/api/v1/admin/subjects \
  -H "Authorization: Bearer TOKEN"

# Crear una materia nueva
curl -X POST http://TU_IP:8000/api/v1/admin/subjects \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":"math","name":"Matemática","icon":"🔢","color":"#2196F3"}'

# Traducir una palabra (público, no necesita token)
curl -X POST http://TU_IP:8000/api/v1/translate \
  -H "Content-Type: application/json" \
  -d '{"text":"beautiful","sourceLang":"en","targetLang":"es"}'

# Generar audio TTS
curl -X POST http://TU_IP:8000/api/v1/tts \
  -H "Content-Type: application/json" \
  -d '{"text":"I am happy","voice":"en-US-JennyNeural"}' \
  --output audio.mp3
```
