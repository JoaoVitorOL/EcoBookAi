package com.ecobook.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecobook.auth.SessionManager
import com.ecobook.data.ApiException
import com.ecobook.data.AuthRepository
import com.ecobook.data.EcoBookRepository
import com.ecobook.dto.UpdateProfileRequestDTO
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
        refreshConsentStatus()
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

    fun updateNome(value: String) = updateProfileField("nome") { profile ->
        profile.copy(nome = value)
    }

    fun updateEmail(value: String) = updateProfileField("email") { profile ->
        profile.copy(email = value)
    }

    fun updateWhatsapp(value: String) = updateProfileField("whatsapp") { profile ->
        profile.copy(whatsapp = WhatsAppFormatter.formatForInput(value))
    }

    fun updateCidade(value: String) = updateProfileField("cidade") { profile ->
        profile.copy(cidade = value)
    }

    fun updateBairro(value: String) = updateProfileField("bairro") { profile ->
        profile.copy(bairro = value)
    }

    fun updateInstituicao(value: String) = updateProfileField("instituicao") { profile ->
        profile.copy(instituicao = value)
    }

    fun saveProfile() {
        if (_uiState.value.isSavingProfile) {
            return
        }

        val validationErrors = validateProfile(_uiState.value.profile)
        if (validationErrors.isNotEmpty()) {
            _uiState.update { state ->
                state.copy(
                    profileFieldErrors = validationErrors,
                    profileMessage = "Revise os campos destacados antes de salvar.",
                    profileMessageIsError = true
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isSavingProfile = true,
                    profileFieldErrors = emptyMap(),
                    profileMessage = null,
                    profileMessageIsError = false
                )
            }

            val profile = _uiState.value.profile
            val request = UpdateProfileRequestDTO(
                email = profile.email.trim(),
                nome = profile.nome.trim(),
                whatsapp = WhatsAppFormatter.toBackendValue(profile.whatsapp),
                cidade = profile.cidade.trim(),
                bairro = profile.bairro.trim(),
                instituicao = profile.instituicao.trim().ifBlank { null },
                consentimentoIa = _uiState.value.pendingAiConsent ?: profile.consentimentoIa,
                necessidadesAcademicas = null
            )

            runCatching { authRepository.updateProfile(request) }
                .onSuccess { updatedProfile ->
                    val reauthRequired = !sessionManager.hasActiveSession()
                    _uiState.update { state ->
                        state.copy(
                            isSavingProfile = false,
                            profileFieldErrors = emptyMap(),
                            profileMessage = if (reauthRequired) {
                                null
                            } else {
                                "Perfil atualizado com sucesso."
                            },
                            profileMessageIsError = false
                        )
                    }
                    if (!reauthRequired && updatedProfile.consentimentoIa != _uiState.value.profile.consentimentoIa) {
                        refreshConsentStatus()
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isSavingProfile = false,
                            profileFieldErrors = error.toFieldErrors(),
                            profileMessage = resolveProfileSaveError(error),
                            profileMessageIsError = true
                        )
                    }
                }
        }
    }

    fun clearProfileMessage() {
        _uiState.update { state ->
            state.copy(
                profileMessage = null,
                profileMessageIsError = false
            )
        }
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
                    val consentStatus = runCatching { authRepository.fetchConsentStatus() }.getOrNull()
                    _uiState.update { state ->
                        state.copy(
                            profile = repository.buildProfileDraft(),
                            consentStatus = consentStatus ?: state.consentStatus,
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

    fun refreshConsentStatus() {
        if (!sessionManager.hasActiveSession()) {
            _uiState.update { state ->
                state.copy(
                    isLoadingConsentStatus = false,
                    consentStatus = null
                )
            }
            return
        }

        _uiState.update { state -> state.copy(isLoadingConsentStatus = true) }

        viewModelScope.launch {
            runCatching { authRepository.fetchConsentStatus() }
                .onSuccess { consentStatus ->
                    _uiState.update { state ->
                        state.copy(
                            isLoadingConsentStatus = false,
                            consentStatus = consentStatus
                        )
                    }
                }
                .onFailure {
                    _uiState.update { state -> state.copy(isLoadingConsentStatus = false) }
                }
        }
    }

    fun deleteAccount(password: String, reason: String) {
        if (_uiState.value.isDeletingAccount) {
            return
        }

        _uiState.update { state ->
            state.copy(
                isDeletingAccount = true,
                accountDeletionMessage = null,
                accountDeletionMessageIsError = false
            )
        }

        viewModelScope.launch {
            runCatching { authRepository.deleteAccount(password, reason) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isDeletingAccount = false,
                            accountDeletionMessage = "Conta removida com sucesso.",
                            accountDeletionMessageIsError = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isDeletingAccount = false,
                            accountDeletionMessage = resolveAccountDeletionError(error),
                            accountDeletionMessageIsError = true
                        )
                    }
                }
        }
    }

    fun clearAccountDeletionMessage() {
        _uiState.update { state ->
            state.copy(
                accountDeletionMessage = null,
                accountDeletionMessageIsError = false
            )
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionManager.sessionState.collectLatest { session ->
                _uiState.update { state ->
                    state.copy(
                        session = session,
                        profile = repository.buildProfileDraft(),
                        consentStatus = if (session.isAuthenticated) state.consentStatus else null,
                        isSavingProfile = false,
                        profileFieldErrors = emptyMap()
                    )
                }
                if (session.isAuthenticated) {
                    refreshConsentStatus()
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
                .onSuccess {
                    refreshConsentStatus()
                }
                .onFailure {
                    _uiState.update { state -> state.copy(profile = repository.buildProfileDraft()) }
                }
        }
    }

    private fun updateProfileField(
        fieldKey: String,
        transform: (com.ecobook.model.UserProfileDraft) -> com.ecobook.model.UserProfileDraft
    ) {
        _uiState.update { state ->
            state.copy(
                profile = transform(state.profile),
                profileFieldErrors = state.profileFieldErrors - fieldKey,
                profileMessage = null,
                profileMessageIsError = false
            )
        }
    }

    private fun validateProfile(profile: com.ecobook.model.UserProfileDraft): Map<String, String> {
        val errors = linkedMapOf<String, String>()

        if (profile.nome.isBlank()) {
            errors["nome"] = "Informe seu nome."
        }
        if (profile.email.isBlank()) {
            errors["email"] = "Informe seu email."
        } else if (!ProfileInputRules.isValidEmail(profile.email)) {
            errors["email"] = "Informe um email valido."
        }
        if (profile.whatsapp.isBlank()) {
            errors["whatsapp"] = "Informe um WhatsApp."
        } else if (!WhatsAppFormatter.isValidInput(profile.whatsapp)) {
            errors["whatsapp"] = "Digite DDD + numero, por exemplo 48 99999-9999."
        }
        if (profile.cidade.isBlank()) {
            errors["cidade"] = "Informe sua cidade."
        }
        if (profile.bairro.isBlank()) {
            errors["bairro"] = "Informe seu bairro."
        }

        return errors
    }

    private fun Throwable.toFieldErrors(): Map<String, String> {
        return when (this) {
            is ApiException -> fieldErrors
            else -> emptyMap()
        }
    }

    private fun resolveProfileSaveError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                400, 409, 422 -> error.message
                401 -> "Sua sessao expirou. Entre novamente para continuar."
                403 -> "Conclua o onboarding antes de atualizar o perfil."
                else -> "Nao foi possivel salvar o perfil agora."
            }

            is SocketTimeoutException -> "A atualizacao demorou demais. Tente novamente."
            is ConnectException,
            is UnknownHostException,
            is IOException -> "Nao foi possivel falar com o backend para salvar o perfil."
            else -> error.message ?: "Falha inesperada ao salvar o perfil."
        }
    }

    private fun resolveProfileError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                400, 422 -> error.message
                401 -> "Sua sessao expirou. Entre novamente para continuar."
                403 -> "Conclua o onboarding antes de alterar o consentimento."
                else -> "Nao foi possivel atualizar o consentimento agora."
            }

            is SocketTimeoutException -> "A atualizacao demorou demais. Tente novamente."
            is ConnectException,
            is UnknownHostException,
            is IOException -> "Nao foi possivel falar com o backend para atualizar o consentimento."
            else -> error.message ?: "Falha inesperada ao atualizar o consentimento."
        }
    }

    private fun resolveAccountDeletionError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                400 -> error.fieldErrors["password"] ?: error.message
                401 -> "Sua sessao expirou. Entre novamente para continuar."
                else -> error.message
            }

            is SocketTimeoutException -> "A exclusao da conta demorou demais. Tente novamente."
            is ConnectException,
            is UnknownHostException,
            is IOException -> "Nao foi possivel falar com o backend para excluir a conta."
            else -> error.message ?: "Falha inesperada ao excluir a conta."
        }
    }
}
