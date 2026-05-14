package com.ecobook.request

import com.ecobook.data.RequestRepository
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
        assertEquals("Solicitacao cancelada com sucesso.", state.toastMessage)
        assertNull(state.activeRequestId)
        assertTrue(state.errorMessage == null)
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
