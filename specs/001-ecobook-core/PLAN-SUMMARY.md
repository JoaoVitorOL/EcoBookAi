# Implementation Plan Summary

**Phase**: 1 Complete (Design & Contracts)  
**Date**: 2026-04-15  
**Status**: ✅ Ready for Phase 2 (Prototypes & Integration Prep)

---

## Phase 1 Deliverables

### ✅ 1. plan.md (This Document)
**Location**: [specs/001-ecobook-core/plan.md](plan.md)

**Contents**:
- Technical context (Kotlin, Spring Boot, PostgreSQL, Gemini, FCM)
- Constitution check (all 6 principles verified ✅)
- Project structure (Android + Backend layout)
- 5 implementation phases (Weeks 1–17)
- Risk mitigation table
- Success criteria & validation

**Key Planning Decisions**:
- Phase 0 (2 weeks): Research, resolve technical unknowns
- Phase 1 (2 weeks): Design, contracts, quickstart
- Phase 2 (3 weeks): Prototypes, integration test infrastructure
- Phase 3 (3 weeks): Gemini testing (20+ images), confidence distribution analysis
- Phase 4 (5 weeks): Full feature development, 85%+ test coverage
- Phase 5 (2 weeks): Launch, monitoring, feedback

---

### ✅ 2. research.md (Phase 0 Output)
**Location**: [specs/001-ecobook-core/research.md](research.md)

**Contents**:
1. **Gemini Integration Viability** → ✅ Proceed (expect ≥75% SUCCESS rate)
2. **Image Quality Impact** → ✅ 3-tier validation (MIME, size, blur check)
3. **API Rate Limiting** → ✅ Free tier sufficient (250/day >> 12 peak/day)
4. **State Machine Atomicity** → ✅ PostgreSQL SERIALIZABLE + SELECT...FOR UPDATE
5. **Android OAuth2 + JWT** → ✅ AppAuth library + custom JWT backend
6. **Local Filesystem Storage** → ✅ `/uploads/{user_id}/{uuid}.ext` per Constitution
7. **Geographic Normalization** → ✅ Uppercase + NFD + ASCII
8. **Gemini Prompt Engineering** → ✅ Structured JSON prompt + strict parsing
9. **Matching Algorithm** → ✅ 6-step deterministic filter + ranking
10. **FCM Reliability** → ✅ Best-effort; in-app polling fallback

**All NEEDS CLARIFICATION Resolved** ✓

---

### ✅ 3. data-model.md (Phase 1 Output)
**Location**: [specs/001-ecobook-core/data-model.md](data-model.md)

**Contents**:
- **4 Core Entities**: Usuario, Material, Solicitacao, + Enums
- **ER Diagram**: Clear relationships (1:N, constraints)
- **Field Definitions**: Type, constraints, validation rules (150+ fields total)
- **7 Enums**: Disciplina, NivelEnsino, SistemaEnsino, EstadoConservacao, StatusMaterial, StatusSolicitacao, StatusRespostaIA
- **State Machines**: Material (4 states) + Solicitacao (5 states) with valid transitions
- **PostgreSQL DDL**: Complete CREATE TABLE statements with indexes, constraints
- **Validation Rules**: Profile completeness, geo-normalization, enum validation, state transitions
- **Invariants**: 5 data consistency rules enforced atomically
- **Performance Considerations**: Query optimization, connection pooling, slow query monitoring
- **Migration Strategy**: Zero-downtime schema evolution pattern

**Key Indexes**:
```
idx_material_status
idx_material_status_disciplina
idx_material_status_nivel_ensino
idx_material_cidade_bairro
idx_material_data_criacao DESC
idx_solicitacao_expires_at (for 14-day expiry job)
```

---

### ✅ 4. quickstart.md (Phase 1 Output)
**Location**: [specs/001-ecobook-core/quickstart.md](quickstart.md)

**Contents**:
- **Prerequisites**: Java 17, PostgreSQL 14, Android SDK, Docker
- **Backend Setup**: OAuth2, Gemini API, Firebase configuration
- **Android Setup**: google-services.json, local.properties, emulator config
- **First Integration Test**: 7-step scenario (register → upload → search → request → approve)
- **Troubleshooting**: Backend, Android, Database issues
- **Development Workflow**: Hot-reload, testing, next steps

