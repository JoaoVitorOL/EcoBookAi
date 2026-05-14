# Implementation Plan Summary

**Phase**: 1 Complete / 2 Complete / 3 Complete / 4 Complete / 5 Implemented / 6 Implemented  
**Date**: 2026-05-14  
**Status**: Phase 5 request workflow and Phase 6 notification workflow are implemented across backend and Android; the remaining closeout item is end-to-end Firebase validation on real-capable devices

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

## Android Implementation Rule

The Android app must follow official Android references and patterns from `developer.android.com`.

Primary references:

- [Architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [Guide to app architecture](https://developer.android.com/topic/architecture)
- [UI layer and state holders](https://developer.android.com/topic/architecture/ui-layer/stateholders)
- [Navigation for Compose](https://developer.android.com/develop/ui/compose/navigation)
- [Notification runtime permission](https://developer.android.com/develop/ui/views/notifications/notification-permission)

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

4. Phase 4 discovery
   - backend discovery exposes `GET /api/v1/materiais` with pagination, geo-aware ranking, publication-year range validation and deterministic filters
   - Android discovery uses the live search endpoint with empty state, infinite scroll, detail dialog and request CTA

5. Phase 5 request workflow
   - backend now exposes request creation, student/donor listings, approve, decline, cancel, complete and automatic reservation expiry
   - Android now performs real request creation from discovery, shows `Minhas solicitacoes`, shows `Pedidos recebidos` for donors and unlocks donor contact only after approval
   - donor-owned material management also includes `GET /api/v1/materiais/me`, `PUT /api/v1/materiais/{id}` and `DELETE /api/v1/materiais/{id}`

6. Phase 6 notification runtime
   - backend now accepts FCM device tokens, dispatches notifications after commit, standardizes payloads and persists transient failures for hourly retry
   - Android now requests notification permission contextually, syncs the FCM token, handles deep links, persists a local notifications inbox, exposes an unread bell entry point inside the main screens instead of a dedicated bottom-nav tab, keeps foreground receipt inside the in-app inbox and lets the user mark notifications as read individually or in batch from the notifications center

What closed Phase 3 formally:

1. Backend automated coverage at the end of the Phase 3 closeout included preview/create material scenarios and passed with `57` tests via `mvn test` on Java 21, using a real PostgreSQL test database
2. Android donation flow compiles and its JVM validation task stays green with `app:compileDebugKotlin` + `.\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest`, and auth now also has a dedicated instrumentation E2E via `.\scripts\Invoke-GradleAsciiPath.ps1 app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ecobook.auth.AuthFlowE2ETest` for Windows workspaces that need an ASCII path alias
3. Material preview/upload is no longer a skeleton; contracts were rebased to distinguish the current runtime shape from later Phase 4+ endpoints

What closed Phase 4 formally:

1. Backend discovery now exposes `GET /api/v1/materiais` with deterministic matching, geo-aware ranking, pagination and publication-range validation, and the backend regression suite now passes with `66` tests via `mvn test`
2. Android discovery now uses the live search endpoint with prefilled location filters, infinite scroll, empty state, detail dialog and JVM validation through `.\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest app:lintDebug app:compileDebugAndroidTestKotlin`
3. The material detail dialog was the handoff point that immediately evolved into the delivered Phase 5 transaction flow recorded on 2026-05-13

What closed Phase 5 functionally:

1. Backend request workflow now exposes `POST /api/v1/materiais/{id}/solicitacoes`, `GET /api/v1/solicitacoes/minhas`, `GET /api/v1/solicitacoes/pendentes`, `GET /api/v1/solicitacoes/aprovadas`, `PATCH /api/v1/solicitacoes/{id}/aprovar`, `recusar`, `cancelar` and `concluir`, plus the daily reservation expiry job
2. Android request UX now covers request creation from discovery, student follow-up, donor approval/rejection/completion flows and WhatsApp handoff after approval
3. Git history records `inicio fase 5` and `fase 5 refinamento` on 2026-05-13, which matches the implemented runtime state

What remains to close Phase 6 formally:

1. Revalidate the implemented notification flow end to end with a real Firebase setup and updated execution notes
2. Capture device-level evidence for foreground/background receipt on hardware or emulator with Google Play services
3. Fold any Firebase-project-specific findings back into the local runbooks

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
- Accurate to declare Phase 5 request workflow delivered in runtime
- Safe to treat Phase 6 notification UX as implemented in runtime and then move to admin/LGPD/hardening fronts after final Firebase-device validation

---

## Historical Note

Some historical reports in the repository still mention Google OAuth2 because they describe an already-executed earlier direction. They are being kept as historical artifacts and are now explicitly marked as legacy where needed.
