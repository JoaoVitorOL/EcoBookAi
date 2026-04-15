# API Contracts Index

**Phase**: 1 (Design & Contracts)  
**Status**: Ready for detailed contract specifications in Phase 2  
**Purpose**: Define interface contracts for all REST API endpoints

---

## Contracts to Be Generated

This directory will contain detailed API contract specifications. Each contract defines:
- HTTP method
- Path
- Request schema (JSON body, query parameters, headers)
- Response schema (success & error cases)
- HTTP status codes
- Business rules and validation
- Related functional requirements

### Contract Files (to be created in Phase 2)

1. **material-api.md**
   - POST /materiais (create material)
   - GET /materiais (search/list materials)
   - GET /materiais/{id} (get material details)
   - PATCH /materiais/{id} (update material status)

2. **solicitacao-api.md**
   - POST /solicitacoes (create request)
   - GET /solicitacoes (list requests for user)
   - GET /solicitacoes/{id} (get request details)
   - PATCH /solicitacoes/{id} (update request status: approve, decline, cancel, complete)

3. **user-api.md**
   - POST /auth/register (user registration)
   - POST /auth/login (OAuth2 flow)
   - GET /usuarios/me (get current user)
   - PATCH /usuarios/{id} (update profile)

4. **ai-response.md**
   - POST /materiais/preview (Gemini classification)
   - Response structure with status_ia, best_prediction, upload_id
   - Confidence scoring and validation

5. **notification-schema.md**
   - FCM payload schemas for 6 notification types
   - SOLICITACAO_RECEBIDA
   - SOLICITACAO_APROVADA
   - SOLICITACAO_RECUSADA
   - SOLICITACAO_CANCELADA
   - MATERIAL_DOADO
   - MATERIAL_CANCELADO

6. **error-response.md**
   - Standard error envelope
   - Error codes and messages
   - HTTP status code mapping

---

## Reference Data

### Request/Response Templates

All contracts use JSON request/response bodies with the following structure:

**Success Response** (2xx):
```json
{
  "id": "uuid",
  "field1": "value",
  "nested": {
    "field2": "value"
  },
  "status": "enum_value",
  "created_at": "2026-04-15T10:30:00Z"
}
```

**Error Response** (4xx, 5xx):
```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable error message",
  "field": "field_name_if_applicable",
  "details": {
    "allowed_values": ["value1", "value2"],
    "received_value": "invalid"
  }
}
```

---

## Endpoint Summary

| Method | Path | Purpose | RFC | Status |
|--------|------|---------|-----|--------|
| POST | /auth/register | Register new user | RF-001 | To specify |
| POST | /auth/login | OAuth2 login | RF-001 | To specify |
| GET | /usuarios/me | Get current user | RF-002 | To specify |
| PATCH | /usuarios/{id} | Update profile | RF-002, RF-003 | To specify |
| POST | /materiais | Create material | RF-008 to RF-010 | To specify |
| POST | /materiais/preview | AI classification | RF-011 to RF-021 | To specify |
| GET | /materiais | Search materials | RF-022 to RF-025 | To specify |
| GET | /materiais/{id} | Get material details | RF-008 | To specify |
| PATCH | /materiais/{id} | Update material status | RF-005 to RF-006 | To specify |
| POST | /solicitacoes | Create request | RF-026 to RF-027 | To specify |
| GET | /solicitacoes | List requests | RF-026 | To specify |
| GET | /solicitacoes/{id} | Get request details | RF-026 | To specify |
| PATCH | /solicitacoes/{id} | Update request status | RF-028 to RF-031 | To specify |
| GET | /notificacoes | List notifications | RF-037 | Optional |

---

## Phase 2 Task

**Owner**: Backend Architect + Android Architect  
**Duration**: ~3 days (Weeks 3–4)  
**Deliverable**: 6 contract files in this directory

Each contract must:
1. Include all required HTTP status codes (200, 201, 400, 403, 404, 409, 422, 500)
2. Reference corresponding RFCs from spec.md
3. Show example success and error responses
4. List all validation rules
5. Document error scenarios

**Template for contract files** (use consistent format):

```markdown
# [API Name] Contracts

## Endpoint 1: [METHOD] [PATH]

**RFC**: RF-XXX  
**Description**: [What this endpoint does]

### Request

**Method**: [GET|POST|PATCH|DELETE]  
**Path**: [/path/{id}]  
**Authentication**: Required (Bearer JWT)

**Headers**:
- `Authorization: Bearer <token>`
- `Content-Type: application/json`

**Body**:
\`\`\`json
{
  "field1": "string",
  "field2": "integer"
}
\`\`\`

**Validation**:
- field1: Required, max 255 chars
- field2: Required, range 1–100

### Response

**Success (HTTP 200/201)**:
\`\`\`json
{
  "id": "uuid",
  "field1": "string",
  "created_at": "2026-04-15T10:30:00Z"
}
\`\`\`

**Error (HTTP 400)**:
\`\`\`json
{
  "error": "INVALID_ENUM",
  "message": "Invalid value for field1",
  "field": "field1",
  "details": {
    "allowed_values": ["VALUE1", "VALUE2"],
    "received_value": "INVALID"
  }
}
\`\`\`

**Error (HTTP 403)**:
\`\`\`json
{
  "error": "INCOMPLETE_PROFILE",
  "message": "Profile must be completed before this operation",
  "field": "perfil_completo"
}
\`\`\`

### Business Rules

- [Rule 1]
- [Rule 2]

### HTTP Codes

- 200 OK: Success
- 201 Created: Resource created
- 400 Bad Request: Validation error
- 403 Forbidden: Profile incomplete
- 404 Not Found: Resource not found
- 409 Conflict: Duplicate or state conflict
- 422 Unprocessable Entity: Invalid state transition
- 500 Internal Server Error: Server error

---
```

---

## Next Steps

1. **Phase 2 Start (Week 5)**:
   - Backend Architect reviews this index
   - Assigns contract files to team members
   - Creates detailed contracts using template above
   - Updates OpenAPI/Swagger specification

2. **Phase 2 End (Week 7)**:
   - All 6 contract files complete
   - Code review and approval
   - Shared with frontend and backend teams
   - Used as basis for controller/API client implementation

3. **Phase 3–4**:
   - Contracts validated against actual implementation
   - Contract-first testing (validate API responses against contracts)
   - Contract updates if requirements change

---

## Contract Maintenance

**When to update contracts**:
- RFC changes or clarifications
- New error scenarios discovered
- Response schema adjustments
- Performance or security requirements added

**Process**:
1. Proposal documented
2. Code review + product approval
3. Backend implementation updated
4. Contract file updated
5. Git commit with message: "docs: update contract [name] for [reason]"

