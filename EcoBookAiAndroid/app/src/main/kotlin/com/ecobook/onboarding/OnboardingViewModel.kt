package com.ecobook.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecobook.auth.SessionManager
import com.ecobook.data.ApiException
import com.ecobook.data.AuthRepository
import com.ecobook.dto.UpdateProfileRequestDTO
import com.ecobook.model.NecessidadeAcademica
import com.ecobook.ui.WhatsAppFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val currentStep: Int = 0,
    val nome: String = "",
    val email: String = "",
    val whatsapp: String = "",
    val cidade: String = "",
    val bairro: String = "",
    val instituicao: String = "",
    val consentimentoIa: Boolean = false,
    val necessidadesAcademicas: Set<NecessidadeAcademica> = emptySet(),
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
    sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            nome = sessionManager.sessionState.value.nome,
            email = sessionManager.sessionState.value.email
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

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

    fun onWhatsappFocusLost() {
        if (_uiState.value.whatsapp.isNotBlank() && !WhatsAppFormatter.isValidInput(_uiState.value.whatsapp)) {
            _uiState.update {
                it.copy(fieldErrors = it.fieldErrors + ("whatsapp" to "Digite DDD + número, por exemplo 48 99999-9999."))
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

    fun toggleConsentimentoIa() {
        _uiState.update { it.copy(consentimentoIa = !it.consentimentoIa, message = null) }
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

    fun nextStep() {
        val validationErrors = validateCurrentStep(_uiState.value.currentStep)
        if (validationErrors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = it.fieldErrors + validationErrors) }
            return
        }

        _uiState.update { state -> state.copy(currentStep = (state.currentStep + 1).coerceAtMost(2)) }
    }

    fun previousStep() {
        _uiState.update { state -> state.copy(currentStep = (state.currentStep - 1).coerceAtLeast(0), message = null) }
    }

    fun submit() {
        val validationErrors = validateAll()
        if (validationErrors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = validationErrors, message = "Revise os campos destacados antes de enviar.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, message = null) }

            val state = _uiState.value
            val request = UpdateProfileRequestDTO(
                nome = state.nome.trim(),
                whatsapp = WhatsAppFormatter.toBackendValue(state.whatsapp),
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
                    _uiState.update { current ->
                        current.copy(
                            isSubmitting = false,
                            fieldErrors = error.toFieldErrors(),
                            message = error.toUserMessage()
                        )
                    }
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
                    put("whatsapp", "Digite DDD + número, por exemplo 48 99999-9999.")
                }
            }

            1 -> buildMap {
                if (_uiState.value.cidade.isBlank()) {
                    put("cidade", "Digite sua cidade.")
                }
                if (_uiState.value.bairro.isBlank()) {
                    put("bairro", "Informe o bairro onde você pode receber ou doar materiais.")
                }
            }

            else -> emptyMap()
        }
    }

    private fun validateAll(): Map<String, String> {
        return validateCurrentStep(0) + validateCurrentStep(1)
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
                else -> "Não foi possível salvar o onboarding agora."
            }

            else -> message ?: "Falha inesperada ao concluir o onboarding."
        }
    }
}
