# Implementation Plan Summary

**Phase**: 1 Complete / 2 Complete / 3 Complete / 4 Complete / 5 Implemented / 6 Implemented / 7 Complete / 8 Complete / 9 Complete / 10 Complete  
**Date**: 2026-05-25  
**Status**: Phase 5 request workflow, Phase 6 notification workflow, the full Phase 7 admin/moderation runtime, the Phase 8 LGPD/security runtime, the full Phase 9 hardening slice, and the final Phase 10 documentation/quality closeout are implemented and validated; the 2026-06-02 follow-up closed the backend reliability fixes discovered after the audit, removed stale Android demo code and rebased the documentation away from the non-existent Android data-export flow

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
| Backend | Spring Boot 3.x + Java 21 | REST API + JWT issuance |
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
   - The onboarding flow now requires opening an in-app terms/privacy summary before platform consent and previews the normalized city value before submit
   - The profile tab now allows self-service edits for `nome`, `email`, `whatsapp`, `cidade`, `bairro` and `instituicao`, with forced reauthentication when the email changes

3. Phase 3 material + IA flow
   - `/api/v1/materiais/preview` validates JPEG/PNG, stores temporary upload, applies consent gate and returns AI/manual fallback payloads
   - `/api/v1/materiais` now persists the reviewed material from `upload_id`, promotes the image and records donor/location metadata
   - temporary upload tracking and scheduled cleanup are active in the backend
   - Android donation flow now supports gallery/camera, processing state, AI review and final publish

4. Phase 4 discovery
   - backend discovery exposes `GET /api/v1/materiais` with offset-first pagination, optional `after_id` cursor continuation, geo-aware ranking, publication-year range validation and deterministic filters
   - Android discovery uses the live search endpoint with empty state, infinite scroll, detail dialog and request CTA

5. Phase 5 request workflow
   - backend now exposes request creation, student/donor listings, approve, decline, cancel, complete and automatic reservation expiry
   - Android now performs real request creation from discovery, shows `Minhas solicitacoes`, shows `Pedidos recebidos` for donors and unlocks donor contact only after approval
   - donor-owned material management also includes `GET /api/v1/materiais/me`, `PUT /api/v1/materiais/{id}` and `DELETE /api/v1/materiais/{id}`

6. Phase 6 notification runtime
   - backend now accepts FCM device tokens, dispatches notifications after commit, standardizes payloads and persists transient failures for hourly retry
   - Android now requests notification permission contextually, syncs the FCM token, handles deep links, persists a local notifications inbox, exposes an unread bell entry point inside the main screens instead of a dedicated bottom-nav tab, keeps foreground receipt inside the in-app inbox and lets the user mark notifications as read individually or in batch from the notifications center

7. Phase 7 admin and moderation runtime
   - backend now exposes `POST /api/v1/materiais/{id}/nao-recebido`, persists `material_non_receipt_report` rows and emits a moderation-seed event after commit
   - Android now lets the student report non-receipt directly from completed request cards in `MyRequestsScreen`, with optional reason text and duplicate-open-report feedback
   - backend now also exposes `GET /api/v1/admin/reports`, `PATCH /api/v1/admin/reports/{id}/resolve`, `GET /api/v1/admin/materials`, `DELETE /api/v1/admin/materials/{id}` and `GET /api/v1/admin/users`
   - admin authorization is complete through `hasRole('ADMIN')` plus startup bootstrap/promotion via `ADMIN_BOOTSTRAP_*`

8. Phase 8 LGPD and security runtime
   - backend enforces `consentimento_ia=false` before any Gemini call and now persists consent history through `ConsentRecord`
   - backend exposes `PATCH /api/v1/usuarios/me/consentimento-ia`, the compatibility alias `PATCH /api/v1/usuarios/me/consent`, `DELETE /api/v1/usuarios/me/consent/ai-classification` and `GET /api/v1/usuarios/me/consent`
   - secure image access now flows through `GET /api/v1/images/{upload_tracking_id}` with role-aware authorization
   - account deletion, anonymization, token revocation, data export and admin audit log querying are implemented in runtime

What closed Phase 3 formally:

1. Backend automated coverage at the end of the Phase 3 closeout included preview/create material scenarios and passed with `57` tests via `mvn test` on Java 21, using a real PostgreSQL test database
2. Android donation flow compiles and its JVM validation task stays green with `app:compileDebugKotlin` + `.\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest`, and auth now also has a dedicated instrumentation E2E via `.\scripts\Invoke-GradleAsciiPath.ps1 app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ecobook.auth.AuthFlowE2ETest` for Windows workspaces that need an ASCII path alias
3. Material preview/upload is no longer a skeleton; contracts were rebased to distinguish the current runtime shape from later Phase 4+ endpoints

