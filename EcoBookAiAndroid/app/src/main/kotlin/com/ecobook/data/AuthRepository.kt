package com.ecobook.data

import com.ecobook.api.AuthApiService
import com.ecobook.auth.SessionManager
import com.ecobook.dto.ApiEnvelopeDTO
import com.ecobook.dto.ApiErrorResponseDTO
import com.ecobook.dto.AuthResponseDTO
import com.ecobook.dto.DeleteAccountRequestDTO
import com.ecobook.dto.DeleteAccountResponseDTO
import com.ecobook.dto.LoginRequestDTO
import com.ecobook.dto.RegisterRequestDTO
import com.ecobook.dto.UpdateAiConsentRequestDTO
import com.ecobook.dto.UpdateProfileRequestDTO
import com.ecobook.dto.UserConsentStatusDTO
import com.ecobook.dto.UsuarioDTO
import com.ecobook.fcm.FcmTokenSyncManager
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val sessionManager: SessionManager,
    private val fcmTokenSyncManager: FcmTokenSyncManager,
    private val gson: Gson
) {

    suspend fun register(email: String, password: String, nome: String): AuthResponseDTO {
        val response = authApiService.register(
            RegisterRequestDTO(
                email = email,
                password = password,
                nome = nome
            )
        )
        val body = requireData(response)
        sessionManager.onAuthSuccess(body)
        fcmTokenSyncManager.syncCachedOrCurrentTokenAsync()
        return body
    }

    suspend fun login(email: String, password: String): AuthResponseDTO {
        val response = authApiService.login(
            LoginRequestDTO(
                email = email,
                password = password
            )
        )
        val body = requireData(response)
        sessionManager.onAuthSuccess(body)
        fcmTokenSyncManager.syncCachedOrCurrentTokenAsync()
        return body
    }

    suspend fun refreshCurrentUser(): UsuarioDTO {
        val response = authApiService.getMe()
        val body = requireData(response)
        sessionManager.onUserLoaded(body)
        return body
    }

    suspend fun fetchConsentStatus(): UserConsentStatusDTO {
        val response = authApiService.getConsentStatus()
        return requireData(response)
    }

    suspend fun updateProfile(request: UpdateProfileRequestDTO): UsuarioDTO {
        val previousEmail = sessionManager.sessionState.value.email
        val response = authApiService.updateProfile(request)
        val body = requireData(response)
        if (previousEmail.equals(body.email, ignoreCase = true)) {
            sessionManager.onUserLoaded(body)
        } else {
            sessionManager.clearSession("Email atualizado com sucesso. Entre novamente com o novo email.")
        }
        return body
    }

    suspend fun updateAiConsent(consentimentoIa: Boolean): UsuarioDTO {
        val response = if (consentimentoIa) {
            authApiService.updateAiConsent(
                UpdateAiConsentRequestDTO(consentimentoIa = true)
            )
        } else {
            authApiService.revokeAiConsent()
        }
        val body = requireData(response)
        sessionManager.onUserLoaded(body)
        return body
    }

    suspend fun deleteAccount(password: String, reason: String?): DeleteAccountResponseDTO {
        val response = authApiService.deleteAccount(
            DeleteAccountRequestDTO(
                password = password,
                reason = reason?.trim()?.ifBlank { null }
            )
        )
        val body = requireData(response)
        sessionManager.clearSession("Conta removida com sucesso.")
        return body
    }

    private fun <T> requireData(response: Response<ApiEnvelopeDTO<T>>): T {
        if (response.isSuccessful) {
            val envelope = response.body() ?: throw ApiException(response.code(), "Resposta vazia do servidor")
            return envelope.data ?: throw ApiException(response.code(), envelope.message)
        }

        val error = response.errorBody()?.string()?.takeIf { it.isNotBlank() }?.let { payload ->
            runCatching { gson.fromJson(payload, ApiErrorResponseDTO::class.java) }.getOrNull()
        }

        throw ApiException(
            statusCode = response.code(),
            message = error?.message ?: "Falha ao processar a requisição",
            fieldErrors = error?.fieldErrors ?: emptyMap()
        )
    }
}
