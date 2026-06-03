# Feature Specification: EcoBook IA - Core System Architecture

**Feature Branch**: `001-ecobook-core`  
**Created**: 2026-04-15  
**Status**: Active MVP baseline (runtime-aligned; implementation complete through Phase 10)  
**Architecture**: Android Native (Kotlin, Jetpack Compose) + Spring Boot Backend + PostgreSQL

Runtime note (2026-05-14):
- This document remains the product and architecture baseline.
- For delivered endpoint shapes and payloads, the runtime source of truth is `specs/001-ecobook-core/contracts/`.
- The current implementation already includes the Phase 5 request workflow and uses runtime routes such as `POST /api/v1/materiais/{id}/solicitacoes` plus action endpoints like `/api/v1/solicitacoes/{id}/aprovar`.
- Android implementation must follow official Android references and patterns from `developer.android.com`.
- Material publication runtime now supports a required front-cover image plus an optional back-cover image stored with the published item; the front cover remains the primary AI input for `/materiais/preview`.
- Notification runtime already belongs to the delivered Phase 6 codepath; when push does not arrive in local tests, the first configuration to verify is `FIREBASE_SERVICE_ACCOUNT_PATH` on the backend and Google Play services availability on the target device/emulator.

---

## Overview

EcoBook IA is an AI-powered material donation matching platform designed to promote educational equity by connecting material donors with students in need. The system matches available donated materials (books, educational supplies) with student requirements using curriculum alignment, geographic proximity, and AI-assisted classification.

**Core Value Proposition**: Reduce barriers to educational access through intelligent material matching and efficient donation workflows.

---

## Clarifications

### Session 2026-04-17

- Q: FCM notification failure handling strategy? → A: Retry with exponential backoff (1s, 2s, 4s, 8s, max 5 retries); after 5 failures, discard and log alert. This prevents overwhelming FCM service on recoverable errors while ensuring most notifications reach users.
- Q: API versioning and backward compatibility strategy? → A: Versioned paths (e.g., `/api/v1/materiais`); support 2 concurrent versions with clear deprecation period. New major versions get new path prefix; overlapping support period allows client migration.
- Q: FCM queue data recovery and dead-letter queue strategy? → A: Failed retries (after 5 attempts) move to `fcm_notifications_dlq` table; ops team reviews and manually retries critical ones; old DLQ entries (>30 days) auto-archived to audit table. Prevents queue bloat and provides audit trail.
- Q: Rate limiting and request throttling strategy? → A: Per-user token buckets with endpoint-specific limits (e.g., 10 uploads/hour, 100 searches/minute, 5 requests/hour); limits reset hourly; excess requests return HTTP 429 with `Retry-After` header. Fair, prevents accidental abuse, industry standard.
- Q: Image storage location and disaster recovery strategy? → A: Local filesystem MVP (`/uploads/{user_id}/{uuid}.ext`) with daily automated backups to external drive; no cloud dependency; sufficient for 100 active users (~1GB). Document S3 migration path for Phase 2+ scale.
- Q: Performance SLA/SLO targets by endpoint type? → A: Aggressive targets for better UX: Material search P95 150ms/P99 300ms; Gemini classification P95 7s/P99 9s; Approval operations P95 50ms/P99 150ms; FCM webhooks P95 30ms/P99 75ms. Targets drive database design (indexes, partitioning), infrastructure sizing, and monitoring thresholds.
- Q: Gemini API error handling and retry strategy? → A: HTTP 429 (rate limit): 3 retries with exponential backoff (1s-2s-4s). HTTP 5xx (server error): 3 retries with conservative backoff (2s-4s-8s). Timeout/connection: 2 retries at 1s delay. Malformed response: no retry, log and return FAILURE. Circuit breaker: pause Gemini calls for 30s after 10+ failures in 5 minutes, return FAILURE immediately. Protects against cascading failures and respects API limits.
- Q: Logging & observability strategy (levels, format, retention)? → A: Deferred to Phase 2 (MVP focuses on core functionality; basic application logging via Spring Boot default; structured logging and centralized aggregation (ELK/Datadog) planned post-launch)

### Session 2026-05-05

- Q: What city behavior should the onboarding/profile flow use for geography input in the current runtime? → A: City and neighborhood are free-text fields in the client; the backend normalizes the values before persisting and matching, without restricting the user to a curated catalog.
- Q: Is AI consent required during profile completion? → A: No. `consentimento_ia` remains optional during onboarding, defaults to false, and can be granted or revoked later from profile/settings without affecting `perfil_completo`.
- Q: How should the Buscar result blocks behave in the final UX? → A: Each result should render as a tappable card; if the image is missing or unavailable, show a neutral placeholder; tapping the card opens a dismissible detail dialog/modal with an explicit close action.
- Q: Must the Doar image flow support only camera capture? → A: No. The product should support both camera capture and gallery selection, and the UI copy should communicate both options clearly.

---

## User Scenarios & Testing

### User Story 1 - User Registration and Material Upload (Priority: P1)

A user (teacher, student, organization, or individual) registers on the platform, completes their profile, and can donate materials or request materials based on their needs.

**Why this priority**: Core functionality - the platform cannot function without material supply. Establishing trustworthy donor profiles is essential for platform credibility.

**Independent Test**: Can be fully tested by: (1) Creating a user account, (2) Uploading a material image, (3) Verifying the material appears as DISPONIVEL in the system and matches student queries.

**Acceptance Scenarios**:

1. **Given** an unregistered user accesses the app, **When** they select "Criar conta", **Then** they see a registration form requesting name, email, password, and password confirmation before proceeding to onboarding
2. **Given** a registered user with incomplete profile, **When** they attempt POST /materiais, **Then** the system returns HTTP 403 Forbidden with field `perfil_completo: false`
3. **Given** a user with complete profile selects a book image to donate, **When** they confirm the system's AI suggestions, **Then** the material is persisted with status DISPONIVEL and becomes available to other users searching for materials

---

### User Story 2 - AI-Assisted Material Classification (Priority: P1)

A donor uploads a material image. The backend calls Google Gemini to extract metadata (discipline, education level, system). The system handles AI confidence levels and provides fallback options.

**Why this priority**: AI classification is the technical differentiator; low confidence or timeout scenarios must degrade gracefully to preserve UX.

**Independent Test**: Can be fully tested by: (1) Uploading various material types (math textbook, novel, workbook), (2) Verifying correct confidence levels trigger appropriate UI behaviors (auto-fill, warnings, manual entry), (3) Testing timeout recovery.

**Acceptance Scenarios**:

1. **Given** a donor selects an image of a math textbook via POST /materiais/preview, **When** the Gemini API returns confidence ≥ 0.75, **Then** all AI-assisted fields (titulo, autor, editora, disciplina, nivel_ensino, ano, sistema_ensino, data_publicacao) are auto-populated with editable inputs and a green checkmark indicator, while `estado_conservacao` remains manual
2. **Given** Gemini returns confidence 0.50–0.75 (LOW_CONFIDENCE), **When** the response renders in the frontend, **Then** fields display suggested values with yellow warning icons and are editable
3. **Given** Gemini confidence < 0.50 or timeout occurs, **When** response is returned, **Then** all classification fields render empty and require manual input; status_ia displays FAILURE or LOW_CONFIDENCE
4. **Given** consentimento_ia = false for the user, **When** they upload an image, **Then** POST /materiais/preview returns `status_ia = FAILURE`, a reusable `upload_id`, and no Gemini call is made
5. **Given** a user completed onboarding with `consentimento_ia = false`, **When** they later enable AI consent from profile/settings and submit a new material preview, **Then** Gemini processing is allowed without requiring a new account or a repeated onboarding flow

---

### User Story 3 - Student Discovers Matching Materials (Priority: P1)

A student specifies their academic needs (discipline, education level, curriculum system, grade year, geographic area). The system matches available materials using a deterministic algorithm and displays results ranked by proximity and recency.

**Why this priority**: Core value delivery - students must reliably find materials that match their requirements.

**Independent Test**: Can be fully tested by: (1) Creating a student profile with specific needs, (2) Querying materials in various discipline/level/system combinations, (3) Verifying ranking order (same neighborhood first, then same city, then by date).

**Acceptance Scenarios**:

