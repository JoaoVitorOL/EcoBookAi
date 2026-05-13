# Material API Contracts

**Reference**: spec.md RF-005 through RF-025, RF-044  
**Version**: 1.3  
**Date**: 2026-05-12  
**Status**: Current runtime contract for Phase 3 create/preview flow plus Phase 4 search; detail/update sections remain target-state docs for later phases

---

> Implementation note: the current backend already exposes working runtime endpoints for `/api/v1/materiais/preview`, `/api/v1/materiais` and `GET /api/v1/materiais`. The `GET /materiais/{id}` and mutation sections below still describe later phases.
> Successful runtime responses are wrapped in `{ status, message, timestamp, path, data }`; the JSON examples below focus on the inner `data` payload unless stated otherwise.

## POST /materiais

Create a new material from a previously generated `upload_id`. In the current Phase 3 runtime the image is uploaded in `POST /materiais/preview`, stored temporarily, and then reused during the final creation call below.

### Request

Current runtime override:
- Phase 3 currently accepts `application/json` with `upload_id` plus the reviewed metadata fields.
- The image itself is uploaded only in `POST /materiais/preview` and is reused from temporary storage here.
- `cidade` and `bairro` are inherited from the authenticated donor profile instead of being resent by the Android app.
- The illustrative body below still contains historical fields from the earlier target contract; when integrating with the current backend, send only the JSON fields accepted by `CreateMaterialRequestDTO`.

```http
POST /api/v1/materiais
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "titulo": "Geometria Plana 7º Ano",
  "descricao": "Livro em bom estado, com marcações de uso, base do currículo Anglo 7º ano",
  "disciplina": "MATEMATICA",
  "nivel_ensino": "FUNDAMENTAL",
  "ano": 7,
  "sistema_ensino": "ANGLO",
  "estado_conservacao": "BOM",
  "cidade": "florianópolis",
  "bairro": "centro",
  "data_publicacao": 2010,
  "upload_id": "temp-upload-uuid-abc123def",
  "image": <binary_file>
}
```

**Validation Rules**:
- `titulo`: 1-255 characters, required, AI-preenchível
- `descricao`: 10-2000 characters, required, **manual-only field** (never auto-populated by system)
- `disciplina`: MATEMATICA | PORTUGUES | HISTORIA | GEOGRAFIA | CIENCIAS | LITERATURA
- `nivel_ensino`: FUNDAMENTAL | MEDIO | SUPERIOR
- `ano`: 1-12 for FUNDAMENTAL/MEDIO; null for SUPERIOR
- `sistema_ensino`: ANGLO | OBJETIVO | COC | POSITIVO | OUTRO
- `estado_conservacao`: NOVO | BOM | USADO | DANIFICADO
- `cidade`: In current Phase 3 runtime this is inherited from the authenticated donor profile and normalized server-side
- `bairro`: In current Phase 3 runtime this is inherited from the authenticated donor profile and normalized server-side
- `data_publicacao`: Optional integer (1900-2100) representing year material was originally published
- `upload_id`: Temporary storage ID from POST /materiais/preview (links to AI classification results)
- `image`: Not accepted in the current Phase 3 runtime; the image must already have been uploaded in POST /materiais/preview
- User must have `perfil_completo = true` (HTTP 403 if false)

**Client Acquisition Note**:
- The Android donation flow lets the user pick an image from the gallery or capture a new photo with the camera before sending the preview multipart request

### Response

Current runtime override:
- The backend response also includes `status_ia` and `confianca_ia`.
- Timestamp fields currently use `criado_em` / `atualizado_em` naming.

**HTTP 201 Created**
```json
{
  "id": "material-uuid-1234567890",
  "titulo": "Geometria Plana 7º Ano",
  "descricao": "Livro em bom estado, com marcações de uso, base do currículo Anglo 7º ano",
  "disciplina": "MATEMATICA",
  "nivel_ensino": "FUNDAMENTAL",
  "ano": 7,
  "sistema_ensino": "ANGLO",
  "estado_conservacao": "BOM",
  "status": "DISPONIVEL",
  "imagem_url": "https://cdn.ecobook.com/materiais/material-uuid-1234567890.jpg",
  "upload_id": "temp-upload-uuid-abc123def",
  "doador": {
    "id": "user-uuid-donor",
    "nome": "João Silva",
    "whatsapp": "+5548999999999",
    "cidade": "FLORIANOPOLIS",
    "bairro": "CENTRO"
  },
  "cidade": "FLORIANOPOLIS",
  "bairro": "CENTRO",
  "data_publicacao": 2010,
  "status_ia": "LOW_CONFIDENCE",
  "confianca_ia": 0.68,
  "criado_em": "2026-04-17T14:30:00Z",
  "atualizado_em": "2026-04-17T14:30:00Z"
}
```

