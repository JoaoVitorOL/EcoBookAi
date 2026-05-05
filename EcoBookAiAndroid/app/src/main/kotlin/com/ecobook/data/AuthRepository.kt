package com.ecobook.data

import com.ecobook.api.AuthApiService
import com.ecobook.auth.SessionManager
import com.ecobook.dto.ApiErrorResponseDTO
import com.ecobook.dto.AuthRequestDTO
import com.ecobook.dto.AuthResponseDTO
import com.ecobook.dto.UpdateProfileRequestDTO
import com.ecobook.dto.UsuarioDTO
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val sessionManager: SessionManager,
    private val gson: Gson
) {

    suspend fun registerWithGoogleToken(googleToken: String): AuthResponseDTO {
        val response = authApiService.register(AuthRequestDTO(googleToken))
        val body = requireBody(response)
        sessionManager.onAuthSuccess(body)
        return body
    }

    suspend fun refreshCurrentUser(): UsuarioDTO {
        val response = authApiService.getMe()
        val body = requireBody(response)
        sessionManager.onUserLoaded(body)
        return body
    }

    suspend fun updateProfile(request: UpdateProfileRequestDTO): UsuarioDTO {
        val response = authApiService.updateProfile(request)
        val body = requireBody(response)
        sessionManager.onUserLoaded(body)
        return body
    }

    private fun <T> requireBody(response: Response<T>): T {
        if (response.isSuccessful) {
            return response.body() ?: throw ApiException(response.code(), "Resposta vazia do servidor")
        }

        val error = response.errorBody()?.string()?.takeIf { it.isNotBlank() }?.let { payload ->
            runCatching { gson.fromJson(payload, ApiErrorResponseDTO::class.java) }.getOrNull()
        }

        throw ApiException(
            statusCode = response.code(),
            message = error?.message ?: "Falha ao processar a requisicao",
            fieldErrors = error?.fieldErrors ?: emptyMap()
        )
    }
}
