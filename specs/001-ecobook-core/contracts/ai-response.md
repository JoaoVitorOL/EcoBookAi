# AI Response Schema

**Reference**: spec.md RF-011 through RF-021  
**Version**: 1.1  
**Date**: 2026-05-05

---

Current runtime note:
- Successful preview responses are wrapped in `{ status, message, timestamp, path, data }`.
- The JSON objects below describe the inner `data` payload returned by the current backend.

## POST /materiais/preview

Image preview endpoint for AI classification. Returns Gemini response with confidence levels and a temporary `upload_id` that remains usable even when the AI falls back to manual entry.

### Request

```http
POST /api/v1/materiais/preview
Content-Type: multipart/form-data
Authorization: Bearer <jwt_token>

[Binary image file: JPEG or PNG, ≤ 5MB]
```

**Validation**:
- MIME type must be `image/jpeg` or `image/png`
- File size must be ≤ 5MB
- User must have active session (JWT token valid)
- If `consentimento_ia = false`, Gemini is not called (returns FAILURE)

Current runtime note:
- Timeouts, malformed AI payloads and no-consent cases still return HTTP `200` with `status_ia = FAILURE`, `error_details`, and a reusable `upload_id`.

### Response

**HTTP 200 OK** - Successful preview transport and processing, including graceful manual-fallback cases

```json
{
  "status_ia": "SUCCESS",
  "upload_id": "temp-upload-uuid-20260417-abc123def",
  "best_prediction": {
    "titulo": {
      "value": "Geometria Plana 7º Ano",
      "confidence": 0.92
    },
    "disciplina": {
      "value": "MATEMATICA",
      "confidence": 0.95
    },
    "nivel_ensino": {
      "value": "FUNDAMENTAL",
      "confidence": 0.88
    },
    "ano": {
      "value": 7,
      "confidence": 0.75
    },
    "sistema_ensino": {
      "value": "ANGLO",
      "confidence": 0.92
    },
    "estado_conservacao": {
      "value": "BOM",
      "confidence": 0.81
    },
    "data_publicacao": {
      "value": 2010,
      "confidence": 0.70
    }
  },
  "error_details": {
    "timeout": false,
    "malformed_response": false,
    "missing_fields": [],
    "invalid_enums": []
  }
}
```

**Response Fields**:

| Field | Type | Description |
|-------|------|-------------|
| `status_ia` | Enum | SUCCESS \| LOW_CONFIDENCE \| FAILURE |
| `upload_id` | String | Temporary storage ID for image; used later in POST /materiais to match preview with final material |
| `best_prediction` | Object | Gemini extraction results with confidence scores |
| `error_details` | Object | Diagnostics: timeout, parse errors, missing/invalid fields |

### status_ia Values

| Value | Confidence Range | Frontend Behavior |
|-------|---|---|
| **SUCCESS** | ≥ 0.75 for all fields | Auto-fill all fields with green checkmark; fields remain editable |
| **LOW_CONFIDENCE** | 0.50–0.75 or some fields < 0.50 | Auto-fill with yellow warning icon; fields remain editable |
| **FAILURE** | < 0.50 or timeout or no consent | Leave all fields empty; require manual entry |

### Confidence Interpretation

Current runtime note:
- `SUCCESS` is only emitted when the returned prediction set is both high-confidence and structurally valid.
- `LOW_CONFIDENCE` covers partial or mixed-confidence predictions, including invalid/missing enum issues with still-usable data.
- `FAILURE` covers timeout, malformed response, no consent, missing API configuration, or no usable predictions.

- **≥ 0.90**: High confidence (green/light icon)
- **0.75–0.89**: Good confidence (green/light icon)
- **0.50–0.74**: Medium confidence (yellow warning)
- **< 0.50**: Low confidence (empty or placeholder)
- **null**: Field not extracted (render as empty)

### Example Responses

#### SUCCESS (All fields HIGH confidence)

```json
{
  "status_ia": "SUCCESS",
  "upload_id": "temp-upload-uuid-abc123",
  "best_prediction": {
    "titulo": { "value": "Matemática 7º Ano", "confidence": 0.92 },
    "disciplina": { "value": "MATEMATICA", "confidence": 0.97 },
    "nivel_ensino": { "value": "FUNDAMENTAL", "confidence": 0.94 },
    "ano": { "value": 7, "confidence": 0.88 },
    "sistema_ensino": { "value": "ANGLO", "confidence": 0.91 },
    "estado_conservacao": { "value": "BOM", "confidence": 0.85 },
    "data_publicacao": { "value": 2015, "confidence": 0.79 }
  },
  "error_details": {
    "timeout": false,
    "malformed_response": false,
    "missing_fields": [],
    "invalid_enums": []
  }
}
```

