package com.ecobook.model.enums;

/**
 * Educational discipline/subject
 */
public enum Disciplina {
    TODAS("Todas"),
    MATEMATICA("Matematica"),
    PORTUGUES("Portugues"),
    HISTORIA("Historia"),
    GEOGRAFIA("Geografia"),
    CIENCIAS("Ciencias"),
    LITERATURA("Literatura");

    private final String label;

    Disciplina(String label) {
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
