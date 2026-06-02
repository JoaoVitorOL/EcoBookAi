# Material API Contracts

**Reference**: spec.md RF-005 through RF-025, RF-044  
**Version**: 1.7  
**Date**: 2026-05-23  
**Status**: Current runtime contract for preview, discovery with offset/cursor pagination, secure image access and donor-owned CRUD flow

---

Runtime notes:
- Successful responses are wrapped in `{ status, message, timestamp, path, data }`.
- The JSON examples below describe the inner `data` payload.
- Material images are staged in `POST /api/v1/materiais/preview` and then promoted during `POST /api/v1/materiais`.
- Promoted images are no longer served from public `/uploads/...` URLs; the runtime now returns authenticated `/api/v1/images/{upload_tracking_id}` URLs in `imagem_url` and `imagem_verso_url`.
- Donor location (`cidade`, `bairro`) is inherited from the authenticated profile instead of being resent by the Android app.

## POST /materiais

Create a new material from a previously staged `upload_id`.

### Request

```http
POST /api/v1/materiais
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "titulo": "Geometria Plana 7o Ano",
  "descricao": "Livro em bom estado, com marcas de uso",
  "disciplina": "MATEMATICA",
  "nivel_ensino": "FUNDAMENTAL",
  "ano": 7,
  "sistema_ensino": "ANGLO",
  "estado_conservacao": "BOM",
  "necessidade_academica": "TEXTBOOKS",
  "data_publicacao": 2010,
  "upload_id": "temp-upload-uuid-abc123def"
}
```

### Validation Rules

- `titulo`: required, max 255 chars
- `descricao`: required, 10-2000 chars, manual-only field
- `disciplina`: `TODAS | MATEMATICA | PORTUGUES | HISTORIA | GEOGRAFIA | CIENCIAS | LITERATURA`
- `nivel_ensino`: `FUNDAMENTAL | MEDIO | SUPERIOR`
- `ano`: `1..9` for `FUNDAMENTAL`, `1..3` for `MEDIO`, omitted for `SUPERIOR`
- `sistema_ensino`: `ANGLO | OBJETIVO | COC | POSITIVO | POLIEDRO | ETAPA | BERNOULLI | SAS | FTD | OUTRO`
- `estado_conservacao`: `NOVO | BOM | USADO | DANIFICADO`
- `necessidade_academica`: `TEXTBOOKS | WORKBOOKS | REFERENCE_MATERIALS | FICTION | TECHNICAL_BOOKS | TEST_PREP`
- `data_publicacao`: optional, `1900..2100`
- `upload_id`: must belong to the authenticated donor and still exist in temporary storage
- User must have `perfil_completo = true`
- `TODAS` should be used when the material clearly covers multiple disciplines

### Response

**HTTP 201 Created**

```json
{
  "id": "material-uuid-1234567890",
  "titulo": "Geometria Plana 7o Ano",
  "autor": "Autor Exemplo",
  "editora": "Editora Exemplo",
  "descricao": "Livro em bom estado, com marcas de uso",
  "disciplina": "MATEMATICA",
  "nivel_ensino": "FUNDAMENTAL",
  "ano": 7,
  "sistema_ensino": "ANGLO",
  "estado_conservacao": "BOM",
  "necessidade_academica": "TEXTBOOKS",
  "status": "DISPONIVEL",
  "imagem_url": "/api/v1/images/upload-tracking-uuid",
  "imagem_verso_url": "/api/v1/images/upload-tracking-uuid?side=back",
  "upload_id": "temp-upload-uuid-abc123def",
  "doador": {
    "id": "user-uuid-doador",
    "nome": "Joao Silva"
  },
  "cidade": "FLORIANOPOLIS",
  "bairro": "CENTRO",
  "data_publicacao": 2010,
  "status_ia": "LOW_CONFIDENCE",
  "confianca_ia": 0.68,
  "criado_em": "2026-05-13T14:30:00",
  "atualizado_em": "2026-05-13T14:30:00"
}
```

