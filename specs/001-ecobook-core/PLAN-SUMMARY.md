# Implementation Plan Summary

**Phase**: 1 Complete (Design & Contracts)  
**Date**: 2026-05-05  
**Status**: Ready for Phase 2 Rebaseline

---

## Planning Update

This summary has been rebased to the new authentication rule:

- Authentication is now **email + password + JWT**
- Passwords are stored on the backend only as strong hashes
- Google OAuth2 is no longer the source of truth for the MVP

The product direction for AI, onboarding, matching, request workflow, and notifications remains the same. The main planning change is that all auth-related work now targets local credentials instead of Google sign-in.

---

## Phase 1 Deliverables

### 1. [plan.md](plan.md)

Defines:
- Technical context for Android + Spring Boot + PostgreSQL + Gemini + FCM
- Constitution alignment checks
- Phased implementation plan
- Architecture and risk management

### 2. [research.md](research.md)

Captures:
- Gemini feasibility and retry strategy
- Image quality, rate limits, and storage decisions
- Deterministic locking and matching rules
- Email/password + JWT authentication approach for Android and backend

### 3. [data-model.md](data-model.md)

Defines:
- Core entities and relationships
- User profile completeness rules
- `password_hash` storage in `usuario`
- PostgreSQL schema and indexes

### 4. [quickstart.md](quickstart.md)

Documents:
- Local environment setup
- Backend startup
- Android setup
- Register/login flow with email and password
- First integration test path

### 5. `contracts/`

Primary auth-facing contract:
- [user-api.md](contracts/user-api.md)

Core endpoints now expected:
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `PUT /api/v1/usuarios/me`
- `GET /api/v1/usuarios/me`

---

## Constitution Alignment

All planning now aligns with Constitution v2.0.0:

| Principle | Alignment | Evidence |
|-----------|-----------|----------|
| Social Impact | Yes | Matching and donation flow remain mission-centered |
| Android-Native | Yes | Native Kotlin/Compose app remains immutable |
| Password Hashing & Deterministic Security | Yes | Email/password hash + JWT replaces Google OAuth2 |
| LGPD & Privacy | Yes | Consent, anonymization, and retention remain unchanged |
| MVP Scope Discipline | Yes | No expansion into multi-provider auth for MVP |
| Rule-Based Matching | Yes | Matching logic remains deterministic and auditable |

---

## Technical Stack Snapshot

| Component | Technology | Notes |
|-----------|-----------|-------|
| Frontend | Kotlin + Jetpack Compose | Android native only |
| Backend | Spring Boot 3.x + Java 17+ | REST API + JWT issuance |
| Database | PostgreSQL 14+ | Stores profile and password hash |
| Authentication | Email/password + JWT | No Google sign-in for MVP |
| AI | Google Gemini 2.5 Flash | Unchanged |
| Messaging | Firebase Cloud Messaging | Unchanged |
| Storage | Local filesystem | Unchanged |

---

## Phase 2 Rebaseline

The next implementation phase should now focus on:

1. Backend auth rework
   - Register endpoint with password hashing
   - Login endpoint with credential verification
   - User repository/model updates for `password_hash`
   - Auth integration tests

2. Android auth rework
   - Login/register screen with email and password
   - Form validation and user feedback
   - JWT persistence and session restore
   - Logout and 401 handling reuse

3. Documentation and contract cleanup
   - Remove remaining Google auth assumptions
   - Keep historical OAuth2 docs clearly marked as legacy

---

## Readiness Notes

What remains valid from the original plan:
- Backend and Android project structure
- JWT-based session handling
- Onboarding and `perfil_completo` gate
- AI-assisted material classification
- Matching, request, and FCM modules

What changed materially:
- Auth source of truth
- User schema for credentials
- Android entry flow
- Backend auth service design

---

## Historical Note

Some historical reports in the repository still mention Google OAuth2 because they describe an already-executed earlier direction. They are being kept as historical artifacts and are now explicitly marked as legacy where needed.