1. **Given** a student with profile (discipline: MATEMATICA, nivel_ensino: FUNDAMENTAL, ano: 7, cidade: Florianópolis, bairro: Centro), **When** they query available materials, **Then** the system returns materials where: status = DISPONIVEL AND disciplina = MATEMATICA AND nivel_ensino = FUNDAMENTAL AND |ano_material - 7| ≤ 1 (or SUPERIOR ignores year filter) AND sistema_ensino matches
2. **Given** multiple matching materials exist, **When** results are ranked, **Then** order is: (a) same neighborhood first, (b) same city next, (c) sorted by data_publicacao DESC (most recent publication first), (d) ID as tiebreaker
3. **Given** a student searches for SUPERIOR level materials, **When** results are filtered, **Then** year constraint is ignored; materials are matched purely by discipline, level, and system
4. **Given** a student searches for sistema_ensino = OUTRO, **When** the system filters, **Then** it returns ONLY materials with sistema_ensino = OUTRO (not all systems)
5. **Given** a student filters with min_ano_publicacao=1990 and max_ano_publicacao=2010, **When** results are filtered, **Then** only materials with data_publicacao between 1990 and 2010 (inclusive) are returned
6. **Given** a student provides only min_ano_publicacao=2015, **When** results are filtered, **Then** only materials with data_publicacao >= 2015 are returned
7. **Given** a student provides only max_ano_publicacao=2000, **When** results are filtered, **Then** only materials with data_publicacao <= 2000 are returned
8. **Given** a student provides min_ano_publicacao=2010 and max_ano_publicacao=2000 (invalid range), **When** the query is sent, **Then** the system returns HTTP 400 Bad Request with error message
9. **Given** a discovery result has no uploaded image yet or the image cannot be loaded, **When** the list renders, **Then** the result still appears with a neutral placeholder image area instead of a broken layout
10. **Given** a student taps a material block in the discovery list, **When** they want more context before requesting, **Then** the app opens a dismissible detail dialog/modal with richer metadata and a visible close action

---

### User Story 4 - Student Requests Material (Priority: P1)

A student finds a matching material and submits a request. The system records the request, notifies the donor via FCM, and transitions the material to RESERVADO if approved.

**Why this priority**: Core transaction flow - requests are the primary interaction between supply and demand.

**Independent Test**: Can be fully tested by: (1) Creating a material and student profile, (2) Submitting a solicitacao, (3) Verifying FCM notification delivery and state transitions.

**Acceptance Scenarios**:

1. **Given** a student views a DISPONIVEL material, **When** they submit a request via POST /materiais/{materialId}/solicitacoes, **Then** the system creates a Solicitacao with status = PENDENTE and returns it
2. **Given** a request is created, **When** the request reaches PENDENTE, **Then** an FCM notification SOLICITACAO_RECEBIDA is sent to the donor
3. **Given** a donor approves a request via PATCH /solicitacoes/{id}/aprovar, **When** the operation succeeds, **Then**: (a) the Solicitacao transitions to APROVADA, (b) the Material transitions to RESERVADO, (c) an FCM notification SOLICITACAO_APROVADA is sent to the student, (d) the Material is locked (no new requests accepted)
4. **Given** a request is in PENDENTE state, **When** the donor declines via PATCH /solicitacoes/{id}/recusar, **Then** the Solicitacao transitions to RECUSADA and FCM notification SOLICITACAO_RECUSADA is sent
5. **Given** a Solicitacao is APROVADA and 14 days pass, **When** the Material is not marked DOADO, **Then** the system automatically reverts Material.status to DISPONIVEL and Solicitacao.status to CANCELADA

---

### User Story 5 - Material Donation Completion (Priority: P2)

After the student receives and accepts the material, the donor marks it as DOADO. The system records completion, sends final notifications, and archives the transaction.

**Why this priority**: Important for feedback loop and donor engagement, but does not block core matching functionality.

**Independent Test**: Can be fully tested by: (1) Completing a full request-approval cycle, (2) Marking material as DOADO, (3) Verifying state finality and notification delivery.

**Acceptance Scenarios**:

1. **Given** a Solicitacao is in APROVADA state, **When** the donor calls PATCH /solicitacoes/{id}/concluir, **Then** the Material transitions to DOADO and Solicitacao.status = CONCLUIDA
2. **Given** a donation is marked CONCLUIDA, **When** the transition occurs, **Then** an FCM notification MATERIAL_DOADO is sent to the student with donor contact details (contato_doador)
3. **Given** a Material is in DOADO state, **When** a new request arrives, **Then** the system returns HTTP 422 Unprocessable Entity (invalid state transition)

---

### User Story 6 - Material Removal and Reversion (Priority: P2)

A donor can remove a material while it is still available, and approved reservations can still revert automatically or by request cancellation. The system enforces business rules for removal and state reversion appropriately.

**Why this priority**: Important for donor flexibility and error recovery, but secondary to core matching.

**Independent Test**: Can be fully tested by: (1) Deleting DISPONIVEL materials, (2) Canceling approved requests to revert RESERVADO materials, (3) Verifying invalid transitions are rejected.

**Acceptance Scenarios**:

1. **Given** a Material in DISPONIVEL state, **When** the donor calls DELETE /materiais/{id}, **Then** the material is removed from runtime APIs and no longer appears in discovery
2. **Given** a Material in RESERVADO state with an APROVADA Solicitacao, **When** the reservation is canceled via PATCH /solicitacoes/{id}/cancelar or it expires after 14 days, **Then** the Material transitions back to DISPONIVEL and the Solicitacao transitions to CANCELADA
3. **Given** a deleted material had pending student requests, **When** the delete completes, **Then** affected students receive MATERIAL_CANCELADO and the cascading delete removes the historical request rows tied to that material
4. **Given** a Material in DOADO state, **When** deletion is attempted, **Then** the system returns HTTP 422 Unprocessable Entity (final state, cannot revert)

---

### User Story 7 - Profile Completion and Onboarding (Priority: P1)

Users must complete their profiles before performing restricted operations. The system enforces profile completeness checks.

**Why this priority**: Profile data is essential for geographic matching, contact verification, and platform trust.

**Independent Test**: Can be fully tested by: (1) Creating incomplete profile, (2) Attempting restricted operations, (3) Verifying HTTP 403 responses, (4) Completing profile and confirming access.

**Acceptance Scenarios**:

1. **Given** a newly registered user with incomplete profile (missing city or WhatsApp), **When** they query GET /usuarios/me, **Then** the response includes `perfil_completo: false`
2. **Given** a user with `perfil_completo: false`, **When** they attempt POST /materiais or POST /materiais/{materialId}/solicitacoes, **Then** the system returns HTTP 403 Forbidden
3. **Given** a user with incomplete profile, **When** they complete all required fields via PUT /usuarios/me, **Then** `perfil_completo` transitions to true and restricted operations become available
4. **Given** a user completes all required profile fields but leaves `consentimento_ia = false`, **When** the profile is saved, **Then** `perfil_completo` still becomes true because AI consent is not part of the completeness gate
5. **Given** a profile-complete user later changes their mind about AI usage, **When** they update consent from profile/settings, **Then** `consentimento_ia` changes without affecting authentication state or profile completeness

---

### User Story 8 - Geographic Normalization and Deduplication (Priority: P1)

The system normalizes geographic data (cities, neighborhoods) to ensure consistent matching and prevent typos from creating false mismatches.

**Why this priority**: Core to accurate geographic matching; without normalization, "Centro" vs "centro" would be treated as different locations.

**Independent Test**: Can be fully tested by: (1) Creating profiles with various case/accent variations, (2) Verifying normalization in database, (3) Confirming matching works across variations.

**Acceptance Scenarios**:

1. **Given** a user enters city "são joão" with neighborhood "criciúma", **When** the profile is saved, **Then** the backend normalizes to CIDADE = "SAO JOAO" and BAIRRO = "CRICIUMA" (uppercase, no accents, trimmed)
2. **Given** one donor enters "Centro" and another enters "CENTRO", **When** they are queried for same-neighborhood matching, **Then** they are considered the same location

---

## Requirements

### Functional Requirements

#### User Management & Onboarding

- **RF-001**: System MUST support user registration and login with email and password; backend stores only `password_hash` and issues JWT on successful authentication
- **RF-002**: System MUST enforce profile completeness before allowing material operations; endpoint GET /usuarios/me returns `perfil_completo: boolean`
- **RF-003**: System MUST normalize geographic data: uppercase letters, remove accents (NFD + ASCII), trim whitespace; e.g., "são joão" → "SAO JOAO"
- **RF-004**: System MUST track `consentimento_ia: boolean` per user to control Gemini API calls
- **RF-004a**: Frontend city and neighborhood inputs during onboarding/profile editing MUST accept free-text values, while backend storage remains normalized text
- **RF-004b**: `consentimento_ia` MUST default to false, MUST NOT block `perfil_completo`, and MUST remain editable later from profile/settings

