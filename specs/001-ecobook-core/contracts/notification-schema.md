# FCM Notification Schema

**Reference**: Phase 6 runtime payloads  
**Version**: 2.0  
**Date**: 2026-05-21  
**Status**: Current runtime contract for FCM payload generation, retry persistence, and Android inbox routing

---

## Overview

The notification stack is already implemented in runtime across backend and Android.

Current runtime behavior:

- backend receives device tokens through `POST /api/v1/fcm/tokens`
- business events publish `NotificationRequestedEvent`
- payloads are standardized before dispatch
- transient failures are persisted in `failed_notification`
- delivered payloads are also persisted in `user_notification`
- Android shows local notifications and keeps a dedicated inbox with unread state

Operational follow-up that still remains outside the code closeout:

- rerun the real Firebase/device validation whenever credentials, Firebase project wiring, or the target emulator/device setup changes

---

## Shared Payload Shape

Every runtime payload is built with the fields below.

```json
{
  "notification_id": "notification-uuid",
  "type": "SOLICITACAO_APROVADA",
  "title": "Solicitacao aprovada",
  "body": "Sua solicitacao foi aprovada.",
  "route": "my-requests",
  "solicitacao_id": "solicitacao-uuid",
  "material_id": "material-uuid",
  "metadata": {
    "material_titulo": "Geometria Plana 7o Ano",
    "doador_nome": "Joao Silva",
    "doador_whatsapp": "+5511999999999"
  }
}
```

Route values currently used by the Android app:

- `donor-requests`
- `my-requests`

The Android app converts these route values to the corresponding deep links and screen navigation.

---

## Runtime Notification Types

| Type | Recipient | Route | Trigger |
|------|-----------|-------|---------|
| `SOLICITACAO_RECEBIDA` | donor | `donor-requests` | student creates a request |
| `SOLICITACAO_APROVADA` | student | `my-requests` | donor approves |
| `SOLICITACAO_RECUSADA` | student | `my-requests` | donor declines or loses the approval race |
| `SOLICITACAO_CANCELADA` | donor or student | `my-requests` | donor cancel, student cancel, or expiry |
| `MATERIAL_DOADO` | student | `my-requests` | donor completes the donation |
| `MATERIAL_CANCELADO` | student | `my-requests` | donor removes a material that still has student requests |

---

## Delivery And Retry

### Backend Dispatch

- Firebase Admin SDK is initialized lazily inside `FcmService`
- when credentials are absent, real push stays dormant without breaking the rest of the flow
- payload generation still happens and the in-app inbox can continue to work

### Failure Queue

Transient failures are persisted in `failed_notification` with:

- `user_id`
- `notification_type`
- `payload_data`
- `retry_count`
- `next_attempt_at`
- `last_error`

Retry behavior:

- hourly retry job
- max `3` attempts before marking the row as permanently failed

### Persisted Inbox

Delivered notifications are stored in `user_notification` with:

- `notification_id`
- `notification_type`
- `title`
- `body`
- `route`
- `request_id`
- `material_id`
- `payload_data`
- `read_at`

---

## Android Handling

### Foreground

- the app builds a local notification
- the inbox is updated immediately
- notifications stay unread until the user explicitly marks them as read

### Background

- FCM system delivery shows the notification
- tapping the notification routes the user to the mapped area in the app

### Inbox UI

- unread bell entry point in the main screens
- notifications center with `Marcar como lida`
- notifications center with `Marcar todas como lidas`

---

## Current Closeout Note

The contract above matches the implemented runtime. Real Firebase/device validation was already exercised on `2026-05-23`; what remains is only operational revalidation when the target environment changes.
