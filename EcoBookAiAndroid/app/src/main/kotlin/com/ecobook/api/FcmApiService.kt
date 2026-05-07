package com.ecobook.api

import com.ecobook.dto.ApiEnvelopeDTO
import com.ecobook.dto.FcmTokenRequestDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface FcmApiService {

    @POST("v1/fcm/tokens")
    suspend fun registerToken(@Body request: FcmTokenRequestDTO): Response<ApiEnvelopeDTO<Unit>>
}