What closed Phase 4 formally:

1. Backend discovery now exposes `GET /api/v1/materiais` with deterministic matching, geo-aware ranking, pagination, optional `after_id` cursor continuation and publication-range validation, and the backend regression suite now passes with `66` tests via `mvn test`
2. Android discovery now uses the live search endpoint with prefilled location filters, infinite scroll, empty state, detail dialog and JVM validation through `.\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest app:lintDebug app:compileDebugAndroidTestKotlin`
3. The material detail dialog was the handoff point that immediately evolved into the delivered Phase 5 transaction flow recorded on 2026-05-13

What closed Phase 5 functionally:

1. Backend request workflow now exposes `POST /api/v1/materiais/{id}/solicitacoes`, `GET /api/v1/solicitacoes/minhas`, `GET /api/v1/solicitacoes/pendentes`, `GET /api/v1/solicitacoes/aprovadas`, `PATCH /api/v1/solicitacoes/{id}/aprovar`, `recusar`, `cancelar` and `concluir`, plus the daily reservation expiry job
2. Android request UX now covers request creation from discovery, student follow-up, donor approval/rejection/completion flows and WhatsApp handoff after approval
3. Git history records `inicio fase 5` and `fase 5 refinamento` on 2026-05-13, which matches the implemented runtime state

What closed Phase 6 formally on 2026-05-23:

1. The implemented notification flow was revalidated end to end against a real Firebase project
2. Device-level evidence was captured on a `Pixel_6` AVD with Google Play services through `FirebaseRealDeviceValidationTest`
3. The findings from that run were folded back into the runbooks and contracts, including the notification DTO serialization fix and the local H2 bootstrap hardening discovered during the validation path

What closed Phase 7 concretely:

1. Non-receipt reporting is no longer backlog-only; the persistence model, endpoint, Android action and backend tests exist in runtime
2. Admin moderation is now usable end to end at the API layer through report triage/resolution, material listing/removal and user listing with activity metrics
3. Admin access no longer depends on manual SQL only; an existing account can be promoted, or a local/admin-only account can be created, through `ADMIN_BOOTSTRAP_*`

What closed Phase 8 concretely:

1. Consent history is no longer backlog-only: the runtime now records platform and AI consent changes and exposes the summary/history endpoint consumed by the Android profile surface
2. LGPD account deletion now anonymizes users, revokes tokens, clears stored uploads, propagates request/material side effects and leaves an audit trail
3. Data export and authenticated image access are both live, which closes the originally open security/privacy execution fronts for the MVP runtime

What advanced Phase 9 concretely:

1. Response compression is live in the backend baseline
2. Micrometer plus the Prometheus actuator endpoint now expose HTTP, JVM, HikariCP and selected business counters through runtime and smoke coverage
3. A dedicated `SmokeTests` suite now exercises health, onboarding/search preview and `/actuator/prometheus` as a deploy-gate baseline
4. Authenticated hot reads now have a 30-minute cache baseline for user profile, consent status and security lookup snapshots
5. Material creation now cleans up promoted files when persistence or secondary-image promotion fails, and that rollback path is covered by automated tests
6. A public `GET /api/v1/reference-data/material-options` catalog now caches immutable enums on the backend and feeds Android discovery, onboarding and donation/edit flows with resilient local fallback
7. Discovery now returns `next_after_id` and supports same-filter `after_id` continuation, so the Android infinite-scroll flow can switch from the first offset page to keyset windows for larger result sets
8. The Phase 9 load/performance front is now covered by a repeatable `LoadValidationTest` harness that runs 20 concurrent upload flows plus 30 concurrent searches, writes evidence under `target/load-reports/`, and passed locally with `0%` errors and sub-`600 ms` p95 latencies on 2026-05-23

Validation update on 2026-05-21:

1. The backend local profile is now a reliable quickstart path again through `mvn spring-boot:run -Dspring-boot.run.profiles=local`, backed by H2-compatible enum domains and a dedicated `LocalH2Dialect`
2. That path was revalidated with `health -> register -> get me -> onboarding -> search -> login`
3. Android local validation was reconfirmed with `app:compileDebugKotlin` and `powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest`

Validation update on 2026-05-23:

