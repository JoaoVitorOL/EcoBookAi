# Quickstart Guide: EcoBook IA Development

**Phase**: 1-2 baseline  
**Date**: 2026-05-05  
**Purpose**: Boot the current backend and validate the implemented email/password auth flow

---

## Prerequisites

### Backend

- Java 21+ compatible JDK
- Maven
- PostgreSQL 14+ for the main profile, or H2 for the local profile

### Android

- Android Studio
- Android SDK 34
- Emulator or physical device

---

## Backend Setup

### 1. Choose a runtime profile

**PostgreSQL profile**
- Uses Flyway migrations
- Default server port: `8080`

**Local H2 profile**
- Uses a local file database and compatibility bootstrap
- Useful for fast local auth testing

### 2. Configure environment

The current backend reads these keys directly:

```bash
DB_PASSWORD=dev_password_123
JWT_SECRET=change-this-jwt-secret-in-production-and-keep-it-at-least-64-characters-long
SERVER_PORT=8080
GEMINI_API_KEY=
```

### 3. Run backend

**PostgreSQL**

```bash
cd EcoBookAiBackend
mvn spring-boot:run
```

**Local H2**

```bash
cd EcoBookAiBackend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 4. Validate health

```bash
curl http://localhost:8080/api/v1/health
```

Expected response:

```json
{
  "status": "UP"
}
```

---

## Android Setup

### 1. Configure local properties

Set the emulator override in `EcoBookAiAndroid/local.properties`:

```properties
sdk.dir=C\:\\Users\\yourname\\AppData\\Local\\Android\\Sdk
backend.url=http://10.0.2.2:8080/api
```

### 2. Firebase note

`google-services.json` is now relevant only for Firebase Messaging and notification tests. Authentication no longer depends on Google Sign-In.

### 3. Build app

```bash
cd EcoBookAiAndroid
./gradlew assembleDebug
```

---

## First Authentication Flow Test

### 1. Register account

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SenhaSegura123",
    "nome": "Test User"
  }'
```

Expected result:
- user created
- `perfil_completo = false`
- JWT returned

### 2. Save token and inspect profile

```bash
TOKEN="jwt-token-value"

curl -X GET http://localhost:8080/api/v1/usuarios/me \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Complete onboarding fields

```bash
curl -X PUT http://localhost:8080/api/v1/usuarios/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Test User",
    "whatsapp": "+5548999999999",
    "cidade": "Florianopolis",
    "bairro": "Centro",
    "consentimento_ia": false,
    "necessidades_academicas": ["TEXTBOOKS"]
  }'
```

Expected result:
- normalized geography
- `perfil_completo = true`
- `consentimento_ia` can remain `false` and be changed later through `PUT /api/v1/usuarios/me`

### 4. Login with the same credentials

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SenhaSegura123"
  }'
```

Expected result:
- JWT returned
- same user profile loaded

---

## Troubleshooting

### Backend

| Issue | Cause | Solution |
|-------|-------|----------|
| `Connection refused: 5432` | PostgreSQL not running | Start PostgreSQL or use the `local` H2 profile |
| `JWT validation failed` | Expired token or wrong secret | Re-login via `POST /api/v1/auth/login` |
| `Email ou senha invalidos` | Wrong email/password | Confirm user exists and password matches |
| `Port 8080 already in use` | Another service using the port | Stop the existing process or change `SERVER_PORT` |
| `Este email ja esta cadastrado` | Existing account uses the same email | Login instead of registering, or run a manual account recovery flow |

### Android

| Issue | Cause | Solution |
|-------|-------|----------|
| Emulator cannot reach backend | Wrong host alias | Use `10.0.2.2` instead of `localhost` |
| App returns to login after request | API responded `401` | Check JWT storage and backend secret |
| Auth request fails after backend change | App still points to old port | Rebuild after updating `backend.url` |

---

## Current Focus

1. Close the remaining Phase 2 debt around checklist/status cleanup and broader automated coverage.
2. Keep consent updates on `PUT /usuarios/me` unless a dedicated endpoint is intentionally added later.
3. Start the real `/materiais/preview` implementation path for Phase 3.
4. Continue the remaining MVP work on material listing, matching, and request flows.
