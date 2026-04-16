# Implementation Plan: EcoBook IA - Core System

**Branch**: `001-ecobook-core` | **Date**: 2026-04-15 | **Spec**: [specs/001-ecobook-core/spec.md](spec.md)  
**Input**: Feature specification from `/specs/001-ecobook-core/spec.md`

---

## Summary

**EcoBook IA** is an AI-powered educational material donation and matching platform exclusively for Android (API 26+, Kotlin/Jetpack Compose). The core system implements deterministic student-to-material matching powered by Google Gemini 2.5 Flash for image-based material classification, with a Spring Boot backend and PostgreSQL database. The MVP supports up to 100 active users with ~500 total materials and implements the complete donation workflow: user registration (Google OAuth2), mandatory profile completion (geographic + academic needs), material upload with AI classification, intelligent matching algorithm (discipline + level + year ± 1 + system + geographic ranking), request lifecycle (PENDENTE → APROVADA → DOADO), and push notifications via Firebase Cloud Messaging.

**Technical Approach**:
- **Frontend**: Native Android (Kotlin, Jetpack Compose) with Material Design 3
- **Backend**: Spring Boot 3.x + Java 17, Spring Data JPA for ORM
- **Database**: PostgreSQL 14+ with connection pooling (HikariCP)
- **AI Integration**: Google Gemini 2.5 Flash API (direct backend integration, no separate microservice)
- **Authentication**: Google OAuth2 + JWT (7-day expiry)
- **Storage**: Local server filesystem (`/uploads/{user_id}/{uuid}.ext`), no cloud storage for MVP
- **Messaging**: Firebase Cloud Messaging (FCM) for 6 event types
- **Testing**: JUnit 5 + Mockito (backend), Compose testing framework (Android)

---

## Technical Context

**Language/Version**: Kotlin (primary), Java 17+ (backend)  
**Primary Dependencies**:
- Frontend: Jetpack Compose, Retrofit2, OkHttp3, Coil (image loading), Firebase Messaging
- Backend: Spring Boot 3.1+, Spring Data JPA, Hibernate, PostgreSQL JDBC, OkHttp3 (Gemini client)

**Storage**: PostgreSQL 14+ (primary), local filesystem for temporary/permanent image storage  
**Testing**: JUnit 5, Mockito, Compose testing, Spring Boot TestContainers  
**Target Platform**: Android 8.0+ (API 26+), Spring Boot on Linux/Docker  
**Project Type**: Mobile app (Android) + REST API backend  
**Performance Goals**:
- P95 latency for material search: < 2 seconds
- Material upload/classification: < 3 minutes end-to-end
- Gemini API timeout: 10 seconds max
- FCM notification delivery: > 95% within 5 seconds
- Concurrent users: ≥ 10,000 (MVP target: 100 active)

**Constraints**:
- Image file size: ≤ 5MB (JPEG/PNG only)
- Gemini API rate limit: 250 req/day (free tier); MVP average ~5 images/user × 100 users = 500 req/month ✓
- JWT token expiry: 7 days
- Reservation expiry: 14 days (auto-revert)
- Database locks: SERIALIZABLE isolation for approval operations
- LGPD compliance: 2-year image retention, soft-delete with anonymization

**Scale/Scope**: 100 active users, ~500 materials (~1GB storage at 2MB avg), 8 user stories, 43 FR, 15+ REST endpoints, 5 state machines, 6 enums, 7 JSON contracts

---

## Constitution Check

✅ **GATE PASSED** - All requirements align with constitution principles.

| Principle | Alignment | Notes |
|-----------|-----------|-------|
| **I. Social Impact & Educational Access** | ✅ | Core feature: connects students with surplus materials; reduces educational inequality |
| **II. Android-Native Architecture (Immutable)** | ✅ | Spec commits: exclusive Android (API 26+, Kotlin/Compose); Spring Boot backend; no web/iOS |
| **III. Security Through OAuth2 & Deterministic Processes** | ✅ | Google OAuth2 + JWT; atomic approval with SELECT...FOR UPDATE locks; no race conditions |
| **IV. Data Privacy & LGPD Compliance** | ✅ | Two-stage consent (platform + AI); image retention 2 years; soft-delete with anonymization |
| **V. MVP Scope Discipline** | ✅ | No collaborative filtering, ML recommendations, condition automation, multi-needs, cloud storage |
| **VI. Rule-Based Deterministic Matching** | ✅ | Structured enums only; geographic normalization (text-based, no GPS); deterministic 7-step filter (5 core + 1 optional publication range) + rank algorithm |

