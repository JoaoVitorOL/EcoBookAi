package com.ecobook.model.enums;

/**
 * Classificações acadêmicas usadas no cadastro e na busca de materiais.
 */
public enum NecessidadeAcademica {
    TEXTBOOKS("Livros didáticos"),
    WORKBOOKS("Cadernos de exercícios"),
    REFERENCE_MATERIALS("Materiais de referência"),
    FICTION("Obras de ficção"),
    TECHNICAL_BOOKS("Livros técnicos"),
    TEST_PREP("Preparação para provas");

    private final String description;

    NecessidadeAcademica(String description) {
        this.description = description;
    }

    /**
     * Retorna a descrição exibida para a necessidade.
     * @return descrição amigável da necessidade
     */
    public String getDescription() {
        return description;
    }
}
