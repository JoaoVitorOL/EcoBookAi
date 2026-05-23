package com.ecobook.api

import com.ecobook.dto.ApiEnvelopeDTO
import com.ecobook.dto.CreateMaterialRequestDTO
import com.ecobook.dto.GeminiResponseDTO
import com.ecobook.dto.MaterialDTO
import com.ecobook.dto.PagedResponseDTO
import com.ecobook.dto.UpdateMaterialRequestDTO
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface MaterialApiService {

    @GET("v1/materiais")
    suspend fun searchMaterials(
        @Query("query") query: String? = null,
        @Query("disciplina") disciplina: String? = null,
        @Query("nivel_ensino") nivelEnsino: String? = null,
        @Query("ano") ano: Int? = null,
        @Query("sistema_ensino") sistemaEnsino: String? = null,
        @Query("cidade") cidade: String? = null,
        @Query("bairro") bairro: String? = null,
        @Query("min_ano_publicacao") minAnoPublicacao: Int? = null,
        @Query("max_ano_publicacao") maxAnoPublicacao: Int? = null,
        @Query("after_id") afterId: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiEnvelopeDTO<PagedResponseDTO<MaterialDTO>>>

    @Multipart
    @POST("v1/materiais/preview")
    suspend fun previewMaterial(
        @Part fileFront: MultipartBody.Part,
        @Part fileBack: MultipartBody.Part? = null
    ): Response<ApiEnvelopeDTO<GeminiResponseDTO>>

    @GET("v1/materiais/me")
    suspend fun listCurrentUserMaterials(): Response<ApiEnvelopeDTO<List<MaterialDTO>>>

    @POST("v1/materiais")
    suspend fun createMaterial(@Body request: CreateMaterialRequestDTO): Response<ApiEnvelopeDTO<MaterialDTO>>

    @PUT("v1/materiais/{id}")
    suspend fun updateMaterial(
        @Path("id") id: String,
        @Body request: UpdateMaterialRequestDTO
    ): Response<ApiEnvelopeDTO<MaterialDTO>>

    @DELETE("v1/materiais/{id}")
    suspend fun deleteMaterial(@Path("id") id: String): Response<Unit>
}
