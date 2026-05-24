package com.ecobook.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.ecobook.navigation.AppRoutes
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class EcoBookMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenSyncManager: FcmTokenSyncManager

    @Inject
    lateinit var notificationInboxRepository: NotificationInboxRepository

    @Inject
    lateinit var appForegroundTracker: AppForegroundTracker

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("Message received from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "EcoBook"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
        val notification = NotificationIntentRouter.messageFromData(
            remoteMessage.data,
            fallbackTitle = title,
            fallbackBody = body
        )

        notification?.let {
            notificationInboxRepository.record(it)
            if (appForegroundTracker.isAppVisible()) {
                Timber.d(
                    "Notification %s recorded only in the in-app inbox because the app is visible.",
                    it.id
                )
            } else {
                sendNotification(it)
            }
            return
        }

        body?.takeIf { it.isNotBlank() }?.let { messageBody ->
            val fallbackNotification = AppNotification(
                id = remoteMessage.messageId,
                title = title,
                body = messageBody,
                destination = NotificationDestination(
                    route = AppRoutes.NOTIFICATIONS,
                    notificationType = "NOTIFICACAO_GERAL"
                )
            )
            notificationInboxRepository.record(fallbackNotification)
            if (appForegroundTracker.isAppVisible()) {
                Timber.d("Foreground fallback notification captured only in the notifications center.")
            } else {
                sendNotification(fallbackNotification)
            }
        }
    }

    override fun onNewToken(token: String) {
        Timber.d("Refreshed token: $token")
        fcmTokenSyncManager.syncTokenAsync(token)
    }

    private fun sendNotification(notification: AppNotification) {
        sendNotification(notification.title, notification.body, notification, notification.id)
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        appNotification: AppNotification?,
        fallbackNotificationId: String?
    ) {
        val channelId = "eco_book_notifications"
        val channel = NotificationChannel(
            channelId,
            "Notificacoes EcoBook",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notificacoes do EcoBook"
            setShowBadge(true)
        }
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        appNotification?.let {
            NotificationIntentRouter.buildPendingIntent(this, it)
                ?.let(notificationBuilder::setContentIntent)
        }

        val builtNotification = notificationBuilder.build()
        val notificationId = appNotification?.let(NotificationIntentRouter::notificationId)
            ?: fallbackNotificationId?.hashCode()
            ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

        notificationManager.notify(notificationId, builtNotification)
    }
}
