package com.ecobook.material

import android.net.Uri
import com.ecobook.dto.MaterialDTO
import com.ecobook.model.AiAssistStatus
import com.ecobook.model.Disciplina
import com.ecobook.model.EstadoConservacao
import com.ecobook.model.NivelEnsino
import com.ecobook.model.SistemaEnsino

enum class MaterialFlowStage {
    SELECT,
    PROCESSING,
    REVIEW,
    SUCCESS
}

enum class ImageSource {
    GALLERY,
    CAMERA
}

data class SelectedImageUiModel(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val source: ImageSource
)

data class MaterialDraft(
    val titulo: String = "",
    val descricao: String = "",
    val disciplina: Disciplina? = null,
    val nivelEnsino: NivelEnsino? = null,
    val ano: String = "",
    val sistemaEnsino: SistemaEnsino? = null,
    val estadoConservacao: EstadoConservacao? = null,
    val dataPublicacao: String = ""
)

data class MaterialUploadUiState(
    val stage: MaterialFlowStage = MaterialFlowStage.SELECT,
    val selectedImage: SelectedImageUiModel? = null,
    val isBusy: Boolean = false,
    val overallStatus: AiAssistStatus? = null,
    val uploadId: String? = null,
    val previewMessage: String? = null,
    val backendMessage: String? = null,
    val validationErrors: Map<String, String> = emptyMap(),
    val confidenceByField: Map<String, Double?> = emptyMap(),
    val draft: MaterialDraft = MaterialDraft(),
    val createdMaterial: MaterialDTO? = null
) {
    val canStartPreview: Boolean
        get() = selectedImage != null && !isBusy

    val canSubmit: Boolean
        get() = uploadId != null && !isBusy
}
