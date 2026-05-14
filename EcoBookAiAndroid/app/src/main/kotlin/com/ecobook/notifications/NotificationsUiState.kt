package com.ecobook.notifications

import com.ecobook.fcm.NotificationInboxEntry

data class NotificationsUiState(
    val notifications: List<NotificationInboxEntry> = emptyList(),
    val unreadCount: Int = 0,
    val isRefreshing: Boolean = false,
    val isMarkingAllAsRead: Boolean = false,
    val activeReadNotificationId: String? = null,
    val errorMessage: String? = null
)
