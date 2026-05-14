package com.ecobook.api

import com.ecobook.dto.ApiEnvelopeDTO
import com.ecobook.dto.SolicitacaoDTO
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RequestApiService {

    @POST("v1/materiais/{materialId}/solicitacoes")
    suspend fun createRequest(
        @Path("materialId") materialId: String
    ): Response<ApiEnvelopeDTO<SolicitacaoDTO>>

    @GET("v1/solicitacoes/minhas")
    suspend fun listMyRequests(
        @Query("status") status: String? = null
    ): Response<ApiEnvelopeDTO<List<SolicitacaoDTO>>>

    @GET("v1/solicitacoes/{id}")
    suspend fun getRequest(
        @Path("id") id: String
    ): Response<ApiEnvelopeDTO<SolicitacaoDTO>>

    @GET("v1/solicitacoes/pendentes")
    suspend fun listPendingRequestsForDonor(): Response<ApiEnvelopeDTO<List<SolicitacaoDTO>>>

    @GET("v1/solicitacoes/aprovadas")
    suspend fun listApprovedRequestsForDonor(): Response<ApiEnvelopeDTO<List<SolicitacaoDTO>>>

    @PATCH("v1/solicitacoes/{id}/aprovar")
    suspend fun approveRequest(
        @Path("id") id: String
    ): Response<ApiEnvelopeDTO<SolicitacaoDTO>>

    @PATCH("v1/solicitacoes/{id}/recusar")
    suspend fun declineRequest(
        @Path("id") id: String
    ): Response<ApiEnvelopeDTO<SolicitacaoDTO>>

    @PATCH("v1/solicitacoes/{id}/cancelar")
    suspend fun cancelRequest(
        @Path("id") id: String
    ): Response<ApiEnvelopeDTO<SolicitacaoDTO>>

    @PATCH("v1/solicitacoes/{id}/concluir")
    suspend fun completeDonation(
        @Path("id") id: String
    ): Response<ApiEnvelopeDTO<SolicitacaoDTO>>
}
