# EcoBook AI - Deployment Guide

**Status**: MVP deployment runbook  
**Date**: 2026-05-23

---

## 1. Backend package

Requirements:

- Java 21 or superior
- Maven 3.9+

Command:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
cd EcoBookAiBackend
mvn clean package
```

Artifact:

- `target/ecobook-backend-1.0.0-SNAPSHOT.jar`

---

## 2. Android build

Command:

```powershell
cd EcoBookAiAndroid
.\gradlew.bat assembleRelease
```

Recommended release path:

- start with internal testing
- validate login, onboarding, preview, request flow and notifications before any wider track

---

## 3. Backend runtime variables

Minimum sensitive/runtime inputs:

- `DB_PASSWORD`
- `JWT_SECRET`
- `GEMINI_API_KEY`
- `FIREBASE_SERVICE_ACCOUNT_PATH`
- `FIREBASE_DATABASE_URL`
- `STORAGE_UPLOAD_DIR`

Recommended production notes:

- keep `JWT_SECRET` outside version control
- isolate upload storage in a persistent directory/volume
- keep Actuator protected at the network or gateway layer

---

## 4. Database rollout

Production-like path:

- PostgreSQL 15+
- Flyway migrations on startup

Checklist:

1. provision the database
2. set datasource variables
3. start the backend
4. confirm migrations completed successfully
5. validate `/api/v1/health` and `/actuator/health`

---

## 5. Firebase-enabled deployment

For real push delivery:

1. configure the Firebase Admin SDK credential
2. confirm the Android app is using the correct `google-services.json`
3. validate foreground and background receipt on a real device or compatible emulator

Until that validation is complete, the inbox API remains the reliable fallback notification surface.

---

## 6. Recommended release checklist

1. Run backend tests
2. Run Android JVM tests
3. Validate the smoke suite
4. Validate the E2E suite
5. Confirm database migration startup
6. Confirm upload storage permissions
7. Confirm push behavior or document fallback-only release scope
