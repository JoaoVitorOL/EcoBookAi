# Data Model: EcoBook IA

**Phase**: 1 (Design & Contracts)  
**Date**: 2026-04-15  
**Purpose**: Complete entity definitions, relationships, validation rules, and PostgreSQL schema

---

## Entity Relationship Diagram

```
┌─────────────┐         ┌──────────────┐         ┌─────────────────┐
│   Usuario   │         │   Material   │         │  Solicitacao    │
├─────────────┤         ├──────────────┤         ├─────────────────┤
│ id (UUID)   │◄────────│ id (UUID)    │──────►  │ id (UUID)       │
│ email       │ 1:N     │ doador_id    │ 1:N     │ material_id     │
│ nome        │ (donor) │ titulo       │ (many)  │ estudante_id    │
│ whatsapp    │         │ disciplina   │         │ status          │
│ cidade      │         │ nivel_ensino │         │ contato_doador  │
│ bairro      │         │ ano          │         │ created_at      │
│ perfil      │         │ sistema_     │         │ approved_at     │
│ _completo   │         │ ensino       │         │ expires_at      │
│ consentimento│        │ estado_cons. │         │ updated_at      │
│ _ia         │         │ status       │         └─────────────────┘
│ role        │         │ imagem_url   │                  ▲
│ created_at  │         │ upload_id    │                  │
│ updated_at  │         │ cidade       │            (max 1 APROVADA
└─────────────┘         │ bairro       │             per Material)
       ▲                │ data_criacao │
       │                │ created_at   │
       │                │ updated_at   │
       │                └──────────────┘
       │
 (many requests
  per student)
```

---

## Entity Definitions

### 1. Usuario (User)

**Purpose**: Represents a platform user who acts as both donor and recipient (no separation).

| Field | Type | Constraint | Validation | Notes |
|-------|------|-----------|-----------|-------|
| `id` | UUID | PRIMARY KEY | Not null | Auto-generated |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL | Valid email format | Used for Google OAuth2 |
| `nome` | VARCHAR(255) | NOT NULL | 1–255 chars | User's full name |
| `whatsapp` | VARCHAR(20) | NOT NULL | E.164 format (e.g., +5548999999999) | Donor contact method |
| `cidade` | VARCHAR(100) | NOT NULL | Normalized (uppercase, no accents) | Geographic location |
| `bairro` | VARCHAR(100) | NOT NULL | Normalized (uppercase, no accents) | Geographic location |
| `instituicao` | VARCHAR(255) | NULLABLE | 0–255 chars | School/organization affiliation |
| `perfil_completo` | BOOLEAN | DEFAULT false | Not null | Profile completion flag |
| `consentimento_ia` | BOOLEAN | DEFAULT false | Not null | Consent for Gemini API usage |
| `role` | ENUM('DOADOR', 'ESTUDANTE', 'AMBOS') | DEFAULT 'AMBOS' | Not null | User type (currently all users are AMBOS) |
| `google_id` | VARCHAR(255) | NULLABLE | Unique per user | OAuth2 Google ID for authentication |
| `created_at` | TIMESTAMP | DEFAULT now() | Not null | Creation timestamp |
| `updated_at` | TIMESTAMP | DEFAULT now() | Not null | Last modification timestamp |

**Indexes**:
```sql
CREATE UNIQUE INDEX idx_usuario_email ON usuario(email);
CREATE INDEX idx_usuario_google_id ON usuario(google_id);
CREATE INDEX idx_usuario_perfil_completo ON usuario(perfil_completo);
```

**Validation Rules**:
- Email must be RFC 5322 compliant
- WhatsApp must match E.164 format (regex: `^\+\d{1,15}$`)
- `cidade` and `bairro` normalized via GeoNormalizationService before insert/update
- `perfil_completo` = true if email, nome, whatsapp, cidade, bairro all non-null and non-empty
- `consentimento_ia` = false by default; user must explicitly opt-in on first upload

