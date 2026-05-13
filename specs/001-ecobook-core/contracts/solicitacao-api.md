# Solicitacao API Contracts

**Reference**: spec.md RF-026 through RF-035  
**Version**: 1.2  
**Date**: 2026-05-13  
**Status**: Current runtime contract for Phase 5 request workflow

---

Runtime notes:
- Successful responses are wrapped in `{ status, message, timestamp, path, data }`.
- The examples below describe the inner `data` payload.
- The current backend uses action endpoints (`/aprovar`, `/recusar`, `/cancelar`, `/concluir`) instead of a generic `PATCH /solicitacoes/{id}` body.
- `contato_doador` is only populated after approval.

## Shared Payload Shape

Every runtime response returns a `SolicitacaoDTO` compatible with the shape below.

```json
{
  "id": "solicitacao-uuid-1234567890",
  "material_id": "material-uuid-1234567890",
  "estudante_id": "user-uuid-student",
  "status": "PENDENTE",
  "contato_doador": null,
  "criado_em": "2026-05-13T14:30:00",
  "atualizado_em": "2026-05-13T14:30:00",
  "aprovado_em": null,
  "expires_at": null,
  "concluido_em": null,
  "material": {
    "id": "material-uuid-1234567890",
    "titulo": "Geometria Plana 7o Ano",
    "descricao": "Livro em bom estado",
    "imagem_url": "/api/uploads/usuario/material.jpg",
    "disciplina": "MATEMATICA",
    "nivel_ensino": "FUNDAMENTAL",
    "ano": 7,
    "status": "DISPONIVEL",
    "cidade": "FLORIANOPOLIS",
    "bairro": "CENTRO",
    "doador_nome": "Joao Silva"
  },
  "estudante": {
    "id": "user-uuid-student",
    "nome": "Maria Santos",
    "cidade": "FLORIANOPOLIS",
    "bairro": "CENTRO"
  }
}
```

## POST /materiais/{material_id}/solicitacoes

Create a new request for an available material.

### Request

```http
POST /api/v1/materiais/{material_id}/solicitacoes
Authorization: Bearer <jwt_token>
```

### Runtime Rules

- The authenticated user becomes the requesting student.
- The target material must exist and be `DISPONIVEL`.
- The student cannot request their own material.
- The same student cannot keep another `PENDENTE` or `APROVADA` request for the same material.
- If the material is already `RESERVADO`, the backend returns HTTP 409.

### Response

**HTTP 201 Created**

```json
{
  "id": "solicitacao-uuid-1234567890",
  "status": "PENDENTE",
  "contato_doador": null,
  "criado_em": "2026-05-13T14:30:00"
}
```

### Error Responses

- `400 INVALID_FORMAT`: invalid UUID or self-request
- `403 INCOMPLETE_PROFILE`: onboarding incomplete
- `404 NOT_FOUND`: material not found
- `409 CONFLICT`: duplicate active request or material already reserved
- `422 UNPROCESSABLE_ENTITY`: material no longer available for requests

---

## GET /solicitacoes/minhas

List requests created by the authenticated student.

### Request

```http
GET /api/v1/solicitacoes/minhas?status=APROVADA
Authorization: Bearer <jwt_token>
```

### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `status` | String | No | Optional filter: `PENDENTE`, `APROVADA`, `RECUSADA`, `CANCELADA`, `CONCLUIDA` |

### Response

**HTTP 200 OK**

```json
[
  {
    "id": "solicitacao-uuid-1",
    "status": "PENDENTE"
  },
  {
    "id": "solicitacao-uuid-2",
    "status": "APROVADA",
    "contato_doador": {
      "nome": "Maria Santos",
      "whatsapp": "+5548999999999"
    },
    "expires_at": "2026-05-27T10:15:00"
  }
]
```

---

## GET /solicitacoes/pendentes

List pending requests for materials owned by the authenticated donor.

### Request

```http
GET /api/v1/solicitacoes/pendentes
Authorization: Bearer <jwt_token>
```

### Response

**HTTP 200 OK**

Returns only `PENDENTE` requests tied to materials created by the authenticated user.

