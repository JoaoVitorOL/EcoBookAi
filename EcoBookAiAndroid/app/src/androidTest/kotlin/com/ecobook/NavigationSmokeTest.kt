package com.ecobook

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.ecobook.ui.ComposeTestUtils
import org.junit.Rule
import org.junit.Test

class NavigationSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun authenticationScreenIsVisibleOnLaunch() {
        ComposeTestUtils.assertAuthenticationVisible(composeRule)
    }
}