1. Backend test infrastructure now falls back automatically through `Testcontainers -> external PostgreSQL -> H2 em memoria`, which keeps `mvn test` runnable even in environments sem Docker
2. Backend startup/test guidance is now explicit about the Java 21+ requirement to avoid the previous class-version mismatch failure mode
3. Root/runtime documentation was rebased so the repository is clearly documented as implementation-complete for `Phase 10`, with the remaining follow-ups called out as legal/operational rather than coding blockers
4. The Phase 9 observability baseline is now live through Micrometer/Prometheus, HikariCP metrics, runtime business counters and an actuator-backed smoke suite
5. At that `2026-05-23` validation point, the backend regression suite was green with `223` tests / `3` controlled skips via `mvn test`, including the request, admin, LGPD, profile-gate, metrics, cache, reference-data, discovery cursor, profile-update, edge-case, boundary, rollback, OpenAPI smoke, export/inbox/audit unit coverage and load-harness fronts
6. Android consent/account UX is now aligned with the current runtime: the user can read an in-app summary before accepting platform terms, edit the main profile fields later, and confirm account deletion through an explicit destructive dialog
7. Android local JVM validation remains green through `powershell -ExecutionPolicy Bypass -File .\scripts\Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest`
8. Phase 9 hardening now also includes explicit edge/boundary coverage for account-deletion cascade reopening, exact 5MB image acceptance, large request/material backlogs and exact reservation-expiry timing
9. A dedicated `E2ETests` suite now consolidates 20 API-level end-to-end scenarios, the runtime-aligned docs set now includes architecture, troubleshooting, deployment and testing guides under `docs/`, and the Android side now has a repeatable `FirebaseRealDeviceValidationTest` for the real FCM path
10. `LoadValidationTest` now closes the missing concurrency evidence with a validated `20 uploads + 30 searches` run, `562 ms` search p95, `616 ms` upload p95, `0%` errors and Prometheus/Hikari report artifacts under `EcoBookAiBackend/target/load-reports/`
11. Swagger/OpenAPI is now live on the backend through `springdoc`, with public docs at `/api/swagger-ui.html`, raw JSON at `/api/v3/api-docs`, controller-level annotations across the REST surface and smoke coverage that keeps the documentation endpoints under regression
12. At that same `2026-05-23` validation point, the coverage gate snapshot stood at `86.08%` JaCoCo line coverage, backed by the `223`-test regression suite plus the added notification-retry, token-revocation and migration-rollback validation coverage
13. Phase 10 now also has explicit evidence for code review (`docs/code-review.md`), static analysis (`docs/static-analysis.md`), dependency scanning (`docs/security-scan.md`) and migration reversibility inventory (`docs/migration-rollbacks.md`)
14. The OWASP baseline was reduced from `25` vulnerable dependencies / `131` findings to `13` / `41`, and the residual transitive/framework risk is now explicitly accepted and documented in `docs/security-scan.md`
15. Flyway rollback coverage is now explicit: every migration has a paired artifact under `db/rollback`, PostgreSQL apply -> rollback -> re-apply is validated in `MigrationRollbackValidationTest`, and the destructive historical migrations now route through a checked-in snapshot/restore workflow
16. The cross-module JavaDoc pass is now complete across the detected backend public methods under `src/main/java`; the closing scan reports `0` missing public methods, and the closeout validation bundle was rerun after that sweep to remove the last formal Phase 10 blocker
17. A post-closeout Android UI review was also completed against official Compose guidance for state hoisting, accessibility defaults and adaptive layouts; the runtime now uses centered max-width content panes, full-row toggle semantics for consent controls, a fixed `NavGraph` back-stack pattern and a green `app:lintDebug` baseline with only dependency/targetSdk warnings remaining
18. The post-closeout operational runbook was then revalidated end to end: the root/module READMEs and `quickstart.md` now reflect the verified startup order, and the Android ASCII Gradle wrapper was hardened against concurrent temporary-drive alias races discovered during that verification pass

Validation update on 2026-06-02:

1. Backend fixtures and integration slices were aligned with the current runtime rules: `cpf` is now present in complete-profile test users, `necessidade_academica` is now included in material creation fixtures, and the `UsuarioControllerWebMvcTest` slice now mocks the image-storage dependency it imports transitively
2. The Android module no longer carries the legacy `HomeScreen` showcase, sample insights, sample donation preview or the orphaned UI models/components that were no longer reachable from the real navigation graph
3. Root/runtime documentation no longer claims that the Android app offers a self-service personal-data export flow; that capability remains backend-only in the current repository
4. The latest validation pass reran `mvn test` and `app:testDebugUnitTest`; the repository stayed green with `229` backend tests, `0` failures, `0` errors, `3` controlled skips, `84.72%` JaCoCo line coverage and a green Android JVM baseline via the ASCII-path Gradle wrapper

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
- Accurate to declare Phase 7 admin/moderation delivered in runtime
- Accurate to declare Phase 8 LGPD/security runtime delivered
- Safe to treat the repository as Phase 10 complete; remaining notes are operational, legal or dependency-monitoring follow-ups rather than implementation blockers

---

## Historical Note

Some historical reports in the repository still mention Google OAuth2 because they describe an already-executed earlier direction. They are being kept as historical artifacts and are explicitly marked as legacy where needed.
