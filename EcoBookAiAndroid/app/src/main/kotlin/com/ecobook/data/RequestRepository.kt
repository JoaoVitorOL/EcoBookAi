package com.ecobook.data

import com.ecobook.api.RequestApiService
import com.ecobook.dto.ApiEnvelopeDTO
import com.ecobook.dto.ApiErrorResponseDTO
import com.ecobook.dto.CreateNonReceiptReportRequestDTO
import com.ecobook.dto.MaterialNonReceiptReportDTO
import com.ecobook.dto.SolicitacaoDTO
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class RequestRepository @Inject constructor(
    private val requestApiService: RequestApiService,
    private val gson: Gson
) {

    suspend fun createRequest(materialId: String): SolicitacaoDTO {
        return requireData(requestApiService.createRequest(materialId))
    }

    suspend fun listMyRequests(status: String? = null): List<SolicitacaoDTO> {
        return requireData(requestApiService.listMyRequests(status))
    }

    suspend fun getRequest(id: String): SolicitacaoDTO {
        return requireData(requestApiService.getRequest(id))
    }

    suspend fun listPendingRequestsForDonor(): List<SolicitacaoDTO> {
        return requireData(requestApiService.listPendingRequestsForDonor())
    }

    suspend fun listApprovedRequestsForDonor(): List<SolicitacaoDTO> {
        return requireData(requestApiService.listApprovedRequestsForDonor())
    }

    suspend fun approveRequest(id: String): SolicitacaoDTO {
        return requireData(requestApiService.approveRequest(id))
    }

    suspend fun declineRequest(id: String): SolicitacaoDTO {
        return requireData(requestApiService.declineRequest(id))
    }

    suspend fun cancelRequest(id: String): SolicitacaoDTO {
        return requireData(requestApiService.cancelRequest(id))
    }

    suspend fun completeDonation(id: String): SolicitacaoDTO {
        return requireData(requestApiService.completeDonation(id))
    }

    suspend fun reportNonReceipt(materialId: String, reason: String? = null): MaterialNonReceiptReportDTO {
        return requireData(
            requestApiService.reportNonReceipt(
                materialId,
                CreateNonReceiptReportRequestDTO(reason = reason)
            )
        )
    }

    private fun <T> requireData(response: Response<ApiEnvelopeDTO<T>>): T {
        if (response.isSuccessful) {
            val envelope = response.body() ?: throw ApiException(response.code(), "Resposta vazia do servidor")
            return envelope.data ?: throw ApiException(response.code(), envelope.message)
        }

        val error = response.errorBody()?.string()?.takeIf { it.isNotBlank() }?.let { payload ->
            runCatching { gson.fromJson(payload, ApiErrorResponseDTO::class.java) }.getOrNull()
        }

        throw ApiException(
            statusCode = response.code(),
            message = error?.message ?: "Falha ao processar a requisição",
            fieldErrors = error?.fieldErrors ?: emptyMap()
        )
    }
}
