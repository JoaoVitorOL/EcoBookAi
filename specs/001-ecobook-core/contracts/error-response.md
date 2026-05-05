# Error Response Schema & HTTP Status Codes

**Reference**: spec.md Communication Contracts  
**Version**: 1.0  
**Date**: 2026-04-17

---

## Standard Error Response Format

All API errors follow this JSON structure:

```json
{
  "error": "ERROR_CODE_UPPERCASE",
  "message": "Human-readable error description in Portuguese",
  "field": "field_name_if_applicable",
  "details": {
    "additional": "context_specific_information"
  },
  "timestamp": "2026-04-17T16:30:00Z",
  "path": "/api/v1/endpoint"
}
```

**Fields**:

| Field | Type | Always Present | Description |
|-------|------|---|---|
| `error` | String | ✅ | Machine-readable error code (enum) |
| `message` | String | ✅ | Human-readable description (Portuguese) |
| `field` | String | ❌ | Optional field name if error is field-specific |
| `details` | Object | ❌ | Optional context-specific data (nested error info) |
| `timestamp` | ISO 8601 | ✅ | When error occurred (server time) |
| `path` | String | ✅ | API endpoint that failed |

---

## HTTP Status Codes & Error Mappings

### 400 - Bad Request

Invalid request format, malformed JSON, invalid enum value, or validation error.

**Common Error Codes**:

| Error Code | Message | Details |
|---|---|---|
| `INVALID_JSON` | Request JSON is malformed | `{ "parse_error": "Unexpected token at line X" }` |
| `INVALID_ENUM` | Invalid enum value for field | `{ "received_value": "X", "allowed_values": ["A", "B"], "field": "disciplina" }` |
| `INVALID_FORMAT` | Invalid format for field (email, phone, etc) | `{ "field": "whatsapp", "format": "E.164", "example": "+5548999999999" }` |
| `INVALID_RANGE` | Value outside allowed range | `{ "field": "ano", "min": 1, "max": 12, "received": 15 }` |
| `VALIDATION_ERROR` | Generic validation failure | `{ "reason": "City and neighborhood required" }` |
| `MISSING_FIELD` | Required field not provided | `{ "field": "titulo" }` |
| `DUPLICATE_EMAIL` | Email already registered | `{ "field": "email", "existing_user": "user-uuid" }` |

**Example Response** (HTTP 400):
```json
{
  "error": "INVALID_ENUM",
  "message": "Campo disciplina contém valor inválido",
  "field": "disciplina",
  "details": {
    "received_value": "MUSICA",
    "allowed_values": ["MATEMATICA", "PORTUGUES", "HISTORIA", "GEOGRAFIA", "CIENCIAS", "LITERATURA"],
    "valid_format": "Use uma das disciplinas permitidas"
  },
  "timestamp": "2026-04-17T16:30:00Z",
  "path": "/api/v1/materiais"
}
```

---

### 401 - Unauthorized

Missing, invalid, or expired authentication token.

**Common Error Codes**:

| Error Code | Message | Details |
|---|---|---|
| `UNAUTHORIZED` | No valid JWT token provided | `{ "header": "Authorization", "format": "Bearer <jwt_token>" }` |
| `TOKEN_EXPIRED` | JWT token has expired | `{ "expired_at": "2026-04-17T16:00:00Z", "action": "Login again" }` |
| `TOKEN_INVALID` | JWT token is invalid or forged | `{ "reason": "Signature verification failed" }` |
| `SESSION_EXPIRED` | Session has timed out | `{ "session_ttl_seconds": 604800 }` |

**Example Response** (HTTP 401):
```json
{
  "error": "UNAUTHORIZED",
  "message": "Token JWT válido é obrigatório",
  "details": {
    "header": "Authorization",
    "format": "Bearer <jwt_token>",
    "login_endpoint": "/api/v1/auth/login"
  },
  "timestamp": "2026-04-17T16:30:00Z",
  "path": "/api/v1/usuarios/me"
}
```

---

### 403 - Forbidden

User is authenticated but not authorized to perform action, or resource is restricted.

**Common Error Codes**:

| Error Code | Message | Details |
|---|---|---|
| `INCOMPLETE_PROFILE` | User profile not fully completed | `{ "perfil_completo": false, "missing_fields": ["cidade", "bairro"] }` |
| `FORBIDDEN` | User lacks permission for action | `{ "reason": "Only material donor can approve requests" }` |
| `CONSENT_REQUIRED` | User has not given required consent | `{ "consent_type": "consentimento_ia", "action": "Enable AI classification" }` |

