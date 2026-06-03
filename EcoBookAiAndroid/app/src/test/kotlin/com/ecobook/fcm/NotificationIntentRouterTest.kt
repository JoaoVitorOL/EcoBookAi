package com.ecobook.fcm

import com.ecobook.navigation.AppRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationIntentRouterTest {

    @Test
    fun `maps donor notification to donor requests route`() {
        val notification = NotificationIntentRouter.messageFromData(
            mapOf(
                "notification_id" to "notif-1",
                "type" to "SOLICITACAO_RECEBIDA",
                "title" to "Novo pedido recebido",
                "body" to "Chegou uma nova solicitacao.",
                "solicitacao_id" to "request-1",
                "material_id" to "material-1"
            )
        )

        assertEquals("notif-1", notification?.id)
        assertEquals(AppRoutes.DONOR_REQUESTS, notification?.destination?.route)
        assertEquals("SOLICITACAO_RECEBIDA", notification?.destination?.notificationType)
    }

    @Test
    fun `maps student notification to my requests route`() {
        val notification = NotificationIntentRouter.messageFromData(
            mapOf(
                "type" to "MATERIAL_DOADO",
                "title" to "Doação concluída",
                "body" to "O material foi marcado como doado.",
                "solicitacao_id" to "request-2"
            )
        )

        assertEquals(AppRoutes.MY_REQUESTS, notification?.destination?.route)
        assertEquals("MATERIAL_DOADO", notification?.destination?.notificationType)
    }

    @Test
    fun `returns null for unknown notification type`() {
        val destination = NotificationIntentRouter.messageFromData(
            mapOf("type" to "TIPO_DESCONHECIDO")
        )

        assertNull(destination)
    }

    @Test
    fun `generates stable ids per app notification payload`() {
        val approved = AppNotification(
            id = "notif-approved",
            title = "Solicitacao aprovada",
            body = "Sua solicitacao foi aprovada.",
            destination = NotificationDestination(
                route = AppRoutes.MY_REQUESTS,
                notificationType = "SOLICITACAO_APROVADA",
                requestId = "request-1",
                materialId = "material-1"
            )
        )
        val declined = approved.copy(id = "notif-declined")

        assertEquals(
            NotificationIntentRouter.notificationId(approved),
            NotificationIntentRouter.notificationId(approved)
        )
        assertNotEquals(
            NotificationIntentRouter.notificationId(approved),
            NotificationIntentRouter.notificationId(declined)
        )
    }

    @Test
    fun `prefers explicit route when backend sends it`() {
        val destination = NotificationIntentRouter.destinationFromData(
            mapOf(
                "type" to "SOLICITACAO_APROVADA",
                "route" to AppRoutes.NOTIFICATIONS
            )
        )

        assertEquals(AppRoutes.NOTIFICATIONS, destination?.route)
        assertTrue(destination?.notificationType == "SOLICITACAO_APROVADA")
    }
}
