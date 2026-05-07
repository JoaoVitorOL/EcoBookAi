# Data Model: EcoBook IA

**Phase**: 1-2 runtime baseline  
**Date**: 2026-05-05  
**Purpose**: Document the entity model that is currently implemented in the backend

---

## Relationship Overview

```text
Usuario (1) ----< Material (many)
Usuario (1) ----< Solicitacao (many)
Material (1) ---< Solicitacao (many)
Usuario (1) ----< UsuarioNecessidades (many enum values)
Material (0..1) - MaterialUploadTracking (many attempts over time)
```

---

## 1. Usuario

**Purpose**: Platform user for authentication, onboarding, donation, and request flows.

| Field | Type | Constraint | Notes |
|-------|------|-----------|-------|
| `id` | UUID | PK | Auto-generated |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL | Login identity |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt hash; raw password never stored |
| `nome` | VARCHAR(255) | NOT NULL | Required at persistence level |
| `whatsapp` | VARCHAR(20) | NULLABLE | Required only for completed profile |
| `cidade` | VARCHAR(100) | NULLABLE | Normalized before save |
| `bairro` | VARCHAR(100) | NULLABLE | Normalized before save |
| `instituicao` | VARCHAR(255) | NULLABLE | Optional |
| `perfil_completo` | BOOLEAN | NOT NULL, default false | Computed from required profile fields |
| `consentimento_ia` | BOOLEAN | NOT NULL, default false | Optional during onboarding; editable later |
| `role` | `role_enum` | NOT NULL, default `USER` | Authorization role |
| `criado_em` | TIMESTAMP | NOT NULL | Insert timestamp |
| `atualizado_em` | TIMESTAMP | NOT NULL | Update timestamp |

**Derived Rules**:
- `perfil_completo = true` only when `nome`, `whatsapp`, `cidade`, and `bairro` are all present and valid
- `consentimento_ia = false` does not block onboarding completion
- `consentimento_ia` can be updated later through profile editing
- Existing emails always conflict at register time

**Current Storage Rules**:
- Backend persists only `password_hash`
- Raw passwords are accepted only in register/login requests
- Android stores JWT and profile metadata locally, but not the password itself

---

## 2. UsuarioNecessidades

**Purpose**: Optional academic-need tags attached to a user profile.

| Field | Type | Constraint | Notes |
|-------|------|-----------|-------|
| `usuario_id` | UUID | FK -> `usuario.id` | Cascade delete |
| `necessidade` | `necessidade_academica_enum` | NOT NULL | Enum value |

**Allowed Enum Values**:
- `TEXTBOOKS`
- `WORKBOOKS`
- `REFERENCE_MATERIALS`
- `FICTION`
- `TECHNICAL_BOOKS`
- `TEST_PREP`

---

## 3. Material

**Purpose**: Educational material offered for donation.

| Field | Type | Constraint | Notes |
|-------|------|-----------|-------|
| `id` | UUID | PK | Auto-generated |
| `doador_id` | UUID | FK -> `usuario.id`, NOT NULL | Donor owner |
| `titulo` | VARCHAR(255) | NOT NULL | Editable even after AI suggestion |
| `autor` | VARCHAR(255) | NULLABLE | Optional; AI-assisted when visible or strongly grounded |
| `editora` | VARCHAR(255) | NULLABLE | Optional; AI-assisted when visible or strongly grounded |
| `descricao` | TEXT | NULLABLE | Manual-only field |
| `disciplina` | `disciplina_enum` | NOT NULL | Subject |
| `nivel_ensino` | `nivel_ensino_enum` | NOT NULL | Education level |
| `ano` | INTEGER | NULLABLE | Must be null for `SUPERIOR` |
| `sistema_ensino` | `sistema_ensino_enum` | NOT NULL | Curriculum system |
| `estado_conservacao` | `estado_conservacao_enum` | NOT NULL | Conservation state |
| `status` | `status_material_enum` | NOT NULL, default `DISPONIVEL` | Lifecycle |
| `imagem_url` | VARCHAR(500) | NULLABLE | Permanent image location |
| `upload_id` | VARCHAR(100) | NULLABLE | Upload tracking key |
| `cidade` | VARCHAR(100) | NOT NULL | Normalized |
| `bairro` | VARCHAR(100) | NOT NULL | Normalized |
| `data_publicacao` | INTEGER | NULLABLE | Publication year |
| `status_ia` | `status_ia_enum` | NULLABLE | AI attempt result |
| `confianca_ia` | DECIMAL(3,2) | NULLABLE | AI confidence |
| `criado_em` | TIMESTAMP | NOT NULL | Insert timestamp |
| `atualizado_em` | TIMESTAMP | NOT NULL | Update timestamp |

