package com.ecobook.ui

import androidx.compose.ui.test.ComposeContentTestRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText

object ComposeTestUtils {

    fun assertBottomNavigationVisible(rule: ComposeContentTestRule) {
        rule.onNodeWithText("Painel").assertIsDisplayed()
        rule.onNodeWithText("Buscar").assertIsDisplayed()
        rule.onNodeWithText("Doar").assertIsDisplayed()
        rule.onNodeWithText("Perfil").assertIsDisplayed()
    }

    fun assertAuthenticationVisible(rule: ComposeContentTestRule) {
        rule.onNodeWithText("Continuar com Google").assertIsDisplayed()
        rule.onNodeWithText("Entrar no EcoBook").assertIsDisplayed()
    }
}
