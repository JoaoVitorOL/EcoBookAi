# User API Contracts

**Reference**: spec.md RF-001, RF-002, RF-003, RF-004  
**Version**: 1.0  
**Date**: 2026-04-17

---

## POST /auth/register

Register a new user on the platform.

### Request

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "joao@example.com",
  "nome": "João Silva",
  "whatsapp": "+5548999999999"
}
```

**Validation Rules**:
- `email`: Must be unique, valid RFC 5322 format
- `nome`: 1-100 characters
- `whatsapp`: Must be E.164 format (e.g., +5548999999999)

### Response

**HTTP 201 Created**
```json
{
  "id": "user-uuid-1234567890",
  "email": "joao@example.com",
  "nome": "João Silva",
  "whatsapp": "+5548999999999",
  "cidade": null,
  "bairro": null,
  "instituicao": null,
  "perfil_completo": false,
  "consentimento_ia": false,
  "google_id": null,
  "created_at": "2026-04-17T10:30:00Z",
  "updated_at": "2026-04-17T10:30:00Z"
}
```

### Error Responses

**HTTP 400 Bad Request** - Invalid email or WhatsApp format
```json
{
  "error": "INVALID_FORMAT",
  "message": "WhatsApp must be in E.164 format (e.g., +5548999999999)",
  "field": "whatsapp"
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

## PATCH /usuarios/{id}

Update user profile information. Sets `perfil_completo = true` when all required fields are present.

### Request

```http
PATCH /api/v1/usuarios/user-uuid-1234567890
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "nome": "João Pedro Silva",
  "whatsapp": "+5548988888888",
  "cidade": "são joão",
  "bairro": "criciúma",
  "instituicao": "Escola Municipal ABC",
  "consentimento_ia": true
}
```

**Validation Rules**:
- `nome`: 1-100 characters
- `whatsapp`: E.164 format
- `cidade`: Will be normalized to uppercase, NFD decomposed, ASCII-only (e.g., "são joão" → "SAO JOAO")
- `bairro`: Will be normalized to uppercase, NFD decomposed, ASCII-only (e.g., "criciúma" → "CRICIUMA")
- `instituicao`: 0-255 characters (optional)
- `consentimento_ia`: Boolean (controls Gemini API usage)

### Response

**HTTP 200 OK**
```json
{
  "id": "user-uuid-1234567890",
  "email": "joao@example.com",
  "nome": "João Pedro Silva",
  "whatsapp": "+5548988888888",
  "cidade": "SAO JOAO",
  "bairro": "CRICIUMA",
  "instituicao": "Escola Municipal ABC",
  "perfil_completo": true,
  "consentimento_ia": true,
  "google_id": null,
  "created_at": "2026-04-17T10:30:00Z",
  "updated_at": "2026-04-17T11:45:00Z"
}
```

**Rules**:
- `perfil_completo` automatically transitions to `true` if `cidade` and `bairro` are both present
- `perfil_completo` transitions to `false` if `cidade` or `bairro` are cleared
- Geographic normalization happens on save: accents removed, uppercase applied
- `consentimento_ia` determines whether backend calls Gemini API for material classification

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

**HTTP 403 Forbidden** - User attempting to modify another user's profile
```json
{
  "error": "FORBIDDEN",
  "message": "Cannot modify another user's profile"
}
```

**HTTP 404 Not Found** - User does not exist
```json
{
  "error": "NOT_FOUND",
  "message": "User not found",
  "field": "id"
}
```

---

## GET /usuarios/me

Retrieve current authenticated user's profile.

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
  "nome": "João Pedro Silva",
  "whatsapp": "+5548988888888",
  "cidade": "SAO JOAO",
  "bairro": "CRICIUMA",
  "instituicao": "Escola Municipal ABC",
  "perfil_completo": true,
  "consentimento_ia": true,
  "google_id": "google_oauth_id_123",
  "created_at": "2026-04-17T10:30:00Z",
  "updated_at": "2026-04-17T11:45:00Z"
}
```

**Key Field Descriptions**:
- `perfil_completo`: Indicates if user can perform restricted operations (POST /materiais, POST /solicitacoes)
- `consentimento_ia`: If `false`, Gemini API will not be called for material classification
- `google_id`: OAuth2 identifier (if authentication method used)

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

| Field | Auto-Populated | Editable | Notes |
|-------|---|---|---|
| `email` | User input | No (immutable after registration) | Unique identifier |
| `nome` | User input | Yes | Free text |
| `whatsapp` | User input | Yes | E.164 format, normalized |
| `cidade` | User input | Yes | Normalized: uppercase + NFD + ASCII |
| `bairro` | User input | Yes | Normalized: uppercase + NFD + ASCII |
| `instituicao` | User input | Yes | Optional affiliation |
| `perfil_completo` | System | No | Computed from presence of cidade + bairro |
| `consentimento_ia` | User input | Yes | Controls Gemini usage |
| `google_id` | OAuth2 provider | No (set once) | Google OAuth identifier |

---

## Normalization Algorithm

**Geographic fields** (`cidade`, `bairro`) undergo this transformation:

1. **Uppercase**: "são joão" → "SAO JOAO"
2. **NFD Decomposition**: Decompose accented characters ("ã" → "a" + combining accent)
3. **ASCII Encoding**: Remove combining accents ("a" + combining accent → "a")
4. **Trim**: Remove leading/trailing whitespace

**Examples**:
- "são joão" → "SAO JOAO"
- "florianópolis" → "FLORIANOPOLIS"
- "criciúma" → "CRICIUMA"
- " centro " → "CENTRO"

**Implementation** (Java pseudocode):
```java
String normalized = Normalizer.normalize(input, Form.NFD)
    .replaceAll("[^\\p{ASCII}]", "")
    .toUpperCase()
    .trim();
```

---

## Authentication Context

All protected endpoints require `Authorization: Bearer <jwt_token>` header.

**JWT Token**:
- **Expiry**: 7 days from issuance
- **Issued by**: Auth service (Google OAuth2 or custom JWT issuer)
- **Claims**: `sub` (user ID), `email`, `iat` (issued at), `exp` (expiration)

**Example JWT Validation** (Spring Security):
```java
@RestController
public class UserController {
    @GetMapping("/usuarios/me")
    public UserDTO getCurrentUser(@AuthenticationPrincipal JwtAuthenticationToken token) {
        String userId = token.getName(); // Subject (user ID)
        // Retrieve and return user
    }
}
```
