package com.ecobook.service;

import com.ecobook.model.Usuario;
import com.ecobook.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FcmServiceTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final FcmService fcmService = new FcmService(usuarioRepository);

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
    @DisplayName("sendNotification should stay disabled when no Firebase service account path is configured")
    void shouldSkipWhenFirebaseIsNotConfigured() {
        UUID userId = UUID.randomUUID();
        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(Usuario.builder()
                .id(userId)
                .email("token@example.com")
                .passwordHash("hash")
                .nome("Token User")
                .fcmToken("device-token")
                .build()));

        ReflectionTestUtils.setField(fcmService, "serviceAccountPath", "");

        assertThat(fcmService.sendNotification(userId.toString(), "EcoBook", "Teste")).isFalse();
    }
}
