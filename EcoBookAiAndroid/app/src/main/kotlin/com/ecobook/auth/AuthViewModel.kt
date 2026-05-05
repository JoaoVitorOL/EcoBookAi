package com.ecobook.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecobook.data.ApiException
import com.ecobook.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val manualGoogleToken: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updateManualGoogleToken(value: String) {
        _uiState.update {
            it.copy(
                manualGoogleToken = value,
                errorMessage = null
            )
        }
    }

    fun signInWithGoogleIdToken(idToken: String) {
        if (idToken.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Informe um Google ID token valido para continuar.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            runCatching {
                authRepository.registerWithGoogleToken(idToken.trim())
                runCatching { authRepository.refreshCurrentUser() }
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, manualGoogleToken = "") }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.toUserMessage()
                    )
                }
            }
        }
    }

    fun onAuthFailure(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    private fun Throwable.toUserMessage(): String {
        return when (this) {
            is ApiException -> when (statusCode) {
                401 -> "Nao foi possivel validar sua identidade no Google. Tente novamente."
                500 -> "O backend respondeu com erro interno. Verifique se a API local esta ativa."
                else -> message
            }

            else -> message ?: "Falha inesperada durante o login."
        }
    }
}
