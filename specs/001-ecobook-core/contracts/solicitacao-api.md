# Solicitacao (Request/Solicitation) API Contracts

**Reference**: spec.md RF-026 through RF-031, RF-032 through RF-035  
**Version**: 1.0  
**Date**: 2026-04-17

---

## POST /solicitacoes

Create a new request for a material. Student requests material from donor.

### Request

```http
POST /api/v1/solicitacoes
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "material_id": "material-uuid-1234567890",
  "estudante_id": "user-uuid-student"
}
```

**Validation Rules**:
- `material_id`: Must exist and status must be DISPONIVEL
- `estudante_id`: Must be the authenticated user (cannot request on behalf of others)
- User must have `perfil_completo = true` → HTTP 403 if false
- Material must not already have APROVADA Solicitacao → HTTP 409 if reserved
- User cannot request their own material → HTTP 400 if donor equals student

### Response

**HTTP 201 Created**
```json
{
  "id": "solicitacao-uuid-1234567890",
  "material_id": "material-uuid-1234567890",
  "estudante_id": "user-uuid-student",
  "status": "PENDENTE",
  "contato_doador": null,
  "created_at": "2026-04-17T15:30:00Z",
  "updated_at": "2026-04-17T15:30:00Z",
  "approved_at": null,
  "expires_at": null
}
```

**Side Effects**:
- FCM notification SOLICITACAO_RECEBIDA sent to donor
- Material remains DISPONIVEL (only transitions to RESERVADO on approval)
- Multiple PENDENTE Solicitacoes can exist for same Material

### Error Responses

**HTTP 400 Bad Request** - Student cannot request own material

```json
{
  "error": "INVALID_REQUEST",
  "message": "Cannot request your own material"
}
```

**HTTP 403 Forbidden** - Profile incomplete

```json
{
  "error": "INCOMPLETE_PROFILE",
  "message": "Profile must be complete to request materials"
}
```

**HTTP 404 Not Found** - Material does not exist

```json
{
  "error": "NOT_FOUND",
  "message": "Material not found"
}
```

**HTTP 409 Conflict** - Material already reserved

```json
{
  "error": "CONFLICT",
  "message": "Material is already reserved; another request was approved for this material",
  "details": {
    "material_status": "RESERVADO",
    "reason": "Only one approved request per material allowed"
  }
}
```

---

## PATCH /solicitacoes/{id}

Update solicitacao status. Only the material donor (original creator) can approve/decline requests. Either party can cancel under valid conditions.

### Request

```http
PATCH /api/v1/solicitacoes/solicitacao-uuid-1234567890
Content-Type: application/json
Authorization: Bearer <jwt_token>

{
  "status": "APROVADA"
}
```

**Supported Status Transitions**:

| Current | Target | Actor | Rules | Side Effects |
|---|---|---|---|---|
| PENDENTE | APROVADA | Material donor only | Atomic update of both Solicitacao and Material | Material→RESERVADO, FCM SOLICITACAO_APROVADA sent, contato_doador populated, expires_at set to +14 days |
| PENDENTE | RECUSADA | Material donor only | No race condition risk | Material remains DISPONIVEL, FCM SOLICITACAO_RECUSADA sent |
| PENDENTE | CANCELADA | Either party | Student can cancel before approval | Material remains DISPONIVEL |
| APROVADA | CONCLUIDA | Material donor only | Final donation state | Material→DOADO, FCM MATERIAL_DOADO sent |
| APROVADA | CANCELADA | Either party | Donor or student can cancel | Material→DISPONIVEL, FCM SOLICITACAO_CANCELADA sent |

### Response (Approval)

**HTTP 200 OK**
```json
{
  "id": "solicitacao-uuid-1234567890",
  "material_id": "material-uuid-1234567890",
  "estudante_id": "user-uuid-student",
  "status": "APROVADA",
  "contato_doador": {
    "nome": "João Silva",
    "whatsapp": "+5548999999999"
  },
  "created_at": "2026-04-17T15:30:00Z",
  "updated_at": "2026-04-17T15:45:00Z",
  "approved_at": "2026-04-17T15:45:00Z",
  "expires_at": "2026-05-01T15:45:00Z"
}
```

**Processing**:
1. Validate state transition (HTTP 422 if invalid)
2. Verify actor is material donor (HTTP 403 if not)
3. **Atomic operation** (database lock):
   - Update Solicitacao.status = APROVADA
   - Update Material.status = RESERVADO
   - Set Solicitacao.approved_at, expires_at (14 days from now)
   - Populate contato_doador from donor profile
4. Send FCM notification SOLICITACAO_APROVADA to student
5. Return updated Solicitacao

