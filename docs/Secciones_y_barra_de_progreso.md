# Secciones y Barras de Progreso - AppEnglish

## Resumen General

Este documento explica la estructura jerárquica de la aplicación AppEnglish y cómo funcionan las barras de progreso en cada nivel. La app sigue una estructura de 5 niveles: **Materias → Temas → Unidades → Bloques → Ejercicios**.

---

## 1. Estructura Jerárquica

```
📚 MATERIA (ej: Inglés)
├── 📝 TEMA (ej: Verbo To Be)
│   ├── 📖 UNIDAD (ej: Afirmativo)
│   │   ├── 📋 BLOQUE (ej: Bloque 1)
│   │   │   ├── ✏️ EJERCICIO 1
│   │   │   ├── ✏️ EJERCICIO 2
│   │   │   └── ✏️ EJERCICIO 3
│   │   ├── 📋 BLOQUE (ej: Bloque 2)
│   │   │   ├── ✏️ EJERCICIO 1
│   │   │   └── ✏️ EJERCICIO 2
│   │   └── 📋 BLOQUE (ej: Bloque 3)
│   │       └── ...
│   ├── 📖 UNIDAD (ej: Negativo)
│   │   └── ...
│   └── 📖 UNIDAD (ej: Interrogativo)
│       └── ...
└── 📝 TEMA (ej: Present Simple)
    └── ...
```

---

## 2. Sección Materias (SubjectsScreen)

### Propósito
Punto de entrada principal de la app. Muestra todas las materias disponibles para el estudiante.

### Contenido
- **Icono** de la materia
- **Nombre** de la materia
- **Cantidad de temas** disponibles

### Navegación
Al tocar una materia → se muestra la lista de temas de esa materia.

### Barra de Progreso
**No tiene barra de progreso propia.** La vista es una lista simple de materias.

---

## 3. Sección Temas (TopicsScreen)

### Propósito
Muestra todos los temas dentro de una materia. Cada tema agrupa varias unidades relacionadas.

### Contenido por Tema
- **Icono** del tema
- **Nombre** del tema
- **Nivel de dificultad** (1-5 estrellas)
- **Barra de progreso**
- **Porcentaje** (x%)
- **Contador** (x/n unidades)

### Barra de Progreso

**Fórmula:**
```
progreso = unidades_completadas / total_unidades
```

**Cálculo del porcentaje:**
```
porcentaje = (unidades_completadas / total_unidades) × 100
```

**Ejemplo:**
- Tema "Verbo To Be" tiene 4 unidades
- El estudiante ha completado 2 unidades
- Progreso: 2/4 = 50%
- Se muestra: barra al 50%, "50%", "2/4 unidades"

**Lógica de "completada":**
Una unidad se considera completada cuando:
- `completed == true` en la tabla `progress`
- O cuando el dominio (`mastery`) ≥ 90% (estado "mastered")