#### Material Management & Lifecycle

- **RF-005**: Material lifecycle states stored by the current domain are: DISPONIVEL (initially), RESERVADO (approved request), DOADO (completed donation), CANCELADO (legacy/admin-oriented state retained in the schema); current donor-facing runtime CRUD exposes DELETE /materiais/{id} for removing DISPONIVEL items instead of a public CANCELADO transition
- **RF-006**: System MUST reject invalid state transitions with HTTP 422 Unprocessable Entity
- **RF-007**: Material MUST be discoverable only in DISPONIVEL state
- **RF-008**: Material creation requires: titulo, descricao, disciplina, nivel_ensino, ano, sistema_ensino, estado_conservacao, imagem (JPEG/PNG, ≤ 5MB), doador_id, cidade, bairro, data_publicacao; optional metadata includes `autor` and `editora`
- **RF-009**: Material images MUST be validated for MIME type (JPEG/PNG only); invalid types rejected with HTTP 400
- **RF-010**: Image preview uploads MUST return `upload_id` in POST /materiais/preview for tracking and later promotion from temporary to permanent storage

#### Image Processing Pipeline

- **RF-011**: POST /materiais/preview accepts multipart form data with image file; backend saves the file temporarily before the final POST /materiais confirmation
- **RF-011a**: Android donation flow MUST support both gallery selection and camera capture as valid sources for the image sent to `/materiais/preview`, and the UI copy MUST communicate both options
- **RF-012**: Backend MUST call Google Gemini API with 10-second timeout; timeout returns HTTP 200 with `status_ia = FAILURE`, `error_details.timeout = true`, and a reusable `upload_id`
- **RF-013**: Backend MUST parse Gemini response and validate: JSON structure, presence of best_prediction, enum values, confidence in [0, 1]
- **RF-014**: If any validation fails, set status_ia = LOW_CONFIDENCE or FAILURE; return to frontend with parsed/partial data
- **RF-015**: After user confirmation (POST /materiais with `upload_id`), backend MUST validate all enum values and persist material with status = DISPONIVEL
- **RF-016**: Temporary image file MUST be promoted to permanent storage after successful persistence

#### AI Confidence & Fallback Rules

- **RF-017**: Gemini confidence ≥ 0.75 → fields auto-filled with green checkmark, editable, status_ia = SUCCESS
- **RF-018**: Gemini confidence 0.50–0.75 → fields auto-filled with yellow warning icon, editable, status_ia = LOW_CONFIDENCE
- **RF-019**: Gemini confidence < 0.50 → all fields empty, manual entry required, status_ia = LOW_CONFIDENCE, and a reusable `upload_id`
- **RF-020**: Gemini FAILURE or timeout → all fields empty, manual entry required, status_ia = FAILURE
- **RF-021**: If consentimento_ia = false, backend skips Gemini call and returns `status_ia = FAILURE` with a reusable `upload_id`

#### Gemini Error Handling & Resilience

- **RF-062**: HTTP 429 (rate limit) responses from Gemini MUST trigger retry with exponential backoff: 1s, 2s, 4s (max 3 retries); if all retries exhausted, return status_ia = FAILURE
- **RF-063**: HTTP 5xx (server error) responses from Gemini MUST trigger retry with conservative exponential backoff: 2s, 4s, 8s (max 3 retries); if all retries exhausted, return status_ia = FAILURE
- **RF-064**: Connection timeout or socket timeout exceptions MUST trigger immediate retry at 1s delay (max 2 retries); if both retries fail, return status_ia = FAILURE
- **RF-065**: Malformed or invalid Gemini response (JSON parse error, missing best_prediction field, invalid enum values) MUST NOT be retried; backend logs error with request ID for debugging and returns status_ia = FAILURE

#### Material Matching Algorithm

- **RF-022**: Matching pipeline (in order): (1) Filter status = DISPONIVEL, (2) Filter disciplina (exact), (3) Filter nivel_ensino (exact), (4) Filter |ano_material - ano_student| ≤ 1 (except SUPERIOR, which ignores school year), (5) Filter sistema_ensino (exact; OUTRO matches only OUTRO), (6) Filter data_publicacao range if provided (min_ano_publicacao and/or max_ano_publicacao), (7) Sort by: same bairro > same cidade > data_publicacao DESC (newest publication first) > id
- **RF-023**: Query endpoint: GET /materiais?disciplina=X&nivel_ensino=Y&ano=Z&sistema_ensino=W&cidade=C&bairro=B returns paginated results
- **RF-023a**: Discovery results in the Android client MUST render each material as a tappable card and MUST use a neutral placeholder when no material image is available or the image fails to load
- **RF-023b**: Tapping a discovery result card MUST open a dismissible dialog/modal with an explicit close action and richer material details before request submission
- **RF-024**: Special rule: SUPERIOR level (nivel_ensino = SUPERIOR) ignores school year constraint in filtering
- **RF-025**: Special rule: sistema_ensino = OUTRO matches ONLY materials with sistema_ensino = OUTRO (not a wildcard)
- **RF-044**: Optional publication date range filter (min_ano_publicacao, max_ano_publicacao) allows narrowing by original publication year (1900-2100); if both provided, must satisfy min <= max; if only min provided, filters data_publicacao >= min; if only max provided, filters data_publicacao <= max; violating min > max returns HTTP 400

#### Request Lifecycle & Approval

- **RF-026**: Request lifecycle states: PENDENTE (initial), APROVADA (donor accepted), RECUSADA (donor declined), CANCELADA (either party cancelled), CONCLUIDA (donation completed); no other states
- **RF-027**: POST /materiais/{materialId}/solicitacoes creates Solicitacao with status = PENDENTE; Material remains DISPONIVEL initially
- **RF-028**: Donor approval: PATCH /solicitacoes/{id}/aprovar atomically (1) updates Solicitacao.status, (2) updates Material.status = RESERVADO, (3) sends FCM notification SOLICITACAO_APROVADA to student
- **RF-029**: Donor decline: PATCH /solicitacoes/{id}/recusar updates Solicitacao.status; Material remains DISPONIVEL; sends FCM notification SOLICITACAO_RECUSADA
- **RF-030**: Material MUST have at most ONE active (APROVADA) Solicitacao at any time; subsequent requests to same material are rejected with HTTP 409 Conflict if Material.status = RESERVADO
- **RF-031**: Completion: PATCH /solicitacoes/{id}/concluir transitions Material.status = DOADO and Solicitacao.status = CONCLUIDA

#### State Consistency & Invariants

- **RF-032**: Invariant: Material.status = RESERVADO → exactly ONE Solicitacao.status = APROVADA for that material
- **RF-033**: Invariant: Material.status = DOADO → exactly ONE Solicitacao.status = CONCLUIDA for that material
- **RF-034**: Invariant: Material.status = DISPONIVEL → NO Solicitacao.status = APROVADA for that material
- **RF-035**: Approval operation MUST use database-level lock (SERIALIZABLE isolation or SELECT ... FOR UPDATE) to prevent race conditions
- **RF-036**: RESERVADO materials MUST auto-expire after 14 days; system job runs daily to revert expired reservations to DISPONIVEL

#### Notifications

- **RF-037**: FCM notifications: 6 types required:
  - SOLICITACAO_RECEBIDA: Sent to donor when request created
  - SOLICITACAO_APROVADA: Sent to student when request approved
  - SOLICITACAO_RECUSADA: Sent to student when request declined
  - SOLICITACAO_CANCELADA: Sent to student when approved request is cancelled
  - MATERIAL_DOADO: Sent to student when donation completed (includes donor contact)
  - MATERIAL_CANCELADO: Sent to student when a DISPONIVEL material is removed by the donor
- **RF-038**: FCM must be reliable; failures are logged but do not block primary operations
- **RF-038a**: Current runtime note: transient FCM notification failures are queued in PostgreSQL and retried hourly, up to 3 retry attempts; unrecoverable token errors clear the stored token and skip further retries
- **RF-038b**: Failed FCM notifications MUST be queued in a persistent retry system; the current runtime processes this queue via a scheduled background job every hour

#### Enums & Data Types

