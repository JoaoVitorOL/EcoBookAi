package com.ecobook.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
    name = "failed_notification",
    indexes = {
        @Index(name = "idx_failed_notification_next_attempt", columnList = "next_attempt_at"),
        @Index(name = "idx_failed_notification_user_id", columnList = "user_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class FailedNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 64)
    private String notificationType;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 512)
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_data", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> payloadData = new LinkedHashMap<>();

    @Column(nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime nextAttemptAt = LocalDateTime.now().plusHours(1);

    @Column
    private LocalDateTime lastAttemptAt;

    @Column
    private LocalDateTime deliveredAt;

    @Column(name = "permanently_failed_at")
    private LocalDateTime permanentlyFailedAt;

    @Column(length = 1000)
    private String lastError;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        if (nextAttemptAt == null) {
            nextAttemptAt = createdAt.plusHours(1);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
