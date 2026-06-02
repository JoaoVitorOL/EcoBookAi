package com.ecobook.data

import com.ecobook.api.EcoBookApiClient
import com.ecobook.model.AIPreviewField
import com.ecobook.model.AiAssistStatus
import com.ecobook.model.BackendConnectionState
import com.ecobook.model.BackendStatus
import com.ecobook.model.DonationPreview
import com.ecobook.model.DonationStep
import com.ecobook.model.NecessidadeAcademica
import com.ecobook.model.ProjectInsight
import com.ecobook.model.SessionUiState
import com.ecobook.model.UserProfileDraft
import com.ecobook.ui.CpfFormatter
import com.ecobook.ui.WhatsAppFormatter
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
            darkThemeOverride = secureStorage.getDarkThemeOverride(),
            insights = sampleInsights(),
            donationPreview = sampleDonationPreview()
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

    private fun sampleInsights(): List<ProjectInsight> {
        return listOf(
            ProjectInsight(
                title = "Backend do app",
                description = "API Spring Boot com segurança JWT, entidades principais, DTOs, Flyway e endpoint de saúde."
            ),
            ProjectInsight(
                title = "App Android",
                description = "Aplicativo nativo em Kotlin + Compose + Hilt + Retrofit, pronto para evoluir no Android Studio."
            ),
            ProjectInsight(
                title = "Integração atual",
                description = "Acesso, perfil, sessão local, envio de imagens com câmera ou galeria, análise com IA, publicação final e busca paginada de materiais já funcionam ponta a ponta. Solicitações e notificações de negócio seguem como a próxima frente."
            )
        )
    }

    private fun sampleDonationPreview(): DonationPreview {
        val steps = listOf(
            DonationStep(
                title = "1. Capturar o material",
                description = "Foto do livro, escolha da imagem e preparação para a análise inicial."
            ),
            DonationStep(
                title = "2. Revisar a classificação IA",
                description = "Confiança alta preenche automático; baixa confiança pede revisão manual."
            ),
            DonationStep(
                title = "3. Publicar e aguardar compatibilidade",
                description = "Depois da confirmação, o material entra como DISPONÍVEL para descoberta."
            )
        )

        return DonationPreview(
            aiStatus = AiAssistStatus.SUCCESS,
            confidence = 0.82,
            description = "Exemplo visual alinhado ao retorno atual da análise para revisar os campos antes da publicação final.",
            fields = listOf(
                AIPreviewField("Título sugerido", "Coleção Anglo Matemática 7"),
                AIPreviewField("Disciplina", "Matemática"),
                AIPreviewField("Nível de ensino", "Fundamental"),
                AIPreviewField("Ano escolar", "7o ano"),
                AIPreviewField("Sistema", "Anglo"),
                AIPreviewField("Estado", "Bom")
            ),
            steps = steps
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
