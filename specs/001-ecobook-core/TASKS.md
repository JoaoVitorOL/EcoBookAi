# EcoBook IA — Implementation Tasks

**Project**: EcoBook IA - AI-Powered Material Donation Matching Platform  
**Phase**: 1–10 (Setup through Polish & Launch)  
**Total Tasks**: 230  
**Status**: Phase 5 request workflow, Phase 6 notification workflow, Phase 7 admin/moderation runtime, and the Phase 8 LGPD/security runtime are implemented; Phase 9 is in progress through response compression, Micrometer/Prometheus observability, smoke coverage, authenticated-read caching, immutable reference-data caching/catalog sync, discovery cursor pagination, rollback cleanup, visible terms/privacy consent UX and profile self-service updates while Firebase real-device validation remains the main external closeout item  
**Generated**: 2026-04-15

---

## Overview

This document organizes all implementation tasks for EcoBook IA across **10 implementation phases** (Weeks 5–17). Tasks are:
- **Organized by user story** (5 P1 stories → 3 P2 stories → Polish)
- **Granular and independently implementable** (each task ≤ 4 hours work)
- **Labeled with RFC references** (maps to spec.md requirements)
- **Marked for parallelization** where dependencies allow
- **File-specific** (exact implementation paths provided)

### Task Completion Checklist Format

```
- [ ] [TaskID] [P?] [Story?] Description with exact file path
```

**Key**: 
- `[P]` = Parallelizable (no dependency on incomplete tasks)
- `[Story?]` = User story label (US1, US2, etc.)
- `[TaskID]` = Sequential task ID (T001, T002, etc.)
- Descriptive title with exact file path

Historical note:
- Some unchecked items below are environment/manual setup tasks or legacy follow-ups whose original intent is already covered elsewhere after the auth rebaseline. They should not be treated as phase gates by themselves.
- Phase 5 request workflow and part of Phase 6 notifications were implemented after the original checkbox pass.
- Some unchecked Phase 5/6 items below were not backfilled one by one; use `PLAN-SUMMARY.md` and `contracts/` as the current runtime truth.
- Local operational startup is now revalidated through the `local` backend profile plus the README/quickstart runbooks updated on `2026-05-21`.
- The dependency graph and several summary ranges below came from an earlier 370-task draft; the detailed sections `T181–T230` plus `PLAN-SUMMARY.md` are the current runtime truth for Phases 7–10.

---

## Dependency Graph & Execution Order

```
Phase 1: Setup & Foundational
├─ T001–T015: Backend Skeleton (Spring Boot structure)
├─ T016–T025: Android Skeleton (Jetpack Compose structure)
├─ T026–T035: Integration Test Infrastructure
└─ [All parallelizable via [P] marker]

Phase 2: User Story 1 (US1) — Registration & Profile
├─ T036–T055: Authentication module (email/password, JWT, User endpoints)
├─ T056–T075: Onboarding module (profile completion, validation)
└─ [Depends on: Phase 1 complete]

Phase 3: User Story 2 (US2) — AI-Assisted Classification
├─ T076–T110: Material upload module (upload UI, endpoint, storage)
├─ T111–T140: Gemini integration (service, parsing, fallback rules)
├─ T141–T160: Classification UI (preview screens, confidence indicators)
└─ [Depends on: Phase 2 (authentication required)]

Phase 4: User Story 3 (US3) — Material Discovery
├─ T161–T175: Matching algorithm module (7-step filter: 5 core + 1 optional publication range, ranking)
├─ T176–T185: Discovery UI (search form, results, filtering)
└─ [Depends on: Phase 2 (material data required)]

Phase 5: User Story 4 (US4) — Request Submission
├─ T186–T200: Request creation & state machine module
├─ T201–T215: Approval workflow (donor approval, atomic locking)
├─ T216–T225: Request UI (create, list, status updates)
└─ [Depends on: Phase 3 (material required)]

Phase 6: User Story 5 (US5) — Donation Completion
├─ T226–T240: Completion workflow (DOADO state, endpoint)
├─ T241–T250: Completion UI (mark donated, confirmation)
└─ [Depends on: Phase 5 complete]

Phase 7: Admin & Moderation (Modules 7–8)
├─ T181–T183: Non-received material reporting
├─ T184–T185: Admin dashboard, moderation, admin authorization
└─ [Depends on: Phase 5]

Phase 8: Security & LGPD (Module 9 — Cross-Cutting)
├─ T186–T191: Soft-delete with anonymization
├─ T192–T206: Consent management, image access control, audit trails
└─ [Depends on: All modules, run in parallel if possible]

Phase 9: Performance, Optimization & Edge Cases
├─ T207–T212: Performance tuning and monitoring
├─ T213–T215: Error handling, rollback and boundary scenarios
├─ T216–T218: End-to-end, load and smoke testing
└─ [Run after the functional modules are stable]

Phase 10: Polish & Documentation
├─ T219–T225: Documentation, code quality and API descriptions
├─ T226–T230: Final quality gates and release validation
└─ [Final validation before launch]
```

---

## PHASE 1: SETUP & FOUNDATIONAL (Weeks 5–7)

### T001–T015: Backend Skeleton Setup

- [x] **T001** [P] Create Spring Boot 3.x project with Maven structure in `EcoBookAiBackend/`
- [x] **T002** [P] Configure `pom.xml` with dependencies: Spring Web, Spring Data JPA, Spring Security, Spring Cloud, PostgreSQL driver, JWT (io.jsonwebtoken), Gemini integration client, Firebase Admin SDK, Lombok
- [x] **T003** [P] Create `application.yml` configuration with database URL, auth properties, JWT secret, Gemini API key placeholder, FCM service account path
- [x] **T004** [P] Setup database connection pooling in `src/main/java/com/ecobook/config/DataSourceConfig.java` (HikariCP, max 20, min 5, validation query)
- [x] **T005** [P] Create Spring Data JPA entity mappings in `src/main/java/com/ecobook/model/`:
  - `Usuario.java` (13 fields per data-model.md)
  - `Material.java` (15 fields per data-model.md)
  - `Solicitacao.java` (8 fields per data-model.md)
- [x] **T006** [P] Create PostgreSQL DDL migration script `src/main/resources/db/migration/V1__initial-schema.sql` with all enums, tables, indexes, constraints from data-model.md
- [x] **T007** [P] Setup Flyway database migrations in `pom.xml` and `application.yml` (auto-migrate on startup)
- [x] **T008** [P] Create REST exception handler in `src/main/java/com/ecobook/exception/GlobalExceptionHandler.java` (400, 403, 404, 409, 422, 500 mappings)
- [x] **T009** [P] Create controller package structure in `src/main/java/com/ecobook/controller/` (stub controllers for Phase 2 endpoints)
- [x] **T010** [P] Create service package structure in `src/main/java/com/ecobook/service/` (service interfaces per module)
- [x] **T011** [P] Create repository package structure in `src/main/java/com/ecobook/repository/` (Spring Data JPA repository interfaces)
- [x] **T012** [P] Create DTO package in `src/main/java/com/ecobook/dto/` (request/response DTOs for all endpoints)
- [x] **T013** [P] Setup Spring Security configuration in `src/main/java/com/ecobook/config/SecurityConfig.java` (CORS, CSRF, JWT filter)
- [x] **T014** [P] Create actuator health endpoint at `src/main/java/com/ecobook/controller/HealthController.java` (returns HTTP 200 on startup)
- [x] **T015** [P] Setup Lombok annotations in `pom.xml` (IDE plugin remains a workstation prerequisite outside version control)

### T016–T025: Android Project Setup

- [x] **T016** [P] Create Android project in `EcoBookAiAndroid/` with minSdk=26, targetSdk=34, Kotlin language, Jetpack Compose UI
- [x] **T017** [P] Add dependencies to `EcoBookAiAndroid/build.gradle.kts`:
  - Jetpack Compose (latest)
  - Jetpack Navigation Compose
  - AndroidX Security Crypto
  - Retrofit 2 (HTTP client)
  - OkHttp3 (HTTP logging)
  - Hilt (dependency injection)
  - FirebaseMessaging (FCM)
  - Coil (image loading)
  - Coroutines
- [x] **T018** [P] Create `EcoBookAiAndroid/src/main/res/values/api_config.xml` with backend_url placeholder (`http://10.0.2.2:8080` for emulator, configurable per build variant)
- [x] **T019** [P] Download `google-services.json` from Firebase Console and place in `EcoBookAiAndroid/app/google-services.json`
- [x] **T020** [P] Create `EcoBookAiAndroid/local.properties` with SDK path and backend URL configuration
- [x] **T021** [P] Setup Hilt application component in `EcoBookAiAndroid/src/main/java/com/ecobook/EcoBookApp.kt` with @HiltAndroidApp annotation
- [x] **T022** [P] Create Retrofit API client in `EcoBookAiAndroid/src/main/java/com/ecobook/api/EcoBookApiClient.kt` (abstract base URL, authentication interceptor placeholder)
- [x] **T023** [P] Create Jetpack Compose navigation graph in `EcoBookAiAndroid/src/main/java/com/ecobook/navigation/NavGraph.kt` (screen routing structure)
- [x] **T024** [P] Create main activity in `EcoBookAiAndroid/src/main/java/com/ecobook/MainActivity.kt` (composable entry point with navigation)
- [x] **T025** [P] Setup EncryptedSharedPreferences in `EcoBookAiAndroid/src/main/java/com/ecobook/utils/SecureStorage.kt` for JWT token storage (read, write, delete operations)

### T026–T035: Integration Test Infrastructure

- [x] **T026** [P] Add test dependencies to backend `pom.xml`: JUnit 5, Mockito, TestContainers (PostgreSQL), Spring Test, REST Assured, MockMvc
- [x] **T027** [P] Create `src/test/java/com/ecobook/config/TestDatabaseConfig.java` with TestContainers PostgreSQL configuration and local PostgreSQL fallback for environments where the Docker/Testcontainers client pair is incompatible
- [x] **T028** [P] Create `src/test/java/com/ecobook/BaseIntegrationTest.java` abstract class with @SpringBootTest, @AutoConfigureMockMvc, @Testcontainers, transaction rollback
- [x] **T029** [P] Create `src/test/java/com/ecobook/util/TestDataBuilder.java` for creating test usuarios, materials, solicitacoes with default values
- [x] **T030** [P] Create GitHub Actions workflow in `.github/workflows/build-and-test.yml` (Maven build, test execution, code coverage reporting)
- [x] **T031** [P] Setup code coverage reporting in `pom.xml` (JaCoCo, target 85% coverage for Phase 4 gate)
- [x] **T032** [P] Create Android test structure in `EcoBookAiAndroid/src/androidTest/java/com/ecobook/` with Compose testing utilities
- [x] **T033** [P] Create mock interceptor in `EcoBookAiAndroid/src/androidTest/java/com/ecobook/api/MockApiInterceptor.kt` for returning stub responses during testing
- [x] **T034** [P] Setup Android emulator configuration in `EcoBookAiAndroid/build.gradle.kts` (API 26+, 1GB RAM, persistent storage path)
- [x] **T035** [P] Create first E2E test scenario in `src/test/java/com/ecobook/FirstIntegrationTest.java` (register → health check → verify database connectivity)

---

## PHASE 2: USER STORY 1 — Registration & Profile (Weeks 8–9)

