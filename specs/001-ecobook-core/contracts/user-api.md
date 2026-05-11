# User API Contracts

**Reference**: spec.md RF-001, RF-002, RF-003, RF-004  
**Version**: 2.1  
**Date**: 2026-05-05  
**Status**: Aligned with the current backend implementation

---

Current runtime note:
- Successful responses are wrapped in the standard envelope `{ status, message, timestamp, path, data }`.
- The JSON objects shown in the success examples below represent the `data` payload for readability.

## POST /auth/register

Create a new local account with `email + password`.

### Request

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "joao@example.com",
  "password": "SenhaSegura123",
  "nome": "Joao Silva"
}
```

**Validation Rules**:
- `email`: required, valid format, max 255 characters
- `password`: required, 8-72 characters
- `nome`: required, max 100 characters at API boundary

**Security Rules**:
- Backend stores only `password_hash`
- Raw password is accepted only at register/login boundaries
- Raw password must never appear in API responses, logs, or analytics
- Successful registration issues a JWT immediately

### Response

**HTTP 201 Created**
```json
{
  "id": "user-uuid-1234567890",
  "email": "joao@example.com",
  "nome": "Joao Silva",
  "whatsapp": null,
  "cidade": null,
  "bairro": null,
  "instituicao": null,
  "perfil_completo": false,
  "consentimento_ia": false,
  "role": "USER",
  "token": "jwt-token-value",
  "expires_in": 604800
}
```

### Error Responses

**HTTP 400 Bad Request**
```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Falha de validacao",
  "field_errors": {
    "password": "A senha deve ter entre 8 e 72 caracteres"
  }
}
```

**HTTP 409 Conflict**
```json
{
  "status": 409,
  "error": "CONFLICT",
  "message": "Este email ja esta cadastrado"
}
```

**Important Behavior**:
- Existing email addresses always return `409 Conflict`
- Legacy Google-era rows are no longer auto-claimable during register; they require manual migration or an explicit password-reset/recovery flow

---

## POST /auth/login

Authenticate an existing account with `email + password`.

### Request

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "joao@example.com",
  "password": "SenhaSegura123"
}
```

### Response

**HTTP 200 OK**
```json
{
  "id": "user-uuid-1234567890",
  "email": "joao@example.com",
  "nome": "Joao Silva",
  "whatsapp": "+5548999999999",
  "cidade": "FLORIANOPOLIS",
  "bairro": "CENTRO",
  "instituicao": "Escola Municipal ABC",
  "perfil_completo": true,
  "consentimento_ia": true,
  "role": "USER",
  "token": "jwt-token-value",
  "expires_in": 604800
}
```

### Error Response

**HTTP 401 Unauthorized**
```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Email ou senha invalidos"
}
```

---

## GET /usuarios/me

Return the authenticated user's current profile.

### Request

```http
GET /api/v1/usuarios/me
Authorization: Bearer <jwt_token>
```

### Response

**HTTP 200 OK**
```json
{
  "id": "user-uuid-1234567890",
  "email": "joao@example.com",
  "nome": "Joao Silva",
  "whatsapp": "+5548999999999",
  "cidade": "FLORIANOPOLIS",
  "bairro": "CENTRO",
  "instituicao": "Escola Municipal ABC",
  "perfil_completo": true,
  "consentimento_ia": true,
  "role": "USER",
  "necessidades_academicas": [
    "TEXTBOOKS",
    "TEST_PREP"
  ],
  "criado_em": "2026-05-05T10:30:00",
  "atualizado_em": "2026-05-05T11:45:00"
}
```

### Error Response

**HTTP 401 Unauthorized**
```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Um token JWT valido e obrigatorio",
  "path": "/api/v1/usuarios/me"
}
```

---

## PUT /usuarios/me

Update the current authenticated user's profile. This endpoint still accepts `consentimento_ia` during full profile updates.

### Request

```http
PUT /api/v1/usuarios/me
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "nome": "Joao Pedro Silva",
  "whatsapp": "+5548988888888",
  "cidade": "sao jose",
  "bairro": "centro",
  "instituicao": "Escola Municipal ABC",
  "consentimento_ia": true,
  "necessidades_academicas": [
    "TEXTBOOKS",
    "TEST_PREP"
  ]
}
```

**Validation Rules**:
- `nome`: required for successful profile completion
- `whatsapp`: required for successful profile completion; must match `+55XXXXXXXXXXX`
- `cidade`: required for successful profile completion; normalized before save
- `bairro`: required for successful profile completion; normalized before save
- `instituicao`: optional
- `consentimento_ia`: optional, does not block `perfil_completo`
- `necessidades_academicas`: optional set of enum values

