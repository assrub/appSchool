# Datos Estadísticos - AppEnglish

## Resumen General

Este documento detalla todos los datos estadísticos que maneja la aplicación AppEnglish, tanto en la app móvil como en el panel de administración. El objetivo es proporcionar métricas pedagógicas significativas que ayuden a estudiantes y administradores a comprender el progreso real del aprendizaje.

---

## 1. Métricas Pedagógicas Fundamentales

### 1.1 Avance (Progress)
**Definición:** Porcentaje de ejercicios intentados del total disponible.

**Fórmula:**
```
avance = (items_intentados / total_items) × 100
```

**Propósito:** Indica cuánto del contenido ha sido abordado, sin importar si las respuestas fueron correctas o incorrectas.

**Interpretación:**
- 0% = No ha comenzado
- 1-99% = En progreso
- 100% = Ha intentado todos los ejercicios

---

### 1.2 Precisión (Accuracy)
**Definición:** Porcentaje de respuestas correctas en el PRIMER intento.

**Fórmula:**
```
precisión = (respuestas_correctas_primer_intento / items_intentados) × 100
```

**Propósito:** Mide la comprensión inicial del estudiante. Una alta precisión indica que el estudiante entendió el concepto desde el principio.

**Interpretación:**
- 0-49% = Dificultad significativa, necesita repaso
- 50-79% = Comprensión moderada
- 80-100% = Excelente comprensión inicial

---

### 1.3 Dominio (Mastery)
**Definición:** Porcentaje de ejercicios dominados (correctos en primer intento O corregidos exitosamente en reintentos).

**Fórmula:**
```
dominio = (items_dominados / total_items) × 100
```

**Propósito:** Mide el aprendizaje real. Un ejercicio se considera "dominado" cuando el estudiante finalmente lo responde correctamente, independientemente de cuántos intentos necesitó.

**Interpretación:**
- 0-49% = Aprendizaje incompleto
- 50-89% = Aprendizaje en progreso
- 90-100% = **DOMINADO** - El estudiante ha internalizado el concepto

---

### 1.4 Estado (Status)
**Definición:** Estado cualitativo del progreso de una unidad.

**Valores posibles:**
- `not_started` = No iniciado (0 items intentados)
- `in_progress` = En progreso (algunos items intentados, no todos dominados)
- `completed` = Completado (todos los items intentados, pero dominio < 90%)
- `mastered` = **Dominado** (dominio ≥ 90%)

**Propósito:** Proporcionar una visión rápida del estado de aprendizaje.

---

## 2. Datos Estadísticos en la App Móvil

### 2.1 Sección "Mi Progreso" (ProgressScreen)

#### Tarjeta de Resumen
**Datos mostrados:**
- **Porcentaje general:** `(unidades_completadas / total_unidades) × 100`
- **Unidades completadas:** Cantidad de unidades con estado `completed` o `mastered`
- **Precisión promedio:** Media de `accuracy` de todas las unidades
- **Dominio promedio:** Media de `mastery` de todas las unidades
- **Unidades dominadas:** Cantidad de unidades con estado `mastered`
- **Tiempo total:** Suma de `time_spent_seconds` de todas las unidades

**Propósito:** Dar al estudiante una visión global de su progreso y rendimiento.

---

#### Progreso por Materia
**Datos mostrados:**
- **Temas completados:** Cantidad de temas donde todas las unidades están `completed` o `mastered`
- **Total de temas:** Cantidad total de temas en la materia

**Propósito:** Mostrar avance a nivel de materia.

---

#### Progreso por Tema
**Datos mostrados:**
- **Unidades completadas:** Cantidad de unidades con estado `completed` o `mastered`
- **Total de unidades:** Cantidad total de unidades en el tema

**Propósito:** Mostrar avance a nivel de tema.

---

#### Progreso por Unidad
**Datos mostrados:**
- **Barra de avance:** `items_intentados / total_items`
- **Porcentaje de avance:** `(items_intentados / total_items) × 100`
- **Precisión:** `accuracy` de la unidad
- **Dominio:** `mastery` de la unidad
- **Puntuación:** `score / total_items` (respuestas correctas)
- **Estado:** `status` de la unidad (NO INICIADO / EN PROGRESO / COMPLETADO / DOMINADO)

**Propósito:** Mostrar el progreso detallado de cada unidad con todas las métricas pedagógicas.

---

### 2.2 Sección "Temas" (TopicsScreen)

