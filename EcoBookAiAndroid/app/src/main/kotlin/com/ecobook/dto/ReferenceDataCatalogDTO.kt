package com.ecobook.dto

data class ReferenceDataCatalogDTO(
    val disciplinas: List<ReferenceOptionDTO> = emptyList(),
    val niveisEnsino: List<ReferenceOptionDTO> = emptyList(),
    val sistemasEnsino: List<ReferenceOptionDTO> = emptyList(),
    val estadosConservacao: List<ReferenceOptionDTO> = emptyList(),
    val necessidadesAcademicas: List<ReferenceOptionDTO> = emptyList()
)
