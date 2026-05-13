package com.ecobook.discovery

import com.ecobook.dto.MaterialDTO
import com.ecobook.model.Disciplina
import com.ecobook.model.NivelEnsino
import com.ecobook.model.SistemaEnsino

enum class DiscoveryNavigation {
    MY_REQUESTS
}

data class DiscoveryFilters(
    val query: String = "",
    val disciplina: Disciplina? = null,
    val nivelEnsino: NivelEnsino? = null,
    val ano: String = "",
    val sistemaEnsino: SistemaEnsino? = null,
    val cidade: String = "",
    val bairro: String = "",
    val minAnoPublicacao: String = "",
    val maxAnoPublicacao: String = ""
)

data class DiscoveryUiState(
    val filters: DiscoveryFilters = DiscoveryFilters(),
    val activeFilters: DiscoveryFilters = DiscoveryFilters(),
    val results: List<MaterialDTO> = emptyList(),
    val page: Int = 0,
    val pageSize: Int = 20,
    val total: Long = 0,
    val hasNext: Boolean = false,
    val isLoadingInitial: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasSearched: Boolean = false,
    val errorMessage: String? = null,
    val toastMessage: String? = null,
    val selectedMaterial: MaterialDTO? = null,
    val requestingMaterialId: String? = null,
    val pendingNavigation: DiscoveryNavigation? = null
) {
    val isLoading: Boolean
        get() = isLoadingInitial || isLoadingMore
}
