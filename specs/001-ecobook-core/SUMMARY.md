# EcoBook IA Specification - Generation Summary

**Date**: 2026-04-15  
**Status**: ✅ Complete and Ready for Planning  

---

## Generated Artifacts

### 1. Core Specification Document
📄 **Location**: [specs/001-ecobook-core/spec.md](../spec.md)

**Contents**:
- ✅ All 7 enum definitions (Disciplina, NivelEnsino, SistemaEnsino, EstadoConservacao, StatusMaterial, StatusSolicitacao, StatusRespostaIA)
- ✅ Complete REST API specification (15+ endpoints with methods, RFCs, business rules, HTTP codes)
- ✅ All 7 JSON communication contracts (AI response, Material, Solicitacao, errors, 6 FCM notification types, academic need structure)
- ✅ Material matching algorithm (complete 6-step pipeline with geographic ranking and special rules)
- ✅ Image processing pipeline (10-step workflow from user selection to permanent storage)
- ✅ AI confidence fallback rules (6 scenarios from SUCCESS to FAILURE)
- ✅ AI response parsing rules (7 validation criteria)
- ✅ Material state machine (4 states + valid transitions + HTTP 422 for invalid)
- ✅ Solicitacao state machine (5 states + terminal states + expiry rules)
- ✅ Consistency invariants (5 core invariants ensuring data integrity)
- ✅ Geographic normalization rules (uppercase + NFD + ASCII, with examples)
- ✅ Onboarding & profile completeness rules (perfil_completo tracking, HTTP 403 blocking)
- ✅ WhatsApp validation (E.164 format enforcement)
- ✅ Image upload rules (JPEG/PNG only, ≤5MB, MIME validation, mediated access)
- ✅ consentimento_ia behavior (no Gemini call when false, returns FAILURE)
- ✅ Gemini MVP prompt template
- ✅ All RF-001 through RF-043 functional requirements
- ✅ All RNF-001 through RNF-005 non-functional requirements

### 2. Quality Checklist
📋 **Location**: [specs/001-ecobook-core/checklists/requirements.md](requirements.md)

**Status**: ✅ **ALL ITEMS PASS**

- Content quality: Business-focused, no implementation leakage
- Requirement completeness: 43 FR + 5 NFR, all testable
- Success criteria: 10 measurable outcomes covering speed, accuracy, reliability, adoption
- Assumptions: 12 documented, reasonable defaults
- State machines: Fully defined with transitions and rules
- API contracts: 7 complete JSON schemas
- User scenarios: 8 independent user stories (P1/P2) with acceptance criteria

### 3. Feature Metadata
📄 **Location**: [.specify/feature.json](.specify/feature.json)

```json
{
  "feature_directory": "specs/001-ecobook-core"
}
```

---

## Key Specification Highlights

### Comprehensive Scope Coverage

✅ **User Management**: Registration, profile completion, consent tracking  
✅ **Material Lifecycle**: DISPONIVEL → RESERVADO → DOADO (or CANCELADO)  
✅ **Request Workflow**: PENDENTE → APROVADA → CONCLUIDA (or RECUSADA/CANCELADA)  
✅ **AI Classification**: Gemini integration with confidence-based fallbacks  
✅ **Matching Algorithm**: 6-step pipeline with geographic ranking  
✅ **Notifications**: 6 FCM event types for key state transitions  
✅ **State Machines**: Explicit valid/invalid transitions with HTTP 422 enforcement  
✅ **Data Consistency**: 5 core invariants with atomic approval locks  
✅ **Geographic Normalization**: Consistent uppercase/ASCII representation  
✅ **Image Processing**: 10-step pipeline from upload to permanent storage  

### Technical Contracts

**All communication contracts specified in JSON**:
- ✅ POST /materiais/preview response (status_ia, best_prediction, upload_id)
- ✅ POST /materiais request/response (Material entity with all fields)
- ✅ GET /solicitacoes/{id} response (contato_doador populated only when APROVADA)
- ✅ 6 FCM notification payloads (SOLICITACAO_RECEBIDA, APROVADA, RECUSADA, CANCELADA, MATERIAL_DOADO, MATERIAL_CANCELADO)
- ✅ Error response structure (error, message, field, details)

### Business Rules Encoded

**Matching Algorithm**: 
- Filter DISPONIVEL → Filter disciplina (exact) → Filter nivel_ensino (exact) → Filter year range (±1, except SUPERIOR) → Filter sistema_ensino (exact, OUTRO special rule) → Rank by proximity + recency

