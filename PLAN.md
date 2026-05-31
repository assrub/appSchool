# AppEnglish — Plan de Desarrollo

## Visión General

App móvil educativa para Android con animaciones tipo juego (personajes que recorren caminos, puertas que se desbloquean). Escalable a múltiples materias mediante contenido definido en JSON servido desde un backend en VPS.

**Objetivo inmediato:** Que el alumno practique el verbo "to be" en inglés y rinda bien su examen en 2 días.

---

## 1. Stack Tecnológico

| Capa | Tecnología | Justificación |
|------|-----------|---------------|
| App Android | Kotlin + Jetpack Compose | Nativo, rendimiento, animaciones Canvas/Lottie |
| DI | Hilt | Inyección de dependencias estándar Android |
| HTTP Client | Retrofit + OkHttp | Consumo de API REST del backend |
| BD Local | Room (SQLite) | Progreso offline, diccionario, preferencias |
| Backend | Python 3.11 + FastAPI | Ligero, async, deploy simple en VPS |
| BD Remota | PostgreSQL | Backup de progreso, contenido versionado |
| TTS | Edge TTS (via backend) | Voces naturales EN-US (Christopher, Jenny) |
| Traducción | MyMemory API (via backend) | Traducción EN→ES gratuita con caché |
| Animaciones | Lottie + Canvas (Compose) | Personajes, caminos, puertas, efectos |
| Contenido | JSON servido desde backend | Agregar materias/temas sin tocar código |

---

## 2. Arquitectura de la App

```
app/
├── main/java/com/appenglish/
│   ├── AppEnglishApp.kt              # Application + Hilt
│   ├── MainActivity.kt               # Single Activity
│   │
│   ├── di/                            # Hilt modules
│   │   ├── NetworkModule.kt          # Retrofit, OkHttp
│   │   ├── DatabaseModule.kt         # Room
│   │   └── RepositoryModule.kt      # Repositorios
│   │
│   ├── data/
│   │   ├── remote/
│   │   │   ├── api/
│   │   │   │   ├── ContentApi.kt     # /content/*
│   │   │   │   ├── TtsApi.kt         # /tts
│   │   │   │   └── TranslateApi.kt   # /translate
│   │   │   └── dto/                  # Data Transfer Objects
│   │   │       ├── SubjectDto.kt
│   │   │       ├── TopicDto.kt
│   │   │       └── ExerciseDto.kt
│   │   ├── local/
│   │   │   ├── dao/
│   │   │   │   ├── ProgressDao.kt
│   │   │   │   └── DictionaryDao.kt
│   │   │   ├── entity/
│   │   │   │   ├── ProgressEntity.kt
│   │   │   │   └── DictionaryEntry.kt
│   │   │   └── AppDatabase.kt
│   │   └── repository/
│   │       ├── ContentRepository.kt
│   │       ├── ProgressRepository.kt
│   │       └── DictionaryRepository.kt
│   │
│   ├── domain/
│   │   └── model/
│   │       ├── Subject.kt
│   │       ├── Topic.kt
│   │       ├── Exercise.kt
│   │       ├── ExerciseBlock.kt
│   │       └── ExerciseItem.kt
│   │
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── Theme.kt
│   │   │   ├── Color.kt
│   │   │   └── Type.kt
│   │   ├── navigation/
│   │   │   └── NavGraph.kt          # Navigation Compose
│   │   ├── screens/
│   │   │   ├── subjects/
│   │   │   │   ├── SubjectsScreen.kt
│   │   │   │   └── SubjectsViewModel.kt
│   │   │   ├── topic/
│   │   │   │   ├── TopicScreen.kt
│   │   │   │   ├── TheoryTab.kt
│   │   │   │   └── TopicViewModel.kt
│   │   │   ├── exercise/
│   │   │   │   ├── ExerciseScreen.kt
│   │   │   │   ├── FillBlankExercise.kt
│   │   │   │   └── ExerciseViewModel.kt
│   │   │   ├── dictionary/
│   │   │   │   ├── DictionaryScreen.kt
│   │   │   │   └── DictionaryViewModel.kt
│   │   │   ├── progress/
│   │   │   │   ├── ProgressScreen.kt
│   │   │   │   └── ProgressViewModel.kt
│   │   │   └── test/
│   │   │       ├── FinalTestScreen.kt
│   │   │       └── FinalTestViewModel.kt
│   │   └── components/
│   │       ├── ExerciseInput.kt      # Input + botón corregir
│   │       ├── ExerciseFeedback.kt   # ✅/❌ animado
│   │       ├── ProgressBar.kt         # Barra de progreso
│   │       ├── TtsButton.kt          # Botón 🔊 TTS
│   │       ├── TranslatePopover.kt   # Popup traducción (long press)
│   │       └── MagicTable.kt         # Tabla explicativa
│   │
│   ├── animation/                    # FUTURO: Capa de animaciones
│   │   ├── PathMap.kt               # Canvas del camino
│   │   ├── Character.kt             # Personaje animado (Lottie)
│   │   ├── DoorNode.kt             # Nodo-puerta en el camino
│   │   └── AnimationManager.kt      # Orquestador de animaciones
│   │
│   └── util/
│       ├── AudioPlayer.kt           # Reproduce audio TTS
│       └── Extensions.kt
│
├── assets/
│   ├── lottie/                       # Animaciones Lottie (futuro)
│   │   ├── character_walk.json
│   │   ├── door_open.json
│   │   └── celebration.json
│   └── images/                       # Iconos, fondos (futuro)
│       └── ...
│
└── build.gradle.kts
```

