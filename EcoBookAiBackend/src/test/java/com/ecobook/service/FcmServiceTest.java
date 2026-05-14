package com.ecobook.service;

import com.ecobook.repository.FailedNotificationRepository;
import com.ecobook.model.FailedNotification;
import com.ecobook.model.Usuario;
import com.ecobook.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FcmServiceTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final FailedNotificationRepository failedNotificationRepository = mock(FailedNotificationRepository.class);
    private final FcmService fcmService = new FcmService(usuarioRepository, failedNotificationRepository);

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
}
