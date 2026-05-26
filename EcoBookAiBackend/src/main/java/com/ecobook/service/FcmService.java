package com.ecobook.service;

import com.ecobook.dto.notification.NotificationPayloadDTO;
import com.ecobook.model.FailedNotification;
import com.ecobook.model.Usuario;
import com.ecobook.repository.FailedNotificationRepository;
import com.ecobook.repository.UsuarioRepository;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Firebase Cloud Messaging service for notifications.
 * The service stays dormant until FIREBASE_SERVICE_ACCOUNT_PATH is configured.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FcmService {

    private static final String FIREBASE_APP_NAME = "ecobook-fcm";
    private static final String GOOGLE_APPLICATION_CREDENTIALS = "GOOGLE_APPLICATION_CREDENTIALS";
    private static final int MAX_RETRY_COUNT = 3;

    private final UsuarioRepository usuarioRepository;
    private final FailedNotificationRepository failedNotificationRepository;

    @Value("${firebase.service-account-path:}")
    private String serviceAccountPath;

    @Value("${firebase.database-url:}")
    private String databaseUrl;

    private volatile FirebaseMessaging firebaseMessaging;

    /**
     * Dispatches a push notification to the target user.
     * @param userId user identifier
     * @param title notification title
     * @param body notification body
     * @return true when the operation succeeds or the condition is met; otherwise false
     */
    @Transactional
    public boolean sendNotification(String userId, String title, String body) {
        return sendNotification(userId, title, body, Map.of());
    }

    /**
     * Dispatches a push notification to the target user.
     * @param userId user identifier
     * @param title notification title
     * @param body notification body
     * @param data notification or response data map
     * @return true when the operation succeeds or the condition is met; otherwise false
     */
    @Transactional
    public boolean sendNotification(String userId, String title, String body, Map<String, String> data) {
        return sendNotificationInternal(userId, title, body, enrichPayload(title, body, data));
    }

    /**
     * Dispatches a push notification to the target user.
     * @param userId user identifier
     * @param payload notification payload to persist or send
     * @return true when the operation succeeds or the condition is met; otherwise false
     */
    @Transactional
    public boolean sendNotification(UUID userId, NotificationPayloadDTO payload) {
        return sendNotificationInternal(
                userId.toString(),
                payload.getTitle(),
                payload.getBody(),
                payload.toDataMap()
        );
    }

    /**
     * Retries failed notification deliveries that are ready for another attempt.
     * @return result of the operation
     */
    @Transactional
    public int retryFailedNotifications() {
        LocalDateTime now = LocalDateTime.now();
        int processedCount = 0;

        for (FailedNotification failedNotification : failedNotificationRepository
                .findTop100ByDeliveredAtIsNullAndPermanentlyFailedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(now)) {
            processedCount++;
            retryFailedNotification(failedNotification, now);
        }

        return processedCount;
    }

    private boolean sendNotificationInternal(String userId, String title, String body, Map<String, String> data) {
        Optional<Usuario> usuarioOptional = findUsuario(userId);
        if (usuarioOptional.isEmpty()) {
            log.warn("FCM skipped: user {} was not found or is invalid.", userId);
            return false;
        }

        Usuario usuario = usuarioOptional.get();
        if (!StringUtils.hasText(usuario.getFcmToken())) {
            log.info("FCM skipped: user {} does not have a registered device token.", userId);
            return false;
        }

        FirebaseMessaging messaging = getFirebaseMessagingClient();
        if (messaging == null) {
            String errorMessage = "Firebase Admin SDK indisponível; verifique FIREBASE_SERVICE_ACCOUNT_PATH no backend";
            log.warn("FCM unavailable for user {}: {}", userId, errorMessage);
            queueFailedNotification(usuario.getId(), title, body, data, errorMessage);
            return false;
        }

        Message message = buildMessage(usuario.getFcmToken(), title, body, data);
        try {
            String responseId = messaging.send(message);
            log.info("FCM sent successfully for user {} with response id {}", userId, responseId);
            return true;
        } catch (FirebaseMessagingException exception) {
            log.warn("FCM failed for user {}: {}", userId, exception.getMessage(), exception);
            if (shouldClearStoredToken(exception)) {
                clearStoredToken(usuario.getId(), usuario.getFcmToken());
            } else {
                queueFailedNotification(usuario.getId(), title, body, data, exception.getMessage());
            }
            return false;
        }
    }

    private void retryFailedNotification(FailedNotification failedNotification, LocalDateTime now) {
        Optional<Usuario> usuarioOptional = usuarioRepository.findById(failedNotification.getUserId());
        if (usuarioOptional.isEmpty()) {
            markAsPermanentlyFailed(failedNotification, now, "Usuário não encontrado para retry");
            return;
        }

        Usuario usuario = usuarioOptional.get();
        if (!StringUtils.hasText(usuario.getFcmToken())) {
            markAsPermanentlyFailed(failedNotification, now, "Usuário sem token FCM registrado");
            return;
        }

        FirebaseMessaging messaging = getFirebaseMessagingClient();
        if (messaging == null) {
            scheduleRetry(failedNotification, now, "Firebase Admin SDK indisponível no momento");
            return;
        }

        try {
            String responseId = messaging.send(buildMessage(
                    usuario.getFcmToken(),
                    failedNotification.getTitle(),
                    failedNotification.getBody(),
                    failedNotification.getPayloadData()
            ));
            failedNotification.setDeliveredAt(now);
            failedNotification.setLastAttemptAt(now);
            failedNotification.setLastError(null);
            failedNotificationRepository.save(failedNotification);
            log.info("FCM retry succeeded for queued notification {} with response id {}", failedNotification.getId(), responseId);
        } catch (FirebaseMessagingException exception) {
            log.warn("FCM retry failed for queued notification {}: {}", failedNotification.getId(), exception.getMessage(), exception);
            if (shouldClearStoredToken(exception)) {
                clearStoredToken(usuario.getId(), usuario.getFcmToken());
                markAsPermanentlyFailed(failedNotification, now, exception.getMessage());
                return;
            }

            scheduleRetry(failedNotification, now, exception.getMessage());
        }
    }

    private Optional<Usuario> findUsuario(String userId) {
        try {
            return usuarioRepository.findById(UUID.fromString(userId.trim()));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private Message buildMessage(String token, String title, String body, Map<String, String> data) {
        Message.Builder builder = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build());

        data.forEach((key, value) -> {
            if (StringUtils.hasText(key) && value != null) {
                builder.putData(key, value);
            }
        });
        return builder.build();
    }

    private Map<String, String> enrichPayload(String title, String body, Map<String, String> data) {
        LinkedHashMap<String, String> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("body", body);
        data.forEach((key, value) -> {
            if (StringUtils.hasText(key) && value != null) {
                payload.put(key, value);
            }
        });
        return payload;
    }

    private FirebaseMessaging getFirebaseMessagingClient() {
        if (firebaseMessaging != null) {
            return firebaseMessaging;
        }

        synchronized (this) {
            if (firebaseMessaging != null) {
                return firebaseMessaging;
            }
            String resolvedServiceAccountPath = resolveServiceAccountPath();
            if (!StringUtils.hasText(resolvedServiceAccountPath)) {
                log.info("FCM is disabled because neither FIREBASE_SERVICE_ACCOUNT_PATH nor GOOGLE_APPLICATION_CREDENTIALS is configured.");
                return null;
            }

            Path credentialsPath = Path.of(resolvedServiceAccountPath);
            if (!Files.exists(credentialsPath)) {
                log.warn("FCM is disabled because the Firebase service account file was not found at {}", credentialsPath);
                return null;
            }

            try (InputStream serviceAccountStream = Files.newInputStream(credentialsPath)) {
                FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccountStream));

                if (StringUtils.hasText(databaseUrl)) {
                    optionsBuilder.setDatabaseUrl(databaseUrl.trim());
                }

                FirebaseApp firebaseApp = getOrCreateFirebaseApp(optionsBuilder.build());
                firebaseMessaging = FirebaseMessaging.getInstance(firebaseApp);
                log.info("Firebase Admin SDK initialized for FCM notifications.");
                return firebaseMessaging;
            } catch (IOException exception) {
                log.error("Unable to initialize Firebase Admin SDK for FCM.", exception);
                return null;
            }
        }
    }

    String resolveServiceAccountPath() {
        if (StringUtils.hasText(serviceAccountPath)) {
            return serviceAccountPath.trim();
        }

        String googleApplicationCredentials = googleApplicationCredentialsEnv();
        if (StringUtils.hasText(googleApplicationCredentials)) {
            return googleApplicationCredentials.trim();
        }

        return "";
    }

    String googleApplicationCredentialsEnv() {
        return System.getenv(GOOGLE_APPLICATION_CREDENTIALS);
    }

    private FirebaseApp getOrCreateFirebaseApp(FirebaseOptions options) {
        try {
            return FirebaseApp.getInstance(FIREBASE_APP_NAME);
        } catch (IllegalStateException ignored) {
            return FirebaseApp.initializeApp(options, FIREBASE_APP_NAME);
        }
    }

    private boolean shouldClearStoredToken(FirebaseMessagingException exception) {
        MessagingErrorCode errorCode = exception.getMessagingErrorCode();
        return errorCode == MessagingErrorCode.UNREGISTERED
                || errorCode == MessagingErrorCode.INVALID_ARGUMENT;
    }

    private void clearStoredToken(UUID userId, String failedToken) {
        usuarioRepository.findById(userId).ifPresent(usuario -> {
            if (failedToken.equals(usuario.getFcmToken())) {
                usuario.setFcmToken(null);
                usuarioRepository.save(usuario);
                log.info("FCM token cleared for user {} after an unrecoverable Firebase response.", userId);
            }
        });
    }

    private void queueFailedNotification(UUID userId,
                                         String title,
                                         String body,
                                         Map<String, String> payload,
                                         String errorMessage) {
        FailedNotification queuedNotification = failedNotificationRepository.save(FailedNotification.builder()
                .userId(userId)
                .notificationType(payload.getOrDefault("type", "GENERIC"))
                .title(title)
                .body(body)
                .payloadData(new LinkedHashMap<>(payload))
                .lastError(errorMessage)
                .build());

        log.info("Queued failed FCM notification {} for retry", queuedNotification.getId());
    }

    private void scheduleRetry(FailedNotification failedNotification, LocalDateTime now, String errorMessage) {
        int nextRetryCount = failedNotification.getRetryCount() + 1;
        failedNotification.setRetryCount(nextRetryCount);
        failedNotification.setLastAttemptAt(now);
        failedNotification.setLastError(errorMessage);

        if (nextRetryCount >= MAX_RETRY_COUNT) {
            failedNotification.setPermanentlyFailedAt(now);
            failedNotification.setNextAttemptAt(now);
            failedNotificationRepository.save(failedNotification);
            log.error("FCM notification {} reached the retry limit and is now permanently failed.", failedNotification.getId());
            return;
        }

        failedNotification.setNextAttemptAt(now.plusHours(1));
        failedNotificationRepository.save(failedNotification);
    }

    private void markAsPermanentlyFailed(FailedNotification failedNotification, LocalDateTime now, String errorMessage) {
        failedNotification.setLastAttemptAt(now);
        failedNotification.setLastError(errorMessage);
        failedNotification.setPermanentlyFailedAt(now);
        failedNotificationRepository.save(failedNotification);
        log.error("FCM notification {} marked as permanently failed: {}", failedNotification.getId(), errorMessage);
    }
}