**Allowed `status_ia_enum` Values**:
- `SUCCESS`
- `LOW_CONFIDENCE`
- `FAILURE`
- `NOT_ATTEMPTED`

**Key Rules**:
- `descricao` is never auto-populated
- `estado_conservacao` is always selected manually by the donor
- `autor` and `editora` may be suggested by Gemini when visible on the material or strongly supported by search grounding
- `cidade` and `bairro` are normalized before persistence
- `consentimento_ia = false` must short-circuit Gemini usage

---

## 4. Solicitacao

**Purpose**: Request for a donated material.

| Field | Type | Constraint | Notes |
|-------|------|-----------|-------|
| `id` | UUID | PK | Auto-generated |
| `material_id` | UUID | FK -> `material.id`, NOT NULL | Requested material |
| `estudante_id` | UUID | FK -> `usuario.id`, NOT NULL | Requester |
| `status` | `status_solicitacao_enum` | NOT NULL, default `PENDENTE` | Request lifecycle |
| `contato_doador` | JSONB | NULLABLE | Filled when approved |
| `criado_em` | TIMESTAMP | NOT NULL | Insert timestamp |
| `atualizado_em` | TIMESTAMP | NOT NULL | Update timestamp |
| `aprovado_em` | TIMESTAMP | NULLABLE | Approval timestamp |
| `expires_at` | TIMESTAMP | NULLABLE | Reservation expiry |

**Allowed Status Values**:
- `PENDENTE`
- `APROVADA`
- `RECUSADA`
- `CANCELADA`
- `CONCLUIDA`

---

## 5. MaterialUploadTracking

**Purpose**: Track upload attempts and AI-processing lifecycle by `upload_id`.

| Field | Type | Constraint | Notes |
|-------|------|-----------|-------|
| `id` | UUID | PK | Auto-generated |
| `upload_id` | VARCHAR(100) | UNIQUE, NOT NULL | Tracking identifier |
| `material_id` | UUID | FK -> `material.id` | Nullable until final material exists |
| `status` | VARCHAR(50) | NOT NULL | Upload processing status |
| `usuario_id` | UUID | FK -> `usuario.id` | Owner of the temporary upload |
| `file_path` | VARCHAR(1000) | NULLABLE | Filesystem path for temp/permanent image |
| `mime_type` | VARCHAR(100) | NULLABLE | Validated MIME type |
| `file_size` | BIGINT | NULLABLE | Image size in bytes |
| `expires_at` | TIMESTAMP | NULLABLE | Cleanup deadline for unused uploads |
| `status_ia` | ENUM | NULLABLE | SUCCESS / LOW_CONFIDENCE / FAILURE / NOT_ATTEMPTED |
| `confianca_ia` | DECIMAL(3,2) | NULLABLE | Highest recorded confidence for audit |
| `criado_em` | TIMESTAMP | NOT NULL | Insert timestamp |

Retention note:
- After successful material creation, the tracking row is retained and linked to `material_id`; only expired unused uploads are deleted by the cleanup job.

---

## Implemented Invariants

- Current auth model is `email + password + JWT`
- There is no persisted `google_id` in the final runtime schema
- There is no soft-delete column in the current runtime schema
- Current timestamp columns in persisted tables use Portuguese naming:
  - `criado_em`
  - `atualizado_em`
  - `aprovado_em`
- `usuario.whatsapp`, `usuario.cidade`, and `usuario.bairro` are nullable in storage because onboarding completes after account creation

---

## Current DDL Snapshot

For the exact SQL snapshot that matches the runtime backend schema, see:

- [database-schema.sql](</c:/Users/jvol2/OneDrive/Área de Trabalho/projIntegrador/EcoBookAi/specs/001-ecobook-core/database-schema.sql>)
