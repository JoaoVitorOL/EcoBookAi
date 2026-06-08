package com.ecobook.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecobook.auth.SessionManager
import com.ecobook.data.ApiException
import com.ecobook.data.AuthRepository
import com.ecobook.data.ReferenceDataRepository
import com.ecobook.dto.UpdateProfileRequestDTO
import com.ecobook.model.NecessidadeAcademica
import com.ecobook.model.ReferenceDataCatalog
import com.ecobook.ui.CpfFormatter
import com.ecobook.ui.WhatsAppFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val showWelcomeStep: Boolean = false,
    val currentStep: Int = 0,
    val nome: String = "",
    val email: String = "",
    val whatsapp: String = "",
    val cpf: String = "",
    val cidade: String = "",
    val bairro: String = "",
    val instituicao: String = "",
    val hasViewedPlatformTerms: Boolean = false,
    val platformConsentAccepted: Boolean = false,
    val consentimentoIa: Boolean = false,
    val necessidadesAcademicas: Set<NecessidadeAcademica> = emptySet(),
    val necessidadesDisponiveis: List<NecessidadeAcademica> = ReferenceDataCatalog.defaults().necessidadesAcademicas,
    val isSubmitting: Boolean = false,
    val fieldErrors: Map<String, String> = emptyMap(),
    val message: String? = null
) {
    val isLastStep: Boolean
        get() = currentStep == 2
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val referenceDataRepository: ReferenceDataRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            showWelcomeStep = sessionManager.sessionState.value.showNewUserWelcome,
            nome = sessionManager.sessionState.value.nome,
            email = sessionManager.sessionState.value.email
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadReferenceData()
    }

    fun updateNome(value: String) {
        _uiState.update { it.copy(nome = value, fieldErrors = it.fieldErrors - "nome", message = null) }
    }

    fun updateWhatsapp(value: String) {
        _uiState.update {
            it.copy(
                whatsapp = WhatsAppFormatter.formatForInput(value),
                fieldErrors = it.fieldErrors - "whatsapp",
                message = null
            )
        }
    }

    fun updateCpf(value: String) {
        _uiState.update {
            it.copy(
                cpf = CpfFormatter.formatForInput(value),
                fieldErrors = it.fieldErrors - "cpf",
                message = null
            )
        }
    }

    fun onWhatsappFocusLost() {
        if (_uiState.value.whatsapp.isNotBlank() && !WhatsAppFormatter.isValidInput(_uiState.value.whatsapp)) {
            _uiState.update {
                it.copy(fieldErrors = it.fieldErrors + ("whatsapp" to "Digite os 11 dígitos do WhatsApp com DDD."))
            }
        }
    }

    fun onCpfFocusLost() {
        val cpf = _uiState.value.cpf
        if (cpf.isBlank()) {
            return
        }

        _uiState.update {
            val error = when {
                CpfFormatter.toBackendValue(cpf).length < 11 ->
                    "Digite os 11 dígitos do CPF do adulto responsável."
                else -> null
            }
            if (error == null) {
                it.copy(fieldErrors = it.fieldErrors - "cpf")
            } else {
                it.copy(fieldErrors = it.fieldErrors + ("cpf" to error))
            }
        }
    }

    fun updateCidade(value: String) {
        _uiState.update { it.copy(cidade = value, fieldErrors = it.fieldErrors - "cidade", message = null) }
    }

    fun updateBairro(value: String) {
        _uiState.update { it.copy(bairro = value, fieldErrors = it.fieldErrors - "bairro", message = null) }
    }

    fun updateInstituicao(value: String) {
        _uiState.update { it.copy(instituicao = value, message = null) }
    }

    fun togglePlatformConsentAccepted() {
        if (!_uiState.value.hasViewedPlatformTerms) {
            _uiState.update {
                it.copy(
                    fieldErrors = it.fieldErrors + ("platform_consent" to "Leia os termos e a privacidade antes de aceitar."),
                    message = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                platformConsentAccepted = !it.platformConsentAccepted,
                fieldErrors = it.fieldErrors - "platform_consent",
                message = null
            )
        }
    }

    fun toggleConsentimentoIa() {
        _uiState.update { it.copy(consentimentoIa = !it.consentimentoIa, message = null) }
    }

    fun markPlatformTermsViewed() {
        _uiState.update {
            it.copy(
                hasViewedPlatformTerms = true,
                fieldErrors = it.fieldErrors - "platform_consent",
                message = null
            )
        }
    }

    fun toggleNecessidade(necessidade: NecessidadeAcademica) {
        _uiState.update { state ->
            val updated = if (state.necessidadesAcademicas.contains(necessidade)) {
                state.necessidadesAcademicas - necessidade
            } else {
                state.necessidadesAcademicas + necessidade
            }
            state.copy(necessidadesAcademicas = updated, message = null)
        }
    }

    fun dismissWelcomeStep() {
        if (!_uiState.value.showWelcomeStep) {
            return
        }

        sessionManager.dismissNewUserWelcome()
        _uiState.update { state ->
            state.copy(
                showWelcomeStep = false,
                message = null,
                fieldErrors = emptyMap()
            )
        }
    }

    fun nextStep() {
        if (_uiState.value.showWelcomeStep) {
            dismissWelcomeStep()
            return
        }

        val validationErrors = validateCurrentStep(_uiState.value.currentStep)
        if (validationErrors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    fieldErrors = it.fieldErrors + validationErrors,
                    message = null
                )
            }
            return
        }

        _uiState.update { state ->
            state.copy(
                currentStep = (state.currentStep + 1).coerceAtMost(2),
                message = null
            )
        }
    }

    fun previousStep() {
        _uiState.update { state -> state.copy(currentStep = (state.currentStep - 1).coerceAtLeast(0), message = null) }
    }

    fun submit() {
        val validationErrors = validateAll()
        if (validationErrors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    currentStep = stepForErrors(validationErrors, it.currentStep),
                    fieldErrors = validationErrors,
                    message = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, message = null) }

            val state = _uiState.value
            val request = UpdateProfileRequestDTO(
                email = null,
                nome = state.nome.trim(),
                whatsapp = WhatsAppFormatter.toBackendValue(state.whatsapp),
                cpf = CpfFormatter.toBackendValue(state.cpf),
                cidade = state.cidade.trim(),
                bairro = state.bairro.trim(),
                instituicao = state.instituicao.trim().ifBlank { null },
                consentimentoIa = state.consentimentoIa,
                necessidadesAcademicas = state.necessidadesAcademicas.map { it.name }.toSet()
            )

            runCatching { authRepository.updateProfile(request) }
                .onSuccess {
                    _uiState.update { current ->
                        current.copy(
                            isSubmitting = false,
                            fieldErrors = emptyMap(),
                            message = "Perfil concluído. Abrindo o app principal."
                        )
                    }
                }
                .onFailure { error ->
                    val fieldErrors = error.toFieldErrors()
                    _uiState.update { current ->
                        current.copy(
                            isSubmitting = false,
                            currentStep = stepForErrors(fieldErrors, current.currentStep),
                            fieldErrors = fieldErrors,
                            message = if (fieldErrors.isEmpty()) error.toUserMessage() else null
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
                    necessidadesDisponiveis = catalog.necessidadesAcademicas,
                    necessidadesAcademicas = state.necessidadesAcademicas.filterTo(linkedSetOf()) {
                        it in catalog.necessidadesAcademicas
                    }
                )
            }
        }
    }

    private fun validateCurrentStep(step: Int): Map<String, String> {
        return when (step) {
            0 -> buildMap {
                if (_uiState.value.nome.isBlank()) {
                    put("nome", "Informe seu nome para continuar.")
                }
                if (_uiState.value.whatsapp.isBlank()) {
                    put("whatsapp", "Informe um WhatsApp para contato.")
                } else if (!WhatsAppFormatter.isValidInput(_uiState.value.whatsapp)) {
                    put("whatsapp", "Digite os 11 dígitos do WhatsApp com DDD.")
                }
                if (_uiState.value.cpf.isBlank()) {
                    put("cpf", "Informe o CPF do adulto responsável.")
                } else if (CpfFormatter.toBackendValue(_uiState.value.cpf).length < 11) {
                    put("cpf", "Digite os 11 dígitos do CPF do adulto responsável.")
                }
            }

            1 -> buildMap {
                if (_uiState.value.cidade.isBlank()) {
                    put("cidade", "Digite sua cidade.")
                }
                if (_uiState.value.bairro.isBlank()) {
                    put("bairro", "Informe o bairro onde voce pode receber ou doar materiais.")
                }
            }

            else -> emptyMap()
        }
    }

    private fun validateAll(): Map<String, String> {
        return validateCurrentStep(0) + validateCurrentStep(1) + buildMap {
            if (!_uiState.value.platformConsentAccepted) {
                put("platform_consent", "Aceite os termos da plataforma para concluir o perfil.")
            }
        }
    }

    private fun Throwable.toFieldErrors(): Map<String, String> {
        return when (this) {
            is ApiException -> fieldErrors
            else -> emptyMap()
        }
    }

    private fun Throwable.toUserMessage(): String {
        return when (this) {
            is ApiException -> when (statusCode) {
                400, 422 -> message
                403 -> "Seu perfil já está completo. Vamos seguir para o app principal."
                else -> "Não foi possível salvar seu cadastro agora."
            }

            else -> message ?: "Falha inesperada ao concluir seu cadastro."
        }
    }

    private fun stepForErrors(
        fieldErrors: Map<String, String>,
        fallbackStep: Int
    ): Int {
        return when {
            fieldErrors.keys.any { it in setOf("nome", "whatsapp", "cpf") } -> 0
            fieldErrors.keys.any { it in setOf("cidade", "bairro", "instituicao") } -> 1
            fieldErrors.keys.any { it in setOf("platform_consent") } -> 2
            else -> fallbackStep
        }
    }
}
