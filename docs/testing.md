# EcoBook AI - Testing Guide

**Status**: current automated-testing map  
**Date**: 2026-05-23

---

## Test Structure

The repository currently follows a pragmatic pattern:

- **Arrange**: seed users, materials, requests or mock dependencies
- **Act**: call `MockMvc`, service methods or scheduler jobs
- **Assert**: verify HTTP contract, database side effects and state transitions

---

## Main Test Buckets

- `AuthControllerIntegrationTest`: register/login contract and password safety
- `UsuarioServiceTest`, `UsuarioControllerTest`, `UserDeletionWorkflowTest`: profile, consent and deletion flows
- `MaterialController*` tests: preview, create, discovery, owner actions and edge validation
- `SolicitacaoWorkflowTest`: request lifecycle and donor/student flows
- `ReservationExpiryJobTest`: time-based reservation release
- `SmokeTests`: operational baseline endpoints (`health`, `prometheus`, auth/profile/discovery and the public OpenAPI/Swagger surface)
- `E2ETests`: 20 consolidated end-to-end scenarios
- `LoadValidationTest`: explicit Phase 9 load/performance harness for `20` concurrent uploads + `30` concurrent searches, with JSON and Prometheus evidence under `target/load-reports/`
- `FirebaseRealDeviceValidationTest`: Android `connectedDebugAndroidTest` path that validates `FCM token -> backend sync -> persisted notification -> in-app receipt` against a real Firebase project on a Google Play-enabled emulator/device

---

## Runtime Notes Covered By Tests

- reference-data cache/fallback contract
- cursor pagination
- rollback cleanup for promoted files
- email change reauthentication
- visible consent/profile UX contract reflected by backend behavior
- account deletion reopening reserved materials when needed
- repeatable local load/performance evidence for the Phase 9 concurrency target
- real Firebase receipt on Android through `FirebaseRealDeviceValidationTest`

---

## Useful Commands

Backend full suite:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
cd EcoBookAiBackend
mvn test
```

Focused E2E suite:

```powershell
mvn --% -Dtest=E2ETests test
```

Focused load/performance validation (`T217`):

```powershell
mvn --% -Decobook.runLoadTest=true -Dtest=LoadValidationTest test
```

Latest validated local outcome on `2026-05-23`:

- `20` concurrent upload flows
- `30` concurrent search flows
- `522 ms` search p95
- `512 ms` upload p95
- `0%` error rate
- evidence written to `EcoBookAiBackend/target/load-reports/`

Android JVM tests:

```powershell
cd EcoBookAiAndroid
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest
```

Android lint / UI review gate:

```powershell
cd EcoBookAiAndroid
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:lintDebug
```

Android debug assemble:

```powershell
cd EcoBookAiAndroid
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:assembleDebug
```

Android real Firebase validation:

```powershell
cd EcoBookAiAndroid
powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.ecobook.fcm.FirebaseRealDeviceValidationTest'
```

Prerequisites for the Firebase validation:

- backend running locally at `http://10.0.2.2:8080/api`
- valid Firebase Admin SDK credential configured on the backend
- `app/google-services.json` present
- emulator/device with Google Play services and notification permission granted

Latest Android UI/code-quality note:

- the post-closeout UI review on `2026-05-23` left Android lint green with `0` errors and `20` non-blocking warnings; details live in `docs/android-ui-review.md`
- the same round also hardened `Invoke-GradleAsciiPath.ps1` against temporary-drive allocation races, so the recommended Windows path is now more stable for repeated local runs
