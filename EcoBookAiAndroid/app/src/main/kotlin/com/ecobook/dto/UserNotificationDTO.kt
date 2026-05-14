package com.ecobook.dto

data class UserNotificationDTO(
    val id: String,
    val title: String,
    val body: String,
    val notificationType: String,
    val route: String,
    val requestId: String? = null,
    val materialId: String? = null,
    val receivedAt: String,
    val unread: Boolean
)
