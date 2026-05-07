package com.ecobook.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.ComposeContentTestRule

object ComposeTestUtils {

    fun assertBottomNavigationVisible(rule: ComposeContentTestRule) {
        rule.onNodeWithText("Painel").assertIsDisplayed()
        rule.onNodeWithText("Buscar").assertIsDisplayed()
        rule.onNodeWithText("Doar").assertIsDisplayed()
        rule.onNodeWithText("Perfil").assertIsDisplayed()
    }

    fun assertAuthenticationVisible(rule: ComposeContentTestRule) {
        rule.onNodeWithText("Entrar no EcoBook").assertIsDisplayed()
        rule.onNodeWithText("Ainda nao tem conta? Criar conta").assertIsDisplayed()
    }
}
