package com.ecobook.api

import com.ecobook.dto.ApiEnvelopeDTO
import com.ecobook.dto.HealthResponseDTO
import retrofit2.http.GET

interface EcoBookApiClient {

    @GET("v1/health")
    suspend fun getHealth(): ApiEnvelopeDTO<HealthResponseDTO>
}
