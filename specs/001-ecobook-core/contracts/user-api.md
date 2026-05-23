# User API Contracts

**Reference**: spec.md RF-001, RF-002, RF-003, RF-004  
**Version**: 2.5  
**Date**: 2026-05-23  
**Status**: Aligned with the current backend implementation, including Phase 8 privacy/LGPD endpoints, the Phase 9 reference-data catalog, and profile self-service updates

---

Current runtime note:
- Successful responses are wrapped in the standard envelope `{ status, message, timestamp, path, data }`.
- The JSON objects shown in the success examples below represent the `data` payload for readability.
- Android onboarding/profile now surface a readable in-app summary of platform terms and privacy before the user accepts the platform consent step.

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

Runtime note:
- If a previously issued JWT references an email that no longer exists in the database, the security filter also returns `401 UNAUTHORIZED` because no authenticated principal can be established for the request.

---

## GET /reference-data/material-options

Return the immutable option catalog consumed by Android discovery filters, onboarding needs and donation/edit forms.

### Request

```http
GET /api/v1/reference-data/material-options
```

### Runtime Rules

- Public endpoint; JWT is optional
- Backend caches the catalog in memory as Phase 9 reference data
- Android keeps local enum fallbacks, so temporary endpoint failure does not block the UI

### Response

**HTTP 200 OK**
```json
{
  "disciplinas": [
    { "value": "MATEMATICA", "label": "Matematica" },
    { "value": "PORTUGUES", "label": "Portugues" }
  ],
  "niveis_ensino": [
    { "value": "FUNDAMENTAL", "label": "Fundamental" },
    { "value": "MEDIO", "label": "Ensino Medio" }
  ],
  "sistemas_ensino": [
    { "value": "ANGLO", "label": "Anglo" },
    { "value": "OBJETIVO", "label": "Objetivo" }
  ],
  "estados_conservacao": [
    { "value": "NOVO", "label": "Novo" },
    { "value": "BOM", "label": "Bom" }
  ],
  "necessidades_academicas": [
    { "value": "TEXTBOOKS", "label": "Livros didaticos" },
    { "value": "TEST_PREP", "label": "Preparacao para testes" }
  ]
}
```

### Error Response

