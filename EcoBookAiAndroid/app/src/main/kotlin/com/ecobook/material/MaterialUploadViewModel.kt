package com.ecobook.material

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecobook.data.ApiException
import com.ecobook.data.MaterialRepository
import com.ecobook.data.ReferenceDataRepository
import com.ecobook.dto.CreateMaterialRequestDTO
import com.ecobook.dto.GeminiResponseDTO
import com.ecobook.model.AiAssistStatus
import com.ecobook.model.Disciplina
import com.ecobook.model.EstadoConservacao
import com.ecobook.model.NecessidadeAcademica
import com.ecobook.model.NivelEnsino
import com.ecobook.model.ReferenceDataCatalog
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
    private val materialRepository: MaterialRepository,
    private val referenceDataRepository: ReferenceDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MaterialUploadUiState())
    val uiState: StateFlow<MaterialUploadUiState> = _uiState.asStateFlow()

    init {
        loadReferenceData()
    }

    fun onImageSelected(uri: Uri, source: ImageSource, slot: ImageSlot) {
        runCatching { materialRepository.describeImage(uri, source) }
            .onSuccess { image ->
                _uiState.update {
                    val nextState = when (slot) {
                        ImageSlot.FRONT -> it.copy(selectedFrontImage = image)
                        ImageSlot.BACK -> it.copy(selectedBackImage = image)
                    }
                    nextState.copy(backendMessage = null)
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        backendMessage = error.message ?: "Não foi possível abrir a imagem selecionada."
                    )
                }
            }
    }

    fun onCameraPermissionDenied() {
        _uiState.update {
            it.copy(
                backendMessage = "Permita o uso da câmera para capturar uma nova foto do material."
            )
        }
    }

    fun clearSelectedImage(slot: ImageSlot? = null) {
        _uiState.update { state ->
            when (slot) {
                ImageSlot.FRONT -> resetState(state, selectedBackImage = state.selectedBackImage)
                ImageSlot.BACK -> state.copy(selectedBackImage = null, backendMessage = null)
                null -> resetState(state)
            }
        }
    }

    fun startPreview() {
        val frontImage = _uiState.value.selectedFrontImage ?: run {
            _uiState.update { it.copy(backendMessage = "Escolha a imagem da capa da frente antes de seguir.") }
            return
        }
        val backImage = _uiState.value.selectedBackImage

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

            runCatching { materialRepository.previewImage(frontImage, backImage) }
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
    fun updateAutor(value: String) = updateDraft { draft -> draft.copy(autor = value) }
    fun updateEditora(value: String) = updateDraft { draft -> draft.copy(editora = value) }
    fun updateDescricao(value: String) = updateDraft { draft -> draft.copy(descricao = value) }
    fun updateAno(value: String) = updateDraft { draft ->
        draft.copy(ano = sanitizeAnoEscolar(value, draft.nivelEnsino))
    }
    fun updateDataPublicacao(value: String) = updateDraft { draft -> draft.copy(dataPublicacao = value.filter(Char::isDigit).take(4)) }
    fun updateDisciplina(value: Disciplina?) = updateDraft { draft -> draft.copy(disciplina = value) }
    fun updateNivelEnsino(value: NivelEnsino?) = updateDraft { draft ->
        draft.copy(
            nivelEnsino = value,
            ano = sanitizeAnoEscolar(draft.ano, value)
        )
    }
    fun updateSistemaEnsino(value: SistemaEnsino?) = updateDraft { draft -> draft.copy(sistemaEnsino = value) }
    fun updateEstadoConservacao(value: EstadoConservacao?) = updateDraft { draft -> draft.copy(estadoConservacao = value) }
    fun updateNecessidadeAcademica(value: NecessidadeAcademica?) = updateDraft { draft ->
        draft.copy(necessidadeAcademica = value)
    }

    fun prepareSubmit(): Boolean {
        val currentState = _uiState.value
        val validationErrors = validate(currentState.draft, currentState.uploadId)
        if (validationErrors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    backendMessage = "Revise os campos destacados antes de publicar.",
                    validationErrors = validationErrors
                )
            }
            return false
        }

        return true
    }

    fun submitMaterial() {
        val currentState = _uiState.value
        if (currentState.stage != MaterialFlowStage.REVIEW || currentState.isBusy) {
            return
        }

        if (!prepareSubmit()) {
            return
        }

        val request = buildRequest(currentState)
            ?: run {
                _uiState.update {
                    it.copy(backendMessage = "Não foi possível montar a solicitação do material.")
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
        _uiState.value = resetState(_uiState.value)
    }

    private fun loadReferenceData() {
        viewModelScope.launch {
            val catalog = runCatching { referenceDataRepository.getCatalog() }
                .getOrElse { referenceDataRepository.defaultCatalog() }

            _uiState.update { state ->
                state.copy(
                    disciplinas = catalog.disciplinas,
                    niveisEnsino = catalog.niveisEnsino,
                    sistemasEnsino = catalog.sistemasEnsino,
                    estadosConservacao = catalog.estadosConservacao,
                    necessidadesAcademicas = catalog.necessidadesAcademicas,
                    draft = sanitizeDraft(state.draft, catalog)
                )
            }
        }
    }

    private fun updateDraft(transform: (MaterialDraft) -> MaterialDraft) {
        _uiState.update { state ->
            state.copy(
                draft = transform(state.draft),
                backendMessage = null,
                validationErrors = state.validationErrors.toMutableMap().apply {
                    remove("titulo")
                    remove("autor")
                    remove("editora")
                    remove("descricao")
                    remove("disciplina")
                    remove("nivel_ensino")
                    remove("ano")
                    remove("sistema_ensino")
                    remove("estado_conservacao")
                    remove("necessidade_academica")
                    remove("data_publicacao")
                }
            )
        }
    }

    private fun applyPreviewResponse(response: GeminiResponseDTO) {
        val predictions = response.bestPrediction
        val nivelEnsino = predictions["nivel_ensino"].enumValue(NivelEnsino::valueOf)
        val draft = MaterialDraft(
            titulo = predictions["titulo"].stringValue(),
            autor = predictions["autor"].stringValue(),
            editora = predictions["editora"].stringValue(),
            descricao = "",
            disciplina = predictions["disciplina"].enumValue(Disciplina::valueOf),
            nivelEnsino = nivelEnsino,
            ano = sanitizeAnoEscolar(predictions["ano"].intValue()?.toString().orEmpty(), nivelEnsino),
            sistemaEnsino = predictions["sistema_ensino"].enumValue(SistemaEnsino::valueOf),
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
            AiAssistStatus.SUCCESS -> "A IA encontrou um conjunto consistente de campos. Você ainda pode editar tudo antes de publicar."
            AiAssistStatus.LOW_CONFIDENCE -> "A IA trouxe pistas úteis, mas alguns campos pedem revisão manual antes da publicação."
            AiAssistStatus.FAILURE -> "A IA não conseguiu sugerir campos confiáveis. Complete o formulário manualmente. A capa traseira, quando enviada, segue vinculada ao cadastro."
            null -> "Revise os campos antes de continuar."
        }
    }

    private fun validate(draft: MaterialDraft, uploadId: String?): Map<String, String> {
        val errors = linkedMapOf<String, String>()

        if (uploadId.isNullOrBlank()) {
            errors["upload_id"] = "Envie a imagem novamente para gerar um upload_id válido."
        }
        if (draft.titulo.isBlank()) {
            errors["titulo"] = "Informe um título para o material."
        } else if (draft.titulo.length > 255) {
            errors["titulo"] = "O título precisa ter no máximo 255 caracteres."
        }

        if (draft.autor.length > 255) {
            errors["autor"] = "O autor precisa ter no máximo 255 caracteres."
        }

        if (draft.editora.length > 255) {
            errors["editora"] = "A editora precisa ter no máximo 255 caracteres."
        }

        if (draft.descricao.isBlank()) {
            errors["descricao"] = "Descreva o estado e o contexto de uso do material."
        } else if (draft.descricao.length !in 10..2000) {
            errors["descricao"] = "A descrição precisa ter entre 10 e 2000 caracteres."
        }

        if (draft.disciplina == null) {
            errors["disciplina"] = "Escolha a disciplina."
        }
        if (draft.nivelEnsino == null) {
            errors["nivel_ensino"] = "Escolha o nível de ensino."
        }
        if (draft.sistemaEnsino == null) {
            errors["sistema_ensino"] = "Escolha o sistema de ensino."
        }
        if (draft.estadoConservacao == null) {
            errors["estado_conservacao"] = "Escolha o estado de conservação."
        }
        if (draft.necessidadeAcademica == null) {
            errors["necessidade_academica"] = "Escolha a necessidade acadêmica do material."
        }

        if (draft.nivelEnsino == NivelEnsino.SUPERIOR) {
            if (draft.ano.isNotBlank()) {
                errors["ano"] = "Materiais de nível superior não usam ano escolar."
            }
        } else {
            val parsedYear = draft.ano.toIntOrNull()
            val maxAno = maxAnoEscolar(draft.nivelEnsino)
            if (parsedYear == null || parsedYear !in 1..maxAno) {
                errors["ano"] = "Informe um ano escolar válido para o nível selecionado."
            }
        }

        if (draft.dataPublicacao.isNotBlank()) {
            val publicationYear = draft.dataPublicacao.toIntOrNull()
            if (publicationYear == null || publicationYear !in 1900..2100) {
                errors["data_publicacao"] = "Informe um ano de publicação entre 1900 e 2100."
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
            autor = draft.autor.trim().ifBlank { null },
            editora = draft.editora.trim().ifBlank { null },
            descricao = draft.descricao.trim(),
            disciplina = draft.disciplina?.name ?: return null,
            nivelEnsino = draft.nivelEnsino?.name ?: return null,
            ano = if (draft.nivelEnsino == NivelEnsino.SUPERIOR) null else draft.ano.toIntOrNull(),
            sistemaEnsino = draft.sistemaEnsino?.name ?: return null,
            estadoConservacao = draft.estadoConservacao?.name ?: return null,
            necessidadeAcademica = draft.necessidadeAcademica?.name ?: return null,
            dataPublicacao = draft.dataPublicacao.toIntOrNull()
        )
    }

    private fun resolvePreviewError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                400 -> error.message.ifBlank { "A imagem precisa estar em JPEG ou PNG." }
                401 -> "Sua sessão expirou. Entre novamente para continuar."
                403 -> "Conclua seu perfil antes de enviar materiais."
                413 -> "A imagem excede 5MB. Tente outra foto ou deixe o app compactar uma versão menor."
                else -> error.message
            }

            is IllegalArgumentException -> error.message ?: "Selecione uma imagem JPEG ou PNG válida."
            is SocketTimeoutException -> "A análise demorou demais. Tente novamente em alguns instantes."
            is ConnectException,
            is UnknownHostException,
            is IOException -> "Não foi possível conectar ao backend configurado no app."
            else -> error.message ?: "Falha inesperada ao enviar a imagem."
        }
    }

    private fun resolveSubmitError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                400 -> error.message.ifBlank { "Revise os campos antes de publicar." }
                403 -> "Conclua seu perfil antes de publicar materiais."
                404 -> "O upload temporário expirou. Reenvie a imagem para continuar."
                409 -> error.message
                else -> error.message
            }

            is ConnectException,
            is UnknownHostException,
            is IOException -> "Não foi possível salvar o material porque o backend não respondeu."
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
            is String -> Regex("\\d{1,4}").find(rawValue)?.value?.toIntOrNull()
            else -> null
        }
    }

    private fun <T> com.ecobook.dto.PredictionFieldDTO?.enumValue(parser: (String) -> T): T? {
        val rawValue = this?.value?.toString()?.trim().orEmpty()
        if (rawValue.isBlank()) return null
        return runCatching { parser(rawValue) }.getOrNull()
    }

    private fun sanitizeDraft(draft: MaterialDraft, catalog: ReferenceDataCatalog): MaterialDraft {
        val nivelEnsino = draft.nivelEnsino?.takeIf(catalog.niveisEnsino::contains)
        return draft.copy(
            disciplina = draft.disciplina?.takeIf(catalog.disciplinas::contains),
            nivelEnsino = nivelEnsino,
            ano = sanitizeAnoEscolar(draft.ano, nivelEnsino),
            sistemaEnsino = draft.sistemaEnsino?.takeIf(catalog.sistemasEnsino::contains),
            estadoConservacao = draft.estadoConservacao?.takeIf(catalog.estadosConservacao::contains),
            necessidadeAcademica = draft.necessidadeAcademica?.takeIf(catalog.necessidadesAcademicas::contains)
        )
    }

    private fun resetState(
        currentState: MaterialUploadUiState,
        selectedFrontImage: SelectedImageUiModel? = null,
        selectedBackImage: SelectedImageUiModel? = null
    ): MaterialUploadUiState {
        return MaterialUploadUiState(
            selectedFrontImage = selectedFrontImage,
            selectedBackImage = selectedBackImage,
            disciplinas = currentState.disciplinas,
            niveisEnsino = currentState.niveisEnsino,
            sistemasEnsino = currentState.sistemasEnsino,
            estadosConservacao = currentState.estadosConservacao
        )
    }

    private fun sanitizeAnoEscolar(value: String, nivelEnsino: NivelEnsino?): String {
        if (nivelEnsino == NivelEnsino.SUPERIOR) {
            return ""
        }

        val digits = value.filter(Char::isDigit).take(1)
        val parsedYear = digits.toIntOrNull() ?: return digits
        val maxAno = maxAnoEscolar(nivelEnsino)
        return if (parsedYear in 1..maxAno) parsedYear.toString() else ""
    }

    private fun maxAnoEscolar(nivelEnsino: NivelEnsino?): Int {
        return when (nivelEnsino) {
            NivelEnsino.MEDIO -> 3
            else -> 9
        }
    }
}
