package com.ecobook.model.enums;

/**
 * Material conservation state
 */
public enum EstadoConservacao {
    NOVO("Novo"),
    BOM("Bom"),
    USADO("Usado"),
    DANIFICADO("Danificado");

    private final String label;

    EstadoConservacao(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
