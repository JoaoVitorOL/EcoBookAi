package com.ecobook.api

import com.ecobook.dto.AuthRequestDTO
import com.ecobook.dto.AuthResponseDTO
import com.ecobook.dto.UpdateProfileRequestDTO
import com.ecobook.dto.UsuarioDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApiService {

    @POST("v1/auth/register")
    suspend fun register(@Body request: AuthRequestDTO): Response<AuthResponseDTO>

    @GET("v1/usuarios/me")
    suspend fun getMe(): Response<UsuarioDTO>

    @PUT("v1/usuarios/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequestDTO): Response<UsuarioDTO>
}
