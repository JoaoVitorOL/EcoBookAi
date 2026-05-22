package com.ecobook.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecobook.auth.SessionManager
import com.ecobook.data.ApiException
import com.ecobook.data.NotificationRepository
import com.ecobook.fcm.NotificationInboxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationInboxRepository: NotificationInboxRepository,
    private val notificationRepository: NotificationRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            notificationInboxRepository.notifications.collectLatest { notifications ->
                _uiState.update { state ->
                    state.copy(
                        notifications = notifications,
                        unreadCount = notifications.count { it.unread }
                    )
                }
            }
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing || !sessionManager.hasActiveSession()) {
            return
        }

        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { notificationRepository.listNotifications() }
                .onSuccess { notifications ->
                    notificationInboxRepository.replaceAll(notifications)
                    _uiState.update { state ->
                        state.copy(
                            isRefreshing = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    Timber.w(error, "Unable to refresh business notifications from backend.")
                    _uiState.update { state ->
                        state.copy(
                            isRefreshing = false,
                            errorMessage = resolveSyncError(error)
                        )
                    }
                }
        }
    }

    fun onScreenOpened() {
        refresh()
    }

    fun markAllAsRead() {
        if (_uiState.value.isMarkingAllAsRead || _uiState.value.unreadCount == 0) {
            return
        }

        _uiState.update { it.copy(isMarkingAllAsRead = true, errorMessage = null) }
        viewModelScope.launch {
            if (sessionManager.hasActiveSession()) {
                runCatching { notificationRepository.markAllAsRead() }
                    .onSuccess {
                        notificationInboxRepository.markAllAsRead()
                        _uiState.update { state ->
                            state.copy(
                                isMarkingAllAsRead = false,
                                errorMessage = null
                            )
                        }
                    }
                    .onFailure { error ->
                        Timber.w(error, "Unable to mark all backend notifications as read.")
                        _uiState.update { state ->
                            state.copy(
                                isMarkingAllAsRead = false,
                                errorMessage = resolveSyncError(error)
                            )
                        }
                    }
            } else {
                notificationInboxRepository.markAllAsRead()
                _uiState.update { state ->
                    state.copy(
                        isMarkingAllAsRead = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        if (notificationId.isBlank()) {
            return
        }

        val notification = _uiState.value.notifications.firstOrNull { it.id == notificationId } ?: return
        if (!notification.unread || _uiState.value.activeReadNotificationId == notificationId) {
            return
        }

        _uiState.update { it.copy(activeReadNotificationId = notificationId, errorMessage = null) }
        viewModelScope.launch {
            if (sessionManager.hasActiveSession()) {
                runCatching { notificationRepository.markAsRead(notificationId) }
                    .onSuccess {
                        notificationInboxRepository.markAsRead(notificationId)
                        _uiState.update { state ->
                            state.copy(
                                activeReadNotificationId = null,
                                errorMessage = null
                            )
                        }
                    }
                    .onFailure { error ->
                        Timber.w(error, "Unable to mark backend notification %s as read.", notificationId)
                        _uiState.update { state ->
                            state.copy(
                                activeReadNotificationId = null,
                                errorMessage = resolveSyncError(error)
                            )
                        }
                    }
            } else {
                notificationInboxRepository.markAsRead(notificationId)
                _uiState.update { state ->
                    state.copy(
                        activeReadNotificationId = null,
                        errorMessage = null
                    )
                }
            }
        }
    }

    private fun resolveSyncError(error: Throwable): String {
        val fallbackMessage = "Não foi possível sincronizar a central agora. Confira o backend configurado no app e tente novamente."

        return when (error) {
            is ApiException -> when (error.statusCode) {
                401 -> "Sua sessão expirou. Entre novamente para continuar."
                403 -> "Você não tem permissão para acessar essa central de notificações."
                else -> sanitizeBackendMessage(error.message, fallbackMessage)
            }

            is SocketTimeoutException -> "A sincronização das notificações demorou demais para responder."
            is ConnectException,
            is UnknownHostException,
            is IOException -> "Não foi possível sincronizar a central de notificações com o backend configurado no app."
            else -> sanitizeBackendMessage(error.message, fallbackMessage)
        }
    }

    private fun sanitizeBackendMessage(rawMessage: String?, fallbackMessage: String): String {
        val normalized = rawMessage
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return fallbackMessage

        return when {
            normalized.equals("text", ignoreCase = true) -> fallbackMessage
            normalized.equals("unknown error", ignoreCase = true) -> fallbackMessage
            normalized.equals("falha ao processar a requisição", ignoreCase = true) -> fallbackMessage
            normalized.length < 4 -> fallbackMessage
            else -> normalized
        }
    }
}
