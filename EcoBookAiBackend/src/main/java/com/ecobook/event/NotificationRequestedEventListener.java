package com.ecobook.event;

import com.ecobook.service.FcmService;
import com.ecobook.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationRequestedEventListener {

    private final FcmService fcmService;
    private final UserNotificationService userNotificationService;

    /**
     * Handles notification dispatch requests after they are published.
     * @param event published domain event payload
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationRequested(NotificationRequestedEvent event) {
        userNotificationService.recordNotification(event.recipientUserId(), event.payload());
        fcmService.sendNotification(event.recipientUserId(), event.payload());
    }
}
