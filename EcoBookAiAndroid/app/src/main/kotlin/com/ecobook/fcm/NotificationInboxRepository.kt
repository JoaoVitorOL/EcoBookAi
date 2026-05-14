package com.ecobook.fcm

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import kotlin.math.abs

@Singleton
class NotificationInboxRepository @Inject constructor(
    private val store: NotificationInboxStore,
    private val gson: Gson
) {

    private val lock = Any()
    private val listType = object : TypeToken<List<NotificationInboxEntry>>() {}.type
    private val _notifications = MutableStateFlow(loadEntries())
    val notifications: StateFlow<List<NotificationInboxEntry>> = _notifications.asStateFlow()

    fun record(notification: AppNotification) {
        synchronized(lock) {
            val resolvedId = notification.id?.takeIf(String::isNotBlank) ?: fallbackId(notification)
            val existingEntry = _notifications.value.firstOrNull { it.id == resolvedId }
            val updatedEntry = NotificationInboxEntry(
                id = resolvedId,
                title = notification.title,
                body = notification.body,
                notificationType = notification.destination.notificationType,
                route = notification.destination.route,
                requestId = notification.destination.requestId,
                materialId = notification.destination.materialId,
                receivedAtEpochMillis = existingEntry?.receivedAtEpochMillis ?: System.currentTimeMillis(),
                unread = existingEntry?.unread ?: true
            )

            val merged = buildList {
                add(updatedEntry)
                addAll(_notifications.value.filterNot { it.id == resolvedId })
            }.take(MAX_ENTRIES)

            persistEntries(merged)
        }
    }

    fun markAllAsRead() {
        synchronized(lock) {
            val current = _notifications.value
            if (current.none { it.unread }) {
                return
            }

            persistEntries(emptyList())
        }
    }

    fun markAsRead(notificationId: String) {
        if (notificationId.isBlank()) {
            return
        }

        synchronized(lock) {
            val updated = _notifications.value.filterNot { entry -> entry.id == notificationId }
            persistEntries(updated)
        }
    }

    fun replaceAll(entries: List<NotificationInboxEntry>) {
        synchronized(lock) {
            val unreadEntries = entries.filter(NotificationInboxEntry::unread)
            val backendIds = unreadEntries.mapTo(mutableSetOf()) { it.id }
            persistEntries(
                buildList {
                    addAll(unreadEntries)
                    addAll(_notifications.value.filterNot { it.id in backendIds })
                }.sortedByDescending(NotificationInboxEntry::receivedAtEpochMillis)
                    .take(MAX_ENTRIES)
            )
        }
    }

    private fun loadEntries(): List<NotificationInboxEntry> {
        val payload = store.readInbox().orEmpty()
        if (payload.isBlank()) {
            return emptyList()
        }

        return runCatching {
            gson.fromJson<List<NotificationInboxEntry>>(payload, listType)
                ?.filter(NotificationInboxEntry::unread)
                ?.sortedByDescending(NotificationInboxEntry::receivedAtEpochMillis)
                .orEmpty()
        }.getOrElse { error ->
            Timber.w(error, "Unable to restore notification inbox from local storage.")
            emptyList()
        }
    }

    private fun persistEntries(entries: List<NotificationInboxEntry>) {
        val visibleEntries = entries
            .filter(NotificationInboxEntry::unread)
            .sortedByDescending(NotificationInboxEntry::receivedAtEpochMillis)
            .take(MAX_ENTRIES)

        _notifications.value = visibleEntries
        store.writeInbox(gson.toJson(visibleEntries))
    }

    private fun fallbackId(notification: AppNotification): String {
        val rawId = listOf(
            notification.destination.notificationType,
            notification.destination.requestId.orEmpty(),
            notification.destination.materialId.orEmpty(),
            notification.destination.route,
            notification.title,
            notification.body
        ).joinToString("|").hashCode()

        return when (rawId) {
            Int.MIN_VALUE -> "0"
            else -> abs(rawId).toString()
        }
    }

    private companion object {
        const val MAX_ENTRIES = 50
    }
}
