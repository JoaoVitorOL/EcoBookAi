# API Contracts

**Phase**: 1-7 runtime  
**Date**: 2026-05-21  
**Purpose**: Current contract index for the endpoints and payloads already implemented in the repository

---

## Source Of Truth

Use the files in this folder as the current runtime documentation for the implemented HTTP contracts.

Shared runtime assumptions:

- successful responses use the standard envelope `{ status, message, timestamp, path, data }`
- authentication is `email + password + JWT`
- `perfil_completo` gates material, request, and notifications inbox operations
- the backend currently exposes both FCM token registration and a persisted notification inbox API

---

## Current Contract Files

### [user-api.md](user-api.md)

Authentication and profile endpoints:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/usuarios/me`
- `PUT /api/v1/usuarios/me`
- `PATCH /api/v1/usuarios/me/consentimento-ia`

### [material-api.md](material-api.md)

Material and discovery endpoints:

- `POST /api/v1/materiais/preview`
- `POST /api/v1/materiais`
- `GET /api/v1/materiais`
- `GET /api/v1/materiais/me`
- `PUT /api/v1/materiais/{id}`
- `DELETE /api/v1/materiais/{id}`

### [solicitacao-api.md](solicitacao-api.md)

Request workflow endpoints:

- `POST /api/v1/materiais/{id}/solicitacoes`
- `GET /api/v1/solicitacoes/minhas`
- `GET /api/v1/solicitacoes/pendentes`
- `GET /api/v1/solicitacoes/aprovadas`
- `GET /api/v1/solicitacoes/{id}`
- `PATCH /api/v1/solicitacoes/{id}/aprovar`
- `PATCH /api/v1/solicitacoes/{id}/recusar`
- `PATCH /api/v1/solicitacoes/{id}/cancelar`
- `PATCH /api/v1/solicitacoes/{id}/concluir`

### [notification-api.md](notification-api.md)

Notification management endpoints already exposed by the backend:

- `POST /api/v1/fcm/tokens`
- `GET /api/v1/notificacoes`
- `PATCH /api/v1/notificacoes/{id}/ler`
- `PATCH /api/v1/notificacoes/ler-todas`

### [report-api.md](report-api.md)

Post-donation moderation endpoints already exposed by the backend:

- `POST /api/v1/materiais/{id}/nao-recebido`
- `GET /api/v1/admin/reports`
- `PATCH /api/v1/admin/reports/{id}/resolve`

### [notification-schema.md](notification-schema.md)

FCM payload types, routes, retry behavior, and inbox persistence rules for the current notification runtime.

### [ai-response.md](ai-response.md)

Preview payload contract for `POST /api/v1/materiais/preview`.

### [error-response.md](error-response.md)

Shared error envelope and HTTP mapping guidance.

---

## Related Documents

- [quickstart.md](../quickstart.md)
- [data-model.md](../data-model.md)
- [PLAN-SUMMARY.md](../PLAN-SUMMARY.md)
- [TASKS.md](../TASKS.md)
