package com.ecobook.model

enum class Disciplina(val label: String) {
    TODAS("Todas"),
    MATEMATICA("Matematica"),
    PORTUGUES("Portugues"),
    HISTORIA("Historia"),
    GEOGRAFIA("Geografia"),
    CIENCIAS("Ciencias"),
    LITERATURA("Literatura")
}

enum class NivelEnsino(val label: String) {
    FUNDAMENTAL("Fundamental"),
    MEDIO("Medio"),
    SUPERIOR("Superior")
}

enum class SistemaEnsino(val label: String) {
    ANGLO("Anglo"),
    OBJETIVO("Objetivo"),
    COC("COC"),
    POSITIVO("Positivo"),
    OUTRO("Outro")
}

enum class EstadoConservacao(val label: String) {
    NOVO("Novo"),
    BOM("Bom"),
    USADO("Usado"),
    DANIFICADO("Danificado")
}

enum class NecessidadeAcademica(val label: String) {
    TEXTBOOKS("Livros didaticos"),
    WORKBOOKS("Cadernos de atividades"),
    REFERENCE_MATERIALS("Materiais de referencia"),
    FICTION("Literatura"),
    TECHNICAL_BOOKS("Livros tecnicos"),
    TEST_PREP("Preparacao para provas")
}

enum class AiAssistStatus(val label: String) {
    SUCCESS("Alta confianca"),
    LOW_CONFIDENCE("Revisao manual"),
    FAILURE("Sem sugestao")
}

enum class BackendConnectionState {
    CHECKING,
    ONLINE,
    OFFLINE
}

enum class SessionDestination {
    AUTH,
    ONBOARDING,
    MAIN
}

data class BackendStatus(
    val state: BackendConnectionState,
    val headline: String,
    val detail: String,
    val application: String? = null,
    val version: String? = null,
    val timestamp: String? = null
) {
    companion object {
        fun checking() = BackendStatus(
            state = BackendConnectionState.CHECKING,
            headline = "Verificando o backend",
            detail = "Consultando /api/v1/health para confirmar a conexao."
        )
    }
}

data class SessionUiState(
    val isAuthenticated: Boolean = false,
    val userId: String? = null,
    val email: String = "",
    val nome: String = "",
    val role: String = "",
    val profileComplete: Boolean = false,
    val lastErrorMessage: String? = null
) {
    val destination: SessionDestination
        get() = when {
            !isAuthenticated -> SessionDestination.AUTH
            profileComplete -> SessionDestination.MAIN
            else -> SessionDestination.ONBOARDING
        }
}

data class UserProfileDraft(
    val nome: String = "",
    val email: String = "",
    val whatsapp: String = "",
    val cidade: String = "",
    val bairro: String = "",
    val instituicao: String = "",
    val consentimentoIa: Boolean = false,
    val roleLabel: String = "Usuario",
    val hasSavedSession: Boolean = false
) {
    private val requiredFields = listOf(nome, email, whatsapp, cidade, bairro)

    val completionRatio: Float
        get() = requiredFields.count { it.isNotBlank() }.toFloat() / requiredFields.size.toFloat()

    val completionPercent: Int
        get() = (completionRatio * 100).toInt()
}

data class MaterialHighlight(
    val id: String,
    val title: String,
    val summary: String,
    val discipline: Disciplina,
    val level: NivelEnsino,
    val teachingSystem: SistemaEnsino,
    val conservationState: EstadoConservacao,
    val schoolYear: String,
    val neighborhood: String,
    val city: String,
    val publicationYear: Int,
    val matchNote: String
) {
    val locationLabel: String
        get() = "$neighborhood, $city"
}

data class ProjectInsight(
    val title: String,
    val description: String
)

data class DonationStep(
    val title: String,
    val description: String
)

data class AIPreviewField(
    val label: String,
    val value: String
)

data class DonationPreview(
    val aiStatus: AiAssistStatus = AiAssistStatus.SUCCESS,
    val confidence: Double = 0.82,
    val description: String = "Exemplo de retorno para o fluxo de classificacao assistida por IA.",
    val fields: List<AIPreviewField> = emptyList(),
    val steps: List<DonationStep> = emptyList()
)