**State Transitions**:
- Profile: incomplete → complete (one-way transition, can remain incomplete indefinitely)
- Consent: false → true (user can toggle on/off in settings)

---

### 2. Material (Donated Material)

**Purpose**: Represents a donated educational resource (textbook, workbook, etc.).

| Field | Type | Constraint | Validation | Notes |
|-------|------|-----------|-----------|-------|
| `id` | UUID | PRIMARY KEY | Not null | Auto-generated |
| `doador_id` | UUID | FOREIGN KEY (usuario.id) | Not null | Donor who listed material |
| `titulo` | VARCHAR(255) | NOT NULL | 1–255 chars | Material title |
| `descricao` | TEXT | NULLABLE | 0–2000 chars | Detailed description |
| `disciplina` | ENUM | NOT NULL | See enum list below | Subject/discipline |
| `nivel_ensino` | ENUM | NOT NULL | FUNDAMENTAL, MEDIO, SUPERIOR | Education level |
| `ano` | INTEGER | NULLABLE | 1–12 or NULL for SUPERIOR | Grade/year; NULL for SUPERIOR |
| `sistema_ensino` | ENUM | NOT NULL | ANGLO, OBJETIVO, COC, POSITIVO, OUTRO | Curriculum system |
| `estado_conservacao` | ENUM | NOT NULL | NOVO, BOM, USADO, DANIFICADO | Conservation state |
| `status` | ENUM | DEFAULT 'DISPONIVEL' | DISPONIVEL, RESERVADO, DOADO, CANCELADO | Lifecycle state |
| `imagem_url` | VARCHAR(500) | NULLABLE | Valid file path or URL | Permanent storage location (local: /uploads/u{hash}/m{hash}.jpg) |
| `upload_id` | VARCHAR(100) | NULLABLE | Unique identifier | Temporary upload tracking (for audit, deduplication) |
| `cidade` | VARCHAR(100) | NOT NULL | Normalized | Geographic location (normalized at insert/update) |
| `bairro` | VARCHAR(100) | NOT NULL | Normalized | Neighborhood (normalized at insert/update) |
| `data_criacao` | DATE | NOT NULL | Valid date | When material was initially offered |
| `created_at` | TIMESTAMP | DEFAULT now() | Not null | DB insert timestamp |
| `updated_at` | TIMESTAMP | DEFAULT now() | Not null | DB update timestamp |

**Indexes**:
```sql
CREATE INDEX idx_material_status ON material(status);
CREATE INDEX idx_material_status_disciplina ON material(status, disciplina);
CREATE INDEX idx_material_status_nivel_ensino ON material(status, nivel_ensino);
CREATE INDEX idx_material_sistema_ensino ON material(sistema_ensino);
CREATE INDEX idx_material_cidade_bairro ON material(cidade, bairro);
CREATE INDEX idx_material_data_criacao DESC ON material(data_criacao DESC);
CREATE INDEX idx_material_doador_id ON material(doador_id);
```

**Validation Rules**:
- `doador_id` must reference valid usuario with `role` in (DOADOR, AMBOS)
- `disciplina`, `nivel_ensino`, `sistema_ensino`, `estado_conservacao` must be exact enum values (HTTP 400 if not)
- `ano` must be in [1, 12] if `nivel_ensino` in (FUNDAMENTAL, MEDIO); NULL if SUPERIOR
- `cidade` and `bairro` normalized via GeoNormalizationService before insert
- `status` can only transition via PATCH /materiais/{id} with atomic locking
- `imagem_url` must be non-null if material created successfully (set during image promotion)

**State Transitions** (enforced via HTTP 422):
```
DISPONIVEL ──┐
    ↓ (request received, donor approves)
RESERVADO ──┐
    ├──(14 days pass)→ DISPONIVEL (auto-revert)
    └──(donor marks complete)→ DOADO [FINAL]

DISPONIVEL ──┐
    └──(donor cancels)→ CANCELADO [FINAL]

RESERVADO ──┐
    └──(donor cancels)→ CANCELADO [FINAL]
```