**Lock Strategy** (Q7 - RFC-035):
```java
@Transactional(isolation = Isolating.SERIALIZABLE)
public void approveSolicitacao(String solicitacaoId) {
    // 1. SELECT Solicitacao FOR UPDATE (database lock)
    Solicitacao solicitacao = solicitacaoRepo.findByIdForUpdate(solicitacaoId);
    
    // 2. SELECT Material FOR UPDATE (lock both rows)
    Material material = materialRepo.findByIdForUpdate(solicitacao.getMaterialId());
    
    // 3. Validate Material has no other APROVADA Solicitacao
    long approvedCount = solicitacaoRepo.countByMaterialIdAndStatus(
        material.getId(), StatusSolicitacao.APROVADA);
    if (approvedCount > 0) {
        throw new ConflictException("Material already has approved request");
    }
    
    // 4. Update both entities
    solicitacao.setStatus(StatusSolicitacao.APROVADA);
    solicitacao.setApprovedAt(Instant.now());
    solicitacao.setExpiresAt(Instant.now().plus(Duration.ofDays(14)));
    solicitacao.setContatoDoador(new Contato(donor.getNome(), donor.getWhatsapp()));
    
    material.setStatus(StatusMaterial.RESERVADO);
    material.setUpdatedAt(Instant.now());
    
    // 5. Persist (atomic within transaction)
    solicitacaoRepo.save(solicitacao);
    materialRepo.save(material);
    
    // 6. Send FCM (outside transaction for retry safety)
    fcmService.sendAsync(SOLICITACAO_APROVADA, student, solicitacao);
}
```

### Response (Decline)

**HTTP 200 OK**
```json
{
  "id": "solicitacao-uuid-1234567890",
  "material_id": "material-uuid-1234567890",
  "estudante_id": "user-uuid-student",
  "status": "RECUSADA",
  "contato_doador": null,
  "created_at": "2026-04-17T15:30:00Z",
  "updated_at": "2026-04-17T15:50:00Z",
  "approved_at": null,
  "expires_at": null
}
```

**Side Effects**:
- Material remains DISPONIVEL
- Other PENDENTE requests for same Material still active
- FCM notification SOLICITACAO_RECUSADA sent to student

### Response (Completion)

**HTTP 200 OK**
```json
{
  "id": "solicitacao-uuid-1234567890",
  "material_id": "material-uuid-1234567890",
  "estudante_id": "user-uuid-student",
  "status": "CONCLUIDA",
  "contato_doador": {
    "nome": "João Silva",
    "whatsapp": "+5548999999999"
  },
  "created_at": "2026-04-17T15:30:00Z",
  "updated_at": "2026-04-17T16:00:00Z",
  "approved_at": "2026-04-17T15:45:00Z",
  "expires_at": "2026-05-01T15:45:00Z"
}
```

**Processing**:
1. Verify current status = APROVADA (HTTP 422 if not)
2. Verify actor is material donor (HTTP 403 if not)
3. **Atomic update**:
   - Update Solicitacao.status = CONCLUIDA
   - Update Material.status = DOADO
4. Send FCM notification MATERIAL_DOADO to student (includes donor contact)
5. Return updated Solicitacao

### Error Responses

**HTTP 401 Unauthorized** - Not authenticated

```json
{
  "error": "UNAUTHORIZED",
  "message": "Valid JWT token required"
}
```

**HTTP 403 Forbidden** - Not authorized (not donor for approval/completion, or not either party for cancellation)

```json
{
  "error": "FORBIDDEN",
  "message": "Only the material donor can approve or complete this request",
  "details": {
    "actor": "user-uuid-student",
    "material_donor": "user-uuid-donor",
    "action": "approve"
  }
}
```

**HTTP 404 Not Found** - Solicitacao does not exist

```json
{
  "error": "NOT_FOUND",
  "message": "Solicitacao not found"
}
```

**HTTP 409 Conflict** - Material already has approved request

```json
{
  "error": "CONFLICT",
  "message": "Cannot approve: material already has another approved request",
  "details": {
    "existing_approved_solicitacao_id": "solicitacao-uuid-other"
  }
}
```

**HTTP 422 Unprocessable Entity** - Invalid state transition

```json
{
  "error": "INVALID_STATE_TRANSITION",
  "message": "Cannot transition from RECUSADA to APROVADA",
  "details": {
    "current_status": "RECUSADA",
    "requested_status": "APROVADA",
    "reason": "RECUSADA is a terminal state"
  }
}
```

---

## GET /solicitacoes

List all requests for the current user (as donor or student).

### Request

```http
GET /api/v1/solicitacoes?status=PENDENTE&page=1&limit=20
Authorization: Bearer <jwt_token>
```

**Query Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `status` | String | No | Filter by status (PENDENTE, APROVADA, RECUSADA, CANCELADA, CONCLUIDA) |
| `page` | Integer | No | Page number (default 1) |
| `limit` | Integer | No | Results per page (default 20, max 100) |

### Response

