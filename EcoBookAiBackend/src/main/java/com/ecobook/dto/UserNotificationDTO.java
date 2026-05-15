package com.ecobook.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class UserNotificationDTO {

    private final String id;
    private final String title;
    private final String body;
    private final String notificationType;
    private final String route;
    private final String requestId;
    private final String materialId;
    private final LocalDateTime receivedAt;
    private final boolean unread;
    @Builder.Default
    private final Map<String, String> metadata = Map.of();
}
