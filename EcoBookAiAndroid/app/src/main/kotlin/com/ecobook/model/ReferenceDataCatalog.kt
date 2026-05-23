package com.ecobook.model

data class ReferenceDataCatalog(
    val disciplinas: List<Disciplina>,
    val niveisEnsino: List<NivelEnsino>,
    val sistemasEnsino: List<SistemaEnsino>,
    val estadosConservacao: List<EstadoConservacao>,
    val necessidadesAcademicas: List<NecessidadeAcademica>
) {
    companion object {
        fun defaults() = ReferenceDataCatalog(
            disciplinas = Disciplina.entries.toList(),
            niveisEnsino = NivelEnsino.entries.toList(),
            sistemasEnsino = SistemaEnsino.entries.toList(),
            estadosConservacao = EstadoConservacao.entries.toList(),
            necessidadesAcademicas = NecessidadeAcademica.entries.toList()
        )
    }
}
