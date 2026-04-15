# Feature Specification: EcoBook IA - Core System Architecture

**Feature Branch**: `001-ecobook-core`  
**Created**: 2026-04-15  
**Status**: Draft  
**Architecture**: Android Native (Kotlin, Jetpack Compose) + Spring Boot Backend + PostgreSQL

---

## Overview

EcoBook IA is an AI-powered material donation matching platform designed to promote educational equity by connecting material donors with students in need. The system matches available donated materials (books, educational supplies) with student requirements using curriculum alignment, geographic proximity, and AI-assisted classification.

**Core Value Proposition**: Reduce barriers to educational access through intelligent material matching and efficient donation workflows.

---

## User Scenarios & Testing

### User Story 1 - Donor Registers and Creates Material Profile (Priority: P1)

A donor (teacher, organization, or individual) registers on the platform, completes their profile, and lists a donated material with the system.

**Why this priority**: Core functionality - the platform cannot function without material supply. Establishing trustworthy donor profiles is essential for platform credibility.

**Independent Test**: Can be fully tested by: (1) Creating a user account, (2) Uploading a material image, (3) Verifying the material appears as DISPONIVEL in the system and matches student queries.

**Acceptance Scenarios**:

1. **Given** an unregistered donor accesses the app, **When** they select "Register as Donor", **Then** they see a registration form requesting name, email, WhatsApp (E.164 format), city, neighborhood, and optional institution affiliation
2. **Given** a registered donor with incomplete profile, **When** they attempt POST /materiais, **Then** the system returns HTTP 403 Forbidden with field `perfil_completo: false`
3. **Given** a donor with complete profile selects a book image, **When** they confirm the system's AI suggestions, **Then** the material is persisted with status DISPONIVEL and appears in student discovery flows

---

### User Story 2 - AI-Assisted Material Classification (Priority: P1)

A donor uploads a material image. The backend calls Google Gemini to extract metadata (discipline, education level, system). The system handles AI confidence levels and provides fallback options.

**Why this priority**: AI classification is the technical differentiator; low confidence or timeout scenarios must degrade gracefully to preserve UX.

**Independent Test**: Can be fully tested by: (1) Uploading various material types (math textbook, novel, workbook), (2) Verifying correct confidence levels trigger appropriate UI behaviors (auto-fill, warnings, manual entry), (3) Testing timeout recovery.

**Acceptance Scenarios**:

1. **Given** a donor selects an image of a math textbook via POST /materiais/preview, **When** the Gemini API returns confidence ≥ 0.75, **Then** all fields (discipline, nivel_ensino, sistema_ensino) are auto-populated with disabled-for-edit inputs
2. **Given** Gemini returns confidence 0.50–0.75 (LOW_CONFIDENCE), **When** the response renders in the frontend, **Then** fields display suggested values with yellow warning icons and are editable
3. **Given** Gemini confidence < 0.50 or timeout occurs, **When** response is returned, **Then** all classification fields render empty and require manual input; status_ia displays FAILURE or LOW_CONFIDENCE
4. **Given** consentimento_ia = false for the user, **When** they upload an image, **Then** POST /materiais/preview returns status_ia: FAILURE and no Gemini call is made

---

### User Story 3 - Student Discovers Matching Materials (Priority: P1)

A student specifies their academic needs (discipline, education level, curriculum system, grade year, geographic area). The system matches available materials using a deterministic algorithm and displays results ranked by proximity and recency.

**Why this priority**: Core value delivery - students must reliably find materials that match their requirements.

**Independent Test**: Can be fully tested by: (1) Creating a student profile with specific needs, (2) Querying materials in various discipline/level/system combinations, (3) Verifying ranking order (same neighborhood first, then same city, then by date).

**Acceptance Scenarios**:

1. **Given** a student with profile (discipline: MATEMATICA, nivel_ensino: FUNDAMENTAL, ano: 7, cidade: Florianópolis, bairro: Centro), **When** they query available materials, **Then** the system returns materials where: status = DISPONIVEL AND disciplina = MATEMATICA AND nivel_ensino = FUNDAMENTAL AND |ano_material - 7| ≤ 1 (or SUPERIOR ignores year filter) AND sistema_ensino matches
2. **Given** multiple matching materials exist, **When** results are ranked, **Then** order is: (a) same neighborhood first, (b) same city next, (c) sorted by data_criacao DESC (newest first), (d) ID as tiebreaker
3. **Given** a student searches for SUPERIOR level materials, **When** results are filtered, **Then** year constraint is ignored; materials are matched purely by discipline, level, and system
4. **Given** a student searches for sistema_ensino = OUTRO, **When** the system filters, **Then** it returns ONLY materials with sistema_ensino = OUTRO (not all systems)