- **RF-039**: System MUST enforce enum values:
  - Disciplina: MATEMATICA | PORTUGUES | HISTORIA | GEOGRAFIA | CIENCIAS | LITERATURA
  - NivelEnsino: FUNDAMENTAL | MEDIO | SUPERIOR
  - SistemaEnsino: ANGLO | OBJETIVO | COC | POSITIVO | OUTRO
  - EstadoConservacao: NOVO | BOM | USADO | DANIFICADO
  - StatusMaterial: DISPONIVEL | RESERVADO | DOADO | CANCELADO (legacy/admin-oriented; current donor CRUD removes DISPONIVEL materials via DELETE instead of exposing CANCELADO)
  - StatusSolicitacao: PENDENTE | APROVADA | RECUSADA | CANCELADA | CONCLUIDA
  - StatusRespostaIA: SUCCESS | LOW_CONFIDENCE | FAILURE
- **RF-040**: Enum validation must reject invalid values with HTTP 400 and error message specifying field and allowed values

#### Contact & Communication

- **RF-041**: WhatsApp contact MUST be stored in E.164 format (e.g., +5548999999999)
- **RF-042**: Donor contact details (nome, whatsapp) are included in MATERIAL_DOADO notification and in Solicitacao.contato_doador only when Solicitacao.status = APROVADA
- **RF-043**: Solicitacao.contato_doador is NULL when status ≠ APROVADA

#### API Versioning & Compatibility

- **RF-044**: All REST API endpoints MUST use versioned path prefixes: `/api/v1/` for current version, e.g., `/api/v1/materiais`, `/api/v1/solicitacoes`, `/api/v1/usuarios`
- **RF-045**: System MUST maintain backward compatibility with the current and previous major versions (e.g., v1 and v2 running simultaneously); deprecated versions are supported for 6-month transition period before removal
- **RF-046**: Breaking changes (field removals, renamed paths, changed HTTP methods) are reserved for major version increments only; minor versions (v1.1, v1.2) MUST maintain full backward compatibility
- **RF-047**: API versioning MUST be communicated in documentation and app release notes; clients are notified 3 months before version deprecation

#### FCM Queue Recovery & Dead-Letter Queue

- **RF-048**: Current runtime note: FCM notifications that exhaust the 3 retry attempts are marked in `failed_notification` with `permanently_failed_at` and `last_error`, remaining available for later operator inspection
- **RF-049**: Admin dashboard review of permanently failed notifications remains future work and is not part of the current runtime
- **RF-050**: Dedicated DLQ/archive tables are deferred to a later observability/admin phase; the current runtime keeps permanent failures in the primary retry table
- **RF-051**: Successful FCM notifications are marked with `delivered_at`; the current runtime does not wait for Firebase webhook confirmation before removing them from the active retry path

#### Rate Limiting & Throttling

- **RF-052**: System MUST implement per-user rate limiting using token bucket algorithm; each user gets endpoint-specific quotas that reset hourly: (a) Material upload (POST /api/v1/materiais): 10 requests/hour, (b) Material search (GET /api/v1/materiais): 100 requests/hour, (c) Request submission (POST /api/v1/solicitacoes): 5 requests/hour, (d) All other endpoints: 60 requests/hour
- **RF-053**: Rate limit enforcement MUST happen at the API gateway or Spring Boot filter layer before request reaches business logic; user identity tracked by JWT token (for authenticated requests) or API key (for client apps)
- **RF-054**: When rate limit is exceeded, system MUST return HTTP 429 (Too Many Requests) with response headers: `Retry-After: <seconds>` (time until next token available), `X-RateLimit-Limit: <total quota>`, `X-RateLimit-Remaining: <tokens left>`, `X-RateLimit-Reset: <unix timestamp of reset>`
- **RF-055**: Rate limit state MUST be stored in Redis for sub-millisecond lookup performance (token count, last reset time per user per endpoint); fallback to in-memory cache with eventual consistency if Redis unavailable

#### Image Storage & Disaster Recovery

- **RF-056**: Material images MUST be stored persistently on local filesystem in directory structure `/uploads/{user_id}/{material_uuid}.{ext}` where ext is jpg or png; images promoted to permanent storage after material creation confirmed and persisted to database
- **RF-057**: Temporary upload images MUST be stored in separate directory `/tmp/uploads/` with TTL; files older than 24 hours without associated material record are automatically deleted by cleanup job
- **RF-058**: Daily backup job MUST run at 2:00 AM (server timezone) and copy entire `/uploads/` directory to external backup location (external hard drive mounted at `/backups/` or network share); backup should preserve directory structure and file timestamps
- **RF-059**: Backup retention policy: Keep daily backups for 7 days, then archive oldest backup to long-term storage; maintain at least 30 days of backups for disaster recovery
- **RF-060**: Image serving MUST include HTTP cache headers: `Cache-Control: public, max-age=31536000` (1 year) for permanent images (immutable content); `ETag` header for change detection; conditional GET support (If-None-Match) to reduce bandwidth
- **RF-061**: Disaster recovery plan (documented separately): If primary storage lost, restore from daily backup within 4 hours; interim: serve images from 24-hour-old backup if necessary. S3 migration path defined for Phase 2 (scale >10,000 users)

---

### Non-Functional Requirements

- **RNF-001**: System MUST support Android 8.0+ (API level 26+); native Kotlin implementation via Jetpack Compose
- **RNF-002**: Backend MUST be Spring Boot 3.x with Java 21+; PostgreSQL 14+ as primary database
- **RNF-003**: Gemini API calls MUST timeout after 10 seconds; timeout returns FAILURE status
- **RNF-004**: Image processing MUST validate and reject non-JPEG/PNG files with HTTP 400
- **RNF-005**: System MUST log all security events (authentication, authorization, state transitions) with timestamp and actor
- **RNF-006**: FCM notification retry queue MUST persist failed notifications in PostgreSQL with fields compatible with the current runtime: `id`, `user_id`, `notification_type`, `title`, `body`, `payload_data` (JSON), `retry_count`, `last_attempt_at`, `next_attempt_at`, `delivered_at`, `permanently_failed_at`, `last_error`
- **RNF-007**: API versioning infrastructure MUST support at least 2 major versions running concurrently (e.g., v1 and v2 routable to different handler chains); routing decision based on URL path prefix only
- **RNF-008**: Current runtime note: the FCM notification schema includes the primary retry table and indexes on `next_attempt_at` and `user_id`; DLQ/archive tables remain future observability work
- **RNF-009**: PostgreSQL replication MUST be configured in streaming replication mode (primary-replica) to ensure zero data loss on primary failure; replica can be promoted to primary within 2 minutes of primary outage detection
- **RNF-010**: Rate limiting lookup MUST complete within 5ms per request to avoid bottleneck; Redis cluster with 3+ nodes (primary + replicas) provides high availability; in-memory fallback uses LRU cache limited to 100,000 user entries
- **RNF-011**: Rate limit quotas MUST be configurable per endpoint without code changes; configuration stored in database (feature_flags table) or Spring property file; changes take effect within 1 minute
- **RNF-012**: Material image filesystem storage MUST support at least 10GB capacity (sufficient for ~5000 images @ 2MB average); storage location configurable via environment variable (e.g., `UPLOAD_DIR=/uploads/`); directory structure enforced via application layer
- **RNF-013**: Daily backup job MUST complete within 30 minutes and not impact API response times; backup process runs in background thread with lower priority; backup integrity verified via checksum (SHA-256) before deletion of temporary files
- **RNF-014**: Phase 2 upgrade path to AWS S3 documented: Spring Cloud Storage abstraction layer allows zero-code-change migration; S3 configuration activatable via Spring profile (e.g., `spring.profiles.active=s3`)
- **RNF-015**: Material search endpoint (GET /materiais with filters) MUST maintain P95 latency ≤ 150ms and P99 ≤ 300ms under 10k concurrent users; requires database indexing on (disciplina, nivel_ensino, cidade, bairro, status) and query result pagination (limit 50 per page)
- **RNF-016**: Gemini classification (POST /materiais/preview) MUST complete within P95 7 seconds and P99 9 seconds including image upload, Gemini API call (10s timeout), and response marshaling; Gemini timeout at 10s ensures we stay below target
- **RNF-017**: Material request approval (PATCH /solicitacoes/{id} status=APROVADA) MUST maintain P95 latency ≤ 50ms and P99 ≤ 150ms; database lock acquisition (RF-035) and notification queue insertion must be optimized
- **RNF-017a**: Runtime endpoint note: the current backend exposes this approval path as `PATCH /solicitacoes/{id}/aprovar`
- **RNF-018**: FCM webhook handler (POST /webhooks/fcm) MUST process callbacks within P95 30ms and P99 75ms; webhook processing offloaded to async job queue to avoid blocking caller
- **RNF-019**: Gemini API circuit breaker MUST track failure count and timestamp over 5-minute window; when failure count exceeds 10, circuit breaker enters OPEN state and pauses all Gemini calls for 30 seconds, returning status_ia = FAILURE immediately; after 30s, transitions to HALF_OPEN and allows probe request; on probe success, resets to CLOSED. Prevents cascading failures and API exhaustion.

