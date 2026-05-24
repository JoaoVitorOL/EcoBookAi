package com.ecobook.material

import android.net.Uri
import com.ecobook.data.MaterialRepository
import com.ecobook.data.ReferenceDataRepository
import com.ecobook.dto.GeminiResponseDTO
import com.ecobook.dto.PredictionFieldDTO
import com.ecobook.model.Disciplina
import com.ecobook.model.NivelEnsino
import com.ecobook.model.ReferenceDataCatalog
import com.ecobook.model.SistemaEnsino
import com.ecobook.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MaterialUploadViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun defaultCatalogShouldExposeTodasForDisciplina() {
        val disciplinas = ReferenceDataCatalog.defaults().disciplinas

        assertEquals(Disciplina.TODAS, disciplinas.first())
        assertTrue(disciplinas.contains(Disciplina.TODAS))
    }

    @Test
    fun startPreviewShouldKeepTodasPredictionFromGemini() = runTest {
        val materialRepository = mockk<MaterialRepository>()
        val referenceDataRepository = mockk<ReferenceDataRepository>()
        val selectedImage = SelectedImageUiModel(
            uri = mockk<Uri>(),
            fileName = "frente.png",
            mimeType = "image/png",
            sizeBytes = 1024L,
            source = ImageSource.GALLERY
        )
        val previewResponse = GeminiResponseDTO(
            statusIa = "LOW_CONFIDENCE",
            uploadId = "temp-upload-123",
            bestPrediction = mapOf(
                "titulo" to PredictionFieldDTO(value = "Colecao Integrada", confidence = 0.94),
                "disciplina" to PredictionFieldDTO(value = "TODAS", confidence = 0.95),
                "nivel_ensino" to PredictionFieldDTO(value = "FUNDAMENTAL", confidence = 0.87),
                "ano" to PredictionFieldDTO(value = 7, confidence = 0.82),
                "sistema_ensino" to PredictionFieldDTO(value = "COC", confidence = 0.79)
            )
        )

        every { referenceDataRepository.defaultCatalog() } returns ReferenceDataCatalog.defaults()
        coEvery { referenceDataRepository.getCatalog(any()) } returns ReferenceDataCatalog.defaults()
        every { materialRepository.describeImage(any(), any()) } returns selectedImage
        coEvery { materialRepository.previewImage(selectedImage, null) } returns previewResponse

        val viewModel = MaterialUploadViewModel(materialRepository, referenceDataRepository)
        advanceUntilIdle()

        viewModel.onImageSelected(selectedImage.uri, ImageSource.GALLERY, ImageSlot.FRONT)
        advanceUntilIdle()
        viewModel.startPreview()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(Disciplina.TODAS, state.draft.disciplina)
        assertEquals(NivelEnsino.FUNDAMENTAL, state.draft.nivelEnsino)
        assertEquals(SistemaEnsino.COC, state.draft.sistemaEnsino)
        assertTrue(state.disciplinas.contains(Disciplina.TODAS))
    }
}
