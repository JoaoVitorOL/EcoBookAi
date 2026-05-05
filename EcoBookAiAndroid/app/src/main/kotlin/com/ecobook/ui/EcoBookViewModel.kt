package com.ecobook.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecobook.auth.SessionManager
import com.ecobook.data.AuthRepository
import com.ecobook.data.EcoBookRepository
import com.ecobook.model.BackendStatus
import com.ecobook.model.Disciplina
import com.ecobook.model.NivelEnsino
import dagger.hilt.android.lifecycle.HiltViewModel
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

    fun updateSearchQuery(query: String) {
        _uiState.update { state -> state.copy(searchQuery = query) }
    }

    fun toggleDisciplina(disciplina: Disciplina) {
        _uiState.update { state ->
            state.copy(
                selectedDisciplina = if (state.selectedDisciplina == disciplina) null else disciplina
            )
        }
    }

    fun toggleNivelEnsino(nivelEnsino: NivelEnsino) {
        _uiState.update { state ->
            state.copy(
                selectedNivelEnsino = if (state.selectedNivelEnsino == nivelEnsino) null else nivelEnsino
            )
        }
    }

    fun clearDiscoveryFilters() {
        _uiState.update { state ->
            state.copy(
                searchQuery = "",
                selectedDisciplina = null,
                selectedNivelEnsino = null
            )
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
}
