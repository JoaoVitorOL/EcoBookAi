package com.ecobook.data

import com.ecobook.api.NotificationApiService
import com.ecobook.dto.ApiEnvelopeDTO
import com.ecobook.dto.ApiErrorResponseDTO
import com.ecobook.dto.UserNotificationDTO
import com.ecobook.fcm.NotificationInboxEntry
import com.ecobook.navigation.AppRoutes
import com.google.gson.Gson
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.LinkedHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import retrofit2.Response
import timber.log.Timber

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationApiService: NotificationApiService,
    private val gson: Gson
) {

    suspend fun listNotifications(): List<NotificationInboxEntry> {
        val response = notificationApiService.listNotifications()
        val notifications = requireListData(response)
        return notifications
            .filter { it.unread != false }
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
        val notificationType = notification.notificationType
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: "GENERIC"

        return NotificationInboxEntry(
            id = notification.id
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: fallbackNotificationId(notification, notificationType),
            title = notification.title
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: fallbackTitle(notificationType),
            body = notification.body
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: fallbackBody(notificationType),
            notificationType = notificationType,
            route = resolveRoute(notification.route, notificationType),
            requestId = notification.requestId?.trim()?.takeIf(String::isNotBlank),
            materialId = notification.materialId?.trim()?.takeIf(String::isNotBlank),
            receivedAtEpochMillis = parseReceivedAt(notification.receivedAt, notificationType),
            unread = notification.unread ?: true,
            metadata = sanitizeMetadata(notification.metadata)
        )
    }

    private fun requireListData(response: Response<ApiEnvelopeDTO<List<UserNotificationDTO>>>): List<UserNotificationDTO> {
        if (response.isSuccessful) {
            return response.body()?.data.orEmpty()
        }

        throw buildApiException(response)
    }

    private fun <T> requireData(response: Response<ApiEnvelopeDTO<T>>): T {
        if (response.isSuccessful) {
            val envelope = response.body() ?: throw ApiException(response.code(), "Resposta vazia do servidor")
            @Suppress("UNCHECKED_CAST")
            return (envelope.data ?: Unit) as T
        }

        throw buildApiException(response)
    }

    private fun <T> buildApiException(response: Response<ApiEnvelopeDTO<T>>): ApiException {
        val error = response.errorBody()?.string()?.takeIf { it.isNotBlank() }?.let { payload ->
            runCatching { gson.fromJson(payload, ApiErrorResponseDTO::class.java) }.getOrNull()
        }

        return ApiException(
            statusCode = response.code(),
            message = error?.message ?: "Falha ao processar a requisição",
            fieldErrors = error?.fieldErrors ?: emptyMap()
        )
    }

    private fun resolveRoute(rawRoute: String?, notificationType: String): String {
        val route = rawRoute?.trim().orEmpty()
        return when (route) {
            AppRoutes.MY_REQUESTS,
            AppRoutes.DONOR_REQUESTS,
            AppRoutes.DISCOVERY,
            AppRoutes.DONATE,
            AppRoutes.PROFILE,
            AppRoutes.NOTIFICATIONS -> route
            else -> fallbackRoute(notificationType)
        }
    }

    private fun fallbackRoute(notificationType: String): String {
        return when (notificationType) {
            "SOLICITACAO_RECEBIDA" -> AppRoutes.DONOR_REQUESTS
            "SOLICITACAO_APROVADA",
            "SOLICITACAO_RECUSADA",
            "SOLICITACAO_CANCELADA",
            "MATERIAL_DOADO",
            "MATERIAL_CANCELADO" -> AppRoutes.MY_REQUESTS
            else -> AppRoutes.NOTIFICATIONS
        }
    }

    private fun fallbackTitle(notificationType: String): String {
        return when (notificationType) {
            "SOLICITACAO_RECEBIDA" -> "Novo pedido recebido"
            "SOLICITACAO_APROVADA" -> "Solicitação aprovada"
            "SOLICITACAO_RECUSADA" -> "Solicitação recusada"
            "SOLICITACAO_CANCELADA" -> "Solicitação cancelada"
            "MATERIAL_DOADO" -> "Doação concluída"
            "MATERIAL_CANCELADO" -> "Material removido"
            else -> "Nova notificação"
        }
    }

    private fun fallbackBody(notificationType: String): String {
        return when (notificationType) {
            "SOLICITACAO_RECEBIDA" -> "Sua doação recebeu uma nova solicitação."
            "SOLICITACAO_APROVADA" -> "Sua solicitação foi aprovada."
            "SOLICITACAO_RECUSADA" -> "Sua solicitação foi recusada."
            "SOLICITACAO_CANCELADA" -> "Houve uma atualização de cancelamento na sua solicitação."
            "MATERIAL_DOADO" -> "O material foi marcado como doado."
            "MATERIAL_CANCELADO" -> "O material foi removido pelo doador."
            else -> "Abra a central para revisar os detalhes desta notificação."
        }
    }

    private fun parseReceivedAt(rawValue: String?, notificationType: String): Long {
        val receivedAt = rawValue?.trim().orEmpty()
        if (receivedAt.isBlank()) {
            return System.currentTimeMillis()
        }

        return runCatching {
            LocalDateTime.parse(receivedAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.recoverCatching {
            OffsetDateTime.parse(receivedAt).toInstant().toEpochMilli()
        }.recoverCatching {
            Instant.parse(receivedAt).toEpochMilli()
        }.getOrElse { error ->
            Timber.w(
                error,
                "Unable to parse receivedAt for notification type %s. Falling back to current time.",
                notificationType
            )
            System.currentTimeMillis()
        }
    }

    private fun fallbackNotificationId(notification: UserNotificationDTO, notificationType: String): String {
        val rawId = listOf(
            notificationType,
            notification.requestId.orEmpty(),
            notification.materialId.orEmpty(),
            notification.route.orEmpty(),
            notification.title.orEmpty(),
            notification.body.orEmpty(),
            notification.receivedAt.orEmpty()
        ).joinToString("|").hashCode()

        return if (rawId == Int.MIN_VALUE) "0" else abs(rawId).toString()
    }

    private fun sanitizeMetadata(rawMetadata: Map<String, String>?): Map<String, String> {
        return rawMetadata.orEmpty()
            .mapNotNull { (key, value) ->
                val normalizedKey = key.trim()
                val normalizedValue = value.trim()
                if (normalizedKey.isBlank() || normalizedValue.isBlank()) {
                    null
                } else {
                    normalizedKey to normalizedValue
                }
            }
            .toMap(LinkedHashMap())
    }
}