**Datos mostrados por tema:**
- **Barra de progreso:** `(unidades_completadas / total_unidades)`
- **Porcentaje:** `(unidades_completadas / total_unidades) × 100`
- **Contador:** `unidades_completadas / total_unidades`

**Propósito:** Mostrar qué tan avanzado está cada tema.

---

### 2.3 Sección "Unidades" (UnitsScreen)

**Datos mostrados por unidad:**
- **Barra de progreso:** `completed_items / total_items`
- **Porcentaje:** `(completed_items / total_items) × 100`
- **Contador:** `completed_items / total_items`

**Propósito:** Mostrar qué tan avanzada está cada unidad.

---

### 2.4 Sección "Bloques" (BlocksScreen)

**Datos mostrados por bloque:**
- **Barra de progreso:** `completed_items / total_items`
- **Porcentaje:** `(completed_items / total_items) × 100`
- **Contador:** `completed_items / total_items`
- **Respuestas válidas:** `score` (respuestas correctas)
- **Respuestas inválidas:** `completed_items - score` (respuestas incorrectas)

**Propósito:** Mostrar el progreso detallado de cada bloque dentro de una unidad.

---

### 2.5 Sección "Ejercicios" (ExerciseScreen)

**Datos mostrados durante el ejercicio:**
- **Barra de progreso del bloque:** `current_block_completed_items / current_block_total_items`
- **Contador:** `current_block_completed_items / current_block_total_items`
- **Feedback inmediato:** Correcto ✅ / Incorrecto ❌ con respuesta correcta

**Propósito:** Proporcionar retroalimentación inmediata durante la práctica.

---

## 3. Datos Estadísticos en el Panel de Administración

### 3.1 Sección "Progreso de Estudiantes" (ProgressView)

#### Vista General por Estudiante
**Datos mostrados:**
- **Porcentaje de completitud:** `(unidades_completadas / total_unidades) × 100`
- **Unidades completadas:** Cantidad de unidades con estado `completed` o `mastered`
- **Total de unidades:** Cantidad total de unidades disponibles

**Propósito:** Dar al administrador una visión rápida del progreso de cada estudiante.

---

#### Detalle por Unidad
**Datos mostrados:**
- **Estado:** `status` (not_started / in_progress / completed / mastered)
- **Puntuación:** `score / total_items`
- **Items completados:** `completed_items / total_items`
- **Test Score:** `test_score` (si existe)
- **Fecha de completado:** `completed_at` (si existe)
- **Conteo de respuestas correctas:** `correct_count` (desde AnswerHistory)
- **Conteo de respuestas incorrectas:** `wrong_count` (desde AnswerHistory)
- **Total de intentos:** `total_attempts` (desde AnswerHistory)
- **Precisión:** `accuracy` de la unidad
- **Dominio:** `mastery` de la unidad
- **Items intentados:** `items_attempted`
- **Items dominados:** `items_mastered`
- **Items correctos en primer intento:** `items_correct_first`
- **Tiempo dedicado:** `time_spent_seconds`

**Propósito:** Proporcionar al administrador un análisis detallado del rendimiento del estudiante en cada unidad.

---

#### Detalle por Bloque
**Datos mostrados:**
- **Puntuación:** `score / total_items`
- **Estado:** `completed` (true/false)
- **Conteo de errores:** `wrong_count` (si está completado)

**Propósito:** Mostrar el progreso a nivel de bloque dentro de cada unidad.

---

### 3.2 Sección "Análisis" (Analytics)

#### Unidades Débiles (Weak Units)
**Datos mostrados:**
- **ID de unidad:** `unit_id`
- **ID de tema:** `topic_id`
- **Respuestas correctas:** `correct`
- **Respuestas incorrectas:** `wrong`
- **Total de intentos:** `total_attempts`
- **Tasa de error:** `(wrong / total_attempts) × 100`

**Propósito:** Identificar las unidades donde el estudiante tiene más dificultades.

---

#### Unidades Fuertes (Strong Units)
**Datos mostrados:**
- Mismos datos que "Unidades Débiles" pero ordenados por menor tasa de error

**Propósito:** Identificar las unidades donde el estudiante tiene mejor rendimiento.

---

#### Errores Comunes (Common Mistakes)
**Datos mostrados:**
- **Respuesta dada:** `given_answer`
- **Respuesta correcta:** `correct_answer`
- **Cantidad de veces:** `count`
- **ID de tema:** `topic_id`
- **ID de unidad:** `unit_id`

