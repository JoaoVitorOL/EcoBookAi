package com.ecobook.auth

import com.ecobook.data.ApiException
import com.ecobook.data.AuthRepository
import com.ecobook.dto.AuthResponseDTO
import com.ecobook.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.net.SocketTimeoutException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun submitInRegisterModeShouldExposeLocalValidationErrorsBeforeCallingRepository() = runTest {
        val repository = mockk<AuthRepository>(relaxed = true)
        val viewModel = AuthViewModel(repository)

        viewModel.setMode(AuthMode.REGISTER)
        viewModel.submit()

        val state = viewModel.uiState.value
        assertEquals("Informe seu nome para criar a conta.", state.fieldErrors["nome"])
        assertEquals("Informe seu email.", state.fieldErrors["email"])
        assertEquals("Informe sua senha.", state.fieldErrors["password"])
        assertEquals("Confirme sua senha.", state.fieldErrors["confirm_password"])
        assertNull(state.errorMessage)
        assertFalse(state.isLoading)

        coVerify(exactly = 0) { repository.register(any(), any(), any()) }
        coVerify(exactly = 0) { repository.login(any(), any()) }
    }

    @Test
    fun loginSuccessShouldClearSensitiveFieldsAndErrors() = runTest {
        val repository = mockk<AuthRepository>()
        coEvery {
            repository.login("user@example.com", "SenhaSegura123")
        } returns sampleAuthResponse(email = "user@example.com", nome = "User")

        val viewModel = AuthViewModel(repository)
        viewModel.updateEmail("  user@example.com  ")
        viewModel.updatePassword("SenhaSegura123")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
        assertNull(state.errorMessage)
        assertTrue(state.fieldErrors.isEmpty())

        coVerify(exactly = 1) { repository.login("user@example.com", "SenhaSegura123") }
    }

    @Test
    fun registerConflictShouldExposeFriendlyMessageAndEmailFieldError() = runTest {
        val repository = mockk<AuthRepository>()
        coEvery {
            repository.register("new@example.com", "SenhaSegura123", "Novo Usuario")
        } throws ApiException(
            statusCode = 409,
            message = "Este email já está cadastrado",
            fieldErrors = emptyMap()
        )

        val viewModel = AuthViewModel(repository)
        viewModel.setMode(AuthMode.REGISTER)
        viewModel.updateNome("Novo Usuario")
        viewModel.updateEmail("new@example.com")
        viewModel.updatePassword("SenhaSegura123")
        viewModel.updateConfirmPassword("SenhaSegura123")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.errorMessage?.contains("Este email já está cadastrado") == true)
        assertEquals("Este email já está cadastrado.", state.fieldErrors["email"])

        coVerify(exactly = 1) {
            repository.register("new@example.com", "SenhaSegura123", "Novo Usuario")
        }
    }

    @Test
    fun loginNetworkFailureShouldExposeBackendConnectionGuidance() = runTest {
        val repository = mockk<AuthRepository>()
        coEvery {
            repository.login("offline@example.com", "SenhaSegura123")
        } throws SocketTimeoutException("timeout")

        val viewModel = AuthViewModel(repository)
        viewModel.updateEmail("offline@example.com")
        viewModel.updatePassword("SenhaSegura123")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(
            state.errorMessage?.contains("Não foi possível conectar ao backend configurado no app") == true
        )
        assertTrue(state.fieldErrors.isEmpty())

        coVerify(exactly = 1) { repository.login("offline@example.com", "SenhaSegura123") }
    }

    private fun sampleAuthResponse(email: String, nome: String): AuthResponseDTO {
        return AuthResponseDTO(
            id = "user-123",
            email = email,
            nome = nome,
            perfilCompleto = false,
            consentimentoIa = false,
            role = "USER",
            token = "jwt-token",
            expiresIn = 604800
        )
    }
}