---

## Project Structure

### Documentation (this feature)

```text
specs/001-ecobook-core/
├── spec.md                          # Feature specification (complete requirements + contracts)
├── plan.md                          # This file (implementation plan + phases)
├── research.md                      # Phase 0 output (risk mitigation, design decisions)
├── data-model.md                    # Phase 1 output (entities, relationships, validation)
├── contracts/
│   ├── material-api.md              # POST /materiais, GET /materiais, PATCH /materiais
│   ├── solicitacao-api.md           # POST /solicitacoes, PATCH /solicitacoes
│   ├── user-api.md                  # POST /auth/register, PATCH /usuarios/{id}, GET /usuarios/me
│   ├── notification-schema.md       # FCM payload schemas (6 types)
│   └── ai-response.md               # POST /materiais/preview response structure
├── quickstart.md                    # Phase 1 output (developer setup, running locally)
└── checklists/
    └── requirements.md              # Quality validation (✅ ALL PASS)
```

### Source Code (repository root)

```text
# Android Frontend (Kotlin + Jetpack Compose)
android/
├── app/
│   ├── src/main/kotlin/com/ecobook/
│   │   ├── MainActivity.kt
│   │   ├── di/                          # Dependency injection (Hilt)
│   │   ├── ui/
│   │   │   ├── screens/
│   │   │   │   ├── auth/                # OAuth2 login, register
│   │   │   │   ├── onboarding/          # Profile completion (city, neighborhood, needs)
│   │   │   │   ├── material/            # Upload, preview AI, confirm, list
│   │   │   │   ├── discovery/           # Search, filter, ranking
│   │   │   │   ├── request/             # Create, list, approve/decline
│   │   │   │   └── profile/             # User profile, settings, consent
│   │   │   ├── components/              # Reusable Compose components
│   │   │   └── navigation/              # Navigation graph
│   │   ├── domain/
│   │   │   ├── models/                  # Data classes (User, Material, Solicitacao)
│   │   │   ├── usecases/                # Business logic (upload, search, request)
│   │   │   └── repository/              # Repository interfaces
│   │   ├── data/
│   │   │   ├── local/                   # SharedPreferences (tokens, user prefs)
│   │   │   ├── remote/                  # Retrofit API clients
│   │   │   └── repository/              # Repository implementations
│   │   ├── util/
│   │   │   ├── Constants.kt
│   │   │   ├── DateFormatter.kt
│   │   │   └── FileUtil.kt              # Image compression, validation
│   │   └── fcm/
│   │       └── EcoBookMessagingService.kt  # FCM notification handling
│   ├── src/test/kotlin/                 # Unit tests (Mockito)
│   └── src/androidTest/kotlin/          # Integration tests (Compose, Espresso)
├── build.gradle.kts                     # Dependencies, plugin configs
└── AndroidManifest.xml                  # Permissions (INTERNET, CAMERA, READ_EXTERNAL_STORAGE, FCM)

# Backend (Spring Boot + Java)
backend/
├── src/main/java/com/ecobook/
│   ├── EcoBookApplication.java          # Spring Boot entry point
│   ├── config/
│   │   ├── SecurityConfig.java          # OAuth2 + JWT configuration
│   │   ├── DatabaseConfig.java          # PostgreSQL connection pooling
│   │   └── CorsConfig.java              # CORS policy
│   ├── controller/
│   │   ├── AuthController.java          # POST /auth/register, POST /auth/login
│   │   ├── UsuarioController.java       # GET /usuarios/me, PATCH /usuarios/{id}
│   │   ├── MaterialController.java      # POST /materiais, GET /materiais, PATCH /materiais
│   │   ├── MaterialPreviewController.java  # POST /materiais/preview (AI)
│   │   ├── SolicitacaoController.java   # POST /solicitacoes, GET /solicitacoes, PATCH /solicitacoes
│   │   └── NotificacaoController.java   # GET /notificacoes (optional)
│   ├── service/
│   │   ├── UsuarioService.java
│   │   ├── MaterialService.java         # Material lifecycle, state transitions
│   │   ├── SolicitacaoService.java      # Request lifecycle, approval logic
│   │   ├── GeminiService.java           # Gemini API integration, confidence parsing
│   │   ├── MatchingService.java         # Deterministic matching algorithm
│   │   ├── FcmService.java              # Firebase notification dispatch
│   │   ├── GeoNormalizationService.java # Geographic data normalization
│   │   └── ImageStorageService.java     # File upload/promotion/cleanup
│   ├── repository/
│   │   ├── UsuarioRepository.java       # Spring Data JPA
│   │   ├── MaterialRepository.java      # with custom queries for matching
│   │   ├── SolicitacaoRepository.java
│   │   └── NotificacaoRepository.java   # (optional, audit trail)
│   ├── model/
│   │   ├── Usuario.java
│   │   ├── Material.java
│   │   ├── Solicitacao.java
│   │   ├── enums/                       # Disciplina.java, NivelEnsino.java, etc.
│   │   └── dto/                         # DTOs for API contracts
│   ├── exception/
│   │   ├── ProfileIncompleteException.java
│   │   ├── InvalidStateTransitionException.java
│   │   ├── ConflictException.java       # (Material already reserved)
│   │   └── GlobalExceptionHandler.java  # REST error responses
│   ├── util/
│   │   ├── JwtTokenProvider.java
│   │   ├── ValidationUtil.java          # Enum, WhatsApp, image validation
│   │   └── MimeTypeDetector.java
│   └── job/
│       └── ReservationExpiryJob.java    # Daily task: revert expired RESERVADO materials
├── src/main/resources/
│   ├── application.yml                  # Database, OAuth2, Gemini API keys (env vars)
│   └── schema.sql                       # PostgreSQL DDL
├── src/test/java/                       # JUnit 5 tests
├── pom.xml or build.gradle.kts          # Maven or Gradle dependencies
└── docker-compose.yml                   # PostgreSQL dev environment

# Shared Documentation
docs/
├── architecture.md                      # System diagram, layer descriptions
├── state-machines.md                    # Material + Solicitacao state diagrams
├── api-specification.md                 # OpenAPI/Swagger spec
├── database-schema.md                   # ER diagram, table descriptions
├── deployment.md                        # Docker, CI/CD, production checklist
└── gemini-integration.md                # Prompt template, parsing, confidence rules
```

