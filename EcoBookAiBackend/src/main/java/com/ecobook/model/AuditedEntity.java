package com.ecobook.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
public abstract class AuditedEntity {

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(nullable = false)
    private boolean anonymized = false;

    /**
     * Indicates whether the entity has already been soft-deleted.
     * @return true when the condition holds; otherwise false
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Marks the entity as deleted and stores the deletion metadata.
     * @param actorUserId identifier of the actor performing the deletion
     * @param anonymized whether the deletion should also mark the entity as anonymized
     */
    public void markDeleted(UUID actorUserId, boolean anonymized) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = actorUserId;
        this.anonymized = anonymized;
    }
}
