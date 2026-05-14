package com.ecobook.fcm

import com.ecobook.navigation.AppRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationIntentRouterTest {

    @Test
    fun `maps donor notification to donor requests route`() {
        val destination = NotificationIntentRouter.destinationFromData(
            mapOf(
                "type" to "SOLICITACAO_RECEBIDA",
                "solicitacao_id" to "request-1",
                "material_id" to "material-1"
            )
        )

        assertEquals(AppRoutes.DONOR_REQUESTS, destination?.route)
        assertEquals("SOLICITACAO_RECEBIDA", destination?.notificationType)
    }

    @Test
    fun `maps student notification to my requests route`() {
        val destination = NotificationIntentRouter.destinationFromData(
            mapOf(
                "type" to "MATERIAL_DOADO",
                "solicitacao_id" to "request-2"
            )
        )

        assertEquals(AppRoutes.MY_REQUESTS, destination?.route)
        assertEquals("MATERIAL_DOADO", destination?.notificationType)
    }

    @Test
    fun `returns null for unknown notification type`() {
        val destination = NotificationIntentRouter.destinationFromData(
            mapOf("type" to "TIPO_DESCONHECIDO")
        )

        assertNull(destination)
    }

    @Test
    fun `generates stable ids per notification payload`() {
        val approved = NotificationDestination(
            route = AppRoutes.MY_REQUESTS,
            notificationType = "SOLICITACAO_APROVADA",
            requestId = "request-1",
            materialId = "material-1"
        )
        val declined = approved.copy(notificationType = "SOLICITACAO_RECUSADA")

        assertEquals(
            NotificationIntentRouter.notificationId(approved),
            NotificationIntentRouter.notificationId(approved)
        )
        assertNotEquals(
            NotificationIntentRouter.notificationId(approved),
            NotificationIntentRouter.notificationId(declined)
        )
    }
}