**Processing Steps**:
1. Validate user has `perfil_completo = true` → HTTP 403 if false
2. Retrieve temporary image via `upload_id`
3. Validate `upload_id` ownership and expiration → HTTP 404 if not found or expired
4. Ensure the staged file still exists before promotion
5. Validate all enum values → HTTP 400 if invalid
6. Normalize geographic data (cidade, bairro)
7. Persist Material with status = DISPONIVEL
8. Promote temporary image to permanent storage (imagem_url)
9. Return Material entity with HTTP 201

Current runtime override:
1. Validate `upload_id` ownership and expiration.
2. Promote the staged file from `/uploads/{user_id}/temp/` to `/uploads/{user_id}/`.
3. Persist the material and keep the upload tracking row linked for audit instead of deleting it.

### Error Responses

Current runtime override:
- Delivered backend errors for this endpoint follow `contracts/error-response.md`.
- Typical runtime codes here are `INVALID_FORMAT`, `INCOMPLETE_PROFILE`, and `NOT_FOUND`.
- The historical examples below are kept as target-state references for richer metadata, not as the guaranteed Phase 4 payload shape.

**HTTP 400 Bad Request** - Invalid enum value

```json
{
  "error": "INVALID_ENUM",
  "message": "Invalid enum value for field",
  "field": "disciplina",
  "details": {
    "received_value": "MUSICA",
    "allowed_values": ["MATEMATICA", "PORTUGUES", "HISTORIA", "GEOGRAFIA", "CIENCIAS", "LITERATURA"]
  }
}
```

**HTTP 400 Bad Request** - Invalid image format

```json
{
  "error": "INVALID_IMAGE",
  "message": "Image must be JPEG or PNG format and ≤ 5MB",
  "field": "image",
  "details": {
    "received_mime_type": "image/gif",
    "received_size_mb": 6.2,
    "allowed_types": ["image/jpeg", "image/png"],
    "max_size_mb": 5
  }
}
```

**HTTP 403 Forbidden** - Profile incomplete

```json
{
  "error": "INCOMPLETE_PROFILE",
  "message": "Profile must be complete before uploading materials",
  "details": {
    "perfil_completo": false,
    "missing_fields": ["cidade", "bairro"]
  }
}
```

**HTTP 404 Not Found** - Upload ID not found

```json
{
  "error": "UPLOAD_NOT_FOUND",
  "message": "Temporary upload not found or expired",
  "field": "upload_id"
}
```

---

## GET /materiais

Search for available materials using the current deterministic matching algorithm.

### Request

```http
GET /api/v1/materiais?query=algebra&disciplina=MATEMATICA&nivel_ensino=FUNDAMENTAL&ano=7&sistema_ensino=ANGLO&cidade=florianopolis&bairro=centro&min_ano_publicacao=2005&max_ano_publicacao=2020&page=0&size=20
Authorization: Bearer <jwt_token>
```

**Query Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `query` | String | No | Accent-insensitive text search across title, description, author, editor and location |
| `disciplina` | String | No | Filter by discipline (exact match when provided) |
| `nivel_ensino` | String | No | Filter by education level (exact match when provided) |
| `ano` | Integer | No | Target grade year (1-12 for FUNDAMENTAL/MEDIO; ignored for SUPERIOR materials) |
| `sistema_ensino` | String | No | Curriculum system filter (`OUTRO` matches only `OUTRO`; named systems also accept `OUTRO`) |
| `cidade` | String | No | City anchor used for ranking and optional filtering |
| `bairro` | String | No | Neighborhood anchor used for ranking and optional filtering |
| `min_ano_publicacao` | Integer | No | Filter materials published on or after this year (1900-2100) |
| `max_ano_publicacao` | Integer | No | Filter materials published on or before this year (1900-2100) |
| `page` | Integer | No | Zero-based page number (default 0) |
| `size` | Integer | No | Results per page (default 20, max 100) |

**Validation Rules**:
- All filters are optional
- If `ano` is provided, it must be in `[1, 12]`
- If `min_ano_publicacao` and `max_ano_publicacao` are both provided: `min <= max`
- If either publication year outside [1900, 2100]: HTTP 400
- City/neighborhood are normalized before ranking comparisons (for example `sao joao` → `SAO JOAO`)
- `page` must be `>= 0`; `size` must be in `[1, 100]`

### Response

