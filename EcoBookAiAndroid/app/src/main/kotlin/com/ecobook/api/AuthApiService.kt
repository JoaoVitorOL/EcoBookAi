package com.ecobook.api

import com.ecobook.dto.ApiEnvelopeDTO
import com.ecobook.dto.AuthResponseDTO
import com.ecobook.dto.DeleteAccountRequestDTO
import com.ecobook.dto.DeleteAccountResponseDTO
import com.ecobook.dto.LoginRequestDTO
import com.ecobook.dto.RegisterRequestDTO
import com.ecobook.dto.UserConsentStatusDTO
import com.ecobook.dto.UpdateAiConsentRequestDTO
import com.ecobook.dto.UpdateProfileRequestDTO
import com.ecobook.dto.UsuarioDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApiService {

    @POST("v1/auth/register")
    suspend fun register(@Body request: RegisterRequestDTO): Response<ApiEnvelopeDTO<AuthResponseDTO>>

    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequestDTO): Response<ApiEnvelopeDTO<AuthResponseDTO>>

    @GET("v1/usuarios/me")
    suspend fun getMe(): Response<ApiEnvelopeDTO<UsuarioDTO>>

    @GET("v1/usuarios/me/consent")
    suspend fun getConsentStatus(): Response<ApiEnvelopeDTO<UserConsentStatusDTO>>

    @PUT("v1/usuarios/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequestDTO): Response<ApiEnvelopeDTO<UsuarioDTO>>

    @PATCH("v1/usuarios/me/consent")
    suspend fun updateAiConsent(@Body request: UpdateAiConsentRequestDTO): Response<ApiEnvelopeDTO<UsuarioDTO>>

    @DELETE("v1/usuarios/me/consent/ai-classification")
    suspend fun revokeAiConsent(): Response<ApiEnvelopeDTO<UsuarioDTO>>

    @POST("v1/usuarios/delete")
    suspend fun deleteAccount(@Body request: DeleteAccountRequestDTO): Response<ApiEnvelopeDTO<DeleteAccountResponseDTO>>
}
