package com.birddex.app.ui.welcome

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WelcomeDialogTest {

    @get:Rule val compose = createComposeRule()

    private val clicks = mutableListOf<String>()

    private fun show(name: String?) = compose.setContent {
        WelcomeDialog(
            name = name,
            onExplore = { clicks += "explore" },
            onTryDetect = { clicks += "detect" },
            onDismiss = { clicks += "dismiss" },
        )
    }

    @Test fun greetsByNameAndShowsTheThreeSteps() {
        show("Jane Doe")

        compose.onNodeWithText("Welcome to BirdDex, Jane Doe!").assertIsDisplayed()
        compose.onNodeWithText("Snap a Photo").assertIsDisplayed()
        compose.onNodeWithText("Get Instant ID").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Build Your Collection").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("greyed out", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test fun greetsWithoutANameIfNotKnown() {
        show(null)

        compose.onNodeWithText("Welcome to BirdDex!").assertIsDisplayed()
    }

    @Test fun buttonsSayWhereToGo() {
        show("Jane Doe")

        compose.onNodeWithText("Try Detect").performClick()
        compose.onNodeWithText("Explore MyDex").performClick()

        assertEquals(listOf("detect", "explore"), clicks)
    }
}
