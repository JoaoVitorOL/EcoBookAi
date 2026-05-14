package com.ecobook.fcm

import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationInboxRepositoryTest {

    @Test
    fun recordShouldPersistUnreadEntriesWithoutDuplicatingKnownNotificationIds() {
        val store = FakeNotificationInboxStore()
        val repository = NotificationInboxRepository(store, GsonBuilder().create())
        val notification = sampleNotification(id = "notif-1")

        repository.record(notification)
        repository.record(notification.copy(title = "Titulo atualizado"))

        val entries = repository.notifications.value
        assertEquals(1, entries.size)
        assertEquals("Titulo atualizado", entries.single().title)
        assertTrue(entries.single().unread)
        assertFalse(store.payload.isNullOrBlank())
    }

    @Test
    fun markAllAsReadShouldClearUnreadFlags() {
        val repository = NotificationInboxRepository(FakeNotificationInboxStore(), GsonBuilder().create())
        repository.record(sampleNotification(id = "notif-2"))

        repository.markAllAsRead()

        assertTrue(repository.notifications.value.all { !it.unread })
    }

    private fun sampleNotification(id: String): AppNotification {
        return AppNotification(
            id = id,
            title = "Solicitacao aprovada",
            body = "Sua solicitacao foi aprovada.",
            destination = NotificationDestination(
                route = "my-requests",
                notificationType = "SOLICITACAO_APROVADA",
                requestId = "request-1",
                materialId = "material-1"
            )
        )
    }

    private class FakeNotificationInboxStore : NotificationInboxStore {
        var payload: String? = null

        override fun readInbox(): String? = payload

        override fun writeInbox(payload: String) {
            this.payload = payload
        }
    }
}