### Response

**HTTP 200 OK**
```json
{
  "id": "user-uuid-1234567890",
  "email": "joao@example.com",
  "nome": "Joao Pedro Silva",
  "whatsapp": "+5548988888888",
  "cidade": "SAO JOSE",
  "bairro": "CENTRO",
  "instituicao": "Escola Municipal ABC",
  "perfil_completo": true,
  "consentimento_ia": true,
  "role": "USER",
  "necessidades_academicas": [
    "TEXTBOOKS",
    "TEST_PREP"
  ],
  "criado_em": "2026-05-05T10:30:00",
  "atualizado_em": "2026-05-05T11:45:00"
}
```

### Error Responses

**HTTP 400 Bad Request**
```json
{
  "status": 400,
  "error": "INVALID_FORMAT",
  "message": "O perfil contem campos invalidos",
  "field_errors": {
    "whatsapp": "Use o formato E.164 (+55XXXXXXXXXXX)"
  }
}
```

**HTTP 422 Unprocessable Entity**
```json
{
  "status": 422,
  "error": "UNPROCESSABLE_ENTITY",
  "message": "Preencha todos os campos obrigatorios do perfil",
  "field_errors": {
    "cidade": "Informe sua cidade",
    "bairro": "Informe seu bairro"
  }
}
```

**Rules**:
- `perfil_completo` becomes `true` only when `nome`, `whatsapp`, `cidade`, and `bairro` are present and valid
- Geographic normalization happens on save
- `consentimento_ia` controls whether Gemini can be called for AI classification
- `consentimento_ia` defaults to `false`, does not block onboarding completion, and can be changed later through this endpoint or the dedicated consent endpoint below
- The frontend should prioritize a curated Santa Catarina city list for suggestions, but the API still accepts normalized text values

---

## PATCH /usuarios/me/consentimento-ia

Toggle AI consent without resending the full profile payload.

### Request

```http
PATCH /api/v1/usuarios/me/consentimento-ia
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "consentimento_ia": true
}
```

### Response

**HTTP 200 OK**
```json
{
  "id": "user-uuid-1234567890",
  "email": "joao@example.com",
  "nome": "Joao Pedro Silva",
  "whatsapp": "+5548988888888",
  "cidade": "SAO JOSE",
  "bairro": "CENTRO",
  "instituicao": "Escola Municipal ABC",
  "perfil_completo": true,
  "consentimento_ia": true,
  "role": "USER",
  "necessidades_academicas": [
    "TEXTBOOKS",
    "TEST_PREP"
  ],
  "criado_em": "2026-05-05T10:30:00",
  "atualizado_em": "2026-05-05T11:45:00"
}
```

### Error Response

**HTTP 400 Bad Request**
```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Falha de validacao",
  "field_errors": {
    "consentimento_ia": "Informe se o consentimento de IA deve ficar ativo ou nao"
  }
}
```

---

## Field Reference

| Field | Source | Writable by client | Notes |
|-------|--------|--------------------|-------|
| `id` | System | No | UUID string |
| `email` | User input | No after register | Login identity |
| `password` | User input | Yes, request-only | Never persisted raw |
| `password_hash` | System | No | Internal backend storage only |
| `nome` | User input | Yes | Required for completed profile |
| `whatsapp` | User input | Yes | Required for completed profile |
| `cidade` | User input | Yes | Required for completed profile |
| `bairro` | User input | Yes | Required for completed profile |
| `instituicao` | User input | Yes | Optional |
| `perfil_completo` | System | No | Computed from required profile fields |
| `consentimento_ia` | User input | Yes | Optional for onboarding; may change later |
| `necessidades_academicas` | User input | Yes | Optional enum set |
| `role` | System | No | `USER` or `ADMIN` |
| `token` | System | No | JWT issued after register/login |
| `expires_in` | System | No | JWT TTL in seconds |
| `criado_em` | System | No | Persisted creation timestamp |
| `atualizado_em` | System | No | Persisted update timestamp |

---

## Authentication Context

All protected endpoints require:

```http
Authorization: Bearer <jwt_token>
```

**JWT Token**:
- **Expiry**: 7 days from issuance
- **Issued by**: Backend auth service after successful register/login
- **Claims**:
  - `sub`: authenticated user email
  - `role`: authorization role
  - `perfilCompleto`: profile completion flag
  - `userId`: user UUID
  - `iat`: issued-at timestamp
  - `exp`: expiry timestamp

**Credential Rules**:
- Backend verifies `email + password` on login
- Backend persists only `password_hash`
- Raw passwords must never appear in responses, logs, or analytics
