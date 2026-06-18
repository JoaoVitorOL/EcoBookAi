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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MyRequestsViewModel @Inject constructor(
    private val requestRepository: RequestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyRequestsUiState())
    val uiState: StateFlow<MyRequestsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isLoading) {
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { requestRepository.listMyRequests(_uiState.value.selectedFilter.status) }
                .onSuccess { requests ->
                    _uiState.update {
                        it.copy(
                            requests = requests,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = resolveLoadError(error)
                        )
                    }
                }
        }
    }

    fun updateFilter(filter: MyRequestFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        refresh()
    }

    fun cancelRequest(requestId: String) {
        _uiState.update { it.copy(activeRequestId = requestId, errorMessage = null) }
        viewModelScope.launch {
            runCatching { requestRepository.cancelRequest(requestId) }
                .onSuccess { updatedRequest ->
                    _uiState.update { state ->
                        state.copy(
                            requests = state.requests.map { current ->
                                if (current.id == updatedRequest.id) updatedRequest else current
                            },
                            activeRequestId = null,
                            toastMessage = "Solicitação cancelada com sucesso."
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

    fun reportNonReceipt(request: com.ecobook.dto.SolicitacaoDTO, reason: String) {
        val normalizedReason = reason.trim()
        if (normalizedReason.isBlank()) {
            _uiState.update {
                it.copy(toastMessage = "Informe o motivo do reporte.")
            }
            return
        }

        _uiState.update { it.copy(activeRequestId = request.id, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                requestRepository.reportNonReceipt(
                    materialId = request.materialId,
                    reason = normalizedReason
                )
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        activeRequestId = null,
                        reportedRequestIds = state.reportedRequestIds + request.id,
                        toastMessage = "Reporte enviado. A equipe vai revisar o caso."
                    )
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        activeRequestId = null,
                        reportedRequestIds = if (error is ApiException && error.statusCode == 409) {
                            state.reportedRequestIds + request.id
                        } else {
                            state.reportedRequestIds
                        },
                        toastMessage = resolveReportError(error)
                    )
                }
            }
        }
    }

    fun consumeToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    private fun resolveLoadError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                401 -> "Sua sessão expirou. Entre novamente para continuar."
                403 -> "Conclua seu cadastro para acompanhar suas solicitações."
                else -> error.message
            }

            is SocketTimeoutException -> "A listagem demorou demais para responder. Se o backend acabou de iniciar, aguarde o boot terminar e tente novamente."
            is ConnectException,
            is UnknownHostException,
            is IOException -> "Não foi possível conectar ao backend configurado no app."
            else -> error.message ?: "Falha inesperada ao carregar suas solicitações."
        }
    }

    private fun resolveActionError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                401 -> "Sua sessão expirou. Entre novamente para continuar."
                403 -> "Você não tem permissão para alterar essa solicitação."
                404 -> "Essa solicitação não foi encontrada."
                422 -> error.message
                else -> error.message
            }

            is ConnectException,
            is UnknownHostException,
            is IOException -> "Não foi possível atualizar a solicitação porque o backend não respondeu."
            else -> error.message ?: "Falha inesperada ao atualizar a solicitação."
        }
    }

    private fun resolveReportError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                400 -> error.fieldErrors["reason"] ?: error.message
                401 -> "Sua sessão expirou. Entre novamente para continuar."
                403 -> "Somente o estudante da solicitação concluída pode enviar esse reporte."
                404 -> "Esse material não foi encontrado."
                409 -> "Já existe um reporte aberto para esse material."
                422 -> "Esse material ainda não pode ser reportado como não recebido."
                else -> error.message
            }

            is ConnectException,
            is UnknownHostException,
            is IOException -> "Não foi possível enviar o reporte porque o backend não respondeu."
            else -> error.message ?: "Falha inesperada ao enviar o reporte."
        }
    }
}
