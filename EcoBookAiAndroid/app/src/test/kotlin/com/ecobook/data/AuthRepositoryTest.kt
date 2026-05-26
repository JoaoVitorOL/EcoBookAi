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
import com.google.gson.GsonBuilder
import kotlinx.coroutines.test.runTest
import okhttp3.Headers.Companion.headersOf
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class AuthRepositoryTest {

    @Test
    fun exportPersonalDataShouldReadFilenameAndBytesFromBinaryResponse() = runTest {
        val zipBytes = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
        val apiService = FakeAuthApiService(
            exportResponse = Response.success(
                zipBytes.toResponseBody("application/zip".toMediaType()),
                headersOf("Content-Disposition", "attachment; filename=\"ecobook-export.zip\"")
            )
        )
        val repository = AuthRepository(
            authApiService = apiService,
            sessionManager = relaxedSessionManager(),
            fcmTokenSyncManager = relaxedFcmTokenSyncManager(),
            gson = GsonBuilder().create()
        )

        val exportFile = repository.exportPersonalData()

        assertEquals("ecobook-export.zip", exportFile.fileName)
        assertArrayEquals(zipBytes, exportFile.bytes)
    }

    @Test
    fun exportPersonalDataShouldSurfaceBackendApiErrors() = runTest {
        val apiService = FakeAuthApiService(
            exportResponse = Response.error(
                403,
                """{"error":"FORBIDDEN","message":"Sem permissao para exportar"}"""
                    .toResponseBody("application/json".toMediaType())
            )
        )
        val repository = AuthRepository(
            authApiService = apiService,
            sessionManager = relaxedSessionManager(),
            fcmTokenSyncManager = relaxedFcmTokenSyncManager(),
            gson = GsonBuilder().create()
        )

        try {
            repository.exportPersonalData()
        } catch (error: ApiException) {
            assertEquals(403, error.statusCode)
            assertEquals("Sem permissao para exportar", error.message)
            return@runTest
        }

        error("Expected ApiException to be thrown")
    }

    private fun relaxedSessionManager(): SessionManager {
        return io.mockk.mockk(relaxed = true)
    }

    private fun relaxedFcmTokenSyncManager(): FcmTokenSyncManager {
        return io.mockk.mockk(relaxed = true)
    }
}

private class FakeAuthApiService(
    private val exportResponse: Response<ResponseBody>
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

    override suspend fun updateAiConsent(request: UpdateAiConsentRequestDTO): Response<ApiEnvelopeDTO<UsuarioDTO>> {
        error("Not used in this test")
    }

    override suspend fun revokeAiConsent(): Response<ApiEnvelopeDTO<UsuarioDTO>> {
        error("Not used in this test")
    }

    override suspend fun deleteAccount(request: DeleteAccountRequestDTO): Response<ApiEnvelopeDTO<DeleteAccountResponseDTO>> {
        error("Not used in this test")
    }

    override suspend fun exportPersonalData(): Response<ResponseBody> = exportResponse
}
