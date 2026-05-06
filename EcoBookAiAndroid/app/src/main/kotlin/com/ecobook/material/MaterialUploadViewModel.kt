package com.ecobook.material

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecobook.data.ApiException
import com.ecobook.data.MaterialRepository
import com.ecobook.dto.CreateMaterialRequestDTO
import com.ecobook.dto.GeminiResponseDTO
import com.ecobook.model.AiAssistStatus
import com.ecobook.model.Disciplina
import com.ecobook.model.EstadoConservacao
import com.ecobook.model.NivelEnsino
import com.ecobook.model.SistemaEnsino
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

@HiltViewModel
class MaterialUploadViewModel @Inject constructor(
    private val materialRepository: MaterialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MaterialUploadUiState())
    val uiState: StateFlow<MaterialUploadUiState> = _uiState.asStateFlow()

    fun onImageSelected(uri: Uri, source: ImageSource) {
        runCatching { materialRepository.describeImage(uri, source) }
            .onSuccess { image ->
                _uiState.update {
                    MaterialUploadUiState(
                        selectedImage = image,
                        backendMessage = null
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        backendMessage = error.message ?: "Nao foi possivel abrir a imagem selecionada."
                    )
                }
            }
    }

    fun onCameraPermissionDenied() {
        _uiState.update {
            it.copy(
                backendMessage = "Permita o uso da camera para capturar uma nova foto do material."
            )
        }
    }

    fun clearSelectedImage() {
        _uiState.update { MaterialUploadUiState() }
    }

    fun startPreview() {
        val image = _uiState.value.selectedImage ?: run {
            _uiState.update { it.copy(backendMessage = "Escolha uma imagem antes de seguir.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    stage = MaterialFlowStage.PROCESSING,
                    isBusy = true,
                    backendMessage = null,
                    validationErrors = emptyMap(),
                    createdMaterial = null
                )
            }

            runCatching { materialRepository.previewImage(image) }
                .onSuccess { response -> applyPreviewResponse(response) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            stage = MaterialFlowStage.SELECT,
                            isBusy = false,
                            backendMessage = resolvePreviewError(error),
                            validationErrors = error.asFieldErrors()
                        )
                    }
                }
        }
    }

    fun updateTitulo(value: String) = updateDraft { draft -> draft.copy(titulo = value) }
    fun updateDescricao(value: String) = updateDraft { draft -> draft.copy(descricao = value) }
    fun updateAno(value: String) = updateDraft { draft -> draft.copy(ano = value.filter(Char::isDigit)) }
    fun updateDataPublicacao(value: String) = updateDraft { draft -> draft.copy(dataPublicacao = value.filter(Char::isDigit)) }
    fun updateDisciplina(value: Disciplina?) = updateDraft { draft -> draft.copy(disciplina = value) }
    fun updateNivelEnsino(value: NivelEnsino?) = updateDraft { draft ->
        draft.copy(
            nivelEnsino = value,
            ano = if (value == NivelEnsino.SUPERIOR) "" else draft.ano
        )
    }
    fun updateSistemaEnsino(value: SistemaEnsino?) = updateDraft { draft -> draft.copy(sistemaEnsino = value) }
    fun updateEstadoConservacao(value: EstadoConservacao?) = updateDraft { draft -> draft.copy(estadoConservacao = value) }

    fun submitMaterial() {
        val currentState = _uiState.value
        val validationErrors = validate(currentState.draft, currentState.uploadId)
        if (validationErrors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    backendMessage = "Revise os campos destacados antes de publicar.",
                    validationErrors = validationErrors
                )
            }
            return
        }

        val request = buildRequest(currentState)
            ?: run {
                _uiState.update {
                    it.copy(backendMessage = "Nao foi possivel montar a solicitacao do material.")
                }
                return
            }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    backendMessage = null,
                    validationErrors = emptyMap()
                )
            }

            runCatching { materialRepository.createMaterial(request) }
                .onSuccess { material ->
                    _uiState.update {
                        it.copy(
                            stage = MaterialFlowStage.SUCCESS,
                            isBusy = false,
                            createdMaterial = material,
                            backendMessage = "Material publicado com sucesso e pronto para a Fase 4 de descoberta."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            stage = MaterialFlowStage.REVIEW,
                            isBusy = false,
                            backendMessage = resolveSubmitError(error),
                            validationErrors = error.asFieldErrors()
                        )
                    }
                }
        }
    }

    fun restartFlow() {
        _uiState.value = MaterialUploadUiState()
    }

    private fun updateDraft(transform: (MaterialDraft) -> MaterialDraft) {
        _uiState.update { state ->
            state.copy(
                draft = transform(state.draft),
                backendMessage = null,
                validationErrors = state.validationErrors.toMutableMap().apply {
                    remove("titulo")
                    remove("descricao")
                    remove("disciplina")
                    remove("nivel_ensino")
                    remove("ano")
                    remove("sistema_ensino")
                    remove("estado_conservacao")
                    remove("data_publicacao")
                }
            )
        }
    }

    private fun applyPreviewResponse(response: GeminiResponseDTO) {
        val predictions = response.bestPrediction
        val draft = MaterialDraft(
            titulo = predictions["titulo"].stringValue(),
            descricao = "",
            disciplina = predictions["disciplina"].enumValue(Disciplina::valueOf),
            nivelEnsino = predictions["nivel_ensino"].enumValue(NivelEnsino::valueOf),
            ano = predictions["ano"].intValue()?.toString().orEmpty(),
            sistemaEnsino = predictions["sistema_ensino"].enumValue(SistemaEnsino::valueOf),
            estadoConservacao = predictions["estado_conservacao"].enumValue(EstadoConservacao::valueOf),
            dataPublicacao = predictions["data_publicacao"].intValue()?.toString().orEmpty()
        )

        _uiState.update {
            it.copy(
                stage = MaterialFlowStage.REVIEW,
                isBusy = false,
                overallStatus = response.statusIa.toAssistStatus(),
                uploadId = response.uploadId.takeIf(String::isNotBlank),
                previewMessage = response.errorDetails.message ?: defaultPreviewMessage(response),
                backendMessage = response.errorDetails.message?.takeIf { _ -> response.statusIa == "FAILURE" },
                confidenceByField = predictions.mapValues { entry -> entry.value.confidence },
                draft = draft,
                validationErrors = emptyMap()
            )
        }
    }

    private fun defaultPreviewMessage(response: GeminiResponseDTO): String {
        return when (response.statusIa.toAssistStatus()) {
            AiAssistStatus.SUCCESS -> "A IA encontrou um conjunto consistente de campos. Voce ainda pode editar tudo antes de publicar."
            AiAssistStatus.LOW_CONFIDENCE -> "A IA trouxe pistas uteis, mas alguns campos pedem revisao manual antes da publicacao."
            AiAssistStatus.FAILURE -> "A IA nao conseguiu sugerir campos confiaveis. Complete o formulario manualmente."
            null -> "Revise os campos antes de continuar."
        }
    }

    private fun validate(draft: MaterialDraft, uploadId: String?): Map<String, String> {
        val errors = linkedMapOf<String, String>()

        if (uploadId.isNullOrBlank()) {
            errors["upload_id"] = "Envie a imagem novamente para gerar um upload_id valido."
        }
        if (draft.titulo.isBlank()) {
            errors["titulo"] = "Informe um titulo para o material."
        } else if (draft.titulo.length > 255) {
            errors["titulo"] = "O titulo precisa ter no maximo 255 caracteres."
        }

        if (draft.descricao.isBlank()) {
            errors["descricao"] = "Descreva o estado e o contexto de uso do material."
        } else if (draft.descricao.length !in 10..2000) {
            errors["descricao"] = "A descricao precisa ter entre 10 e 2000 caracteres."
        }

        if (draft.disciplina == null) {
            errors["disciplina"] = "Escolha a disciplina."
        }
        if (draft.nivelEnsino == null) {
            errors["nivel_ensino"] = "Escolha o nivel de ensino."
        }
        if (draft.sistemaEnsino == null) {
            errors["sistema_ensino"] = "Escolha o sistema de ensino."
        }
        if (draft.estadoConservacao == null) {
            errors["estado_conservacao"] = "Escolha o estado de conservacao."
        }

        if (draft.nivelEnsino == NivelEnsino.SUPERIOR) {
            if (draft.ano.isNotBlank()) {
                errors["ano"] = "Materiais de nivel superior nao usam ano escolar."
            }
        } else {
            val parsedYear = draft.ano.toIntOrNull()
            if (parsedYear == null || parsedYear !in 1..12) {
                errors["ano"] = "Informe um ano escolar entre 1 e 12."
            }
        }

        if (draft.dataPublicacao.isNotBlank()) {
            val publicationYear = draft.dataPublicacao.toIntOrNull()
            if (publicationYear == null || publicationYear !in 1900..2100) {
                errors["data_publicacao"] = "Informe um ano de publicacao entre 1900 e 2100."
            }
        }

        return errors
    }

    private fun buildRequest(state: MaterialUploadUiState): CreateMaterialRequestDTO? {
        val draft = state.draft
        val uploadId = state.uploadId ?: return null
        return CreateMaterialRequestDTO(
            uploadId = uploadId,
            titulo = draft.titulo.trim(),
            descricao = draft.descricao.trim(),
            disciplina = draft.disciplina?.name ?: return null,
            nivelEnsino = draft.nivelEnsino?.name ?: return null,
            ano = if (draft.nivelEnsino == NivelEnsino.SUPERIOR) null else draft.ano.toIntOrNull(),
            sistemaEnsino = draft.sistemaEnsino?.name ?: return null,
            estadoConservacao = draft.estadoConservacao?.name ?: return null,
            dataPublicacao = draft.dataPublicacao.toIntOrNull()
        )
    }

    private fun resolvePreviewError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                400 -> error.message.ifBlank { "A imagem precisa estar em JPEG ou PNG." }
                401 -> "Sua sessao expirou. Entre novamente para continuar."
                403 -> "Conclua seu perfil antes de enviar materiais."
                413 -> "A imagem excede 5MB. Tente outra foto ou deixe o app compactar uma versao menor."
                else -> error.message
            }

            is IllegalArgumentException -> error.message ?: "Selecione uma imagem JPEG ou PNG valida."
            is SocketTimeoutException -> "A analise demorou demais. Tente novamente em alguns instantes."
            is ConnectException,
            is UnknownHostException,
            is IOException -> "Nao foi possivel conectar ao backend configurado no app."
            else -> error.message ?: "Falha inesperada ao enviar a imagem."
        }
    }

    private fun resolveSubmitError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                400 -> error.message.ifBlank { "Revise os campos antes de publicar." }
                403 -> "Conclua seu perfil antes de publicar materiais."
                404 -> "O upload temporario expirou. Reenvie a imagem para continuar."
                409 -> error.message
                else -> error.message
            }

            is ConnectException,
            is UnknownHostException,
            is IOException -> "Nao foi possivel salvar o material porque o backend nao respondeu."
            else -> error.message ?: "Falha inesperada ao publicar o material."
        }
    }

    private fun Throwable.asFieldErrors(): Map<String, String> {
        return (this as? ApiException)?.fieldErrors ?: emptyMap()
    }

    private fun String.toAssistStatus(): AiAssistStatus? {
        return when (uppercase()) {
            "SUCCESS" -> AiAssistStatus.SUCCESS
            "LOW_CONFIDENCE" -> AiAssistStatus.LOW_CONFIDENCE
            "FAILURE" -> AiAssistStatus.FAILURE
            else -> null
        }
    }

    private fun com.ecobook.dto.PredictionFieldDTO?.stringValue(): String {
        return this?.value?.toString().orEmpty()
    }

    private fun com.ecobook.dto.PredictionFieldDTO?.intValue(): Int? {
        val rawValue = this?.value ?: return null
        return when (rawValue) {
            is Double -> rawValue.toInt()
            is Float -> rawValue.toInt()
            is Int -> rawValue
            is Number -> rawValue.toInt()
            is String -> rawValue.toIntOrNull()
            else -> null
        }
    }

    private fun <T> com.ecobook.dto.PredictionFieldDTO?.enumValue(parser: (String) -> T): T? {
        val rawValue = this?.value?.toString()?.trim().orEmpty()
        if (rawValue.isBlank()) return null
        return runCatching { parser(rawValue) }.getOrNull()
    }
}
