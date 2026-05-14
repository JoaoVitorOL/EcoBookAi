package com.ecobook.fcm

data class NotificationInboxEntry(
    val id: String,
    val title: String,
    val body: String,
    val notificationType: String,
    val route: String,
    val requestId: String? = null,
    val materialId: String? = null,
    val receivedAtEpochMillis: Long,
    val unread: Boolean = true
)