---

## 3. Definición de Dominio

### Subject (Materia)
```kotlin
data class Subject(
    val id: String,           // "english", "math"
    val name: String,         // "Inglés", "Matemática"
    val icon: String,         // Emoji/icono
    val color: String,        // Color HEX de la materia
    val topics: List<Topic>,  // Temas dentro de la materia
    val mapConfig: MapConfig? // Config del mapa animado (futuro)
)
```

### Topic (Tema)
```kotlin
data class Topic(
    val id: String,           // "verb-to-be"
    val name: String,         // "Verbo To Be"
    val order: Int,           // Orden en el camino
    val difficulty: Int,      // 1-5
    val units: List<Unit>,    // Unidades del tema
    val theory: Theory?,      // Explicación
    val isLocked: Boolean     // ¿Desbloqueado?
)
```

### Unit (Unidad)
```kotlin
data class Unit(
    val id: String,           // "affirmative", "negative"
    val title: String,        // "Afirmativo (am / is / are)"
    val exerciseType: String, // "fill-blank", futuro: "multiple-choice", "drag"
    val blocks: List<ExerciseBlock>
)
```

### ExerciseBlock (Bloque de ejercicios)
```kotlin
data class ExerciseBlock(
    val title: String,        // "PASO 1: Pronombres personales"
    val items: List<ExerciseItem>
)
```

### ExerciseItem (Ítem individual)
```kotlin
data class ExerciseItem(
    val sentence: String,     // "I ______ a happy student."
    val answer: String,       // "am"
    val hint: String?         // Pista opcional: "AM → Solo YO"
)
```

---

## 4. Navegación

```
NavHost(startDestination = "home")
│
├── home                  → Lista de materias (grid con íconos)
│
├── subject/{subjectId}   → Lista de temas de la materia
│                           (futuro: mapa animado con camino)
│
├── topic/{topicId}       → Pantalla del tema
│   ├── Tab: Teoría       → Explicación + tabla + tips
│   ├── Tab: Unidad 1     → Ejercicios interactivos
│   ├── Tab: Unidad 2     → Ejercicios interactivos
│   └── Tab: Test Final   → Mezcla aleatoria de todas las unidades
│
├── dictionary            → Diccionario personal
│
└── progress              → Estadísticas y progreso general
```

---

## 5. Flujo de Ejercicio (MVP)

1. Se muestra la frase con un hueco: `I ______ a happy student.`
2. El alumno escribe en el input y presiona "Corregir" o Enter
3. Feedback:
   - ✅ **Correcto:** Animación verde, muestra tip ("AM → Solo YO"), avanza 1
   - ❌ **Incorrecto:** Animación roja, muestra respuesta correcta, no avanza
4. Barra de progreso muestra posición actual (5/10)
5. Al completar un bloque: toast "¡Bloque completado!" y botón "Siguiente bloque"
6. Al completar todos los bloques de una unidad: la unidad se marca como completada
7. Al completar todas las unidades: se habilita el Test Final
8. Test Final: 20 preguntas aleatorias mezclando las 4 unidades, con puntuación

