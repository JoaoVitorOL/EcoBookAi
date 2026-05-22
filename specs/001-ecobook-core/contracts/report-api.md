# Report API Contracts

**Reference**: spec.md Phase 7 / backlog T181-T185  
**Version**: 1.0  
**Date**: 2026-05-21  
**Status**: Current runtime contract for student reporting plus the report-specific admin moderation endpoints; broader admin catalog/user APIs live in `admin-api.md`

---

Runtime notes:
- Successful responses are wrapped in `{ status, message, timestamp, path, data }`.
- The JSON examples below describe the inner `data` payload.
- The current backend persists one open non-receipt report per completed request.

## POST /materiais/{id}/nao-recebido

Create a non-receipt report for a material already marked as donated.

### Request

```http
POST /api/v1/materiais/{id}/nao-recebido
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "reason": "Combinei a retirada, mas o material nao chegou."
}
```

### Runtime Rules

- The authenticated user must have `perfil_completo = true`.
- The target material must exist and currently be `DOADO`.
- Only the student who owns a `CONCLUIDA` request for that material can create the report.
- `reason` is optional and capped at `500` characters.
- The backend rejects duplicate open reports for the same completed request with HTTP `409`.
- A successful creation publishes `NonReceiptReportCreatedEvent` for future moderation workflows.

### Response

**HTTP 201 Created**

```json
{
  "id": "report-uuid-1234567890",
  "material_id": "material-uuid-1234567890",
  "solicitacao_id": "solicitacao-uuid-1234567890",
  "estudante_id": "user-uuid-student",
  "reason": "Combinei a retirada, mas o material nao chegou.",
  "status": "OPEN",
  "created_at": "2026-05-21T10:40:00",
  "updated_at": "2026-05-21T10:40:00",
  "resolved_at": null
}
```

### Error Responses

- `400 INVALID_FORMAT`: invalid UUID or `reason` longer than `500` chars
- `403 ACCESS_DENIED`: authenticated user is not the student from the completed request
- `404 NOT_FOUND`: material not found
- `409 CONFLICT`: an open report already exists for the request
- `422 UNPROCESSABLE_ENTITY`: material has not reached `DOADO`

---

## GET /admin/reports

List non-receipt reports for moderation.

### Request

```http
GET /api/v1/admin/reports?status=OPEN&page=0&size=20
Authorization: Bearer <admin_jwt_token>
```

### Runtime Rules

- The authenticated user must have role `ADMIN`.
- `status` is optional and currently supports `OPEN` or `RESOLVED`.
- `page` must be `>= 0`.
- `size` must be between `1` and `100`.

### Response

**HTTP 200 OK**

```json
{
  "results": [
    {
      "id": "report-uuid-1234567890",
      "material_id": "material-uuid-1234567890",
      "material_titulo": "Geometria Plana 7o Ano",
      "solicitacao_id": "solicitacao-uuid-1234567890",
      "estudante_id": "user-uuid-student",
      "estudante_nome": "Maria Santos",
      "estudante_email": "maria@example.com",
      "doador_id": "user-uuid-donor",
      "doador_nome": "Joao Silva",
      "reason": "Combinei a retirada, mas o material nao chegou.",
      "status": "OPEN",
      "created_at": "2026-05-21T10:40:00",
      "updated_at": "2026-05-21T10:40:00",
      "resolved_at": null,
      "resolution_notes": null
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1,
  "total_pages": 1,
  "has_next": false
}
```

### Error Responses

- `400 INVALID_FORMAT`: invalid status filter or invalid pagination values
- `401 UNAUTHORIZED`: missing or invalid JWT
- `403 ACCESS_DENIED`: authenticated user is not an admin

---

## PATCH /admin/reports/{id}/resolve

Resolve an open report after moderation review.

### Request

```http
PATCH /api/v1/admin/reports/{id}/resolve
Content-Type: application/json
Authorization: Bearer <admin_jwt_token>

{
  "resolutionNotes": "Contato validado e caso encerrado."
}
```

### Runtime Rules

- The authenticated user must have role `ADMIN`.
- `resolutionNotes` is optional and capped at `1000` characters.
- Only `OPEN` reports can be resolved through this endpoint.

### Response

**HTTP 200 OK**

```json
{
  "id": "report-uuid-1234567890",
  "material_id": "material-uuid-1234567890",
  "material_titulo": "Geometria Plana 7o Ano",
  "solicitacao_id": "solicitacao-uuid-1234567890",
  "estudante_id": "user-uuid-student",
  "estudante_nome": "Maria Santos",
  "estudante_email": "maria@example.com",
  "doador_id": "user-uuid-donor",
  "doador_nome": "Joao Silva",
  "reason": "Combinei a retirada, mas o material nao chegou.",
  "status": "RESOLVED",
  "created_at": "2026-05-21T10:40:00",
  "updated_at": "2026-05-21T11:10:00",
  "resolved_at": "2026-05-21T11:10:00",
  "resolution_notes": "Contato validado e caso encerrado."
}
```

### Error Responses

- `400 INVALID_FORMAT`: invalid UUID or `resolutionNotes` longer than `1000` chars
- `401 UNAUTHORIZED`: missing or invalid JWT
- `403 ACCESS_DENIED`: authenticated user is not an admin
- `404 NOT_FOUND`: report not found
- `409 CONFLICT`: report is already resolved
