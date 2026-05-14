package com.ecobook.scheduler;

import com.ecobook.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRetryJob {

    private final FcmService fcmService;

    @Scheduled(cron = "0 0 * * * *", zone = "UTC")
    public void retryFailedNotifications() {
        int processedCount = fcmService.retryFailedNotifications();
        if (processedCount > 0) {
            log.info("Processed {} queued FCM notifications", processedCount);
        }
    }
}