#### LOW_CONFIDENCE (Mixed confidence levels)

```json
{
  "status_ia": "LOW_CONFIDENCE",
  "upload_id": "temp-upload-uuid-def456",
  "best_prediction": {
    "titulo": { "value": "Física Aplicada", "confidence": 0.68 },
    "disciplina": { "value": "CIENCIAS", "confidence": 0.72 },
    "nivel_ensino": { "value": "MEDIO", "confidence": 0.55 },
    "ano": { "value": null, "confidence": null },
    "sistema_ensino": { "value": "OBJETIVO", "confidence": 0.61 },
    "estado_conservacao": { "value": "USADO", "confidence": 0.51 },
    "data_publicacao": { "value": 2008, "confidence": 0.48 }
  },
  "error_details": {
    "timeout": false,
    "malformed_response": false,
    "missing_fields": ["ano"],
    "invalid_enums": []
  }
}
```

#### FAILURE (Timeout)

```json
{
  "status_ia": "FAILURE",
  "upload_id": "temp-upload-uuid-ghi789",
  "best_prediction": {},
  "error_details": {
    "timeout": true,
    "malformed_response": false,
    "missing_fields": [],
    "invalid_enums": []
  }
}
```

#### FAILURE (User did not consent to AI)

```json
{
  "status_ia": "FAILURE",
  "upload_id": "temp-upload-uuid-jkl012",
  "best_prediction": {},
  "error_details": {
    "timeout": false,
    "malformed_response": false,
    "missing_fields": ["consentimento_ia"],
    "invalid_enums": []
  }
}
```

### Error Responses

**HTTP 400 Bad Request** - Invalid image format or size

```json
{
  "error": "INVALID_IMAGE",
  "message": "Image must be JPEG or PNG and ≤ 5MB",
  "details": {
    "received_mime_type": "image/gif",
    "received_size_mb": 3.2,
    "allowed_types": ["image/jpeg", "image/png"]
  }
}
```

**HTTP 403 Forbidden** - User profile incomplete

```json
{
  "error": "INCOMPLETE_PROFILE",
  "message": "Conclua seu perfil antes de acessar este recurso"
}
```

**HTTP 401 Unauthorized** - No valid JWT token

```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Um token JWT valido e obrigatorio"
}
```

---

## Gemini API Call Details

### Backend Implementation (Spring Boot)

**Service Class** (pseudocode):
```java
@Service
public class GeminiService {
    private static final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY");
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String PROMPT = """
        Analyze this educational material image and extract metadata...
        [Full prompt from spec.md]
        """;
    
    public AIResponse classifyImage(byte[] imageBytes) throws TimeoutException {
        // 1. Encode image to base64
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        
        // 2. Call Gemini API with 10s timeout
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
        
        String requestBody = String.format("""
            {
              "contents": [{
                "parts": [
                  {
                    "text": "%s"
                  },
                  {
                    "inlineData": {
                      "mimeType": "image/jpeg",
                      "data": "%s"
                    }
                  }
                ]
              }]
            }
            """, PROMPT, base64Image);
        
        // 3. Parse response and extract best_prediction
        // 4. Validate JSON structure, enums, confidence
        // 5. Set status_ia based on confidence levels
        // 6. Return AIResponse object
    }
}
```

### Error Handling (Q7 Requirements - RFC-062-065, RNF-019)

**Retry Strategy per Error Type**:

| Error | HTTP Code | Retries | Backoff | Circuit Breaker |
|-------|-----------|---------|---------|-----------------|
| Rate Limit | 429 | 3x | 1s→2s→4s (exponential) | Pause 30s after 10 failures/5min |
| Server Error | 5xx | 3x | 2s→4s→8s (conservative) | Pause 30s after 10 failures/5min |
| Timeout | (timeout) | 2x | 1s (fixed) | Pause 30s after 10 failures/5min |
| Malformed Response | (parse error) | 0x (no retry) | N/A | No circuit breaker trigger |