---

## Key Entities

### User (Usuário)

Represents a registered user who can simultaneously act as both material donor and material recipient (no role separation).

**Attributes**:
- `id` (UUID): Unique identifier
- `email` (String): Unique email address
- `password_hash` (String, internal): Strong one-way hash stored by backend; never exposed by API
- `nome` (String): User's full name
- `whatsapp` (String): formato E.164, por exemplo `+5511999999999`
- `cidade` (String): Normalized to uppercase, no accents
- `bairro` (String): Normalized to uppercase, no accents
- `instituicao` (String, optional): School/organization affiliation
- `perfil_completo` (Boolean): True if all required fields completed
- `consentimento_ia` (Boolean): True if user consents to AI processing
- `created_at` (Timestamp)
- `updated_at` (Timestamp)

---

### Material (Entidade Material)

Represents a donated educational resource.

**Attributes**:
- `id` (UUID): Unique identifier
- `titulo` (String): Material title; can be auto-populated by Gemini via OCR (confidence typically 0.85-0.95); always editable by user
- `autor` (String, optional): Book author; can be auto-populated by Gemini when visible on the material or strongly supported by Google Search grounding
- `editora` (String, optional): Publisher; can be auto-populated by Gemini when visible on the material or strongly supported by Google Search grounding
- `descricao` (String): Detailed description; **manual-only field** (never auto-populated to prevent hallucinations)
- `disciplina` (Enum: MATEMATICA | PORTUGUES | HISTORIA | GEOGRAFIA | CIENCIAS | LITERATURA)
- `nivel_ensino` (Enum: FUNDAMENTAL | MEDIO | SUPERIOR)
- `ano` (Integer): Target grade/year (1–12 for FUNDAMENTAL/MEDIO, null for SUPERIOR)
- `sistema_ensino` (Enum: ANGLO | OBJETIVO | COC | POSITIVO | OUTRO)
- `estado_conservacao` (Enum: NOVO | BOM | USADO | DANIFICADO): Always selected manually by the donor after reviewing the item
- `status` (Enum: DISPONIVEL | RESERVADO | DOADO | CANCELADO; current donor CRUD only surfaces DISPONIVEL, RESERVADO and DOADO directly)
- `imagem_url` (String): URL to permanent storage
- `upload_id` (String, optional): Temporary upload tracking ID
- `doador_id` (UUID): Foreign key to User (donor)
- `cidade` (String): Normalized geographic location
- `bairro` (String): Normalized neighborhood
- `data_publicacao` (Integer/Date, optional): Year or date the material (book/workbook) was originally published (e.g., 2010); can be estimated or provided by AI classification; different from created_at which tracks when donated to platform
- `created_at` (Timestamp)
- `updated_at` (Timestamp)

**Relationships**:
- One Material has many Solicitacoes (1:N)
- At most one APROVADA Solicitacao per Material
- Material → User (doador)

---

### Solicitacao (Request/Solicitation)

Represents a student's request for a material.

**Attributes**:
- `id` (UUID): Unique identifier
- `material_id` (UUID): Foreign key to Material
- `estudante_id` (UUID): Foreign key to User (student)
- `status` (Enum: PENDENTE | APROVADA | RECUSADA | CANCELADA | CONCLUIDA)
- `contato_doador` (Object, nullable): { nome, whatsapp }; populated only when status = APROVADA
- `created_at` (Timestamp)
- `updated_at` (Timestamp)
- `approved_at` (Timestamp, optional): When donor approved
- `expires_at` (Timestamp, optional): 14 days after approval; triggers auto-reversion if not CONCLUIDA

**Relationships**:
- Many Solicitacoes per Material (but at most one APROVADA)
- Many Solicitacoes per User (student)
- Solicitacao → Material
- Solicitacao → User (estudante)

---

### NecessidadeAcademica (Academic Need)

Represents a student's expressed learning requirements for discovery.

**Attributes**:
- `disciplina` (Enum: MATEMATICA | PORTUGUES | HISTORIA | GEOGRAFIA | CIENCIAS | LITERATURA)
- `nivel_ensino` (Enum: FUNDAMENTAL | MEDIO | SUPERIOR)
- `ano` (Integer): Target grade/year (1–12 for FUNDAMENTAL/MEDIO, null for SUPERIOR)
- `sistema_ensino` (Enum: ANGLO | OBJETIVO | COC | POSITIVO | OUTRO)
- `cidade` (String): Normalized
- `bairro` (String, optional): Normalized; if omitted, entire city is searched

---

## Communication Contracts

### 1. AI Response Structure (POST /materiais/preview)

**Backend Response (HTTP 200 OK)**:
```json
{
  "status_ia": "SUCCESS|LOW_CONFIDENCE|FAILURE",
  "upload_id": "temp-upload-uuid-12345",
  "best_prediction": {
    "autor": { "value": "Autor Exemplo", "confidence": 0.83 },
    "editora": { "value": "Editora Exemplo", "confidence": 0.79 },
    "titulo": {
      "value": "Geometria Plana 7º Ano",
      "confidence": 0.92
    },
    "autor": {
      "value": "Autor Exemplo",
      "confidence": 0.83
    },
    "editora": {
      "value": "Editora Exemplo",
      "confidence": 0.79
    },
    "disciplina": {
      "value": "MATEMATICA",
      "confidence": 0.95
    },
    "nivel_ensino": {
      "value": "FUNDAMENTAL",
      "confidence": 0.88
    },
    "ano": {
      "value": 7,
      "confidence": 0.75
    },
    "sistema_ensino": {
      "value": "ANGLO",
      "confidence": 0.92
    },
    "data_publicacao": {
      "value": 2010,
      "confidence": 0.70
    }
  },
  "error_details": {
    "timeout": false,
    "malformed_response": false,
    "missing_fields": [],
    "invalid_enums": []
  }
}
```

**Validation Rules**:
- If `status_ia = FAILURE`: all fields in `best_prediction` may be null; frontend renders empty form
- If `status_ia = LOW_CONFIDENCE`: some fields may be null or have confidence < 0.50; frontend marks with yellow warning and allows editing
- If `status_ia = SUCCESS`: all fields present with confidence ≥ 0.75; frontend auto-fills with green checkmark and allows editing
- If Gemini timeout or consentimento_ia = false: `status_ia = FAILURE`, `best_prediction` empty object

---

### 2. Material Cadastro (POST /materiais - Frontend Request)

```json
{
  "upload_id": "temp-upload-uuid-abc123def",
  "titulo": "Livro de Matemática 7º Ano",
  "autor": "Autor Exemplo",
  "editora": "Editora Exemplo",
  "descricao": "Usado em colégios do sistema Anglo, bem conservado",
  "disciplina": "MATEMATICA",
  "nivel_ensino": "FUNDAMENTAL",
  "ano": 7,
  "sistema_ensino": "ANGLO",
  "estado_conservacao": "BOM",
  "data_publicacao": 2010,
  "cidade": "Florianópolis",
}
```

**Backend Processing**:
1. Validate JSON payload plus `upload_id`
2. Load the staged upload created by `POST /materiais/preview`
3. Validate donor ownership, expiration and enum/year rules
4. Promote temporary image(s) to permanent storage
5. Persist Material with status = DISPONIVEL
6. Return HTTP 201 Created with full Material entity

**Current runtime note**:
- `cidade`, `bairro`, and direct image fields in the JSON sketch above are legacy planning remnants
- current runtime derives `cidade` and `bairro` from the authenticated donor profile
- image binaries are uploaded only in `POST /materiais/preview`; `POST /materiais` receives metadata plus `upload_id`
- the legacy `imagem` field shown in the JSON sketch above is no longer part of the current runtime request body

---

### 3. Material Runtime Response (GET /materiais or GET /materiais/me)

