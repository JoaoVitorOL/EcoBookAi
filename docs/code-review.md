# Code Review Summary

Date: `2026-05-23`

## Scope

- Backend runtime review across `145` Java source files under `EcoBookAiBackend/src/main/java`
- Backlog/runtime alignment review against:
  - `specs/001-ecobook-core/TASKS.md`
  - `specs/001-ecobook-core/contracts/`
  - `README.md`
  - `EcoBookAiBackend/README.md`

## Review Checklist

- Naming and API contract consistency across controllers/services
- Authentication and authorization guardrails
- Null-safety around security and audit flows
- Silent failure paths in material, notification and deletion workflows
- TODO/FIXME/dead-code sweep

## Commands Used

```powershell
rg -n "TODO:|FIXME|XXX" EcoBookAiBackend/src/main/java EcoBookAiBackend/src/test/java
rg -n "System\\.out\\.print|printStackTrace\\(|Thread\\.sleep\\(|Runtime\\.getRuntime\\(" EcoBookAiBackend/src/main/java EcoBookAiBackend/src/test/java
```

## Findings

- No production `TODO`, `FIXME` or `XXX` markers were left in the backend runtime sources.
- No `System.out.println` or `printStackTrace()` debug leftovers were found in production code.
- The only `Thread.sleep(...)` usages are intentional:
  - retry/backoff logic in `GeminiService`
  - deterministic tests in the test suite
- One static-analysis-critical issue in `AuditActionAspect` was removed by replacing a `null` SpEL root object with `joinPoint.getTarget()`.

## Issues Already Fixed During Phase 9/10 Hardening

- `401` responses are now stable when a JWT is valid structurally but no principal can be resolved at runtime.
- Account deletion reopens `RESERVADO` material correctly when an approved recipient is deleted.
- Material creation now cleans up promoted files if persistence fails mid-transaction.
- Notification serialization between backend and Android inbox was aligned after the real Firebase validation.
- Android Compose navigation no longer triggers the `UnrememberedGetBackStackEntry` lint/runtime risk in `NavGraph`.
- Consent rows in onboarding/profile now expose full-row toggle semantics instead of forcing the user to hit only the small control.
- Main Android content panes now cap their width on larger displays instead of stretching every form and list edge-to-edge.
- Runtime backend URL overrides now use asynchronous `SharedPreferences.apply()` instead of blocking `commit()` writes.

## Conclusion

The repository is consistent enough to mark the manual code-review pass as complete for Phase 10. No silent runtime inconsistencies remain as formal implementation blockers in the current backlog.

Residual follow-up context remains documented in:

- [static-analysis.md](static-analysis.md)
- [security-scan.md](security-scan.md)
- [migration-rollbacks.md](migration-rollbacks.md)
