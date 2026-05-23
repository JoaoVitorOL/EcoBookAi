# Admin API Contracts

**Reference**: spec.md Phase 7 / backlog T184-T185  
**Version**: 1.0  
**Date**: 2026-05-21  
**Status**: Current runtime contract for the broader admin moderation/catalog endpoints

---

Runtime notes:
- Successful responses are wrapped in `{ status, message, timestamp, path, data }`.
- The JSON examples below describe the inner `data` payload.
- All endpoints in this file require an authenticated user with role `ADMIN`.

## GET /admin/materials

List platform materials for moderation, including `CANCELADO`.

### Request

```http
GET /api/v1/admin/materials?status=CANCELADO&page=0&size=20
Authorization: Bearer <admin_jwt_token>
```

### Runtime Rules

- `status` is optional and supports `DISPONIVEL`, `RESERVADO`, `DOADO` or `CANCELADO`
- `page` must be `>= 0`
- `size` must be between `1` and `100`

### Response

**HTTP 200 OK**

```json
{
  "results": [
    {
      "id": "material-uuid-1234567890",
      "titulo": "Geometria Plana 7o Ano",
      "autor": "Autor Exemplo",
      "editora": "Editora Exemplo",
      "descricao": "Descricao suficiente do material.",
      "disciplina": "MATEMATICA",
      "nivel_ensino": "FUNDAMENTAL",
      "ano": 7,
      "sistema_ensino": "ANGLO",
      "estado_conservacao": "BOM",
      "status": "CANCELADO",
      "imagem_url": "/api/uploads/user/material.jpg",
      "imagem_verso_url": null,
      "upload_id": "temp-upload-123",
      "doador": {
        "id": "user-uuid-donor",
        "nome": "Joao Silva",
        "whatsapp": "+5511999999999",
        "cidade": "FLORIANOPOLIS",
        "bairro": "CENTRO"
      },
      "cidade": "FLORIANOPOLIS",
      "bairro": "CENTRO",
      "data_publicacao": 2024,
      "status_ia": "SUCCESS",
      "confianca_ia": 0.93,
      "criado_em": "2026-05-21T10:40:00",
      "atualizado_em": "2026-05-21T11:10:00"
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

## DELETE /admin/materials/{id}

Hard-delete a material from the platform for moderation purposes.

### Request

```http
DELETE /api/v1/admin/materials/{id}
Authorization: Bearer <admin_jwt_token>
```

### Runtime Rules

- Deletes the material regardless of donor ownership
- Removes tracked stored images when the upload-tracking link still exists
- Publishes administrative removal notifications to the donor and related request owners before the thread ends

### Response

**HTTP 204 No Content**

### Error Responses

- `400 INVALID_FORMAT`: invalid UUID
- `401 UNAUTHORIZED`: missing or invalid JWT
- `403 ACCESS_DENIED`: authenticated user is not an admin
- `404 NOT_FOUND`: material not found

---

## GET /admin/users

List users with admin-facing activity metrics.

### Request

```http
GET /api/v1/admin/users?page=0&size=20
Authorization: Bearer <admin_jwt_token>
```

### Runtime Rules

- `page` must be `>= 0`
- `size` must be between `1` and `100`
- Activity counters currently include:
  - `materials_count`
  - `donated_materials_count`
  - `requests_count`
  - `completed_requests_count`
  - `open_reports_count`

### Response

**HTTP 200 OK**

```json
{
  "results": [
    {
      "id": "user-uuid-1234567890",
      "email": "maria@example.com",
      "nome": "Maria Santos",
      "whatsapp": "+5511999999999",
      "cidade": "FLORIANOPOLIS",
      "bairro": "CENTRO",
      "instituicao": "UFSC",
      "perfil_completo": true,
      "consentimento_ia": true,
      "role": "USER",
      "necessidades_academicas": [
        "TEXTBOOKS"
      ],
      "materials_count": 3,
      "donated_materials_count": 1,
      "requests_count": 4,
      "completed_requests_count": 2,
      "open_reports_count": 1,
      "criado_em": "2026-05-20T09:00:00",
      "atualizado_em": "2026-05-21T11:10:00"
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

- `400 INVALID_FORMAT`: invalid pagination values
- `401 UNAUTHORIZED`: missing or invalid JWT
- `403 ACCESS_DENIED`: authenticated user is not an admin

---

## GET /admin/audit-log

List the audit trail captured for sensitive user, moderation and LGPD actions.

### Request

```http
GET /api/v1/admin/audit-log?actor_user_id=user-uuid-1&target_user_id=user-uuid-2&action=ACCOUNT_DELETED&from=2026-05-20T00:00:00&to=2026-05-23T23:59:59&page=0&size=20
Authorization: Bearer <admin_jwt_token>
```

### Runtime Rules

- All filters are optional.
- `actor_user_id` and `target_user_id` must be valid UUIDs when provided.
- `page` must be `>= 0`.
- `size` must be between `1` and `100`.
- Results are returned in descending `created_at` order.

### Response

**HTTP 200 OK**

```json
{
  "results": [
    {
      "id": "audit-uuid-1234567890",
      "actor_user_id": "user-uuid-admin",
      "actor_email": "admin@example.com",
      "target_user_id": "user-uuid-target",
      "action": "ACCOUNT_DELETED",
      "resource_type": "USER",
      "resource_id": "user-uuid-target",
      "details": {
        "reason": "Nao utilizo mais a plataforma",
        "deleted_at": "2026-05-22T19:10:00"
      },
      "created_at": "2026-05-22T19:10:01"
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

- `400 INVALID_FORMAT`: invalid UUID or invalid pagination values
- `401 UNAUTHORIZED`: missing or invalid JWT
- `403 ACCESS_DENIED`: authenticated user is not an admin

---

## Admin Bootstrap Note

The backend now supports startup bootstrap/promotion through environment variables:

- `ADMIN_BOOTSTRAP_ENABLED=true`
- `ADMIN_BOOTSTRAP_EMAIL=<admin email>`
- `ADMIN_BOOTSTRAP_PASSWORD=<password>` when creating a missing account
- `ADMIN_BOOTSTRAP_NAME=<display name>` optional

Behavior:

- if the email already exists, the account is promoted to `ADMIN`
- if the email does not exist and a password is provided, a new lightweight `ADMIN` account is created
