package com.ecobook.model.enums;

/**
 * Student academic needs
 */
public enum NecessidadeAcademica {
    TEXTBOOKS("Livros didaticos"),
    WORKBOOKS("Cadernos de exercicios"),
    REFERENCE_MATERIALS("Materiais de referencia"),
    FICTION("Obras de ficcao"),
    TECHNICAL_BOOKS("Livros tecnicos"),
    TEST_PREP("Preparacao para testes");

    private final String description;

    NecessidadeAcademica(String description) {
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
