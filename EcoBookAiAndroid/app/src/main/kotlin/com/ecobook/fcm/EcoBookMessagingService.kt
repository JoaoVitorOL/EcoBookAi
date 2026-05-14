package com.ecobook.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
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
            sendNotification(it)
            return
        }

        body?.takeIf { it.isNotBlank() }?.let { sendNotification(title, it, null, remoteMessage.messageId) }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "EcoBook Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications from EcoBook"
                setShowBadge(true)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

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

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builtNotification)
    }
}