**Example Response** (HTTP 403):
```json
{
  "error": "INCOMPLETE_PROFILE",
  "message": "Perfil deve estar completo para fazer upload de materiais",
  "details": {
    "perfil_completo": false,
    "missing_fields": ["cidade", "bairro"],
    "required_fields": ["nome", "email", "whatsapp", "cidade", "bairro"]
  },
  "timestamp": "2026-04-17T16:30:00Z",
  "path": "/api/v1/materiais"
}
```

---

### 404 - Not Found

Resource does not exist.

**Common Error Codes**:

| Error Code | Message | Details |
|---|---|---|
| `NOT_FOUND` | Resource not found | `{ "resource_type": "material", "id": "material-uuid-123" }` |
| `USER_NOT_FOUND` | User does not exist | `{ "user_id": "user-uuid-456" }` |
| `MATERIAL_NOT_FOUND` | Material does not exist | `{ "material_id": "material-uuid-123" }` |
| `UPLOAD_NOT_FOUND` | Temporary upload not found (expired) | `{ "upload_id": "temp-uuid-789", "expires_in_hours": 24 }` |

**Example Response** (HTTP 404):
```json
{
  "error": "MATERIAL_NOT_FOUND",
  "message": "Material não encontrado",
  "details": {
    "material_id": "material-uuid-123",
    "suggestion": "Verifique se o ID está correto ou se o material foi cancelado"
  },
  "timestamp": "2026-04-17T16:30:00Z",
  "path": "/api/v1/materiais/material-uuid-123"
}
```

---

### 409 - Conflict

Request conflicts with current state (e.g., duplicate record, material already reserved).

**Common Error Codes**:

| Error Code | Message | Details |
|---|---|---|
| `CONFLICT` | Resource state prevents operation | `{ "current_state": "RESERVADO", "reason": "Material already has approved request" }` |
| `MATERIAL_RESERVED` | Material already has approved request | `{ "approved_solicitacao_id": "solicitacao-uuid-456" }` |
| `DUPLICATE_REQUEST` | User already has request for this material | `{ "existing_solicitacao_id": "solicitacao-uuid-789" }` |

**Example Response** (HTTP 409):
```json
{
  "error": "CONFLICT",
  "message": "Material já está reservado",
  "details": {
    "material_id": "material-uuid-123",
    "material_status": "RESERVADO",
    "approved_solicitacao_id": "solicitacao-uuid-456",
    "approved_by": "user-uuid-999",
    "reason": "Um pedido já foi aprovado para este material"
  },
  "timestamp": "2026-04-17T16:30:00Z",
  "path": "/api/v1/solicitacoes"
}
```

---

### 422 - Unprocessable Entity

Request is well-formed but cannot be processed due to invalid state transition or business logic violation.

**Common Error Codes**:

| Error Code | Message | Details |
|---|---|---|
| `INVALID_STATE_TRANSITION` | Cannot transition to requested state | `{ "current_status": "DOADO", "requested_status": "CANCELADO", "reason": "Terminal state" }` |
| `INVALID_OPERATION` | Operation not allowed in current state | `{ "operation": "approve", "current_state": "RECUSADA", "reason": "Cannot transition from terminal state" }` |

**Example Response** (HTTP 422):
```json
{
  "error": "INVALID_STATE_TRANSITION",
  "message": "Transição de estado não permitida",
  "details": {
    "entity": "Solicitacao",
    "current_status": "RECUSADA",
    "requested_status": "APROVADA",
    "reason": "RECUSADA é um estado final e não pode transicionar",
    "allowed_transitions": ["CONCLUIDA", "CANCELADA"]
  },
  "timestamp": "2026-04-17T16:30:00Z",
  "path": "/api/v1/solicitacoes/solicitacao-uuid-123"
}
```

---

### 429 - Too Many Requests

Rate limit exceeded.

**Common Error Codes**:

| Error Code | Message | Details |
|---|---|---|
| `RATE_LIMIT_EXCEEDED` | Request rate limit exceeded | `{ "limit": 10, "window": "1 hour", "retry_after": 3600 }` |

**Example Response** (HTTP 429):
```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Limite de requisições atingido",
  "details": {
    "endpoint": "/api/v1/materiais",
    "limit": 100,
    "window_seconds": 60,
    "reset_at": "2026-04-17T16:31:00Z"
  },
  "timestamp": "2026-04-17T16:30:00Z",
  "path": "/api/v1/materiais"
}
```

