package com.ecobook.scheduler;

import com.ecobook.service.SolicitacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationExpiryJob {

    private final SolicitacaoService solicitacaoService;

    /**
     * Expires approved requests whose reservation window has elapsed.
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    public void expireReservations() {
        int expiredCount = solicitacaoService.expireApprovedRequests();
        if (expiredCount > 0) {
            log.info("Expired {} reservations", expiredCount);
        }
    }
}