**Structure Decision**: Selected **Option 3: Mobile + API** architecture to align with Constitution Principle II (Android-native frontend + Spring Boot backend). Backend and frontend are separate but tightly coupled via REST API contracts. Monolithic Spring Boot backend (no microservices) for MVP simplicity. Local filesystem for image storage (no S3/GCS) per Constitution V (MVP scope discipline).

---

## Complexity Tracking

| Aspect | Complexity | Justification | Mitigation |
|--------|-----------|---|---|
| **Gemini Integration** | High | Free-tier API rate limits (250 req/day), timeout handling, confidence parsing | Phase 3 testing (≥20 diverse images), fallback UI rules, 10s timeout |
| **State Machines** | High | 2 entities × 4–5 states + transitions + invariants + atomic locking | Explicit state diagram, SELECT...FOR UPDATE for approval, daily expiry job |
| **Image Pipeline** | Medium | 10-step workflow (upload → temp → Gemini → parse → validate → persist → promote) | Idempotent upload_id tracking, clear validation gates, error recovery |
| **Matching Algorithm** | Medium | 7-step deterministic pipeline (5 core + 1 optional publication range) with geographic ranking; correctness critical | Comprehensive unit tests for range filters, property-based tests (QuickCheck), audit logs |
| **Geographic Normalization** | Low | Text-based normalization (uppercase + NFD + ASCII); must be consistent | Unit test all variations, pre-populate reference city/neighborhood table |
| **FCM Notifications** | Medium | 6 event types, reliable delivery required but not guaranteed, retry logic | Test in QA environment, log all delivery attempts, fallback UI indication |
| **Concurrent Users** | Medium | Database locks, connection pooling, query optimization needed | HikariCP pooling, SERIALIZABLE isolation for critical ops, index strategy |
| **Android Integration** | Medium | OAuth2 flow, JWT handling, image capture/compression, file permissions | Use well-tested libraries (Retrofit, OkHttp), test on 3+ device types |

