package com.ecobook.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ecobook.MainActivity
import org.junit.Rule
import org.junit.Test

class AuthValidationInstrumentationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun registerModeShouldShowLocalValidationMessagesBeforeAnyBackendCall() {
        composeRule.onNodeWithText("Ainda nao tem conta? Criar conta").performClick()
        composeRule.onAllNodesWithText("Criar conta")[1].performClick()

        composeRule.onNodeWithText("Informe seu nome para criar a conta.").assertIsDisplayed()
        composeRule.onNodeWithText("Informe seu email.").assertIsDisplayed()
        composeRule.onNodeWithText("Informe sua senha.").assertIsDisplayed()
        composeRule.onNodeWithText("Confirme sua senha.").assertIsDisplayed()
    }
}
