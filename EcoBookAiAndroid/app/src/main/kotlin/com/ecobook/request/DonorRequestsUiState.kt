package com.ecobook.request

import com.ecobook.dto.SolicitacaoDTO

enum class DonorRequestsTab(val label: String) {
    PENDING("Pendentes"),
    APPROVED("Aprovadas")
}

data class DonorRequestsUiState(
    val pendingRequests: List<SolicitacaoDTO> = emptyList(),
    val approvedRequests: List<SolicitacaoDTO> = emptyList(),
    val selectedTab: DonorRequestsTab = DonorRequestsTab.PENDING,
    val isLoading: Boolean = false,
    val activeRequestId: String? = null,
    val errorMessage: String? = null,
    val toastMessage: String? = null
) {
    val visibleRequests: List<SolicitacaoDTO>
        get() = when (selectedTab) {
            DonorRequestsTab.PENDING -> pendingRequests
            DonorRequestsTab.APPROVED -> approvedRequests
        }
}
