package com.ecobook.service;

import com.ecobook.dto.notification.NotificationPayloadDTO;
import com.ecobook.dto.notification.NotificationType;
import com.ecobook.model.Usuario;
import com.ecobook.model.UserNotification;
import com.ecobook.repository.UserNotificationRepository;
import com.ecobook.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Test
    @DisplayName("recordNotification should ignore payloads without a stable notification id")
    void shouldIgnoreNotificationsWithoutStableId() {
        userNotificationService.recordNotification(UUID.randomUUID(), NotificationPayloadDTO.builder()
                .notificationId(" ")
                .title("Sem id")
                .body("Nao deve persistir")
                .build());

        verifyNoInteractions(userNotificationRepository);
    }

    @Test
    @DisplayName("listCurrentUserNotifications should map unread state and metadata for the current user")
    void shouldListCurrentUserNotifications() {
        UUID userId = UUID.randomUUID();
        Usuario usuario = Usuario.builder()
                .id(userId)
                .email("notify@example.com")
                .nome("Pessoa Notificada")
                .build();
        UserNotification notification = UserNotification.builder()
                .userId(userId)
                .notificationId("notif-789")
                .notificationType("SOLICITACAO_APROVADA")
                .title("Aprovada")
                .body("Sua solicitacao foi aprovada")
                .route("my-requests")
                .requestId(UUID.randomUUID())
                .materialId(UUID.randomUUID())
                .payloadData(Map.of("material_titulo", "Colecao Fisica"))
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("notify@example.com")).thenReturn(Optional.of(usuario));
        when(userNotificationRepository.findTop100ByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(notification));

        assertThat(userNotificationService.listCurrentUserNotifications("notify@example.com"))
                .singleElement()
                .satisfies(dto -> {
                    assertThat(dto.getId()).isEqualTo("notif-789");
                    assertThat(dto.getNotificationType()).isEqualTo("SOLICITACAO_APROVADA");
                    assertThat(dto.isUnread()).isTrue();
                    assertThat(dto.getMetadata()).containsEntry("material_titulo", "Colecao Fisica");
                    assertThat(dto.getRequestId()).isEqualTo(notification.getRequestId().toString());
                    assertThat(dto.getMaterialId()).isEqualTo(notification.getMaterialId().toString());
                });
    }

    @Test
    @DisplayName("markAsRead should persist the first read timestamp only once")
    void shouldMarkNotificationAsReadOnlyOnce() {
        UUID userId = UUID.randomUUID();
        Usuario usuario = Usuario.builder()
                .id(userId)
                .email("reader@example.com")
                .build();
        UserNotification unread = UserNotification.builder()
                .userId(userId)
                .notificationId("notif-read")
                .notificationType("GENERIC")
                .title("Titulo")
                .body("Corpo")
                .route("notifications")
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("reader@example.com")).thenReturn(Optional.of(usuario));
        when(userNotificationRepository.findByUserIdAndNotificationId(userId, "notif-read")).thenReturn(Optional.of(unread));

        userNotificationService.markAsRead("reader@example.com", "notif-read");

        assertThat(unread.getReadAt()).isNotNull();
        verify(userNotificationRepository).save(unread);
    }

    @Test
    @DisplayName("markAsRead should not rewrite notifications that were already read")
    void shouldNotRewriteAlreadyReadNotifications() {
        UUID userId = UUID.randomUUID();
        Usuario usuario = Usuario.builder()
                .id(userId)
                .email("already-read@example.com")
                .build();
        UserNotification alreadyRead = UserNotification.builder()
                .userId(userId)
                .notificationId("notif-read")
                .notificationType("GENERIC")
                .title("Titulo")
                .body("Corpo")
                .route("notifications")
                .readAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("already-read@example.com")).thenReturn(Optional.of(usuario));
        when(userNotificationRepository.findByUserIdAndNotificationId(userId, "notif-read")).thenReturn(Optional.of(alreadyRead));

        userNotificationService.markAsRead("already-read@example.com", "notif-read");

        verify(userNotificationRepository, never()).save(alreadyRead);
    }

    @Test
    @DisplayName("markAllAsRead should stamp every unread notification and save them in batch")
    void shouldMarkAllUnreadNotificationsAsRead() {
        UUID userId = UUID.randomUUID();
        Usuario usuario = Usuario.builder()
                .id(userId)
                .email("batch-reader@example.com")
                .build();
        UserNotification first = UserNotification.builder()
                .userId(userId)
                .notificationId("notif-1")
                .notificationType("GENERIC")
                .title("Primeira")
                .body("Corpo")
                .route("notifications")
                .build();
        UserNotification second = UserNotification.builder()
                .userId(userId)
                .notificationId("notif-2")
                .notificationType("GENERIC")
                .title("Segunda")
                .body("Corpo")
                .route("notifications")
                .build();

        when(usuarioRepository.findByEmailIgnoreCase("batch-reader@example.com")).thenReturn(Optional.of(usuario));
        when(userNotificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(first, second));

        userNotificationService.markAllAsRead("batch-reader@example.com");

        assertThat(first.getReadAt()).isNotNull();
        assertThat(second.getReadAt()).isNotNull();
        assertThat(first.getReadAt()).isEqualTo(second.getReadAt());
        verify(userNotificationRepository).saveAll(anyList());
    }
}
