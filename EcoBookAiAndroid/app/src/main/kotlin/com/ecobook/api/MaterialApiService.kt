package com.ecobook.api

import com.ecobook.dto.CreateMaterialRequestDTO
import com.ecobook.dto.GeminiResponseDTO
import com.ecobook.dto.MaterialDTO
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface MaterialApiService {

    @Multipart
    @POST("v1/materiais/preview")
    suspend fun previewMaterial(@Part file: MultipartBody.Part): Response<GeminiResponseDTO>

    @POST("v1/materiais")
    suspend fun createMaterial(@Body request: CreateMaterialRequestDTO): Response<MaterialDTO>
}
