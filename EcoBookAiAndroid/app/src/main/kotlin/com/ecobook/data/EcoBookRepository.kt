package com.ecobook.data

import com.ecobook.api.EcoBookApiClient
import com.ecobook.model.AIPreviewField
import com.ecobook.model.AiAssistStatus
import com.ecobook.model.BackendConnectionState
import com.ecobook.model.BackendStatus
import com.ecobook.model.DonationPreview
import com.ecobook.model.DonationStep
import com.ecobook.model.ProjectInsight
import com.ecobook.model.SessionUiState
import com.ecobook.model.UserProfileDraft
import com.ecobook.ui.EcoBookUiState
import com.ecobook.utils.SecureStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EcoBookRepository @Inject constructor(
    private val apiClient: EcoBookApiClient,
    private val secureStorage: SecureStorage
) {

    fun initialState(): EcoBookUiState {
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
            profile = buildProfileDraft(),
            insights = sampleInsights(),
            donationPreview = sampleDonationPreview()
        )
    }

    fun buildProfileDraft(): UserProfileDraft {
        return UserProfileDraft(
            nome = secureStorage.getUserName().orEmpty(),
            email = secureStorage.getUserEmail().orEmpty(),
            whatsapp = secureStorage.getUserWhatsapp().orEmpty(),
            cidade = secureStorage.getUserCidade().orEmpty(),
            bairro = secureStorage.getUserBairro().orEmpty(),
            instituicao = secureStorage.getUserInstituicao().orEmpty(),
            consentimentoIa = secureStorage.getConsentimentoIa(),
            roleLabel = secureStorage.getUserRole()?.replace('_', ' ')?.ifBlank { "Usuario" } ?: "Usuario",
            hasSavedSession = secureStorage.hasToken()
        )
    }

    suspend fun loadBackendStatus(): BackendStatus {
        return runCatching { apiClient.getHealth() }
            .fold(
                onSuccess = { envelope ->
                    val response = envelope.data ?: return@fold BackendStatus(
                        state = BackendConnectionState.OFFLINE,
                        headline = "Backend indisponivel",
                        detail = envelope.message
                    )
                    BackendStatus(
                        state = BackendConnectionState.ONLINE,
                        headline = "Backend online",
                        detail = "${response.application} respondeu ${response.status} e esta pronto para integracao local.",
                        application = response.application,
                        version = response.version,
                        timestamp = response.timestamp
                    )
                },
                onFailure = { error ->
                    BackendStatus(
                        state = BackendConnectionState.OFFLINE,
                        headline = "Backend indisponivel",
                        detail = error.message
                            ?: "Nao foi possivel acessar o endpoint /api/v1/health. Verifique a URL e a porta configuradas no app."
                    )
                }
            )
    }

    private fun sampleInsights(): List<ProjectInsight> {
        return listOf(
            ProjectInsight(
                title = "EcoBookAiBackend",
                description = "Backend Spring Boot com seguranca JWT, entidades principais, DTOs, Flyway e endpoint de saude em /api/v1/health."
            ),
            ProjectInsight(
                title = "EcoBookAiAndroid",
                description = "App Android nativo em Kotlin + Compose + Hilt + Retrofit, agora organizado para abrir direto no Android Studio."
            ),
            ProjectInsight(
                title = "Integracao atual",
                description = "Auth, perfil, sessao local, upload com camera/galeria, preview com IA, publicacao final e busca paginada de materiais ja funcionam ponta a ponta. Solicitacoes e notificacoes de negocio seguem como a proxima frente."
            )
        )
    }

    private fun sampleDonationPreview(): DonationPreview {
        val steps = listOf(
            DonationStep(
                title = "1. Capturar o material",
                description = "Foto do livro, escolha da imagem e preparacao para o endpoint de preview."
            ),
            DonationStep(
                title = "2. Revisar a classificacao IA",
                description = "Confianca alta preenche automatico; baixa confianca pede revisao manual."
            ),
            DonationStep(
                title = "3. Publicar e aguardar matching",
                description = "Depois da confirmacao, o material entra como DISPONIVEL para descoberta."
            )
        )

        return DonationPreview(
            aiStatus = AiAssistStatus.SUCCESS,
            confidence = 0.82,
            description = "Exemplo visual alinhado ao retorno atual de /materiais/preview para revisar os campos antes da publicacao final.",
            fields = listOf(
                AIPreviewField("Titulo sugerido", "Colecao Anglo Matematica 7"),
                AIPreviewField("Disciplina", "Matematica"),
                AIPreviewField("Nivel de ensino", "Fundamental"),
                AIPreviewField("Ano escolar", "7o ano"),
                AIPreviewField("Sistema", "Anglo"),
                AIPreviewField("Estado", "Bom")
            ),
            steps = steps
        )
    }
}
