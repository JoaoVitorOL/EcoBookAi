package com.ecobook.api

import com.ecobook.dto.AuthResponseDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Headers

interface EcoBookApiClient {

    @Headers("Content-Type: application/json")
    @POST("v1/auth/register")
    suspend fun registerUser(@Body request: Map<String, String>): Response<AuthResponseDTO>

    @GET("v1/health")
    suspend fun getHealth(): Response<Map<String, Any>>
}
