package com.ecobook.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserNotificationTest {

    @Test
    @DisplayName("onCreate should backfill createdAt and updatedAt when they are missing")
    void shouldInitializeLifecycleFieldsOnCreate() {
        UserNotification notification = UserNotification.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .notificationId("notif-123")
                .notificationType("REQUEST_APPROVED")
                .title("Titulo")
                .body("Corpo")
                .route("/requests/1")
                .createdAt(null)
                .updatedAt(null)
                .build();

        notification.onCreate();

        assertThat(notification.getCreatedAt()).isNotNull();
        assertThat(notification.getUpdatedAt()).isEqualTo(notification.getCreatedAt());
    }

    @Test
    @DisplayName("onUpdate should refresh updatedAt while preserving createdAt")
    void shouldRefreshUpdatedAtOnUpdate() {
        LocalDateTime createdAt = LocalDateTime.now().minusHours(2);
        LocalDateTime updatedAt = createdAt.plusMinutes(10);
        UserNotification notification = UserNotification.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .notificationId("notif-123")
                .notificationType("REQUEST_APPROVED")
                .title("Titulo")
                .body("Corpo")
                .route("/requests/1")
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        notification.onCreate();
        notification.onUpdate();

        assertThat(notification.getCreatedAt()).isEqualTo(createdAt);
        assertThat(notification.getUpdatedAt()).isAfterOrEqualTo(updatedAt);
    }
}