**Invariants**:
- If status = RESERVADO, must have exactly ONE solicitacao with status = APROVADA
- If status = DOADO, must have exactly ONE solicitacao with status = CONCLUIDA
- If status = DISPONIVEL, must have ZERO solicitacoes with status = APROVADA
- status transitions from DOADO or CANCELADO are forbidden (HTTP 422)

---

### 3. Solicitacao (Request/Solicitation)

**Purpose**: Represents a student's request for a material; lifecycle mirrors donation transaction.

| Field | Type | Constraint | Validation | Notes |
|-------|------|-----------|-----------|-------|
| `id` | UUID | PRIMARY KEY | Not null | Auto-generated |
| `material_id` | UUID | FOREIGN KEY (material.id) | Not null | Material being requested |
| `estudante_id` | UUID | FOREIGN KEY (usuario.id) | Not null | Student making request |
| `status` | ENUM | DEFAULT 'PENDENTE' | PENDENTE, APROVADA, RECUSADA, CANCELADA, CONCLUIDA | Lifecycle state |
| `contato_doador` | JSONB | NULLABLE | { "nome": "...", "whatsapp": "+55..." } | Donor contact; populated ONLY when status = APROVADA |
| `created_at` | TIMESTAMP | DEFAULT now() | Not null | Request creation timestamp |
| `updated_at` | TIMESTAMP | DEFAULT now() | Not null | Last status change timestamp |
| `approved_at` | TIMESTAMP | NULLABLE | Valid timestamp | When donor approved (used for expiry calculation) |
| `expires_at` | TIMESTAMP | NULLABLE | approved_at + 14 days | Reservation expiry; triggers auto-cancel if not CONCLUIDA |

**Indexes**:
```sql
CREATE INDEX idx_solicitacao_material_id ON solicitacao(material_id);
CREATE INDEX idx_solicitacao_estudante_id ON solicitacao(estudante_id);
CREATE INDEX idx_solicitacao_status ON solicitacao(status);
CREATE INDEX idx_solicitacao_expires_at ON solicitacao(expires_at);
```

**Validation Rules**:
- `material_id` must reference valid material
- `estudante_id` must reference valid usuario
- `status` must be exact enum value (HTTP 400 if not)
- `contato_doador` is NULL unless status = APROVADA (set during approval)
- `approved_at` is NULL unless status = APROVADA
- `expires_at` = approved_at + 14 days (set at approval; used for auto-revert job)
- Only ONE solicitacao per (material_id, estudante_id) can be in status != (RECUSADA, CANCELADA, CONCLUIDA)
  - i.e., if existing PENDENTE exists and new request submitted → HTTP 409 Conflict

**State Transitions** (enforced via HTTP 422):
```
PENDENTE ──┐
    ├──(donor approves)→ APROVADA ──┐
    │                                  ├──(donor marks complete)→ CONCLUIDA [FINAL]
    │                                  └──(either party cancels or 14 days pass)→ CANCELADA [FINAL]
    │
    ├──(donor declines)→ RECUSADA [FINAL]
    │
    └──(student cancels before approval)→ CANCELADA [FINAL]
```

---

### 4. Enums

**Disciplina** (Subject/Discipline):
```java
ENUM: MATEMATICA, PORTUGUES, HISTORIA, GEOGRAFIA, CIENCIAS, LITERATURA
```

**NivelEnsino** (Education Level):
```java
ENUM: FUNDAMENTAL, MEDIO, SUPERIOR
```

**SistemaEnsino** (Curriculum System):
```java
ENUM: ANGLO, OBJETIVO, COC, POSITIVO, OUTRO
```

**EstadoConservacao** (Conservation State):
```java
ENUM: NOVO, BOM, USADO, DANIFICADO
```

**StatusMaterial** (Material Lifecycle State):
```java
ENUM: DISPONIVEL, RESERVADO, DOADO, CANCELADO
```