**Colores de la barra:**
- **Verde** (#4CAF50): Progreso normal
- La barra se llena de izquierda a derecha según el porcentaje

### Propósito Pedagógico
Permite al estudiante ver de un vistazo qué tan avanzado está cada tema. Los temas con bajo porcentaje necesitan más atención.

---

## 4. Sección Unidades (UnitsScreen)

### Propósito
Muestra las unidades dentro de un tema. Cada unidad agrupa varios bloques de ejercicios.

### Contenido por Unidad
- **Icono** de la unidad
- **Nombre** de la unidad
- **Tipo de ejercicio** (Completar, Opción múltiple, etc.)
- **Barra de progreso**
- **Porcentaje** (x%)
- **Contador** (x/n items)

### Barra de Progreso

**Fórmula:**
```
progreso = items_completados / total_items
```

**Cálculo del porcentaje:**
```
porcentaje = (items_completados / total_items) × 100
```

**Ejemplo:**
- Unidad "Afirmativo" tiene 37 ejercicios en total
- El estudiante ha completado 20 ejercicios
- Progreso: 20/37 = 54%
- Se muestra: barra al 54%, "54%", "20/37 items"

**Lógica de "items_completados":**
`completedItems` se incrementa cada vez que el estudiante responde un ejercicio (correcta o incorrectamente). Esto cuenta el número de ejercicios **intentados**, no los correctos.

**Colores de la barra:**
- **Verde** (#4CAF50): Progreso normal

### Propósito Pedagógico
Muestra cuánto del contenido de una unidad ha sido abordado. Un estudiante puede tener 100% de avance pero baja precisión si respondió muchos ejercicios incorrectamente.

---

## 5. Sección Bloques (BlocksScreen)

### Propósito
Muestra los bloques dentro de una unidad. Cada bloque es un grupo de ejercicios que se presentan juntos.

### Contenido por Bloque
- **Título** del bloque
- **Barra de progreso**
- **Porcentaje** (x%)
- **Contador** (x/n ejercicios)
- **Respuestas válidas** (✅ x)
- **Respuestas inválidas** (❌ x)

### Barra de Progreso

**Fórmula:**
```
progreso = completed_items / total_items
```

**Cálculo del porcentaje:**
```
porcentaje = (completed_items / total_items) × 100
```

**Ejemplo:**
- Bloque "PASO 1: Pronombres" tiene 8 ejercicios
- El estudiante ha completado 5 ejercicios (3 correctos, 2 incorrectos)
- Progreso: 5/8 = 62%
- Se muestra: barra al 62%, "62%", "5/8 ejercicios", "✅ 3 ❌ 2"

**Lógica de conteo:**
- `completed_items` = ejercicios intentados (correctos + incorrectos)
- `score` = ejercicios respondidos correctamente
- `completed_items - score` = ejercicios respondidos incorrectamente

**Colores de la barra:**
- **Verde** (#4CAF50): Progreso normal

### Propósito Pedagógico
Muestra el progreso detallado a nivel de bloque. Los indicadores ✅ y ❌ ayudan al estudiante a identificar rápidamente su rendimiento en ese bloque específico.

---

## 6. Sección Ejercicios (ExerciseScreen)

### Propósito
Pantalla principal de práctica donde el estudiante interactúa con los ejercicios.

### Contenido
- **Título del bloque actual**
- **Barra de progreso del bloque**
- **Contador** (x/n ejercicios)
- **Ejercicio actual** (oración con hueco, opciones, etc.)
- **Botones de acción** (Corregir, Siguiente, etc.)

### Barra de Progreso del Bloque

**Fórmula:**
```
progreso = current_block_completed_items / current_block_total_items
```

**Ejemplo:**
- Bloque tiene 8 ejercicios
- El estudiante ha completado 3 ejercicios
- Progreso: 3/8 = 37%
- Se muestra: barra al 37%, "3/8"

**Lógica de tracking:**
- `currentBlockCompletedItems` se incrementa con cada respuesta (correcta o incorrecta)
- `currentBlockScore` se incrementa solo con respuestas correctas
- Al cambiar de bloque, se reinician estos contadores

**Colores de la barra:**
- **Verde** (#4CAF50): Progreso normal

### Feedback de Ejercicio

**Respuesta Correcta:**
- Barra flotante verde en la parte inferior
- Muestra "¡Correcto!"
- Muestra la oración completa
- Botón "Siguiente"

**Respuesta Incorrecta:**
- Barra flotante roja en la parte inferior
- Muestra "Incorrecto"
- Muestra "Respuesta correcta: [respuesta]"
- Botón "Aceptar"

### Propósito Pedagógico
Proporciona retroalimentación inmediata durante la práctica. La barra de progreso muestra cuántos ejercicios quedan en el bloque actual.

---

## 7. Sección Test Final (FinalTestScreen)

### Propósito
Evaluación final que mezcla ejercicios de todas las unidades de un tema.

### Contenido
- **Barra de progreso** del test
- **Contador** (x/n preguntas)
- **Puntuación actual**
- **Pregunta actual**
- **Opciones de respuesta**

### Barra de Progreso

**Fórmula:**
```
progreso = pregunta_actual / total_preguntas
```

**Ejemplo:**
- Test tiene 20 preguntas
- El estudiante está en la pregunta 7
- Progreso: 7/20 = 35%
- Se muestra: barra al 35%, "7/20"

**Lógica de scoring:**
- El estudiante puede avanzar aunque se equivoque
- El score refleja respuestas correctas reales
- Si responde 15 de 20 correctamente, el score es 15/20 = 75%

**Colores de la barra:**
- **Verde** (#4CAF50): Progreso normal

### Propósito Pedagógico
Evalúa el dominio general del tema. A diferencia de los ejercicios normales, el test final permite al estudiante avanzar sin acertar, proporcionando una evaluación más realista.

---

## 8. Sección "Mi Progreso" (ProgressScreen)

### Propósito
Vista consolidada de todo el progreso del estudiante.

### Contenido

#### Tarjeta de Resumen
- **Porcentaje general** de completitud
- **Unidades completadas** / Total unidades
- **Barra de progreso general**
- **Precisión promedio** (%)
- **Dominio promedio** (%)
- **Unidades dominadas** (cantidad)
- **Tiempo total** de estudio

#### Jerarquía de Progreso
```
📚 Materia
└── 📝 Tema (x/n unidades)
    └── 📖 Unidad
        ├── Barra de avance (items_intentados / total_items)
        ├── Avance: x%
        ├── Precisión: x%
        ├── Dominio: x%
        ├── Estado: NO INICIADO / EN PROGRESO / COMPLETADO / DOMINADO
        └── Puntuación: x/n
```

### Propósito Pedagógico
Proporciona una visión completa del progreso del estudiante en todas las materias, temas y unidades. Ayuda a identificar fortalezas y debilidades a nivel global.

---

## 9. Sección "Progreso de Estudiantes" (Admin Panel)

### Propósito
Permite a los administradores/padres supervisar el progreso de los estudiantes.

### Contenido

#### Vista General
- **Porcentaje de completitud** del estudiante
- **Unidades completadas** / Total unidades
- **Barra de progreso general**

#### Detalle por Unidad
- **Estado** (not_started / in_progress / completed / mastered)
- **Puntuación** (score / total_items)
- **Items completados** (completed_items / total_items)
- **Test Score** (si existe)
- **Precisión** (%)
- **Dominio** (%)
- **Items intentados**
- **Items dominados**
- **Tiempo dedicado**
- **Conteo de respuestas correctas/incorrectas**

#### Análisis
- **Unidades débiles** (mayor tasa de error)
- **Unidades fuertes** (menor tasa de error)
- **Errores comunes** (patrones de errores)
- **Ejercicios más difíciles**
- **Mejora por reintentos**

### Propósito Pedagógico
Proporciona a los administradores/padres herramientas para supervisar y evaluar el progreso del estudiante. Permite identificar áreas que necesitan intervención pedagógica.

---

## 10. Resumen de Fórmulas

| Sección | Barra de Progreso | Porcentaje | Contador |
|---------|-------------------|------------|----------|
| **Temas** | `unidades_completadas / total_unidades` | `(unidades_completadas / total_unidades) × 100` | `x/n unidades` |
| **Unidades** | `completed_items / total_items` | `(completed_items / total_items) × 100` | `x/n items` |
| **Bloques** | `completed_items / total_items` | `(completed_items / total_items) × 100` | `x/n ejercicios` |
| **Ejercicios** | `current_block_completed_items / current_block_total_items` | `(current_block_completed_items / current_block_total_items) × 100` | `x/n` |
| **Test Final** | `pregunta_actual / total_preguntas` | `(pregunta_actual / total_preguntas) × 100` | `x/n` |

---

## 11. Diferencia entre Métricas

### Avance vs Precisión vs Dominio

| Métrica | Qué mide | Fórmula | Interpretación |
|---------|----------|---------|----------------|
| **Avance** | Cuánto contenido se ha abordado | `items_intentados / total_items` | Progreso de cobertura |
| **Precisión** | Qué tan bien se responde en el primer intento | `correctas_primer_intento / items_intentados` | Comprensión inicial |
| **Dominio** | Qué se ha aprendido realmente | `items_dominados / total_items` | Aprendizaje real |

**Ejemplo práctico:**
- Estudiante A: 100% avance, 50% precisión, 60% dominio
  - Intentó todos los ejercicios, falló mucho al inicio, pero aprendió con los reintentos
- Estudiante B: 50% avance, 90% precisión, 45% dominio
  - Solo hizo la mitad, pero lo hizo muy bien
- Estudiante C: 100% avance, 90% precisión, 90% dominio
  - Excelente rendimiento general

---

## 12. Estados de las Barras de Progreso

### Colores y Significado

| Color | Significado | Uso |
|-------|-------------|-----|
| **Verde** (#4CAF50) | Progreso normal | Todas las barras de progreso |
| **Verde oscuro** (#2E7D32) | Completado con éxito | Indicadores de estado |
| **Púrpura** (#9C27B0) | Dominado | Indicadores de dominio |
| **Azul** (#2196F3) | Información | Precisión |
| **Naranja** (#FFA726) | Advertencia | Errores / Necesita repaso |
| **Rojo** (#E53935) | Error | Respuestas incorrectas |

### Animaciones
- Las barras de progreso usan animación `tween` de 600ms
- Transición suave de 0% al porcentaje actual
- Mejora la percepción visual del progreso

---

## 13. Conclusión

El sistema de progreso de AppEnglish está diseñado para proporcionar información clara y útil en cada nivel de la jerarquía educativa. Las barras de progreso y métricas ayudan a:

1. **Estudiantes:** Visualizar su avance y rendimiento
2. **Administradores/Padres:** Supervisar el progreso y identificar áreas de mejora
3. **Sistema:** Tomar decisiones pedagógicas basadas en datos

La clave es que cada métrica tiene un propósito específico y juntas proporcionan una visión completa del proceso de aprendizaje.