---

## Implementation Phases

### Phase 0: Analysis & Scope (Weeks 1–2)

**Goals**: Define system boundaries, resolve technical unknowns, create detailed risk mitigation plan.

**Deliverables**: `research.md` (design decisions + risk analysis)

**Activities**:
1. **Gemini Integration Risk Analysis**
   - Research: Gemini 2.5 Flash capabilities on Brazilian educational materials
   - Find: 20 sample textbook images (5+ disciplines, 3+ education levels)
   - Baseline: Test classification accuracy, identify common failure modes
   - Document: Confidence thresholds, expected LOW_CONFIDENCE/FAILURE rates, fallback UI design

2. **Image Quality Impact Study**
   - Research: Camera API (Android), image compression strategies
   - Find: Library for Kotlin/Android image validation (MIME, size, resolution)
   - Test: Compression filters, blur detection, contrast analysis
   - Document: Acceptable quality thresholds, user guidance

3. **API Rate Limit Strategy**
   - Research: Gemini free tier limits (250 req/day), paid tier pricing
   - Calculate: MVP load (~500 requests/month = 16/day avg, ~12 Peak/day) ✓ within free tier
   - Plan: Graceful degradation if rate-limited, user notification

4. **Database Schema & Locking Strategy**
   - Research: PostgreSQL SERIALIZABLE isolation, SELECT...FOR UPDATE syntax
   - Design: Transaction isolation levels for approval operations
   - Test: Race condition scenarios (two concurrent approvals for same material)

5. **OAuth2 & JWT Implementation**
   - Research: Google OAuth2 flow for Android (AppAuth library)
   - Design: JWT token format, 7-day expiry, refresh token strategy
   - Integrate: Spring Security, OAuth2ResourceServer

6. **Storage Architecture Validation**
   - Research: Local filesystem vs cloud storage trade-offs (Constitution V constraint)
   - Design: Directory structure (`/uploads/{user_id}/{uuid}.ext`), cleanup strategy
   - Plan: 2-year retention policy, automated purge job

**Output**: `research.md` with 6 sections above, all NEEDS CLARIFICATION resolved.

---

### Phase 1: Design & Contracts (Weeks 3–4)

**Prerequisites**: Phase 0 `research.md` complete

**Goals**: Define data model, API contracts, developer setup.

**Deliverables**: `data-model.md`, `/contracts/`, `quickstart.md`, updated agent context

**Activities**:

1. **Data Model & Schema**
   - **Entities**: Usuario, Material, Solicitacao, with enums (7 types)
   - **Fields**: All from spec (RF-008, RF-026 requirements)
   - **Relationships**: 1 Usuario → many Materials (donor), many Solicitacoes (student); Material → max 1 APROVADA Solicitacao
   - **Validation**: Enum constraints, WhatsApp format (E.164), geographic normalization, image type
   - **State Transitions**: Material (DISPONIVEL → RESERVADO → DOADO/CANCELADO), Solicitacao (PENDENTE → APROVADA → CONCLUIDA/RECUSADA/CANCELADA)
   - **Indexes**: On status, disciplina, nivel_ensino, cidade, bairro for fast filtering
   - Output: `data-model.md` + PostgreSQL DDL script

2. **API Contracts** (in `/contracts/`)
   - **material-api.md**: POST /materiais, GET /materiais, PATCH /materiais (methods, payloads, status codes)
   - **solicitacao-api.md**: POST /solicitacoes, PATCH /solicitacoes (approval/decline/cancel), GET /solicitacoes
   - **user-api.md**: POST /auth/register, PATCH /usuarios/{id}, GET /usuarios/me
   - **ai-response.md**: POST /materiais/preview response (status_ia, best_prediction, upload_id)
   - **notification-schema.md**: 6 FCM payloads (SOLICITACAO_RECEBIDA, APROVADA, RECUSADA, CANCELADA, MATERIAL_DOADO, MATERIAL_CANCELADO)
   - Each contract includes: HTTP method, path, request body, response body (with types), status codes, business rules

