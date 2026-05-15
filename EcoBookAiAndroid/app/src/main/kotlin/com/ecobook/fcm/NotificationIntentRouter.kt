package com.ecobook.fcm

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.TaskStackBuilder
import com.ecobook.MainActivity
import com.ecobook.navigation.AppRoutes
import kotlin.math.abs

data class NotificationDestination(
    val route: String,
    val notificationType: String,
    val requestId: String? = null,
    val materialId: String? = null
)

object NotificationIntentRouter {

    const val EXTRA_ROUTE = "ecobook.notification.route"
    const val EXTRA_NOTIFICATION_ID = "ecobook.notification.id"
    const val EXTRA_NOTIFICATION_TYPE = "ecobook.notification.type"
    const val EXTRA_REQUEST_ID = "ecobook.notification.request_id"
    const val EXTRA_MATERIAL_ID = "ecobook.notification.material_id"
    const val EXTRA_TITLE = "ecobook.notification.title"
    const val EXTRA_BODY = "ecobook.notification.body"

    private const val DEEP_LINK_SCHEME = "ecobook"
    private const val DEEP_LINK_HOST = "app"
    private val reservedKeys = setOf(
        "notification_id",
        "type",
        "title",
        "body",
        "message",
        "route",
        "solicitacao_id",
        "material_id"
    )

    fun messageFromData(
        data: Map<String, String>,
        fallbackTitle: String? = null,
        fallbackBody: String? = null
    ): AppNotification? {
        val destination = destinationFromData(data) ?: return null
        val title = data["title"]?.takeIf(String::isNotBlank)
            ?: fallbackTitle?.takeIf(String::isNotBlank)
            ?: "EcoBook"
        val body = data["body"]?.takeIf(String::isNotBlank)
            ?: data["message"]?.takeIf(String::isNotBlank)
            ?: fallbackBody?.takeIf(String::isNotBlank)
            ?: return null

        return AppNotification(
            id = data["notification_id"]?.takeIf(String::isNotBlank),
            title = title,
            body = body,
            destination = destination,
            metadata = metadataFromData(data)
        )
    }

    fun messageFromIntent(intent: Intent?): AppNotification? {
        if (intent == null) {
            return null
        }

        val extras = intent.extras?.keySet()
            ?.associateWith { key -> intent.extras?.getString(key).orEmpty() }
            ?.filterValues(String::isNotBlank)
            .orEmpty()

        val destination = intent.data?.let(::destinationFromUri)
            ?: destinationFromData(extras)
            ?: destinationFromExtras(intent)
            ?: return null

        val title = extras["title"]
            ?: intent.getStringExtra(EXTRA_TITLE)
            ?: "EcoBook"
        val body = extras["body"]
            ?: extras["message"]
            ?: intent.getStringExtra(EXTRA_BODY)
            ?: return null

        return AppNotification(
            id = extras["notification_id"] ?: intent.getStringExtra(EXTRA_NOTIFICATION_ID),
            title = title,
            body = body,
            destination = destination,
            metadata = metadataFromData(extras)
        )
    }

    fun destinationFromData(data: Map<String, String>): NotificationDestination? {
        val type = data["type"]
            ?.trim()
            ?.uppercase()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val route = routeForPath(data["route"]) ?: routeForType(type) ?: return null
        return NotificationDestination(
            route = route,
            notificationType = type,
            requestId = data["solicitacao_id"]?.takeIf { it.isNotBlank() },
            materialId = data["material_id"]?.takeIf { it.isNotBlank() }
        )
    }

    fun destinationFromIntent(intent: Intent?): NotificationDestination? {
        return messageFromIntent(intent)?.destination
    }

    fun notificationId(notification: AppNotification): Int {
        val rawId = listOf(
            notification.id.orEmpty(),
            notification.destination.notificationType,
            notification.destination.requestId.orEmpty(),
            notification.destination.materialId.orEmpty(),
            notification.destination.route
        ).joinToString("|").hashCode()

        return when (rawId) {
            Int.MIN_VALUE -> 0
            else -> abs(rawId)
        }
    }

