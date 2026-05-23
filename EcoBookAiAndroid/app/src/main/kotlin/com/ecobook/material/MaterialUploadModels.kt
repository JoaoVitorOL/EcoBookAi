package com.ecobook.material

import android.net.Uri
import com.ecobook.dto.MaterialDTO
import com.ecobook.model.AiAssistStatus
import com.ecobook.model.Disciplina
import com.ecobook.model.EstadoConservacao
import com.ecobook.model.NivelEnsino
import com.ecobook.model.ReferenceDataCatalog
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

enum class ImageSlot {
    FRONT,
    BACK
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
    val autor: String = "",
    val editora: String = "",
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
    val selectedFrontImage: SelectedImageUiModel? = null,
    val selectedBackImage: SelectedImageUiModel? = null,
    val disciplinas: List<Disciplina> = ReferenceDataCatalog.defaults().disciplinas,
    val niveisEnsino: List<NivelEnsino> = ReferenceDataCatalog.defaults().niveisEnsino,
    val sistemasEnsino: List<SistemaEnsino> = ReferenceDataCatalog.defaults().sistemasEnsino,
    val estadosConservacao: List<EstadoConservacao> = ReferenceDataCatalog.defaults().estadosConservacao,
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
    val selectedImage: SelectedImageUiModel?
        get() = selectedFrontImage

    val canStartPreview: Boolean
        get() = selectedFrontImage != null && !isBusy

    val canSubmit: Boolean
        get() = uploadId != null && !isBusy
}
