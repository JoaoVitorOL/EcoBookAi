package com.ecobook.data

import com.ecobook.api.EcoBookApiClient
import com.ecobook.model.AIPreviewField
import com.ecobook.model.AiAssistStatus
import com.ecobook.model.BackendConnectionState
import com.ecobook.model.BackendStatus
import com.ecobook.model.Disciplina
import com.ecobook.model.DonationPreview
import com.ecobook.model.DonationStep
import com.ecobook.model.EstadoConservacao
import com.ecobook.model.MaterialHighlight
import com.ecobook.model.NivelEnsino
import com.ecobook.model.ProjectInsight
import com.ecobook.model.SessionUiState
import com.ecobook.model.SistemaEnsino
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
            catalog = sampleCatalog(),
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
                onSuccess = { response ->
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

    private fun sampleCatalog(): List<MaterialHighlight> {
        return listOf(
            MaterialHighlight(
                id = "material-1",
                title = "Colecao Anglo Matematica 7",
                summary = "Livro em bom estado, ideal para reforco e atividades guiadas.",
                discipline = Disciplina.MATEMATICA,
                level = NivelEnsino.FUNDAMENTAL,
                teachingSystem = SistemaEnsino.ANGLO,
                conservationState = EstadoConservacao.BOM,
                schoolYear = "7o ano",
                neighborhood = "Centro",
                city = "Florianopolis",
                publicationYear = 2021,
                matchNote = "Mesmo bairro e sistema de ensino compativel."
            ),
            MaterialHighlight(
                id = "material-2",
                title = "Kit Positivo Ciencias 8",
                summary = "Conjunto com apostilas e caderno de experimentos.",
                discipline = Disciplina.CIENCIAS,
                level = NivelEnsino.FUNDAMENTAL,
                teachingSystem = SistemaEnsino.POSITIVO,
                conservationState = EstadoConservacao.USADO,
                schoolYear = "8o ano",
                neighborhood = "Trindade",
                city = "Florianopolis",
                publicationYear = 2020,
                matchNote = "Mesmo municipio, publicacao recente."
            ),
            MaterialHighlight(
                id = "material-3",
                title = "Literatura para vestibular",
                summary = "Selecao com obras de leitura obrigatoria e marcacoes.",
                discipline = Disciplina.LITERATURA,
                level = NivelEnsino.MEDIO,
                teachingSystem = SistemaEnsino.OUTRO,
                conservationState = EstadoConservacao.BOM,
                schoolYear = "Ensino medio",
                neighborhood = "Campeche",
                city = "Florianopolis",
                publicationYear = 2019,
                matchNote = "Boa aderencia para trilha de leitura e vestibulares."
            ),
            MaterialHighlight(
                id = "material-4",
                title = "Geografia regional do Sul",
                summary = "Material complementar com mapas e exercicios de revisao.",
                discipline = Disciplina.GEOGRAFIA,
                level = NivelEnsino.MEDIO,
                teachingSystem = SistemaEnsino.COC,
                conservationState = EstadoConservacao.NOVO,
                schoolYear = "2a serie",
                neighborhood = "Kobrasol",
                city = "Sao Jose",
                publicationYear = 2022,
                matchNote = "Fora do bairro, mas com excelente estado de conservacao."
            ),
            MaterialHighlight(
                id = "material-5",
                title = "Historia do Brasil contemporaneo",
                summary = "Volume unico para graduacao com anexos e resumos.",
                discipline = Disciplina.HISTORIA,
                level = NivelEnsino.SUPERIOR,
                teachingSystem = SistemaEnsino.OUTRO,
                conservationState = EstadoConservacao.USADO,
                schoolYear = "Superior",
                neighborhood = "Agronomica",
                city = "Florianopolis",
                publicationYear = 2018,
                matchNote = "No nivel superior, o matching ignora o ano escolar."
            )
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
                description = "Hoje a API funcional confirmada e o health check; auth, perfil, materiais e solicitacoes ja estao modelados e especificados para a proxima fase."
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
            description = "Simulacao do retorno esperado para /materiais/preview quando a etapa de IA estiver conectada ao backend.",
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