```json
{
  "id": "material-uuid-1",
  "titulo": "Livro de Matemática 7º Ano",
  "autor": "Autor Exemplo",
  "editora": "Editora Exemplo",
  "descricao": "Usado em colégios do sistema Anglo, bem conservado",
  "disciplina": "MATEMATICA",
  "nivel_ensino": "FUNDAMENTAL",
  "ano": 7,
  "sistema_ensino": "ANGLO",
  "estado_conservacao": "BOM",
  "status": "DISPONIVEL",
  "imagem_url": "https://cdn.ecobook.com/materiais/material-uuid-1.jpg",
  "doador": {
    "id": "user-uuid-donor",
    "nome": "João Silva",
    "whatsapp": "+5548999999999",
    "cidade": "FLORIANOPOLIS",
    "bairro": "CENTRO"
  },
  "cidade": "FLORIANOPOLIS",
  "bairro": "CENTRO",
  "data_publicacao": "2010",
  "created_at": "2026-04-10T14:30:00Z",
  "updated_at": "2026-04-10T14:30:00Z"
}
```

---

### 4. Solicitacao Response (GET /solicitacoes/{id})

```json
{
  "id": "solicitacao-uuid-1",
  "material_id": "material-uuid-1",
  "estudante_id": "user-uuid-student",
  "status": "PENDENTE|APROVADA|RECUSADA|CANCELADA|CONCLUIDA",
  "contato_doador": null,
  "created_at": "2026-04-12T10:15:00Z",
  "updated_at": "2026-04-12T10:15:00Z",
  "approved_at": null,
  "expires_at": null
}
```

**Note**: When `status = APROVADA`, `contato_doador` is populated:
```json
{
  "contato_doador": {
    "nome": "João Silva",
    "whatsapp": "+5548999999999"
  }
}
```

---

### 5. Necessidade Acadêmica (Student Query Profile)

```json
{
  "disciplina": "MATEMATICA",
  "nivel_ensino": "FUNDAMENTAL",
  "ano": 7,
  "sistema_ensino": "ANGLO",
  "cidade": "Florianópolis",
  "bairro": "Centro"
}
```

**Usage**: Submitted in GET /materiais query string or stored as student profile preference.

---

### 6. FCM Notification Payloads (6 types)

#### SOLICITACAO_RECEBIDA
Sent to donor when student submits request.
```json
{
  "type": "SOLICITACAO_RECEBIDA",
  "solicitacao_id": "solicitacao-uuid-1",
  "material_id": "material-uuid-1",
  "material_titulo": "Livro de Matemática 7º Ano",
  "estudante_nome": "Maria Santos",
  "message": "Sua doação recebeu um pedido. Revise em seu app."
}
```

#### SOLICITACAO_APROVADA
Sent to student when donor approves request.
```json
{
  "type": "SOLICITACAO_APROVADA",
  "solicitacao_id": "solicitacao-uuid-1",
  "material_id": "material-uuid-1",
  "material_titulo": "Livro de Matemática 7º Ano",
  "doador_nome": "João Silva",
  "doador_whatsapp": "+5548999999999",
  "message": "Sua solicitação foi aprovada! Contate o doador para combinar."
}
```

#### SOLICITACAO_RECUSADA
Sent to student when donor declines request.
```json
{
  "type": "SOLICITACAO_RECUSADA",
  "solicitacao_id": "solicitacao-uuid-1",
  "material_id": "material-uuid-1",
  "material_titulo": "Livro de Matemática 7º Ano",
  "message": "Sua solicitação foi recusada. Procure outro material."
}
```

#### SOLICITACAO_CANCELADA
Sent to student when donor cancels an approved request.
```json
{
  "type": "SOLICITACAO_CANCELADA",
  "solicitacao_id": "solicitacao-uuid-1",
  "material_id": "material-uuid-1",
  "material_titulo": "Livro de Matemática 7º Ano",
  "message": "A doação foi cancelada. Procure outro material."
}
```

#### MATERIAL_DOADO
Sent to student when donation is completed.
```json
{
  "type": "MATERIAL_DOADO",
  "solicitacao_id": "solicitacao-uuid-1",
  "material_id": "material-uuid-1",
  "material_titulo": "Livro de Matemática 7º Ano",
  "doador_nome": "João Silva",
  "doador_whatsapp": "+5548999999999",
  "message": "Sua doação chegou! Confirme recebimento."
}
```

#### MATERIAL_CANCELADO
Sent to student when material is cancelled by donor (at any stage).
```json
{
  "type": "MATERIAL_CANCELADO",
  "material_id": "material-uuid-1",
  "material_titulo": "Livro de Matemática 7º Ano",
  "message": "A doação foi cancelada pelo doador."
}
```

---

### 7. Standard Error Response

Current runtime note:
- The implemented backend currently returns `status`, `error`, `message`, `timestamp`, `path`, and optional `field_errors`.
- Richer `field` / `details` metadata below should be treated as target-state guidance, not as the guaranteed Phase 4 runtime shape.
- The stable runtime contract for delivered endpoints is documented in `contracts/error-response.md`.

All error responses follow this structure:

```json
{
  "error": "INVALID_ENUM|INCOMPLETE_PROFILE|INVALID_STATE_TRANSITION|CONFLICT|...",
  "message": "Human-readable error description",
  "field": "campo_específico_se_aplicável",
  "details": {
    "allowed_values": ["OPTION_A", "OPTION_B"],
    "received_value": "INVALID_OPTION"
  }
}
```

**HTTP Status Codes**:
- `400`: Invalid request (malformed JSON, invalid enum, validation error)
- `403`: Forbidden (incomplete profile, insufficient permissions)
- `404`: Not found (material/request does not exist)
- `409`: Conflict (e.g., material already reserved, duplicate request)
- `422`: Unprocessable entity (invalid state transition, e.g., trying to approve already-completed request)
- `500`: Internal server error

---

## REST API Endpoints

### 1. User Management

#### POST /auth/register
- **Method**: POST
- **RFC**: RF-001, RF-002
- **Description**: Register new user
- **Request**: `{ email, password, nome }`
- **Response**: Auth response with user entity + JWT (HTTP 201)
- **Rules**: 
  - Email must be unique
  - Password must satisfy minimum policy
  - Backend stores only `password_hash`
  - Initial `perfil_completo = false` until onboarding fields are completed

#### POST /auth/login
- **Method**: POST
- **RFC**: RF-001
- **Description**: Authenticate existing user
- **Request**: `{ email, password }`
- **Response**: Auth response with user entity + JWT (HTTP 200)
- **Rules**:
  - Email and password must match stored credentials
  - Invalid credentials return HTTP 401
  - Raw password is never returned

#### PUT /usuarios/me
- **Method**: PUT
- **RFC**: RF-002, RF-003, RF-004
- **Description**: Update user profile
- **Request**: `{ nome, whatsapp, cidade, bairro, instituicao, consentimento_ia }`
- **Response**: Updated User entity (HTTP 200)
- **Rules**:
  - Normalize cidade/bairro (uppercase, no accents)
  - Set `perfil_completo = true` if all required fields present
  - `consentimento_ia` controls Gemini API usage
  - `consentimento_ia` is optional for onboarding completion and can be enabled later from profile/settings without requiring the user to recreate the profile
  - Frontend UX should accept free-text city and neighborhood values, while the API normalizes them before persistence and matching

#### GET /usuarios/me
- **Method**: GET
- **RFC**: RF-002
- **Description**: Get current user profile
- **Response**: User entity including `perfil_completo` (HTTP 200)
- **Authentication**: Required (JWT/session token)

---

### 2. Material Management

#### POST /materiais
- **Method**: POST (application/json)
- **RFC**: RF-005 through RF-010, RF-039
- **Description**: Create new material from a previously staged `upload_id`
- **Request**: Material data + image file (JPEG/PNG, ≤ 5MB)
- **Response**: Material entity with `status = DISPONIVEL` and `upload_id` (HTTP 201)
- **Current runtime request**: JSON metadata plus the staged `upload_id` returned by `POST /materiais/preview`
- **Rules**:
  - User must have `perfil_completo = true` (HTTP 403 if false)
  - Validate all enums (HTTP 400 if invalid)
  - `ano` must be `1..9` for `FUNDAMENTAL`, `1..3` for `MEDIO`, and omitted for `SUPERIOR`
  - Donor location is inherited from the authenticated profile
  - `upload_id` must come from `POST /materiais/preview`, belong to the authenticated donor, and still exist in temporary storage
  - Backend promotes the staged image(s) and persists the final material