---

### User Story 4 - Student Requests Material (Priority: P1)

A student finds a matching material and submits a request. The system records the request, notifies the donor via FCM, and transitions the material to RESERVADO if approved.

**Why this priority**: Core transaction flow - requests are the primary interaction between supply and demand.

**Independent Test**: Can be fully tested by: (1) Creating a material and student profile, (2) Submitting a solicitacao, (3) Verifying FCM notification delivery and state transitions.

**Acceptance Scenarios**:

1. **Given** a student views a DISPONIVEL material, **When** they submit a request via POST /solicitacoes, **Then** the system creates a Solicitacao with status = PENDENTE and returns it
2. **Given** a request is created, **When** the request reaches PENDENTE, **Then** an FCM notification SOLICITACAO_RECEBIDA is sent to the donor
3. **Given** a donor approves a request via PATCH /solicitacoes/{id} with status = APROVADA, **When** the operation succeeds, **Then**: (a) the Solicitacao transitions to APROVADA, (b) the Material transitions to RESERVADO, (c) an FCM notification SOLICITACAO_APROVADA is sent to the student, (d) the Material is locked (no new requests accepted)
4. **Given** a request is in PENDENTE state, **When** the donor declines via PATCH with status = RECUSADA, **Then** the Solicitacao transitions to RECUSADA and FCM notification SOLICITACAO_RECUSADA is sent
5. **Given** a Solicitacao is APROVADA and 14 days pass, **When** the Material is not marked DOADO, **Then** the system automatically reverts Material.status to DISPONIVEL and Solicitacao.status to CANCELADA

---

### User Story 5 - Material Donation Completion (Priority: P2)

After the student receives and accepts the material, the donor marks it as DOADO. The system records completion, sends final notifications, and archives the transaction.

**Why this priority**: Important for feedback loop and donor engagement, but does not block core matching functionality.

**Independent Test**: Can be fully tested by: (1) Completing a full request-approval cycle, (2) Marking material as DOADO, (3) Verifying state finality and notification delivery.

**Acceptance Scenarios**:

1. **Given** a Solicitacao is in APROVADA state, **When** the donor calls PATCH /solicitacoes/{id} with status = CONCLUIDA, **Then** the Material transitions to DOADO and Solicitacao.status = CONCLUIDA
2. **Given** a donation is marked CONCLUIDA, **When** the transition occurs, **Then** an FCM notification MATERIAL_DOADO is sent to the student with donor contact details (contato_doador)
3. **Given** a Material is in DOADO state, **When** a new request arrives, **Then** the system returns HTTP 422 Unprocessable Entity (invalid state transition)

---

### User Story 6 - Material Cancellation and Reversion (Priority: P2)

A donor can cancel a material donation at various stages. The system enforces business rules for cancellation and reverts state appropriately.

**Why this priority**: Important for donor flexibility and error recovery, but secondary to core matching.

**Independent Test**: Can be fully tested by: (1) Canceling DISPONIVEL materials, (2) Canceling RESERVADO materials (with side effects on requests), (3) Verifying invalid transitions are rejected.

**Acceptance Scenarios**:

1. **Given** a Material in DISPONIVEL state, **When** the donor calls PATCH /materiais/{id} with status = CANCELADO, **Then** the Material transitions to CANCELADO and is no longer discoverable
2. **Given** a Material in RESERVADO state with an APROVADA Solicitacao, **When** the donor cancels, **Then** the Material transitions to CANCELADO, the Solicitacao transitions to CANCELADA, and FCM notification SOLICITACAO_CANCELADA is sent
3. **Given** a Material in CANCELADO state, **When** a request tries to modify it, **Then** the system returns HTTP 422 Unprocessable Entity
4. **Given** a Material in DOADO state, **When** cancellation is attempted, **Then** the system returns HTTP 422 Unprocessable Entity (final state, cannot revert)

---

### User Story 7 - Profile Completion and Onboarding (Priority: P1)

Users must complete their profiles before performing restricted operations. The system enforces profile completeness checks.

**Why this priority**: Profile data is essential for geographic matching, contact verification, and platform trust.

**Independent Test**: Can be fully tested by: (1) Creating incomplete profile, (2) Attempting restricted operations, (3) Verifying HTTP 403 responses, (4) Completing profile and confirming access.

