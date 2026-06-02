# Guía de Debugging - AppSchool

## Acceso al Admin Panel
- **URL:** http://2.25.142.139:9000
- Login con credenciales de administrador

## Sección Sistema (Admin Panel)
Desde el sidebar, click en **⚙️ Sistema**. Contiene 4 pestañas:

### 📋 Logs
- Logs en vivo de los contenedores Docker
- Selector de contenedor: `api`, `admin`, `db`
- Auto-refresh cada 5 segundos
- Botón "Refrescar" para forzar actualización
- **Útil para:** ver errores 500, tracebacks, problemas de conexión

### 🗄️ DB Explorer
- Lista todas las tablas de PostgreSQL con cantidad de filas
- Click en una tabla para ver:
  - Schema (columnas, tipos, nullable)
  - Preview (primeros 20 registros)
- **Útil para:** verificar datos en `progress`, `answer_history`, `block_progress`

### 💻 SQL Console
- Editor de queries SELECT
- Ejecuta consultas directas a la base de datos
- Muestra resultados en tabla con tiempo de ejecución
- **Solo permite SELECT** (no INSERT/UPDATE/DELETE/DROP)
- Límite: 200 filas por consulta
- **Útil para:** consultas personalizadas como:
  ```sql
  SELECT * FROM progress WHERE user_id = 1;
  SELECT * FROM block_progress LIMIT 20;
  SELECT * FROM answer_history WHERE user_id = 1 ORDER BY answered_at DESC LIMIT 10;
  ```

### ❤️ Health
- Estado de la API
- Versión actual (versionCode + versionName)
- Quick actions para ir a Logs o DB

## Endpoints de Sistema (API)

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/v1/admin/system/health` | Health check |
| GET | `/api/v1/admin/system/logs?container=api&lines=100` | Logs de Docker |
| GET | `/api/v1/admin/system/db-tables` | Tablas con row counts |
| POST | `/api/v1/admin/system/db-query` | Ejecutar SELECT |
| GET | `/api/v1/admin/system/db-table/{table}` | Schema + preview |
| GET | `/api/v1/version` | Versión del backend |

## Credenciales del VPS

Las credenciales de acceso al servidor están en:

```
backend/.env.production
```

Este archivo **NO** se sube a GitHub (está en `.gitignore`).

### Formato del archivo

```env
# PostgreSQL
DATABASE_URL=postgresql+asyncpg://user:pass@db:5432/appenglish

# VPS SSH
SSH_HOST=2.25.142.139
SSH_USER=root
SSH_KEY_PATH=/path/to/ssh/key

# Docker
DOCKER_PROJECT_PATH=/docker/appSchool
```

### Cómo cambiar las credenciales

1. Editar `backend/.env.production`
2. Redeployar:
   ```bash
   cd backend
   docker compose down && docker compose up -d --build
   ```

## Tips de Debugging

### Verificar que el backend está vivo
```bash
curl http://2.25.142.139:8000/api/v1/version
# {"versionCode":80,"versionName":"3.2.51","apkUrl":"/uploads/app-release.apk"}
```

### Ver errores 500
Ir a Admin Panel → Sistema → Logs (seleccionar container `api`)

### Ver datos de progreso
Ir a Admin Panel → Sistema → SQL Console → ejecutar:
```sql
SELECT * FROM progress WHERE user_id = 1;
SELECT * FROM block_progress WHERE user_id = 1;
SELECT * FROM answer_history WHERE user_id = 1 LIMIT 10;
```

### Resolver problema de progreso en 0%
1. Verificar que el endpoint `/api/v1/progress/{user_id}` no devuelva 500
2. En SQL Console, verificar que existan registros en `progress` para ese usuario
3. Si no hay datos:
   - El usuario debe abrir la app actualizada (v80+)
   - Hacer al menos 1 ejercicio de un bloque
   - El progreso se sincronizará automáticamente
4. Si hay datos pero el admin muestra 0%:
   - Verificar en `progress_summary` que el `user_id` coincida