---

## GET /solicitacoes/aprovadas

List donor requests that are already approved and still awaiting completion.

### Request

```http
GET /api/v1/solicitacoes/aprovadas
Authorization: Bearer <jwt_token>
```

### Response

**HTTP 200 OK**

Returns only `APROVADA` requests tied to materials created by the authenticated user.

---

## GET /solicitacoes/{id}

Load one request. Only the donor or the requesting student can access it.

### Request

```http
GET /api/v1/solicitacoes/{id}
Authorization: Bearer <jwt_token>
```

### Response

**HTTP 200 OK**

Returns the full shared payload shape shown above.

### Error Responses

- `400 INVALID_FORMAT`: invalid UUID
- `403 ACCESS_DENIED`: authenticated user is neither donor nor requesting student
- `404 NOT_FOUND`: request not found

---

## PATCH /solicitacoes/{id}/aprovar

Approve a pending request as the donor.

### Request

```http
PATCH /api/v1/solicitacoes/{id}/aprovar
Authorization: Bearer <jwt_token>
```

### Runtime Behavior

- Runs inside a serializable transaction.
- Locks both the request row and the related material row.
- Requires request status `PENDENTE` and material status `DISPONIVEL`.
- Sets request status to `APROVADA`.
- Populates `aprovado_em`, `expires_at` (`+14 days`) and `contato_doador`.
- Sets material status to `RESERVADO`.
- Automatically changes every other pending request for the same material to `RECUSADA`.

### Response

**HTTP 200 OK**

Returns the updated `SolicitacaoDTO` with donor contact populated.

### Error Responses

- `403 ACCESS_DENIED`: only the donor can approve
- `404 NOT_FOUND`: request or material not found
- `409 CONFLICT`: another approved request already exists for the material
- `422 UNPROCESSABLE_ENTITY`: invalid request/material state

---

## PATCH /solicitacoes/{id}/recusar

Decline a pending request as the donor.

### Request

```http
PATCH /api/v1/solicitacoes/{id}/recusar
Authorization: Bearer <jwt_token>
```

### Runtime Behavior

- Allowed only for the donor.
- Requires request status `PENDENTE`.
- Keeps the material `DISPONIVEL`.
- Clears approval-related fields if they somehow existed.

### Response

**HTTP 200 OK**

Returns the updated request with status `RECUSADA`.

---

## PATCH /solicitacoes/{id}/cancelar

Cancel a request as either participant.

### Request

```http
PATCH /api/v1/solicitacoes/{id}/cancelar
Authorization: Bearer <jwt_token>
```

### Runtime Behavior

- Allowed for the requesting student or the donor.
- Valid only while the request is `PENDENTE` or `APROVADA`.
- If the request was `APROVADA`, the material is atomically reverted from `RESERVADO` to `DISPONIVEL`.
- Clears `contato_doador`.

### Response

**HTTP 200 OK**

Returns the updated request with status `CANCELADA`.

---

## PATCH /solicitacoes/{id}/concluir

Mark a reservation as donated.

### Request

```http
PATCH /api/v1/solicitacoes/{id}/concluir
Authorization: Bearer <jwt_token>
```

### Runtime Behavior

- Allowed only for the donor.
- Requires request status `APROVADA`.
- Requires material status `RESERVADO`.
- Sets request status to `CONCLUIDA` and fills `concluido_em`.
- Sets material status to `DOADO` and fills `doado_em`.

### Response

**HTTP 200 OK**

Returns the updated request with status `CONCLUIDA`.

---

## Reservation Expiry Job

Approved reservations expire automatically after 14 days.

### Runtime Behavior

- Daily scheduler runs at `02:00 UTC`.
- Finds requests where `status = APROVADA` and `expires_at < now`.
- Reverts the related material from `RESERVADO` to `DISPONIVEL`.
- Changes the request status to `CANCELADA`.
- Clears `contato_doador`.

### Invariants

- A material cannot keep two approved requests.
- `RESERVADO` is only valid while an approved request still exists.
- Donor contact is never exposed before approval.