**HTTP 200 OK** - Sorted by geographic proximity + recency

```json
{
  "results": [
    {
      "id": "material-uuid-1",
      "titulo": "Geometria Plana 7o Ano",
      "descricao": "Livro em bom estado",
      "disciplina": "MATEMATICA",
      "nivel_ensino": "FUNDAMENTAL",
      "ano": 7,
      "sistema_ensino": "ANGLO",
      "estado_conservacao": "BOM",
      "status": "DISPONIVEL",
      "imagem_url": "https://cdn.ecobook.com/materiais/material-uuid-1.jpg",
      "doador": {
        "id": "user-uuid-1",
        "nome": "Joao Silva",
        "whatsapp": "+5548999999999",
        "cidade": "FLORIANOPOLIS",
        "bairro": "CENTRO"
      },
      "cidade": "FLORIANOPOLIS",
      "bairro": "CENTRO",
      "data_publicacao": 2010,
      "criado_em": "2026-04-15T10:00:00Z",
      "atualizado_em": "2026-04-15T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "total": 45,
  "total_pages": 3,
  "has_next": true
}
```

**Sorting Algorithm** (in order of priority):

1. **Same neighborhood first** (bairro match)
2. **Same city, different neighborhood** (cidade match)
3. **Within matching results**: Sort by `data_publicacao DESC` (newest first)
4. **Tiebreaker**: Sort by `id` (deterministic order)

**Client Rendering Notes**:
- Each result should render as a card in the discovery list
- If `imagem_url` is missing or fails to load, the client should render a neutral placeholder instead of a broken image area
- Tapping a card should open a dismissible dialog/modal with richer detail and an explicit close action
- The discovery dialog may already show a request CTA, but the actual solicitation transaction begins in Phase 5

### Error Responses

Current runtime override:
- Delivered backend errors for this endpoint follow `contracts/error-response.md`.
- Typical runtime codes here are `INVALID_FORMAT` and `INCOMPLETE_PROFILE`.
- The historical examples below are kept as target-state references for richer metadata, not as the guaranteed Phase 4 payload shape.

**HTTP 400 Bad Request** - Invalid enum value

```json
{
  "error": "INVALID_ENUM",
  "message": "Invalid enum value",
  "field": "disciplina",
  "details": {
    "received_value": "MUSICA",
    "allowed_values": ["MATEMATICA", "PORTUGUES", "HISTORIA", "GEOGRAFIA", "CIENCIAS", "LITERATURA"]
  }
}
```

**HTTP 400 Bad Request** - Invalid publication date range

```json
{
  "error": "INVALID_RANGE",
  "message": "min_ano_publicacao must be ≤ max_ano_publicacao",
  "details": {
    "min_ano_publicacao": 2010,
    "max_ano_publicacao": 2000
  }
}
```

**HTTP 400 Bad Request** - Publication year outside valid range

```json
{
  "error": "INVALID_YEAR",
  "message": "Publication year must be between 1900 and 2100",
  "field": "min_ano_publicacao",
  "details": {
    "received_value": 1850,
    "valid_range": [1900, 2100]
  }
}
```

**HTTP 403 Forbidden** - User profile incomplete

```json
{
  "error": "INCOMPLETE_PROFILE",
  "message": "Profile must be complete to search materials"
}
```

---

## GET /materiais/{id}

Retrieve details for a single material.

### Request

```http
GET /api/v1/materiais/material-uuid-1234567890
Authorization: Bearer <jwt_token>
```

### Response

**HTTP 200 OK**
```json
{
  "id": "material-uuid-1234567890",
  "titulo": "Geometria Plana 7º Ano",
  "descricao": "Livro em bom estado, com marcações de uso",
  "disciplina": "MATEMATICA",
  "nivel_ensino": "FUNDAMENTAL",
  "ano": 7,
  "sistema_ensino": "ANGLO",
  "estado_conservacao": "BOM",
  "status": "DISPONIVEL",
  "imagem_url": "https://cdn.ecobook.com/materiais/material-uuid-1234567890.jpg",
  "upload_id": "temp-upload-uuid-abc123def",
  "doador": {
    "id": "user-uuid-donor",
    "nome": "João Silva",
    "whatsapp": "+5548999999999",
    "cidade": "FLORIANOPOLIS",
    "bairro": "CENTRO"
  },
  "cidade": "FLORIANOPOLIS",
  "bairro": "CENTRO",
  "data_publicacao": 2010,
  "created_at": "2026-04-10T14:30:00Z",
  "updated_at": "2026-04-10T14:30:00Z"
}
```

### Error Responses

