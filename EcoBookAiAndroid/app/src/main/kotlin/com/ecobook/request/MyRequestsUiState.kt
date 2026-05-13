package com.ecobook.request

import com.ecobook.dto.SolicitacaoDTO

enum class MyRequestFilter(val label: String, val status: String?) {
    ALL("Todas", null),
    PENDING("Pendentes", "PENDENTE"),
    APPROVED("Aprovadas", "APROVADA"),
    DECLINED("Recusadas", "RECUSADA"),
    CANCELLED("Canceladas", "CANCELADA"),
    COMPLETED("Concluidas", "CONCLUIDA")
}

data class MyRequestsUiState(
    val requests: List<SolicitacaoDTO> = emptyList(),
    val selectedFilter: MyRequestFilter = MyRequestFilter.ALL,
    val isLoading: Boolean = false,
    val activeRequestId: String? = null,
    val errorMessage: String? = null,
    val toastMessage: String? = null
) {
    val visibleRequests: List<SolicitacaoDTO>
        get() = requests.filter { request ->
            selectedFilter.status == null || request.status == selectedFilter.status
        }
}
