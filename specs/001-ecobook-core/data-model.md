# Data Model: EcoBook IA

**Phase**: 1-7 runtime  
**Date**: 2026-05-21  
**Purpose**: Document the entity model currently implemented across the backend and the persisted notification stack

---

## Source Of Truth

The runtime schema is defined by:

- JPA mappings in `EcoBookAiBackend/src/main/java/com/ecobook/model/`
- Flyway migrations `V1` through `V15`

This document is the current human-readable summary of that runtime model.

---

## Relationship Overview

```text
Usuario (1) ----< Material (many)
Usuario (1) ----< Solicitacao (many)
Material (1) ---< Solicitacao (many)
Usuario (1) ----< UsuarioNecessidades (many enum values)
Usuario (1) ----< TemporaryUpload (many attempts over time)
Material (0..1) - TemporaryUpload (one promoted upload record)
Usuario (1) ----< UserNotification (many)
Usuario (1) ----< FailedNotification (many)
Material (1) ---< MaterialNonReceiptReport (many)
Solicitacao (1) - MaterialNonReceiptReport (many)
Usuario (1) ----< MaterialNonReceiptReport (many as reporting student)
```

---

## 1. Usuario

Platform user for auth, onboarding, donation, request, and notification token sync.

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | Primary key |
| `email` | VARCHAR(255) | Unique login identity |
| `password_hash` | VARCHAR(255) | Backend-only password storage |
| `nome` | VARCHAR(255) | Required at persistence level |
| `whatsapp` | VARCHAR(20) | Nullable until onboarding finishes |
| `cidade` | VARCHAR(100) | Normalized before save |
| `bairro` | VARCHAR(100) | Normalized before save |
| `instituicao` | VARCHAR(255) | Optional |
| `fcm_token` | VARCHAR(512) | Optional current device token |
| `perfil_completo` | BOOLEAN | Derived from profile fields |
| `consentimento_ia` | BOOLEAN | Controls Gemini usage |
| `role` | `role_enum` | `USER` or `ADMIN` |
| `criado_em` | TIMESTAMP | Creation timestamp |
| `atualizado_em` | TIMESTAMP | Update timestamp |

Supporting collection:

- `usuario_necessidades(usuario_id, necessidade)` with `necessidade_academica_enum`

---

## 2. Material

Educational material offered by a donor.

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | Primary key |
| `doador_id` | UUID | FK to `usuario` |
| `titulo` | VARCHAR(255) | Required |
| `autor` | VARCHAR(255) | Optional |
| `editora` | VARCHAR(255) | Optional |
| `descricao` | TEXT | Manual field |
| `disciplina` | `disciplina_enum` | Includes `TODAS` for multi-discipline material |
| `nivel_ensino` | `nivel_ensino_enum` | `FUNDAMENTAL`, `MEDIO`, `SUPERIOR` |
| `ano` | INTEGER | Null for `SUPERIOR` |
| `sistema_ensino` | `sistema_ensino_enum` | Current runtime list includes `ANGLO`, `OBJETIVO`, `COC`, `POSITIVO`, `POLIEDRO`, `ETAPA`, `BERNOULLI`, `SAS`, `FTD`, `OUTRO` |
| `estado_conservacao` | `estado_conservacao_enum` | Manual state |
| `status` | `status_material_enum` | `DISPONIVEL`, `RESERVADO`, `DOADO`, `CANCELADO` |
| `imagem_url` | VARCHAR(500) | Promoted front cover path |
| `imagem_verso_url` | VARCHAR(500) | Optional promoted back cover path |
| `upload_id` | VARCHAR(100) | Tracking key from preview stage |
| `cidade` | VARCHAR(100) | Inherited from donor profile |
| `bairro` | VARCHAR(100) | Inherited from donor profile |
| `data_publicacao` | INTEGER | Optional publication year |
| `status_ia` | `status_ia_enum` | `SUCCESS`, `LOW_CONFIDENCE`, `FAILURE`, `NOT_ATTEMPTED` |
| `confianca_ia` | DECIMAL(3,2) | Optional AI confidence |
| `doado_em` | TIMESTAMP | Filled when donation completes |
| `criado_em` | TIMESTAMP | Creation timestamp |
| `atualizado_em` | TIMESTAMP | Update timestamp |

---

## 3. Solicitacao