**HTTP 500 Internal Server Error**
```json
{
  "status": 500,
  "error": "INTERNAL_SERVER_ERROR",
  "message": "Nao foi possivel carregar o catalogo de referencia"
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
  "email": "joao.pedro@example.com",
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
- `email`: optional, valid format, max 255 characters, unique when changed
- `nome`: required for successful profile completion
- `whatsapp`: required for successful profile completion; must match `+55XXXXXXXXXXX`
- `cidade`: required for successful profile completion; normalized before save
- `bairro`: required for successful profile completion; normalized before save
- `instituicao`: optional
- `consentimento_ia`: optional, does not block `perfil_completo`
- `necessidades_academicas`: optional set of enum values; if omitted during a generic profile edit, the backend preserves the current stored set

### Response

**HTTP 200 OK**
```json
{
  "id": "user-uuid-1234567890",
  "email": "joao.pedro@example.com",
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

**HTTP 400 Bad Request (duplicate email)**
```json
{
  "status": 400,
  "error": "INVALID_FORMAT",
  "message": "O perfil contem campos invalidos",
  "field_errors": {
    "email": "Este email ja esta cadastrado"
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
- The Android onboarding/profile UI previews the normalized city storage value before submit
- `consentimento_ia` controls whether Gemini can be called for AI classification
- `consentimento_ia` defaults to `false`, does not block onboarding completion, and can be changed later through this endpoint, the dedicated PATCH endpoint, or the DELETE revoke endpoint below
- If `email` changes, the backend normalizes it to lowercase and the old JWT subject becomes stale; the client must perform a fresh login with the new email
- If `necessidades_academicas` is omitted in a profile edit payload, the backend keeps the currently stored values instead of wiping the set
- The frontend should use free-text city and neighborhood inputs; the API normalizes the values before persisting and using them for matching

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

## DELETE /usuarios/me/consent/ai-classification

Revoke AI consent explicitly without sending a request body.

### Request

```http
DELETE /api/v1/usuarios/me/consent/ai-classification
Authorization: Bearer <jwt_token>
```

### Runtime Rules

- Requires an authenticated user with role `USER`
- Sets `consentimento_ia = false`
- Is idempotent: if consent was already disabled, the current profile is still returned with `consentimento_ia = false`

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
  "consentimento_ia": false,
  "role": "USER",
  "necessidades_academicas": [
    "TEXTBOOKS",
    "TEST_PREP"
  ],
  "criado_em": "2026-05-05T10:30:00",
  "atualizado_em": "2026-05-21T14:10:00"
}
```

### Error Response

**HTTP 401 Unauthorized**
```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Um token JWT valido e obrigatorio"
}
```

---

## GET /usuarios/me/consent

Return the current consent summary plus the recorded history for the authenticated user.

### Request

```http
GET /api/v1/usuarios/me/consent
Authorization: Bearer <jwt_token>
```

### Response

**HTTP 200 OK**
```json
{
  "platform_consent_given": true,
  "platform_consent_given_at": "2026-05-22T18:10:00",
  "ai_consent_enabled": false,
  "ai_consent_given_at": "2026-05-22T18:25:00",
  "ai_consent_revoked_at": "2026-05-22T18:40:00",
  "history": [
    {
      "id": "consent-uuid-1",
      "consent_type": "PLATFORM",
      "status": "GIVEN",
      "created_at": "2026-05-22T18:10:00",
      "revoked_at": null
    },
    {
      "id": "consent-uuid-2",
      "consent_type": "AI_CLASSIFICATION",
      "status": "REVOKED",
      "created_at": "2026-05-22T18:40:00",
      "revoked_at": "2026-05-22T18:40:00"
    }
  ]
}
```

---

## POST /usuarios/delete

Delete the current authenticated account with password confirmation.

### Request

```http
POST /api/v1/usuarios/delete
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "password": "SenhaSegura123",
  "reason": "Nao utilizo mais a plataforma"
}
```

### Runtime Rules

- Requires an authenticated user with role `USER`
- Verifies the submitted password against the stored password hash
- Soft-deletes the current account, anonymizes profile data, revokes the active JWT, removes stored images, and clears the Android local session
- Cancels active materials and requests associated with the deleted account

### Response

**HTTP 200 OK**
```json
{
  "user_id": "user-uuid-1234567890",
  "deleted_at": "2026-05-22T19:10:00"
}
```

### Error Responses

- `400 VALIDATION_ERROR`: password missing or reason too long
- `401 UNAUTHORIZED`: invalid password
- `404 NOT_FOUND`: user not found

---

## POST /usuarios/me/export

Export the authenticated user's personal data bundle.

### Request

```http
POST /api/v1/usuarios/me/export
Authorization: Bearer <jwt_token>
```

### Runtime Rules

- Requires an authenticated user with role `USER`
- Returns a ZIP file immediately in the HTTP response
- Current ZIP entries include `profile.json`, `materials.json`, `requests.json`, `notifications.json`, `failed-notifications.json`, `consents.json`, `audit-log.json`, and `summary.json`

### Response

**HTTP 200 OK**
- `Content-Type: application/octet-stream`
- `Content-Disposition: attachment; filename="ecobook-dados-YYYY-MM-DD.zip"`

---

## Field Reference

| Field | Source | Writable by client | Notes |
|-------|--------|--------------------|-------|
| `id` | System | No | UUID string |
| `email` | User input | Yes, with reauthentication | Login identity; changing it invalidates the previous JWT session |
| `password` | User input | Yes, request-only | Never persisted raw |
| `password_hash` | System | No | Internal backend storage only |
| `nome` | User input | Yes | Required for completed profile |
| `whatsapp` | User input | Yes | Required for completed profile |
| `cidade` | User input | Yes | Required for completed profile |
| `bairro` | User input | Yes | Required for completed profile |
| `instituicao` | User input | Yes | Optional |
| `perfil_completo` | System | No | Computed from required profile fields |
| `consentimento_ia` | User input | Yes | Optional for onboarding; may change later |
| `necessidades_academicas` | User input | Yes | Optional enum set; omitted generic edits preserve the stored values |
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
