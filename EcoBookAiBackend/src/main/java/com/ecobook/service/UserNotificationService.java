package com.ecobook.service;

import com.ecobook.dto.UserNotificationDTO;
import com.ecobook.dto.notification.NotificationPayloadDTO;
import com.ecobook.exception.ResourceNotFoundException;
import com.ecobook.model.UserNotification;
import com.ecobook.model.Usuario;
import com.ecobook.repository.UserNotificationRepository;
import com.ecobook.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserNotificationService {

    private final UsuarioRepository usuarioRepository;
    private final UserNotificationRepository userNotificationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordNotification(UUID recipientUserId, NotificationPayloadDTO payload) {
        if (recipientUserId == null || payload == null || !StringUtils.hasText(payload.getNotificationId())) {
            return;
        }

        UserNotification notification = userNotificationRepository
                .findByUserIdAndNotificationId(recipientUserId, payload.getNotificationId())
                .orElseGet(() -> UserNotification.builder()
                        .userId(recipientUserId)
                        .notificationId(payload.getNotificationId())
                        .build());

        notification.setNotificationType(payload.getType() == null ? "GENERIC" : payload.getType().name());
        notification.setTitle(payload.getTitle());
        notification.setBody(payload.getBody());
        notification.setRoute(payload.getRoute());
        notification.setRequestId(parseUuid(payload.getRequestId()));
        notification.setMaterialId(parseUuid(payload.getMaterialId()));
        notification.setPayloadData(new LinkedHashMap<>(payload.getMetadata() == null ? Map.of() : payload.getMetadata()));

        UserNotification savedNotification = userNotificationRepository.saveAndFlush(notification);
        log.info(
                "Business notification persisted for user {} with notificationId {} and type {}",
                savedNotification.getUserId(),
                savedNotification.getNotificationId(),
                savedNotification.getNotificationType()
        );
    }

    @Transactional(readOnly = true)
    public List<UserNotificationDTO> listCurrentUserNotifications(String email) {
        Usuario usuario = loadUsuario(email);
        return userNotificationRepository.findTop100ByUserIdOrderByCreatedAtDesc(usuario.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void markAsRead(String email, String notificationId) {
        Usuario usuario = loadUsuario(email);
        UserNotification notification = userNotificationRepository.findByUserIdAndNotificationId(
                        usuario.getId(),
                        notificationId == null ? "" : notificationId.trim()
                )
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada"));

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            userNotificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead(String email) {
        Usuario usuario = loadUsuario(email);
        List<UserNotification> unreadNotifications = userNotificationRepository
                .findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(usuario.getId());

        if (unreadNotifications.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        unreadNotifications.forEach(notification -> notification.setReadAt(now));
        userNotificationRepository.saveAll(unreadNotifications);
    }

    private Usuario loadUsuario(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private UserNotificationDTO toDto(UserNotification notification) {
        return UserNotificationDTO.builder()
                .id(notification.getNotificationId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .notificationType(notification.getNotificationType())
                .route(notification.getRoute())
                .requestId(notification.getRequestId() == null ? null : notification.getRequestId().toString())
                .materialId(notification.getMaterialId() == null ? null : notification.getMaterialId().toString())
                .receivedAt(notification.getCreatedAt())
                .unread(notification.getReadAt() == null)
                .metadata(new LinkedHashMap<>(notification.getPayloadData() == null ? Map.of() : notification.getPayloadData()))
                .build();
    }

    private UUID parseUuid(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }

        try {
            return UUID.fromString(rawValue.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
