package com.ecobook.api

import com.ecobook.auth.SessionManager
import com.ecobook.utils.SecureStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val secureStorage: SecureStorage,
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = secureStorage.getToken()
        val finalRequest = if (token.isNullOrBlank()) {
            request
        } else {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }

        val response = chain.proceed(finalRequest)
        if (response.code == 401) {
            sessionManager.clearSession("Sua sessão expirou. Entre novamente para continuar.")
        }
        return response
    }
}