**Propósito:** Identificar patrones de errores específicos que el estudiante comete repetidamente.

---

#### Ejercicios Más Difíciles (Hardest Exercises)
**Datos mostrados:**
- **ID de tema:** `topic_id`
- **ID de unidad:** `unit_id`
- **Respuesta correcta:** `correct_answer`
- **Total de intentos:** `total_attempts`
- **Intentos fallidos:** `failed_attempts`
- **Respuestas incorrectas comunes:** `common_wrong_answers` (top 3)
- **Conteo de respuestas incorrectas comunes:** `common_wrong_counts`

**Propósito:** Identificar ejercicios específicos que causan más dificultades.

---

#### Mejora por Reintentos (Retry Improvement)
**Datos mostrados:**
- **ID de tema:** `topic_id`
- **ID de unidad:** `unit_id`
- **Respuesta correcta:** `correct_answer`
- **Total de intentos:** `total_attempts`
- **Intentos fallidos:** `failed_attempts`
- **Eventualmente correcto:** `eventually_correct` (true/false)
- **Errores consecutivos al inicio:** `consecutive_wrong_at_start`

**Propósito:** Mostrar si el estudiante mejora con la práctica y cuántos intentos necesita para dominar un ejercicio.

---

#### Precisión General (Overall Accuracy)
**Datos mostrados:**
- **Precisión:** `(total_correct / total_answered) × 100`
- **Total respondido:** `total_answered`
- **Total correcto:** `total_correct`

**Propósito:** Proporcionar una métrica global del rendimiento del estudiante.

---

## 4. Almacenamiento de Datos

### 4.1 Base de Datos Local (Room - Android)

#### Tabla `progress`
- `device_id` (String): ID del dispositivo
- `topic_id` (String): ID del tema
- `unit_id` (String): ID de la unidad
- `completed` (Boolean): Si la unidad está completada
- `score` (Int): Respuestas correctas
- `total_items` (Int): Total de ejercicios en la unidad
- `completed_items` (Int): Ejercicios intentados
- `test_score` (Int?): Puntuación del test final
- `accuracy` (Float): Precisión (0-100)
- `mastery` (Float): Dominio (0-100)
- `status` (String): Estado (not_started/in_progress/completed/mastered)
- `items_attempted` (Int): Items intentados
- `items_mastered` (Int): Items dominados
- `items_correct_first` (Int): Items correctos en primer intento
- `time_spent_seconds` (Int): Tiempo dedicado en segundos
- `started_at` (Long): Timestamp de inicio
- `completed_at` (Long?): Timestamp de completado

---

#### Tabla `block_progress`
- `device_id` (String): ID del dispositivo
- `topic_id` (String): ID del tema
- `unit_id` (String): ID de la unidad
- `block_index` (Int): Índice del bloque
- `score` (Int): Respuestas correctas en el bloque
- `total_items` (Int): Total de ejercicios en el bloque
- `completed` (Boolean): Si el bloque está completado
- `completed_items` (Int): Ejercicios intentados en el bloque
- `completed_at` (Long?): Timestamp de completado

---

#### Tabla `answer_history` (sincronizada con backend)
- `topic_id` (String): ID del tema
- `unit_id` (String): ID de la unidad
- `given_answer` (String): Respuesta dada por el estudiante
- `correct_answer` (String): Respuesta correcta
- `is_correct` (Boolean): Si la respuesta fue correcta

---

### 4.2 Base de Datos Backend (PostgreSQL)

#### Tabla `progress`
Mismos campos que la tabla local, más:
- `user_id` (Integer): ID del usuario (FK a app_users)
- `last_activity_at` (Timestamp): Última actividad

---

#### Tabla `block_progress`
Mismos campos que la tabla local, más:
- `user_id` (Integer): ID del usuario (FK a app_users)

---

#### Tabla `answer_history`
Mismos campos que la tabla local, más:
- `id` (Integer): ID único
- `user_id` (Integer): ID del usuario (FK a app_users)
- `answered_at` (Timestamp): Fecha y hora de la respuesta

---

## 5. Cálculos y Fórmulas Detalladas

### 5.1 Cálculo de Precisión (Accuracy)

**En ExerciseViewModel (Android):**
```kotlin
val accuracy = if (attempted > 0) (firstCorrect.toFloat() / attempted * 100) else 0f
```

