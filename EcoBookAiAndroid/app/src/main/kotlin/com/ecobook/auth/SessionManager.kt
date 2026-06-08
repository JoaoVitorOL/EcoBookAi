package com.ecobook.auth

import com.ecobook.dto.AuthResponseDTO
import com.ecobook.dto.UsuarioDTO
import com.ecobook.model.SessionUiState
import com.ecobook.utils.SecureStorage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class SessionManager @Inject constructor(
    private val secureStorage: SecureStorage
) {

    private val _sessionState = MutableStateFlow(restoreSession())
    val sessionState: StateFlow<SessionUiState> = _sessionState.asStateFlow()

    fun hasActiveSession(): Boolean = _sessionState.value.isAuthenticated

    fun onRegistrationSuccess(response: AuthResponseDTO) {
        persistAuthenticatedSession(
            response = response,
            showNewUserWelcome = !response.perfilCompleto
        )
    }

    fun onLoginSuccess(response: AuthResponseDTO) {
        persistAuthenticatedSession(
            response = response,
            showNewUserWelcome = false
        )
    }

    fun dismissNewUserWelcome() {
        secureStorage.saveShowNewUserWelcome(false)
        _sessionState.update { current ->
            current.copy(showNewUserWelcome = false)
        }
    }

    private fun persistAuthenticatedSession(
        response: AuthResponseDTO,
        showNewUserWelcome: Boolean
    ) {
        secureStorage.clearProfileData()
        secureStorage.saveToken(response.token)
        secureStorage.saveUserId(response.id)
        secureStorage.saveUserName(response.nome)
        secureStorage.saveUserEmail(response.email)
        secureStorage.saveUserRole(response.role)
        secureStorage.saveProfileComplete(response.perfilCompleto)
        secureStorage.saveShowNewUserWelcome(showNewUserWelcome)
        secureStorage.saveUserWhatsapp(response.whatsapp)
        secureStorage.saveUserCpf(response.cpf)
        secureStorage.saveUserCidade(response.cidade)
        secureStorage.saveUserBairro(response.bairro)
        secureStorage.saveUserInstituicao(response.instituicao)
        secureStorage.saveUserFotoPerfilUrl(response.fotoPerfilUrl)
        secureStorage.saveConsentimentoIa(response.consentimentoIa)
        secureStorage.saveUserNecessidadesAcademicas(response.necessidadesAcademicas)

        _sessionState.value = SessionUiState(
            isAuthenticated = true,
            userId = response.id,
            email = response.email,
            nome = response.nome,
            role = response.role,
            profileComplete = response.perfilCompleto,
            showNewUserWelcome = showNewUserWelcome
        )
    }

    fun onUserLoaded(response: UsuarioDTO) {
        if (response.perfilCompleto) {
            secureStorage.saveShowNewUserWelcome(false)
        }
        secureStorage.saveUserId(response.id)
        secureStorage.saveUserName(response.nome)
        secureStorage.saveUserEmail(response.email)
        secureStorage.saveUserRole(response.role)
        secureStorage.saveProfileComplete(response.perfilCompleto)
        secureStorage.saveUserWhatsapp(response.whatsapp)
        secureStorage.saveUserCpf(response.cpf)
        secureStorage.saveUserCidade(response.cidade)
        secureStorage.saveUserBairro(response.bairro)
        secureStorage.saveUserInstituicao(response.instituicao)
        secureStorage.saveUserFotoPerfilUrl(response.fotoPerfilUrl)
        secureStorage.saveConsentimentoIa(response.consentimentoIa)
        secureStorage.saveUserNecessidadesAcademicas(response.necessidadesAcademicas)

        _sessionState.update {
            it.copy(
                isAuthenticated = secureStorage.hasToken(),
                userId = response.id,
                email = response.email,
                nome = response.nome,
                role = response.role,
                profileComplete = response.perfilCompleto,
                showNewUserWelcome = if (response.perfilCompleto) false else it.showNewUserWelcome,
                lastErrorMessage = null
            )
        }
    }

    fun clearSession(message: String? = null) {
        secureStorage.clear()
        _sessionState.value = SessionUiState(lastErrorMessage = message)
    }

    private fun restoreSession(): SessionUiState {
        val tokenPresent = secureStorage.hasToken()
        return SessionUiState(
            isAuthenticated = tokenPresent,
            userId = secureStorage.getUserId(),
            email = secureStorage.getUserEmail().orEmpty(),
            nome = secureStorage.getUserName().orEmpty(),
            role = secureStorage.getUserRole().orEmpty(),
            profileComplete = secureStorage.getProfileComplete(),
            showNewUserWelcome = secureStorage.getShowNewUserWelcome()
        )
    }
}
