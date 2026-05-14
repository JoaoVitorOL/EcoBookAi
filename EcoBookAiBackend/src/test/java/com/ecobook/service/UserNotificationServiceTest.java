package com.ecobook.service;

import com.ecobook.dto.notification.NotificationPayloadDTO;
import com.ecobook.dto.notification.NotificationType;
import com.ecobook.model.UserNotification;
import com.ecobook.repository.UserNotificationRepository;
import com.ecobook.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserNotificationServiceTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final UserNotificationRepository userNotificationRepository = mock(UserNotificationRepository.class);
    private final UserNotificationService userNotificationService = new UserNotificationService(
            usuarioRepository,
            userNotificationRepository
    );

    @Test
    @DisplayName("recordNotification should persist a backend inbox entry with the payload data")
    void shouldPersistBackendInboxEntry() {
        UUID userId = UUID.randomUUID();
        NotificationPayloadDTO payload = NotificationPayloadDTO.builder()
                .notificationId("notif-123")
                .type(NotificationType.SOLICITACAO_CANCELADA)
                .title("Solicitacao cancelada")
                .body("A solicitacao foi cancelada.")
                .route("my-requests")
                .requestId(UUID.randomUUID().toString())
                .materialId(UUID.randomUUID().toString())
                .metadata(Map.of("material_titulo", "Colecao Anglo"))
                .build();

        when(userNotificationRepository.findByUserIdAndNotificationId(userId, "notif-123"))
                .thenReturn(Optional.empty());
        when(userNotificationRepository.saveAndFlush(any(UserNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userNotificationService.recordNotification(userId, payload);

        verify(userNotificationRepository).saveAndFlush(argThat(notification ->
                notification.getUserId().equals(userId)
                        && "notif-123".equals(notification.getNotificationId())
                        && "SOLICITACAO_CANCELADA".equals(notification.getNotificationType())
                        && "Solicitacao cancelada".equals(notification.getTitle())
                        && "A solicitacao foi cancelada.".equals(notification.getBody())
                        && "my-requests".equals(notification.getRoute())
                        && "Colecao Anglo".equals(notification.getPayloadData().get("material_titulo"))
        ));
    }

    @Test
    @DisplayName("recordNotification should update an existing inbox entry when the notification id already exists")
    void shouldUpdateExistingInboxEntry() {
        UUID userId = UUID.randomUUID();
        UserNotification existingNotification = UserNotification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .notificationId("notif-456")
                .notificationType("SOLICITACAO_RECEBIDA")
                .title("Antigo")
                .body("Corpo antigo")
                .route("donor-requests")
                .build();

        NotificationPayloadDTO payload = NotificationPayloadDTO.builder()
                .notificationId("notif-456")
                .type(NotificationType.SOLICITACAO_APROVADA)
                .title("Solicitacao aprovada")
                .body("Sua solicitacao foi aprovada.")
                .route("my-requests")
                .build();

        when(userNotificationRepository.findByUserIdAndNotificationId(userId, "notif-456"))
                .thenReturn(Optional.of(existingNotification));
        when(userNotificationRepository.saveAndFlush(existingNotification))
                .thenReturn(existingNotification);

        userNotificationService.recordNotification(userId, payload);

        assertThat(existingNotification.getNotificationType()).isEqualTo("SOLICITACAO_APROVADA");
        assertThat(existingNotification.getTitle()).isEqualTo("Solicitacao aprovada");
        assertThat(existingNotification.getBody()).isEqualTo("Sua solicitacao foi aprovada.");
        assertThat(existingNotification.getRoute()).isEqualTo("my-requests");
        verify(userNotificationRepository).saveAndFlush(existingNotification);
    }
}
