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
    const val EXTRA_NOTIFICATION_TYPE = "ecobook.notification.type"
    const val EXTRA_REQUEST_ID = "ecobook.notification.request_id"
    const val EXTRA_MATERIAL_ID = "ecobook.notification.material_id"

    private const val DEEP_LINK_SCHEME = "ecobook"
    private const val DEEP_LINK_HOST = "app"

    fun destinationFromData(data: Map<String, String>): NotificationDestination? {
        val type = data["type"]
            ?.trim()
            ?.uppercase()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val route = routeForType(type) ?: return null
        return NotificationDestination(
            route = route,
            notificationType = type,
            requestId = data["solicitacao_id"]?.takeIf { it.isNotBlank() },
            materialId = data["material_id"]?.takeIf { it.isNotBlank() }
        )
    }

    fun destinationFromIntent(intent: Intent?): NotificationDestination? {
        if (intent == null) {
            return null
        }

        return intent.data?.let(::destinationFromUri) ?: destinationFromExtras(intent)
    }

    fun notificationId(destination: NotificationDestination): Int {
        val rawId = listOf(
            destination.notificationType,
            destination.requestId.orEmpty(),
            destination.materialId.orEmpty(),
            destination.route
        ).joinToString("|").hashCode()

        return when (rawId) {
            Int.MIN_VALUE -> 0
            else -> abs(rawId)
        }
    }

    fun buildPendingIntent(
        context: Context,
        destination: NotificationDestination
    ): PendingIntent? {
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            destination.toUri(),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ROUTE, destination.route)
            putExtra(EXTRA_NOTIFICATION_TYPE, destination.notificationType)
            putExtra(EXTRA_REQUEST_ID, destination.requestId)
            putExtra(EXTRA_MATERIAL_ID, destination.materialId)
        }

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(deepLinkIntent)
            getPendingIntent(
                notificationId(destination),
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
            else -> null
        }
    }

    private fun pathForRoute(route: String): String {
        return when (route) {
            AppRoutes.DONOR_REQUESTS -> AppRoutes.DONOR_REQUESTS
            AppRoutes.MY_REQUESTS -> AppRoutes.MY_REQUESTS
            AppRoutes.DISCOVERY -> AppRoutes.DISCOVERY
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
}
