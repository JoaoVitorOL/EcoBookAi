# Error Response Schema & HTTP Status Codes

**Reference**: spec.md Communication Contracts  
**Version**: 2.1  
**Date**: 2026-05-12  
**Status**: Aligned with the current backend implementation for the delivered Phase 1-4 runtime

---

## Standard Error Response Format

For the currently implemented backend flows, API errors follow this JSON structure:

```json
{
  "status": 400,
  "error": "ERROR_CODE_UPPERCASE",
  "message": "Descricao legivel do erro",
  "timestamp": "2026-05-12T18:00:00",
  "path": "/api/v1/endpoint",
  "field_errors": {
    "campo": "mensagem opcional"
  }
}
```

**Fields**:

| Field | Type | Always Present | Description |
|-------|------|---|---|
| `status` | Number | Yes | HTTP status code |
| `error` | String | Yes | Machine-readable error code |
| `message` | String | Yes | Human-readable description |
| `timestamp` | ISO 8601 | Yes | When error occurred (server time) |
| `path` | String | Yes | API endpoint that failed |
| `field_errors` | Object | No | Optional map of field-specific validation messages |

Implementation note:
- The current runtime does not guarantee `details`, `field`, or domain-specific nested metadata.
- Future modules may enrich payloads, but clients must treat the schema above as the stable Phase 1-4 baseline.

---

## HTTP Status Codes & Current Mappings

### 400 - Bad Request

Used for malformed requests or invalid field formats.

Common runtime codes:
- `VALIDATION_ERROR`
- `INVALID_FORMAT`

Example:

```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Falha de validacao",
  "timestamp": "2026-05-12T18:00:00",
  "path": "/api/v1/auth/register",
  "field_errors": {
    "password": "A senha deve ter entre 8 e 72 caracteres"
  }
}
```

### 401 - Unauthorized

Used for invalid credentials or missing/invalid JWT.

Common runtime codes:
- `UNAUTHORIZED`

Examples:

```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Email ou senha invalidos",
  "timestamp": "2026-05-12T18:00:00",
  "path": "/api/v1/auth/login"
}
```

```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Um token JWT valido e obrigatorio",
  "timestamp": "2026-05-12T18:00:00",
  "path": "/api/v1/usuarios/me"
}
```

### 403 - Forbidden

Used for authenticated users without permission or with incomplete profile.

Common runtime codes:
- `INCOMPLETE_PROFILE`
- `ACCESS_DENIED`

Example:

```json
{
  "status": 403,
  "error": "INCOMPLETE_PROFILE",
  "message": "Conclua seu perfil antes de acessar este recurso",
  "timestamp": "2026-05-12T18:00:00",
  "path": "/api/v1/materiais"
}
```

### 404 - Not Found

Used when the requested user or resource does not exist.

Common runtime codes:
- `NOT_FOUND`

Example:

```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Usuario nao encontrado",
  "timestamp": "2026-05-12T18:00:00",
  "path": "/api/v1/usuarios/me"
}
```

### 409 - Conflict

Used when the current resource state prevents the requested operation.

Common runtime codes:
- `CONFLICT`

Example:

```json
{
  "status": 409,
  "error": "CONFLICT",
  "message": "Este email ja esta cadastrado",
  "timestamp": "2026-05-12T18:00:00",
  "path": "/api/v1/auth/register"
}
```

### 422 - Unprocessable Entity

Used when the request shape is correct but business validation fails.

Common runtime codes:
- `UNPROCESSABLE_ENTITY`
- `INVALID_STATE_TRANSITION`

Example:

```json
{
  "status": 422,
  "error": "UNPROCESSABLE_ENTITY",
  "message": "Preencha todos os campos obrigatorios do perfil",
  "timestamp": "2026-05-12T18:00:00",
  "path": "/api/v1/usuarios/me",
  "field_errors": {
    "bairro": "Informe seu bairro",
    "cidade": "Informe sua cidade"
  }
}
```

### 500 - Internal Server Error

Used for unexpected failures.

Common runtime codes:
- `INTERNAL_SERVER_ERROR`

Example:

```json
{
  "status": 500,
  "error": "INTERNAL_SERVER_ERROR",
  "message": "Ocorreu um erro inesperado",
  "timestamp": "2026-05-12T18:00:00",
  "path": "/api/v1/endpoint"
}
```
