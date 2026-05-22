package com.ecobook.request

import com.ecobook.data.ApiException
import com.ecobook.data.RequestRepository
import com.ecobook.dto.MaterialNonReceiptReportDTO
import com.ecobook.dto.SolicitacaoDTO
import com.ecobook.testutil.MainDispatcherRule
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyRequestsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun updateFilterShouldReloadFromBackendUsingSelectedStatus() = runTest {
        val repository = mockk<RequestRepository>()
        coEvery { repository.listMyRequests(null) } returns listOf(sampleRequest("req-all", "PENDENTE"))
        coEvery { repository.listMyRequests("APROVADA") } returns listOf(sampleRequest("req-approved", "APROVADA"))

        val viewModel = MyRequestsViewModel(repository)
        advanceUntilIdle()
        clearMocks(repository, answers = false)

        viewModel.updateFilter(MyRequestFilter.APPROVED)
        advanceUntilIdle()

        assertEquals(MyRequestFilter.APPROVED, viewModel.uiState.value.selectedFilter)
        assertEquals(listOf("req-approved"), viewModel.uiState.value.requests.map { it.id })
        coVerify(exactly = 1) { repository.listMyRequests("APROVADA") }
    }

    @Test
    fun cancelRequestShouldUpdateTheRequestAndExposeSuccessToast() = runTest {
        val repository = mockk<RequestRepository>()
        coEvery { repository.listMyRequests(null) } returns listOf(sampleRequest("req-cancel", "PENDENTE"))
        coEvery { repository.cancelRequest("req-cancel") } returns sampleRequest("req-cancel", "CANCELADA")

        val viewModel = MyRequestsViewModel(repository)
        advanceUntilIdle()

        viewModel.cancelRequest("req-cancel")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("CANCELADA", state.requests.single().status)
        assertEquals("Solicitação cancelada com sucesso.", state.toastMessage)
        assertNull(state.activeRequestId)
        assertTrue(state.errorMessage == null)
    }

    @Test
    fun reportNonReceiptShouldTrackTheRequestAndExposeSuccessToast() = runTest {
        val repository = mockk<RequestRepository>()
        val request = sampleRequest("req-report", "CONCLUIDA")

        coEvery { repository.listMyRequests(null) } returns listOf(request)
        coEvery { repository.reportNonReceipt("material-req-report", "Nao chegou") } returns
            MaterialNonReceiptReportDTO(
                id = "report-1",
                materialId = "material-req-report",
                solicitacaoId = "req-report",
                estudanteId = "student-req-report",
                reason = "Nao chegou",
                status = "OPEN"
            )

        val viewModel = MyRequestsViewModel(repository)
        advanceUntilIdle()

        viewModel.reportNonReceipt(request, "Nao chegou")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("req-report" in state.reportedRequestIds)
        assertEquals("Reporte enviado. A equipe vai revisar o caso.", state.toastMessage)
        assertNull(state.activeRequestId)
        coVerify(exactly = 1) { repository.reportNonReceipt("material-req-report", "Nao chegou") }
    }

    @Test
    fun reportNonReceiptShouldKeepLocalReportedStateWhenBackendReturnsConflict() = runTest {
        val repository = mockk<RequestRepository>()
        val request = sampleRequest("req-duplicate", "CONCLUIDA")

        coEvery { repository.listMyRequests(null) } returns listOf(request)
        coEvery { repository.reportNonReceipt("material-req-duplicate", "Tentativa repetida") } throws
            ApiException(409, "Já existe um reporte aberto para esse material.")

        val viewModel = MyRequestsViewModel(repository)
        advanceUntilIdle()

        viewModel.reportNonReceipt(request, "Tentativa repetida")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("req-duplicate" in state.reportedRequestIds)
        assertEquals("Já existe um reporte aberto para esse material.", state.toastMessage)
        assertNull(state.activeRequestId)
    }

    private fun sampleRequest(id: String, status: String): SolicitacaoDTO {
        return SolicitacaoDTO(
            id = id,
            materialId = "material-$id",
            estudanteId = "student-$id",
            status = status
        )
    }
}