**Acceptance Scenarios**:

1. **Given** a newly registered user with incomplete profile (missing city or WhatsApp), **When** they query GET /usuarios/me, **Then** the response includes `perfil_completo: false`
2. **Given** a user with `perfil_completo: false`, **When** they attempt POST /materiais or POST /solicitacoes, **Then** the system returns HTTP 403 Forbidden
3. **Given** a user with incomplete profile, **When** they complete all required fields via PATCH /usuarios/{id}, **Then** `perfil_completo` transitions to true and restricted operations become available

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

- **RF-001**: System MUST support user registration with email and WhatsApp (E.164 format: +5548999999999)
- **RF-002**: System MUST enforce profile completeness before allowing material operations; endpoint GET /usuarios/me returns `perfil_completo: boolean`
- **RF-003**: System MUST normalize geographic data: uppercase letters, remove accents (NFD + ASCII), trim whitespace; e.g., "são joão" → "SAO JOAO"
- **RF-004**: System MUST track `consentimento_ia: boolean` per user to control Gemini API calls

#### Material Management & Lifecycle

- **RF-005**: Material lifecycle states are: DISPONIVEL (initially), RESERVADO (approved request), DOADO (completed donation), CANCELADO (donor-initiated cancellation); no other states allowed
- **RF-006**: System MUST reject invalid state transitions with HTTP 422 Unprocessable Entity
- **RF-007**: Material MUST be discoverable only in DISPONIVEL state
- **RF-008**: Material creation requires: titulo, descricao, disciplina, nivel_ensino, ano, sistema_ensino, estado_conservacao, imagem (JPEG/PNG, ≤ 5MB), doador_id, cidade, bairro, data_criacao
- **RF-009**: Material images MUST be validated for MIME type (JPEG/PNG only); invalid types rejected with HTTP 400
- **RF-010**: Image uploads MUST return `upload_id` in POST /materiais response for tracking and promotion from temporary to permanent storage

#### Image Processing Pipeline

- **RF-011**: POST /materiais accepts multipart form data with image file; backend saves temporarily to disk/S3
- **RF-012**: Backend MUST call Google Gemini API with 10-second timeout; timeout returns status_ia = FAILURE
- **RF-013**: Backend MUST parse Gemini response and validate: JSON structure, presence of best_prediction, enum values, confidence in [0, 1]
- **RF-014**: If any validation fails, set status_ia = LOW_CONFIDENCE or FAILURE; return to frontend with parsed/partial data
- **RF-015**: After user confirmation (PATCH /materiais/{id}), backend MUST validate all enum values and persist material with status = DISPONIVEL
- **RF-016**: Temporary image file MUST be promoted to permanent storage after successful persistence

#### AI Confidence & Fallback Rules

- **RF-017**: Gemini confidence ≥ 0.75 → fields auto-filled, disabled for edit in UI, status_ia = SUCCESS
- **RF-018**: Gemini confidence 0.50–0.75 → fields auto-filled with warning icon, editable, status_ia = LOW_CONFIDENCE
- **RF-019**: Gemini confidence < 0.50 → all fields empty, manual entry required, status_ia = LOW_CONFIDENCE
- **RF-020**: Gemini FAILURE or timeout → all fields empty, manual entry required, status_ia = FAILURE
- **RF-021**: If consentimento_ia = false, backend skips Gemini call and returns status_ia = FAILURE

#### Material Matching Algorithm

- **RF-022**: Matching pipeline (in order): (1) Filter status = DISPONIVEL, (2) Filter disciplina (exact), (3) Filter nivel_ensino (exact), (4) Filter |ano_material - ano_student| ≤ 1 (except SUPERIOR, which ignores year), (5) Filter sistema_ensino (exact; OUTRO matches only OUTRO), (6) Sort by: same bairro > same cidade > data_criacao DESC > id
- **RF-023**: Query endpoint: GET /materiais?disciplina=X&nivel_ensino=Y&ano=Z&sistema_ensino=W&cidade=C&bairro=B returns paginated results
- **RF-024**: Special rule: SUPERIOR level (nivel_ensino = SUPERIOR) ignores year constraint in filtering
- **RF-025**: Special rule: sistema_ensino = OUTRO matches ONLY materials with sistema_ensino = OUTRO (not a wildcard)

#### Request Lifecycle & Approval