### Error Responses

- `400 INVALID_FORMAT`: invalid enum, invalid year or invalid UUID
- `403 INCOMPLETE_PROFILE`: onboarding incomplete
- `404 NOT_FOUND`: staged upload not found or expired
- `413 PAYLOAD_TOO_LARGE`: preview image exceeded limits before staging

---

## GET /materiais

Search available materials.

### Request

```http
GET /api/v1/materiais?query=algebra&disciplina=MATEMATICA&nivel_ensino=FUNDAMENTAL&ano=7&sistema_ensino=ANGLO&necessidade_academica=TEXTBOOKS&cidade=florianopolis&bairro=centro&min_ano_publicacao=2005&max_ano_publicacao=2020&page=0&size=20&after_id=material-uuid-20
Authorization: Bearer <jwt_token>
```

### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `query` | String | No | Accent-insensitive match against title, description, author, editor and location |
| `disciplina` | String | No | Exact discipline filter |
| `nivel_ensino` | String | No | Exact education-level filter |
| `ano` | Integer | No | School year filter (`1..9` for `FUNDAMENTAL`, `1..3` for `MEDIO`) |
| `sistema_ensino` | String | No | Teaching-system filter |
| `necessidade_academica` | String | No | Exact academic-need filter for the material metadata |
| `cidade` | String | No | City anchor for ranking and tie-breaking; does not exclude other cities by itself |
| `bairro` | String | No | Neighborhood anchor for ranking and tie-breaking; does not exclude other neighborhoods by itself |
| `min_ano_publicacao` | Integer | No | Lower bound for publication year |
| `max_ano_publicacao` | Integer | No | Upper bound for publication year |
| `page` | Integer | No | Zero-based page number echoed for UI state; the first page uses offset pagination |
| `size` | Integer | No | Page size (`1..100`) |
| `after_id` | UUID String | No | Cursor returned by the previous page with the same filters; switches the response to keyset continuation |

### Response

**HTTP 200 OK**

```json
{
  "results": [
    {
      "id": "material-uuid-1",
      "titulo": "Geometria Plana 7o Ano",
      "autor": "Autor Exemplo",
      "editora": "Editora Exemplo",
      "descricao": "Livro em bom estado",
      "disciplina": "MATEMATICA",
      "nivel_ensino": "FUNDAMENTAL",
      "ano": 7,
      "sistema_ensino": "ANGLO",
      "estado_conservacao": "BOM",
      "necessidade_academica": "TEXTBOOKS",
      "status": "DISPONIVEL",
      "imagem_url": "/api/v1/images/upload-tracking-uuid",
      "imagem_verso_url": "/api/v1/images/upload-tracking-uuid?side=back",
      "doador": {
        "id": "user-uuid-doador",
        "nome": "Joao Silva"
      },
      "cidade": "FLORIANOPOLIS",
      "bairro": "CENTRO",
      "data_publicacao": 2010,
      "criado_em": "2026-05-13T14:30:00",
      "atualizado_em": "2026-05-13T14:30:00"
    }
  ],
  "page": 0,
  "size": 20,
  "total": 45,
  "total_pages": 3,
  "has_next": true,
  "after_id": null,
  "next_after_id": "material-uuid-20",
  "pagination_mode": "OFFSET"
}
```

### Runtime Behavior

- Only materials still stored as `DISPONIVEL` are returned
- Sorting prioritizes same neighborhood, then same city, then newer `data_publicacao`, then stable `id`
- `cidade` and `bairro` influence ranking and cursor order, but they are not hard filters on their own
- Requests without `after_id` use the existing offset page and may return `next_after_id` for continuation
- Requests with a valid `after_id` from the previous page and the same filters return `pagination_mode = KEYSET`
- If the image URL fails client-side, the Android app must render a neutral placeholder
- `imagem_verso_url` may be null when the donor published only the front cover

