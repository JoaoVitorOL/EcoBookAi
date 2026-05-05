# Phase 1 Audit

> Historical note (2026-05-05): this audit reflects the codebase while authentication was still being planned around Google OAuth2. The current source of truth for auth is now email + password + JWT. Treat older OAuth2 references in this document as legacy context only.

**Date**: 2026-05-04  
**Scope**: Revalidation of `T001-T035` from `specs/001-ecobook-core/TASKS.md`

## Summary

Phase 1 was mostly present, but the original "35/35 complete" claim was too optimistic.
This audit focused on structural work only and did not implement later-phase business flows.

## Corrections Applied

- Backend JPA IDs were aligned with the database schema by using `UUID` in the main entities and repositories.
- Backend main profile now favors Flyway-managed schema validation instead of `create-drop`.
- Backend test profile was hardened for H2 PostgreSQL compatibility and a longer JWT test secret.
- `BaseIntegrationTest` now rolls back transactions automatically.
- `FirstIntegrationTest` now validates both the health endpoint and basic database connectivity.
- Stub controllers were added for `auth`, `usuarios`, `materiais`, and `solicitacoes` so Phase 1 really has controller scaffolding.
- Android now supports an optional `backend.url` override in `local.properties`.
- Android now has `src/androidTest` structure, a Compose smoke test, and `MockApiInterceptor`.
- Android Gradle config now includes a managed test device baseline (`pixel6Api34`) for emulator-oriented validation.
- Documentation was updated where it still claimed the backend had not been validated locally.

## Still Pending or External

- `T019`: `app/google-services.json` is still intentionally absent because it is a credentialed Firebase artifact and should not be faked or committed blindly.
- `T002`: the repository still does not include a real Gemini SDK dependency wired for production use; Phase 1 only has the surrounding placeholders and service skeleton.
- `T015`: the Lombok IDE plugin portion is an environment/tooling step, not something the repository can enforce by itself.
- `T035`: the "register -> health check -> verify database connectivity" scenario is only partially represented. Health check and DB probe exist now, but real registration belongs to the Phase 2 authentication implementation.
- Full feature endpoints in `AuthController`, `UsuarioController`, `MaterialController`, and `SolicitacaoController` remain intentionally deferred to their planned phases.

## Validation Executed

- Backend: `mvn clean test`
- Android: `gradlew.bat testDebugUnitTest assembleDebug lintDebug`

## Controller Location

- Backend controllers: `EcoBookAiBackend/src/main/java/com/ecobook/controller/`
- Android app: no MVC-style controller layer; the app uses `MainActivity`, `NavGraph`, ViewModels, repository classes, and services instead.
