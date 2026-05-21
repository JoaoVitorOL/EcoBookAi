package com.ecobook.event;

public record NonReceiptReportCreatedEvent(
        String reportId,
        String materialId,
        String solicitacaoId,
        String estudanteId
) {
}
