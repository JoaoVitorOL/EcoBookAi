package com.ecobook.scheduler;

import com.ecobook.service.FcmService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationRetryJobTest {

    private final FcmService fcmService = mock(FcmService.class);
    private final NotificationRetryJob notificationRetryJob = new NotificationRetryJob(fcmService);

    @Test
    @DisplayName("retryFailedNotifications should still ask the service to process the queue when nothing is pending")
    void shouldInvokeRetryWhenQueueIsEmpty() {
        when(fcmService.retryFailedNotifications()).thenReturn(0);

        notificationRetryJob.retryFailedNotifications();

        verify(fcmService).retryFailedNotifications();
    }

    @Test
    @DisplayName("retryFailedNotifications should keep the successful path when queued notifications were processed")
    void shouldInvokeRetryWhenQueueHasItems() {
        when(fcmService.retryFailedNotifications()).thenReturn(3);

        notificationRetryJob.retryFailedNotifications();

        verify(fcmService).retryFailedNotifications();
    }
}
