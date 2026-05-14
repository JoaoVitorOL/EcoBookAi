package com.ecobook.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecobook.auth.SessionManager
import com.ecobook.data.NotificationRepository
import com.ecobook.fcm.NotificationInboxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationInboxRepository: NotificationInboxRepository,
    private val notificationRepository: NotificationRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val uiState: StateFlow<NotificationsUiState> = notificationInboxRepository.notifications
        .map { notifications ->
            NotificationsUiState(
                notifications = notifications,
                unreadCount = notifications.count { it.unread }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationsUiState()
        )

    fun refresh() {
        if (!sessionManager.hasActiveSession()) {
            return
        }

        viewModelScope.launch {
            runCatching { notificationRepository.listNotifications() }
                .onSuccess(notificationInboxRepository::replaceAll)
                .onFailure { error ->
                    Timber.w(error, "Unable to refresh business notifications from backend.")
                }
        }
    }

    fun onScreenOpened() {
        if (!sessionManager.hasActiveSession()) {
            notificationInboxRepository.markAllAsRead()
            return
        }

        viewModelScope.launch {
            runCatching { notificationRepository.listNotifications() }
                .onSuccess(notificationInboxRepository::replaceAll)
                .onFailure { error ->
                    Timber.w(error, "Unable to load notifications from backend on screen open.")
                }

            runCatching { notificationRepository.markAllAsRead() }
                .onFailure { error ->
                    Timber.w(error, "Unable to mark all backend notifications as read.")
                }

            notificationInboxRepository.markAllAsRead()
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            if (sessionManager.hasActiveSession()) {
                runCatching { notificationRepository.markAllAsRead() }
                    .onFailure { error ->
                        Timber.w(error, "Unable to mark all backend notifications as read.")
                    }
            }
            notificationInboxRepository.markAllAsRead()
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            if (sessionManager.hasActiveSession()) {
                runCatching { notificationRepository.markAsRead(notificationId) }
                    .onFailure { error ->
                        Timber.w(error, "Unable to mark backend notification %s as read.", notificationId)
                    }
            }
            notificationInboxRepository.markAsRead(notificationId)
        }
    }
}