**Story Goal**: User can create an account with email and password, complete profile (city, neighborhood, WhatsApp), and be verified before accessing material uploads.

**Independent Test Criteria**:
- ✅ Unregistered user completes register flow
- ✅ Registered user completes login flow
- ✅ Profile completion blocking enforcement (403 on restricted operations)
- ✅ Geographic normalization applied consistently
- ✅ JWT token generation and validation working
- ✅ All enum validations enforced

### Module 1: Authentication (RF-001)

#### Backend: Email/Password & JWT

- [x] **T036** [US1] Implement auth configuration in `src/main/java/com/ecobook/config/SecurityConfig.java` (PasswordEncoder, public auth endpoints, JWT validation)
- [x] **T037** [US1] Create `src/main/java/com/ecobook/security/JwtTokenProvider.java` service:
  - Generate JWT (7-day expiry, subject=email, custom claims: role, perfil_completo)
  - Validate token (signature, expiry, claims)
  - Refresh token (if refresh token endpoint needed, add in Phase 4)
- [x] **T038** [US1] Create `src/main/java/com/ecobook/security/JwtAuthenticationFilter.java` (extracts JWT from Authorization header, validates, sets SecurityContext)
- [x] **T039** [US1] Register JWT filter in `SecurityConfig.java` with `addFilterBefore(JwtAuthenticationFilter, UsernamePasswordAuthenticationFilter)`
- [x] **T040** [US1] Create `src/main/java/com/ecobook/controller/AuthController.java`:
  - **POST /api/v1/auth/register**: Accepts email, password, nome; creates Usuario; returns JWT + user info
  - **POST /api/v1/auth/login**: Accepts email and password; verifies credentials; returns JWT + user info
- [x] **T041** [US1] Create `src/main/java/com/ecobook/service/AuthService.java`:
  - `registerUser(request)`: Validate email uniqueness, hash password, create Usuario, generate JWT
  - `loginUser(request)`: Verify password hash, generate JWT
  - Never return raw password or `password_hash`
- [x] **T042** [US1] Update `src/main/java/com/ecobook/repository/UsuarioRepository.java` (Spring Data JPA):
  - `findByEmail(email): Optional<Usuario>`
  - `existsByEmailIgnoreCase(email): boolean`
- [x] **T043** [US1] Create JWT validation test in `src/test/java/com/ecobook/security/JwtTokenProviderTest.java` (generate token, validate, test expiry)
- [x] **T044** [US1] Create auth integration tests in `src/test/java/com/ecobook/AuthControllerIntegrationTest.java` (POST /auth/register, POST /auth/login, password hash verification)
- [x] **T045** [P] [US1] Create endpoint tests for invalid credentials and expired token: POST /auth/login with invalid password -> HTTP 401

#### Android: Email/Password Flow

- [x] **T046** [P] [US1] Create `EcoBookAiAndroid/src/main/java/com/ecobook/auth/AuthScreen.kt` Compose screen:
  - Display `Login` and `Criar conta` forms
  - Inputs: email, password, confirm password (register only), nome (register only)
  - Show loading spinner during authentication
  - Navigate to onboarding on success
- [x] **T047** [P] [US1] Implement auth form validation inside `EcoBookAiAndroid/src/main/java/com/ecobook/auth/AuthViewModel.kt`:
  - Validate email format and empty fields
  - Validate password minimum length
  - Validate password confirmation match
  - Return field-specific error messages
- [x] **T048** [P] [US1] Create `EcoBookAiAndroid/src/main/java/com/ecobook/api/AuthApiService.kt` Retrofit interface:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
- [x] **T049** [P] [US1] Implement `EcoBookAiAndroid/src/main/java/com/ecobook/auth/AuthViewModel.kt`:
  - Trigger register/login requests
  - Store JWT in SecureStorage after login success
  - Handle credential errors (show user-friendly messages)
  - Trigger navigation to OnboardingScreen on success
- [x] **T050** [P] [US1] Create JWT interceptor in `EcoBookAiAndroid/src/main/java/com/ecobook/api/AuthInterceptor.kt`:
  - Read JWT from SecureStorage
  - Inject into `Authorization: Bearer {token}` header for all requests
- [x] **T051** [P] [US1] Add authentication interceptor to Retrofit client in `EcoBookApiClient.kt`
- [x] **T052** [P] [US1] Add dedicated Android JVM validation tests for `AuthViewModel`
- [x] **T053** [P] [US1] Reintroduce a dedicated cross-runtime auth E2E test if Android JVM/instrumentation coverage expands beyond the current backend integration suite + Compose smoke coverage
- [x] **T054** [US1] Create logout functionality in `EcoBookAiAndroid/src/main/java/com/ecobook/auth/LogoutViewModel.kt` (clear JWT from SecureStorage, navigate to login)
- [x] **T055** [US1] Create 401 error handling in `AuthInterceptor.kt` (if token invalid, clear storage and redirect to login)

#### Backend: Protected Endpoints

- [x] **T056** [US1] Create `src/main/java/com/ecobook/controller/UsuarioController.java`:
  - **GET /api/v1/usuarios/me**: Return current user (from JWT claim) with perfil_completo status
  - Response DTO: id, email, nome, whatsapp, cidade, bairro, perfil_completo, role
- [x] **T057** [US1] Implement `UsuarioController.getMe()` handler:
  - Extract user email from SecurityContext
  - Fetch from database
  - Return UsuarioDTO with all fields
- [x] **T058** [US1] Add `@PreAuthorize("hasRole('USER')")` annotation to `getMe()` (requires valid JWT)
- [x] **T059** [US1] Create integration test in `src/test/java/com/ecobook/UsuarioControllerTest.java`:
  - GET /usuarios/me with valid JWT → HTTP 200 with user data
  - GET /usuarios/me without JWT → HTTP 401 Unauthorized
- [x] **T060** [US1] Create standard JSON response wrapper for all controllers (envelope with optional error details)

### Module 2: Onboarding & Profile (RF-001)

#### Backend: Profile Completion Endpoint

- [x] **T061** [US1] Implement `src/main/java/com/ecobook/service/GeoNormalizationService.java`:
  - `normalize(city: String, neighborhood: String): NormalizedGeo`
  - Algorithm: Uppercase + NFD decomposition + ASCII transliteration + trim
  - Test cases: "São João" → "SAO JOAO", "Ribeirão Preto" → "RIBEIRAO PRETO", "São José dos Campos" → "SAO JOSE DOS CAMPOS"
- [x] **T062** [US1] Implement `src/main/java/com/ecobook/validator/WhatsAppValidator.java`:
  - Validate E.164 format: `^\+55\d{10,11}$` (Brazil, 10-11 digits)
  - Reject: "11991234567" (missing +55), "+551199123456" (wrong length), "+55 11 99123456" (spaces)
- [x] **T063** [US1] Add validation annotations to `Usuario.java`:
  - `@Pattern(regexp = "^\\+55\\d{10,11}$")` on whatsapp field
  - `@NotBlank` on nome, cidade, bairro
- [x] **T064** [US1] Create `src/main/java/com/ecobook/controller/UsuarioController.java` endpoint:
  - **PUT /api/v1/usuarios/me**: Update user profile
  - Request body: nome, whatsapp (E.164), cidade, bairro, necessidade_academica (optional enum)
  - Response: Updated UsuarioDTO with perfil_completo status
- [x] **T065** [US1] Implement `UsuarioService.updateProfile()`:
  - Normalize city + neighborhood via GeoNormalizationService
  - Validate WhatsApp format
  - Check all required fields present (nome, whatsapp, cidade, bairro)
  - Update database, set perfil_completo = true
  - Emit domain event: ProfileCompletedEvent (for later analytics/notifications)
- [x] **T066** [US1] Create validation test in `src/test/java/com/ecobook/validator/WhatsAppValidatorTest.java` (valid, invalid, edge cases)
- [x] **T067** [US1] Create profile update integration test in `src/test/java/com/ecobook/UsuarioServiceTest.java`:
  - PUT /usuarios/me with valid data → perfil_completo = true
  - PUT /usuarios/me with invalid WhatsApp → HTTP 400 with field error
  - PUT /usuarios/me with incomplete data → HTTP 422 with missing field errors
- [x] **T068** [US1] Add `@NotNull @NotBlank` validators to nome, whatsapp, cidade, bairro fields in Usuario entity

#### Backend: Profile Completeness Gate

- [x] **T069** [US1] Create `src/main/java/com/ecobook/aspect/ProfileCompletenessAspect.java` (AOP):
  - Intercept methods annotated with `@RequireCompleteProfile`
  - Check SecurityContext user's perfil_completo = true
  - Return HTTP 403 if false: `{"error": "INCOMPLETE_PROFILE", "message": "Conclua seu perfil antes de acessar este recurso"}` 
- [x] **T070** [US1] Create `@RequireCompleteProfile` annotation in `src/main/java/com/ecobook/annotation/RequireCompleteProfile.java`
- [x] **T071** [US1] Mark future endpoints with `@RequireCompleteProfile`: POST /materiais, POST /materiais/{id}/solicitacoes, PUT /materiais/{id}, DELETE /materiais/{id}
- [x] **T072** [US1] Test aspect behavior in `src/test/java/com/ecobook/aspect/ProfileCompletenessAspectTest.java` (incomplete profile → 403, complete → proceed)

#### Android: Onboarding Screen

- [x] **T073** [P] [US1] Create `EcoBookAiAndroid/src/main/java/com/ecobook/onboarding/OnboardingScreen.kt` Compose screen:
  - Display multi-step form (step 1: WhatsApp, step 2: City/Neighborhood, step 3: Academic needs)
  - Fields: WhatsApp (E.164 input with formatting), city (autocomplete dropdown), neighborhood (text input), academic_needs (multi-select enum)
  - Validate WhatsApp on focus loss (regex: `^\+55\d{10,11}$`)
  - Show error messages inline if validation fails
  - Next/Previous buttons between steps
  - Submit button on final step → PUT /usuarios/me
- [x] **T074** [P] [US1] Implement `EcoBookAiAndroid/src/main/java/com/ecobook/onboarding/OnboardingViewModel.kt`:
  - Store form state (nome, whatsapp, cidade, bairro, necessidade_academica)
  - Validation logic: all required fields non-empty, WhatsApp E.164 format
  - `updateProfile()`: Call AuthApiService.putUsuariosMe(requestBody)
  - Handle success: Navigate to MainScreen
  - Handle 400/422 errors: Show field-specific error messages
  - Handle 403: Show "Profile already complete" message, navigate to MainScreen
- [x] **T075** [P] [US1] Deliver free-text city input in the onboarding/profile runtime:
  - Accept manual city entry in the Android form and let the backend normalize before persistence
  - Remove the old curated-city autocomplete dependency from the active flow
- [x] **T076** [P] [US1] Add WhatsApp formatter in `EcoBookAiAndroid/src/main/java/com/ecobook/ui/WhatsAppFormatter.kt`:
  - User types: "11991234567" → automatically format to "+5511991234567"
  - Prevent typing invalid characters (only digits and +)

---

## PHASE 3: USER STORY 2 — AI-Assisted Classification (Weeks 10–12)

**Story Goal**: User can upload a material image, receive AI-powered classification suggestions (with confidence levels), review and edit suggestions, and confirm to persist the material.

