package com.ecobook.model.enums;

/**
 * Curriculum system
 */
public enum SistemaEnsino {
    ANGLO("Anglo"),
    OBJETIVO("Objetivo"),
    COC("COC"),
    POSITIVO("Positivo"),
    OUTRO("Outro");

    private final String label;

    SistemaEnsino(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
