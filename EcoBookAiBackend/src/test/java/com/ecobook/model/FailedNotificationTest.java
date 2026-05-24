package com.ecobook.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FailedNotificationTest {

    @Test
    @DisplayName("onCreate should backfill timestamps and next attempt when they are missing")
    void shouldInitializeLifecycleFieldsOnCreate() {
        FailedNotification notification = FailedNotification.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .notificationType("REQUEST_APPROVED")
                .title("Titulo")
                .body("Corpo")
                .payloadData(new LinkedHashMap<>())
                .retryCount(0)
                .createdAt(null)
                .updatedAt(null)
                .nextAttemptAt(null)
                .build();

        notification.onCreate();

        assertThat(notification.getCreatedAt()).isNotNull();
        assertThat(notification.getUpdatedAt()).isEqualTo(notification.getCreatedAt());
        assertThat(notification.getNextAttemptAt()).isEqualTo(notification.getCreatedAt().plusHours(1));
    }

    @Test
    @DisplayName("onUpdate should refresh updatedAt without disturbing existing timestamps")
    void shouldRefreshUpdatedAtOnUpdate() {
        LocalDateTime createdAt = LocalDateTime.now().minusHours(2);
        LocalDateTime updatedAt = createdAt.plusMinutes(15);
        LocalDateTime nextAttemptAt = createdAt.plusHours(1);

        FailedNotification notification = FailedNotification.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .notificationType("REQUEST_APPROVED")
                .title("Titulo")
                .body("Corpo")
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .nextAttemptAt(nextAttemptAt)
                .build();

        notification.onCreate();
        notification.onUpdate();

        assertThat(notification.getCreatedAt()).isEqualTo(createdAt);
        assertThat(notification.getNextAttemptAt()).isEqualTo(nextAttemptAt);
        assertThat(notification.getUpdatedAt()).isAfterOrEqualTo(updatedAt);
    }
}