**Independent Test Criteria**:
- ✅ Image upload with validation (JPEG/PNG, ≤5MB, MIME check)
- ✅ Gemini API integration with 10-second timeout
- ✅ Confidence-level fallback rules (HIGH/LOW/FAILURE)
- ✅ Temporary file management and promotion
- ✅ Enum validation on all fields
- ✅ Consentimento_ia bypass when false

> Closeout note: the checked tasks below follow the shipped Phase 3 runtime documented in contracts/material-api.md and contracts/ai-response.md. Where the original task text assumed multipart create requests, a separate ReviewViewModel, or deletion of the upload tracking row, the implementation uses the current runtime contract instead: preview upload first, JSON create with upload_id, a shared upload/review ViewModel on Android, and retained upload tracking rows for audit.

### Module 3: Material Upload with IA (RF-002, RF-003, RF-004)

#### Backend: Gemini Integration

- [x] **T077** [US2] Create `src/main/java/com/ecobook/service/GeminiService.java` Spring service and Phase 3 runtime client:
  - Integrate Google Generative AI SDK (Java)
  - Constructor: Initialize GenerativeModel with API key, timeout 10s
- [x] **T078** [US2] Implement `GeminiService.classifyMaterial(imageFile)` method:
  - Accept file as multipart (JPEG/PNG)
  - Build structured JSON prompt (see spec.md):
    - **AI-Assisted Fields** (fields that Gemini will attempt to extract): titulo, disciplina, nivel_ensino, ano, sistema_ensino, estado_conservacao, data_publicacao
    - **Manual-Only Fields** (NEVER auto-populated): descricao (to prevent hallucinations)
    ```json
    {
      "image_base64": "...",
      "fields": ["titulo", "disciplina", "nivel_ensino", "ano", "sistema_ensino", "estado_conservacao", "data_publicacao"]
    }
    ```
  - Call Gemini API with 10-second timeout
  - Return response with status_ia (SUCCESS, LOW_CONFIDENCE, FAILURE) + field predictions + confidence scores
- [x] **T079** [US2] Implement response parsing in `GeminiService.parseGeminiResponse()`:
  - Validate JSON structure (not null, not empty)
  - Validate enum values (disciplina in [MATEMATICA, ...], etc.)
  - Validate confidence scores (0.0–1.0)
  - Handle malformed JSON → status_ia: FAILURE
  - Handle invalid enum → status_ia: LOW_CONFIDENCE for that field
  - Handle confidence out of range → status_ia: LOW_CONFIDENCE for that field
- [x] **T080** [US2] Implement confidence fallback rules in `GeminiService.determineFallbackStatus()`:
  - If ANY field ≥0.75: status_ia = SUCCESS
  - If ANY field 0.50–0.75: status_ia = LOW_CONFIDENCE
  - If ANY field <0.50: status_ia = FAILURE
  - If timeout/error: status_ia = FAILURE
- [x] **T081** [US2] Create timeout handling in `GeminiService`:
  - Wrap API call in try-catch with timeout exception handler
  - Return response with status_ia: FAILURE, error_message: "Timeout (10s exceeded)"
  - Log timeout events for monitoring
- [x] **T082** [US2] Create `src/main/java/com/ecobook/dto/GeminiResponseDTO.java`:
  - Fields: status_ia (enum), best_prediction (map of field → value), confidence_scores (map of field → 0.0–1.0), upload_id (temporary file reference)
  - Example response:
    ```json
    {
      "status_ia": "LOW_CONFIDENCE",
      "best_prediction": {
        "titulo": "Matemática 5º Ano",
        "disciplina": "MATEMATICA",
        "nivel_ensino": "FUNDAMENTAL",
        "ano": "5",
        "sistema_ensino": "OUTRO",
        "estado_conservacao": "BOM",
        "data_publicacao": 2010
      },
      "confidence_scores": {
        "titulo": 0.89,
        "disciplina": 0.92,
        "nivel_ensino": 0.45,  // below 0.50 threshold
        "ano": 0.78,
        "sistema_ensino": 0.30,
        "estado_conservacao": 0.88,
        "data_publicacao": 0.70
      },
      "upload_id": "temp-uuid-abc123"
    }
    ```
- [x] **T083** [US2] Create `src/main/java/com/ecobook/repository/TemporaryUploadRepository.java` (track temporary file metadata):
  - Entity: id (UUID), file_path, user_id, uploaded_at, expires_at (24 hours)
  - Query: `findExpiredUploads()` for cleanup job
- [x] **T084** [US2] Create Gemini service integration test in `src/test/java/com/ecobook/service/GeminiServiceTest.java`:
  - Mock Gemini API responses (mock multiple confidence levels)
  - Test SUCCESS (≥0.75), LOW_CONFIDENCE (0.50–0.75), FAILURE (<0.50)
  - Test timeout handling (simulate 11-second delay → FAILURE)
  - Test malformed JSON response → FAILURE
  - Test invalid enum → LOW_CONFIDENCE

#### Backend: Image Upload Endpoint

- [x] **T085** [US2] Create `src/main/java/com/ecobook/service/ImageStorageService.java`:
  - Store uploaded image in `/uploads/{user_id}/temp/{uuid}.ext`
  - Validate MIME type (JPEG, PNG only)
  - Validate file size (≤5MB)
  - Validate not corrupt (try to decode image, if fails → reject)
  - Generate UUID for upload_id
  - Return image_path, upload_id, file_size
- [x] **T086** [US2] Implement image validation in `ImageStorageService.validateImage()`:
  - Check MIME type via file header (magic bytes): `FFD8FF` for JPEG, `89504E47` for PNG
  - Reject: `.gif`, `.webp`, `.bmp`, `.txt`, `.zip`, etc.
  - Check file size: if > 5MB, reject with HTTP 413 (Payload Too Large)
  - Decode image bytes to ensure integrity (use BufferedImage for quick validation)
- [x] **T087** [US2] Create directory structure for uploaded files:
  - Ensure `/uploads/{user_id}/temp/` exists (create on first upload)
  - Cleanup `/uploads/{user_id}/temp/` files older than 24 hours (scheduled job in Phase 4)
- [x] **T088** [US2] Create `src/main/java/com/ecobook/controller/MaterialController.java` preview endpoint:
  - **POST /api/v1/materiais/preview**: Multipart file upload + Gemini classification
  - Request: multipart/form-data with key `file` (JPEG/PNG)
  - Response: GeminiResponseDto (status_ia, predictions, confidence_scores, upload_id)
  - Error: 400 (invalid MIME), 413 (file too large), 503 (Gemini timeout/error)
- [x] **T089** [US2] Implement `MaterialController.preview()` handler:
  - Validate image (call ImageStorageService.validateImage())
  - Extract user_id from JWT
  - Check consentimento_ia flag: if false, return FAILURE response immediately (no API call)
  - Save temporary file to `/uploads/{user_id}/temp/{uuid}.ext`
  - Call GeminiService.classifyMaterial()
  - Return GeminiResponseDto with upload_id (temp file reference)
- [x] **T090** [US2] Create preview endpoint test in `src/test/java/com/ecobook/MaterialControllerPreviewTest.java`:
  - POST /materiais/preview with valid JPEG → 200 with classification
  - POST /materiais/preview with oversized file (>5MB) → 413
  - POST /materiais/preview with .txt file → 400 (invalid MIME)
  - POST /materiais/preview with consentimento_ia=false → 200 with FAILURE status
  - POST /materiais/preview without JWT → 401

#### Backend: Material Persistence Endpoint

- [x] **T091** [US2] Create `src/main/java/com/ecobook/controller/MaterialController.java` create endpoint:
  - **POST /api/v1/materiais**: Create material from preview results
  - Request: upload_id, titulo, disciplina, nivel_ensino, ano, sistema_ensino, estado_conservacao, data_publicacao (optional), descricao
  - Response: MaterialDto (id, doador_id, status=DISPONIVEL, image_url, created_at)
  - Error: 400 (invalid upload_id), 403 (profile incomplete), 404 (file not found), 422 (invalid state/enum)
- [x] **T092** [US2] Implement `MaterialService.createMaterial()` method:
  - Validate upload_id exists in TemporaryUploadRepository
  - Validate all enum fields (disciplina, nivel_ensino, sistema_ensino, estado_conservacao)
  - Reject invalid enums: HTTP 400 with field errors
  - Promote temporary file: move `/uploads/{user_id}/temp/{uuid}.ext` → `/uploads/{user_id}/{uuid}.ext`
  - Create Material entity with status=DISPONIVEL
  - Persist to database
  - Delete TemporaryUpload record
  - Return MaterialDto with permanent image_url
- [x] **T093** [US2] Create enum validation in `MaterialService.validateEnums()`:
  - Validate disciplina against enum values
  - Validate nivel_ensino (FUNDAMENTAL, MEDIO, SUPERIOR)
  - Validate ano (1–12 for FUNDAMENTAL/MEDIO, NULL for SUPERIOR)
  - Validate sistema_ensino against enum values
  - Validate estado_conservacao (NOVO, BOM, USADO, DANIFICADO)
  - Throw ValidationException with field-level errors if any invalid
- [x] **T094** [US2] Create material persistence test coverage in `src/test/java/com/ecobook/MaterialControllerCreateTest.java`:
  - POST /materiais with valid upload_id → 201 Created with DISPONIVEL status
  - POST /materiais with invalid disciplina → 400 with field error
  - POST /materiais with non-existent upload_id → 404
  - POST /materiais with profile incomplete → 403
  - Verify temporary file deleted after promotion
- [x] **T095** [US2] Implement rollback logic if persistence fails:
  - If POST /materiais fails after file promotion, do not delete temp file (manual cleanup later)
  - Log error for manual intervention
  - Return HTTP 500 with error message

#### Backend: Cleanup Job

- [x] **T096** [US2] Create `src/main/java/com/ecobook/scheduler/TemporaryUploadCleanupJob.java`:
  - Scheduled task: runs every 6 hours (or configurable interval)
  - Query: find all TemporaryUpload records with expires_at < now
  - For each expired record: delete file from `/uploads/{user_id}/temp/{uuid}`, delete record from database
  - Log cleanup summary: "Cleaned up 5 temporary uploads"
- [x] **T097** [US2] Add `@Scheduled(fixedDelay = 21600000)` annotation to cleanup job (6 hours = 21600000 ms)
- [x] **T098** [US2] Create cleanup test in `src/test/java/com/ecobook/scheduler/TemporaryUploadCleanupJobTest.java`:
  - Insert expired TemporaryUpload records
  - Run cleanup job
  - Verify records deleted, files removed

#### Android: Upload Screen

- [x] **T099** [P] [US2] Create `EcoBookAiAndroid/src/main/java/com/ecobook/material/MaterialUploadScreen.kt` Compose screen:
  - Display image entry CTA that explicitly communicates both options: choose from gallery or capture with camera
  - Show preview of selected image
  - Display size and format information
  - Show "Next" button to proceed to processing screen
  - Handle image selection and validation (show error if >5MB or wrong format)
- [x] **T100** [P] [US2] Implement image picker in `EcoBookAiAndroid/src/main/java/com/ecobook/material/ImagePickerHelper.kt`:
  - Use Android's ACTION_GET_CONTENT (gallery) or ACTION_IMAGE_CAPTURE (camera)
  - Handle permission requests (READ_EXTERNAL_STORAGE, CAMERA)
  - Return selected image URI and metadata (size, MIME type)
