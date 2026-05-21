# Notification API Contracts

**Reference**: Phase 6 runtime notification stack  
**Version**: 1.0  
**Date**: 2026-05-21  
**Status**: Current runtime contract for token registration and in-app notifications inbox

---

Runtime notes:

- successful responses are wrapped in `{ status, message, timestamp, path, data }`
- the examples below show the inner `data` payload only
- `GET /notificacoes` and the read endpoints require `perfil_completo = true`

## POST /fcm/tokens

Register or rotate the current device token for the authenticated user.

### Request

```http
POST /api/v1/fcm/tokens
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "token": "fcm-device-token"
}
```

### Runtime Behavior

- overwrites the stored token for the current authenticated session
- keeps Firebase dispatch dormant when Admin SDK credentials are not configured
- still allows the Android app to keep the local inbox behavior active

### Response

**HTTP 200 OK**

No inner `data` payload is currently returned.

---

## GET /notificacoes

Load the persisted notifications inbox for the authenticated user.

### Request

```http
GET /api/v1/notificacoes
Authorization: Bearer <jwt_token>
```

### Response

**HTTP 200 OK**

```json
[
  {
    "id": "notification-row-uuid",
    "title": "Solicitacao aprovada",
    "body": "Sua solicitacao do material foi aprovada.",
    "notification_type": "SOLICITACAO_APROVADA",
    "route": "my-requests",
    "request_id": "solicitacao-uuid",
    "material_id": "material-uuid",
    "received_at": "2026-05-21T09:21:10",
    "unread": true,
    "metadata": {
      "material_titulo": "Geometria Plana 7o Ano",
      "doador_nome": "Joao Silva",
      "doador_whatsapp": "+5511999999999"
    }
  }
]
```

### Ordering Rules

- newest notifications first
- `unread = true` while `read_at` is still null in persistence

---

## PATCH /notificacoes/{id}/ler

Mark one notification as read.

### Request

```http
PATCH /api/v1/notificacoes/{id}/ler
Authorization: Bearer <jwt_token>
```

### Response

**HTTP 200 OK**

No inner `data` payload is currently returned.

---

## PATCH /notificacoes/ler-todas

Mark every inbox item for the authenticated user as read.

### Request

```http
PATCH /api/v1/notificacoes/ler-todas
Authorization: Bearer <jwt_token>
```

### Response

**HTTP 200 OK**

No inner `data` payload is currently returned.

---

## Error Responses

- `401 UNAUTHORIZED`: missing or invalid JWT
- `403 INCOMPLETE_PROFILE`: onboarding not completed for inbox access
- `404 NOT_FOUND`: notification row does not belong to the current user or does not exist