### Error Responses

- `400 INVALID_FORMAT`: invalid enum, invalid page/size, invalid publication-year range, malformed `after_id` or cursor that does not belong to the current filtered result window
- `403 INCOMPLETE_PROFILE`: onboarding incomplete

---

## GET /images/{image_id}

Download a promoted image through the authenticated image gateway.

### Request

```http
GET /api/v1/images/upload-tracking-uuid
Authorization: Bearer <jwt_token>
```

Optional back-cover access:

```http
GET /api/v1/images/upload-tracking-uuid?side=back
Authorization: Bearer <jwt_token>
```

### Runtime Rules

- Requires authentication
- Donors and admins may access the stored image set
- Front-cover discovery remains available to authenticated users while the material is still `DISPONIVEL`
- Back-cover access is restricted to donor, approved/completed requester, or admin
- Missing files return `404 NOT_FOUND`

### Response

**HTTP 200 OK**
- Binary image body
- `Content-Type: image/jpeg` or `image/png`

### Error Responses

- `401 UNAUTHORIZED`: missing or invalid JWT
- `403 ACCESS_DENIED`: requester is not allowed to view the requested image side
- `404 NOT_FOUND`: image or promoted upload reference not found

---

## GET /materiais/me

List all materials created by the authenticated donor.

### Request

```http
GET /api/v1/materiais/me
Authorization: Bearer <jwt_token>
```

### Response

**HTTP 200 OK**

Returns an array of `MaterialDTO` items inside the standard API envelope. This powers the donor history tab in the Android app.

---

## PUT /materiais/{id}

Edit a material owned by the authenticated donor.

### Request

```http
PUT /api/v1/materiais/material-uuid-1234567890
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "titulo": "Geometria Plana 7o Ano",
  "descricao": "Livro em bom estado",
  "disciplina": "MATEMATICA",
  "nivel_ensino": "FUNDAMENTAL",
  "ano": 7,
  "sistema_ensino": "ANGLO",
  "estado_conservacao": "BOM",
  "necessidade_academica": "TEXTBOOKS",
  "data_publicacao": 2010
}
```

### Validation Rules

- Allowed only for the donor who created the material
- Valid only while the material is `DISPONIVEL`
- Editable metadata fields are fully revalidated on every update

### Response

**HTTP 200 OK**

Returns the updated `MaterialDTO`.

### Error Responses

- `403 ACCESS_DENIED`: user is not the material creator
- `404 NOT_FOUND`: material not found
- `422 UNPROCESSABLE_ENTITY`: material is not editable in its current state

---

## DELETE /materiais/{id}

Hard-delete a material owned by the authenticated donor.

### Request

```http
DELETE /api/v1/materiais/material-uuid-1234567890
Authorization: Bearer <jwt_token>
```

### Runtime Behavior

- Allowed only for the donor who created the material
- Valid only while the material is `DISPONIVEL`
- Removes the material row from the database instead of changing its status to `CANCELADO`
- Removes related upload-tracking rows when they exist and deletes the promoted image file from local storage when that file is still present
- After a successful delete, the material no longer appears in discovery results or in `GET /materiais/me`

### Response

**HTTP 204 No Content**

### Error Responses

- `403 ACCESS_DENIED`: user is not the material creator
- `404 NOT_FOUND`: material not found
- `422 UNPROCESSABLE_ENTITY`: material is not deletable in its current state

---

## Performance SLA (Q6 Requirements)

**Material Search Endpoint** (`GET /materiais`)

| Latency Metric | Target | Notes |
|---|---|---|
| P95 latency | <= 150ms | Backed by composite indexes and deterministic ranking |
| P99 latency | <= 300ms | Query optimization + HikariCP connection pooling |

**Implementation Details**

- PostgreSQL indexes on filterable columns plus publication-year sorting support
- Query pagination defaults to 20 items per page
- Donor data is included without N+1 fetch amplification
