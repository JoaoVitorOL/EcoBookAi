# Quickstart Guide: EcoBook IA Development

**Phase**: 1 (Design & Contracts)  
**Date**: 2026-05-05  
**Purpose**: Setup local development environment and validate the first auth + profile flow

---

## Prerequisites

### Backend

- Java 17+ or 21+
- PostgreSQL 14+
- Maven or Gradle
- Docker (recommended for local PostgreSQL)
- Git

### Android

- Android Studio
- Android SDK 34
- Emulator or physical device

---

## Backend Setup

### 1. Start PostgreSQL

```bash
docker compose up -d postgres
```

### 2. Configure environment

Create `.env` in `EcoBookAiBackend/`:

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/ecobook
DATABASE_USER=ecobook
DATABASE_PASSWORD=dev_password_123
JWT_SECRET=your-super-secret-jwt-key-minimum-32-characters-long
GEMINI_API_KEY=your-gemini-api-key
FCM_PROJECT_ID=your-firebase-project-id
FCM_SERVICE_ACCOUNT_JSON=/path/to/firebase-service-account.json
```

### 3. Run backend

```bash
cd EcoBookAiBackend
mvn spring-boot:run
```

### 4. Validate health

```bash
curl http://localhost:8080/api/v1/health
```

Expected response:

```json
{ "status": "UP" }
```

---

## Android Setup

### 1. Configure local properties

Create `EcoBookAiAndroid/local.properties`:

```properties
sdk.dir=C\:\\Users\\yourname\\AppData\\Local\\Android\\Sdk
backend.url=http://10.0.2.2:8080/api
```

### 2. Firebase note

`google-services.json` is only relevant for Firebase Messaging tests. Authentication no longer depends on Google sign-in.

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
    "consentimento_ia": true
  }'
```

Expected result:
- normalized geography
- `perfil_completo = true`

### 4. Login with same credentials

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

## First Material Preview Test

After authentication and profile completion:

```bash
curl -X POST http://localhost:8080/api/v1/materiais/preview \
  -H "Authorization: Bearer $TOKEN" \
  -F "image=@sample-math-textbook.jpg"
```

Expected result:
- Gemini classification response
- `status_ia` returned
- `upload_id` generated

---

## Troubleshooting

### Backend

| Issue | Cause | Solution |
|-------|-------|----------|
| `Connection refused: 5432` | PostgreSQL not running | Start Docker compose or local PostgreSQL |
| `JWT validation failed` | Expired token or wrong secret | Re-login via `POST /api/v1/auth/login` |
| `Invalid credentials` | Wrong email/password | Confirm user exists and password matches |
| `Port 8080 already in use` | Another service using the port | Stop existing process or change `SERVER_PORT` |

### Android

| Issue | Cause | Solution |
|-------|-------|----------|
| Emulator cannot reach backend | Wrong host alias | Use `10.0.2.2` instead of `localhost` |
| App returns to login after request | API responded `401` | Check JWT storage and backend secret |
| Auth screen still shows Google flow | Codebase not fully migrated yet | Follow current docs as the target direction |

---

## Next Steps

1. Replace legacy Google-auth code paths with email/password screens and endpoints
2. Add auth integration tests for register/login
3. Update backend schema to persist `password_hash`
4. Validate logout and 401 recovery in Android
