package com.ecobook.dto.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    SOLICITACAO_RECEBIDA("donor-requests"),
    SOLICITACAO_APROVADA("my-requests"),
    SOLICITACAO_RECUSADA("my-requests"),
    SOLICITACAO_CANCELADA("my-requests"),
    MATERIAL_DOADO("my-requests"),
    MATERIAL_CANCELADO("my-requests");

    private final String route;
}