- [x] **T101** [P] [US2] Implement image compression in `EcoBookAiAndroid/src/main/java/com/ecobook/material/ImageCompressionHelper.kt`:
  - Compress JPEG/PNG to ≤5MB if necessary
  - Target resolution: 1024x1024px (reduce to fit 5MB limit)
  - Preserve image quality (JPEG quality 85–90%)
- [x] **T102** [P] [US2] Create `EcoBookAiAndroid/src/main/java/com/ecobook/material/ProcessingScreen.kt` Compose screen:
  - Display animated loading spinner
  - Show status message: "Analyzing your material..."
  - Display 10-second timeout warning (if approaching limit)
  - Handle timeout gracefully: show retry button if POST /materiais/preview times out
- [x] **T103** [P] [US2] Implement `EcoBookAiAndroid/src/main/java/com/ecobook/material/MaterialUploadViewModel.kt`:
  - Store selected image URI and metadata
  - `uploadImage()`: POST multipart file to /api/v1/materiais/preview
  - Handle success: Store upload_id + classification results, navigate to ReviewScreen
  - Handle error (timeout, network, 413): Show user-friendly message with retry option
  - Handle 400 (invalid MIME): Show "Invalid file format" message
- [x] **T104** [P] [US2] Create API client for image upload in `EcoBookAiAndroid/src/main/java/com/ecobook/api/MaterialApiService.kt`:
  - `POST /api/v1/materiais/preview` with multipart/form-data
  - Return GeminiResponseDto
- [x] **T105** [P] [US2] Create `EcoBookAiAndroid/src/main/java/com/ecobook/material/ReviewScreen.kt` Compose screen:
  - Display classification results from GeminiResponseDto
  - Show confidence indicators per field:
    - **SUCCESS (≥0.75)**: Green checkmark, field auto-filled, disabled for editing
    - **LOW_CONFIDENCE (0.50–0.75)**: Yellow warning icon, field pre-filled but editable
    - **FAILURE (<0.50)**: Gray question mark, field empty, manual entry required
  - Display confidence scores as percentages (e.g., "92% confident")
  - Allow user to edit all fields regardless of confidence
  - Show title input field (required, ≤255 chars; AI-assisted, always editable)
  - Show description textarea (optional, ≤2000 chars; **manual-only, never auto-filled** to prevent hallucinations)
  - Show estado_conservacao dropdown (NOVO, BOM, USADO, DANIFICADO)
  - "Confirm" button → POST /materiais with edited values
  - "Cancel" button → navigate back (delete temp file via backend cleanup)
- [x] **T106** [P] [US2] Implement review state and confirmation flow inside `EcoBookAiAndroid/src/main/java/com/ecobook/material/MaterialUploadViewModel.kt`:
  - Store classification results (upload_id, predictions, confidence_scores)
  - Store edited values (titulo, disciplina, nivel_ensino, etc.)
  - Validation: require titulo, validate enums on confirm
  - `confirmMaterial()`: POST /api/v1/materiais with edited values + upload_id
  - Handle success: navigate to HomeScreen, show toast "Material created"
  - Handle error: show field-specific errors (400/422) or generic error (500)
- [x] **T107** [P] [US2] Create confirmation dialog in ReviewScreen:
  - "Confirm Material?" with summary of fields
  - "Edit" button → stay on ReviewScreen
  - "Confirm" button → POST /materiais
- [x] **T108** [P] [US2] Create integration test coverage for upload flow in `src/test/java/com/ecobook/MaterialControllerPreviewTest.java` and `src/test/java/com/ecobook/MaterialControllerCreateTest.java`:
  - Mock Gemini API response (HIGH confidence)
  - POST /materiais/preview → GeminiResponseDto
  - POST /materiais with upload_id → 201 Created
  - Verify Material persisted with DISPONIVEL status
  - Verify file promoted to permanent location
  - Verify TemporaryUpload record deleted
- [x] **T109** [P] [US2] Create error scenario tests:
  - POST /materiais/preview with consentimento_ia=false → FAILURE status
  - POST /materiais/preview with timeout → 503 error
  - POST /materiais with invalid enum → 400 with field error
  - POST /materiais with profile incomplete → 403

#### Android: Composition & Styling

- [x] **T110** [P] [US2] Create reusable composables in `EcoBookAiAndroid/src/main/java/com/ecobook/ui/`:
  - `ConfidenceIndicator.kt` (checkmark for SUCCESS, warning for LOW, question for FAILURE)
  - `EditableField.kt` (text input with optional error message)
  - `EnumDropdown.kt` (dropdown for enum selection)
  - `LoadingSpinner.kt` (animated spinner with message)
- [x] **T111** [P] [US2] Create Material Design 3 theme in `EcoBookAiAndroid/src/main/java/com/ecobook/ui/theme/Theme.kt`:
  - Colors: Primary (green for donations), Secondary (blue for requests), Tertiary (orange for alerts)
  - Typography: Roboto for headings, Roboto for body text
  - Shapes: Rounded corners 8dp for cards, 4dp for buttons

---

## PHASE 4: USER STORY 3 — Material Discovery (Weeks 13–14)

**Story Goal**: Student can specify academic needs and discover matching donated materials ranked by relevance and proximity.

**Independent Test Criteria**:
- ✅ 7-step matching algorithm correctly filters materials (5 core + 1 optional publication range)
- ✅ Ranking order enforced (neighborhood > city > date > id)
- ✅ Special rules for SUPERIOR and OUTRO sistema_ensino
- ✅ Geographic normalization consistent with database
- ✅ Pagination working (page, size parameters)

### Module 4: Matching Algorithm (RF-008)

#### Backend: Matching Algorithm

- [x] **T112** [US3] Create `src/main/java/com/ecobook/service/MatchingService.java`:
  - Implement 7-step filtering pipeline (5 core + 1 optional publication range; deterministic, not ML)
  - Step 1: Filter status=DISPONIVEL only
  - Step 2: Filter disciplina matches exactly
  - Step 3: Filter nivel_ensino matches exactly
  - Step 4: Filter ano range (±1 year for FUNDAMENTAL/MEDIO, NULL for SUPERIOR)
  - Step 5: Filter sistema_ensino matches exactly (special rule: OUTRO only matches OUTRO)
  - Step 6: Sort results by ranking order (neighborhood > city > data_publicacao DESC > id)
- [x] **T113** [US3] Implement geographic ranking anchors in `MatchingService.normalize()` + comparator:
  - Normalize student's city + neighborhood via GeoNormalizationService
  - Compare normalized values against Material records for same-neighborhood and same-city ranking
  - Keep matching accent-insensitive and deterministic (no fuzzy matching)
- [x] **T114** [US3] Implement ano range logic in `MatchingService.matchesYear()`:
  - If nivel_ensino = SUPERIOR: ignore ano constraint (include all SUPERIOR materials)
  - If nivel_ensino = FUNDAMENTAL/MEDIO: filter ano to [student_ano-1, student_ano+1]
  - Example: student year 5 → accept materials for years 4, 5, 6
- [x] **T115** [US3] Implement sistema_ensino special rule in `MatchingService.matchesSystem()`:
  - If student sistema_ensino = OUTRO: only return OUTRO materials
  - If student sistema_ensino = ANGLO/OBJETIVO/COC/POSITIVO: return exact system matches + OUTRO materials
  - Rationale: OUTRO system not affiliated with specific curriculum
- [x] **T116** [US3] Create ranking comparator in `MatchingService.createComparator()`:
  - Comparator: (mat1, mat2) → int
  - Primary: Same neighborhood as student? (true > false)
  - Secondary: Same city as student? (true > false)
  - Tertiary: data_publicacao DESC (most recent publication first)
  - Quaternary: id ASC (UUID tiebreaker for stability)
- [x] **T117** [US3] Create `src/main/java/com/ecobook/dto/SearchCriteriaDto.java`:
  - Fields: query, disciplina, nivel_ensino, ano, sistema_ensino, cidade, bairro, min_ano_publicacao, max_ano_publicacao
  - All fields optional (nullable)
  - Validation: enums validated if provided
- [x] **T118** [US3] Create MatchingService integration test in `src/test/java/com/ecobook/service/MatchingServiceTest.java`:
  - Create 10+ materials with varying disciplinas, levels, years, systems, locations
  - Query for MATEMATICA + FUNDAMENTAL + year 5 + OUTRO system + city "Sao Paulo" + neighborhood "Centro"
  - Verify results: only matching materials returned, correct ordering (neighborhood first, then city, then newest)
  - Test SUPERIOR year constraint ignored
  - Test OUTRO system rule (OUTRO matches only OUTRO, ANGLO matches ANGLO + OUTRO)

#### Backend: Search Endpoint

- [x] **T119** [US3] Create `src/main/java/com/ecobook/controller/MaterialController.java` endpoint:
  - **GET /api/v1/materiais**: Query matching materials
  - Query parameters: disciplina, nivel_ensino, ano, sistema_ensino, cidade, bairro, min_ano_publicacao (optional), max_ano_publicacao (optional), page (default 0), size (default 20)
  - Response: `PagedResponseDTO<MaterialDTO>` wrapped by the standard API envelope
  - Error: 400 (invalid enum value, invalid publication range), 403 (profile incomplete)
- [x] **T120** [US3] Implement `MaterialController.search()` handler:
  - Extract query parameters
  - Validate enums (if provided)
  - Validate publication date range: if both min and max provided, check min <= max; if either provided, check bounds in [1900, 2100]
  - Return HTTP 400 if validation fails
  - Build SearchCriteriaDto (including min_ano_publicacao, max_ano_publicacao)
  - Call MatchingService.findMatching(criteria, pageable)
  - Return `PagedResponseDTO<MaterialDTO>` with paginated results
- [x] **T121** [US3] Implement pagination in `MatchingService.findMatching()`:
  - Support Spring's Pageable (page, size, sort)
  - Fixed sort order: AVAILABLE only, then ranking comparator
  - Return `PagedResponseDTO<MaterialDTO>` with total count, page number, total_pages and has_next
- [x] **T122** [US3] Create database indexes for fast queries in `src/main/resources/db/migration/V8__add_phase4_search_indexes.sql`:
  - `CREATE INDEX idx_material_status_disciplina_nivel ON material(status, disciplina, nivel_ensino)` (filter + composite)
  - `CREATE INDEX idx_material_status_cidade_bairro ON material(status, cidade, bairro)` (geo ranking support)
  - `CREATE INDEX idx_material_status_data_publicacao ON material(status, data_publicacao DESC)` (sorting)
- [x] **T123** [US3] Create search endpoint test in `src/test/java/com/ecobook/controller/MaterialControllerSearchTest.java`:
  - GET /materiais?disciplina=MATEMATICA&nivel_ensino=FUNDAMENTAL → 200 with paginated results
  - GET /materiais?disciplina=INVALID → 400 (invalid enum)
  - GET /materiais?page=0&size=10 → 200 with 10 results max
  - GET /materiais?page=1&size=10 → 200 with a non-first page slice (zero-based pagination)
  - Verify ranking order (neighborhood first, then city, then newest)
  - GET /materiais?min_ano_publicacao=2000&max_ano_publicacao=2015 → 200, returns only materials with data_publicacao in [2000, 2015]
  - GET /materiais?min_ano_publicacao=2015 → 200, returns only materials with data_publicacao >= 2015
  - GET /materiais?max_ano_publicacao=2000 → 200, returns only materials with data_publicacao <= 2000
  - GET /materiais?min_ano_publicacao=2015&max_ano_publicacao=2000 → 400 (invalid range)
  - GET /materiais?min_ano_publicacao=1800 → 400 (out of bounds)
  - GET /materiais?max_ano_publicacao=2200 → 400 (out of bounds)

