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
        name = "user_notification",
        indexes = {
                @Index(name = "idx_user_notification_user_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_user_notification_user_read_at", columnList = "user_id, read_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(name = "notification_id", nullable = false, length = 100)
    private String notificationId;

    @Column(name = "notification_type", nullable = false, length = 64)
    private String notificationType;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 512)
    private String body;

    @Column(nullable = false, length = 120)
    private String route;

    @Column(name = "request_id")
    private UUID requestId;

    @Column(name = "material_id")
    private UUID materialId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_data", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> payloadData = new LinkedHashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
