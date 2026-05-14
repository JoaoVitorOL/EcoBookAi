package com.ecobook.data

import com.ecobook.api.NotificationApiService
import com.ecobook.dto.ApiEnvelopeDTO
import com.ecobook.dto.ApiErrorResponseDTO
import com.ecobook.dto.UserNotificationDTO
import com.ecobook.fcm.NotificationInboxEntry
import com.google.gson.Gson
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationApiService: NotificationApiService,
    private val gson: Gson
) {

    suspend fun listNotifications(): List<NotificationInboxEntry> {
        return requireData(notificationApiService.listNotifications())
            .map(::toInboxEntry)
            .sortedByDescending(NotificationInboxEntry::receivedAtEpochMillis)
    }

    suspend fun markAsRead(id: String) {
        requireData(notificationApiService.markAsRead(id))
    }

    suspend fun markAllAsRead() {
        requireData(notificationApiService.markAllAsRead())
    }

    private fun toInboxEntry(notification: UserNotificationDTO): NotificationInboxEntry {
        return NotificationInboxEntry(
            id = notification.id,
            title = notification.title,
            body = notification.body,
            notificationType = notification.notificationType,
            route = notification.route,
            requestId = notification.requestId,
            materialId = notification.materialId,
            receivedAtEpochMillis = LocalDateTime.parse(
                notification.receivedAt,
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
            ).atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            unread = notification.unread
        )
    }

    private fun <T> requireData(response: Response<ApiEnvelopeDTO<T>>): T {
        if (response.isSuccessful) {
            val envelope = response.body() ?: throw ApiException(response.code(), "Resposta vazia do servidor")
            @Suppress("UNCHECKED_CAST")
            return (envelope.data ?: Unit) as T
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
