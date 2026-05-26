package com.ecobook.service;

import com.ecobook.model.FailedNotification;
import com.ecobook.model.Usuario;
import com.ecobook.repository.FailedNotificationRepository;
import com.ecobook.repository.UsuarioRepository;
import com.google.firebase.ErrorCode;
import com.google.firebase.FirebaseException;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FcmServiceTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final FailedNotificationRepository failedNotificationRepository = mock(FailedNotificationRepository.class);
    private final FcmService fcmService = new FcmService(usuarioRepository, failedNotificationRepository);

    @Test
    @DisplayName("sendNotification should reject malformed user ids before touching the repositories")
    void shouldRejectMalformedUserId() {
        assertThat(fcmService.sendNotification("not-a-uuid", "EcoBook", "Teste")).isFalse();
        verifyNoInteractions(usuarioRepository, failedNotificationRepository);
    }

    @Test
    @DisplayName("sendNotification should skip users without a stored FCM token")
    void shouldSkipUsersWithoutStoredToken() {
        UUID userId = UUID.randomUUID();
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(Usuario.builder()
                .id(userId)
                .email("no-token@example.com")
                .passwordHash("hash")
                .nome("No Token")
                .build()));

        assertThat(fcmService.sendNotification(userId.toString(), "EcoBook", "Teste")).isFalse();
    }

    @Test
    @DisplayName("sendNotification should deliver through Firebase when the SDK is already initialized")
    void shouldSendNotificationWhenFirebaseClientExists() throws Exception {
        UUID userId = UUID.randomUUID();
        Usuario usuario = Usuario.builder()
                .id(userId)
                .email("token@example.com")
                .passwordHash("hash")
                .nome("Token User")
                .fcmToken("device-token")
                .build();
        FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(firebaseMessaging.send(any(Message.class))).thenReturn("projects/ecobook/messages/123");
        ReflectionTestUtils.setField(fcmService, "firebaseMessaging", firebaseMessaging);

        assertThat(fcmService.sendNotification(userId.toString(), "EcoBook", "Teste", Map.of("route", "my-requests")))
                .isTrue();

        verify(firebaseMessaging).send(any(Message.class));
        verify(failedNotificationRepository, never()).save(any(FailedNotification.class));
    }

    @Test
    @DisplayName("sendNotification should queue a retry when Firebase is not configured")
    void shouldQueueWhenFirebaseIsNotConfigured() {
        UUID userId = UUID.randomUUID();
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(Usuario.builder()
                .id(userId)
                .email("token@example.com")
                .passwordHash("hash")
                .nome("Token User")
                .fcmToken("device-token")
                .build()));
        when(failedNotificationRepository.save(any(FailedNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReflectionTestUtils.setField(fcmService, "serviceAccountPath", "");

        assertThat(fcmService.sendNotification(userId.toString(), "EcoBook", "Teste")).isFalse();
        verify(failedNotificationRepository).save(any(FailedNotification.class));
    }

    @Test
    @DisplayName("sendNotification should clear the stored token when Firebase reports it as unregistered")
    void shouldClearStoredTokenWhenFirebaseReportsUnregisteredDevice() throws Exception {
        UUID userId = UUID.randomUUID();
        Usuario usuario = Usuario.builder()
                .id(userId)
                .email("token@example.com")
                .passwordHash("hash")
                .nome("Token User")
                .fcmToken("device-token")
                .build();
        FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(firebaseMessaging.send(any(Message.class))).thenThrow(
                messagingException(MessagingErrorCode.UNREGISTERED, "Device token expired")
        );
        ReflectionTestUtils.setField(fcmService, "firebaseMessaging", firebaseMessaging);

        assertThat(fcmService.sendNotification(userId.toString(), "EcoBook", "Teste")).isFalse();

        assertThat(usuario.getFcmToken()).isNull();
        verify(usuarioRepository).save(usuario);
        verify(failedNotificationRepository, never()).save(any(FailedNotification.class));
    }

    @Test
    @DisplayName("retryFailedNotifications should mark queued notifications as permanently failed when the user no longer exists")
    void shouldMarkQueuedNotificationAsPermanentlyFailedWhenUserIsMissing() {
        FailedNotification queued = queuedNotification(UUID.randomUUID());
        when(failedNotificationRepository
                .findTop100ByDeliveredAtIsNullAndPermanentlyFailedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any(LocalDateTime.class)))
                .thenReturn(java.util.List.of(queued));
        when(usuarioRepository.findById(queued.getUserId())).thenReturn(Optional.empty());
        when(failedNotificationRepository.save(any(FailedNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(fcmService.retryFailedNotifications()).isEqualTo(1);
        assertThat(queued.getPermanentlyFailedAt()).isNotNull();
        assertThat(queued.getLastError()).contains("retry");
    }

    @Test
    @DisplayName("retryFailedNotifications should schedule another attempt when Firebase is temporarily unavailable")
    void shouldScheduleRetryWhenFirebaseClientIsUnavailable() {
        UUID userId = UUID.randomUUID();
        FailedNotification queued = queuedNotification(userId);
        Usuario usuario = Usuario.builder()
                .id(userId)
                .email("retry@example.com")
                .passwordHash("hash")
                .nome("Retry User")
                .fcmToken("device-token")
                .build();

        when(failedNotificationRepository
                .findTop100ByDeliveredAtIsNullAndPermanentlyFailedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any(LocalDateTime.class)))
                .thenReturn(java.util.List.of(queued));
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(failedNotificationRepository.save(any(FailedNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(fcmService, "serviceAccountPath", "");

        assertThat(fcmService.retryFailedNotifications()).isEqualTo(1);
        assertThat(queued.getRetryCount()).isEqualTo(1);
        assertThat(queued.getPermanentlyFailedAt()).isNull();
        assertThat(queued.getNextAttemptAt()).isAfter(LocalDateTime.now().plusMinutes(50));
        assertThat(queued.getLastError()).contains("Firebase Admin SDK");
    }

    @Test
    @DisplayName("retryFailedNotifications should mark the queue item as delivered after a successful retry")
    void shouldMarkQueuedNotificationAsDeliveredAfterRetrySuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        FailedNotification queued = queuedNotification(userId);
        Usuario usuario = Usuario.builder()
                .id(userId)
                .email("retry@example.com")
                .passwordHash("hash")
                .nome("Retry User")
                .fcmToken("device-token")
                .build();
        FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);

        when(failedNotificationRepository
                .findTop100ByDeliveredAtIsNullAndPermanentlyFailedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any(LocalDateTime.class)))
                .thenReturn(java.util.List.of(queued));
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(firebaseMessaging.send(any(Message.class))).thenReturn("projects/ecobook/messages/retry-success");
        when(failedNotificationRepository.save(any(FailedNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(fcmService, "firebaseMessaging", firebaseMessaging);

        assertThat(fcmService.retryFailedNotifications()).isEqualTo(1);
        assertThat(queued.getDeliveredAt()).isNotNull();
        assertThat(queued.getLastAttemptAt()).isNotNull();
        assertThat(queued.getLastError()).isNull();
    }

    @Test
    @DisplayName("retryFailedNotifications should permanently fail after reaching the retry limit")
    void shouldPermanentlyFailAfterRetryLimit() {
        UUID userId = UUID.randomUUID();
        FailedNotification queued = queuedNotification(userId);
        queued.setRetryCount(2);
        Usuario usuario = Usuario.builder()
                .id(userId)
                .email("retry@example.com")
                .passwordHash("hash")
                .nome("Retry User")
                .fcmToken("device-token")
                .build();

        when(failedNotificationRepository
                .findTop100ByDeliveredAtIsNullAndPermanentlyFailedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any(LocalDateTime.class)))
                .thenReturn(java.util.List.of(queued));
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
        when(failedNotificationRepository.save(any(FailedNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(fcmService, "serviceAccountPath", "");

        assertThat(fcmService.retryFailedNotifications()).isEqualTo(1);
        assertThat(queued.getPermanentlyFailedAt()).isNotNull();
        assertThat(queued.getRetryCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("sendNotification should keep retry initialization available after a missing credential path")
    void shouldAllowFutureInitializationAttemptsAfterMissingCredentialPath() {
        UUID userId = UUID.randomUUID();
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(Usuario.builder()
                .id(userId)
                .email("token@example.com")
                .passwordHash("hash")
                .nome("Token User")
                .fcmToken("device-token")
                .build()));
        when(failedNotificationRepository.save(any(FailedNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReflectionTestUtils.setField(fcmService, "serviceAccountPath", "C:/missing/firebase-adminsdk.json");

        assertThat(fcmService.sendNotification(userId.toString(), "EcoBook", "Teste")).isFalse();
        assertThat(ReflectionTestUtils.getField(fcmService, "firebaseMessaging")).isNull();

        ReflectionTestUtils.setField(fcmService, "serviceAccountPath", "");
        assertThat(fcmService.sendNotification(userId.toString(), "EcoBook", "Teste novamente")).isFalse();

        verify(failedNotificationRepository, times(2)).save(any(FailedNotification.class));
    }

    @Test
    @DisplayName("resolveServiceAccountPath should fallback to GOOGLE_APPLICATION_CREDENTIALS")
    void shouldFallbackToGoogleApplicationCredentials() {
        FcmService envAwareService = new FcmService(usuarioRepository, failedNotificationRepository) {
            @Override
            String googleApplicationCredentialsEnv() {
                return "C:/firebase/ecobook-adminsdk.json";
            }
        };

        ReflectionTestUtils.setField(envAwareService, "serviceAccountPath", "");

        assertThat(envAwareService.resolveServiceAccountPath())
                .isEqualTo("C:/firebase/ecobook-adminsdk.json");
    }

    private FailedNotification queuedNotification(UUID userId) {
        return FailedNotification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .notificationType("GENERIC")
                .title("Titulo")
                .body("Corpo")
                .payloadData(Map.of("route", "notifications"))
                .nextAttemptAt(LocalDateTime.now().minusMinutes(1))
                .build();
    }

    private FirebaseMessagingException messagingException(MessagingErrorCode errorCode, String message) throws Exception {
        FirebaseException baseException = new FirebaseException(ErrorCode.UNKNOWN, message, null);
        Method factory = FirebaseMessagingException.class
                .getDeclaredMethod("withMessagingErrorCode", FirebaseException.class, MessagingErrorCode.class);
        factory.setAccessible(true);
        return (FirebaseMessagingException) factory.invoke(null, baseException, errorCode);
    }
}
