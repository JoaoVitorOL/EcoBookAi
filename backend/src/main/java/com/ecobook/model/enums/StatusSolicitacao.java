package com.ecobook.model.enums;

/**
 * Request/Solicitation lifecycle status
 */
public enum StatusSolicitacao {
    PENDENTE("Aguardando resposta do doador"),
    APROVADA("Aprovada pelo doador"),
    RECUSADA("Recusada pelo doador"),
    CANCELADA("Cancelada"),
    CONCLUIDA("Concluída - Material doado");

    private final String description;

    StatusSolicitacao(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
