package com.ecobook.model.enums;

/**
 * Educational discipline/subject
 */
public enum Disciplina {
    TODAS("Todas"),
    MATEMATICA("Matemática"),
    PORTUGUES("Português"),
    HISTORIA("História"),
    GEOGRAFIA("Geografia"),
    CIENCIAS("Ciências"),
    LITERATURA("Literatura");

    private final String label;

    Disciplina(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
