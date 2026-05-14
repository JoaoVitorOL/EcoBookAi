package com.ecobook.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecobook.data.ApiException
import com.ecobook.data.MaterialRepository
import com.ecobook.dto.MaterialDTO
import com.ecobook.dto.UpdateMaterialRequestDTO
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
class DonateViewModel @Inject constructor(
    private val materialRepository: MaterialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DonateUiState(isLoading = true))
    val uiState: StateFlow<DonateUiState> = _uiState.asStateFlow()

    init {
        refreshMaterials()
    }

    fun refreshMaterials() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { materialRepository.listCurrentUserMaterials() }
                .onSuccess { materials ->
                    _uiState.update {
                        it.copy(
                            materials = materials,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = resolveLoadError(error)
                        )
                    }
                }
        }
    }

    fun onMaterialPublished(material: MaterialDTO) {
        _uiState.update {
            it.copy(toastMessage = "\"${material.titulo}\" foi publicado e ja entrou na sua lista.")
        }
        refreshMaterials()
    }

    fun openEditor(material: MaterialDTO) {
        _uiState.update {
            it.copy(
                materialBeingEdited = material,
                editDraft = material.toDonateDraft(),
                validationErrors = emptyMap(),
                editorMessage = null
            )
        }
    }

    fun dismissEditor() {
        _uiState.update {
            it.copy(
                materialBeingEdited = null,
                editDraft = DonateMaterialDraft(),
                validationErrors = emptyMap(),
                editorMessage = null
            )
        }
    }

    fun promptDelete(material: MaterialDTO) {
        _uiState.update { it.copy(pendingDeleteMaterial = material) }
    }

    fun dismissDeletePrompt() {
        _uiState.update { it.copy(pendingDeleteMaterial = null) }
    }

    fun updateTitulo(value: String) = updateDraft { copy(titulo = value) }
    fun updateAutor(value: String) = updateDraft { copy(autor = value) }
    fun updateEditora(value: String) = updateDraft { copy(editora = value) }
    fun updateDescricao(value: String) = updateDraft { copy(descricao = value) }
    fun updateAno(value: String) = updateDraft { copy(ano = sanitizeAnoEscolar(value, nivelEnsino)) }
    fun updateDataPublicacao(value: String) = updateDraft { copy(dataPublicacao = value.filter(Char::isDigit).take(4)) }
    fun updateDisciplina(value: Disciplina?) = updateDraft { copy(disciplina = value) }
    fun updateNivelEnsino(value: NivelEnsino?) = updateDraft {
        copy(
            nivelEnsino = value,
            ano = sanitizeAnoEscolar(ano, value)
        )
    }
    fun updateSistemaEnsino(value: SistemaEnsino?) = updateDraft { copy(sistemaEnsino = value) }
    fun updateEstadoConservacao(value: EstadoConservacao?) = updateDraft { copy(estadoConservacao = value) }

    fun saveEditedMaterial() {
        val material = uiState.value.materialBeingEdited ?: return
        val draft = uiState.value.editDraft
        val validationErrors = validateDraft(draft)
        if (validationErrors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    validationErrors = validationErrors,
                    editorMessage = "Revise os campos destacados antes de salvar."
                )
            }
            return
        }

        val request = UpdateMaterialRequestDTO(
            titulo = draft.titulo.trim(),
            autor = draft.autor.trim().ifBlank { null },
            editora = draft.editora.trim().ifBlank { null },
            descricao = draft.descricao.trim(),
            disciplina = draft.disciplina?.name ?: return,
            nivelEnsino = draft.nivelEnsino?.name ?: return,
            ano = if (draft.nivelEnsino == NivelEnsino.SUPERIOR) null else draft.ano.toIntOrNull(),
            sistemaEnsino = draft.sistemaEnsino?.name ?: return,
            estadoConservacao = draft.estadoConservacao?.name ?: return,
            dataPublicacao = draft.dataPublicacao.toIntOrNull()
        )

        _uiState.update {
            it.copy(
                isSaving = true,
                editorMessage = null,
                validationErrors = emptyMap()
            )
        }

        viewModelScope.launch {
            runCatching { materialRepository.updateMaterial(material.id, request) }
                .onSuccess { updatedMaterial ->
                    _uiState.update { state ->
                        state.copy(
                            materials = state.materials.map { current ->
                                if (current.id == updatedMaterial.id) updatedMaterial else current
                            },
                            isSaving = false,
                            materialBeingEdited = null,
                            editDraft = DonateMaterialDraft(),
                            validationErrors = emptyMap(),
                            editorMessage = null,
                            toastMessage = "Material atualizado com sucesso."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            editorMessage = resolveEditorError(error),
                            validationErrors = error.asFieldErrors()
                        )
                    }
                }
        }
    }

    fun deletePendingMaterial() {
        val material = uiState.value.pendingDeleteMaterial ?: return
        _uiState.update { it.copy(isDeleting = true) }

        viewModelScope.launch {
            runCatching { materialRepository.deleteMaterial(material.id) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            materials = state.materials.filterNot { current -> current.id == material.id },
                            isDeleting = false,
                            pendingDeleteMaterial = null,
                            toastMessage = "Material excluido da sua conta."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            pendingDeleteMaterial = null,
                            toastMessage = resolveDeleteError(error)
                        )
                    }
                }
        }
    }

    fun consumeToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    private fun updateDraft(transform: DonateMaterialDraft.() -> DonateMaterialDraft) {
        _uiState.update { state ->
            state.copy(
                editDraft = state.editDraft.transform(),
                validationErrors = emptyMap(),
                editorMessage = null
            )
        }
    }

    private fun validateDraft(draft: DonateMaterialDraft): Map<String, String> {
        val errors = linkedMapOf<String, String>()

        if (draft.titulo.isBlank()) {
            errors["titulo"] = "Informe um titulo para o material."
        } else if (draft.titulo.length > 255) {
            errors["titulo"] = "O titulo precisa ter no maximo 255 caracteres."
        }

        if (draft.autor.length > 255) {
            errors["autor"] = "O autor precisa ter no maximo 255 caracteres."
        }

        if (draft.editora.length > 255) {
            errors["editora"] = "A editora precisa ter no maximo 255 caracteres."
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
            val maxAno = maxAnoEscolar(draft.nivelEnsino)
            if (parsedYear == null || parsedYear !in 1..maxAno) {
                errors["ano"] = "Informe um ano escolar valido para o nivel selecionado."
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

    private fun resolveLoadError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                401 -> "Sua sessao expirou. Entre novamente para continuar."
                403 -> "Conclua o onboarding para gerenciar seus materiais."
                else -> error.message
            }

            is SocketTimeoutException -> "A listagem demorou demais para responder."
            is ConnectException,
            is UnknownHostException,
            is IOException -> "Nao foi possivel conectar ao backend configurado no app."
            else -> error.message ?: "Falha inesperada ao carregar seus materiais."
        }
    }

    private fun resolveEditorError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                400, 422 -> error.message
                403 -> "Somente o doador do material pode editar este cadastro."
                404 -> "Esse material nao foi encontrado."
                else -> error.message
            }

            is ConnectException,
            is UnknownHostException,
            is IOException -> "Nao foi possivel salvar as alteracoes porque o backend nao respondeu."
            else -> error.message ?: "Falha inesperada ao atualizar o material."
        }
    }

    private fun resolveDeleteError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                403 -> "Somente o doador do material pode excluir este cadastro."
                404 -> "Esse material nao foi encontrado."
                422 -> error.message
                else -> error.message
            }

            is ConnectException,
            is UnknownHostException,
            is IOException -> "Nao foi possivel excluir o material porque o backend nao respondeu."
            else -> error.message ?: "Falha inesperada ao excluir o material."
        }
    }

    private fun Throwable.asFieldErrors(): Map<String, String> {
        return (this as? ApiException)?.fieldErrors ?: emptyMap()
    }

    private fun MaterialDTO.toDonateDraft(): DonateMaterialDraft {
        return DonateMaterialDraft(
            titulo = titulo,
            autor = autor.orEmpty(),
            editora = editora.orEmpty(),
            descricao = descricao,
            disciplina = enumOrNull<Disciplina>(disciplina),
            nivelEnsino = enumOrNull<NivelEnsino>(nivelEnsino),
            ano = ano?.toString().orEmpty(),
            sistemaEnsino = enumOrNull<SistemaEnsino>(sistemaEnsino),
            estadoConservacao = enumOrNull<EstadoConservacao>(estadoConservacao),
            dataPublicacao = dataPublicacao?.toString().orEmpty()
        )
    }

    private inline fun <reified T : Enum<T>> enumOrNull(value: String): T? {
        return runCatching { enumValueOf<T>(value) }.getOrNull()
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
