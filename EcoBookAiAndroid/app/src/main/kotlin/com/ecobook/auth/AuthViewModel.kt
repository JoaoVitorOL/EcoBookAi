package com.ecobook.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecobook.data.ApiException
import com.ecobook.data.AuthRepository
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

enum class AuthMode {
    LOGIN,
    REGISTER
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val mode: AuthMode = AuthMode.LOGIN,
    val nome: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val errorMessage: String? = null,
    val fieldErrors: Map<String, String> = emptyMap()
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun setMode(mode: AuthMode) {
        _uiState.update {
            it.copy(
                mode = mode,
                confirmPassword = if (mode == AuthMode.LOGIN) "" else it.confirmPassword,
                errorMessage = null,
                fieldErrors = emptyMap()
            )
        }
    }

    fun updateNome(value: String) = updateField("nome", value) { state, normalized ->
        state.copy(nome = normalized)
    }

    fun updateEmail(value: String) = updateField("email", value) { state, normalized ->
        state.copy(email = normalized)
    }

    fun updatePassword(value: String) = updateField("password", value) { state, normalized ->
        state.copy(password = normalized)
    }

    fun updateConfirmPassword(value: String) = updateField("confirm_password", value) { state, normalized ->
        state.copy(confirmPassword = normalized)
    }

    fun submit() {
        val currentState = _uiState.value
        val validationErrors = validate(currentState)
        if (validationErrors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    errorMessage = null,
                    fieldErrors = validationErrors
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    fieldErrors = emptyMap()
                )
            }

            runCatching {
                when (currentState.mode) {
                    AuthMode.LOGIN -> authRepository.login(
                        email = currentState.email.trim(),
                        password = currentState.password
                    )

                    AuthMode.REGISTER -> authRepository.register(
                        email = currentState.email.trim(),
                        password = currentState.password,
                        nome = currentState.nome.trim()
                    )
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        password = "",
                        confirmPassword = "",
                        errorMessage = null,
                        fieldErrors = emptyMap()
                    )
                }
            }.onFailure { error ->
                val resolvedError = error.toUiError()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = resolvedError.message,
                        fieldErrors = resolvedError.fieldErrors
                    )
                }
            }
        }
    }

    private fun updateField(
        fieldKey: String,
        value: String,
        reducer: (AuthUiState, String) -> AuthUiState
    ) {
        _uiState.update { state ->
            reducer(state, value).copy(
                errorMessage = null,
                fieldErrors = state.fieldErrors - fieldKey
            )
        }
    }

    private fun validate(state: AuthUiState): Map<String, String> {
        val errors = linkedMapOf<String, String>()

        if (state.mode == AuthMode.REGISTER && state.nome.isBlank()) {
            errors["nome"] = "Informe seu nome para criar a conta."
        }

        if (state.email.isBlank()) {
            errors["email"] = "Informe seu email."
        }

        if (state.password.isBlank()) {
            errors["password"] = "Informe sua senha."
        } else if (state.password.length < 8) {
            errors["password"] = "A senha precisa ter pelo menos 8 caracteres."
        }

        if (state.mode == AuthMode.REGISTER) {
            if (state.confirmPassword.isBlank()) {
                errors["confirm_password"] = "Confirme sua senha."
            } else if (state.password != state.confirmPassword) {
                errors["confirm_password"] = "As senhas precisam ser iguais."
            }
        }

        return errors
    }

    private fun Throwable.toUiError(): ResolvedAuthError {
        return when (this) {
            is ApiException -> when (statusCode) {
                400 -> ResolvedAuthError(
                    message = message,
                    fieldErrors = fieldErrors
                )

                401 -> ResolvedAuthError(
                    message = "Email ou senha inválidos.",
                    fieldErrors = emptyMap()
                )

                409 -> ResolvedAuthError(
                    message = "Este email já está cadastrado. Entre com a conta existente ou use outro email.",
                    fieldErrors = fieldErrors.ifEmpty {
                        mapOf("email" to "Este email já está cadastrado.")
                    }
                )

                500 -> ResolvedAuthError(
                    message = "O backend respondeu com erro interno. Verifique se a API local está ativa."
                )

                else -> ResolvedAuthError(message = message)
            }

            is SocketTimeoutException,
            is ConnectException,
            is UnknownHostException,
            is IOException -> ResolvedAuthError(
                message = "Não foi possível conectar ao backend configurado no app. Verifique se a API local está ativa e se a URL/porta estão corretas antes de tentar novamente."
            )

            else -> ResolvedAuthError(message = message ?: "Falha inesperada durante a autenticação.")
        }
    }
}

private data class ResolvedAuthError(
    val message: String,
    val fieldErrors: Map<String, String> = emptyMap()
)