#### Android: Discovery Screen

- [x] **T124** [P] [US3] Create `EcoBookAiAndroid/src/main/java/com/ecobook/discovery/DiscoveryScreen.kt` Compose screen:
  - Display search filters (dropdown menus for disciplina, nivel_ensino, ano, sistema_ensino)
  - Display city and neighborhood (pre-populated from user profile or editable)
  - "Search" button → GET /api/v1/materiais with criteria
  - "Reset" button → clear filters, show all DISPONIVEL materials
- [x] **T125** [P] [US3] Implement filter state in `EcoBookAiAndroid/src/main/java/com/ecobook/discovery/DiscoveryViewModel.kt`:
  - Store current filters (disciplina, nivel_ensino, ano, etc.)
  - Track search results (list of MaterialDto)
  - Track pagination (page, size, hasNext)
  - `search()`: call GET /api/v1/materiais with current filters
  - `loadNextPage()`: increment page, append results
- [x] **T126** [P] [US3] Create material list UI in `EcoBookAiAndroid/src/main/java/com/ecobook/discovery/MaterialListItem.kt`:
  - Render each material as a tappable card
  - Display material thumbnail image or a neutral gray placeholder when the image is missing/unavailable
  - Show disciplina, nivel_ensino, ano, estado_conservacao badges
  - Show city and neighborhood (proximity indicator)
  - Show upload date (relative: "2 days ago")
  - Show donor name (or "Anonymous" if privacy desired)
  - Show publication year (data_publicacao) if available
  - Tap to open a detail dialog before request material
- [x] **T127** [P] [US3] Create LazyColumn with pagination in DiscoveryScreen:
  - Load initial 20 materials on screen open
  - Detect "load more" (user scrolled to end)
  - Load next page automatically
  - Show loading spinner at bottom while fetching
- [x] **T128** [P] [US3] Implement search filters UI:
  - Disciplina dropdown (MATEMATICA, PORTUGUES, ...) with "All" option
  - Nivel ensino dropdown (FUNDAMENTAL, MEDIO, SUPERIOR) with "All" option
  - Ano text input (optional, validated as 1–12)
  - Sistema ensino dropdown (ANGLO, OBJETIVO, COC, POSITIVO, OUTRO, All)
  - Min publication year input (optional, 1900-2100, labeled "Publication year from:")
  - Max publication year input (optional, 1900-2100, labeled "Publication year to:")
  - Validation: if both min and max provided, min must be <= max; show error toast if violated
- [x] **T129** [P] [US3] Create empty state UI when no results found:
  - Show "No materials found" message
  - Suggest adjusting filters
  - Show "Browse all materials" button to reset filters
- [x] **T130** [P] [US3] Create material detail dialog in `EcoBookAiAndroid/src/main/java/com/ecobook/discovery/MaterialDetailDialog.kt`:
  - Open from material card tap as a dismissible modal/dialog
  - Display full image or fallback placeholder
  - Show all metadata (disciplina, nivel_ensino, ano, sistema_ensino, estado_conservacao, data_publicacao)
  - Show description (if provided)
  - Show donor city and neighborhood (proximity indicator)
  - Show donor name + contact handoff button clarifying that WhatsApp contact becomes actionable after approval
  - Provide explicit close action and support dismiss by back/tap outside
  - Show "Request Material" button with a clear UI handoff to the Phase 5 solicitation flow

Phase 4 closeout note (2026-05-12):
- Backend discovery now serves live matching through `GET /api/v1/materiais` with pagination, ranking and publication-range filtering.
- Android discovery now uses the real backend endpoint with filter state, empty state, pagination and material detail dialog.
- The request CTA was the Phase 4 handoff point. Runtime update (2026-05-14): the repository already includes the delivered Phase 5 workflow for request creation, inboxes and approval/completion actions.

---

## PHASE 5: USER STORY 4 & 5 — Request Workflow (Weeks 15–16)

**Story Goal**: Student requests a material, donor approves/declines, material transitions through RESERVADO → DOADO lifecycle.

**Independent Test Criteria**:
- ✅ Request creation with state validation
- ✅ Atomic approval with locking (no race conditions)
- ✅ 14-day expiry auto-revert working
- ✅ All state transitions enforced (422 on invalid)
- ✅ Notifications triggered on each event

### Module 5: Request Workflow (RF-005, RF-007)

Runtime note on 2026-05-14:
- The Phase 5 request workflow is implemented in runtime across backend and Android.
- Some checklist items below still preserve historical planning names such as `RequestViewModel`, `donor/` package paths or dedicated lock-test filenames, but the delivered code was consolidated mainly under `request/`, `DiscoveryViewModel`, `SolicitacaoWorkflowTest` and `ReservationExpiryJobTest`.
- Use the current runtime files as source of truth when the checklist wording differs from the final implementation layout.

#### Backend: Request Creation

- [x] **T131** [US4] Create `src/main/java/com/ecobook/controller/SolicitacaoController.java`:
  - **POST /api/v1/materiais/{id}/solicitacoes**: Student requests material
  - Path param: material_id (UUID)
  - Response: SolicitacaoDto (id, status=PENDENTE, created_at)
  - Error: 400 (solicitante = doador), 403 (profile incomplete), 404 (material not found), 409 (already requested), 422 (invalid state)
- [x] **T132** [US4] Implement `SolicitacaoService.createRequest()` method:
  - Validate material exists and status = DISPONIVEL
  - Validate requestor ≠ donor (HTTP 400)
  - Check if student already has PENDENTE/APROVADA request for this material (HTTP 409 Conflict)
  - Create Solicitacao with status=PENDENTE, created_at=now
  - Persist to database
  - Emit SolicitacaoCreatedEvent (for FCM notification trigger)
  - Return SolicitacaoDto
- [x] **T133** [US4] Create request creation test in `src/test/java/com/ecobook/service/SolicitacaoServiceTest.java`:
  - Student requests available material → 201 PENDENTE
  - Student requests own material → 400
  - Donor requests own material → 400
  - Material already requested by student → 409
  - Profile incomplete → 403

#### Backend: Atomic Approval with Locking

- [x] **T134** [US4] Create `src/main/java/com/ecobook/controller/SolicitacaoController.java` endpoint:
  - **PATCH /api/v1/solicitacoes/{id}/aprovar**: Donor approves request
  - Path param: request_id (UUID)
  - Response: SolicitacaoDto (updated with status=APROVADA, contato_doador, approved_at, expires_at)
  - Error: 401 (not donor), 403 (profile incomplete), 404 (not found), 422 (invalid state)
- [x] **T135** [US4] Implement atomic approval transaction in `SolicitacaoService.approveRequest()`:
  - Use `@Transactional(isolation = Isolation.SERIALIZABLE)` annotation
  - Lock Material record: `SELECT ... FOR UPDATE` (PostgreSQL)
  - Validate Solicitacao status = PENDENTE
  - Validate Material status = DISPONIVEL
  - Validate no other APROVADA requests for this material (atomic check)
  - Update Solicitacao: status=APROVADA, approved_at=now, expires_at=now+14days, contato_doador={nome, whatsapp}
  - Update Material: status=RESERVADO
  - Reject all other PENDENTE requests for this material: status=RECUSADA
  - Persist atomically (all or nothing)
  - Emit SolicitacaoApprovedEvent (for FCM notification)
  - Return updated SolicitacaoDto
- [x] **T136** [US4] Create rejection logic in approval transaction:
  - Query all other PENDENTE Solicitacoes for same Material
  - Update all to status=RECUSADA, declined_reason="Material already reserved"
  - Emit SolicitacaoRejectedEvent for each (FCM notification to affected students)
- [x] **T137** [US4] Create locking test in `src/test/java/com/ecobook/service/SolicitacaoServiceLockingTest.java`:
  - Two concurrent requests for same material
  - First approval succeeds: Material → RESERVADO, Solicitacao → APROVADA
  - Second approval fails: HTTP 422 (invalid state), Solicitacao → RECUSADA
  - Verify no race condition (no orphaned APROVADA requests)

#### Backend: Approval Endpoints & Decline

- [x] **T138** [US4] Create `src/main/java/com/ecobook/controller/SolicitacaoController.java` endpoint:
  - **PATCH /api/v1/solicitacoes/{id}/recusar**: Donor declines request
  - Response: SolicitacaoDto (updated with status=RECUSADA, declined_reason)
  - Error: 401 (not donor), 404 (not found), 422 (invalid state)
- [x] **T139** [US4] Implement `SolicitacaoService.declineRequest()`:
  - Validate requestor is donor
  - Validate Solicitacao status = PENDENTE
  - Update status=RECUSADA, declined_reason (optional)
  - Material status remains DISPONIVEL
  - Emit SolicitacaoRejectedEvent (FCM notification)
- [x] **T140** [US4] Create `src/main/java/com/ecobook/controller/SolicitacaoController.java` endpoint:
  - **PATCH /api/v1/solicitacoes/{id}/cancelar**: Student/Donor cancels request
  - Response: SolicitacaoDto (updated with status=CANCELADA)
  - Error: 401 (not requestor/donor), 404 (not found), 422 (invalid state)
- [x] **T141** [US4] Implement `SolicitacaoService.cancelRequest()`:
  - Validate requestor is student or donor
  - If status=APROVADA: revert Material status=DISPONIVEL (atomic transaction)
  - Update Solicitacao status=CANCELADA
  - Emit SolicitacaoCanceledEvent (FCM notification)
- [x] **T142** [US4] Create approval workflow integration test in `src/test/java/com/ecobook/SolicitacaoWorkflowTest.java`:
  - Create material, student requests → PENDENTE
  - Donor approves → Material RESERVADO, Solicitacao APROVADA
  - Another student requests same material → 409 Conflict
  - Student cancels → Material DISPONIVEL, Solicitacao CANCELADA

#### Backend: Donation Completion & Material State

- [x] **T143** [US5] Create `src/main/java/com/ecobook/controller/SolicitacaoController.java` endpoint:
  - **PATCH /api/v1/solicitacoes/{id}/concluir**: Mark donation as completed
  - Response: SolicitacaoDto (updated with status=CONCLUIDA)
  - Error: 401 (not donor), 404 (not found), 422 (invalid state)
- [x] **T144** [US5] Implement `SolicitacaoService.completeDonation()`:
  - Validate requestor is donor
  - Validate Solicitacao status = APROVADA
  - Validate Material status = RESERVADO
  - Update Solicitacao: status=CONCLUIDA, completed_at=now
  - Update Material: status=DOADO, donated_at=now
  - Emit SolicitacaoCompletedEvent (FCM notification)
  - Material becomes final state (no further transitions)
- [x] **T145** [US5] Create `src/main/java/com/ecobook/controller/MaterialController.java` endpoint:
  - **PUT /api/v1/materiais/{id}**: Edit material (only DISPONIVEL state)
  - Request: titulo, disciplina, nivel_ensino, ano, sistema_ensino, estado_conservacao, data_publicacao (optional), descricao
  - Response: MaterialDto (updated)
  - Error: 400 (invalid enum), 403 (profile incomplete), 404 (not found), 422 (invalid state)
