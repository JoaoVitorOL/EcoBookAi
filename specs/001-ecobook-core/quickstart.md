# Quickstart Guide: EcoBook IA

**Phase**: 1-10 runtime  
**Date**: 2026-05-23  
**Purpose**: Boot the current backend, validate the implemented auth/onboarding/search flow, and compile/test the Android app with the current local runbook.

---

## Prerequisites

### Backend

- Java 21+
- Maven 3.9+
- PostgreSQL 15+ only if you want the default profile

### Android

- Java 17 or 21
- Android Studio
- Android SDK Platform 34
- Emulator or physical device

---

## Recommended End-To-End Path

The recommended first boot is still the `local` backend profile.

Why this path:

- no Docker dependency
- H2-backed local database
- Gemini mock enabled
- matches the root README validation order
- revalidated again on `2026-05-23`

### 1. Start the backend

From the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Run-BackendLocal.ps1 -JavaHome "C:\Program Files\Java\jdk-26"
```

If port `8080` is already in use:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Run-BackendLocal.ps1 -JavaHome "C:\Program Files\Java\jdk-26" -Port 8081
```

### 2. Validate health

```powershell
Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8080/api/v1/health
```

Expected response excerpt:

```json
{
  "status": 200,
  "message": "Backend online"
}
```

### 3. Configure Android local properties

Use `EcoBookAiAndroid/local.properties.example` as the base:

```properties
sdk.dir=C\:\\Users\\yourname\\AppData\\Local\\Android\\Sdk
backend.url=http://10.0.2.2:8080/api
```

If your backend runs inside WSL and the emulator cannot reach `10.0.2.2`, replace the host with the current WSL IP.

### 4. Compile and validate Android

```powershell
cd EcoBookAiAndroid
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:assembleDebug
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:lintDebug
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest
```

Runtime note:

- the ASCII-path wrapper is the recommended path for this Windows workspace
- on `2026-05-23`, the wrapper was hardened against concurrent temporary-drive alias races

### 5. Run the app

1. Open `EcoBookAiAndroid` in Android Studio.
2. Wait for Gradle sync.
3. Start an API 34 emulator or connect a physical device.
4. Run the debug app.

---

## Verified API Smoke Flow

The following flow remains the simplest API-level proof that the local profile is healthy.

### 1. Register

```bash
curl -X POST http://127.0.0.1:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "quickstart@example.com",
    "password": "SenhaSegura123",
    "nome": "Quickstart User"
  }'
```

Expected result:

- HTTP `201`
- JWT returned
- `perfil_completo = false`

### 2. Load current profile

```bash
curl -X GET http://127.0.0.1:8080/api/v1/usuarios/me \
  -H "Authorization: Bearer <jwt>"
```

### 3. Complete onboarding fields

```bash
curl -X PUT http://127.0.0.1:8080/api/v1/usuarios/me \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Quickstart User",
    "whatsapp": "+5511999999999",
    "cidade": "Florianopolis",
    "bairro": "Centro",
    "consentimento_ia": true,
    "necessidades_academicas": ["TEXTBOOKS", "WORKBOOKS"]
  }'
```

Expected result:

- HTTP `200`
- `perfil_completo = true`
- normalized `cidade` and `bairro`

### 4. Run a first discovery query

```bash
curl -X GET "http://127.0.0.1:8080/api/v1/materiais?page=0&size=5" \
  -H "Authorization: Bearer <jwt>"
```

Expected result:

- HTTP `200`
- paged envelope
- empty list is acceptable on a fresh database

### 5. Login again

```bash
curl -X POST http://127.0.0.1:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "quickstart@example.com",
    "password": "SenhaSegura123"
  }'
```

Expected result:

- HTTP `200`
- JWT returned again

---

## Optional Paths

### Optional admin bootstrap

If you want to exercise admin endpoints locally, start the backend with:

```powershell
$env:ADMIN_BOOTSTRAP_ENABLED = 'true'
$env:ADMIN_BOOTSTRAP_EMAIL = 'admin@example.com'
$env:ADMIN_BOOTSTRAP_PASSWORD = 'SenhaAdmin123'
$env:ADMIN_BOOTSTRAP_NAME = 'Admin Local'
mvn --% spring-boot:run -Dspring-boot.run.profiles=local
```

### Default PostgreSQL profile

From the repository root:

```powershell
docker compose up -d postgres
```

Then:

```bash
cd EcoBookAiBackend
mvn spring-boot:run
```

This profile expects:

- `jdbc:postgresql://localhost:5432/ecobook`
- user `ecobook`
- password `dev_password_123` unless overridden by `DB_PASSWORD`

### Real Firebase validation

With the backend already running and Firebase credentials configured:

```powershell
cd EcoBookAiAndroid
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.ecobook.fcm.FirebaseRealDeviceValidationTest'
```

---

## Current Closeout Notes

- The repository is now accurate to describe as `Phase 10 complete`.
- The remaining follow-ups are operational or governance items, not implementation blockers:
  - legal review of the MVP terms/privacy text
  - periodic Android dependency/`targetSdk` refresh
  - monitoring of the residual dependency-risk baseline documented in `docs/security-scan.md`