3. **Quickstart Guide** (`quickstart.md`)
   - Prerequisites: Java 17, Kotlin 1.9, Android SDK 26+, PostgreSQL 14, Docker
   - Backend setup:
     - Clone repo, create `.env` with OAuth2 credentials + Gemini API key
     - `docker-compose up` (PostgreSQL)
     - `./gradlew bootRun` or `mvn spring-boot:run`
     - Verify: http://localhost:8080/actuator/health
   - Android setup:
     - Android Studio 2023.x+, SDK minSdk=26
     - Create Firebase project, download `google-services.json`
     - Update `local.properties` with SDK path
     - Run emulator: `./gradlew connectedAndroidTest`
   - First test:
     - Start backend, start Android emulator
     - Register user via OAuth2
     - Upload test image, verify Gemini parsing
     - Create search needs, verify matching algorithm

4. **Agent Context Update**
   - Run `.specify/scripts/powershell/update-agent-context.ps1 -AgentType claude`
   - Add to Claude context: Spring Boot 3.x, Kotlin, Jetpack Compose, PostgreSQL, Gemini API, FCM
   - Preserve any manual additions between marker comments

**Output**: 
- `data-model.md` (5 entities, relationships, validation, DDL)
- `/contracts/` (5 JSON schema files, 15+ endpoints)
- `quickstart.md` (dev setup, first test, troubleshooting)
- Updated `.specify/memory/agent-context-claude.md`

---

### Phase 2: Prototypes & Integration Prep (Weeks 5–7)

**Goals**: Build working prototypes, prepare for Phase 3 integration testing.

**Deliverables**: Backend skeleton (API endpoints), Android skeleton (UI screens), integration test suite

**Activities**:

1. **Backend Skeleton**
   - Spring Boot project setup (Spring Boot 3.1, Spring Data JPA, PostgreSQL driver)
   - OAuth2 + JWT configuration (Spring Security)
   - Controllers for 15+ endpoints (stubs returning mock data)
   - Repository layer with custom queries for matching algorithm
   - Service layer structure (UsuarioService, MaterialService, SolicitacaoService, GeminiService, MatchingService)
   - Exception handling (custom exceptions + GlobalExceptionHandler)
   - Unit tests (40% coverage target for this phase)

2. **Android Skeleton**
   - Jetpack Compose project setup (minSdk=26, targetSdk=34)
   - OAuth2 login screen (using AppAuth library)
   - Onboarding flow (profile completion: city, neighborhood, academic needs)
   - Material upload screen (image picker, file validation, preview)
   - Discovery/search screen (basic filter, results list)
   - Request flow (create, list, approve/decline)
   - Bottom navigation (Home, Upload, Requests, Profile)
   - Navigation graph with deep linking
   - Local storage (SharedPreferences for JWT token, user prefs)
   - Dependency injection (Hilt)

3. **Integration Test Infrastructure**
   - Spring Boot TestContainers for PostgreSQL integration tests
   - Retrofit mock server for API contract validation
   - Gemini API mock (hardcoded responses) for image processing tests
   - FCM stub for notification testing
   - Integration test suite covering: user registration, material upload (without Gemini), search algorithm, request workflow, state transitions

4. **CI/CD Pipeline Setup** (optional for Phase 2, recommended)
   - GitHub Actions workflows for backend (build, lint, test)
   - GitHub Actions for Android (build, lint, Espresso tests)
   - Coverage reporting (Codecov)

**Output**: 
- Runnable Spring Boot backend (API stubs, ready for Phase 3 Gemini integration)
- Runnable Android app (UI screens, OAuth2 flow, mock API calls)
- Integration test suite (30+ tests)
- CI/CD workflows (GitHub Actions)

---

### Phase 3: Image Collection & AI Integration Testing (Weeks 8–10)

**Goals**: Validate Gemini integration with real data, measure confidence distributions, finalize AI fallback UX.

