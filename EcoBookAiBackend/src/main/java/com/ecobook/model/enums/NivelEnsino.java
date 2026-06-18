package com.ecobook.model.enums;

/**
 * Education level
 */
public enum NivelEnsino {
    FUNDAMENTAL("Ensino Fundamental"),
    MEDIO("Ensino Medio"),
    SUPERIOR("Ensino Superior");

    private final String label;

    NivelEnsino(String label) {
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