**En Backend:**
```python
accuracy = round(correct_count / total * 100, 1) if total > 0 else 0
```

**Diferencia:** El backend calcula precisión basándose en TODAS las respuestas (incluyendo reintentos), mientras que la app calcula basándose solo en el PRIMER intento.

**Recomendación:** Unificar la fórmula para que ambos usen la misma definición.

---

### 5.2 Cálculo de Dominio (Mastery)

**En ExerciseViewModel (Android):**
```kotlin
val mastery = if (totalItems > 0) (mastered.toFloat() / totalItems * 100) else 0f
```

**En Backend:**
No se calcula explícitamente, se usa el valor enviado por la app.

**Definición de "mastered":**
```kotlin
val newMastered = if (correct) state.masteredItems + itemKey else state.masteredItems
```
Un item se agrega a `masteredItems` cuando se responde correctamente (ya sea en primer intento o en reintento).

---

### 5.3 Cálculo de Estado (Status)

**En ExerciseViewModel (Android):**
```kotlin
val status = when {
    attempted == 0 -> "not_started"
    mastered >= totalItems && totalItems > 0 -> "mastered"
    state.isFinished -> "completed"
    else -> "in_progress"
}
```

**Lógica:**
1. Si no hay items intentados → `not_started`
2. Si todos los items están dominados → `mastered`
3. Si el ejercicio está finalizado pero no todos dominados → `completed`
4. En cualquier otro caso → `in_progress`

---

## 6. Sincronización de Datos

### 6.1 Flujo de Sincronización

1. **Durante el ejercicio:**
   - Cada respuesta se guarda localmente en `answer_history`
   - El progreso se actualiza localmente en `progress` y `block_progress`
   - Se envía al backend cada 3 segundos (debounce)

2. **Al iniciar la app:**
   - Se descarga el progreso del backend (`loadRemoteProgress`)
   - Si no existe registro local, se crea desde el backend
   - Si existe registro local, se mantiene (el local tiene prioridad)

3. **Al resetear progreso:**
   - Se borra localmente
   - Se envía sincronización vacía al backend
   - El backend actualiza sus registros

---

### 6.2 Resolución de Conflictos

**Política actual:** El progreso local tiene prioridad sobre el remoto.

**Razón:** El estudiante puede estar trabajando offline, y su progreso local es la fuente de verdad más reciente.

**Excepción:** Si no existe registro local, se crea desde el remoto (útil cuando el estudiante cambia de dispositivo).

---

## 7. Objetivos Pedagógicos

### 7.1 Para el Estudiante
- **Motivación:** Ver el progreso visual motiva a continuar
- **Autoconciencia:** Las métricas de precisión y dominio ayudan a identificar áreas de mejora
- **Satisfacción:** El estado "DOMINADO" proporciona una sensación de logro

### 7.2 Para el Administrador/Padre
- **Supervisión:** Ver el progreso real del estudiante
- **Identificación de problemas:** Las unidades débiles y errores comunes muestran dónde necesita ayuda
- **Evaluación:** La precisión y dominio proporcionan métricas objetivas del aprendizaje
- **Intervención:** Los datos permiten decidir qué temas necesitan repaso

---

## 8. Recomendaciones de Mejora

### 8.1 Unificar Cálculos
- Alinear la fórmula de precisión entre app y backend
- Documentar claramente qué incluye cada métrica

### 8.2 Visualización
- Agregar gráficos de tendencia temporal (¿mejora con el tiempo?)
- Mostrar comparativas (esta semana vs. semana anterior)

### 8.3 Gamificación
- Badges por logros (ej: "5 unidades dominadas")
- Racha de días consecutivos de estudio
- Metas semanales personalizadas

### 8.4 Reportes
- Generar reportes PDF semanales/mensuales
- Enviar notificaciones de progreso a padres/administradores

---

## 9. Conclusión

El sistema de estadísticas de AppEnglish proporciona métricas pedagógicas significativas que van más allá del simple "porcentaje completado". Las métricas de **precisión** y **dominio** permiten distinguir entre:
- Un estudiante que completa ejercicios pero no los entiende (alta completitud, baja precisión)
- Un estudiante que entiende bien pero no termina (baja completitud, alta precisión)
- Un estudiante que realmente ha aprendido (alta completitud, alto dominio)

Estas distinciones son cruciales para una evaluación educativa efectiva y para proporcionar intervenciones pedagógicas apropiadas.