**First Test Walkthrough**:
1. Start backend + PostgreSQL
2. Register test user
3. Complete profile (city, neighborhood)
4. Upload textbook image → Gemini classification
5. Create student profile
6. Search materials (verify ranking)
7. Submit request
8. Approve (verify state sync)

---

### ✅ 5. contracts/ Directory (Phase 1 Partial)
**Location**: specs/001-ecobook-core/contracts/

**Planned Contracts** (to be generated in Phase 2):
- `material-api.md` (POST /materiais, GET /materiais, PATCH /materiais)
- `solicitacao-api.md` (POST /solicitacoes, PATCH /solicitacoes, GET /solicitacoes)
- `user-api.md` (POST /auth/register, PATCH /usuarios/{id}, GET /usuarios/me)
- `ai-response.md` (POST /materiais/preview response schema)
- `notification-schema.md` (6 FCM notification payloads)
- `error-response.md` (Standard error envelope)

---

## Constitution Alignment

✅ **All 6 Principles Verified**:

| Principle | Alignment | Evidence |
|-----------|-----------|----------|
| **I. Social Impact** | ✅ | Material matching connects donors with students; reduces educational inequality |
| **II. Android-Native (Immutable)** | ✅ | Architecture commits: exclusive Kotlin/Compose; Spring Boot backend; no web/iOS |
| **III. Security & OAuth2** | ✅ | Google OAuth2 + JWT 7-day expiry; atomic approval with SERIALIZABLE locks |
| **IV. Data Privacy & LGPD** | ✅ | 2-year image retention, soft-delete with anonymization, 2-stage consent |
| **V. MVP Scope Discipline** | ✅ | No ML recommendations, condition automation, multi-needs, cloud storage, or analytics |
| **VI. Rule-Based Deterministic** | ✅ | 6-step deterministic algorithm, enum-only categories, text-based geo (no GPS) |

---

## Technical Stack Finalized

| Component | Technology | Rationale | Alternatives Rejected |
|-----------|-----------|-----------|----------------------|
| **Frontend** | Kotlin + Jetpack Compose | Material Design 3, native performance, Android-first | React Native, Flutter (violates Constitution) |
| **Backend** | Spring Boot 3.x + Java 17 | Mature, OAuth2 support, Spring Data JPA ORM | Node.js (wrong language), Django (Python) |
| **Database** | PostgreSQL 14+ | ACID compliance, enum types, SERIALIZABLE isolation | MongoDB (eventual consistency risk), MySQL (less ACID) |
| **AI Integration** | Google Gemini 2.5 Flash | Free tier (250 req/day), multimodal, easy Java client | OpenAI (API cost), local ML (operational complexity) |
| **Authentication** | Google OAuth2 + JWT | Industry standard, no email/password friction | Firebase Auth (vendor lock-in), custom auth (security risk) |
| **Storage** | Local filesystem | Constitutional constraint (MVP), no extra cost | AWS S3 (cost, latency), Google Cloud Storage (cost) |
| **Messaging** | Firebase Cloud Messaging | Free tier, reliable, well-supported | AWS SNS (cost), custom MQTT (operations) |
| **Testing** | JUnit 5 + Mockito (backend), Compose Testing (Android) | Industry standard, well-supported | TestNG (less common), custom frameworks |

---

## Work Breakdown by Phase

### Phase 0 (Weeks 1–2) — ✅ COMPLETE
**Team**: CTO, Tech Lead  
**Output**: research.md (10 design decisions, all NEEDS CLARIFICATION resolved)

### Phase 1 (Weeks 3–4) — ✅ COMPLETE
**Team**: Backend Architect, Android Architect  
**Output**: plan.md, research.md, data-model.md, quickstart.md, agent context

### Phase 2 (Weeks 5–7) — NEXT
**Team**: 1 Backend Dev, 1 Android Dev, 1 QA  
**Output**: 
- Backend skeleton (controllers stubs, service interfaces, repository layer)
- Android skeleton (screens, navigation, OAuth2 flow)
- Integration test infrastructure (30+ tests)

### Phase 3 (Weeks 8–10) — FOLLOW
**Team**: 1 QA, Backend Dev (on-call)  
**Output**:
- 20+ diverse textbook images collected
- Gemini integration tested, confidence distribution documented
- Phase 4 team briefed on AI approach

### Phase 4 (Weeks 11–15) — MAIN
**Team**: 2 Backend Devs, 2 Android Devs, 1 QA  
**Output**:
- All 43 FR implemented
- All 8 user stories completed
- 85%+ test coverage
- Performance benchmarks validated