**StatusSolicitacao** (Request Lifecycle State):
```java
ENUM: PENDENTE, APROVADA, RECUSADA, CANCELADA, CONCLUIDA
```

**StatusRespostaIA** (AI Response Status):
```java
ENUM: SUCCESS, LOW_CONFIDENCE, FAILURE
```

---

## PostgreSQL DDL

```sql
-- Create enum types
CREATE TYPE disciplina_enum AS ENUM (
    'MATEMATICA', 'PORTUGUES', 'HISTORIA', 'GEOGRAFIA', 'CIENCIAS', 'LITERATURA'
);

CREATE TYPE nivel_ensino_enum AS ENUM (
    'FUNDAMENTAL', 'MEDIO', 'SUPERIOR'
);

CREATE TYPE sistema_ensino_enum AS ENUM (
    'ANGLO', 'OBJETIVO', 'COC', 'POSITIVO', 'OUTRO'
);

CREATE TYPE estado_conservacao_enum AS ENUM (
    'NOVO', 'BOM', 'USADO', 'DANIFICADO'
);

CREATE TYPE status_material_enum AS ENUM (
    'DISPONIVEL', 'RESERVADO', 'DOADO', 'CANCELADO'
);

CREATE TYPE status_solicitacao_enum AS ENUM (
    'PENDENTE', 'APROVADA', 'RECUSADA', 'CANCELADA', 'CONCLUIDA'
);

CREATE TYPE user_role_enum AS ENUM (
    'DOADOR', 'ESTUDANTE', 'AMBOS'
);

-- Usuario table
CREATE TABLE usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    nome VARCHAR(255) NOT NULL,
    whatsapp VARCHAR(20) NOT NULL CHECK (whatsapp ~ '^\+\d{1,15}$'),
    cidade VARCHAR(100) NOT NULL,
    bairro VARCHAR(100) NOT NULL,
    instituicao VARCHAR(255),
    perfil_completo BOOLEAN DEFAULT false NOT NULL,
    consentimento_ia BOOLEAN DEFAULT false NOT NULL,
    role user_role_enum DEFAULT 'AMBOS' NOT NULL,
    google_id VARCHAR(255) UNIQUE,
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    updated_at TIMESTAMP DEFAULT now() NOT NULL
);

CREATE UNIQUE INDEX idx_usuario_email ON usuario(email);
CREATE INDEX idx_usuario_google_id ON usuario(google_id);
CREATE INDEX idx_usuario_perfil_completo ON usuario(perfil_completo);

-- Material table
CREATE TABLE material (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doador_id UUID NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT,
    disciplina disciplina_enum NOT NULL,
    nivel_ensino nivel_ensino_enum NOT NULL,
    ano INTEGER CHECK (ano IS NULL OR (ano >= 1 AND ano <= 12)),
    sistema_ensino sistema_ensino_enum NOT NULL,
    estado_conservacao estado_conservacao_enum NOT NULL,
    status status_material_enum DEFAULT 'DISPONIVEL' NOT NULL,
    imagem_url VARCHAR(500),
    upload_id VARCHAR(100),
    cidade VARCHAR(100) NOT NULL,
    bairro VARCHAR(100) NOT NULL,
    data_criacao DATE NOT NULL,
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    updated_at TIMESTAMP DEFAULT now() NOT NULL,
    
    -- Constraint: SUPERIOR level must have NULL ano
    CONSTRAINT check_superior_ano CHECK (
        (nivel_ensino = 'SUPERIOR' AND ano IS NULL) OR
        (nivel_ensino IN ('FUNDAMENTAL', 'MEDIO') AND ano IS NOT NULL)
    )
);

CREATE INDEX idx_material_status ON material(status);
CREATE INDEX idx_material_status_disciplina ON material(status, disciplina);
CREATE INDEX idx_material_status_nivel_ensino ON material(status, nivel_ensino);
CREATE INDEX idx_material_sistema_ensino ON material(sistema_ensino);
CREATE INDEX idx_material_cidade_bairro ON material(cidade, bairro);
CREATE INDEX idx_material_data_criacao ON material(data_criacao DESC);
CREATE INDEX idx_material_doador_id ON material(doador_id);

-- Solicitacao table
CREATE TABLE solicitacao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    material_id UUID NOT NULL REFERENCES material(id) ON DELETE CASCADE,
    estudante_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    status status_solicitacao_enum DEFAULT 'PENDENTE' NOT NULL,
    contato_doador JSONB,  -- { "nome": "...", "whatsapp": "+55..." }
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    updated_at TIMESTAMP DEFAULT now() NOT NULL,
    approved_at TIMESTAMP,
    expires_at TIMESTAMP,
    
    -- Constraint: contato_doador must be non-null only when status = APROVADA
    CONSTRAINT check_contato_doador CHECK (
        (status = 'APROVADA' AND contato_doador IS NOT NULL) OR
        (status != 'APROVADA' AND contato_doador IS NULL)
    ),
    
    -- Constraint: only one non-terminal solicitacao per material
    UNIQUE (material_id, estudante_id)
);

CREATE INDEX idx_solicitacao_material_id ON solicitacao(material_id);
CREATE INDEX idx_solicitacao_estudante_id ON solicitacao(estudante_id);
CREATE INDEX idx_solicitacao_status ON solicitacao(status);
CREATE INDEX idx_solicitacao_expires_at ON solicitacao(expires_at);

-- Audit table (optional, for LGPD compliance / deletion tracking)
CREATE TABLE auditlog (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    evento VARCHAR(100) NOT NULL,  -- 'usuario_criado', 'material_criado', 'solicitacao_aprovada', 'usuario_deletado', etc.
    usuario_id UUID REFERENCES usuario(id) ON DELETE SET NULL,
    tabela VARCHAR(50),
    registro_id UUID,
    dados_antes JSONB,
    dados_depois JSONB,
    created_at TIMESTAMP DEFAULT now() NOT NULL
);

CREATE INDEX idx_auditlog_usuario_id ON auditlog(usuario_id);
CREATE INDEX idx_auditlog_evento ON auditlog(evento);
CREATE INDEX idx_auditlog_created_at ON auditlog(created_at);
```

