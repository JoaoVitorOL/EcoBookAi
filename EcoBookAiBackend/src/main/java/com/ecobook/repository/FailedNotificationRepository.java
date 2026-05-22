package com.ecobook.repository;

import com.ecobook.model.FailedNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface FailedNotificationRepository extends JpaRepository<FailedNotification, UUID> {

    List<FailedNotification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<FailedNotification> findTop100ByDeliveredAtIsNullAndPermanentlyFailedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            LocalDateTime nextAttemptAt
    );
}
