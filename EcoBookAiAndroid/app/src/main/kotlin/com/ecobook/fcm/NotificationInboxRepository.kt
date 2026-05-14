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
            val now = System.currentTimeMillis()
            val resolvedId = notification.id?.takeIf(String::isNotBlank) ?: fallbackId(notification)
            val updatedEntry = NotificationInboxEntry(
                id = resolvedId,
                title = notification.title,
                body = notification.body,
                notificationType = notification.destination.notificationType,
                route = notification.destination.route,
                requestId = notification.destination.requestId,
                materialId = notification.destination.materialId,
                receivedAtEpochMillis = now,
                unread = true
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

            persistEntries(current.map { it.copy(unread = false) })
        }
    }

    fun markAsRead(notificationId: String) {
        if (notificationId.isBlank()) {
            return
        }

        synchronized(lock) {
            val updated = _notifications.value.map { entry ->
                if (entry.id == notificationId) entry.copy(unread = false) else entry
            }
            persistEntries(updated)
        }
    }

    fun replaceAll(entries: List<NotificationInboxEntry>) {
        synchronized(lock) {
            persistEntries(
                entries.sortedByDescending(NotificationInboxEntry::receivedAtEpochMillis)
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
                ?.sortedByDescending(NotificationInboxEntry::receivedAtEpochMillis)
                .orEmpty()
        }.getOrElse { error ->
            Timber.w(error, "Unable to restore notification inbox from local storage.")
            emptyList()
        }
    }

    private fun persistEntries(entries: List<NotificationInboxEntry>) {
        _notifications.value = entries
        store.writeInbox(gson.toJson(entries))
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
