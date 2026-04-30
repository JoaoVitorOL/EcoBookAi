package com.ecobook.model

enum class Disciplina(val label: String) {
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

data class UserProfileDraft(
    val nome: String = "Ana Martins",
    val email: String = "ana.martins@ecobook.ai",
    val whatsapp: String = "+55 48 99999-1111",
    val cidade: String = "Florianopolis",
    val bairro: String = "Centro",
    val instituicao: String = "Escola Horizonte Sul",
    val consentimentoIa: Boolean = true,
    val roleLabel: String = "Doadora e estudante",
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