---

## 6. Funcionalidades Principales

### 6.1. Text-to-Speech (TTS)
- Botón 🔊 visible en cada frase de ejercicios y teoría
- Al presionar, envía el texto al backend → Edge TTS → reproduce audio
- Voces configurables: `en-US-ChristopherNeural` (♂) / `en-US-JennyNeural` (♀)
- Cache local de audios frecuentes para no re-descargar

### 6.2. Traducción (Long Press)
- Mantener presionada una palabra/frase → popover con:
  - Traducción EN→ES (MyMemory API via backend)
  - Botón 🔊 para escuchar la palabra
  - "Guardar en diccionario"
- Si no hay conexión, muestra mensaje "Sin conexión"

### 6.3. Diccionario Personal
- Room DB local: `word`, `translation`, `dateAdded`, `timesLookedUp`
- Pantalla dedicada con lista de palabras guardadas
- Ordenable por fecha, alfabético, frecuencia
- Al tocar una palabra → se reproduce su pronunciación

### 6.4. Progreso
- Room DB local: `topicId`, `unitId`, `completed`, `score`, `dateCompleted`
- Pantalla de progreso con:
  - % completado por materia
  - % completado por tema
  - Mejor puntuación en Test Final
  - Rachas de días consecutivos
- Sincronización con backend para backup

---

## 7. Formato de Contenido (JSON en Backend)