- **RF-026**: Request lifecycle states: PENDENTE (initial), APROVADA (donor accepted), RECUSADA (donor declined), CANCELADA (either party cancelled), CONCLUIDA (donation completed); no other states
- **RF-027**: POST /solicitacoes creates Solicitacao with status = PENDENTE; Material remains DISPONIVEL initially
- **RF-028**: Donor approval: PATCH /solicitacoes/{id} with status = APROVADA atomically (1) updates Solicitacao.status, (2) updates Material.status = RESERVADO, (3) sends FCM notification SOLICITACAO_APROVADA to student
- **RF-029**: Donor decline: PATCH /solicitacoes/{id} with status = RECUSADA updates Solicitacao.status; Material remains DISPONIVEL; sends FCM notification SOLICITACAO_RECUSADA
- **RF-030**: Material MUST have at most ONE active (APROVADA) Solicitacao at any time; subsequent requests to same material are rejected with HTTP 409 Conflict if Material.status = RESERVADO
- **RF-031**: Completion: PATCH /solicitacoes/{id} with status = CONCLUIDA transitions Material.status = DOADO and Solicitacao.status = CONCLUIDA

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
  - MATERIAL_CANCELADO: Sent to student when material is cancelled
- **RF-038**: FCM must be reliable; failures are logged but do not block primary operations

#### Enums & Data Types

- **RF-039**: System MUST enforce enum values:
  - Disciplina: MATEMATICA | PORTUGUES | HISTORIA | GEOGRAFIA | CIENCIAS | LITERATURA
  - NivelEnsino: FUNDAMENTAL | MEDIO | SUPERIOR
  - SistemaEnsino: ANGLO | OBJETIVO | COC | POSITIVO | OUTRO
  - EstadoConservacao: NOVO | BOM | USADO | DANIFICADO
  - StatusMaterial: DISPONIVEL | RESERVADO | DOADO | CANCELADO
  - StatusSolicitacao: PENDENTE | APROVADA | RECUSADA | CANCELADA | CONCLUIDA
  - StatusRespostaIA: SUCCESS | LOW_CONFIDENCE | FAILURE
- **RF-040**: Enum validation must reject invalid values with HTTP 400 and error message specifying field and allowed values

#### Contact & Communication

- **RF-041**: WhatsApp contact MUST be stored in E.164 format (e.g., +5548999999999)
- **RF-042**: Donor contact details (nome, whatsapp) are included in MATERIAL_DOADO notification and in Solicitacao.contato_doador only when Solicitacao.status = APROVADA
- **RF-043**: Solicitacao.contato_doador is NULL when status ≠ APROVADA

---

### Non-Functional Requirements

- **RNF-001**: System MUST support Android 8.0+ (API level 26+); native Kotlin implementation via Jetpack Compose
- **RNF-002**: Backend MUST be Spring Boot 3.x with Java 17+; PostgreSQL 14+ as primary database
- **RNF-003**: Gemini API calls MUST timeout after 10 seconds; timeout returns FAILURE status
- **RNF-004**: Image processing MUST validate and reject non-JPEG/PNG files with HTTP 400
- **RNF-005**: System MUST log all security events (authentication, authorization, state transitions) with timestamp and actor

---

## Key Entities

### User (Usuário)

Represents a registered user who can act as donor or student.

**Attributes**:
- `id` (UUID): Unique identifier
- `email` (String): Unique email address
- `nome` (String): User's full name
- `whatsapp` (String): E.164 format (+55XXXXXXXXXXX)
- `cidade` (String): Normalized to uppercase, no accents
- `bairro` (String): Normalized to uppercase, no accents
- `instituicao` (String, optional): School/organization affiliation
- `perfil_completo` (Boolean): True if all required fields completed
- `consentimento_ia` (Boolean): True if user consents to AI processing
- `role` (Enum: DOADOR | ESTUDANTE | AMBOS): Primary account type
- `created_at` (Timestamp)
- `updated_at` (Timestamp)

---

### Material (Entidade Material)

Represents a donated educational resource.

