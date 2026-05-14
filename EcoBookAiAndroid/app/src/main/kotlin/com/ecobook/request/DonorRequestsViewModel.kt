package com.ecobook.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecobook.data.ApiException
import com.ecobook.data.RequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DonorRequestsViewModel @Inject constructor(
    private val requestRepository: RequestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DonorRequestsUiState())
    val uiState: StateFlow<DonorRequestsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isLoading) {
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                coroutineScope {
                    val pendingDeferred = async { requestRepository.listPendingRequestsForDonor() }
                    val approvedDeferred = async { requestRepository.listApprovedRequestsForDonor() }
                    pendingDeferred.await() to approvedDeferred.await()
                }
            }.onSuccess { (pending, approved) ->
                _uiState.update {
                    it.copy(
                        pendingRequests = pending,
                        approvedRequests = approved,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = resolveLoadError(error)
                    )
                }
            }
        }
    }

    fun selectTab(tab: DonorRequestsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun approveRequest(requestId: String) {
        performAction(requestId) { requestRepository.approveRequest(requestId) }
    }

    fun declineRequest(requestId: String) {
        performAction(requestId) { requestRepository.declineRequest(requestId) }
    }

    fun completeDonation(requestId: String) {
        performAction(requestId) { requestRepository.completeDonation(requestId) }
    }

    fun revokeApproval(requestId: String) {
        performAction(requestId) {
            requestRepository.cancelRequest(requestId)
        }
    }

    fun consumeToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    private fun performAction(
        requestId: String,
        block: suspend () -> com.ecobook.dto.SolicitacaoDTO
    ) {
        _uiState.update { it.copy(activeRequestId = requestId, errorMessage = null) }
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { updatedRequest ->
                    _uiState.update { state ->
                        state.copy(
                            pendingRequests = when (updatedRequest.status) {
                                "APROVADA" -> state.pendingRequests.filterNot { it.id == updatedRequest.id }
                                "RECUSADA", "CANCELADA", "CONCLUIDA" -> state.pendingRequests.filterNot { it.id == updatedRequest.id }
                                else -> state.pendingRequests.map { current ->
                                    if (current.id == updatedRequest.id) updatedRequest else current
                                }
                            },
                            approvedRequests = when (updatedRequest.status) {
                                "APROVADA" -> listOf(updatedRequest) + state.approvedRequests.filterNot { it.id == updatedRequest.id }
                                "CONCLUIDA", "CANCELADA", "RECUSADA" -> state.approvedRequests.filterNot { it.id == updatedRequest.id }
                                else -> state.approvedRequests.map { current ->
                                    if (current.id == updatedRequest.id) updatedRequest else current
                                }
                            },
                            activeRequestId = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            activeRequestId = null,
                            toastMessage = resolveActionError(error)
                        )
                    }
                }
        }
    }

    private fun resolveLoadError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                401 -> "Sua sessao expirou. Entre novamente para continuar."
                403 -> "Conclua o onboarding para acompanhar os pedidos recebidos."
                else -> error.message
            }

            is SocketTimeoutException -> "A listagem demorou demais para responder."
            is ConnectException,
            is UnknownHostException,
            is IOException -> "Nao foi possivel conectar ao backend configurado no app."
            else -> error.message ?: "Falha inesperada ao carregar os pedidos recebidos."
        }
    }

    private fun resolveActionError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                401 -> "Sua sessao expirou. Entre novamente para continuar."
                403 -> "Voce nao tem permissao para alterar essa solicitacao."
                404 -> "Essa solicitacao nao foi encontrada."
                409, 422 -> error.message
                else -> error.message
            }

            is ConnectException,
            is UnknownHostException,
            is IOException -> "Nao foi possivel atualizar o pedido porque o backend nao respondeu."
            else -> error.message ?: "Falha inesperada ao atualizar o pedido."
        }
    }
}
