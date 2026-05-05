# FCM Notification Schema

**Reference**: spec.md RF-037, RF-038, notification payloads  
**Version**: 1.0  
**Date**: 2026-04-17

---

## Overview

Firebase Cloud Messaging (FCM) notifications are sent for 6 key events in the donation workflow. All notifications are **best-effort** (not guaranteed delivery, acceptable for MVP), but failures are logged for retry.

**Retry Strategy** (per Q8 clarification, deferred to Phase 2 for advanced observability):
- Exponential backoff: 1s → 2s → 4s → 8s → 16s (max 5 attempts)
- Dead-letter queue (DLQ) after 5 failures: moved to `fcm_notifications_dlq` table
- Archive after 30 days of failure

---

## 1. SOLICITACAO_RECEBIDA

**Event**: Student submits request for material  
**Sent to**: Donor  
**Timing**: Immediately after POST /solicitacoes  
**Priority**: High (time-sensitive: donor should approve/decline promptly)

### Payload

```json
{
  "type": "SOLICITACAO_RECEBIDA",
  "solicitacao_id": "solicitacao-uuid-1234567890",
  "material_id": "material-uuid-1234567890",
  "material_titulo": "Geometria Plana 7º Ano",
  "estudante_nome": "Maria Santos",
  "message": "Sua doação recebeu um pedido. Revise em seu app."
}
```

### Field Descriptions

| Field | Type | Description |
|-------|------|-------------|
| `type` | String | Notification type identifier (for routing in client) |
| `solicitacao_id` | UUID | Link to specific request (deep link to approval screen) |
| `material_id` | UUID | Link to specific material (context) |
| `material_titulo` | String | Material title (preview, max 100 chars) |
| `estudante_nome` | String | Requester's name (preview, max 50 chars) |
| `message` | String | User-facing message (localized) |

### Client Handling

```kotlin
// Android (Jetpack Compose example)
when (notification.type) {
    "SOLICITACAO_RECEBIDA" -> {
        // Deep link to material detail screen with solicitacao context
        navController.navigate("solicitacao/${notification.solicitacao_id}")
    }
}
```

---

## 2. SOLICITACAO_APROVADA

**Event**: Donor approves student's request  
**Sent to**: Student  
**Timing**: Immediately after PATCH /solicitacoes/{id} with status=APROVADA  
**Priority**: High (critical: student needs donor contact to coordinate pickup)

### Payload

```json
{
  "type": "SOLICITACAO_APROVADA",
  "solicitacao_id": "solicitacao-uuid-1234567890",
  "material_id": "material-uuid-1234567890",
  "material_titulo": "Geometria Plana 7º Ano",
  "doador_nome": "João Silva",
  "doador_whatsapp": "+5548999999999",
  "message": "Sua solicitação foi aprovada! Contate o doador para combinar."
}
```

**Key Field**: `doador_whatsapp` provides student direct contact to arrange handoff

### Client Handling

```kotlin
// Copy WhatsApp to clipboard and prompt to message
val whatsapp = notification.doador_whatsapp
val message = "Olá, recebi aprovação para o material ${notification.material_titulo}. Quando podemos combinar?"
val whatsappIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$whatsapp?text=$message"))
startActivity(whatsappIntent)
```

---

## 3. SOLICITACAO_RECUSADA

**Event**: Donor declines student's request  
**Sent to**: Student  
**Timing**: Immediately after PATCH /solicitacoes/{id} with status=RECUSADA  
**Priority**: Medium (informational: student can search for alternatives)

### Payload

```json
{
  "type": "SOLICITACAO_RECUSADA",
  "solicitacao_id": "solicitacao-uuid-1234567890",
  "material_id": "material-uuid-1234567890",
  "material_titulo": "Geometria Plana 7º Ano",
  "message": "Sua solicitação foi recusada. Procure outro material."
}
```

### Client Handling

```kotlin
// Show snackbar and prompt to search for alternatives
Snackbar.make(view, "Request declined", Snackbar.LENGTH_SHORT).show()
// Redirect to search with same filters
```

---

## 4. SOLICITACAO_CANCELADA

**Event**: Donor cancels an approved request (or auto-expiry after 14 days)  
**Sent to**: Student  
**Timing**: Immediately after PATCH /solicitacoes/{id} with status=CANCELADA (or 14-day expiry job)  
**Priority**: High (reverses expectation: student needs to know material is no longer reserved)

### Payload

```json
{
  "type": "SOLICITACAO_CANCELADA",
  "solicitacao_id": "solicitacao-uuid-1234567890",
  "material_id": "material-uuid-1234567890",
  "material_titulo": "Geometria Plana 7º Ano",
  "message": "A doação foi cancelada. Procure outro material."
}
```

**Reason for Cancellation** (in message):
- Donor explicitly cancelled: "A doação foi cancelada pelo doador."
- Auto-expiry after 14 days: "A reserva expirou. O material está novamente disponível." (optional context)

### Client Handling

```kotlin
// Update state: material back to DISPONIVEL
// Option: Show "Request this material again?" button
```

---

## 5. MATERIAL_DOADO

**Event**: Donor marks donation as complete  
**Sent to**: Student  
**Timing**: Immediately after PATCH /solicitacoes/{id} with status=CONCLUIDA  
**Priority**: High (final confirmation + contact info for post-delivery feedback)

### Payload