### Phase 5 (Weeks 16–17) — LAUNCH
**Team**: Full team, DevOps  
**Output**:
- Deployed MVP (backend + Android APK)
- Production monitoring (Grafana, error tracking)
- User feedback loop established

---

## Success Metrics

### Phase 1 Completion Criteria
- [x] plan.md complete (5 phases, risk mitigation, team allocation)
- [x] Constitution check passed (all 6 principles verified)
- [x] Data model finalized (4 entities, 7 enums, PostgreSQL DDL)
- [x] Quickstart guide created (first integration test walkthrough)
- [x] Architecture documented (Android + Backend layout)
- [x] All NEEDS CLARIFICATION resolved (research.md complete)

### Phase 2 Readiness
- [ ] Backend skeleton runnable (HTTP 200 on /actuator/health)
- [ ] Android app launchable (OAuth2 login screen appears)
- [ ] Integration test suite (30+ tests, CI/CD configured)
- [ ] Developer documentation complete

### Phase 3 Readiness
- [ ] 20+ test images collected and labeled
- [ ] Gemini integration tested, confidence thresholds validated
- [ ] Fallback UX finalized based on real data

### Phase 4 Readiness
- [ ] All 43 FR implemented and tested
- [ ] All 8 user stories verified
- [ ] 85%+ test coverage achieved
- [ ] P95 search latency < 2s, upload < 3min confirmed

### Phase 5 Success Metrics
- [ ] ≥ 50 active users within 4 weeks
- [ ] ≥ 250 materials in catalog
- [ ] Gemini SUCCESS rate ≥ 75%
- [ ] Backend uptime ≥ 99.5%
- [ ] FCM delivery ≥ 95%

---

## Risks & Mitigation (Revisited)

| Risk | Severity | Status | Mitigation | Owner |
|------|----------|--------|-----------|-------|
| Gemini biased on regional materials | Medium | Active | Phase 3 testing (20+ images), prompt refinement | CTO |
| Image quality variation | High | Mitigated | Client-side validation (MIME, size, blur), user guidance | Android Lead |
| Rate limit exceeded | Low | Mitigated | Monitor usage (16 avg/day), graceful degradation | Backend Lead |
| State machine race condition | Low | Mitigated | SERIALIZABLE isolation, SELECT...FOR UPDATE, testing | Backend Lead |
| User abandonment in onboarding | Medium | Mitigated | Phase 2 prototype testing (5–10 users), UX iteration | Product Lead |
| Team skill gaps (Kotlin, Gemini) | Medium | Mitigated | Early training, external code reviews, pair programming | CTO |

---

## Handoff to Phase 2

### Prerequisites Complete ✓
- [x] Technical decisions finalized
- [x] Architecture documented
- [x] Data model defined
- [x] API contracts planned
- [x] Quickstart guide created
- [x] Risk analysis complete
- [x] Team roles assigned

### Phase 2 Inputs
1. **plan.md** (this document)
2. **research.md** (technical decisions)
3. **data-model.md** (entity definitions, DDL)
4. **quickstart.md** (dev environment setup)
5. **spec.md** (43 FR, 8 user stories, 15+ endpoints)

### Phase 2 Outputs (Expected)
1. Backend skeleton (controllers, services, repositories)
2. Android skeleton (screens, navigation, API client)
3. Integration test suite (30+ passing tests)
4. CI/CD workflows (GitHub Actions)
5. API documentation (Swagger/OpenAPI)

---

## Approval & Sign-Off

| Role | Name | Date | Approved |
|------|------|------|----------|
| Product Lead | [TBD] | 2026-04-15 | ☐ |
| CTO | [TBD] | 2026-04-15 | ☐ |
| Backend Lead | [TBD] | 2026-04-15 | ☐ |
| Android Lead | [TBD] | 2026-04-15 | ☐ |
| QA Lead | [TBD] | 2026-04-15 | ☐ |

---

## Document Control

| Version | Date | Author | Change |
|---------|------|--------|--------|
| 1.0 | 2026-04-15 | Spec Kit CLI | Initial plan, Phase 0 & 1 complete |

---

**Status**: ✅ Phase 1 COMPLETE  
**Next Gate**: Phase 2 kickoff (Week 5)  
**Approved By**: [Pending sign-off from team leads]