Student request for a material.

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | Primary key |
| `material_id` | UUID | FK to `material` |
| `estudante_id` | UUID | FK to `usuario` |
| `status` | `status_solicitacao_enum` | `PENDENTE`, `APROVADA`, `RECUSADA`, `CANCELADA`, `CONCLUIDA` |
| `contato_doador` | JSON/JSONB | Populated only after approval |
| `aprovado_em` | TIMESTAMP | Approval timestamp |
| `expires_at` | TIMESTAMP | Reservation expiry timestamp |
| `concluido_em` | TIMESTAMP | Donation completion timestamp |
| `criado_em` | TIMESTAMP | Creation timestamp |
| `atualizado_em` | TIMESTAMP | Update timestamp |

Implemented invariants:

- only one approved request per material
- donor contact is hidden until approval
- approved reservations can expire back to `CANCELADA` + material `DISPONIVEL`

---

## 4. TemporaryUpload

Tracks staged upload files and AI processing lifecycle before final material creation.

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | Primary key |
| `upload_id` | VARCHAR(100) | Unique external tracking key |
| `material_id` | UUID | Optional link after promotion |
| `status` | VARCHAR(50) | Upload lifecycle status |
| `usuario_id` | UUID | Owner of the staged upload |
| `file_path` | VARCHAR(1000) | Front cover path |
| `secondary_file_path` | VARCHAR(1000) | Optional back cover path |
| `mime_type` | VARCHAR(100) | Front MIME |
| `secondary_mime_type` | VARCHAR(100) | Back MIME |
| `file_size` | BIGINT | Front size |
| `secondary_file_size` | BIGINT | Back size |
| `expires_at` | TIMESTAMP | Cleanup deadline for unused uploads |
| `status_ia` | `status_ia_enum` | Preview result status |
| `confianca_ia` | DECIMAL(3,2) | Highest confidence captured |
| `criado_em` | TIMESTAMP | Creation timestamp |

---

## 5. UserNotification

Persisted inbox row shown inside the Android notifications center.

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | Primary key |
| `user_id` | UUID | Notification recipient |
| `notification_id` | VARCHAR(100) | Stable payload identifier |
| `notification_type` | VARCHAR(64) | Runtime notification type |
| `title` | VARCHAR(160) | UI title |
| `body` | VARCHAR(512) | UI body |
| `route` | VARCHAR(120) | Android navigation route |
| `request_id` | UUID | Optional request reference |
| `material_id` | UUID | Optional material reference |
| `payload_data` | JSONB | Flattened metadata map |
| `created_at` | TIMESTAMP | Receipt timestamp |
| `updated_at` | TIMESTAMP | Update timestamp |
| `read_at` | TIMESTAMP | Null while unread |

---

## 6. FailedNotification

Persistent retry queue for transient FCM delivery failures.

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | Primary key |
| `user_id` | UUID | Target user |
| `notification_type` | VARCHAR(64) | Runtime type |
| `title` | VARCHAR(160) | Replayed title |
| `body` | VARCHAR(512) | Replayed body |
| `payload_data` | JSONB | Replayed metadata |
| `retry_count` | INTEGER | Max runtime retry count = `3` |
| `created_at` | TIMESTAMP | Queue insertion |
| `updated_at` | TIMESTAMP | Last row update |
| `next_attempt_at` | TIMESTAMP | Next retry schedule |
| `last_attempt_at` | TIMESTAMP | Last retry execution |
| `delivered_at` | TIMESTAMP | Filled on eventual success |
| `permanently_failed_at` | TIMESTAMP | Filled after final failure |
| `last_error` | VARCHAR(1000) | Last delivery error |

---

## 7. MaterialNonReceiptReport

Moderation seed entity created when a completed student reports that a donated material never arrived.

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | Primary key |
| `material_id` | UUID | FK to the donated `material` |
| `solicitacao_id` | UUID | FK to the completed `solicitacao` |
| `estudante_id` | UUID | FK to the reporting student |
| `reason` | VARCHAR(500) | Optional student text |
| `status` | VARCHAR(32) | Current runtime values: `OPEN`, `RESOLVED` |
| `created_at` | TIMESTAMP | Creation timestamp |
| `updated_at` | TIMESTAMP | Update timestamp |
| `resolved_at` | TIMESTAMP | Filled by future admin workflow |
| `resolution_notes` | VARCHAR(1000) | Reserved for future moderation notes |

Implemented invariants:

- only students with a `CONCLUIDA` request can create the report
- the related material must already be `DOADO`
- only one `OPEN` report is allowed per completed request

---

## Runtime Notes

- the `local` profile now maps PostgreSQL named enums to H2-compatible domains for local bootstrapping
- `database-schema.sql` should be treated as a convenience snapshot only; when any conflict exists, the JPA mappings plus Flyway migrations win
- the non-receipt report table and the admin resolution flow are both live in runtime, including `PATCH /api/v1/admin/reports/{id}/resolve`
