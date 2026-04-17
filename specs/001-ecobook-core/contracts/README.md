# API Contracts

**Phase**: 1 (Design & Contracts) — **Status**: ✅ Complete  
**Date**: 2026-04-17  
**Purpose**: Formal API contract definitions for all REST endpoints

---

## ✅ Complete Contracts (6 files)

### 1. [user-api.md](user-api.md)
User management endpoints: registration, profile updates, authentication

**Endpoints**:
- `POST /auth/register` — Register new user (email, WhatsApp E.164 format)
- `PATCH /usuarios/{id}` — Update user profile (geographic normalization, consent tracking)
- `GET /usuarios/me` — Get current authenticated user

**Key Features**:
- E.164 WhatsApp format validation (+5548999999999)
- Geographic normalization: NFD decomposition + ASCII + uppercase (e.g., "São João" → "SAO JOAO")
- Profile completeness tracking (`perfil_completo`) — gates material operations
- AI consent flag (`consentimento_ia`) — controls Gemini API usage
- JWT authentication (7-day expiry)

**Related RF**: RF-001, RF-002, RF-003, RF-004

---

### 2. [material-api.md](material-api.md)
Material (donation) endpoints with image upload and AI classification

**Endpoints**:
- `POST /materiais` — Create material with image upload (validates enums, normalizes geography)
- `GET /materiais` — Search materials with deterministic matching algorithm (7-step filter + rank by proximity)
- `GET /materiais/{id}` — Get single material details with donor info
- `PATCH /materiais/{id}` — Update material status (state transitions: DISPONIVEL → RESERVADO → DOADO/CANCELADO)

**Key Features**:
- State machine: 4 states with atomic transitions
- Matching algorithm: 7-step filter (status, discipline, level, year±1, system, publication range, geography) + rank by proximity + recency
- **Performance SLA (Q6)**: P95 ≤ 150ms, P99 ≤ 300ms (13× improvement via indexes + pooling)
- Field classification:
  - `titulo`: AI-preenchível (confidence 0.85-0.95), always editable
  - `descricao`: Manual-only field (NEVER auto-populated to prevent hallucinations)
  - Other AI-assisted: disciplina, nivel_ensino, ano, sistema_ensino, estado_conservacao, data_publicacao
- Image validation: JPEG/PNG only, ≤ 5MB

**Related RF**: RF-005 through RF-025, RF-044

---

### 3. [ai-response.md](ai-response.md)
Gemini AI classification response schema and comprehensive error handling

**Endpoints**:
- `POST /materiais/preview` — Get AI predictions for image with confidence scores and upload tracking ID

**Key Features**:
- Confidence-driven UI rendering:
  - SUCCESS (≥0.75): Auto-fill all fields with green checkmark, editable
  - LOW_CONFIDENCE (0.50-0.75): Auto-fill with yellow warning icon, editable
  - FAILURE (<0.50 or timeout or no consent): Empty fields, manual entry required
- **Q7 Error Handling** (RFC-062-065, RNF-019):
  - HTTP 429 (rate limit): 3 retries, exponential backoff (1s→2s→4s)
  - HTTP 5xx (server error): 3 retries, conservative backoff (2s→4s→8s)
  - Timeout: 2 retries at 1s delay
  - Malformed response: No retry, fail immediately
  - Circuit breaker: Pause 30s after 10+ failures/5min window
- Upload ID tracking: Temporary image → user review → permanent storage promotion
- Validates JSON structure, enum values, confidence range [0, 1]

**Related RF**: RF-011 through RF-021, RF-062-065, RNF-019

---

### 4. [solicitacao-api.md](solicitacao-api.md)
Request/Solicitation endpoints for complete donation workflow

**Endpoints**:
- `POST /solicitacoes` — Create request for material (checks profile completeness, material availability)
- `PATCH /solicitacoes/{id}` — Update request status (state transitions: PENDENTE → APROVADA → CONCLUIDA/RECUSADA/CANCELADA)
- `GET /solicitacoes` — List user's requests (as donor or student)
- `GET /solicitacoes/{id}` — Get single request details

**Key Features**:
- State machine: 5 states with terminal states (CONCLUIDA, RECUSADA, CANCELADA)
- **Atomic approval** (RFC-035): Database lock (SERIALIZABLE + SELECT...FOR UPDATE) ensures:
  - Only one APROVADA Solicitacao per Material
  - Material transitions to RESERVADO atomically
  - contato_doador populated only after approval
