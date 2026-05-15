package com.ecobook.dto

data class UserNotificationDTO(
    val id: String? = null,
    val title: String? = null,
    val body: String? = null,
    val notificationType: String? = null,
    val route: String? = null,
    val requestId: String? = null,
    val materialId: String? = null,
    val receivedAt: String? = null,
    val unread: Boolean? = null,
    val metadata: Map<String, String> = emptyMap()
)
