package com.ecobook.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import android.util.Base64
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.ecobook.BuildConfig
import com.ecobook.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.SecureRandom

class GoogleAuthClient(context: Context) {

    companion object {
        private const val DEBUG_SHA1 = "CE:5B:16:1E:DE:81:09:EC:52:63:E3:3E:3C:98:C9:0B:E2:B9:19:6A"
        private const val GOOGLE_ACCOUNT_TYPE = "com.google"
    }

    class AddGoogleAccountRequiredException(message: String) : IllegalStateException(message)

    private val appContext = context.applicationContext
    private val credentialManager by lazy { CredentialManager.create(appContext) }
    private val secureRandom = SecureRandom()

    fun isConfigured(): Boolean {
        val clientId = resolveClientId()
        return clientId.isNotBlank() && clientId != "YOUR_WEB_CLIENT_ID_HERE"
    }

    suspend fun tryAutomaticSignIn(activityContext: Context): Result<String?> {
        if (!isConfigured()) {
            return Result.success(null)
        }

        val activity = activityContext.findActivity()
            ?: return Result.failure(
                IllegalStateException("Nao foi possivel iniciar o login porque a Activity atual nao foi encontrada.")
            )

        return runCatching {
            tryRequestAuthorizedGoogleAccount(activity)
        }.recoverCatching { error ->
            throw IllegalStateException(error.toUserMessage())
        }
    }

    suspend fun signIn(activityContext: Context): Result<String> {
        if (!isConfigured()) {
            return Result.failure(
                IllegalStateException(
                    "Configure google.oauth.clientId com o Web client ID do tipo Web application antes de usar o login."
                )
            )
        }

        val activity = activityContext.findActivity()
            ?: return Result.failure(
                IllegalStateException("Nao foi possivel iniciar o login porque a Activity atual nao foi encontrada.")
            )

        return runCatching {
            requestInteractiveGoogleCredential(activity)
        }.recoverCatching { error ->
            throw IllegalStateException(error.toUserMessage())
        }
    }

    suspend fun clearCredentialState(): Result<Unit> {
        return runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
    }

    private fun extractGoogleIdToken(credential: androidx.credentials.Credential): String {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }

        throw IllegalStateException("O Google retornou uma credencial inesperada para este login.")
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(24)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
    }

    fun openAddGoogleAccountSettings(context: Context): Result<Unit> {
        return runCatching {
            val intent = Intent(Settings.ACTION_ADD_ACCOUNT).apply {
                putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf(GOOGLE_ACCOUNT_TYPE))
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            check(intent.resolveActivity(context.packageManager) != null) {
                "Este dispositivo nao oferece uma tela para adicionar conta Google."
            }

            context.startActivity(intent)
        }
    }

    private suspend fun requestInteractiveGoogleCredential(activity: Activity): String {
        return tryRequestAnyGoogleAccount(activity)
            ?: tryRequestExplicitGoogleButton(activity)
            ?: throw AddGoogleAccountRequiredException(
                "Nenhuma Conta Google ficou disponivel para o app neste dispositivo. " +
                    "Entrar no navegador ou no app do Google nao garante isso; adicione a conta " +
                    "no Android e tente novamente."
            )
    }

    private suspend fun tryRequestAuthorizedGoogleAccount(activity: Activity): String? {
        return runCatching {
            requestGoogleIdToken(
                activity = activity,
                request = buildGoogleIdRequest(
                    filterByAuthorizedAccounts = true,
                    autoSelectEnabled = true
                )
            )
        }.getOrElse { error ->
            if (error is NoCredentialException) {
                null
            } else {
                throw error
            }
        }
    }

    private suspend fun tryRequestAnyGoogleAccount(activity: Activity): String? {
        return runCatching {
            requestGoogleIdToken(
                activity = activity,
                request = buildGoogleIdRequest(
                    filterByAuthorizedAccounts = false,
                    autoSelectEnabled = false
                )
            )
        }.getOrElse { error ->
            if (error is NoCredentialException) {
                null
            } else {
                throw error
            }
        }
    }

    private suspend fun tryRequestExplicitGoogleButton(activity: Activity): String? {
        return runCatching {
            requestGoogleIdToken(activity, buildSignInWithGoogleButtonRequest())
        }.getOrElse { error ->
            if (error is NoCredentialException) {
                null
            } else {
                throw error
            }
        }
    }

    private fun buildGoogleIdRequest(
        filterByAuthorizedAccounts: Boolean,
        autoSelectEnabled: Boolean
    ): GetCredentialRequest {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(resolveClientId())
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setAutoSelectEnabled(autoSelectEnabled)
            .setNonce(generateNonce())
            .build()

        return GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
    }

    private fun buildSignInWithGoogleButtonRequest(): GetCredentialRequest {
        val option = GetSignInWithGoogleOption.Builder(resolveClientId())
            .setNonce(generateNonce())
            .build()

        return GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
    }

    private suspend fun requestGoogleIdToken(
        activity: Activity,
        request: GetCredentialRequest
    ): String {
        val response = credentialManager.getCredential(
            context = activity,
            request = request
        )
        return extractGoogleIdToken(response.credential)
    }

    private fun resolveClientId(): String {
        val override = BuildConfig.GOOGLE_OAUTH_CLIENT_ID_OVERRIDE.trim()
        return if (override.isNotBlank()) {
            override
        } else {
            appContext.getString(R.string.google_oauth_client_id).trim()
        }
    }

    private fun Throwable.toUserMessage(): String {
        return when (this) {
            is AddGoogleAccountRequiredException -> message
                ?: "Adicione uma Conta Google ao Android deste dispositivo e tente novamente."
            is GetCredentialCancellationException -> "Login cancelado antes da confirmacao no Google."
            is NoCredentialException -> "Nenhuma conta Google elegivel foi encontrada no Android deste dispositivo."
            is GetCredentialException -> credentialManagerMessage(message)
            else -> message ?: "Falha inesperada durante o login com Google."
        }
    }

    private fun credentialManagerMessage(message: String?): String {
        val rawMessage = message?.trim().orEmpty()
        if (rawMessage.contains("28444") ||
            rawMessage.contains("Developer console is not set up correctly", ignoreCase = true)
        ) {
            return buildString {
                append("O Google recusou o login porque a configuracao OAuth ainda esta inconsistente. ")
                append("O valor de google.oauth.clientId no app precisa ser o Web client ID do tipo Web application, ")
                append("nao o client Android usado para package/SHA-1. ")
                append("No Google Cloud, mantenha tambem um client Android para package ")
                append(BuildConfig.APPLICATION_ID)
                append(" com SHA-1 ")
                append(DEBUG_SHA1)
                append(". Depois use no app e no backend exatamente o mesmo Web client ID do projeto.")
            }
        }

        return rawMessage.ifBlank { "Nao foi possivel concluir o login com Google." }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
