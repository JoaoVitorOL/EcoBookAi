<!-- SYNC IMPACT REPORT (2026-05-05)
Version Change: 1.0.0 -> 2.0.0
Principles Updated:
  - III. Security Through Password Hashing & Deterministic Processes

Follow-up:
  - plan.md, PLAN-SUMMARY.md, research.md, spec.md, TASKS.md, data-model.md,
    contracts/, quickstart.md, and project READMEs reviewed for alignment.
-->

# EcoBook IA Constitution

## Core Principles

### I. Social Impact & Educational Access
Educational materials redistribution through a circular economy model creates measurable social, environmental, and pedagogical impact. This platform prioritizes accessibility for diverse user profiles, including different economic backgrounds, digital literacy levels, and regional constraints. Every feature must directly serve the mission of connecting students who have surplus materials with those who need them, reducing educational inequality while promoting sustainability. No feature is justified solely by technical novelty or organizational convenience.

### II. Android-Native Architecture (Immutable Technical Decision)
EcoBook IA is exclusively a native Android application (minimum API 26 / Android 8.0), developed in Kotlin with Jetpack Compose for UI. This decision is immutable for the MVP and beyond. Backend is Spring Boot + Java with PostgreSQL. Google Gemini 2.5 Flash API is integrated directly as an internal Java service class, with no separate Python microservice. Push notifications use Firebase Cloud Messaging. No web version, no iOS, no Flutter, and no cloud-native repackaging. This constraint keeps the product focused, high-quality, and Android-first.

### III. Security Through Password Hashing & Deterministic Processes
Authentication is exclusively via email and password stored on our server as a strong one-way hash in the user record, plus our own JWT token (7-day expiry). No Google OAuth2 and no social login fragmentation in the MVP. Raw passwords are never stored, logged, or returned by the API. Password verification is server-enforced and must use a dedicated hashing algorithm such as BCrypt or Argon2. All critical operations (for example, material approval) are atomic database transactions with explicit locks to prevent race conditions and ensure consistency. Secrets management is non-negotiable: API keys, tokens, and credentials belong in environment variables or secure vaults, never in source code.

### IV. Data Privacy & LGPD Compliance
User consent is collected at two levels: (1) platform usage, (2) AI data use. Users are explicitly informed that Gemini's free plan may involve Google training data on their input. Soft deletion with immediate anonymization is mandatory: user names become `Usuario Removido`, emails become irreversible hashes, and WhatsApp numbers are removed. Uploaded images are retained for a maximum of 2 years and then purged. No tracking pixels or third-party analytics are allowed without explicit consent. LGPD audit trails must be maintained for deletions and consent changes.

### V. MVP Scope Discipline
The MVP is ruthlessly scoped. No collaborative filtering, no ML-based recommendations, no automated vision analysis for item condition, no collection handling (series volumes), no multi-simultaneous needs per user, no automatic system-of-education equivalence, no cloud storage (local server `/uploads/{id_usuario}/{uuid}.ext`), and no automated dispute resolution. Password reset, email verification, and advanced account recovery are follow-on hardening work unless explicitly added to scope. These exclusions are architectural decisions that protect MVP quality. Features outside this boundary require a full governance re-vote and constitution amendment.

### VI. Rule-Based Deterministic Matching (No Machine Learning)
Student-to-material matching is performed via deterministic rules: structured enums (`Discipline`, `EducationLevel`, `SchoolSystem`, `ConservationState`, `MaterialStatus`, `RequestStatus`, `AIResponseStatus`), geographic normalization (city + neighborhood text, no GPS/coordinates), and fixed business logic filters. This ensures reproducibility, auditability, and avoids ML training debt. One active academic need per user; one physical material unit per record (indivisible).

## Technical Stack & Constraints

**Frontend**: Android native (Kotlin + Jetpack Compose), minimum API 26  
**Backend**: Spring Boot + Java  
**Database**: PostgreSQL  
**External APIs**: Google Gemini 2.5 Flash (direct backend integration), Firebase Cloud Messaging  
**Authentication**: Email + password hash stored in `usuario`, JWT issued by backend  
**Image Storage**: Local server filesystem `/uploads/{id_usuario}/{uuid}.ext`  
**No external cloud storage** (Google Cloud Storage, AWS S3) in MVP  
**Structured enums only** (never free-text for category fields): `Discipline`, `NivelEnsino`, `SistemaEnsino`, `EstadoConservacao`, `StatusMaterial`, `StatusSolicitacao`, `StatusRespostaIA`

## User Roles & Permissions (RBAC)

**USER Role**:
- Acts simultaneously as donor and recipient, with no profile separation.
- Permissions: `POST/GET /materiais`, `POST /materiais/{id}/solicitacoes`, manage own profile.
- Blocked if profile incomplete (onboarding not finished): HTTP 403 on protected endpoints.

**ADMIN Role**:
- Full system access: user management, content moderation, system reporting.
- Administrative-only actions: user deactivation, bulk import tools.

**Profile Incompleteness Blocking**:
- Field completeness is validated on every request to protected endpoints.
- Incomplete profiles receive HTTP 403 with guidance to complete onboarding.

## LGPD & Ethical Governance

**Consent Management**:
- Two-stage consent in onboarding: Platform Usage + AI Data Usage.
- AI consent warns of Gemini free-tier data exposure.
- Consent can be revoked at any time via settings; retroactive opt-out triggers soft-delete.

**Data Retention & Deletion**:
- Images: 2-year maximum retention; automated purge after deadline.
- Profiles: soft delete with immediate anonymization (name -> `Usuario Removido`, email -> SHA-256 hash, WhatsApp removed).
- Logs: audit trail maintained for 90 days; consent/deletion events logged indefinitely.

**Transparency & Accountability**:
- Privacy policy in Portuguese accessible in-app and on website.
- Data subject rights exercised via in-app forms or email (DPO contact still to be provided).
- Regular LGPD compliance audits at least quarterly.

## Governance

**Constitution Supremacy**: This Constitution supersedes all prior practices, roadmaps, and design documents. In case of conflict, the Constitution is the source of truth.

**Amendment Process**:
1. Proposed amendment submitted with rationale and impact analysis.
2. Reviewed against existing principles for contradictions.
3. Approved by Product Lead + CTO (consensus required).
4. Version incremented: MAJOR for principle removal/redefinition, MINOR for new principle/section, PATCH for clarification only.
5. All dependent artifacts reviewed for alignment.
6. Amended Constitution published, with commit message documenting the change.

**Compliance Verification**:
- All PRs and feature requests are verified against Constitution principles at review time.
- If a feature violates a principle, the PR is rejected with explicit reference to the relevant section.
- Architecture reviews assess drift from the Technical Stack & Constraints section.

**Reference in Development**:
- Development team consults this Constitution during spec writing.
- Task categorization in `tasks.md` reflects principle-driven work such as security hardening, LGPD compliance, and scope enforcement.

---

**Version**: 2.0.0 | **Ratified**: 2026-04-15 | **Last Amended**: 2026-05-05
