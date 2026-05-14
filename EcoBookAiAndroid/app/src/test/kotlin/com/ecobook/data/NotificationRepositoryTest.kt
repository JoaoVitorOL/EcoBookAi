package com.ecobook.data

import com.ecobook.api.NotificationApiService
import com.ecobook.dto.ApiEnvelopeDTO
import com.ecobook.dto.UserNotificationDTO
import com.ecobook.navigation.AppRoutes
import com.google.gson.GsonBuilder
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class NotificationRepositoryTest {

    @Test
    fun listNotificationsShouldTolerateLegacyNullFieldsAndFallbackRoutes() = runTest {
        val apiService = object : NotificationApiService {
            override suspend fun listNotifications(): Response<ApiEnvelopeDTO<List<UserNotificationDTO>>> {
                return Response.success(
                    ApiEnvelopeDTO(
                        status = 200,
                        message = "ok",
                        timestamp = "2026-05-14T10:00:00",
                        path = "/api/v1/notificacoes",
                        data = listOf(
                            UserNotificationDTO(
                                id = null,
                                title = null,
                                body = null,
                                notificationType = "SOLICITACAO_APROVADA",
                                route = null,
                                requestId = "request-1",
                                materialId = "material-1",
                                receivedAt = null,
                                unread = null
                            )
                        )
                    )
                )
            }

            override suspend fun markAsRead(id: String): Response<ApiEnvelopeDTO<Unit>> {
                return Response.success(ApiEnvelopeDTO(200, "ok", "2026-05-14T10:00:00", "", Unit))
            }

            override suspend fun markAllAsRead(): Response<ApiEnvelopeDTO<Unit>> {
                return Response.success(ApiEnvelopeDTO(200, "ok", "2026-05-14T10:00:00", "", Unit))
            }
        }

        val repository = NotificationRepository(apiService, GsonBuilder().create())

        val notifications = repository.listNotifications()

        assertEquals(1, notifications.size)
        assertEquals("Solicitacao aprovada", notifications.single().title)
        assertEquals("Sua solicitacao foi aprovada.", notifications.single().body)
        assertEquals(AppRoutes.MY_REQUESTS, notifications.single().route)
        assertTrue(notifications.single().unread)
        assertTrue(notifications.single().receivedAtEpochMillis > 0)
        assertTrue(notifications.single().id.isNotBlank())
    }

    @Test
    fun listNotificationsShouldSurfaceBackendApiErrors() = runTest {
        val apiService = object : NotificationApiService {
            override suspend fun listNotifications(): Response<ApiEnvelopeDTO<List<UserNotificationDTO>>> {
                return Response.error(
                    403,
                    """{"error":"FORBIDDEN","message":"Sem acesso"}"""
                        .toResponseBody("application/json".toMediaType())
                )
            }

            override suspend fun markAsRead(id: String): Response<ApiEnvelopeDTO<Unit>> {
                return Response.success(ApiEnvelopeDTO(200, "ok", "2026-05-14T10:00:00", "", Unit))
            }

            override suspend fun markAllAsRead(): Response<ApiEnvelopeDTO<Unit>> {
                return Response.success(ApiEnvelopeDTO(200, "ok", "2026-05-14T10:00:00", "", Unit))
            }
        }

        val repository = NotificationRepository(apiService, GsonBuilder().create())

        try {
            repository.listNotifications()
        } catch (error: ApiException) {
            assertEquals(403, error.statusCode)
            assertEquals("Sem acesso", error.message)
            return@runTest
        }

        error("Expected ApiException to be thrown")
    }

    @Test
    fun listNotificationsShouldHideReadNotificationsFromCenter() = runTest {
        val apiService = object : NotificationApiService {
            override suspend fun listNotifications(): Response<ApiEnvelopeDTO<List<UserNotificationDTO>>> {
                return Response.success(
                    ApiEnvelopeDTO(
                        status = 200,
                        message = "ok",
                        timestamp = "2026-05-14T10:00:00",
                        path = "/api/v1/notificacoes",
                        data = listOf(
                            UserNotificationDTO(
                                id = "notif-read",
                                title = "Ja lida",
                                body = "Nao deve aparecer",
                                notificationType = "SOLICITACAO_APROVADA",
                                route = AppRoutes.MY_REQUESTS,
                                requestId = "request-read",
                                materialId = "material-read",
                                receivedAt = "2026-05-14T09:00:00",
                                unread = false
                            ),
                            UserNotificationDTO(
                                id = "notif-unread",
                                title = "Pendente",
                                body = "Deve continuar visivel",
                                notificationType = "SOLICITACAO_RECEBIDA",
                                route = AppRoutes.DONOR_REQUESTS,
                                requestId = "request-unread",
                                materialId = "material-unread",
                                receivedAt = "2026-05-14T10:00:00",
                                unread = true
                            )
                        )
                    )
                )
            }

            override suspend fun markAsRead(id: String): Response<ApiEnvelopeDTO<Unit>> {
                return Response.success(ApiEnvelopeDTO(200, "ok", "2026-05-14T10:00:00", "", Unit))
            }

            override suspend fun markAllAsRead(): Response<ApiEnvelopeDTO<Unit>> {
                return Response.success(ApiEnvelopeDTO(200, "ok", "2026-05-14T10:00:00", "", Unit))
            }
        }

        val repository = NotificationRepository(apiService, GsonBuilder().create())

        val notifications = repository.listNotifications()

        assertEquals(1, notifications.size)
        assertEquals("notif-unread", notifications.single().id)
        assertEquals("Pendente", notifications.single().title)
    }
}
