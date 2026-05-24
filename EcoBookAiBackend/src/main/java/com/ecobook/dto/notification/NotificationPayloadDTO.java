package com.ecobook.dto.notification;

import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Builder
public class NotificationPayloadDTO {

    private final String notificationId;
    private final NotificationType type;
    private final String title;
    private final String body;
    private final String route;
    private final String requestId;
    private final String materialId;
    @Builder.Default
    private final Map<String, String> metadata = Map.of();

    /**
     * Converts the notification payload into a Firebase-compatible data map.
     * @return result of the operation
     */
    public Map<String, String> toDataMap() {
        LinkedHashMap<String, String> data = new LinkedHashMap<>();
        putIfHasText(data, "notification_id", notificationId);
        putIfHasText(data, "type", type == null ? null : type.name());
        putIfHasText(data, "title", title);
        putIfHasText(data, "body", body);
        putIfHasText(data, "route", route);
        putIfHasText(data, "solicitacao_id", requestId);
        putIfHasText(data, "material_id", materialId);
        metadata.forEach((key, value) -> putIfHasText(data, key, value));
        return data;
    }

    private void putIfHasText(Map<String, String> target, String key, String value) {
        if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
            target.put(key, value.trim());
        }
    }
}
