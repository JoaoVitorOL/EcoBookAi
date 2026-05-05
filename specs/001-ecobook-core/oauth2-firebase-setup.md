# Legacy OAuth2/Firebase Auth Notes

**Status**: Deprecated for authentication as of 2026-05-05

---

## Why this file still exists

This file originally documented a Google OAuth2-based authentication direction.

The project has now pivoted to:

- local authentication with **email + password**
- server-side `password_hash` storage
- backend-issued **JWT** for session handling

Because of that pivot, this file is no longer a source of truth for authentication behavior.

---

## What still remains relevant here

Only Firebase-related notes that support FCM may still be useful conceptually.

The authentication guidance previously described in this file is superseded by:

- [constitution.md](../../.specify/memory/constitution.md)
- [plan.md](plan.md)
- [research.md](research.md)
- [quickstart.md](quickstart.md)
- [contracts/user-api.md](contracts/user-api.md)
- [EcoBookAiAndroid/README.md](../../EcoBookAiAndroid/README.md)
- [EcoBookAiBackend/README.md](../../EcoBookAiBackend/README.md)

---

## Current authentication rule

1. User registers with email, password, and name.
2. Backend stores only `password_hash` in `usuario`.
3. User logs in with email and password.
4. Backend issues JWT.
5. Android stores JWT securely and clears session on `401`.
