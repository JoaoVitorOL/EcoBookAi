package com.ecobook.model.enums;

/**
 * Curriculum system
 */
public enum SistemaEnsino {
    ANGLO("Anglo"),
    OBJETIVO("Objetivo"),
    COC("COC"),
    POSITIVO("Positivo"),
    POLIEDRO("Poliedro"),
    ETAPA("Etapa"),
    BERNOULLI("Bernoulli"),
    SAS("SAS"),
    FTD("FTD"),
    OUTRO("Outro");

    private final String label;

    SistemaEnsino(String label) {
        this.label = label;
    }

    /**
     * Returns the l ab el.
     * @return requested value
     */
    public String getLabel() {
        return label;
    }
}
