package com.ecobook.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput

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

    @OptIn(ExperimentalTestApi::class)
    fun enterTextInField(
        rule: ComposeContentTestRule,
        label: String,
        value: String
    ) {
        rule.onNode(hasText(label) and hasSetTextAction()).performTextClearance()
        rule.onNode(hasText(label) and hasSetTextAction()).performTextInput(value)
    }
}