---

## Validation & Data Consistency Rules

### Profile Completeness

**Incomplete Profile** (perfil_completo = false):
- Missing any of: email, nome, whatsapp, cidade, bairro
- Blocks: POST /materiais (HTTP 403), POST /solicitacoes (HTTP 403)
- Allowed: GET operations, profile update (PATCH /usuarios/{id})

**Complete Profile** (perfil_completo = true):
- All required fields: email, nome, whatsapp (E.164), cidade (normalized), bairro (normalized)
- Allows: All POST/PATCH operations

### Geographic Normalization

**Algorithm**: 
1. Uppercase: `input.toUpperCase()`
2. NFD Decomposition: `Normalizer.normalize(input, Form.NFD)`
3. ASCII-only: `input.replaceAll("[^\\p{ASCII}]", "")`
4. Trim: `input.trim()`

**Examples**:
```
"são joão" → "SAO JOAO"
"São João" → "SAO JOAO"
"criciúma" → "CRICIUMA"
"Florianópolis" → "FLORIANOPOLIS"
"centro-left" → "CENTRO-LEFT"  (hyphens preserved)
```

### Enum Validation

**On Insert/Update**: 
- Validate exact enum value (no fuzzy matching)
- Invalid enum → HTTP 400 with error: `{"error": "INVALID_ENUM", "field": "disciplina", "allowed_values": ["MATEMATICA", ...], "received_value": "MATH"}`

**Storage**:
- PostgreSQL enum types enforce values at database level
- ORM (Hibernate/Spring Data JPA) provides type-safe enum handling

### State Transition Validation

**Material Transitions** (only valid paths allowed):
```
DISPONIVEL → RESERVADO (via approved solicitacao)
DISPONIVEL → CANCELADO (via donor action)
RESERVADO → DOADO (via completed solicitacao)
RESERVADO → DISPONIVEL (auto-revert after 14 days)
RESERVADO → CANCELADO (via donor or auto-cancel)
```
**Invalid transition** → HTTP 422 Unprocessable Entity