**Example Retry Logic**:
```java
public AIResponse classifyImageWithRetry(byte[] imageBytes) throws TimeoutException {
    int retries = 0;
    int maxRetries = 2; // Timeout: 2 retries
    
    while (retries <= maxRetries) {
        try {
            return callGemini(imageBytes);
        } catch (TimeoutException e) {
            retries++;
            if (retries > maxRetries) {
                return AIResponse.failure("Timeout after " + maxRetries + " retries");
            }
            Thread.sleep(1000); // 1s fixed delay for timeout
        } catch (HttpClientErrorException.TooManyRequests e) { // 429
            // Exponential backoff: 1s, 2s, 4s
            long delayMs = 1000L * (long) Math.pow(2, retries);
            Thread.sleep(delayMs);
            retries++;
        } catch (HttpServerErrorException e) { // 5xx
            // Conservative backoff: 2s, 4s, 8s
            long delayMs = (1000L + 1000L * retries) * 2;
            Thread.sleep(delayMs);
            retries++;
        }
    }
    return AIResponse.failure("Max retries exceeded");
}
```

### Circuit Breaker Pattern

**States**:
- **CLOSED** (normal): Requests proceed
- **OPEN** (failed): Requests fail immediately after 10+ failures in 5min window
- **HALF_OPEN** (recovering): Allow one probe request; if succeeds, transition CLOSED; if fails, reopen

**Implementation**:
```java
@Component
public class GeminiCircuitBreaker {
    private CircuitBreakerState state = CircuitBreakerState.CLOSED;
    private int failureCount = 0;
    private Instant lastFailureWindow = Instant.now();
    private static final int FAILURE_THRESHOLD = 10;
    private static final Duration WINDOW = Duration.ofMinutes(5);
    private static final Duration PAUSE = Duration.ofSeconds(30);
    
    public synchronized void recordSuccess() {
        state = CircuitBreakerState.CLOSED;
        failureCount = 0;
    }
    
    public synchronized void recordFailure() {
        if (Instant.now().minus(WINDOW).isBefore(lastFailureWindow)) {
            failureCount++;
        } else {
            failureCount = 1;
            lastFailureWindow = Instant.now();
        }
        
        if (failureCount >= FAILURE_THRESHOLD) {
            state = CircuitBreakerState.OPEN;
            // Pause 30s; allow HALF_OPEN probe after pause
        }
    }
}
```

---

## Validation Rules (Backend)

After Gemini response is received:

1. **JSON Structure**: Must contain `best_prediction` object
2. **Enum Validation**: All extracted values must match allowed enums
3. **Confidence Range**: Must be in [0.0, 1.0]
4. **Required Fields**: Must have at least 4 of 7 fields for SUCCESS
5. **Field Types**: `ano`, `data_publicacao` must be integers if present; others are strings

**Validation Pseudocode**:
```java
public AIResponse validateAndSetStatus(String geminiJson) {
    JSONObject parsed = new JSONObject(geminiJson);
    
    // Check structure
    if (!parsed.has("best_prediction")) {
        return AIResponse.failure("Missing best_prediction");
    }
    
    // Validate enums
    String disciplina = parsed.getJSONObject("best_prediction").getString("disciplina");
    if (!isValidEnum(disciplina, Enum.DISCIPLINA)) {
        return AIResponse.lowConfidence("Invalid disciplina enum");
    }
    
    // Validate confidence
    double confidence = parsed.getJSONObject("best_prediction").getDouble("confidence");
    if (confidence < 0 || confidence > 1) {
        return AIResponse.lowConfidence("Invalid confidence range");
    }
    
    // Set status based on confidence
    if (confidence >= 0.75) return AIResponse.success(...);
    if (confidence >= 0.50) return AIResponse.lowConfidence(...);
    return AIResponse.failure(...);
}
```

---

## Data Retention & Upload ID Lifecycle

**Upload ID Tracking**:

1. **Generation**: When image first uploaded to `/materiais/preview`, unique `upload_id` generated (e.g., `temp-upload-uuid-20260417-abc123def`)
2. **Temporary Storage**: Image saved temporarily with `upload_id` as filename
3. **User Review**: Frontend displays classified results; user edits as needed
4. **Confirmation**: User confirms and sends full data + `upload_id` to `POST /materiais`
5. **Promotion**: Backend validates `upload_id`, retrieves temporary image, moves to permanent storage, updates `imagem_url`
6. **Cleanup**: After final creation, the staged file is promoted to permanent storage; the upload tracking row stays linked to the material for audit trail

**Lifetime**:
- Temporary image: 24 hours (cleanup job removes unused uploads daily)
- Permanent image: 2 years (LGPD retention, then soft-delete with anonymization)
- Upload ID record: Retained indefinitely (audit trail)