**Header**: Include `Retry-After` header
```
Retry-After: 60
```

---

### 500 - Internal Server Error

Unexpected server error.

**Common Error Codes**:

| Error Code | Message | Details |
|---|---|---|
| `INTERNAL_ERROR` | Unexpected server error | `{ "error_id": "err-2026-04-17-12345", "support": "support@ecobook.com" }` |
| `DATABASE_ERROR` | Database connection or query failed | `{ "error_id": "err-2026-04-17-12346", "action": "Retry request" }` |
| `EXTERNAL_SERVICE_ERROR` | Gemini or FCM call failed | `{ "service": "gemini", "error_id": "err-2026-04-17-12347" }` |

**Example Response** (HTTP 500):
```json
{
  "error": "INTERNAL_ERROR",
  "message": "Erro interno do servidor. Tente novamente mais tarde.",
  "details": {
    "error_id": "err-2026-04-17-165430-abc123",
    "timestamp_ms": 1713372630000,
    "support_email": "support@ecobook.com",
    "status_page": "https://status.ecobook.com"
  },
  "timestamp": "2026-04-17T16:30:00Z",
  "path": "/api/v1/materiais"
}
```

---

## Error Handling Best Practices

### Client-Side (Android)

```kotlin
// In Retrofit error interceptor
class ErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        
        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
            
            when (response.code) {
                400 -> handleBadRequest(errorResponse)
                401 -> handleUnauthorized(errorResponse)
                403 -> handleForbidden(errorResponse)
                404 -> handleNotFound(errorResponse)
                409 -> handleConflict(errorResponse)
                422 -> handleUnprocessable(errorResponse)
                429 -> handleRateLimit(errorResponse)
                else -> handleServerError(errorResponse)
            }
        }
        
        return response
    }
    
    private fun handleUnauthorized(error: ErrorResponse) {
        // Clear JWT token, redirect to login
        preferences.removeJWTToken()
        navController.navigate("login")
    }
    
    private fun handleRateLimit(error: ErrorResponse) {
        // Show toast: "Too many requests, please try again later"
        // Calculate retry time from error.details.retry_after
    }
}
```

### Backend (Spring Boot)

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(InvalidEnumException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEnum(InvalidEnumException e) {
        ErrorResponse error = new ErrorResponse(
            "INVALID_ENUM",
            "Campo contém valor inválido",
            e.getFieldName(),
            Map.of(
                "received_value", e.getValue(),
                "allowed_values", e.getAllowedValues()
            )
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidStateTransitionException e) {
        ErrorResponse error = new ErrorResponse(
            "INVALID_STATE_TRANSITION",
            "Transição de estado não permitida",
            null,
            Map.of(
                "current_status", e.getCurrentStatus(),
                "requested_status", e.getRequestedStatus(),
                "reason", e.getReason()
            )
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }
}
```

---

## Error Logging

**Backend Logging** (Spring Boot):

```java
@Service
public class ErrorLoggingService {
    private static final Logger logger = LoggerFactory.getLogger(ErrorLoggingService.class);
    
    public void logError(String errorCode, String message, Exception e, String userId, String path) {
        logger.error(
            "Error [{}] for user [{}] on path [{}]: {}",
            errorCode, userId, path, message, e
        );
        
        // Send to external logging service (ELK, DataDog, etc.) - Phase 2
        // metricsService.recordError(errorCode, userId);
    }
}
```

**Client Logging** (Android):

```kotlin
// Firebase Crashlytics integration (optional V2)
FirebaseCrashlytics.getInstance().recordException(exception)
```

---

## Testing Error Scenarios

**Error Test Cases**:

```java
@Test
void testInvalidEnumReturns400() {
    String body = "{\"disciplina\": \"MUSICA\"}";
    mockMvc.perform(post("/api/v1/materiais")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_ENUM"))
        .andExpect(jsonPath("$.details.allowed_values", hasItems("MATEMATICA", "PORTUGUES")));
}

@Test
void testInvalidStateTransitionReturns422() {
    // Create material in DOADO state, try to transition to CANCELADO
    mockMvc.perform(patch("/api/v1/materiais/{id}", materialId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\": \"CANCELADO\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.error").value("INVALID_STATE_TRANSITION"));
}

@Test
void testIncompleteProfileReturns403() {
    mockMvc.perform(post("/api/v1/materiais")
            .header("Authorization", "Bearer " + incompleteUserToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(materialJson))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("INCOMPLETE_PROFILE"));
}
```