**Deliverables**: Research dataset (20+ images), AI integration report, updated fallback rules if needed

**Activities**:

1. **Image Dataset Collection** (RFC 7.3.2: "at least 20 images")
   - Sources:
     - Public textbook covers (MATEMATICA: 5, PORTUGUES: 5, HISTORIA: 4, GEOGRAFIA: 3, CIENCIAS: 3, LITERATURA: 1 = 21 images)
     - Diverse education levels (FUNDAMENTAL, MEDIO, SUPERIOR)
     - Diverse school systems (ANGLO, OBJETIVO, COC, POSITIVO, OUTRO)
   - Preprocessing: Resize to 1-2MB each, verify JPEG/PNG format
   - Store in test directory with metadata (expected_disciplina, expected_level, expected_system)

2. **Gemini Integration Testing**
   - Implement GeminiService.java with real API calls (not mock)
   - Test each image through POST /materiais/preview endpoint
   - Collect results: status_ia (SUCCESS/LOW_CONFIDENCE/FAILURE), confidence scores, predicted fields
   - Analyze:
     - SUCCESS rate (target: ≥ 75% based on RFC 7.6)
     - LOW_CONFIDENCE rate (target: 15–25%)
     - FAILURE rate (target: ≤ 10% excluding timeouts)
     - Timeout incidents (< 5% of calls)
     - Accuracy on predicted disciplines (if manual labels available)
   - Identify failure patterns: blurry images, regional systems not recognized, atypical covers

3. **AI Fallback UX Refinement**
   - Based on real Gemini results, adjust confidence thresholds if needed
   - Create user guidance: "High confidence: fields auto-filled", "Low confidence: please review", "Unable to recognize: enter manually"
   - Design visual indicators (checkmark, warning icon, question mark)
   - Plan copy/messaging for confidence levels in Portuguese

4. **Timeout Handling Validation**
   - Simulate network latency (throttle requests)
   - Test 10-second timeout behavior
   - Verify graceful degradation: user presented with empty form
   - Verify retry logic: user can attempt upload again

5. **Rate Limit Validation**
   - 20 images × 3 tests per image = 60 API calls (well under 250/day limit)
   - Verify no rate-limit errors observed
   - Plan for production: monitor daily call count, alert if approaching 250 threshold

**Output**:
- `research.md` (Phase 3 AI Integration section): 20+ test images, success rates, confidence distributions
- Gemini integration report: accuracy analysis, failure modes, recommendations
- Updated GeminiService.java with production-ready error handling
- Updated UI design doc: confidence level visual indicators, user messaging

---

### Phase 4: Iterative Development & Full Integration (Weeks 11–15)

**Goals**: Implement all functional requirements, complete state machines, end-to-end testing.

**Deliverables**: Feature-complete system, integration tests (85%+ coverage), performance benchmarks

**Activities**:

1. **Backend Implementation (43 FR + 5 NFR)**
   - Complete GeminiService with all parsing rules (RF-011 through RF-021)
   - Implement MatchingService with 7-step algorithm (RF-022 through RF-025, RF-044)
   - Material state machine (RF-005 through RF-006): DISPONIVEL → RESERVADO (14-day expiry) → DOADO/CANCELADO
   - Solicitacao state machine (RF-026 through RF-031): PENDENTE → APROVADA → CONCLUIDA/RECUSADA/CANCELADA
   - Atomic approval (RF-035): SELECT...FOR UPDATE on Material row, transaction isolation SERIALIZABLE
   - Geographic normalization (RF-003): uppercase + NFD decomposition + ASCII-only
   - Profile completeness blocking (RF-002): HTTP 403 on protected endpoints if perfil_completo = false
   - Image upload + storage (RF-010, RF-016): upload_id tracking, temporary → permanent promotion
   - WhatsApp validation (RF-041): E.164 format enforcement
   - FcmService: 6 notification types (RF-037 through RF-038)
   - Reservation expiry job: daily run, revert RESERVADO to DISPONIVEL after 14 days
   - Enum validation (RF-039 through RF-040): HTTP 400 on invalid values

