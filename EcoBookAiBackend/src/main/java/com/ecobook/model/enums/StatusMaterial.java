package com.ecobook.model.enums;

/**
 * Material lifecycle status
 */
public enum StatusMaterial {
    DISPONIVEL("Disponível para requisição"),
    RESERVADO("Reservado por estudante"),
    DOADO("Doado (transação concluída)"),
    CANCELADO("Cancelado pelo doador");

    private final String description;

    StatusMaterial(String description) {
        this.description = description;
    }

    /**
     * Returns the d es cr ip ti on.
     * @return requested value
     */
    public String getDescription() {
        return description;
    }
}
