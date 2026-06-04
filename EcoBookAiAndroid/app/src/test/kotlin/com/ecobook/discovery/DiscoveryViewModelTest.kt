package com.ecobook.discovery

import com.ecobook.data.MaterialRepository
import com.ecobook.data.ReferenceDataRepository
import com.ecobook.data.RequestRepository
import com.ecobook.dto.MaterialDTO
import com.ecobook.dto.MaterialDonorDTO
import com.ecobook.dto.PagedResponseDTO
import com.ecobook.model.NivelEnsino
import com.ecobook.model.ReferenceDataCatalog
import com.ecobook.testutil.MainDispatcherRule
import com.ecobook.utils.SecureStorage
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoveryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initShouldPrefillCityAndNeighborhoodAndRunFirstSearch() = runTest {
        val repository = mockk<MaterialRepository>()
        val requestRepository = mockk<RequestRepository>(relaxed = true)
        val referenceDataRepository = mockReferenceDataRepository()
        val secureStorage = mockk<SecureStorage>()
        every { secureStorage.getUserCidade() } returns "Florianopolis"
        every { secureStorage.getUserBairro() } returns "Centro"
        coEvery {
            repository.searchMaterials(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns pagedResponse(
            results = listOf(sampleMaterial(id = "mat-1")),
            page = 0,
            hasNext = false,
            total = 1
        )

        val viewModel = DiscoveryViewModel(repository, requestRepository, referenceDataRepository, secureStorage)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Florianopolis", state.filters.cidade)
        assertEquals("Centro", state.filters.bairro)
        assertEquals(1, state.results.size)
        assertEquals("mat-1", state.results.first().id)
        assertFalse(state.isLoading)

        coVerify(exactly = 1) {
            repository.searchMaterials(
                query = null,
                disciplina = null,
                nivelEnsino = null,
                ano = null,
                sistemaEnsino = null,
                necessidadeAcademica = null,
                cidade = "Florianopolis",
                bairro = "Centro",
                minAnoPublicacao = null,
                maxAnoPublicacao = null,
                afterId = null,
                page = 0,
                size = 20
            )
        }
    }

    @Test
    fun searchShouldExposeInlineFilterErrorsWhenPublicationRangeIsInvalid() = runTest {
        val repository = mockk<MaterialRepository>()
        val requestRepository = mockk<RequestRepository>(relaxed = true)
        val referenceDataRepository = mockReferenceDataRepository()
        val secureStorage = mockk<SecureStorage>()
        every { secureStorage.getUserCidade() } returns null
        every { secureStorage.getUserBairro() } returns null
        coEvery {
            repository.searchMaterials(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns pagedResponse()

        val viewModel = DiscoveryViewModel(repository, requestRepository, referenceDataRepository, secureStorage)
        advanceUntilIdle()
        clearMocks(repository, answers = false)

        viewModel.updateMinAnoPublicacao("2025")
        viewModel.updateMaxAnoPublicacao("2020")
        viewModel.search()

        val state = viewModel.uiState.value
        assertEquals(
            "O ano inicial de publicacao nao pode ser maior que o ano final.",
            state.filterErrors["minAnoPublicacao"]
        )
        assertEquals(
            "O ano inicial de publicacao nao pode ser maior que o ano final.",
            state.filterErrors["maxAnoPublicacao"]
        )
        assertEquals(null, state.toastMessage)

        coVerify(exactly = 0) {
            repository.searchMaterials(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun loadNextPageShouldAppendResultsUsingActiveFilters() = runTest {
        val repository = mockk<MaterialRepository>()
        val requestRepository = mockk<RequestRepository>(relaxed = true)
        val referenceDataRepository = mockReferenceDataRepository()
        val secureStorage = mockk<SecureStorage>()
        every { secureStorage.getUserCidade() } returns "Sao Jose"
        every { secureStorage.getUserBairro() } returns ""
        coEvery {
            repository.searchMaterials(
                query = null,
                disciplina = null,
                nivelEnsino = null,
                ano = null,
                sistemaEnsino = null,
                necessidadeAcademica = null,
                cidade = "Sao Jose",
                bairro = null,
                minAnoPublicacao = null,
                maxAnoPublicacao = null,
                afterId = null,
                page = 0,
                size = 20
            )
        } returns pagedResponse(
            results = listOf(sampleMaterial(id = "mat-1")),
            page = 0,
            hasNext = true,
            total = 2,
            nextAfterId = "mat-1"
        )
        coEvery {
            repository.searchMaterials(
                query = null,
                disciplina = null,
                nivelEnsino = null,
                ano = null,
                sistemaEnsino = null,
                necessidadeAcademica = null,
                cidade = "Sao Jose",
                bairro = null,
                minAnoPublicacao = null,
                maxAnoPublicacao = null,
                afterId = "mat-1",
                page = 1,
                size = 20
            )
        } returns pagedResponse(
            results = listOf(sampleMaterial(id = "mat-2")),
            page = 1,
            hasNext = false,
            total = 2
        )

        val viewModel = DiscoveryViewModel(repository, requestRepository, referenceDataRepository, secureStorage)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.results.size)
        assertEquals(listOf("mat-1", "mat-2"), state.results.map { it.id })
        assertFalse(state.hasNext)
    }

    @Test
    fun selectingSuperiorShouldClearSchoolYearField() = runTest {
        val repository = mockk<MaterialRepository>()
        val requestRepository = mockk<RequestRepository>(relaxed = true)
        val referenceDataRepository = mockReferenceDataRepository()
        val secureStorage = mockk<SecureStorage>()
        every { secureStorage.getUserCidade() } returns null
        every { secureStorage.getUserBairro() } returns null
        coEvery {
            repository.searchMaterials(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns pagedResponse()

        val viewModel = DiscoveryViewModel(repository, requestRepository, referenceDataRepository, secureStorage)
        advanceUntilIdle()

        viewModel.updateAno("7")
        viewModel.updateNivelEnsino(NivelEnsino.SUPERIOR)

        assertTrue(viewModel.uiState.value.filters.ano.isBlank())
    }

    @Test
    fun selectingHighSchoolShouldOnlyKeepYearsUpToThree() = runTest {
        val repository = mockk<MaterialRepository>()
        val requestRepository = mockk<RequestRepository>(relaxed = true)
        val referenceDataRepository = mockReferenceDataRepository()
        val secureStorage = mockk<SecureStorage>()
        every { secureStorage.getUserCidade() } returns null
        every { secureStorage.getUserBairro() } returns null
        coEvery {
            repository.searchMaterials(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns pagedResponse()

        val viewModel = DiscoveryViewModel(repository, requestRepository, referenceDataRepository, secureStorage)
        advanceUntilIdle()

        viewModel.updateNivelEnsino(NivelEnsino.MEDIO)
        viewModel.updateAno("4")

        assertTrue(viewModel.uiState.value.filters.ano.isBlank())

        viewModel.updateAno("3")

        assertEquals("3", viewModel.uiState.value.filters.ano)
    }

    private fun pagedResponse(
        results: List<MaterialDTO> = emptyList(),
        page: Int = 0,
        hasNext: Boolean = false,
        total: Long = results.size.toLong(),
        nextAfterId: String? = null
    ): PagedResponseDTO<MaterialDTO> {
        return PagedResponseDTO(
            results = results,
            page = page,
            size = 20,
            total = total,
            totalPages = if (hasNext) page + 2 else page + 1,
            hasNext = hasNext,
            nextAfterId = nextAfterId
        )
    }

    private fun sampleMaterial(id: String): MaterialDTO {
        return MaterialDTO(
            id = id,
            titulo = "Geometria $id",
            autor = "Autor",
            editora = "Editora",
            descricao = "Descricao do material",
            disciplina = "MATEMATICA",
            nivelEnsino = "FUNDAMENTAL",
            ano = 7,
            sistemaEnsino = "ANGLO",
            estadoConservacao = "BOM",
            necessidadeAcademica = "TEXTBOOKS",
            status = "DISPONIVEL",
            imagemUrl = null,
            uploadId = null,
            doador = MaterialDonorDTO(
                id = "doador-1",
                nome = "Pessoa Doadora",
                whatsapp = "+5548999999999",
                cidade = "Florianopolis",
                bairro = "Centro"
            ),
            cidade = "Florianopolis",
            bairro = "Centro",
            dataPublicacao = 2023,
            statusIa = null,
            confiancaIa = null,
            criadoEm = "2026-05-12T10:00:00Z",
            atualizadoEm = "2026-05-12T10:00:00Z"
        )
    }

    private fun mockReferenceDataRepository(): ReferenceDataRepository {
        return mockk<ReferenceDataRepository>().also { repository ->
            coEvery { repository.getCatalog(any()) } returns ReferenceDataCatalog.defaults()
            every { repository.defaultCatalog() } returns ReferenceDataCatalog.defaults()
        }
    }
}