- **HTTP Codes**: 201 Created, 400 Bad Request, 403 Forbidden, 500 Internal Server Error

#### POST /materiais/preview
- **Method**: POST (multipart/form-data)
- **RFC**: RF-011 through RF-021
- **Description**: Preview AI classification for uploaded image
- **Request**: Image file only
- **Response**: AI response structure (status_ia, best_prediction, upload_id)
- **Rules**:
  - If `consentimento_ia = false`: return FAILURE, no Gemini call
  - Call Gemini with 10s timeout
  - Parse response, validate JSON/enums/confidence
  - Return confidence levels for UI decision logic
- **HTTP Codes**: 200 OK, 400 Bad Request, 500 Internal Server Error

#### GET /materiais
- **Method**: GET
- **RFC**: RF-022 through RF-025, RF-044
- **Description**: Search available materials with matching algorithm
- **Query Params**: `disciplina, nivel_ensino, ano, sistema_ensino, cidade, bairro, min_ano_publicacao (optional), max_ano_publicacao (optional), page, size`
- **Response**: Paginated array of Material entities (HTTP 200)
- **Rules**:
  - Filter by: status=DISPONIVEL, disciplina (exact), nivel_ensino (exact), school year (`ano`: `1..9` for FUNDAMENTAL, `1..3` for MEDIO), sistema_ensino (exact), city/neighborhood
  - Optional publication date range filter (min_ano_publicacao, max_ano_publicacao): if both provided, must satisfy min <= max and both in [1900, 2100]
  - Sort by: same bairro > same cidade > data_publicacao DESC (newest publication first) > id
  - SUPERIOR ignores school year filter (ano)
  - OUTRO sistema_ensino matches ONLY OUTRO
  - If min_ano_publicacao > max_ano_publicacao when both provided: HTTP 400 Bad Request
  - If min or max outside [1900, 2100]: HTTP 400 Bad Request
- **HTTP Codes**: 200 OK, 400 Bad Request (invalid enum, invalid publication range), 403 Forbidden (profile incomplete)

#### Runtime Note - GET /materiais/{id}

The current runtime does not expose a dedicated `GET /materiais/{id}` endpoint.

- Material details used by the Android runtime are carried in `GET /materiais` and `GET /materiais/me`
- Any future single-item endpoint should mirror the same `MaterialDTO` payload shape

#### Runtime Note - Current Material Write Endpoints

The current runtime contract supersedes the legacy planning note below:

- `PUT /materiais/{id}` updates editable metadata only while the material is `DISPONIVEL`
- `DELETE /materiais/{id}` removes a `DISPONIVEL` material from runtime APIs with hard delete
- the donor-facing API does not currently expose `PATCH /materiais/{id}` with `status = CANCELADO`

#### Legacy Planning Note - PATCH /materiais/{id}
- **Method**: PATCH
- **RFC**: RF-005 through RF-006
- **Description**: Update material status (e.g., DISPONIVEL → CANCELADO)
- **Request**: `{ status: CANCELADO }`
- **Response**: Updated Material entity (HTTP 200)
- **Rules**:
  - Validate state transition (HTTP 422 if invalid)
  - Only the user who created the material (in their donor capacity) can modify it
  - Users can request materials from other users regardless of their own donor status
  - Canceling RESERVADO material cascades to related Solicitacao
- **HTTP Codes**: 200 OK, 403 Forbidden, 422 Unprocessable Entity

---

### 3. Request Management

#### Runtime Note - Current Request Endpoints

The current runtime contract supersedes the legacy planning notes below:

- `POST /materiais/{material_id}/solicitacoes`
- `GET /solicitacoes/minhas`
- `GET /solicitacoes/pendentes`
- `GET /solicitacoes/aprovadas`
- `GET /solicitacoes/{id}`
- `PATCH /solicitacoes/{id}/aprovar`
- `PATCH /solicitacoes/{id}/recusar`
- `PATCH /solicitacoes/{id}/cancelar`
- `PATCH /solicitacoes/{id}/concluir`

#### Legacy Planning Note - POST /solicitacoes
- **Method**: POST
- **RFC**: RF-026 through RF-027
- **Description**: Create request for material
- **Request**: `{ material_id, estudante_id }`
- **Response**: Solicitacao entity with `status = PENDENTE` (HTTP 201)
- **Rules**:
  - User must have `perfil_completo = true` (HTTP 403 if false)
  - Material must exist and be DISPONIVEL (HTTP 404 or 409 if not)
  - If Material already has APROVADA Solicitacao, reject with HTTP 409 Conflict
  - Send FCM notification SOLICITACAO_RECEBIDA to donor
- **HTTP Codes**: 201 Created, 403 Forbidden, 404 Not Found, 409 Conflict

#### Legacy Planning Note - PATCH /solicitacoes/{id}
- **Method**: PATCH
- **RFC**: RF-028 through RF-031, RF-032 through RF-035
- **Description**: Update request status (PENDENTE → APROVADA, RECUSADA, or CANCELADA)
- **Request**: `{ status: APROVADA|RECUSADA|CANCELADA|CONCLUIDA }`
- **Response**: Updated Solicitacao entity (HTTP 200)
- **Rules**:
  - Validate state transition (HTTP 422 if invalid)
  - Only the user who created the material can approve/decline requests to that material; either party (material creator or requester) can cancel under valid conditions
  - Approval is atomic: update Solicitacao.status, Material.status = RESERVADO, send FCM, use database lock
  - Decline keeps Material DISPONIVEL
  - Cancel may cascade if Material was RESERVADO
  - Completion: Solicitacao → CONCLUIDA, Material → DOADO
- **HTTP Codes**: 200 OK, 403 Forbidden, 422 Unprocessable Entity

#### Legacy Planning Note - GET /solicitacoes
- **Method**: GET
- **RFC**: RF-026
- **Description**: List requests for current user (in both donor and student capacities)
- **Query Params**: `status, page, limit`
- **Response**: Paginated array of Solicitacao entities (HTTP 200)
- **Rules**:
  - Return requests where current user is the material creator (as donor) OR the requester (as student)
  - Filter by status if provided
- **HTTP Codes**: 200 OK

#### GET /solicitacoes/{id}
- **Method**: GET
- **RFC**: RF-026
- **Description**: Get single request details
- **Response**: Solicitacao entity; `contato_doador` populated only if status = APROVADA (HTTP 200)
- **HTTP Codes**: 200 OK, 404 Not Found

---

### 4. Notifications

#### GET /notificacoes
- **Method**: GET
- **RFC**: RF-037
- **Description**: List FCM notifications (optional history endpoint)
- **Query Params**: `page, limit, type`
- **Response**: Paginated array of notifications (HTTP 200)
- **HTTP Codes**: 200 OK

---

## AI Processing Pipeline

### 10-Step Image Processing Workflow

1. **User Selection**: Frontend user selects image from device gallery or captures a new photo with the camera; the donation UX must clearly communicate both options
2. **Upload to Backend**: Frontend sends multipart POST /materiais/preview with image file
3. **Temporary Storage**: Backend saves image to temporary location (disk/S3) and generates `upload_id`
4. **Gemini Call**: Backend sends image + prompt to Google Gemini API with 10s timeout
5. **Response Parsing**: Backend parses Gemini JSON response:
   - Validate JSON structure
   - Check for `best_prediction` object
   - Validate enum values (MATEMATICA, FUNDAMENTAL, etc.)
   - Extract confidence scores (must be in [0, 1])
   - Set `status_ia` = SUCCESS|LOW_CONFIDENCE|FAILURE
6. **Return to Frontend**: POST /materiais/preview response includes `status_ia`, `best_prediction`, `upload_id`
7. **User Review**: Frontend displays:
   - If SUCCESS: auto-filled fields with green checkmark, editable, upload_id in form
   - If LOW_CONFIDENCE: auto-filled with yellow warning icon, editable, upload_id in form
   - If FAILURE: empty fields, manual entry required, upload_id preserved so the manual flow can continue without reupload
8. **Confirmation Submission**: User reviews/edits, confirms; frontend sends full data + `upload_id` to POST /materiais
9. **Backend Validation**: Backend validates all enums, matches upload_id, persists Material with status = DISPONIVEL
10. **Image Promotion**: Temporary image is moved/renamed to permanent storage location (S3/CDN); imagem_url populated; upload_id retained for audit trail

---

## Gemini AI Prompt Template (MVP)

