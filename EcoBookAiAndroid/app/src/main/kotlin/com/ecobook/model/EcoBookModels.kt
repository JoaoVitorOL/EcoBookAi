package com.ecobook.model

enum class Disciplina(val label: String) {
    TODAS("Todas"),
    MATEMATICA("Matemática"),
    PORTUGUES("Português"),
    HISTORIA("História"),
    GEOGRAFIA("Geografia"),
    CIENCIAS("Ciências"),
    LITERATURA("Literatura")
}

enum class NivelEnsino(val label: String) {
    FUNDAMENTAL("Fundamental"),
    MEDIO("Médio"),
    SUPERIOR("Superior")
}

enum class SistemaEnsino(val label: String) {
    ANGLO("Anglo"),
    OBJETIVO("Objetivo"),
    COC("COC"),
    POSITIVO("Positivo"),
    POLIEDRO("Poliedro"),
    ETAPA("Etapa"),
    BERNOULLI("Bernoulli"),
    SAS("SAS"),
    FTD("FTD"),
    OUTRO("Outro")
}

enum class EstadoConservacao(val label: String) {
    NOVO("Novo"),
    BOM("Bom"),
    USADO("Usado"),
    DANIFICADO("Danificado")
}

enum class NecessidadeAcademica(val label: String) {
    TEXTBOOKS("Livros didáticos"),
    WORKBOOKS("Cadernos de atividades"),
    REFERENCE_MATERIALS("Materiais de referência"),
    FICTION("Literatura"),
    TECHNICAL_BOOKS("Livros técnicos"),
    TEST_PREP("Preparação para provas")
}

enum class AiAssistStatus(val label: String) {
    SUCCESS("Alta confiança"),
    LOW_CONFIDENCE("Revisão manual"),
    FAILURE("Sem sugestão")
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
            detail = "Consultando /api/v1/health para confirmar a conexão."
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
    val showNewUserWelcome: Boolean = false,
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
    val cpf: String = "",
    val cidade: String = "",
    val bairro: String = "",
    val instituicao: String = "",
    val fotoPerfilUrl: String = "",
    val consentimentoIa: Boolean = false,
    val necessidadesAcademicas: Set<NecessidadeAcademica> = emptySet(),
    val roleLabel: String = "Usuário",
    val hasSavedSession: Boolean = false
) {
    private val requiredFields = listOf(nome, email, whatsapp, cpf, cidade, bairro)

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