**Confidence Fallbacks**:
- ≥0.75: Auto-fill, disabled edit, SUCCESS
- 0.50–0.75: Auto-fill, warning, editable, LOW_CONFIDENCE
- <0.50: Empty, manual entry required, LOW_CONFIDENCE
- Timeout/Failure: Empty, manual entry required, FAILURE

**State Transitions**:
- Material: DISPONIVEL ↔ RESERVADO (14-day expiry) → DOADO (final) or CANCELADO (final)
- Solicitacao: PENDENTE → APROVADA → CONCLUIDA (final) or RECUSADA (final) or CANCELADA (final)

**Invariants**:
- Material RESERVADO = exactly 1 APROVADA Solicitacao
- Material DOADO = exactly 1 CONCLUIDA Solicitacao
- Material DISPONIVEL = NO APROVADA Solicitacao
- Approval is atomic (database lock required)
- Geographic data always normalized

---

## User Stories (8 Total, P1/P2)

| Story | Title | Priority | Key Workflow |
|-------|-------|----------|---|
| 1 | Donor Registration & Material Profile | P1 | Register → Complete Profile → Create Material |
| 2 | AI Classification | P1 | Upload Image → Gemini → Confidence Fallback → Review |
| 3 | Student Material Discovery | P1 | Profile Needs → Query → Ranking Algorithm → Results |
| 4 | Student Request | P1 | View Material → Submit Request → Donor Notification |
| 5 | Donation Completion | P2 | Approve Request → Mark DOADO → Send Contact |
| 6 | Material Cancellation | P2 | Cancel at Various Stages → Cascade Effects |
| 7 | Profile Onboarding | P1 | Incomplete Profile → HTTP 403 → Complete → Access |
| 8 | Geographic Normalization | P1 | Input Variations → Normalized Storage → Consistent Matching |

---

## Success Criteria (10 Total, Measurable)

| ID | Criterion | Metric |
|----|-----------|--------|
| SC-001 | Material Discovery Speed | P95 latency < 2 seconds |
| SC-002 | Material Upload Time | 3 minutes end-to-end |
| SC-003 | Matching Algorithm Accuracy | Zero inconsistencies across repeated queries |
| SC-004 | AI Classification Success | ≥75% SUCCESS, ≤25% LOW_CONFIDENCE/FAILURE |
| SC-005 | Approval Workflow Speed | <10 seconds state → state → FCM |
| SC-006 | Concurrent Users | ≥10,000 without degradation |
| SC-007 | Image Validation | 100% invalid files rejected, zero orphaned uploads |
| SC-008 | FCM Reliability | ≥95% delivery within 5 seconds |
| SC-009 | Expiry Automation | Zero missed 14-day expirations |
| SC-010 | User Onboarding | ≤15% abandonment, ≥85% complete in 24h |

---

## Next Steps

### Ready for `/speckit.plan`

The specification is complete and can now be decomposed into:
1. **Epic-level tasks** (user stories broken into implementation chunks)
2. **Technical tasks** (database schema, API implementation, AI integration)
3. **Testing tasks** (unit tests, integration tests, state machine validation)
4. **Deployment tasks** (release process, rollout strategy)

### No Clarification Required

- All requirements are unambiguous and testable
- No [NEEDS CLARIFICATION] markers remain
- Assumptions are reasonable and documented
- Scope is clearly bounded (MVP context for Android-only, Brazilian market, no web/analytics/reputation)

---

## Specification Statistics

| Metric | Count |
|--------|-------|
| Functional Requirements | 43 |
| Non-Functional Requirements | 5 |
| User Stories | 8 |
| Success Criteria | 10 |
| REST Endpoints | 15+ |
| Enum Types | 7 |
| Entity Types | 5 |
| JSON Contracts | 7 |
| FCM Notification Types | 6 |
| State Transitions | 12+ |
| Consistency Invariants | 5 |
| Assumptions | 12 |

---

## File Structure

```
specs/
└── 001-ecobook-core/
    ├── spec.md                          # Main specification (all requirements, contracts, algorithms)
    └── checklists/
        └── requirements.md              # Quality validation checklist (✅ ALL PASS)

.specify/
└── feature.json                         # Feature metadata (directory path)
```

---

## Validation Summary

✅ **Content Quality**: Maintains WHAT vs HOW separation; business-focused language  
✅ **Completeness**: All 43 FR + 5 NFR specified with acceptance criteria  
✅ **Testability**: Every requirement can be validated independently  
✅ **Clarity**: No ambiguities; acceptance scenarios define specific test cases  
✅ **Consistency**: State machines, invariants, and data contracts align  
✅ **Scope**: MVP boundaries clear; out-of-scope items explicitly listed  

---

**🎯 Status**: READY FOR PLANNING PHASE