```json
{
  "type": "MATERIAL_DOADO",
  "solicitacao_id": "solicitacao-uuid-1234567890",
  "material_id": "material-uuid-1234567890",
  "material_titulo": "Geometria Plana 7º Ano",
  "doador_nome": "João Silva",
  "doador_whatsapp": "+5548999999999",
  "message": "Sua doação chegou! Confirme recebimento."
}
```

**Key Purpose**: Provides donor contact for any post-delivery issues and prompt for confirmation

### Client Handling

```kotlin
// Show donation completion screen
// Prompt: "Confirm material received?" 
// → Success: Rate donor (optional V2 feature)
//         Send thank-you WhatsApp
```

---

## 6. MATERIAL_CANCELADO

**Event**: Donor cancels material at any stage (DISPONIVEL or RESERVADO)  
**Sent to**: All students with PENDENTE or APROVADA Solicitacoes  
**Timing**: Immediately after PATCH /materiais/{id} with status=CANCELADO  
**Priority**: Medium (informational: material no longer available)

### Payload

```json
{
  "type": "MATERIAL_CANCELADO",
  "material_id": "material-uuid-1234567890",
  "material_titulo": "Geometria Plana 7º Ano",
  "message": "A doação foi cancelada pelo doador."
}
```

**Broadcasting**:
- If Material was DISPONIVEL: Send to all users with PENDENTE Solicitacoes (they can forget about it)
- If Material was RESERVADO: Send to student with APROVADA Solicitacao (cascades Solicitacao to CANCELADA)

### Client Handling

```kotlin
// Remove from "My Requests" list
// Show toast: "Material cancelled by donor"
// Update material status to CANCELADO (grayed out if still visible in search)
```

---

## Notification Routing & Storage

### Android FCM Handling

**FCM Token Management**:
```kotlin
// In Application.onCreate() or Firebase setup
FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
    if (task.isSuccessful) {
        val token = task.result
        // Send to backend: POST /usuarios/{id}/fcm-token
        apiService.updateFCMToken(FCMTokenRequest(token))
    }
}
```

**Message Handler**:
```kotlin
class MyFCMService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val type = remoteMessage.data["type"]
        val solicitacaoId = remoteMessage.data["solicitacao_id"]
        
        when (type) {
            "SOLICITACAO_RECEBIDA" -> handleSolicitacaoRecebida(remoteMessage)
            "SOLICITACAO_APROVADA" -> handleSolicitacaoAprovada(remoteMessage)
            "SOLICITACAO_RECUSADA" -> handleSolicitacaoRecusada(remoteMessage)
            "SOLICITACAO_CANCELADA" -> handleSolicitacaoCancelada(remoteMessage)
            "MATERIAL_DOADO" -> handleMaterialDoado(remoteMessage)
            "MATERIAL_CANCELADO" -> handleMaterialCancelado(remoteMessage)
        }
    }
}
```

### Backend Notification Service

**Spring Boot Service**:
```java
@Service
public class FCMNotificationService {
    
    public void sendNotification(String userId, FCMNotification notification) {
        // 1. Retrieve user's FCM token
        String fcmToken = userService.getFCMToken(userId);
        
        // 2. Build FCM message
        Message message = Message.builder()
            .setToken(fcmToken)
            .setData(notification.toMap())
            .setNotification(new Notification(
                notification.getTitle(),
                notification.getBody()
            ))
            .build();
        
        // 3. Send with retry logic (exponential backoff, circuit breaker)
        sendWithRetry(message);
    }
    
    private void sendWithRetry(Message message) {
        int attempts = 0;
        int maxAttempts = 5;
        
        while (attempts < maxAttempts) {
            try {
                String response = FirebaseMessaging.getInstance().send(message);
                logger.info("FCM sent: {}", response);
                return; // Success
            } catch (FirebaseMessagingException e) {
                attempts++;
                if (attempts >= maxAttempts) {
                    // Move to DLQ
                    notificationDLQRepo.save(new FailedNotification(message, e));
                    logger.error("FCM failed after {} attempts, moved to DLQ", maxAttempts);
                } else {
                    // Exponential backoff: 1s, 2s, 4s, 8s, 16s
                    long delayMs = 1000L * (long) Math.pow(2, attempts - 1);
                    Thread.sleep(delayMs);
                }
            }
        }
    }
}
```

---

## Notification Permissions (Android)

**AndroidManifest.xml**:
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

**Runtime Permission** (Android 13+):
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    requestPermissions(arrayOf(POST_NOTIFICATIONS), PERMISSION_REQUEST_CODE)
}
```

---

## Localization & Customization

**Message Templates** (i18n deferred to V2):

Currently all messages in Portuguese (Brazilian). Future versions can parameterize:

```json
{
  "message": "i18n.SOLICITACAO_RECEBIDA.title",
  "args": {
    "estudante_nome": "Maria Santos"
  }
}
```

---

## Notification Delivery SLA (Q6)

**FCM Notification Delivery**:

| Metric | Target |
|--------|--------|
| P95 delivery latency | ≤ 5 seconds |
| Success rate | > 95% |

**Factors Affecting Delivery**:
- FCM network availability (Google's infrastructure)
- Device connectivity (WiFi vs cellular)
- App installation status
- User notification permissions enabled
- Doze/battery optimization on Android

**MVP Acceptable**:
- Best-effort delivery; failures logged to DLQ
- No guaranteed delivery SLA (acceptable for non-financial notifications)
- Phase 2: SMS/Email fallback for critical notifications