**Attributes**:
- `id` (UUID): Unique identifier
- `titulo` (String): Material title
- `descricao` (String): Detailed description
- `disciplina` (Enum: MATEMATICA | PORTUGUES | HISTORIA | GEOGRAFIA | CIENCIAS | LITERATURA)
- `nivel_ensino` (Enum: FUNDAMENTAL | MEDIO | SUPERIOR)
- `ano` (Integer): Target grade/year (1–12 for FUNDAMENTAL/MEDIO, null for SUPERIOR)
- `sistema_ensino` (Enum: ANGLO | OBJETIVO | COC | POSITIVO | OUTRO)
- `estado_conservacao` (Enum: NOVO | BOM | USADO | DANIFICADO)
- `status` (Enum: DISPONIVEL | RESERVADO | DOADO | CANCELADO)
- `imagem_url` (String): URL to permanent storage
- `upload_id` (String, optional): Temporary upload tracking ID
- `doador_id` (UUID): Foreign key to User (donor)
- `cidade` (String): Normalized geographic location
- `bairro` (String): Normalized neighborhood
- `data_criacao` (Timestamp): When material was first offered
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
    "estado_conservacao": {
      "value": "BOM",
      "confidence": 0.81
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
- If `status_ia = LOW_CONFIDENCE`: some fields may be null or have confidence < 0.50; frontend marks with warning and allows editing
- If `status_ia = SUCCESS`: all fields present with confidence ≥ 0.75; frontend auto-fills and disables edit
- If Gemini timeout or consentimento_ia = false: `status_ia = FAILURE`, `best_prediction` empty object

---

### 2. Material Cadastro (POST /materiais - Frontend Request)

```json
{
  "titulo": "Livro de Matemática 7º Ano",
  "descricao": "Usado em colégios do sistema Anglo, bem conservado",
  "disciplina": "MATEMATICA",
  "nivel_ensino": "FUNDAMENTAL",
  "ano": 7,
  "sistema_ensino": "ANGLO",
  "estado_conservacao": "BOM",
  "cidade": "Florianópolis",
  "bairro": "Centro",
  "imagem": "<base64-or-multipart-file>"
}
```

**Backend Processing**:
1. Parse/validate multipart form
2. Save image temporarily
3. Call POST /materiais/preview (internal or separate endpoint)
4. Merge preview results into request
5. Validate all enums
6. Persist Material with status = DISPONIVEL
7. Promote temporary image to permanent storage
8. Return HTTP 201 Created with full Material entity

---

### 3. Material Response (GET /materiais/{id} - Backend Response)

```json
{
  "id": "material-uuid-1",
  "titulo": "Livro de Matemática 7º Ano",
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
  "data_criacao": "2026-04-10T14:30:00Z",
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
- **Request**: `{ email, nome, whatsapp, role: DOADOR|ESTUDANTE|AMBOS }`
- **Response**: User entity (HTTP 201)
- **Rules**: 
  - WhatsApp must be E.164 format
  - Email must be unique
  - Initial `perfil_completo = false` until city/neighborhood added

#### PATCH /usuarios/{id}
- **Method**: PATCH
- **RFC**: RF-002, RF-003, RF-004
- **Description**: Update user profile
- **Request**: `{ nome, whatsapp, cidade, bairro, instituicao, consentimento_ia }`
- **Response**: Updated User entity (HTTP 200)
- **Rules**:
  - Normalize cidade/bairro (uppercase, no accents)
  - Set `perfil_completo = true` if all required fields present
  - `consentimento_ia` controls Gemini API usage

#### GET /usuarios/me
- **Method**: GET
- **RFC**: RF-002
- **Description**: Get current user profile
- **Response**: User entity including `perfil_completo` (HTTP 200)
- **Authentication**: Required (JWT/session token)

---

### 2. Material Management

#### POST /materiais
- **Method**: POST (multipart/form-data)
- **RFC**: RF-005 through RF-010, RF-039
- **Description**: Create new material with image upload
- **Request**: Material data + image file (JPEG/PNG, ≤ 5MB)
- **Response**: Material entity with `status = DISPONIVEL` and `upload_id` (HTTP 201)
- **Rules**:
  - User must have `perfil_completo = true` (HTTP 403 if false)
  - Validate MIME type (JPEG/PNG only; HTTP 400 if not)
  - Validate all enums (HTTP 400 if invalid)
  - Normalize cidade/bairro
  - Save image temporarily, call Gemini, merge results, persist, promote image
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
- **RFC**: RF-022 through RF-025
- **Description**: Search available materials with matching algorithm
- **Query Params**: `disciplina, nivel_ensino, ano, sistema_ensino, cidade, bairro, page, limit`
- **Response**: Paginated array of Material entities (HTTP 200)
- **Rules**:
  - Filter by: status=DISPONIVEL, disciplina (exact), nivel_ensino (exact), year range, sistema_ensino (exact), city/neighborhood
  - Sort by: same bairro > same cidade > data_criacao DESC > id
  - SUPERIOR ignores year filter
  - OUTRO sistema_ensino matches ONLY OUTRO
- **HTTP Codes**: 200 OK, 400 Bad Request (invalid enum)

#### GET /materiais/{id}
- **Method**: GET
- **RFC**: RF-008
- **Description**: Get single material details
- **Response**: Material entity with doador info (HTTP 200)
- **HTTP Codes**: 200 OK, 404 Not Found

#### PATCH /materiais/{id}
- **Method**: PATCH
- **RFC**: RF-005 through RF-006
- **Description**: Update material status (e.g., DISPONIVEL → CANCELADO)
- **Request**: `{ status: CANCELADO }`
- **Response**: Updated Material entity (HTTP 200)
- **Rules**:
  - Validate state transition (HTTP 422 if invalid)
  - Only donor can modify own material
  - Canceling RESERVADO material cascades to related Solicitacao
- **HTTP Codes**: 200 OK, 403 Forbidden, 422 Unprocessable Entity

---

### 3. Request Management

#### POST /solicitacoes
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

#### PATCH /solicitacoes/{id}
- **Method**: PATCH
- **RFC**: RF-028 through RF-031, RF-032 through RF-035
- **Description**: Update request status (PENDENTE → APROVADA, RECUSADA, or CANCELADA)
- **Request**: `{ status: APROVADA|RECUSADA|CANCELADA|CONCLUIDA }`
- **Response**: Updated Solicitacao entity (HTTP 200)
- **Rules**:
  - Validate state transition (HTTP 422 if invalid)
  - Only donor can approve/decline; only appropriate party can cancel
  - Approval is atomic: update Solicitacao.status, Material.status = RESERVADO, send FCM, use database lock
  - Decline keeps Material DISPONIVEL
  - Cancel may cascade if Material was RESERVADO
  - Completion: Solicitacao → CONCLUIDA, Material → DOADO
- **HTTP Codes**: 200 OK, 403 Forbidden, 422 Unprocessable Entity

#### GET /solicitacoes
- **Method**: GET
- **RFC**: RF-026
- **Description**: List requests for current user (donor or student)
- **Query Params**: `status, page, limit`
- **Response**: Paginated array of Solicitacao entities (HTTP 200)
- **Rules**:
  - Return only requests where current user is donor or estudante
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

1. **User Selection**: Frontend user selects image from device gallery or camera
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
   - If SUCCESS: auto-filled fields, disabled edit, upload_id in form
   - If LOW_CONFIDENCE: auto-filled with warning, editable, upload_id in form
   - If FAILURE: empty fields, manual entry required, no upload_id
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
    "disciplina": { "value": "MATEMATICA|PORTUGUES|HISTORIA|GEOGRAFIA|CIENCIAS|LITERATURA", "confidence": 0.95 },
    "nivel_ensino": { "value": "FUNDAMENTAL|MEDIO|SUPERIOR", "confidence": 0.85 },
    "ano": { "value": 7, "confidence": 0.75 },
    "sistema_ensino": { "value": "ANGLO|OBJETIVO|COC|POSITIVO|OUTRO", "confidence": 0.90 },
    "estado_conservacao": { "value": "NOVO|BOM|USADO|DANIFICADO", "confidence": 0.88 }
  }
}

Confidence must be 0.0–1.0. If uncertain about any field, set low confidence or null. Never invent values.
```

---

## AI Confidence Fallback Rules

| Confidence Range | Behavior | UI Presentation | status_ia |
|---|---|---|---|
| ≥ 0.75 | Auto-fill all fields | Green checkmark, disabled edit | SUCCESS |
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

Rules:
- Only DISPONIVEL materials appear in search results
- RESERVADO → DOADO: only via Solicitacao APROVADA → CONCLUIDA
- RESERVADO → DISPONIVEL: automatic after 14 days expiry if not DOADO
- DOADO and CANCELADO are terminal states (no transitions out)
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
6. **User Authentication**: Separate authentication service (Firebase, JWT-based) handles login/token issuance; this spec assumes authenticated requests
7. **Reservation Expiry Job**: Background job runs daily to check and revert expired RESERVADO materials; failures are logged but don't block operations
8. **FCM Reliability**: Firebase Cloud Messaging failures are logged and retried; not guaranteed delivery (acceptable for non-critical notifications)
9. **WhatsApp Format**: All WhatsApp numbers are Brazilian (+55 prefix); international numbers not supported in MVP
10. **Enum Stability**: Enum values (disciplines, systems, conservation states) are unlikely to change during typical feature development cycle
11. **Material Ownership**: Donors own only their own materials; students can only request (no modification); Admin panel handles disputes
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