    fun buildPendingIntent(
        context: Context,
        notification: AppNotification
    ): PendingIntent? {
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            notification.destination.toUri(),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ROUTE, notification.destination.route)
            putExtra(EXTRA_NOTIFICATION_ID, notification.id)
            putExtra(EXTRA_NOTIFICATION_TYPE, notification.destination.notificationType)
            putExtra(EXTRA_REQUEST_ID, notification.destination.requestId)
            putExtra(EXTRA_MATERIAL_ID, notification.destination.materialId)
            putExtra(EXTRA_TITLE, notification.title)
            putExtra(EXTRA_BODY, notification.body)
        }

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(deepLinkIntent)
            getPendingIntent(
                notificationId(notification),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    private fun destinationFromExtras(intent: Intent): NotificationDestination? {
        val route = intent.getStringExtra(EXTRA_ROUTE)
            ?.takeIf { it.isNotBlank() }
            ?: routeForType(
                intent.getStringExtra(EXTRA_NOTIFICATION_TYPE)
                    ?.trim()
                    ?.uppercase()
                    .orEmpty()
            )
            ?: return null

        return NotificationDestination(
            route = route,
            notificationType = intent.getStringExtra(EXTRA_NOTIFICATION_TYPE)
                ?.trim()
                ?.uppercase()
                ?.takeIf { it.isNotBlank() }
                ?: inferredTypeForRoute(route),
            requestId = intent.getStringExtra(EXTRA_REQUEST_ID),
            materialId = intent.getStringExtra(EXTRA_MATERIAL_ID)
        )
    }

    private fun destinationFromUri(uri: Uri): NotificationDestination? {
        if (uri.scheme != DEEP_LINK_SCHEME || uri.host != DEEP_LINK_HOST) {
            return null
        }

        val route = routeForPath(uri.lastPathSegment) ?: return null
        return NotificationDestination(
            route = route,
            notificationType = uri.getQueryParameter("type")
                ?.trim()
                ?.uppercase()
                ?.takeIf { it.isNotBlank() }
                ?: inferredTypeForRoute(route),
            requestId = uri.getQueryParameter("solicitacao_id"),
            materialId = uri.getQueryParameter("material_id")
        )
    }

    private fun NotificationDestination.toUri(): Uri {
        return Uri.Builder()
            .scheme(DEEP_LINK_SCHEME)
            .authority(DEEP_LINK_HOST)
            .appendPath(pathForRoute(route))
            .apply {
                appendQueryParameter("type", notificationType)
                requestId?.let { appendQueryParameter("solicitacao_id", it) }
                materialId?.let { appendQueryParameter("material_id", it) }
            }
            .build()
    }

    private fun routeForType(type: String): String? {
        return when (type) {
            "SOLICITACAO_RECEBIDA" -> AppRoutes.DONOR_REQUESTS
            "SOLICITACAO_APROVADA",
            "SOLICITACAO_RECUSADA",
            "SOLICITACAO_CANCELADA",
            "MATERIAL_DOADO",
            "MATERIAL_CANCELADO" -> AppRoutes.MY_REQUESTS
            else -> null
        }
    }

    private fun routeForPath(path: String?): String? {
        return when (path) {
            AppRoutes.DONOR_REQUESTS -> AppRoutes.DONOR_REQUESTS
            AppRoutes.MY_REQUESTS -> AppRoutes.MY_REQUESTS
            AppRoutes.DISCOVERY -> AppRoutes.DISCOVERY
            AppRoutes.NOTIFICATIONS -> AppRoutes.NOTIFICATIONS
            else -> null
        }
    }

    private fun pathForRoute(route: String): String {
        return when (route) {
            AppRoutes.DONOR_REQUESTS -> AppRoutes.DONOR_REQUESTS
            AppRoutes.MY_REQUESTS -> AppRoutes.MY_REQUESTS
            AppRoutes.DISCOVERY -> AppRoutes.DISCOVERY
            AppRoutes.NOTIFICATIONS -> AppRoutes.NOTIFICATIONS
            else -> AppRoutes.MY_REQUESTS
        }
    }

    private fun inferredTypeForRoute(route: String): String {
        return when (route) {
            AppRoutes.DONOR_REQUESTS -> "SOLICITACAO_RECEBIDA"
            AppRoutes.DISCOVERY -> "DISCOVERY"
            else -> "SOLICITACAO_APROVADA"
        }
    }

    private fun metadataFromData(data: Map<String, String>): Map<String, String> {
        return buildMap {
            data.forEach { (key, value) ->
                val normalizedKey = key.trim()
                val normalizedValue = value.trim()
                if (
                    normalizedKey.isNotBlank() &&
                    normalizedValue.isNotBlank() &&
                    normalizedKey !in reservedKeys
                ) {
                    put(normalizedKey, normalizedValue)
                }
            }
        }
    }
}
