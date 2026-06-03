package com.ecobook.data

import com.ecobook.api.AuthApiService
import com.ecobook.auth.SessionManager
import com.ecobook.dto.ApiEnvelopeDTO
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
import com.ecobook.material.PreparedImage
import com.google.gson.GsonBuilder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class AuthRepositoryTest {

    @Test
    fun uploadProfilePhotoShouldReturnUpdatedUserAndRefreshSession() = runTest {
        val updatedUser = UsuarioDTO(
            id = "user-1",
            email = "adulto@example.com",
            nome = "Adulto Responsavel",
            whatsapp = "+5511991234567",
            cpf = "52998224725",
            cidade = "FLORIANOPOLIS",
            bairro = "CENTRO",
            instituicao = "Escola EcoBook",
            fotoPerfilUrl = "/api/v1/usuarios/user-1/foto-perfil",
            perfilCompleto = true,
            consentimentoIa = true
        )
        val sessionManager = relaxedSessionManager()
        val apiService = FakeAuthApiService(
            uploadProfilePhotoResponse = Response.success(
                ApiEnvelopeDTO(
                    status = 200,
                    message = "ok",
                    timestamp = "2026-05-31T18:00:00",
                    path = "/api/v1/usuarios/me/foto-perfil",
                    data = updatedUser
                )
            )
        )
        val repository = AuthRepository(
            authApiService = apiService,
            sessionManager = sessionManager,
            fcmTokenSyncManager = relaxedFcmTokenSyncManager(),
            gson = GsonBuilder().create()
        )

        val result = repository.uploadProfilePhoto(
            PreparedImage(
                fileName = "profile-photo.jpg",
                mimeType = "image/jpeg",
                bytes = byteArrayOf(1, 2, 3)
            )
        )

        assertEquals("/api/v1/usuarios/user-1/foto-perfil", result.fotoPerfilUrl)
        verify(exactly = 1) { sessionManager.onUserLoaded(updatedUser) }
    }

    @Test
    fun uploadProfilePhotoShouldSurfaceBackendApiErrors() = runTest {
        val sessionManager = relaxedSessionManager()
        val apiService = FakeAuthApiService(
            uploadProfilePhotoResponse = Response.error(
                422,
                """{"error":"UNPROCESSABLE_ENTITY","message":"Imagem inválida","fieldErrors":{"image":"Selecione JPG ou PNG"}}"""
                    .toResponseBody("application/json".toMediaType())
            )
        )
        val repository = AuthRepository(
            authApiService = apiService,
            sessionManager = sessionManager,
            fcmTokenSyncManager = relaxedFcmTokenSyncManager(),
            gson = GsonBuilder().create()
        )

        try {
            repository.uploadProfilePhoto(
                PreparedImage(
                    fileName = "profile-photo.webp",
                    mimeType = "image/webp",
                    bytes = byteArrayOf(9, 8, 7)
                )
            )
        } catch (error: ApiException) {
            assertEquals(422, error.statusCode)
            assertEquals("Imagem inválida", error.message)
            assertEquals("Selecione JPG ou PNG", error.fieldErrors["image"])
            verify(exactly = 0) { sessionManager.onUserLoaded(any()) }
            return@runTest
        }

        error("Expected ApiException to be thrown")
    }

    private fun relaxedSessionManager(): SessionManager {
        return mockk(relaxed = true)
    }

    private fun relaxedFcmTokenSyncManager(): FcmTokenSyncManager {
        return mockk(relaxed = true)
    }
}

private class FakeAuthApiService(
    private val uploadProfilePhotoResponse: Response<ApiEnvelopeDTO<UsuarioDTO>>
) : AuthApiService {

    override suspend fun register(request: RegisterRequestDTO): Response<ApiEnvelopeDTO<AuthResponseDTO>> {
        error("Not used in this test")
    }

    override suspend fun login(request: LoginRequestDTO): Response<ApiEnvelopeDTO<AuthResponseDTO>> {
        error("Not used in this test")
    }

    override suspend fun getMe(): Response<ApiEnvelopeDTO<UsuarioDTO>> {
        error("Not used in this test")
    }

    override suspend fun getConsentStatus(): Response<ApiEnvelopeDTO<UserConsentStatusDTO>> {
        error("Not used in this test")
    }

    override suspend fun updateProfile(request: UpdateProfileRequestDTO): Response<ApiEnvelopeDTO<UsuarioDTO>> {
        error("Not used in this test")
    }

    override suspend fun uploadProfilePhoto(image: MultipartBody.Part): Response<ApiEnvelopeDTO<UsuarioDTO>> {
        return uploadProfilePhotoResponse
    }

    override suspend fun updateAiConsent(request: UpdateAiConsentRequestDTO): Response<ApiEnvelopeDTO<UsuarioDTO>> {
        error("Not used in this test")
    }

    override suspend fun revokeAiConsent(): Response<ApiEnvelopeDTO<UsuarioDTO>> {
        error("Not used in this test")
    }

    override suspend fun deleteAccount(request: DeleteAccountRequestDTO): Response<ApiEnvelopeDTO<DeleteAccountResponseDTO>> {
        error("Not used in this test")
    }
}