**HTTP 404 Not Found**
```json
{
  "error": "NOT_FOUND",
  "message": "Material not found"
}
```

---

## PATCH /materiais/{id}

Update material status (e.g., cancel a donation).

### Request

```http
PATCH /api/v1/materiais/material-uuid-1234567890
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "status": "CANCELADO"
}
```

**Validation Rules**:
- User must be the material creator (donor) → HTTP 403 if not
- Only valid state transitions allowed → HTTP 422 if invalid
- Canceling RESERVADO material cascades: related APROVADA Solicitacao → CANCELADA, FCM notification sent

**Valid Transitions**:

| Current Status | Target Status | Allowed | Notes |
|---|---|---|---|
| DISPONIVEL | CANCELADO | ✅ Yes | Donor decides not to donate |
| DISPONIVEL | DOADO | ❌ No | Invalid transition |
| RESERVADO | CANCELADO | ✅ Yes | Cascades to Solicitacao; sends SOLICITACAO_CANCELADA notification |
| RESERVADO | DOADO | ✅ Yes | Completion (via Solicitacao state) |
| DOADO | * | ❌ No | Terminal state; no transitions out |
| CANCELADO | * | ❌ No | Terminal state; no transitions out |

### Response

**HTTP 200 OK**
```json
{
  "id": "material-uuid-1234567890",
  "titulo": "Geometria Plana 7º Ano",
  "descricao": "Livro em bom estado",
  "disciplina": "MATEMATICA",
  "nivel_ensino": "FUNDAMENTAL",
  "ano": 7,
  "sistema_ensino": "ANGLO",
  "estado_conservacao": "BOM",
  "status": "CANCELADO",
  "imagem_url": "https://cdn.ecobook.com/materiais/material-uuid-1234567890.jpg",
  "doador": {
    "id": "user-uuid-donor",
    "nome": "João Silva",
    "whatsapp": "+5548999999999",
    "cidade": "FLORIANOPOLIS",
    "bairro": "CENTRO"
  },
  "cidade": "FLORIANOPOLIS",
  "bairro": "CENTRO",
  "data_publicacao": 2010,
  "created_at": "2026-04-10T14:30:00Z",
  "updated_at": "2026-04-17T16:45:00Z"
}
```

### Error Responses

**HTTP 403 Forbidden** - User is not the material creator

```json
{
  "error": "FORBIDDEN",
  "message": "Only the material donor can modify this material"
}
```

**HTTP 422 Unprocessable Entity** - Invalid state transition

```json
{
  "error": "INVALID_STATE_TRANSITION",
  "message": "Cannot transition from DOADO to any state",
  "details": {
    "current_status": "DOADO",
    "requested_status": "CANCELADO",
    "reason": "DOADO is a terminal state"
  }
}
```

**HTTP 404 Not Found**
```json
{
  "error": "NOT_FOUND",
  "message": "Material not found"
}
```

---

## Performance SLA (Q6 Requirements)

**Material Search Endpoint** (GET /materiais):

| Latency Metric | Target | Status | Notes |
|---|---|---|---|
| P95 latency | ≤ 150ms | Aggressive target | Achieved via database indexes on (status, disciplina, nivel_ensino, cidade, bairro, data_publicacao DESC) |
| P99 latency | ≤ 300ms | Aggressive target | Query optimization + connection pooling (HikariCP) |

**Implementation Details**:
- PostgreSQL indexes on composite keys for filter + sort
- HikariCP connection pool (20 connections default)
- Query result pagination (default 20 items/page)
- No N+1 queries (eager load doador data)

---

## Field Classification Reference

| Field | Auto-Populated | Editable | Source |
|-------|---|---|---|
| `titulo` | Gemini OCR (confidence 0.85-0.95) | Yes | AI or manual |
| `descricao` | **Manual-only** (NEVER auto-populated) | Yes | Always user input |
| `disciplina` | Gemini classification | Yes | AI or manual |
| `nivel_ensino` | Gemini classification | Yes | AI or manual |
| `ano` | Gemini classification or manual | Yes | AI or manual |
| `sistema_ensino` | Gemini classification | Yes | AI or manual |
| `estado_conservacao` | Gemini classification | Yes | AI or manual |
| `data_publicacao` | Gemini OCR (if visible) | Yes | AI or manual |
| `status` | System (starts DISPONIVEL) | No (only via state transitions) | Automatic |
| `imagem_url` | System (after promotion) | No (immutable) | Generated |
| `upload_id` | System (from preview) | No (audit trail) | Generated |
| `city`, `bairro` | User input (normalized) | Yes | Automatic normalization |
