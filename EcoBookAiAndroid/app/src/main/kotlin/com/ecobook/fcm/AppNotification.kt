package com.ecobook.fcm

data class AppNotification(
    val id: String?,
    val title: String,
    val body: String,
    val destination: NotificationDestination,
    val metadata: Map<String, String> = emptyMap()
)