- [x] **T146** [US5] Implement material editing in `MaterialService.updateMaterial()`:
  - Validate status = DISPONIVEL (reject if RESERVADO/DOADO/CANCELADO)
  - Validate requestor is donor
  - Validate enums
  - Update all mutable fields
  - Persist
- [x] **T147** [US5] Create `src/main/java/com/ecobook/controller/MaterialController.java` endpoint:
  - **DELETE /api/v1/materiais/{id}**: Cancel material (donor only)
  - Response: HTTP 204 No Content
  - Error: 401 (not donor), 404 (not found), 422 (invalid state)
- [x] **T148** [US5] Implement material cancellation in `MaterialService.cancelMaterial()`:
  - Update Material status=CANCELADO
  - Cancel all related PENDENTE/APROVADA Solicitacoes: status=CANCELADA
  - Emit MaterialCanceledEvent (FCM notification to affected students)

#### Backend: Expiry Job

- [x] **T149** [US5] Create `src/main/java/com/ecobook/scheduler/ReservationExpiryJob.java`:
  - Scheduled task: runs daily at 2 AM UTC
  - Query: all Solicitacoes with status=APROVADA AND expires_at < now
  - For each expired request:
    - Update Solicitacao: status=CANCELADA, expired_reason="14-day reservation expired"
    - Update Material: status=DISPONIVEL (revert reservation)
    - Emit SolicitacaoExpiredEvent (FCM notification to student)
  - Log summary: "Expired 3 reservations"
- [x] **T150** [US5] Add `@Scheduled(cron = "0 0 2 * * ?")` annotation (daily 2 AM UTC)
- [x] **T151** [US5] Create expiry job test in `src/test/java/com/ecobook/scheduler/ReservationExpiryJobTest.java`:
  - Insert APROVADA Solicitacao with expires_at = yesterday
  - Run expiry job
  - Verify Solicitacao → CANCELADA, Material → DISPONIVEL

#### Backend: State Validation

- [x] **T152** [US4] Create `src/main/java/com/ecobook/service/MaterialStateValidator.java`:
  - Method: `validateTransition(currentStatus, newStatus)` → throws if invalid
  - Valid transitions:
    - DISPONIVEL → RESERVADO (approval)
    - RESERVADO → DOADO (completion)
    - RESERVADO → DISPONIVEL (expiry auto-revert)
    - RESERVADO → DISPONIVEL (manual cancel of approved request)
    - Any other → throw InvalidStateTransitionException (HTTP 422)
- [x] **T153** [US4] Create state validation test in `src/test/java/com/ecobook/service/MaterialStateValidatorTest.java`:
  - Test all valid transitions
  - Test all invalid transitions (should throw)

#### Android: Request Creation UI

- [x] **T154** [P] [US4] Create "Request Material" action in MaterialDetailScreen:
  - Button: "Request Material"
  - On click: Call `POST /materiais/{material_id}/solicitacoes`
  - Success: Show confirmation dialog "Material requested! Waiting for donor approval."
  - Navigate to "My Requests" screen
- [x] **T155** [P] [US4] Implement `EcoBookAiAndroid/src/main/java/com/ecobook/request/RequestViewModel.kt`:
  - `createRequest(materialId)`: POST /api/v1/materiais/{materialId}/solicitacoes
  - Handle 409 Conflict: Show "You already have a pending request for this material"
  - Handle 422 Invalid State: Show "Material no longer available"
  - Handle success: Navigate to MyRequestsScreen

#### Android: My Requests Screen

- [x] **T156** [P] [US4] Create `EcoBookAiAndroid/src/main/java/com/ecobook/request/MyRequestsScreen.kt` Compose screen:
  - Display list of student's requests (all statuses: PENDENTE, APROVADA, RECUSADA, CANCELADA, CONCLUIDA)
  - For each request, show:
    - Material title + image
    - Current status (PENDENTE, APROVADA, RECUSADA, CANCELADA, CONCLUIDA)
    - Status indicator (hourglass for PENDENTE, checkmark for APROVADA, X for RECUSADA, done for CONCLUIDA)
    - Days remaining (if APROVADA): "3 days left to complete"
    - Action button: "Contact Donor" (if APROVADA), "Cancel" (if PENDENTE/APROVADA)
- [x] **T157** [P] [US4] Implement `EcoBookAiAndroid/src/main/java/com/ecobook/request/MyRequestsViewModel.kt`:
  - Load student's requests: GET /api/v1/solicitacoes/minhas
  - Filter by status (optional)
  - `cancelRequest(requestId)`: PATCH /solicitacoes/{id}/cancelar
  - `contactDonor(solicitacaoId)`: Open WhatsApp contact from contato_doador JSONB
- [x] **T158** [P] [US4] Create contact donor action:
  - Only visible when status=APROVADA (contato_doador populated)
  - On click: Open WhatsApp intent with donor's WhatsApp number + pre-filled message: "Hi! I'm requesting your [material title]. Can we arrange pickup?"
- [x] **T159** [P] [US4] Create request list UI in `EcoBookAiAndroid/src/main/java/com/ecobook/ui/RequestListItem.kt`:
  - Show request status with color code (orange=PENDENTE, green=APROVADA, red=RECUSADA, gray=CANCELADA/CONCLUIDA)
  - Show material thumbnail + title
  - Show donor name + location
  - Show expiry date (if APROVADA)

#### Android: Donor Approval UI

- [x] **T160** [P] [US4] Create `EcoBookAiAndroid/src/main/java/com/ecobook/donor/DonorRequestsScreen.kt` Compose screen:
  - Display list of incoming requests for donor's materials (status=PENDENTE only)
  - For each request, show:
    - Student name + location
    - Requested material title
    - "Accept" and "Decline" buttons
  - Tapping "Accept": PATCH /solicitacoes/{id}/aprovar
  - Tapping "Decline": PATCH /solicitacoes/{id}/recusar
  - On accept: Show confirmation, navigate back (request status → APROVADA)
- [x] **T161** [P] [US4] Implement `EcoBookAiAndroid/src/main/java/com/ecobook/donor/DonorRequestsViewModel.kt`:
  - Load incoming requests: GET /api/v1/solicitacoes/pendentes
  - `approveRequest(solicitacaoId)`: PATCH /solicitacoes/{id}/aprovar
  - `declineRequest(solicitacaoId)`: PATCH /solicitacoes/{id}/recusar
  - Handle success: Refresh list, show toast "Request approved"
- [x] **T162** [P] [US4] Create donor incoming requests endpoint:
  - **GET /api/v1/solicitacoes/pendentes**: List pending requests for donor's materials
  - Filters by authenticated donor's materials with PENDENTE requests

#### Android: Completion UI

- [x] **T163** [P] [US5] Create "Mark as Donated" action in DonorRequestsScreen:
  - Only show when status=APROVADA (after student confirms receipt)
  - Button: "Mark as Donated"
  - On click: PATCH /solicitacoes/{id}/concluir
  - Success: Show "Material marked as donated", navigate back
- [x] **T164** [P] [US5] Implement in `DonorRequestsViewModel`:
  - `completeDonation(solicitacaoId)`: PATCH /solicitacoes/{id}/concluir
- [x] **T165** [P] [US5] Create endpoint for listing donor's approved requests:
  - **GET /api/v1/solicitacoes/aprovadas**: List donor's approved requests (awaiting completion)

---

## PHASE 6: FCM Notifications (RF-006 — Cross-Cutting)

**Story Goal**: Send push notifications for 6 key events (request received, approved, declined, cancelled, material donated, material cancelled).

### Module 6: Firebase Cloud Messaging

#### Backend: FCM Configuration

- [x] **T166** [P] Runtime note: Firebase Admin SDK initialization now lives lazily inside `src/main/java/com/ecobook/service/FcmService.java` instead of a separate `FirebaseConfig.java`
  - Service account path comes from `firebase.service-account-path`
  - Firebase stays dormant when local credentials are intentionally absent
- [x] **T167** [P] Create `src/main/java/com/ecobook/service/FcmService.java`:
  - Method: `sendNotification(userId, title, body, data)` → sends to user's device
  - Retrieve FCM token from user record
  - Build notification message (title, body, data payload)
  - Send via FirebaseMessaging.send()
  - Log result (success, failure, not registered)
- [x] **T168** [P] Runtime note: token registration is implemented through `FcmController` + `UsuarioService.updateFcmToken()` instead of a dedicated `FcmTokenService.java`
  - Endpoint: **POST /api/v1/fcm/tokens**
  - Store `fcm_token` in `Usuario`
  - Support token rotation (overwrite existing token per user/device session)
- [x] **T169** [P] Create event listeners for notifications:
  - Runtime implementation uses a generic `NotificationRequestedEvent` + `NotificationRequestedEventListener`
  - Request workflow now publishes post-commit notification intents for donor/student events
  - Material deletion also emits notification events to affected students

#### Backend: 6 Notification Payloads

- [x] **T170** [P] Create notification payloads in `src/main/java/com/ecobook/dto/notification/`:
  - Runtime implementation consolidates the six payload types into `NotificationType`, `NotificationPayloadDTO` and `NotificationPayloadFactory`
  - Payload data now includes `notification_id`, `type`, `title`, `body`, `route`, `solicitacao_id` and `material_id` where applicable
