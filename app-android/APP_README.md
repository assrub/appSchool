# AppEnglish Android App

## Requisitos

- Android Studio Hedgehog (2023.1.1) o superior
- JDK 17
- Gradle 8.5

## Configuración

La URL del backend se configura en:
`app/src/main/java/com/appenglish/util/ApiConfig.kt`

```kotlin
object ApiConfig {
    const val BASE_URL = "http://2.25.142.139:8000/api/v1/"
}
```

Cambiar la IP cuando se despliegue en el VPS.

## Build

```bash
# Debug APK
./gradlew assembleDebug

# Release APK  
./gradlew assembleRelease
```

La APK se genera en `app/build/outputs/apk/debug/app-debug.apk`

## Estructura

```
app/
├── MainActivity.kt          # Single Activity + NavHost
├── AppEnglishApp.kt         # Hilt Application
├── di/AppModule.kt          # Hilt DI: Retrofit, Room, Repos
├── data/
│   ├── remote/api/          # Retrofit interfaces
│   ├── remote/dto/          # Data Transfer Objects
│   ├── local/               # Room DB, DAOs, Entities
│   └── repository/          # Content, Progress, Dictionary
├── domain/model/            # Domain models
├── ui/
│   ├── theme/               # Material 3 colors & typography
│   ├── navigation/          # NavGraph
│   ├── screens/
│   │   ├── subjects/        # Pantalla principal
│   │   ├── topic/           # Tema con tabs
│   │   ├── exercise/        # Ejercicios fill-in-blank
│   │   ├── test/            # Test final
│   │   ├── dictionary/      # Diccionario
│   │   └── progress/        # Progreso
│   └── components/          # TtsButton, TranslatePopover
└── util/ApiConfig.kt        # URL del backend
```
