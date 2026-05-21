package com.ecobook.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class NonReceiptReportCreatedEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportCreated(NonReceiptReportCreatedEvent event) {
        log.warn(
                "Non-receipt report created: reportId={}, materialId={}, solicitacaoId={}, estudanteId={}",
                event.reportId(),
                event.materialId(),
                event.solicitacaoId(),
                event.estudanteId()
        );
    }
}