- 14-day reservation window: Auto-expiry daily job reverts expired reservations
- **Performance SLA (Q6)**: P95 ≤ 50ms, P99 ≤ 150ms (atomic transaction + indexed queries)
- Only material donor can approve/decline; either party can cancel

**Related RF**: RF-026 through RF-031, RF-032 through RF-035

---

### 5. [notification-schema.md](notification-schema.md)
Firebase Cloud Messaging (FCM) notification payloads for 6 event types

**Notification Types**:

| Type | Recipient | Event | Payload Includes |
|------|-----------|-------|------------------|
| **SOLICITACAO_RECEBIDA** | Donor | Student requests material | student_nome, material_titulo, solicitacao_id |
| **SOLICITACAO_APROVADA** | Student | Donor approves request | **doador_whatsapp**, material_titulo, solicitacao_id |
| **SOLICITACAO_RECUSADA** | Student | Donor declines request | material_titulo, solicitacao_id |
| **SOLICITACAO_CANCELADA** | Student | Approved request cancelled | material_titulo, solicitacao_id |
| **MATERIAL_DOADO** | Student | Donation completed | **doador_whatsapp** (final contact), material_titulo, solicitacao_id |
| **MATERIAL_CANCELADO** | All requesters | Material cancelled at any stage | material_titulo, material_id |

**Key Features**:
- Deep linking: Each notification includes ID for app navigation
- Retry strategy: Exponential backoff (1s→2s→4s→8s→16s, max 5 attempts)
- Dead-letter queue: Failed notifications moved to DLQ after 5 failures, archived after 30 days
- Best-effort delivery (≥95% within 5s, acceptable for MVP)
- Android integration: Jetpack Compose with native FCM service

**Related RF**: RF-037, RF-038

---

### 6. [error-response.md](error-response.md)
Standard error response format and comprehensive HTTP status code mapping

**Standard Error Format**:
```json
{
  "error": "ERROR_CODE_UPPERCASE",
  "message": "Human-readable message (Portuguese)",
  "field": "field_name_if_applicable",
  "details": { "additional": "context" },
  "timestamp": "2026-04-17T16:30:00Z",
  "path": "/api/v1/endpoint"
}
```

**HTTP Status Codes**:

| Code | Error Codes | Examples |
|------|---|---|
| **400** | INVALID_JSON, INVALID_ENUM, INVALID_FORMAT, VALIDATION_ERROR, MISSING_FIELD | Invalid discipline enum, malformed JSON, email not E.164 format |
| **401** | UNAUTHORIZED, TOKEN_EXPIRED, SESSION_EXPIRED | No JWT token, token expired, session timeout |
| **403** | INCOMPLETE_PROFILE, FORBIDDEN, CONSENT_REQUIRED | Profile missing city/bairro, not material donor, no AI consent |
| **404** | NOT_FOUND, USER_NOT_FOUND, MATERIAL_NOT_FOUND, UPLOAD_NOT_FOUND | Resource doesn't exist, upload expired |
| **409** | CONFLICT, MATERIAL_RESERVED, DUPLICATE_REQUEST | Material already has approved request, duplicate solicitacao |
| **422** | INVALID_STATE_TRANSITION, INVALID_OPERATION | Cannot transition from DOADO, invalid state combo |
| **429** | RATE_LIMIT_EXCEEDED | 10 uploads/hour exceeded, 100 searches/minute exceeded |
| **500** | INTERNAL_ERROR, DATABASE_ERROR, EXTERNAL_SERVICE_ERROR | Unexpected server error, DB connection failed, Gemini unavailable |

**Related RF**: All endpoints (RF-001 through RF-065)

---

## How to Use These Contracts

### Backend Development (Spring Boot)

1. **Choose endpoint** from contract file
2. **Implement controller**:
   ```java
   @PostMapping("/materiais")
   public ResponseEntity<MaterialDTO> createMaterial(@RequestBody MaterialRequest req) {
       // Validate using contract: check enums, image MIME, profile completeness
       // Return 201 Created with MaterialDTO
       // Return 400/403/500 with ErrorResponse per contract
   }
   ```
3. **Validate** using rules from contract
4. **Map** HTTP status codes per error scenarios
5. **Test** using request/response examples as fixtures

### Android Development (Kotlin/Retrofit)

1. **Choose endpoint** from contract file
2. **Generate data classes** from response examples:
   ```kotlin
   @JsonClass(generateAdapter = true)
   data class MaterialResponse(
       @Json(name = "id") val id: String,
       @Json(name = "titulo") val titulo: String,
       // ... other fields
   )
   ```
