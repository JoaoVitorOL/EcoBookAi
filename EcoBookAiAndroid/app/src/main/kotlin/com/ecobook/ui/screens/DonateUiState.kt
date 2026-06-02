package com.ecobook.ui.screens

import com.ecobook.dto.MaterialDTO
import com.ecobook.model.Disciplina
import com.ecobook.model.EstadoConservacao
import com.ecobook.model.NecessidadeAcademica
import com.ecobook.model.NivelEnsino
import com.ecobook.model.ReferenceDataCatalog
import com.ecobook.model.SistemaEnsino

data class DonateMaterialDraft(
    val titulo: String = "",
    val autor: String = "",
    val editora: String = "",
    val descricao: String = "",
    val disciplina: Disciplina? = null,
    val nivelEnsino: NivelEnsino? = null,
    val ano: String = "",
    val sistemaEnsino: SistemaEnsino? = null,
    val estadoConservacao: EstadoConservacao? = null,
    val necessidadeAcademica: NecessidadeAcademica? = null,
    val dataPublicacao: String = ""
)

data class DonateUiState(
    val materials: List<MaterialDTO> = emptyList(),
    val disciplinas: List<Disciplina> = ReferenceDataCatalog.defaults().disciplinas,
    val niveisEnsino: List<NivelEnsino> = ReferenceDataCatalog.defaults().niveisEnsino,
    val sistemasEnsino: List<SistemaEnsino> = ReferenceDataCatalog.defaults().sistemasEnsino,
    val estadosConservacao: List<EstadoConservacao> = ReferenceDataCatalog.defaults().estadosConservacao,
    val necessidadesAcademicas: List<NecessidadeAcademica> = ReferenceDataCatalog.defaults().necessidadesAcademicas,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
    val toastMessage: String? = null,
    val editorMessage: String? = null,
    val materialBeingEdited: MaterialDTO? = null,
    val pendingDeleteMaterial: MaterialDTO? = null,
    val editDraft: DonateMaterialDraft = DonateMaterialDraft(),
    val validationErrors: Map<String, String> = emptyMap()
) {
    val visibleMaterials: List<MaterialDTO>
        get() = materials.filter { material ->
            material.status == "DISPONIVEL" || material.status == "RESERVADO"
        }
}