```
Analyze this educational material image and extract metadata.

Return ONLY valid JSON in this format:
{
  "best_prediction": {
    "titulo": { "value": "Geometria Plana 7º Ano", "confidence": 0.92 },
    "disciplina": { "value": "MATEMATICA|PORTUGUES|HISTORIA|GEOGRAFIA|CIENCIAS|LITERATURA", "confidence": 0.95 },
    "nivel_ensino": { "value": "FUNDAMENTAL|MEDIO|SUPERIOR", "confidence": 0.85 },
    "ano": { "value": 7, "confidence": 0.75 },
    "sistema_ensino": { "value": "ANGLO|OBJETIVO|COC|POSITIVO|OUTRO", "confidence": 0.90 },
    "estado_conservacao": { "value": "NOVO|BOM|USADO|DANIFICADO", "confidence": 0.88 },
    "data_publicacao": { "value": 2010, "confidence": 0.70 }
  }
}

Confidence must be 0.0–1.0. If uncertain about any field, set low confidence or null. Never invent values.

For titulo: Extract from cover/spine/title page visible text via OCR. Only extract text explicitly visible; never invent titles.
For data_publicacao: Extract the publication year (e.g., 2010 for a 2010 textbook) if visible on cover/spine. 
If only a decade is visible (e.g., "1990s"), estimate conservatively (1995). If entirely uncertain, return null.
NOT EXTRACTED (manual-only): descricao - never attempt to generate descriptions as this causes hallucinations.
```

---

## AI Confidence Fallback Rules

| Confidence Range | Behavior | UI Presentation | status_ia |
|---|---|---|---|
| ≥ 0.75 | Auto-fill all fields | Green checkmark, editable | SUCCESS |
| 0.50–0.75 | Auto-fill with warning | Yellow warning icon, editable | LOW_CONFIDENCE |
| < 0.50 | Leave empty | Empty fields, manual entry | LOW_CONFIDENCE |
| Timeout (>10s) | Leave empty | Empty fields, manual entry, alert | FAILURE |
| consentimento_ia=false | Skip Gemini | Empty fields, manual entry | FAILURE |
| JSON parse error | Leave empty | Empty fields, manual entry | FAILURE |
| Missing/invalid enum | Set to LOW_CONFIDENCE | Affected field editable | LOW_CONFIDENCE |
| Invalid confidence value | Set to LOW_CONFIDENCE | Affected field editable | LOW_CONFIDENCE |

---

## State Machines

### Material State Transitions

```
DISPONIVEL ──(user requests)──> [waiting for approval]
    ↓
    ├──(donor approves)──> RESERVADO ──(14 days pass)──> DISPONIVEL
    │                          ↓
    │                    (donation completed)
    │                          ↓
    │                        DOADO [FINAL]
    │
    └──(donor cancels)──> CANCELADO [FINAL]

Runtime note:
- the current donor-facing runtime removes `DISPONIVEL` materials via `DELETE /materiais/{id}` instead of exposing a public transition to `CANCELADO`

Rules:
- Only DISPONIVEL materials appear in search results
- RESERVADO → DOADO: only via Solicitacao APROVADA → CONCLUIDA
- RESERVADO → DISPONIVEL: automatic after 14 days expiry if not DOADO
- DOADO is terminal in the current donor-facing runtime
- CANCELADO remains reserved in the schema for legacy/admin-oriented flows and is not exposed by the current donor CRUD
- Invalid transitions → HTTP 422
```

### Solicitacao State Transitions

```
PENDENTE ──(donor approves)──> APROVADA ──(donation completed)──> CONCLUIDA [FINAL]
   ↑            ↓
   │     (donor declines)
   │            ↓
   │         RECUSADA [FINAL]
   │
   └──(cancel before approval)──> CANCELADA [FINAL]

APROVADA ──(either party cancels)──> CANCELADA [FINAL]

Rules:
- PENDENTE: Material remains DISPONIVEL, other requests can exist
- APROVADA: Material → RESERVADO, max one APROVADA per Material, other requests blocked (HTTP 409)
- RECUSADA, CANCELADA, CONCLUIDA: Terminal states
- Expiry: If APROVADA for 14 days without CONCLUIDA → auto-cancel, Material → DISPONIVEL
- Invalid transitions → HTTP 422
```

---

## Consistency Invariants

1. **One Approved Request Per Material**
   - If Material.status = RESERVADO → exactly ONE Solicitacao.status = APROVADA
   - If Material.status = DISPONIVEL → NO Solicitacao.status = APROVADA
   - If Material.status = DOADO → exactly ONE Solicitacao.status = CONCLUIDA

2. **No Orphaned Reservations**
   - Material.status = RESERVADO with no APROVADA Solicitacao violates invariant
   - Daily job detects and reverts to DISPONIVEL

3. **Atomic Approval**
   - Solicitacao approval must atomically update both Solicitacao and Material status
   - Use database-level lock (SERIALIZABLE isolation or SELECT ... FOR UPDATE)
   - Prevents race condition: two requests approving simultaneously

4. **Geographic Normalization**
   - All cidade/bairro stored uppercase, NFD-decomposed, ASCII-only
   - "São João" → "SAO JOAO"
   - Matching compares normalized values

5. **Image Promotion**
   - Temporary image must be promoted to permanent storage after Material.status = DISPONIVEL
   - upload_id links temporary and permanent records for audit

---

## Assumptions

1. **Internet Connectivity**: Android users have stable connectivity for image upload and API calls; timeouts are expected and handled gracefully
2. **Geographic Data**: Brazilian location context assumed (E.164 WhatsApp +55 prefix, city/state normalization); internationalization deferred to future versions
3. **Gemini Availability**: Google Gemini API is available and responsive within 10 seconds for ~95% of requests; timeouts trigger fallback
4. **Image Storage**: Persistent image storage (S3, CDN) is available and responds within SLA; temporary storage is local disk or separate staging bucket
5. **Database Consistency**: PostgreSQL SERIALIZABLE isolation level available for approval locking; or explicit SELECT ... FOR UPDATE used
6. **User Authentication**: Spring backend handles login/token issuance directly using email/password hash verification and JWT
7. **Reservation Expiry Job**: Background job runs daily to check and revert expired RESERVADO materials; failures are logged but don't block operations
8. **FCM Reliability**: Firebase Cloud Messaging failures are logged and retried; not guaranteed delivery (acceptable for non-critical notifications)
9. **WhatsApp Format**: All WhatsApp numbers are Brazilian (+55 prefix); international numbers not supported in MVP
10. **Enum Stability**: Enum values (disciplines, systems, conservation states) are unlikely to change during typical feature development cycle
11. **Material Ownership**: Users own materials they have created and made available for donation. When requesting materials from other users, requesters have no modification rights; only the material creator can modify or manage their materials. Admin panel handles disputes.
12. **No Bidding/Negotiation**: Single-step approval/decline; no counter-offers or negotiation flow

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: Students can discover materials matching their curriculum requirements in under 2 seconds (P95 latency)
- **SC-002**: Donors can upload and classify a material in under 3 minutes (including image selection and form completion)
- **SC-003**: Material matching algorithm correctly ranks results by geographic proximity and recency with zero inconsistencies across repeated queries
- **SC-004**: AI classification succeeds (SUCCESS status) for ≥ 75% of textbook/workbook images; LOW_CONFIDENCE or FAILURE rates ≤ 25%
- **SC-005**: Request approval/rejection workflow completes in under 10 seconds end-to-end (Solicitacao state → Material state → FCM dispatch)
- **SC-006**: System supports ≥ 10,000 concurrent users without degradation (database connection pooling, read replicas for search queries)
- **SC-007**: Image upload validation rejects invalid files (non-JPEG/PNG, >5MB) with clear error messages; no rejected files are persisted
- **SC-008**: FCM notifications deliver to ≥ 95% of active users within 5 seconds of triggering event
- **SC-009**: Auto-expiry of 14-day RESERVADO materials executes daily with zero missed expirations
- **SC-010**: Profile completion flow reduces user abandonment to ≤ 15%; ≥ 85% of registered users complete onboarding within 24 hours

---

## Out of Scope (MVP)

- Internationalization (i18n) / multi-language support
- Admin analytics dashboard
- User reputation/rating system
- Material condition verification (in-person validation by intermediary)
- Payment processing or incentive mechanisms
- Web frontend (Android only for MVP)
- Advanced search filters (price range, material type, ISBN lookup)
- Community forums or chat beyond WhatsApp
- Offline-first mobile sync
- Biometric authentication