### Estructura de `topic.json` (Verbo To Be)

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
        ["He / She / It", "is", "She is a doctor"],
        ["You / We / They", "are", "We are friends"],
        ["Un nombre solo (Claudia)", "is", "Claudia is smart"],
        ["Dos nombres (David and Dagui)", "are", "They are outside"]
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
      "blocks": [
        {
          "title": "PASO 1: Pronombres personales y familia cercana",
          "items": [
            {"sentence": "I ______ a happy student.", "answer": "am", "hint": "AM → Solo YO"},
            {"sentence": "You ______ my best friend.", "answer": "are", "hint": "ARE → Para you"},
            ...
          ]
        },
        ...
      ]
    },
    {
      "id": "negative",
      "title": "Negativo (am not / isn't / aren't)",
      "exerciseType": "fill-blank",
      "explanation": "Para negar, añade NOT después de am, is, are.",
      "blocks": [ ... ]
    },
    {
      "id": "interrogative",
      "title": "Interrogativo (Am / Is / Are ...?)",
      "exerciseType": "fill-blank",
      "explanation": "Para preguntar, invierte el orden: verbo + sujeto.",
      "blocks": [ ... ]
    },
    {
      "id": "short-answers",
      "title": "Respuestas cortas (Yes/No)",
      "exerciseType": "fill-blank",
      "explanation": "Responde sin repetir toda la frase.",
      "blocks": [ ... ]
    }
  ]
}
```

---

## 8. Plan de Desarrollo — 2 Días

### DÍA 1 — Setup y Core Funcional

| # | Tarea | Duración |
|---|-------|----------|
| 1.1 | Crear proyecto Android (Kotlin, Jetpack Compose, Material 3) | 30m |
| 1.2 | Configurar Hilt, Retrofit, Room, Navigation Compose | 30m |
| 1.3 | Backend VPS: crear proyecto FastAPI, endpoint `/content/subjects` | 1h |
| 1.4 | Backend VPS: endpoint `/content/topics/{id}` | 30m |
| 1.5 | Backend VPS: endpoint `/tts` con Edge TTS | 1h |
| 1.6 | Backend VPS: endpoint `/translate` con MyMemory + caché | 1h |
| 1.7 | Preparar JSON del verbo to be en backend | 1h |
| 1.8 | Pantalla `SubjectsScreen` (lista de materias) | 30m |
| 1.9 | Pantalla `TopicScreen` con tabs (Teoría + Ejercicios) | 1h |
| 1.10 | Componente `TheoryTab` (explicación, tabla, tips) | 1h |
| 1.11 | Componente `FillBlankExercise` (input, corrección, progreso) | 2h |
| 1.12 | Componente `TtsButton` (llamar backend, reproducir audio) | 1h |
| **Total Día 1** | | **~11h** |

### DÍA 2 — Ejercicios completos + Funcionalidades extra

| # | Tarea | Duración |
|---|-------|----------|
| 2.1 | Conversión final de todos los ejercicios a JSON | 30m |
| 2.2 | Unidad Afirmativo funcional (70+ ejercicios) | 30m |
| 2.3 | Unidad Negativo funcional | 30m |
| 2.4 | Unidad Interrogativo funcional | 30m |
| 2.5 | Unidad Respuestas cortas funcional | 30m |
| 2.6 | Pantalla `FinalTestScreen` (20 preguntas aleatorias mezcladas) | 1.5h |
| 2.7 | Componente `TranslatePopover` (long press → traducción) | 1.5h |
| 2.8 | Room DB: entidades `Progress` y `DictionaryEntry`, DAOs | 1h |
| 2.9 | Pantalla `DictionaryScreen` (lista, ordenar, reproducir) | 1h |
| 2.10 | Pantalla `ProgressScreen` (estadísticas, barras, rachas) | 1h |
| 2.11 | Persistencia de progreso en Room + sincronización con backend | 1h |
| 2.12 | Pulido visual, tema de colores, fuentes, animaciones de feedback | 1h |
| 2.13 | Build APK release, instalar en el celular | 30m |
| **Total Día 2** | | **~11h** |

---

## 9. Post-Examen — Escalabilidad y Animaciones

### Fase 2 — Sistema de Animaciones (Semana 2)
- Canvas con mapa de camino por materia (drawPath sobre Surface)
- Nodos-puerta en posiciones calculadas desde el JSON
- Lottie: personaje que camina entre nodos al completar temas
- Animación de puerta abriéndose (Lottie `door_open.json`)
- Sistema de desbloqueo: tema completado → se desbloquea el siguiente
- Cada materia tiene su propio tema visual:
  - Inglés → Sendero de libros en un bosque
  - Matemática → Camino de circuitos electrónicos
  - Ciencias → Laboratorio con tubos de ensayo
  - Geografía → Mapa del tesoro con islas

### Fase 3 — Sistema de Materias Genérico (Semana 3)
- Agregar Matemática creando solo JSON: `content/math/addition/topic.json`
- Soporte para nuevos tipos de ejercicio:
  - `multiple-choice` (elegir opción correcta)
  - `drag-drop` (arrastrar palabras para formar oraciones)
  - `listening` (escuchar y escribir)
- Editor web de contenido en el VPS (React simple)

### Fase 4 — Multi-usuario y Sincronización (Semana 4)
- Login simple (Google Sign-In o email)
- Sincronización de progreso entre dispositivos
- Estadísticas por alumno en el backend
- Backup automático

---

## 10. Dependencias del Proyecto

### App Android (build.gradle.kts)

```kotlin
// Core
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
implementation("androidx.activity:activity-compose:1.8.2")

// Compose
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-graphics")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.7")

// Hilt (DI)
implementation("com.google.dagger:hilt-android:2.50")
kapt("com.google.dagger:hilt-android-compiler:2.50")
implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

// Retrofit + OkHttp
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Room (local DB)
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Lottie (animaciones, futuro)
implementation("com.airbnb.android:lottie-compose:6.3.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// Media3 (reproducción de audio TTS)
implementation("androidx.media3:media3-exoplayer:1.2.1")
```

### Backend Python (requirements.txt)

```
fastapi==0.109.2
uvicorn[standard]==0.27.1
edge-tts==6.1.9
httpx==0.27.0
sqlalchemy==2.0.27
asyncpg==0.29.0
alembic==1.13.1
pydantic==2.6.1
python-dotenv==1.0.1
```

---

## 11. Convenciones

- **Idioma del código:** Inglés (variables, funciones, comentarios)
- **Idioma de la UI:** Español (textos visibles para el alumno)
- **Nombrado:** camelCase para propiedades, PascalCase para clases/composables
- **Arquitectura:** MVVM (ViewModel + StateFlow + Compose)
- **Manejo de estado:** `StateFlow` en ViewModels, `collectAsStateWithLifecycle()` en Compose
- **Errores:** `sealed class Resource<T> { Loading, Success, Error }`
- **Branch git:** `main` para producción, `dev` para desarrollo
