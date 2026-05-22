package com.ecobook.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecobook.auth.SessionManager
import com.ecobook.data.ApiException
import com.ecobook.data.AuthRepository
import com.ecobook.data.EcoBookRepository
import com.ecobook.model.BackendStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EcoBookViewModel @Inject constructor(
    private val repository: EcoBookRepository,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(repository.initialState())
    val uiState: StateFlow<EcoBookUiState> = _uiState.asStateFlow()

    init {
        observeSession()
        refreshCurrentUserSilently()
        refreshBackendStatus()
    }

    fun refreshBackendStatus() {
        _uiState.update { state ->
            state.copy(backendStatus = BackendStatus.checking())
        }

        viewModelScope.launch {
            val backendStatus = repository.loadBackendStatus()
            _uiState.update { state ->
                state.copy(backendStatus = backendStatus)
            }
        }
    }

    fun updateNome(value: String) {
        _uiState.update { state -> state.copy(profile = state.profile.copy(nome = value)) }
    }

    fun updateEmail(value: String) {
        _uiState.update { state -> state.copy(profile = state.profile.copy(email = value)) }
    }

    fun updateWhatsapp(value: String) {
        _uiState.update { state -> state.copy(profile = state.profile.copy(whatsapp = value)) }
    }

    fun updateCidade(value: String) {
        _uiState.update { state -> state.copy(profile = state.profile.copy(cidade = value)) }
    }

    fun updateBairro(value: String) {
        _uiState.update { state -> state.copy(profile = state.profile.copy(bairro = value)) }
    }

    fun updateInstituicao(value: String) {
        _uiState.update { state -> state.copy(profile = state.profile.copy(instituicao = value)) }
    }

    fun toggleConsentimentoIa() {
        _uiState.update { state ->
            state.copy(profile = state.profile.copy(consentimentoIa = !state.profile.consentimentoIa))
        }
    }

    fun updateAiConsent(enabled: Boolean) {
        if (_uiState.value.isUpdatingAiConsent) {
            return
        }

        _uiState.update { state ->
            state.copy(
                isUpdatingAiConsent = true,
                pendingAiConsent = enabled,
                profileMessage = null,
                profileMessageIsError = false
            )
        }

        viewModelScope.launch {
            runCatching { authRepository.updateAiConsent(enabled) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            profile = repository.buildProfileDraft(),
                            isUpdatingAiConsent = false,
                            pendingAiConsent = null,
                            profileMessage = if (enabled) {
                                "Consentimento para IA ativado."
                            } else {
                                "Consentimento para IA desativado."
                            },
                            profileMessageIsError = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isUpdatingAiConsent = false,
                            pendingAiConsent = null,
                            profileMessage = resolveProfileError(error),
                            profileMessageIsError = true
                        )
                    }
                }
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionManager.sessionState.collectLatest { session ->
                _uiState.update { state ->
                    state.copy(
                        session = session,
                        profile = repository.buildProfileDraft()
                    )
                }
            }
        }
    }

    private fun refreshCurrentUserSilently() {
        if (!sessionManager.hasActiveSession()) {
            return
        }

        viewModelScope.launch {
            runCatching { authRepository.refreshCurrentUser() }
                .onFailure {
                    _uiState.update { state -> state.copy(profile = repository.buildProfileDraft()) }
                }
        }
    }

    private fun resolveProfileError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                400, 422 -> error.message
                401 -> "Sua sessão expirou. Entre novamente para continuar."
                403 -> "Conclua o onboarding antes de alterar o consentimento."
                else -> "Não foi possível atualizar o consentimento agora."
            }

            is SocketTimeoutException -> "A atualização demorou demais. Tente novamente."
            is ConnectException,
            is UnknownHostException,
            is IOException -> "Não foi possível falar com o backend para atualizar o consentimento."
            else -> error.message ?: "Falha inesperada ao atualizar o consentimento."
        }
    }
}
