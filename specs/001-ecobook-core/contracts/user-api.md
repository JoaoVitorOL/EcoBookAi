# User API Contracts

**Reference**: spec.md RF-001, RF-002, RF-003, RF-004  
**Version**: 2.0  
**Date**: 2026-05-05

---

## POST /auth/register

Create a new user account with local credentials.

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
- `email`: Must be unique, valid RFC 5322 format
- `password`: Minimum 8 characters
- `nome`: 1-100 characters

**Security Rules**:
- Password is never returned in API responses
- Backend stores only `password_hash`
- Successful registration issues JWT immediately

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
  "expires_in": 604800,
  "created_at": "2026-05-05T10:30:00Z",
  "updated_at": "2026-05-05T10:30:00Z"
}
```

### Error Responses

**HTTP 400 Bad Request** - Invalid email or password format
```json
{
  "error": "INVALID_FORMAT",
  "message": "Password must have at least 8 characters",
  "field": "password"
}
```

**HTTP 409 Conflict** - Email already exists
```json
{
  "error": "DUPLICATE_EMAIL",
  "message": "Email already registered",
  "field": "email"
}
```

---

## POST /auth/login

Authenticate an existing user with email and password.

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
  "expires_in": 604800,
  "created_at": "2026-05-05T10:30:00Z",
  "updated_at": "2026-05-05T11:45:00Z"
}
```

### Error Responses

**HTTP 401 Unauthorized** - Invalid credentials
```json
{
  "error": "INVALID_CREDENTIALS",
  "message": "Email or password is invalid"
}
```

---

## PUT /usuarios/me

Update the current authenticated user's profile. Sets `perfil_completo = true` when all required profile fields are present.

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
  "consentimento_ia": true
}
```

**Validation Rules**:
- `nome`: 1-100 characters
- `whatsapp`: E.164 format
- `cidade`: Normalized to uppercase, NFD-decomposed, ASCII-only
- `bairro`: Normalized to uppercase, NFD-decomposed, ASCII-only
- `instituicao`: 0-255 characters
- `consentimento_ia`: Boolean

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
  "created_at": "2026-05-05T10:30:00Z",
  "updated_at": "2026-05-05T11:45:00Z"
}
```

**Rules**:
- `perfil_completo` becomes `true` when `nome`, `whatsapp`, `cidade`, and `bairro` are present
- Geographic normalization happens on save
- `consentimento_ia` controls whether Gemini is called for classification

### Error Responses

**HTTP 401 Unauthorized** - No valid JWT token
```json
{
  "error": "UNAUTHORIZED",
  "message": "Valid JWT token required"
}
```

**HTTP 400 Bad Request** - Invalid format or missing required field
```json
{
  "error": "INVALID_FORMAT",
  "message": "City and neighborhood are required for profile completion",
  "field": "cidade"
}
```

---

## GET /usuarios/me

Retrieve the current authenticated user's profile.

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
  "nome": "Joao Pedro Silva",
  "whatsapp": "+5548988888888",
  "cidade": "SAO JOSE",
  "bairro": "CENTRO",
  "instituicao": "Escola Municipal ABC",
  "perfil_completo": true,
  "consentimento_ia": true,
  "role": "USER",
  "created_at": "2026-05-05T10:30:00Z",
  "updated_at": "2026-05-05T11:45:00Z"
}
```

### Error Responses

**HTTP 401 Unauthorized** - No valid JWT token or session expired
```json
{
  "error": "UNAUTHORIZED",
  "message": "Valid JWT token required"
}
```

---

## Field Classification Reference

| Field | Source | Editable | Notes |
|-------|--------|----------|-------|
| `email` | User input | No (immutable after registration) | Unique login identity |
| `password` | User input | Yes, via future password-change flow | Never returned by API |
| `password_hash` | System | No | Internal backend field only |
| `nome` | User input | Yes | Free text |
| `whatsapp` | User input | Yes | E.164 format |
| `cidade` | User input | Yes | Normalized on save |
| `bairro` | User input | Yes | Normalized on save |
| `instituicao` | User input | Yes | Optional |
| `perfil_completo` | System | No | Computed from required profile fields |
| `consentimento_ia` | User input | Yes | Controls Gemini usage |
| `token` | System | No | JWT issued by backend |

---

## Authentication Context

All protected endpoints require `Authorization: Bearer <jwt_token>`.

**JWT Token**:
- **Expiry**: 7 days from issuance
- **Issued by**: Backend auth service after successful register/login
- **Claims**: `sub` (user ID), `email`, `role`, `perfil_completo`, `iat`, `exp`

**Credential Rules**:
- Backend verifies email + password on login
- Backend persists only `password_hash`
- Raw password must never appear in logs, DTO responses, or analytics
