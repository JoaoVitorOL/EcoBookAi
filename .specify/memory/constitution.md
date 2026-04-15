<!-- SYNC IMPACT REPORT (2026-04-15)
Version Change: N/A (First Version) → 1.0.0
New Principles Added: 
  - I. Social Impact & Educational Access
  - II. Android-Native Architecture (Immutable)
  - III. Security Through OAuth2 & Deterministic Process
  - IV. Data Privacy & LGPD Compliance
  - V. MVP Scope Discipline
  - VI. Rule-Based Matching (No ML)

New Sections Added:
  - Technical Stack & Constraints
  - User Roles & Permissions (RBAC)
  - LGPD & Ethical Governance

Templates Requiring Verification:
  - ✓ plan-template.md (Constitutional Check gate applicable)
  - ✓ spec-template.md (Scope compliance)
  - ✓ tasks-template.md (Task categorization)
  - ✓ constitution-template.md (Structure followed)

Follow-up: None - all placeholders resolved.
-->

# EcoBook IA Constitution

## Core Principles

### I. Social Impact & Educational Access
Educational materials redistribution through an economy circular model creates measurable social, environmental, and pedagogical impact. This platform prioritizes accessibility for diverse user profiles (different economic backgrounds, digital literacy, regional constraints). Every feature must directly serve the mission of connecting students who have surplus materials with those who need them, reducing educational inequality while promoting sustainability. No feature is justified solely by technical novelty or organizational convenience.

### II. Android-Native Architecture (Immutable Technical Decision)
EcoBook IA is **exclusively** a native Android application (minimum API 26 / Android 8.0), developed in Kotlin with Jetpack Compose for UI. This decision is immutable for the MVP and beyond. Backend is Spring Boot + Java with PostgreSQL. Google Gemini 2.5 Flash API is integrated directly as an internal Java service class—no separate Python microservice. Push notifications via Firebase Cloud Messaging. **No web version, no iOS, no Flutter, no cloud-native repackaging.** This constraint ensures focused, high-quality Android-first UX and eliminates divergent codebases.

### III. Security Through OAuth2 & Deterministic Processes
Authentication is exclusively via Google OAuth2 + our own JWT token (7-day expiry). No email/password, no social login fragmentation. Token validation is deterministic and server-enforced. All critical operations (e.g., material approval) are atomic database transactions with explicit locks to prevent race conditions and ensure consistency. Secrets management is non-negotiable: API keys, tokens, and credentials stored securely via environment variables or secure vaults—never in code.

### IV. Data Privacy & LGPD Compliance
User consent is collected at two levels: (1) platform usage, (2) AI data use. Users are explicitly informed that Gemini's free plan may involve Google training data on their input. Soft deletion with immediate anonymization: user names become 'Usuário Removido', emails converted to irreversible hashes, WhatsApp cleaned. Uploaded images retained for maximum 2 years, then purged. No tracking pixels, no third-party analytics without explicit consent. LGPD audit trails maintained for deletions and consent changes.

### V. MVP Scope Discipline
The MVP is ruthlessly scoped. No collaborative filtering, no ML-based recommendations, no automated vision analysis for item condition, no collection handling (series volumes), no multi-simultaneous needs per user, no automatic system-of-education equivalence, no cloud storage (local server `/uploads/{id_usuario}/{uuid}.ext`), no automated dispute resolution. These exclusions are **not** planned for post-MVP; they are architectural decisions that protect MVP quality. Features outside this boundary require full governance re-vote and constitution amendment.

### VI. Rule-Based Deterministic Matching (No Machine Learning)
Student-to-material matching is performed via deterministic rules: structured enums (Discipline, EducationLevel, SchoolSystem, ConservationState, MaterialStatus, RequestStatus, AIResponseStatus), geographic normalization (city + neighborhood text, **no GPS/coordinates**), and fixed business logic filters. This ensures reproducibility, auditability, and eliminates ML training debt. One active academic need per user; one physical material unit per record (indivisible).

## Technical Stack & Constraints

**Frontend**: Android native (Kotlin + Jetpack Compose), minimum API 26  
**Backend**: Spring Boot + Java  
**Database**: PostgreSQL  
**External APIs**: Google Gemini 2.5 Flash (direct backend integration), Google OAuth2, Firebase Cloud Messaging  
**Image Storage**: Local server filesystem `/uploads/{id_usuario}/{uuid}.ext`  
**No external cloud storage** (Google Cloud Storage, AWS S3) in MVP.  
**Structured enums**(never free-text for category fields): Discipline, NivelEnsino, SistemaEnsino, EstadoConservacao, StatusMaterial, StatusSolicitacao, StatusRespostaIA.  

## User Roles & Permissions (RBAC)

**USER Role**:
- Acts simultaneously as donor AND recipient (no profile separation).
- Permissions: POST/GET `/materiais`, POST `/materiais/{id}/solicitacoes`, manage own profile.
- Blocked if profile incomplete (onboarding not finished): HTTP 403 on above endpoints.

**ADMIN Role**:
- Full system access: user management, content moderation, system reporting.
- Administrative-only actions: user deactivation, bulk import tools.

**Profile Incompleteness Blocking**:
- Field completeness validated on every request to protected endpoints.
- Incomplete profiles receive 403 Forbidden with guidance to complete onboarding.

## LGPD & Ethical Governance

**Consent Management**:
- Two-stage consent in onboarding: Platform Usage + AI Data Usage.
- AI consent warns of Gemini free-tier data exposure (Google may train models).
- Consent revocable at any time via settings; retroactive opt-out triggers soft-delete.

**Data Retention & Deletion**:
- Images: 2-year maximum retention; automated purge after deadline.
- Profiles: Soft delete with immediate anonymization (name → 'Usuário Removido', email → SHA-256 hash, WhatsApp removed).
- Logs: Audit trail maintained for 90 days; consent/deletion events logged indefinitely.

**Transparency & Accountability**:
- Privacy policy in Portuguese accessible in-app and on website.
- Data subject rights exercised via in-app forms or email (DPO: [TODO - provide contact]).
- Regular LGPD compliance audits (quarterly minimum).

## Governance

**Constitution Supremacy**: This Constitution supersedes all prior practices, roadmaps, and design documents. In case of conflict, Constitution is the source of truth.

**Amendment Process**:
1. Proposed amendment submitted with rationale and impact analysis.
2. Reviewed against existing principles for contradictions.
3. Approved by Product Lead + CTO (consensus required).
4. Version incremented: MAJOR (principle removed/redefined), MINOR (new principle/section added), PATCH (clarifications/wording).
5. All dependent artifacts (plan-template, spec-template, tasks-template) reviewed for alignment.
6. Amended Constitution published; commit message documents change.

**Compliance Verification**:
- All PRs/ feature requests verified against Constitution principles at PR review.
- If feature violates a principle, PR rejected with explicit reference to Constitution section.
- Architecture reviews (quarterly) assess drift from Technical Stack & Constraints section.

**Reference in Development**:
- Development team consults this Constitution during spec writing (use `/speckit.plan` with Constitution Check gate).
- Task categorization in `tasks.md` reflects principle-driven task types (e.g., security hardening, LGPD compliance, scope enforcement).

---

**Version**: 1.0.0 | **Ratified**: 2026-04-15 | **Last Amended**: 2026-04-15