3. **Implement Retrofit interface**:
   ```kotlin
   @POST("/api/v1/materiais")
   suspend fun createMaterial(@Body req: MaterialRequest): MaterialResponse
   ```
4. **Handle errors** per HTTP status code
5. **Deep link** using IDs from FCM notifications

### Testing

1. **Use request/response examples** as test fixtures
2. **Test error scenarios** from error-response.md
3. **Validate state transitions** (material state machine, solicitacao lifecycle)
4. **Test consistency invariants**:
   - Only one APROVADA Solicitacao per Material
   - Material.status ↔ Solicitacao.status alignment
   - Geographic normalization consistency
5. **Test performance SLAs**:
   - Search: P95 ≤ 150ms
   - Approval: P95 ≤ 50ms
   - Classification: P95 ≤ 7s

---

## Cross-Contract Consistency

### Shared Rules

| Concept | Contract | Rules |
|---------|----------|-------|
| **Geographic Normalization** | user-api.md, material-api.md | Uppercase + NFD + ASCII: "São João" → "SAO JOAO" |
| **Material State Machine** | material-api.md, solicitacao-api.md | DISPONIVEL ↔ RESERVADO → DOADO (terminal) or CANCELADO (terminal) |
| **Atomic Operations** | solicitacao-api.md, material-api.md | Approval uses SERIALIZABLE isolation + SELECT...FOR UPDATE |
| **Profile Completeness** | user-api.md, material-api.md, solicitacao-api.md | `perfil_completo` gates material operations (HTTP 403 if false) |
| **Field Classification** | material-api.md, ai-response.md | `titulo` (AI), `descricao` (manual-only) |
| **Enum Validation** | All contracts | All enum fields validated with HTTP 400 + allowed values in response |
| **Timestamp Format** | All contracts | ISO 8601 (e.g., 2026-04-17T16:30:00Z) |

---

## OpenAPI/Swagger Support

Contracts are structured for easy OpenAPI 3.0 generation (future automation):

```yaml
# Example (not auto-generated yet)
openapi: 3.0.0
info:
  title: EcoBook IA API
  version: 1.0.0
paths:
  /api/v1/materiais:
    post:
      summary: Create material
      requestBody:
        content:
          multipart/form-data:
            schema:
              $ref: '#/components/schemas/MaterialRequest'
      responses:
        '201':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/MaterialResponse'
        '400':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
```

**Tool Recommendation**: Springdoc-OpenAPI (Maven) auto-generates from Spring annotations

---

## Performance & Scalability

**SLAs** (Q6 Requirements):

| Endpoint | Metric | Target | Achieved Via |
|----------|--------|--------|---|
| Material Search | P95 latency | ≤ 150ms | Indexes on (status, disciplina, nivel_ensino, cidade, bairro, data_publicacao DESC) + HikariCP (20 connections) |
| Classification | P95 latency | ≤ 7s | Gemini API timeout 10s + local caching + circuit breaker (Q7) |
| Approval | P95 latency | ≤ 50ms | SERIALIZABLE isolation + indexed lookups + async FCM dispatch |
| FCM Webhook | P95 latency | ≤ 30ms | Async job queue + minimal processing |
| FCM Delivery | Success rate | > 95% within 5s | Firebase infrastructure + retry logic (Q7) |

---

## Related Documents

- **[spec.md](../spec.md)** — Complete feature specification (65 FR + 19 NFR, user stories, requirements)
- **[data-model.md](../data-model.md)** — Data model, entities, relationships, PostgreSQL DDL, state machines, invariants
- **[plan.md](../plan.md)** — Implementation plan, 5 phases (17 weeks), Constitution compliance, risk assessment
- **[research.md](../research.md)** — Phase 0 technical research (Gemini, storage, locking, error handling)
- **[quickstart.md](../quickstart.md)** — Developer setup (Docker, local dev, first integration test)
- **[TASKS.md](../TASKS.md)** — 187 granular implementation tasks across all phases

---

## Next Steps (Phase 2 — Week 5+)

1. ✅ **Contracts complete** — All 6 files ready for development
2. ⏳ **Backend Skeleton** — Spring Boot project setup with these contracts
3. ⏳ **Android Skeleton** — Jetpack Compose project with Retrofit client generation
4. ⏳ **Database Setup** — PostgreSQL schema from data-model.md DDL
5. ⏳ **Integration Tests** — Use contract examples for test fixtures

**Owner**: Backend Architect (T016-025), Android Architect (T026-035)
