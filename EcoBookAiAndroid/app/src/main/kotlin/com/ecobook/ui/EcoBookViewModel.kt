package com.ecobook.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecobook.auth.SessionManager
import com.ecobook.data.ApiException
import com.ecobook.data.AuthRepository
import com.ecobook.data.EcoBookRepository
import com.ecobook.data.ReferenceDataRepository
import com.ecobook.dto.UpdateProfileRequestDTO
import com.ecobook.material.ImageCompressionHelper
import com.ecobook.model.BackendStatus
import com.ecobook.model.NecessidadeAcademica
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
    private val referenceDataRepository: ReferenceDataRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    companion object {
        const val PROFILE_PHOTO_FIELD_KEY = "foto_perfil"
    }

    private val _uiState = MutableStateFlow(repository.initialState())
    val uiState: StateFlow<EcoBookUiState> = _uiState.asStateFlow()

    init {
        observeSession()
        loadReferenceData()
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

    fun updateCpf(value: String) = updateProfileField("cpf") { profile ->
        profile.copy(cpf = CpfFormatter.formatForInput(value))
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

    fun toggleNecessidadeAcademica(necessidadeAcademica: NecessidadeAcademica) =
        updateProfileField("necessidades_academicas") { profile ->
            val updatedNeeds = if (profile.necessidadesAcademicas.contains(necessidadeAcademica)) {
                profile.necessidadesAcademicas - necessidadeAcademica
            } else {
                profile.necessidadesAcademicas + necessidadeAcademica
            }
            profile.copy(necessidadesAcademicas = updatedNeeds)
        }

    fun updateDarkThemeOverride(enabled: Boolean) {
        repository.saveDarkThemeOverride(enabled)
        _uiState.update { state ->
            state.copy(darkThemeOverride = repository.getDarkThemeOverride() ?: enabled)
        }
    }

    fun followSystemTheme() {
        repository.saveDarkThemeOverride(null)
        _uiState.update { state ->
            state.copy(darkThemeOverride = repository.getDarkThemeOverride())
        }
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
                cpf = CpfFormatter.toBackendValue(profile.cpf),
                cidade = profile.cidade.trim(),
                bairro = profile.bairro.trim(),
                instituicao = profile.instituicao.trim().ifBlank { null },
                consentimentoIa = _uiState.value.pendingAiConsent ?: profile.consentimentoIa,
                necessidadesAcademicas = profile.necessidadesAcademicas.map { it.name }.toSet()
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

    fun uploadProfilePhoto(context: Context, uri: Uri) {
        if (_uiState.value.isUploadingProfilePhoto) {
            return
        }

        _uiState.update { state ->
            state.copy(
                isUploadingProfilePhoto = true,
                profileFieldErrors = state.profileFieldErrors - PROFILE_PHOTO_FIELD_KEY,
                profileMessage = null,
                profileMessageIsError = false
            )
        }

        viewModelScope.launch {
            runCatching {
                val preparedImage = ImageCompressionHelper.prepareForUpload(context, uri, "profile-photo")
                authRepository.uploadProfilePhoto(preparedImage)
            }.onSuccess {
                val savedProfile = repository.buildProfileDraft()
                _uiState.update { state ->
                    state.copy(
                        isUploadingProfilePhoto = false,
                        profile = savedProfile,
                        savedProfile = savedProfile,
                        profileFieldErrors = state.profileFieldErrors - PROFILE_PHOTO_FIELD_KEY,
                        profileMessage = "Foto de perfil atualizada com sucesso.",
                        profileMessageIsError = false
                    )
                }
            }.onFailure { error ->
                val photoError = resolveProfilePhotoError(error)
                _uiState.update { state ->
                    state.copy(
                        isUploadingProfilePhoto = false,
                        profileFieldErrors = state.profileFieldErrors + (PROFILE_PHOTO_FIELD_KEY to photoError),
                        profileMessage = null,
                        profileMessageIsError = false
                    )
                }
            }
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
                    val savedProfile = repository.buildProfileDraft()
                    _uiState.update { state ->
                        state.copy(
                            profile = savedProfile,
                            savedProfile = savedProfile,
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
                val savedProfile = repository.buildProfileDraft()
                _uiState.update { state ->
                    state.copy(
                        session = session,
                        profile = savedProfile,
                        savedProfile = savedProfile,
                        consentStatus = if (session.isAuthenticated) state.consentStatus else null,
                        isSavingProfile = false,
                        isUploadingProfilePhoto = false,
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
                    val savedProfile = repository.buildProfileDraft()
                    _uiState.update { state ->
                        state.copy(
                            profile = savedProfile,
                            savedProfile = savedProfile
                        )
                    }
            }
        }
    }

    private fun loadReferenceData() {
        viewModelScope.launch {
            val catalog = runCatching { referenceDataRepository.getCatalog() }
                .getOrElse { referenceDataRepository.defaultCatalog() }

            _uiState.update { state ->
                state.copy(
                    profile = state.profile.copy(
                        necessidadesAcademicas = state.profile.necessidadesAcademicas.filterTo(linkedSetOf()) {
                            it in catalog.necessidadesAcademicas
                        }
                    ),
                    savedProfile = state.savedProfile.copy(
                        necessidadesAcademicas = state.savedProfile.necessidadesAcademicas.filterTo(linkedSetOf()) {
                            it in catalog.necessidadesAcademicas
                        }
                    ),
                    necessidadesAcademicasDisponiveis = catalog.necessidadesAcademicas
                )
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
            errors["email"] = "Informe um email válido."
        }
        if (profile.whatsapp.isBlank()) {
            errors["whatsapp"] = "Informe um WhatsApp."
        } else if (!WhatsAppFormatter.isValidInput(profile.whatsapp)) {
            errors["whatsapp"] = "Digite os 11 dígitos do WhatsApp com DDD."
        }
        if (profile.cpf.isBlank()) {
            errors["cpf"] = "Informe o CPF do adulto responsável."
        } else if (CpfFormatter.toBackendValue(profile.cpf).length < 11) {
            errors["cpf"] = "Digite os 11 dígitos do CPF do adulto responsável."
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
                401 -> "Sua sessão expirou. Entre novamente para continuar."
                403 -> "Conclua seu cadastro antes de atualizar o perfil."
                else -> "Não foi possível salvar o perfil agora."
            }

            is SocketTimeoutException -> "A atualização demorou demais. Tente novamente."
            is ConnectException,
            is UnknownHostException,
            is IOException -> "Não foi possível falar com o backend para salvar o perfil."
            else -> error.message ?: "Falha inesperada ao salvar o perfil."
        }
    }

    private fun resolveProfileError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                400, 422 -> error.message
                401 -> "Sua sessão expirou. Entre novamente para continuar."
                403 -> "Conclua seu cadastro antes de alterar o consentimento."
                else -> "Não foi possível atualizar o consentimento agora."
            }

            is SocketTimeoutException -> "A atualização demorou demais. Tente novamente."
            is ConnectException,
            is UnknownHostException,
            is IOException -> "Não foi possível falar com o backend para atualizar o consentimento."
            else -> error.message ?: "Falha inesperada ao atualizar o consentimento."
        }
    }

    private fun resolveProfilePhotoError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                400, 422 -> error.fieldErrors["image"]
                    ?: "Não foi possível validar a foto. Escolha outra imagem em JPG ou PNG."
                413 -> "A foto excede 5MB. Escolha uma imagem menor ou recorte a foto antes de tentar novamente."
                401 -> "Sua sessão expirou. Entre novamente para continuar."
                else -> "Não foi possível atualizar a foto de perfil agora. Tente novamente em alguns instantes."
            }

            is IllegalArgumentException -> error.message
                ?: "Não foi possível usar esta imagem. Escolha outra foto em JPG ou PNG."
            is SocketTimeoutException -> "O envio da foto demorou demais. Tente novamente."
            is ConnectException,
            is UnknownHostException,
            is IOException -> "Não foi possível enviar a foto porque o app não conseguiu falar com o backend."
            else -> error.message ?: "Falha inesperada ao atualizar a foto. Escolha outra imagem e tente novamente."
        }
    }

    private fun resolveAccountDeletionError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                400 -> error.fieldErrors["password"] ?: error.message
                401 -> "Sua sessão expirou. Entre novamente para continuar."
                else -> error.message
            }

            is SocketTimeoutException -> "A exclusão da conta demorou demais. Tente novamente."
            is ConnectException,
            is UnknownHostException,
            is IOException -> "Não foi possível falar com o backend para excluir a conta."
            else -> error.message ?: "Falha inesperada ao excluir a conta."
        }
    }

}
