# API Contracts

**Phase**: 1 (Design & Contracts)  
**Date**: 2026-05-05  
**Purpose**: Formal API contract definitions for all REST endpoints

---

## Auth and User Contracts

### [user-api.md](user-api.md)

User management and authentication endpoints:

- `POST /api/v1/auth/register` - Create account with email and password
- `POST /api/v1/auth/login` - Authenticate with email and password
- `PUT /api/v1/usuarios/me` - Complete or update profile
- `GET /api/v1/usuarios/me` - Retrieve current authenticated user

Core rules:
- Passwords are stored only as strong hashes on the backend
- JWT authentication remains the session mechanism
- `perfil_completo` gates material and request operations
- WhatsApp uses E.164 format
- Geographic normalization remains uppercase + NFD + ASCII

---

## Domain Contracts

### [material-api.md](material-api.md)

Material endpoints:
- `POST /materiais`
- `GET /materiais`
- `GET /materiais/{id}`
- `PATCH /materiais/{id}`

### [ai-response.md](ai-response.md)

AI preview contract:
- `POST /materiais/preview`

### [solicitacao-api.md](solicitacao-api.md)

Request workflow endpoints:
- `POST /solicitacoes`
- `PATCH /solicitacoes/{id}`
- `GET /solicitacoes`
- `GET /solicitacoes/{id}`

### [notification-schema.md](notification-schema.md)

FCM payloads for the donation workflow.

### [error-response.md](error-response.md)

Standard error envelope and HTTP code mapping.

---

## Cross-Contract Rules

Shared assumptions across contracts:

- Authentication uses backend-issued JWT tokens
- Registration and login are local email/password flows
- `password_hash` is internal and never exposed by the API
- `perfil_completo` stays separate from authentication success
- Gemini, matching, and FCM flows remain unchanged by the auth pivot

---

## Related Documents

- [spec.md](../spec.md)
- [plan.md](../plan.md)
- [research.md](../research.md)
- [data-model.md](../data-model.md)
- [quickstart.md](../quickstart.md)
- [TASKS.md](../TASKS.md)