- [x] **T171** [P] Implement notification sending in event listeners:
  - Each listener catches domain event (e.g., SolicitacaoApprovedEvent)
  - Builds appropriate notification payload
  - Calls FcmService.sendNotification(recipient_user_id, payload)
  - Handles failures gracefully (log error, don't crash)

#### Backend: FCM Error Handling

- [x] **T172** [P] Create retry logic for failed notifications:
  - If send fails: Store in FailedNotificationQueue (for manual retry)
  - Log: user_id, notification_type, error_reason, timestamp
  - Cron job: retry failed notifications every hour (max 3 retries)
- [x] **T173** [P] Create `src/main/java/com/ecobook/scheduler/NotificationRetryJob.java`:
  - Query FailedNotificationQueue records with retry_count < 3
  - Retry each notification
  - Delete on success, increment retry_count on failure
  - Disable notification after 3 retries (log as permanently failed)

#### Android: FCM Setup

- [x] **T174** [P] [US4] Create Firebase Cloud Messaging setup in `EcoBookAiAndroid/src/main/AndroidManifest.xml`:
  - Add FCM service declarations
  - Add required permissions (POST_NOTIFICATIONS for Android 13+)
- [x] **T175** [P] [US4] Implement `EcoBookAiAndroid/src/main/java/com/ecobook/fcm/EcoBookMessagingService.kt`:
  - Extend FirebaseMessagingService
  - Override `onMessageReceived(remoteMessage)`
  - Handle notifications in foreground (show local notification via NotificationManager)
  - Handle notifications in background (FCM shows automatically)
  - Extract custom data (action, ids) from payload
  - Runtime note: notification taps currently route to `MyRequests`, `DonorRequests`, `Discovery` or `Notifications` instead of a dedicated `RequestDetailScreen`
- [x] **T176** [P] [US4] Create notification display in `EcoBookMessagingService`:
  - Build NotificationCompat.Builder with title, body, icon, color
  - Use PendingIntent to navigate to appropriate screen
  - Post to NotificationManager
  - Set notification ID per notification type (avoid duplicates)
- [x] **T177** [P] [US4] Implement token registration in the Android runtime:
  - Runtime implementation lives in `AuthRepository`, `EcoBookApp` and `FcmTokenSyncManager` instead of `AuthViewModel.kt`
  - After successful login/app startup: get current FCM token, POST `/api/v1/fcm/tokens`, cache locally and avoid redundant sync
- [x] **T178** [P] [US4] Create notification permission request for Android 13+:
  - Request `POST_NOTIFICATIONS` only after the session reaches the main area
  - Avoid eager prompt in `onCreate` before the user sees app context
- [x] **T179** [P] [US4] Create deep linking for notification actions:
  - Configure AndroidManifest.xml to handle deep links
  - Runtime routes use `ecobook://app/my-requests`, `ecobook://app/donor-requests`, `ecobook://app/discovery` and `ecobook://app/notifications`
  - MainActivity should parse intent and navigate via navigation graph
- [x] **T180** [P] [US4] Create notification unread indicator in the Android main navigation surfaces:
  - Track unread notifications locally
  - Show visual state on the bell entry point rendered in the main screen headers
  - Runtime closeout now keeps notifications unread until the user explicitly chooses `Marcar como lida` or `Marcar todas como lidas` inside `NotificationsScreen`

---

## PHASE 7: Admin & Moderation (Modules 7–8)

### Module 7: Non-Received Material Reporting

- [x] **T181** [US5] Create `src/main/java/com/ecobook/controller/ReportController.java` endpoint:
  - **POST /api/v1/materiais/{id}/nao-recebido**: Report non-receipt
  - Request: reason (optional text, max 500 chars)
  - Response: ReportDto (id, created_at, status=OPEN)
  - Error: 403 (not student), 404 (not found), 422 (material not DOADO)
- [x] **T182** [US5] Implement `ReportService.reportNonReceipt()`:
  - Validate material status = DOADO
  - Validate requestor is student from CONCLUIDA Solicitacao
  - Create MaterialNonReceiptReport record
  - Emit ReportCreatedEvent (notify admin via backend alert, email, or dashboard)
- [x] **T183** [US5] Create Android UI for non-receipt reporting:
  - Runtime implementation surfaces the action on completed request cards inside `MyRequestsScreen`, where the student already has the concluded donation context
  - Show dialog: "Report Non-Receipt" with optional reason text
  - Send reason to backend
  - Show confirmation: "Report submitted. Admin will review."

### Module 8: Admin Dashboard (Optional for MVP)

- [x] **T184** Create `src/main/java/com/ecobook/controller/AdminController.java` (admin-only endpoints):
  - **GET /api/v1/admin/reports**: List all non-receipt reports (pagination, filtering by status)
  - **PATCH /api/v1/admin/reports/{id}/resolve**: Mark report as resolved
  - **GET /api/v1/admin/materials**: List all materials (including CANCELADO)
  - **DELETE /api/v1/admin/materials/{id}**: Remove material from platform (hard delete)
  - **GET /api/v1/admin/users**: List all users with activity metrics
- [x] **T185** Create admin authorization:
  - Add `role` field to Usuario (ADMIN, USER)
  - Add `@PreAuthorize("hasRole('ADMIN')")` to admin endpoints
  - Support admin role setting via database seeding or special endpoint
  - Runtime note: the repository now supports startup bootstrap/promotion through `ADMIN_BOOTSTRAP_*`

---

## PHASE 8: Security & LGPD (Module 9 — Cross-Cutting)

### Module 9: Data Privacy & Security

#### Backend: Soft Delete & Anonymization

- [x] **T186** [P] Create `src/main/java/com/ecobook/entity/AuditedEntity.java` base class:
  - Fields: deleted_at (nullable), deleted_by (nullable), anonymized (boolean)
  - All entities extend AuditedEntity
- [x] **T187** [P] Implement soft delete in `src/main/java/com/ecobook/service/UserDeletionService.java`:
  - `deleteUser(userId)` method:
    1. Set deleted_at = now, anonymized = true
    2. Update nome = "Usuário Removido"
    3. Update email = SHA256(email) (irreversible hash)
    4. Update whatsapp = null, deletar
    5. Set consentimento_ia = false
    6. Delete all images from `/uploads/{user_id}/`
    7. Cancel all DISPONIVEL/RESERVADO materials (status → CANCELADO)
    8. Cancel all open requests (PENDENTE/APROVADA → CANCELADA)
    9. Anonymize all CONCLUIDA requests (remove donor/student identifiable info, keep transaction history)
    10. Log deletion event for audit trail
- [x] **T188** [P] Implement query filtering to exclude deleted records:
  - Runtime note: the live code filters core runtime entities (`usuario`, `material`, `solicitacao`) and keeps moderation/report rows visible for admin review even when related actors are soft-deleted
- [x] **T189** [P] Create account deletion endpoint:
  - **POST /api/v1/usuarios/delete**: Delete current user account
  - Request: password confirmation
  - Response: envelope with deletion timestamp
  - Error: 401 (invalid password), 404 (user not found)
- [x] **T190** [P] Implement `UserController.deleteAccount()`:
  - Call UserDeletionService.deleteUser()
  - Revoke JWT token (add to blacklist)
  - Clear local Android session
- [x] **T191** [P] Create deletion audit log:
  - Store all deletions in AuditLog table (for compliance)
  - Fields: deleted_user_id, deleted_at, deleted_by (admin or user self), reason

#### Backend: Consent Management

- [x] **T192** [P] Create `src/main/java/com/ecobook/entity/ConsentRecord.java` entity:
  - Fields: id (UUID), user_id, consent_type (PLATFORM, AI_CLASSIFICATION), status (GIVEN, REVOKED), created_at, revoked_at
  - Two-stage consent: PLATFORM (sign-up) + AI_CLASSIFICATION (before first material upload)
- [x] **T193** [P] Create consent tracking in registration:
  - First consent: PLATFORM granted automatically on signup
  - Second consent: AI_CLASSIFICATION may be requested during onboarding or later from profile/settings before POST /materiais/preview
  - Store ConsentRecord for each grant/revocation
- [x] **T194** [P] Create `src/main/java/com/ecobook/controller/UsuarioController.java` endpoint:
  - **PATCH /api/v1/usuarios/me/consent**: Update consent for AI classification
  - Request: consentimento_ia (boolean)
  - Response: Updated UsuarioDto
  - Must support enabling or disabling consent after profile creation at any time
- [x] **T195** [P] Implement consent enforcement:
  - If consentimento_ia = false: POST /materiais/preview returns FAILURE status immediately (no Gemini call)
  - Respect user's choice throughout session
- [x] **T196** [P] Create consent revocation endpoint:
  - **DELETE /api/v1/usuarios/me/consent/ai-classification**: Revoke AI consent
  - Update consentimento_ia = false
  - Store ConsentRecord with revoked_at

#### Backend: Image Access Control

- [x] **T197** [P] Create `src/main/java/com/ecobook/controller/ImageController.java`:
  - **GET /api/v1/images/{image_id}**: Download image (authenticated only)
  - Path param: image_id (UUID)
  - Response: Binary image data with Content-Type (image/jpeg or image/png)
  - Error: 401 (not authenticated), 403 (not authorized), 404 (image not found)
- [x] **T198** [P] Implement image authorization in `ImageController.getImage()`:
  - Check if user is:
    - Image owner (donor), OR
    - Student with APROVADA+ Solicitacao for material, OR
    - Admin
  - Reject if none of above (HTTP 403)
  - Runtime note: the live implementation keeps front-cover discovery for authenticated users and restricts back-cover/relationship-sensitive access to donor, approved requester or admin
  - Serve image from filesystem
- [x] **T199** [P] Update Material entity:
  - Runtime note: the live code preserves `imagem_url`/`imagem_verso_url` in the API shape for backward compatibility, but both now resolve to authenticated `/api/v1/images/{upload_tracking_id}` URLs instead of public `/uploads/...`

#### Backend: Audit Logging

- [x] **T200** [P] Create `src/main/java/com/ecobook/entity/AuditLog.java` entity:
  - Fields: id (UUID), user_id, action (CREATE_MATERIAL, APPROVE_REQUEST, etc.), resource_id, resource_type, old_values (JSONB), new_values (JSONB), ip_address, timestamp
- [x] **T201** [P] Create AOP aspect for audit logging:
  - `@AuditLog` annotation on sensitive methods
  - Capture method parameters, return value, user, timestamp, IP
  - Store in AuditLog table
- [x] **T202** [P] Create audit trail query endpoint (admin only):
  - **GET /api/v1/admin/audit-log**: Query audit logs
  - Filters: user_id, action, resource_type, date_range
  - Response: Paginated list of AuditLogDto
- [x] **T203** [P] Create data export endpoint (LGPD right-to-data):
  - **POST /api/v1/usuarios/me/export**: Request personal data export
  - Backend generates ZIP with:
    - User profile JSON
    - All materials (metadata + image URLs)
    - All requests (anonymized as needed)
    - All consent records
    - All audit events involving user
  - Runtime note: the current backend streams the ZIP response synchronously instead of emailing a deferred link

#### Android: Consent UI

- [x] **T204** [P] Create consent dialogs in onboarding:
  - Platform consent: "I agree to EcoBook IA's terms and conditions and privacy policy" (checkbox)
  - AI consent prompt: "I consent to use of AI for analyzing material images" (optional at onboarding; can be enabled later from profile)
  - Platform consent remains required before proceeding; AI consent does not block profile completion
- [x] **T205** [P] Create consent management screen:
  - Show current consent status (PLATFORM: given, AI: given/not-given)
  - Option to grant, re-enable, or revoke AI consent anytime
  - Link to privacy policy + terms
- [x] **T206** [P] Create account deletion screen:
  - Button: "Delete Account Permanently"
  - Confirmation dialog: "Are you sure? All your data will be anonymized."
  - Show what will happen: materials cancelled, requests anonymized, images deleted
  - POST /usuarios/delete on confirmation
  - Clear local session, navigate to login screen

---

## PHASE 9: Performance, Optimization & Edge Cases (Weeks 17)

### Performance Tuning

- [ ] **T207** [P] Add database query optimization:
  - Profile slow queries (>1s) using PostgreSQL EXPLAIN
  - Add indexes for common filters (see Phase 4 indexes)
  - Verify query plans use indexes (SCAN method)
- [ ] **T208** [P] Configure connection pooling:
  - HikariCP max_connections = 20, min_idle = 5
  - Test with concurrent load (10+ simultaneous users)
  - Monitor connection utilization
- [x] **T209** [P] Implement response compression:
  - Add `compression: true` to Spring Boot actuator
  - Enable gzip compression for JSON responses > 1KB
  - Reduces network latency
- [x] **T210** [P] Add caching for immutable data:
  - Cache enum lists (disciplinas, niveis, sistemas) in memory
  - Cache user profiles (30-min TTL)
  - Use Spring Cache abstraction with @Cacheable
  - Runtime note (2026-05-23): authenticated profile, consent-status and security-context reads use a 30-minute cache baseline, and the immutable material-options catalog now ships through `GET /api/v1/reference-data/material-options` with backend cache plus Android fallback-driven consumption
- [x] **T211** [P] Implement pagination optimization:
  - Use keyset pagination (instead of offset) for large result sets
  - Example: GET /materiais?after_id={last_id}&size=20
  - Avoids expensive OFFSET skip
  - Runtime note (2026-05-23): `GET /api/v1/materiais` now returns `next_after_id` on pageable discovery responses, accepts `after_id` for same-filter continuation and lets Android discovery append through cursor/keyset windows while preserving the existing page metadata for UI state
- [x] **T212** [P] Add monitoring & alerting:
  - Configure Micrometer metrics export (optional: Prometheus)
  - Monitor: request count, latency, error rate, database connection pool
  - Setup Sentry for error tracking

### Edge Cases & Error Handling

- [ ] **T213** Create error scenario tests:
  - Concurrent requests for same material (race condition)
  - Network timeout during upload (partial file)
  - Gemini API down (503 Service Unavailable)
  - Database connection pool exhausted (500 Internal Server Error)
  - User deletes account while request APROVADA (cascade delete)
  - Material uploaded with very long title (truncate or reject)
  - WhatsApp number with special characters (sanitize)
  - City name with Unicode characters (normalize correctly)
- [x] **T214** Create rollback & recovery tests:
  - Payment failed mid-transaction (not applicable here, but transaction rollback testing)
  - File upload successful but database insert failed (cleanup orphaned file)
  - Notification failed to send but request created (retry queue)
- [ ] **T215** Test boundary conditions:
  - File exactly 5MB (accept)
  - File 5.001MB (reject)
  - Material with 0 requests (OK)
  - Material with 1000+ requests (performance test)
  - User with 1000+ materials (pagination stress test)
  - 14-day expiry at exact moment (no race condition)

### Integration & End-to-End Testing

- [ ] **T216** [P] Create 20+ end-to-end scenarios in `src/test/java/com/ecobook/E2ETests.java`:
  1. **Happy Path**: Register → Profile → Upload → Search → Request → Approve → Complete
  2. **Approval Race**: Two students request, both try to approve → one succeeds, one rejected
  3. **Expiry**: Material approved, wait 14 days, auto-revert, student discovers again
  4. **Decline Flow**: Student requests, donor declines, student sees RECUSADA
  5. **Cancellation**: Student cancels PENDENTE request, material stays DISPONIVEL
  6. **Profile Gate**: Incomplete profile tries POST /materiais → 403
  7. **Consentimento Gate**: consentimento_ia=false, POST /materiais/preview → FAILURE
  8. **Gemini Timeout**: Simulate 11-second API delay, request times out, FAILURE status
  9. **Invalid Enum**: POST /materiais with invalid disciplina → 400
  10. **Non-Existent Material**: Request non-existent material_id → 404
  11. **Self-Request**: Donor requests own material → 400
  12. **Already Requested**: Student requests same material twice → 409
  13. **Expired JWT**: Use 14-day-old token → 401
  14. **Concurrent Uploads**: Two users upload simultaneously (no file collision)
  15. **Image Formats**: Upload JPEG, PNG, animated GIF (reject), WebP (reject)
  16. **WhatsApp Formats**: Valid (+5511999999999), Invalid (11999999999, +551199999)
  17. **Geographic Normalization**: "São Paulo" === "SAO PAULO", "Ribeirão" === "RIBEIRAO"
  18. **SUPERIOR Year Ignored**: Filter SUPERIOR, year parameter ignored, all years returned
  19. **OUTRO System Rule**: Student wants OUTRO, receives only OUTRO + exact system (not other systems)
  20. **FCM Notification**: Approve request, notification sent and received in app

- [ ] **T217** [P] Create load test with 50+ concurrent users:
  - 20 users simultaneously uploading materials
  - 30 users simultaneously searching
  - Monitor: p95 latency, error rate, database load
  - Target: <2s p95 search latency, <1% error rate
- [x] **T218** [P] Create smoke test suite:
  - Verify core endpoints return 200 (health checks, search, upload stub)
  - Run before every deployment
  - Alert if smoke test fails

---

## PHASE 10: Polish & Documentation (Week 17)

### Code Quality & Documentation

- [ ] **T219** Code review all 200+ implementations:
  - Check: naming conventions, error handling, null checks, security
  - Verify: all TODOs addressed, no dead code
  - Ensure: consistent patterns across modules
- [ ] **T220** [P] Create API documentation (Swagger/OpenAPI):
  - Annotate all controllers with `@Operation`, `@Parameter`, `@RequestBody`, `@ApiResponse`
  - Generate Swagger UI at `http://localhost:8080/swagger-ui.html`
  - Document all 15+ endpoints with examples, error codes, auth requirements
- [ ] **T221** [P] Create architecture documentation:
  - Draw component diagram (Android, Backend, PostgreSQL, Firebase)
  - Document layer structure (controller → service → repository)
  - Explain state machine logic and consistency model
- [ ] **T222** [P] Create troubleshooting guide:
  - Common issues: "JWT expired", "Gemini timeout", "File too large"
  - Solutions for each
  - How to check logs and debug
- [ ] **T223** [P] Create deployment guide:
  - Build backend JAR: `mvn clean package`
  - Build Android APK: `./gradlew assembleRelease`
  - Deploy to Play Store (internal testing first)
  - Setup production database (migrations automated)
  - Configure environment variables (.env for backend)
- [ ] **T224** [P] Add JavaDoc comments to all public methods:
  - Describe: what method does, parameters, return value, exceptions
  - Example: GeminiService.classifyMaterial(File imageFile)
- [ ] **T225** [P] Add unit test documentation:
  - Document test structure: Arrange, Act, Assert
  - Explain each test's purpose and edge case coverage

### Final Quality Gates

- [ ] **T226** Verify 85%+ test coverage:
  - Run: `mvn clean test jacoco:report`
  - Check: target/site/jacoco/index.html
  - Coverage target: 85% line coverage for Phase 4 gate
- [ ] **T227** Run linting & code analysis:
  - CheckStyle: verify code style (naming, formatting)
  - SonarQube: identify code smells, security issues
  - Address all critical issues before merge
- [ ] **T228** Run security scanning:
  - OWASP Dependency Check: scan pom.xml for known vulnerabilities
  - Update dependencies if vulnerabilities found
  - Document any accepted risk (if dependency update breaks compatibility)
- [ ] **T229** Verify database migrations are reversible:
  - Create rollback script for each migration
  - Test: apply migration, rollback, re-apply (idempotent)
  - Ensure zero data loss during rollback
- [ ] **T230** Final E2E validation:
  - Run all 20+ end-to-end scenarios
  - Verify all success paths
  - Test error handling for all failure cases
  - Confirm all FCM notifications delivered
  - Performance: p95 latency <2s, no timeouts

---

## TASK SUMMARY

### Statistics

| Category | Count |
|----------|-------|
| **Total Tasks** | 230 |
| **Backend Tasks** | 95 |
| **Android Tasks** | 65 |
| **Testing Tasks** | 40 |
| **Ops/Deployment** | 15 |
| **Security/LGPD** | 15 |

### Tasks by Priority (P-flag)

- **Parallelizable [P]**: ~150 tasks (can run simultaneously where safe)
- **Sequential**: ~80 tasks (dependencies on prior phases)

### Tasks by User Story

| User Story | Task Count | Focus Area |
|-----------|-----------|-----------|
| **US1: Registration & Profile** | 30 | Auth, email/password, JWT, validation |
| **US2: Material Classification** | 35 | Upload, Gemini, confidence levels, storage |
| **US3: Material Discovery** | 25 | Matching algorithm, search, ranking |
| **US4: Request Workflow** | 50 | State machines, approval, atomic locking, notifications |
| **US5: Donation Completion** | 20 | Completion workflow, expiry job |
| **Cross-Cutting (FCM, Admin, LGPD)** | 50 | Notifications, moderation, privacy |
| **Polish & Testing** | 15 | Documentation, E2E, performance |

### Estimated Timeline

- **Phase 1: Setup** (Week 5–6): 50 tasks, 1 week
- **Phase 2: US1 Auth & Profile** (Week 7): 30 tasks, 1 week
- **Phase 3: US2 AI Classification** (Week 8–9): 35 tasks, 1.5 weeks
- **Phase 4: US3 Discovery** (Week 10): 25 tasks, 0.5 week
- **Phase 5: US4–US5 Requests** (Week 11–12): 70 tasks, 2 weeks (Phase 4+5 parallel after Phase 2)
- **Phase 6: FCM & Cross-Cutting** (Week 13–14): 50 tasks, 2 weeks (parallel with Phase 5)
- **Phase 7: Polish & Testing** (Week 15–17): 15 tasks, 1 week

**Total: 17 weeks (5 weeks parallel work)**

---

## Current Checkpoint After Auth Rebaseline

What is already true in the repository:

- ✅ Backend Spring Boot boots with health, auth and profile endpoints
- ✅ Android project compiles with login/cadastro/onboarding flow
- ✅ Database schema and migrations support local credentials (`password_hash`)
- ✅ JWT generation, validation and profile completeness gate are implemented
- ✅ `/api/v1/materiais/preview` and `/api/v1/materiais` now run the Phase 3 preview/create flow end to end
- ✅ `GET /api/v1/materiais` now delivers the Phase 4 discovery flow end to end
- ✅ Phase 5 request workflow is already present in runtime on backend and Android
- ✅ FCM token registration plus basic notification dispatch/permission flow already exist as the start of Phase 6
- ✅ Phase 7 reporting plus admin moderation/runtime authorization are now implemented on backend, with the Android student reporting flow already live
- ✅ Phase 8 LGPD/security runtime is implemented through consent history, account deletion/anonymization, authenticated image access, audit logging and data export
- ✅ Phase 9 is the current active stop point, now with response compression, Micrometer/Prometheus metrics, smoke coverage, immutable reference-data catalog caching, discovery cursor pagination and rollback cleanup already live
- ✅ Backend regression is stable again (`mvn test` green with 138 tests on the current runtime) and Android local JVM validation is still green through `scripts/Invoke-GradleAsciiPath.ps1 app:testDebugUnitTest`

Phase 3 closeout notes:

- Backend automated coverage now includes preview/create scenarios and passes with `57` tests via `mvn test`
- Android donation flow now uses real image selection/capture, preview IA, manual review and final publish instead of the earlier mock screen
- Android now includes dedicated `AuthViewModel` JVM validation in `app/src/test`, a dedicated auth instrumentation E2E in `app/src/androidTest/kotlin/com/ecobook/auth/AuthFlowE2ETest.kt`, and Windows paths with accents/spaces are handled through `scripts/Invoke-GradleAsciiPath.ps1` so both `testDebugUnitTest` and targeted `connectedDebugAndroidTest` runs can execute through an ASCII drive alias when needed

---

## Next Steps

1. **Assign Tasks**: Distribute tasks to backend dev, Android dev, QA based on user story phases
2. **Create Tickets**: Convert tasks to GitHub Issues with labels (backend, android, testing, RFC references)
3. **Setup CI/CD**: GitHub Actions workflow ready (T030)
4. **Revalidate Phase 6 End to End**: run the implemented notification stack against a real Firebase project/device flow and capture execution notes
5. **Advance Phase 9**: finish load validation, boundary/edge-case coverage and the remaining Firebase hardening evidence
6. **Start the practical Phase 10 closeout**: runtime/API docs, troubleshooting, deployment notes and final quality gates

---

**Generated**: 2026-04-15  
**Status**: Phase 8 runtime delivery recorded and Phase 9 is actively underway; use this file as a living backlog for Firebase validation plus the remaining load/edge-case hardening and Phase 10 closeout  
**Document Owner**: Product/Tech Lead  
**Last Updated**: 2026-05-23

