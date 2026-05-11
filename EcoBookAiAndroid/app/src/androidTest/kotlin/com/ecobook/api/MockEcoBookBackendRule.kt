package com.ecobook.api

import androidx.test.platform.app.InstrumentationRegistry
import com.ecobook.utils.SecureStorage
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class MockEcoBookBackendRule : TestWatcher() {

    companion object {
        const val PORT = 8765
    }

    private val server = MockWebServer()
    private val requests = CopyOnWriteArrayList<RecordedRequest>()
    private val targetContext by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext
    }

    override fun starting(description: Description) {
        requests.clear()
        SecureStorage(targetContext).clear()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request

                return when (request.path.normalizedPath()) {
                    "/api/v1/health", "/v1/health" -> jsonResponse(200, healthResponse())
                    "/api/v1/auth/register", "/v1/auth/register" -> jsonResponse(201, registerResponse())
                    "/api/v1/auth/login", "/v1/auth/login" -> jsonResponse(200, loginResponse())
                    "/api/v1/usuarios/me", "/v1/usuarios/me" -> when (request.method) {
                        "GET" -> jsonResponse(200, currentUserResponse())
                        "PUT" -> jsonResponse(200, completedProfileResponse())
                        "PATCH" -> jsonResponse(200, completedProfileResponse())
                        else -> notFoundResponse(request)
                    }

                    "/api/v1/fcm/tokens", "/v1/fcm/tokens" -> jsonResponse(200, emptyEnvelope("/api/v1/fcm/tokens"))
                    else -> notFoundResponse(request)
                }
            }
        }

        server.start(PORT)
    }

    override fun finished(description: Description) {
        SecureStorage(targetContext).clear()
        runCatching { server.shutdown() }
        requests.clear()
    }

    fun awaitRequest(path: String, timeoutMillis: Long = 5_000): RecordedRequest {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (System.nanoTime() < deadline) {
            requests.firstOrNull { it.path.normalizedPath() == path }?.let { return it }
            Thread.sleep(50)
        }
        error("Timed out waiting for request to $path. Paths seen: ${requests.mapNotNull { it.path.normalizedPath() }}")
    }

    private fun notFoundResponse(request: RecordedRequest): MockResponse {
        val path = request.path.normalizedPath() ?: "/desconhecido"
        return jsonResponse(
            404,
            """{"status":404,"message":"Mock route not found","timestamp":"2026-05-11T00:00:00","path":"$path","data":null}"""
        )
    }

    private fun jsonResponse(code: Int, body: String): MockResponse {
        return MockResponse()
            .setResponseCode(code)
            .addHeader("Content-Type", "application/json")
            .setBody(body)
    }

    private fun emptyEnvelope(path: String): String {
        return """{"status":200,"message":"ok","timestamp":"2026-05-11T00:00:00","path":"$path","data":null}"""
    }

    private fun healthResponse(): String {
        return """{"status":200,"message":"Backend online","timestamp":"2026-05-11T00:00:00","path":"/api/v1/health","data":{"status":"UP","application":"EcoBook IA Backend","version":"instrumented-test","timestamp":"2026-05-11T00:00:00"}}"""
    }

    private fun registerResponse(): String {
        return """{"status":201,"message":"Conta criada com sucesso","timestamp":"2026-05-11T00:00:00","path":"/api/v1/auth/register","data":{"id":"user-123","email":"e2e@example.com","nome":"Teste E2E","whatsapp":null,"cidade":null,"bairro":null,"instituicao":null,"perfil_completo":false,"consentimento_ia":false,"role":"USER","token":"jwt-e2e-token","expires_in":604800}}"""
    }

    private fun loginResponse(): String {
        return """{"status":200,"message":"Login realizado com sucesso","timestamp":"2026-05-11T00:00:00","path":"/api/v1/auth/login","data":{"id":"user-123","email":"e2e@example.com","nome":"Teste E2E","whatsapp":"+5548999999999","cidade":"Florianopolis","bairro":"Centro","instituicao":"UFSC","perfil_completo":true,"consentimento_ia":true,"role":"USER","token":"jwt-e2e-token","expires_in":604800}}"""
    }

    private fun currentUserResponse(): String {
        return """{"status":200,"message":"Perfil carregado","timestamp":"2026-05-11T00:00:00","path":"/api/v1/usuarios/me","data":{"id":"user-123","email":"e2e@example.com","nome":"Teste E2E","whatsapp":"+5548999999999","cidade":"Florianopolis","bairro":"Centro","instituicao":"UFSC","perfil_completo":true,"consentimento_ia":true,"role":"USER"}}"""
    }

    private fun completedProfileResponse(): String {
        return """{"status":200,"message":"Perfil atualizado com sucesso","timestamp":"2026-05-11T00:00:00","path":"/api/v1/usuarios/me","data":{"id":"user-123","email":"e2e@example.com","nome":"Teste E2E","whatsapp":"+5548999999999","cidade":"Florianopolis","bairro":"Centro","instituicao":"UFSC","perfil_completo":true,"consentimento_ia":true,"role":"USER"}}"""
    }
}

private fun String?.normalizedPath(): String? = this?.substringBefore('?')
