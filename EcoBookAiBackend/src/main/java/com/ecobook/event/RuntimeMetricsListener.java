package com.ecobook.event;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class RuntimeMetricsListener {

    private final MeterRegistry meterRegistry;

    /**
     * Updates runtime metrics after a notification request is published.
     * @param event published domain event payload
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationRequested(NotificationRequestedEvent event) {
        String type = event.payload() == null || event.payload().getType() == null
                ? "unknown"
                : event.payload().getType().name().toLowerCase(Locale.ROOT);

        Counter.builder("ecobook.notification.requested.total")
                .description("Notifications requested after a committed business event")
                .tag("type", type)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Updates runtime metrics after a profile is completed.
     * @param event published domain event payload
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProfileCompleted(ProfileCompletedEvent event) {
        Counter.builder("ecobook.profile.completed.total")
                .description("User profiles that crossed the completeness gate")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Updates runtime metrics after a non-receipt report is created.
     * @param event published domain event payload
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNonReceiptReportCreated(NonReceiptReportCreatedEvent event) {
        Counter.builder("ecobook.report.non_receipt.created.total")
                .description("Non-receipt reports created after donation completion")
                .register(meterRegistry)
                .increment();
    }
}
