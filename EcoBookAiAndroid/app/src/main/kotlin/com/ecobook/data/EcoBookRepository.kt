package com.ecobook.data

import com.ecobook.api.EcoBookApiClient
import com.ecobook.model.BackendConnectionState
import com.ecobook.model.BackendStatus
import com.ecobook.model.NecessidadeAcademica
import com.ecobook.model.SessionUiState
import com.ecobook.model.UserProfileDraft
import com.ecobook.ui.CpfFormatter
import com.ecobook.ui.EcoBookUiState
import com.ecobook.ui.WhatsAppFormatter
import com.ecobook.utils.SecureStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EcoBookRepository @Inject constructor(
    private val apiClient: EcoBookApiClient,
    private val secureStorage: SecureStorage
) {

    fun initialState(): EcoBookUiState {
        val profile = buildProfileDraft()
        val session = SessionUiState(
            isAuthenticated = secureStorage.hasToken(),
            userId = secureStorage.getUserId(),
            email = secureStorage.getUserEmail().orEmpty(),
            nome = secureStorage.getUserName().orEmpty(),
            role = secureStorage.getUserRole().orEmpty(),
            profileComplete = secureStorage.getProfileComplete()
        )

        return EcoBookUiState(
            session = session,
            profile = profile,
            savedProfile = profile,
            darkThemeOverride = secureStorage.getDarkThemeOverride()
        )
    }

    fun getDarkThemeOverride(): Boolean? = secureStorage.getDarkThemeOverride()

    fun saveDarkThemeOverride(enabled: Boolean?) {
        secureStorage.saveDarkThemeOverride(enabled)
    }

    fun buildProfileDraft(): UserProfileDraft {
        return UserProfileDraft(
            nome = secureStorage.getUserName().orEmpty(),
            email = secureStorage.getUserEmail().orEmpty(),
            whatsapp = WhatsAppFormatter.formatForInput(secureStorage.getUserWhatsapp().orEmpty()),
            cpf = CpfFormatter.formatForInput(secureStorage.getUserCpf().orEmpty()),
            cidade = secureStorage.getUserCidade().orEmpty(),
            bairro = secureStorage.getUserBairro().orEmpty(),
            instituicao = secureStorage.getUserInstituicao().orEmpty(),
            fotoPerfilUrl = secureStorage.getUserFotoPerfilUrl().orEmpty(),
            consentimentoIa = secureStorage.getConsentimentoIa(),
            necessidadesAcademicas = resolveAcademicNeeds(secureStorage.getUserNecessidadesAcademicas()),
            roleLabel = secureStorage.getUserRole()?.replace('_', ' ')?.ifBlank { "Usuário" } ?: "Usuário",
            hasSavedSession = secureStorage.hasToken()
        )
    }

    suspend fun loadBackendStatus(): BackendStatus {
        return runCatching { apiClient.getHealth() }
            .fold(
                onSuccess = { envelope ->
                    val response = envelope.data ?: return@fold BackendStatus(
                        state = BackendConnectionState.OFFLINE,
                        headline = "Backend indisponível",
                        detail = envelope.message
                    )
                    BackendStatus(
                        state = BackendConnectionState.ONLINE,
                        headline = "Backend online",
                        detail = "${response.application} respondeu ${response.status} e está pronto para integração local.",
                        application = response.application,
                        version = response.version,
                        timestamp = response.timestamp
                    )
                },
                onFailure = { error ->
                    BackendStatus(
                        state = BackendConnectionState.OFFLINE,
                        headline = "Backend indisponível",
                        detail = error.message
                            ?: "Não foi possível acessar o endpoint /api/v1/health. Verifique a URL e a porta configuradas no app."
                    )
                }
            )
    }

    private fun resolveAcademicNeeds(rawNeeds: Set<String>): Set<NecessidadeAcademica> {
        return rawNeeds.mapNotNullTo(linkedSetOf()) { rawNeed ->
            NecessidadeAcademica.entries.firstOrNull { necessidadeAcademica ->
                necessidadeAcademica.name == rawNeed
            }
        }
    }
}