2. **Android Implementation**
   - Complete all screens: auth, onboarding, material upload, discovery, requests, profile
   - Image capture/compression (JPEG/PNG, ≤5MB)
   - Gemini confidence UI: auto-filled (SUCCESS), editable with warning (LOW_CONFIDENCE), empty (FAILURE)
   - Offline token refresh (7-day JWT expiry)
   - FCM integration: receive all 6 notification types, display in notification center + in-app toast
   - Settings: consent toggles (platform + AI usage)
   - Error handling: network errors, validation errors, API errors with user-friendly messages

3. **Database**
   - PostgreSQL schema with 5 entity tables + junction tables
   - Indexes: status, disciplina, nivel_ensino, cidade, bairro (composite indexes for filtering)
   - Constraints: FK relationships, enum check constraints, NOT NULL enforcements
   - Migration scripts for future updates

4. **Integration & Performance Testing**
   - End-to-end flows: registration → upload → search → request → approval → donation
   - State transition validation: verify all invalid transitions return HTTP 422
   - Concurrency tests: two concurrent approval requests for same material (verify locking)
   - Matching algorithm correctness: verify ranking order across 50+ material combinations
   - Performance benchmarks:
     - Search latency: P95 < 2s (populate 500 materials, run varied queries)
     - Upload + classification: < 3min end-to-end
     - Approval latency: < 10s state → state
   - FCM reliability: 95%+ delivery rate (measured in test environment)

5. **Documentation & Code Quality**
   - API documentation (OpenAPI/Swagger)
   - Architecture decision records (ADRs)
   - Code review checklist
   - Deployment runbook

**Output**:
- Feature-complete backend (all 43 FR implemented)
- Feature-complete Android app (all 8 user stories implemented)
- Integration test suite (85%+ coverage)
- Performance benchmark report
- API documentation (Swagger/OpenAPI)

---

### Phase 5: Launch & Monitoring (Weeks 16–17)

**Goals**: Release MVP, monitor production, collect user feedback.

**Deliverables**: Deployed system, monitoring dashboard, feedback loop

**Activities**:

1. **Deployment**
   - Backend: Docker container, deploy to production server (Linux, 2GB+ RAM, PostgreSQL)
   - Android: Build signed APK, release to internal testing track (Google Play)
   - Database: Backup strategy, recovery procedure

2. **Monitoring & Observability**
   - Application logs: structured logging (JSON format) with correlation IDs
   - Error tracking: Sentry or similar for crash reporting
   - Performance monitoring: response time histograms, database query times
   - Gemini API monitoring: rate limit tracking, timeout frequency, success rates
   - FCM monitoring: delivery rates, failure reasons
   - Database monitoring: connection pool utilization, slow query log

3. **User Feedback & Iteration**
   - In-app feedback form (simple rating + comment)
   - Monitor user registration + engagement metrics
   - Collect material upload success rates
   - Analyze matching algorithm relevance (did user find useful materials?)
   - Plan post-MVP improvements based on feedback

4. **Operational Runbooks**
   - Incident response: Gemini API outage, database connection pool exhaustion, high FCM failure rate
   - Routine maintenance: database backups, log cleanup, image storage maintenance (2-year purge)
   - Scaling strategy: if user base grows beyond 100, upgrade database, optimize queries

**Output**:
- Deployed MVP system (backend + Android)
- Monitoring dashboard (Grafana or CloudWatch)
- Operational runbooks
- User feedback summary (first 2 weeks)

---

## Risk Mitigation & Contingency

