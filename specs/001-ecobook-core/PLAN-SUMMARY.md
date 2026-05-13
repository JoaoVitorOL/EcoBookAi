# Implementation Plan Summary

**Phase**: 1 Complete / 2 Complete / 3 Complete  
**Date**: 2026-05-12  
**Status**: Phase 4 discovery delivered across backend and Android; Phase 5 can begin

---

## Planning Update

This summary has been rebased to the new authentication rule:

- Authentication is now **email + password + JWT**
- Passwords are stored on the backend only as strong hashes
- Google OAuth2 is no longer the source of truth for the MVP

The product direction for AI, onboarding, matching, request workflow, and notifications remains the same. The main planning change is that all auth-related work now targets local credentials instead of Google sign-in.

---

## Phase 1 Deliverables

### 1. [plan.md](plan.md)

Defines:
- Technical context for Android + Spring Boot + PostgreSQL + Gemini + FCM
- Constitution alignment checks
- Phased implementation plan
- Architecture and risk management

### 2. [research.md](research.md)

Captures:
- Gemini feasibility and retry strategy
- Image quality, rate limits, and storage decisions
- Deterministic locking and matching rules
- Email/password + JWT authentication approach for Android and backend

### 3. [data-model.md](data-model.md)

Defines:
- Core entities and relationships
- User profile completeness rules
- `password_hash` storage in `usuario`
- PostgreSQL schema and indexes

### 4. [quickstart.md](quickstart.md)

Documents:
- Local environment setup
- Backend startup
- Android setup
- Register/login flow with email and password
- First integration test path

### 5. `contracts/`

Primary auth-facing contract:
- [user-api.md](contracts/user-api.md)

Core endpoints now expected:
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `PUT /api/v1/usuarios/me`
- `GET /api/v1/usuarios/me`

---

## Constitution Alignment

All planning now aligns with Constitution v2.0.0:

| Principle | Alignment | Evidence |
|-----------|-----------|----------|
| Social Impact | Yes | Matching and donation flow remain mission-centered |
| Android-Native | Yes | Native Kotlin/Compose app remains immutable |
| Password Hashing & Deterministic Security | Yes | Email/password hash + JWT replaces Google OAuth2 |
| LGPD & Privacy | Yes | Consent, anonymization, and retention remain unchanged |
| MVP Scope Discipline | Yes | No expansion into multi-provider auth for MVP |
| Rule-Based Matching | Yes | Matching logic remains deterministic and auditable |

---

## Technical Stack Snapshot

| Component | Technology | Notes |
|-----------|-----------|-------|
| Frontend | Kotlin + Jetpack Compose | Android native only |
| Backend | Spring Boot 3.x + Java 17+ | REST API + JWT issuance |
| Database | PostgreSQL 14+ | Stores profile and password hash |
| Authentication | Email/password + JWT | No Google sign-in for MVP |
| AI | Google Gemini 2.5 Flash | Unchanged |
| Messaging | Firebase Cloud Messaging | Unchanged |
| Storage | Local filesystem | Unchanged |

---

## Current Closeout Snapshot

What is already implemented in the repository:

1. Backend auth/profile foundation
   - `POST /api/v1/auth/register`
   - `POST /api/v1/auth/login`
   - `GET /api/v1/usuarios/me`
   - `PUT /api/v1/usuarios/me`
   - Password hashing + JWT issuance + profile completeness gate

2. Android auth/profile foundation
   - Login/register screen with email and password
   - Form validation inside the auth ViewModel
   - JWT persistence and session restore
   - Logout and `401` handling
   - Onboarding with WhatsApp, cidade, bairro and optional `consentimento_ia`

3. Phase 3 material + IA flow
   - `/api/v1/materiais/preview` validates JPEG/PNG, stores temporary upload, applies consent gate and returns AI/manual fallback payloads
   - `/api/v1/materiais` now persists the reviewed material from `upload_id`, promotes the image and records donor/location metadata
   - temporary upload tracking and scheduled cleanup are active in the backend
   - Android donation flow now supports gallery/camera, processing state, AI review and final publish

What closed Phase 3 formally:

1. Backend automated coverage at the end of the Phase 3 closeout included preview/create material scenarios and passed with `57` tests via `mvn test` on Java 21, using a real PostgreSQL test database
2. Android donation flow compiles and its JVM validation task stays green with `app:compileDebugKotlin` + `.\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest`, and auth now also has a dedicated instrumentation E2E via `.\scripts\Invoke-GradleAsciiPath.ps1 app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ecobook.auth.AuthFlowE2ETest` for Windows workspaces that need an ASCII path alias
3. Material preview/upload is no longer a skeleton; contracts were rebased to distinguish the current runtime shape from later Phase 4+ endpoints

What closed Phase 4 formally:

1. Backend discovery now exposes `GET /api/v1/materiais` with deterministic matching, geo-aware ranking, pagination and publication-range validation, and the backend regression suite now passes with `66` tests via `mvn test`
2. Android discovery now uses the live search endpoint with prefilled location filters, infinite scroll, empty state, detail dialog and JVM validation through `.\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest app:lintDebug app:compileDebugAndroidTestKotlin`
3. The material detail dialog already surfaces the request CTA, while the actual solicitation transaction remains intentionally deferred to Phase 5

---

## Readiness Notes

What remains valid from the original plan:
- Backend and Android project structure
- JWT-based session handling
- Onboarding and `perfil_completo` gate
- AI-assisted material classification
- Deterministic matching rules
- Request and FCM modules

What changed materially:
- Auth source of truth
- User schema for credentials
- Android entry flow
- Backend auth service design

What is now accurate about readiness:
- Accurate to declare Phase 3 core delivered
- Accurate to declare Phase 4 discovery delivered
- Safe to begin Phase 5 request workflow work

---

## Historical Note

Some historical reports in the repository still mention Google OAuth2 because they describe an already-executed earlier direction. They are being kept as historical artifacts and are now explicitly marked as legacy where needed.