**Solicitacao Transitions** (only valid paths allowed):
```
PENDENTE → APROVADA (via donor approval)
PENDENTE → RECUSADA (via donor decline)
PENDENTE → CANCELADA (via student cancel before approval)
APROVADA → CONCLUIDA (via donation completion)
APROVADA → CANCELADA (via auto-expiry or manual cancel)
```
**Invalid transition** → HTTP 422 Unprocessable Entity

### Locking Strategy

**Approval Transaction** (atomic with lock):
```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public void approveRequest(UUID solicitacaoId) {
    Material material = materialRepository.findByIdForUpdate(solicitacaoId);  // SELECT...FOR UPDATE
    Solicitacao solicitacao = solicitacaoRepository.findByIdForUpdate(solicitacaoId);  // SELECT...FOR UPDATE
    
    // Validate invariant: no other APROVADA solicitacao
    long aprovadaCount = solicitacaoRepository.countByMaterialIdAndStatus(material.getId(), "APROVADA");
    if (aprovadaCount > 0) throw new ConflictException("Material already has approved request");
    
    // Update atomically
    material.setStatus(StatusMaterial.RESERVADO);
    solicitacao.setStatus(StatusSolicitacao.APROVADA);
    solicitacao.setApprovedAt(Instant.now());
    solicitacao.setExpiresAt(Instant.now().plus(14, ChronoUnit.DAYS));
    solicitacao.setContatoDoador(new ContatoDoador(donor.getNome(), donor.getWhatsapp()));
    
    materialRepository.save(material);
    solicitacaoRepository.save(solicitacao);
    
    fcmService.sendNotification(...);  // Async, after transaction commits
}
```

---

## Database Performance Considerations

### Query Optimization

**Material Search** (most frequently called):
```sql
SELECT m.* FROM material m
WHERE m.status = 'DISPONIVEL'
  AND m.disciplina = ?
  AND m.nivel_ensino = ?
  AND (m.nivel_ensino = 'SUPERIOR' OR ABS(m.ano - ?) <= 1)
  AND (m.sistema_ensino = ? OR ? = 'OUTRO')
  AND m.cidade = ?
ORDER BY
  CASE WHEN m.bairro = ? THEN 0 ELSE 1 END,
  m.data_criacao DESC,
  m.id
LIMIT 20;
```
**Index**: `idx_material_status_disciplina`, `idx_material_status_nivel_ensino`, `idx_material_cidade_bairro`  
**Expected Plan**: Index scan + sort + limit → <100ms for 500 materials

### Connection Pooling

**HikariCP Configuration** (Spring Boot):
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Slow Query Monitoring

```sql
-- PostgreSQL log_statement config
log_statement = 'all'
log_min_duration_statement = 1000  -- Log queries slower than 1 second
```

---

## Migration Strategy (Future)

### Schema Evolution

**Pattern for database migrations** (using Flyway or Liquibase):
```
db/migration/
├── V1__initial_schema.sql        # DDL above
├── V2__add_audit_table.sql       # Future: audit log
├── V3__add_materialized_view.sql # Future: performance optimization
└── ...
```

**Zero-Downtime Deployments**:
1. Add column with DEFAULT (backward-compatible)
2. Deploy code that reads/writes new column
3. Backfill existing rows
4. Remove DEFAULT
5. Remove old column (if applicable)

---

## Testing Checklist

- [ ] Unit tests for GeoNormalizationService (all accent/case variations)
- [ ] Unit tests for MatchingService (verify 6-step algorithm with sample materials)
- [ ] Integration tests for state transitions (verify invariants, lock behavior)
- [ ] Concurrency tests (two simultaneous approvals for same material)
- [ ] Database constraint tests (enum validation, year constraints)
- [ ] Performance tests (query latency with 500+ materials)

