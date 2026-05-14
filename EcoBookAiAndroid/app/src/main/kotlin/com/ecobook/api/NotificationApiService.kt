package com.ecobook.api

import com.ecobook.dto.ApiEnvelopeDTO
import com.ecobook.dto.UserNotificationDTO
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface NotificationApiService {

    @GET("v1/notificacoes")
    suspend fun listNotifications(): Response<ApiEnvelopeDTO<List<UserNotificationDTO>>>

    @PATCH("v1/notificacoes/{id}/ler")
    suspend fun markAsRead(
        @Path("id") id: String
    ): Response<ApiEnvelopeDTO<Unit>>

    @PATCH("v1/notificacoes/ler-todas")
    suspend fun markAllAsRead(): Response<ApiEnvelopeDTO<Unit>>
}
