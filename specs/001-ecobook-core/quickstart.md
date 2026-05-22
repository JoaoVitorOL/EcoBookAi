# Quickstart Guide: EcoBook IA

**Phase**: 1-8 runtime  
**Date**: 2026-05-21  
**Purpose**: Boot the current backend, validate the implemented auth/onboarding/search flow, and compile the Android app with the current local runbook.

---

## Prerequisites

### Backend

- Java 21
- Maven 3.9+
- PostgreSQL 15+ only if you want the default profile

### Android

- Java 17 or 21
- Android Studio
- Android SDK Platform 34
- Emulator or physical device

---

## Recommended Backend Path

The recommended first boot is the `local` profile.

Why this path:

- no Docker dependency
- H2-backed local database
- Gemini mock enabled
- validated end to end on `2026-05-21`

### 1. Compile

```bash
cd EcoBookAiBackend
mvn -q -DskipTests compile
```

### 2. Start the backend

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

On Windows PowerShell, prefer:

```powershell
$env:JAVA_HOME = 'C:\caminho\para\jdk-21-ou-superior'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn --% spring-boot:run -Dspring-boot.run.profiles=local
```

### 3. Validate health

```bash
curl http://127.0.0.1:8080/api/v1/health
```

Expected response excerpt:

```json
{
  "status": 200,
  "message": "Backend online"
}
```

### 4. Optional: bootstrap an admin account

If you want to exercise the admin endpoints locally, start the backend with these environment variables:

```powershell
$env:ADMIN_BOOTSTRAP_ENABLED = 'true'
$env:ADMIN_BOOTSTRAP_EMAIL = 'admin@example.com'
$env:ADMIN_BOOTSTRAP_PASSWORD = 'SenhaAdmin123'
$env:ADMIN_BOOTSTRAP_NAME = 'Admin Local'
mvn --% spring-boot:run -Dspring-boot.run.profiles=local
```

Runtime behavior:

- if the email already exists, the account is promoted to `ADMIN`
- if the email does not exist, a lightweight admin account is created
- leaving `ADMIN_BOOTSTRAP_ENABLED=false` keeps the feature inactive

---

## Verified API Smoke Flow

The following flow was revalidated against the `local` profile.

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
- if you rerun the same flow against the same local database, use a different email or clear `EcoBookAiBackend/data/ecobook-local*`

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

## Android Setup

### 1. Configure local properties

Use `EcoBookAiAndroid/local.properties.example` as the base:

```properties
sdk.dir=C\:\\Users\\yourname\\AppData\\Local\\Android\\Sdk
backend.url=http://10.0.2.2:8080/api
```

If your backend runs inside WSL and the emulator cannot reach `10.0.2.2`, replace the host with the current WSL IP.

### 2. Compile and validate

```powershell
cd EcoBookAiAndroid
.\gradlew.bat app:compileDebugKotlin
.\gradlew.bat assembleDebug
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest
```

### 3. Optional instrumentation

With an emulator already started:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ecobook.auth.AuthFlowE2ETest
```

---

## Default PostgreSQL Profile

Use this profile when you need Flyway + PostgreSQL parity.

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

Environment note:

- the fully revalidated quickstart in this repository is still the `local` profile
- use the default profile only when the backend process can really reach that same `localhost:5432`, or override `SPRING_DATASOURCE_URL` with a reachable host

---

## Current Closeout Notes

- Phase 6 notification runtime is implemented, but real-device Firebase validation is still the main remaining closeout step.
- Phase 7 admin/moderation runtime is already live on the backend.
- Phase 8 has started through AI consent update/revocation plus preview enforcement; consent history, deletion/anonymization and image-access hardening remain backlog work.
