package com.ecobook.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogTest {

    @Test
    @DisplayName("onCreate should populate createdAt when it is missing")
    void shouldInitializeCreatedAtOnCreate() {
        AuditLog auditLog = AuditLog.builder()
                .id(UUID.randomUUID())
                .action("USER_EXPORT")
                .createdAt(null)
                .build();

        auditLog.onCreate();

        assertThat(auditLog.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("onCreate should preserve an explicit createdAt value")
    void shouldPreserveExistingCreatedAtOnCreate() {
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(30);
        AuditLog auditLog = AuditLog.builder()
                .id(UUID.randomUUID())
                .action("USER_EXPORT")
                .createdAt(createdAt)
                .build();

        auditLog.onCreate();

        assertThat(auditLog.getCreatedAt()).isEqualTo(createdAt);
    }
}
