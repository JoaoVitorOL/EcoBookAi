package com.ecobook.repository;

import com.ecobook.model.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

    List<UserNotification> findTop100ByUserIdOrderByCreatedAtDesc(UUID userId);

    List<UserNotification> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(UUID userId);

    Optional<UserNotification> findByUserIdAndNotificationId(UUID userId, String notificationId);
}
