# Cómo funciona y cómo deployar AppSchool

## ¿Qué hace cada cosa?

```
appSchool/
├── backend/          ← El cerebro. API REST que guarda todo en PostgreSQL.
├── admin-panel/      ← Panel web para vos (el profe). Creás ejercicios, ves progreso.
├── app-android/      ← App del celu de tu hijo. Solo consume la API, no toca nada más.
```

---

## Requisito único: Docker

Necesitás Docker instalado en el VPS. Si no lo tenés:

```bash
curl -fsSL https://get.docker.com | sh
```

Nada más. **No necesitás Python, Node.js, PostgreSQL ni nada.** Docker se encarga de todo.

---

## Deploy (primera vez)

```bash
# 1. Clonar el proyecto en el VPS
cd /opt
git clone git@github.com:assrub/appSchool.git
cd appSchool/backend

# 2. Levantar todo (tarda unos minutos la primera vez)
docker compose up -d --build
```

Eso levanta **3 containers** automáticamente:

| Container | Puerto | Qué es |
|-----------|--------|--------|
| `api` | `8000` | FastAPI: la app Android y el admin panel lo usan |
| `admin` | `9000` | Panel web para administrar contenido |
| `db` | (interno) | PostgreSQL: guarda ejercicios, progreso, diccionario |

La primera vez, automáticamente:
- Crea las tablas en la base de datos
- Carga los 185 ejercicios del verbo "to be"
- Crea el usuario admin para el panel

---

## Actualizar (cuando haya cambios nuevos)

```bash
cd /opt/appSchool/backend
git pull
docker compose down
docker compose up -d --build
```

Los datos (progreso del nene, ejercicios) **no se pierden** porque PostgreSQL guarda todo en un volumen Docker.

---

## Acceder

| URL | Para qué |
|-----|----------|
| `http://TU_IP:8000/docs` | Documentación de la API (Swagger) |
| `http://TU_IP:9000` | Panel de administración |

### Login del panel admin

```
Usuario: admin
Contraseña: admin123
```

> **Cambiá la contraseña** apenas puedas. Está hardcodeada para el primer deploy.

---

## Endpoints principales de la API

La app Android usa estos endpoints. No necesitás saberlos, pero por las dudas:

| Endpoint | Qué devuelve |
|----------|-------------|
| `GET /api/v1/content/subjects` | Lista de materias (Inglés, etc.) |
| `GET /api/v1/content/topics/verb-to-be` | Contenido completo del verbo to be |
| `GET /api/v1/content/topics/verb-to-be/test` | Test final aleatorio |
| `POST /api/v1/tts` | Texto a voz (inglés) |
| `POST /api/v1/translate` | Traducción EN→ES |
| `POST /api/v1/progress/sync` | Sincronizar progreso del nene |

---

## Problemas comunes

### "El panel admin no carga" (puerto 9000)
Asegurate que el puerto 9000 esté abierto en el firewall del VPS:
```bash
sudo ufw allow 9000
```

### "La app Android no conecta"
Verificá que la IP en `app-android/app/src/main/java/com/appenglish/util/ApiConfig.kt` sea la correcta:
```kotlin
const val BASE_URL = "http://2.25.142.139:8000/api/v1/"
```

### "Quiero resetear todo y empezar de cero"
```bash
cd /opt/appSchool/backend
docker compose down -v   # -v borra la base de datos
docker compose up -d --build
```

---

## Comandos útiles

```bash
# Ver logs de la API
docker compose logs api -f

# Ver logs del admin panel
docker compose logs admin -f

# Reiniciar solo la API
docker compose restart api

# Ver si todo está corriendo
docker compose ps
```