| Risk | Impact | Probability | Mitigation | Contingency |
|------|--------|-------------|-----------|-------------|
| **Gemini API bias** | LOW_CONFIDENCE/FAILURE rates > 25% for Brazilian materials | Medium | Phase 3 testing (20+ diverse images), regional textbook sourcing | Expand fallback manual entry UX, partner with educators for guidance |
| **Image quality variability** | Low image quality → high FAILURE rates | Medium | Phase 1 research (image validation libs), Phase 2 user guidance | Implement basic image preprocessing (contrast/blur detection) |
| **Gemini rate limit exceeded** | API calls rejected, users blocked | Low | Monitor usage (MVP: 16/day avg, 12 peak/day), alert at 80% threshold | Implement client-side request queue, prioritize verified users |
| **Database lock contention** | Approval operations timeout | Low | Phase 1 research (SERIALIZABLE isolation), Phase 4 testing | Add READ COMMITTED with optimistic locking if needed |
| **FCM delivery failures** | Users miss critical notifications | Medium | Phase 4 testing (95%+ target), server-side retry | In-app notification fallback, email backup (future) |
| **State machine violation** | Data inconsistency (e.g., DOADO material with pending requests) | Low | Explicit state diagram, comprehensive unit tests, invariant checks on DB | Database constraints (CHECK clauses), periodic audit job |
| **User abandonment in onboarding** | Low adoption despite free platform | Medium | Phase 2 prototype testing with 5–10 users, UX iteration | Simplify onboarding (fewer required fields), provide templates |
| **Image storage capacity** | Disk full at 1GB (MVP limit) | Low | Plan 1GB initially, auto-cleanup after 2 years | Migrate to cloud storage (S3/GCS) if expansion needed |
| **Team skill gaps** | Delayed implementation (Kotlin, Gemini, PostgreSQL) | Medium | Early Phase 1 training, external code reviews | Pair programming, vendor support (Google, Google Cloud) |

---

## Success Criteria & Validation

### Phase Completions

| Phase | Criterion | Validation |
|-------|-----------|-----------|
| **Phase 0** | `research.md` complete, all NEEDS CLARIFICATION resolved | Code review: Spec Lead + CTO |
| **Phase 1** | Data model + contracts + quickstart defined; agent context updated | Peer review: backend + Android leads |
| **Phase 2** | Backend skeleton + Android skeleton runnable; integration test suite passes | Test execution: 30+ passing tests |
| **Phase 3** | 20+ images tested, Gemini confidence distribution documented | Gemini success rate ≥ 75%, analysis complete |
| **Phase 4** | All 43 FR implemented, 85%+ integration test coverage, performance benchmarks met | Full test suite passes, perf within SLAs |
| **Phase 5** | System deployed, monitoring active, user feedback collected | Production uptime ≥ 99.5% (1 week baseline) |

### MVP Success Metrics

- **User Adoption**: ≥ 50 active users within 4 weeks of internal release
- **Material Catalog**: ≥ 250 materials uploaded within 4 weeks
- **Matching Accuracy**: ≥ 80% of discovered materials match student queries (user satisfaction survey)
- **AI Reliability**: Gemini SUCCESS rate ≥ 75%, LOW_CONFIDENCE ≤ 25%, FAILURE ≤ 10%
- **System Reliability**: Backend uptime ≥ 99.5%, FCM delivery ≥ 95%
- **Performance**: P95 search latency < 2s, upload < 3min, approval < 10s
- **Code Quality**: ≥ 80% test coverage, zero critical security issues in code review

---

## Dependencies & Sequencing

**Critical Path** (must complete in order):
1. Phase 0 research (2 weeks) → informs Phase 1 design
2. Phase 1 design (2 weeks) → defines Phase 2 implementation
3. Phase 2 prototypes (3 weeks) → basis for Phase 3 testing
4. Phase 3 AI testing (3 weeks) → validates technical feasibility
5. Phase 4 full development (5 weeks) → feature complete
6. Phase 5 launch (2 weeks) → deployed MVP

**Inter-Phase Dependencies**:
- Phase 1 contracts are input to Phase 2 (backend controllers must match contracts)
- Phase 2 backend skeleton must support Phase 3 Gemini testing (GeminiService, POST /materiais/preview)
- Phase 3 AI results feed into Phase 4 fallback UX refinements
- Phase 4 integration tests validate Phase 3 Gemini + matching algorithm

**Team Allocation** (suggested):
- **Backend Lead**: Phases 0–5 (architect, implement GeminiService, MatchingService, state machines)
- **Android Lead**: Phases 0–5 (UI architect, implement screens, OAuth2, FCM)
- **QA Lead**: Phases 3–5 (test plan, image collection, performance testing, monitoring)
- **DevOps**: Phase 2–5 (infrastructure, CI/CD, Docker, monitoring)

