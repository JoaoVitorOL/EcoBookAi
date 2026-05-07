package com.ecobook.service;

import com.ecobook.model.Usuario;
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

    private final UsuarioRepository usuarioRepository;

    @Value("${firebase.service-account-path:}")
    private String serviceAccountPath;

    @Value("${firebase.database-url:}")
    private String databaseUrl;

    private volatile FirebaseMessaging firebaseMessaging;
    private volatile boolean initializationAttempted;

    @Transactional(readOnly = true)
    public boolean sendNotification(String userId, String title, String body) {
        return sendNotification(userId, title, body, Map.of());
    }

    @Transactional(readOnly = true)
    public boolean sendNotification(String userId, String title, String body, Map<String, String> data) {
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
            }
            return false;
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

    private FirebaseMessaging getFirebaseMessagingClient() {
        if (firebaseMessaging != null) {
            return firebaseMessaging;
        }

        synchronized (this) {
            if (firebaseMessaging != null) {
                return firebaseMessaging;
            }
            if (initializationAttempted) {
                return null;
            }

            initializationAttempted = true;
            if (!StringUtils.hasText(serviceAccountPath)) {
                log.info("FCM is disabled because FIREBASE_SERVICE_ACCOUNT_PATH is not configured.");
                return null;
            }

            Path credentialsPath = Path.of(serviceAccountPath.trim());
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
}
