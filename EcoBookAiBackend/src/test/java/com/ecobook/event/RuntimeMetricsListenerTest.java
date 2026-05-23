package com.ecobook.event;

import com.ecobook.dto.notification.NotificationPayloadDTO;
import com.ecobook.dto.notification.NotificationType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeMetricsListenerTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final RuntimeMetricsListener listener = new RuntimeMetricsListener(meterRegistry);

    @Test
    @DisplayName("should increment a tagged notification counter for committed notification events")
    void shouldCountRequestedNotifications() {
        listener.onNotificationRequested(new NotificationRequestedEvent(
                UUID.randomUUID(),
                NotificationPayloadDTO.builder()
                        .notificationId(UUID.randomUUID().toString())
                        .type(NotificationType.SOLICITACAO_APROVADA)
                        .title("Solicitacao aprovada")
                        .body("Teste")
                        .route(NotificationType.SOLICITACAO_APROVADA.getRoute())
                        .build()
        ));

        assertThat(meterRegistry.get("ecobook.notification.requested.total")
                .tag("type", "solicitacao_aprovada")
                .counter()
                .count()).isEqualTo(1.0d);
    }

    @Test
    @DisplayName("should count completed profiles")
    void shouldCountCompletedProfiles() {
        listener.onProfileCompleted(new ProfileCompletedEvent(UUID.randomUUID(), "profile@example.com"));

        assertThat(meterRegistry.get("ecobook.profile.completed.total")
                .counter()
                .count()).isEqualTo(1.0d);
    }

    @Test
    @DisplayName("should count created non-receipt reports")
    void shouldCountCreatedNonReceiptReports() {
        listener.onNonReceiptReportCreated(new NonReceiptReportCreatedEvent(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        ));

        assertThat(meterRegistry.get("ecobook.report.non_receipt.created.total")
                .counter()
                .count()).isEqualTo(1.0d);
    }
}
