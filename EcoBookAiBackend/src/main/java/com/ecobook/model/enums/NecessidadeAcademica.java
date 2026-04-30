package com.ecobook.model.enums;

/**
 * Student academic needs
 */
public enum NecessidadeAcademica {
    TEXTBOOKS("Livros didáticos"),
    WORKBOOKS("Cadernos de exercícios"),
    REFERENCE_MATERIALS("Materiais de referência"),
    FICTION("Obras de ficção"),
    TECHNICAL_BOOKS("Livros técnicos"),
    TEST_PREP("Preparação para testes");

    private final String description;

    NecessidadeAcademica(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