**HTTP 200 OK**
```json
{
  "total": 12,
  "page": 1,
  "limit": 20,
  "results": [
    {
      "id": "solicitacao-uuid-1",
      "material_id": "material-uuid-1",
      "estudante_id": "user-uuid-student-1",
      "status": "PENDENTE",
      "contato_doador": null,
      "created_at": "2026-04-17T15:30:00Z",
      "updated_at": "2026-04-17T15:30:00Z",
      "approved_at": null,
      "expires_at": null,
      "material": {
        "id": "material-uuid-1",
        "titulo": "Geometria Plana 7º Ano",
        "disciplina": "MATEMATICA",
        "status": "DISPONIVEL"
      }
    },
    {
      "id": "solicitacao-uuid-2",
      "material_id": "material-uuid-2",
      "estudante_id": "user-uuid-student-2",
      "status": "APROVADA",
      "contato_doador": {
        "nome": "Maria Santos",
        "whatsapp": "+5548988888888"
      },
      "created_at": "2026-04-16T10:00:00Z",
      "updated_at": "2026-04-16T10:15:00Z",
      "approved_at": "2026-04-16T10:15:00Z",
      "expires_at": "2026-04-30T10:15:00Z",
      "material": {
        "id": "material-uuid-2",
        "titulo": "Português 8º Ano",
        "disciplina": "PORTUGUES",
        "status": "RESERVADO"
      }
    }
  ]
}
```

**Behavior**:
- Returns requests where user is either the material donor OR the student requester
- Can filter by status
- Results include basic material info for context

---

## GET /solicitacoes/{id}

Retrieve details for a single solicitacao.

### Request

```http
GET /api/v1/solicitacoes/solicitacao-uuid-1234567890
Authorization: Bearer <jwt_token>
```

### Response

**HTTP 200 OK** (Before approval)
```json
{
  "id": "solicitacao-uuid-1234567890",
  "material_id": "material-uuid-1234567890",
  "estudante_id": "user-uuid-student",
  "status": "PENDENTE",
  "contato_doador": null,
  "created_at": "2026-04-17T15:30:00Z",
  "updated_at": "2026-04-17T15:30:00Z",
  "approved_at": null,
  "expires_at": null,
  "material": {
    "id": "material-uuid-1234567890",
    "titulo": "Geometria Plana 7º Ano",
    "descricao": "Livro em bom estado",
    "disciplina": "MATEMATICA",
    "nivel_ensino": "FUNDAMENTAL",
    "ano": 7,
    "status": "DISPONIVEL"
  },
  "estudante": {
    "id": "user-uuid-student",
    "nome": "Maria Santos",
    "cidade": "FLORIANOPOLIS",
    "bairro": "CENTRO"
  }
}
```

**HTTP 200 OK** (After approval - includes contato_doador)
```json
{
  "id": "solicitacao-uuid-1234567890",
  "material_id": "material-uuid-1234567890",
  "estudante_id": "user-uuid-student",
  "status": "APROVADA",
  "contato_doador": {
    "nome": "João Silva",
    "whatsapp": "+5548999999999"
  },
  "created_at": "2026-04-17T15:30:00Z",
  "updated_at": "2026-04-17T15:45:00Z",
  "approved_at": "2026-04-17T15:45:00Z",
  "expires_at": "2026-05-01T15:45:00Z",
  "material": {
    "id": "material-uuid-1234567890",
    "titulo": "Geometria Plana 7º Ano",
    "descricao": "Livro em bom estado",
    "disciplina": "MATEMATICA",
    "nivel_ensino": "FUNDAMENTAL",
    "ano": 7,
    "status": "RESERVADO"
  },
  "estudante": {
    "id": "user-uuid-student",
    "nome": "Maria Santos",
    "cidade": "FLORIANOPOLIS",
    "bairro": "CENTRO"
  }
}
```

**Key Rule**: `contato_doador` is **only populated when status = APROVADA**; null in all other states.

### Error Responses

**HTTP 404 Not Found**
```json
{
  "error": "NOT_FOUND",
  "message": "Solicitacao not found"
}
```

---

## Expiry & Auto-Cancellation

**14-Day Reservation Window**:

When Solicitacao is approved (`status = APROVADA`):
- `expires_at` set to current time + 14 days
- Material transitions to RESERVADO

**Daily Cleanup Job**:
```sql
-- Runs daily; reverts expired reservations
UPDATE solicitacao 
SET status = 'CANCELADA', updated_at = NOW()
WHERE status = 'APROVADA' AND expires_at < NOW();

UPDATE material 
SET status = 'DISPONIVEL', updated_at = NOW()
WHERE id IN (
  SELECT DISTINCT material_id FROM solicitacao 
  WHERE status = 'CANCELADA' AND expires_at < NOW()
);
```

**Invariants Maintained**:
- Material.RESERVADO with no APROVADA Solicitacao is invalid (daily job prevents this)
- If expiry occurs, Solicitacao → CANCELADA and Material → DISPONIVEL automatically
- No manual intervention needed

---

## Performance SLA (Q6 Requirements)

**Solicitacao Approval Endpoint** (PATCH /solicitacoes/{id}):

| Latency Metric | Target | Notes |
|---|---|---|
| P95 latency | ≤ 50ms | Achieved via atomic database transaction with minimal locking scope |
| P99 latency | ≤ 150ms | Worst case: FCM dispatch delay (async, fire-and-forget) |

**Implementation Details**:
- SERIALIZABLE isolation level for atomic approval
- SELECT...FOR UPDATE on both Solicitacao and Material rows
- Database indexes on (material_id, status), (estudante_id, status)
- FCM dispatch happens asynchronously after transaction commit
