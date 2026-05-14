package com.ecobook.notifications

import com.ecobook.fcm.NotificationInboxEntry

data class NotificationsUiState(
    val notifications: List<NotificationInboxEntry> = emptyList(),
    val unreadCount: Int = 0
)
