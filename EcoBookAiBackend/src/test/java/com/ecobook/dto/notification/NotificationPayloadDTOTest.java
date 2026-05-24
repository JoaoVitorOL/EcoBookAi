package com.ecobook.dto.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationPayloadDTOTest {

    @Test
    @DisplayName("toDataMap should serialize standard notification keys and trimmed metadata")
    void shouldSerializeNotificationPayloadToDataMap() {
        NotificationPayloadDTO payload = NotificationPayloadDTO.builder()
                .notificationId(" notif-123 ")
                .type(NotificationType.SOLICITACAO_APROVADA)
                .title(" Titulo ")
                .body(" Corpo ")
                .route(" my-requests ")
                .requestId(" req-1 ")
                .materialId(" mat-1 ")
                .metadata(Map.of(" custom_key ", " valor "))
                .build();

        assertThat(payload.toDataMap())
                .containsEntry("notification_id", "notif-123")
                .containsEntry("type", "SOLICITACAO_APROVADA")
                .containsEntry("title", "Titulo")
                .containsEntry("body", "Corpo")
                .containsEntry("route", "my-requests")
                .containsEntry("solicitacao_id", "req-1")
                .containsEntry("material_id", "mat-1")
                .containsEntry(" custom_key ", "valor");
    }

    @Test
    @DisplayName("toDataMap should skip blank core fields and metadata values")
    void shouldSkipBlankValues() {
        NotificationPayloadDTO payload = NotificationPayloadDTO.builder()
                .notificationId(" ")
                .title(null)
                .metadata(Map.of("material_titulo", " ", "route", "inbox"))
                .build();

        assertThat(payload.toDataMap()).containsExactlyEntriesOf(Map.of("route", "inbox"));
    }
}
