package com.ecobook.model.enums;

/**
 * AI classification status
 */
public enum StatusIA {
    SUCCESS("Classificação bem-sucedida"),
    LOW_CONFIDENCE("Confiança baixa na classificação"),
    FAILURE("Falha na classificação"),
    NOT_ATTEMPTED("Não tentado");

    private final String description;

    StatusIA(String description) {
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
