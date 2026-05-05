package com.ecobook.api

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class MockApiInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val body = when (request.url.encodedPath) {
            "/api/v1/health", "/v1/health" -> HEALTH_RESPONSE
            else -> """{"error":"NOT_FOUND"}"""
        }

        val code = if (body == HEALTH_RESPONSE) 200 else 404

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Not Found")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private companion object {
        const val HEALTH_RESPONSE =
            """{"status":"UP","application":"EcoBook IA Backend","version":"test","timestamp":"2026-05-04T00:00:00"}"""
    }
}
