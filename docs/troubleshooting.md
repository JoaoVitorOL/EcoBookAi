# EcoBook AI - Troubleshooting

**Status**: current runtime guide  
**Date**: 2026-05-23

---

## JWT expired

Symptoms:

- `401 UNAUTHORIZED`
- user was previously logged in, but requests now fail

Checks:

- login again and confirm the app saved the new token
- if the user changed the account email, the old token is expected to stop working

Fix:

- authenticate again with the current email and password

---

## Gemini timeout or fallback

Symptoms:

- preview returns `status_ia = FAILURE`
- preview still returns an `upload_id`, but no prediction

Checks:

- confirm `consentimento_ia = true`
- confirm `GEMINI_API_KEY` is present in the backend process
- inspect backend logs for timeout or retry exhaustion messages

Fix:

- keep the manual completion flow
- restore the API key or network connectivity if AI assistance is desired

---

## File too large

Symptoms:

- preview fails with `PAYLOAD_TOO_LARGE`
- error message says the image exceeded 5MB

Fix:

- compress the image before upload
- prefer a regular JPG/PNG cover photo instead of a camera-original file

---

## Java version mismatch

Symptoms:

- Maven fails before tests or startup
- message mentions Java 21 requirement or unsupported release

Fix:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Then rerun Maven.

---

## Testcontainers or PostgreSQL not available

Symptoms:

- local tests mention Docker/Testcontainers failure

Runtime note:

- the suite now falls back automatically to H2 in memory when Docker and external PostgreSQL are unavailable

Fix:

- for the closest production-like path, run `docker compose up -d postgres`
- otherwise, let the suite continue with H2 fallback

---

## Local H2 first boot or stale schema

Symptoms:

- local profile fails during startup before the API is available
- H2 mentions missing `usuario` or columns such as `anonymized`

Fix:

- remove `EcoBookAiBackend/data/ecobook-local*`
- start the backend again with the `local` profile

Runtime note:

- the local bootstrap script was hardened on `2026-05-23` so an empty H2 file no longer crashes first boot, but very old persisted local files may still need cleanup

---

## Push notifications not arriving

Symptoms:

- inbox records exist, but device push does not appear

Checks:

- confirm `FIREBASE_SERVICE_ACCOUNT_PATH`
- confirm the Android device/emulator has Google Play services
- verify notification permission on Android 13+

Fix:

- use the in-app inbox as the fallback channel
- run `FirebaseRealDeviceValidationTest` to separate token-sync issues from backend-send issues
- if the emulator path is green, optionally repeat the final smoke on physical hardware before release

---

## Android Gradle path or `X:\app\...` failures

Symptoms:

- Gradle says files under `X:\app\...` do not exist
- `assembleDebug`, `lintDebug` or `testDebugUnitTest` fail inconsistently on Windows

Checks:

- confirm you are using `scripts/Invoke-GradleAsciiPath.ps1` instead of calling Gradle directly in this workspace
- rerun the commands sequentially if multiple Gradle jobs were launched together from different terminals or IDE tasks

Fix:

- use the wrapper script documented in the root `README.md`
- if the failure happened before the `2026-05-23` hardening pass, pull the latest repo state so the temporary-drive allocation lock is present

---

## Reserved material stuck after account deletion

Previous silent failure:

- an approved student deleting the account could leave the donor material in `RESERVADO`

Current runtime:

- account deletion now cancels the request and reopens the donor material automatically
