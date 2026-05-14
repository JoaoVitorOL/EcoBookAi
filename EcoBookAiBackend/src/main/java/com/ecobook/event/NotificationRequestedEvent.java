package com.ecobook.event;

import com.ecobook.dto.notification.NotificationPayloadDTO;

import java.util.UUID;

public record NotificationRequestedEvent(UUID recipientUserId, NotificationPayloadDTO payload) {
}
