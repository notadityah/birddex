package com.birddex.app.ui.account

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.birddex.app.data.feedback.FeedbackCategory
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedbackSheetTest {

    @get:Rule val compose = createComposeRule()

    @Test fun sendsThePickedCategoryAndMessage() {
        val sent = mutableListOf<Pair<FeedbackCategory, String>>()
        compose.setContent { FeedbackSheet(FeedbackUiState(), onSend = { c, m -> sent += c to m }, onDismiss = {}) }

        compose.onNodeWithText("Bug Report").assertIsDisplayed() // the website's default
        compose.onNodeWithText("Bug Report").performClick()
        compose.onNodeWithText("Suggestion").performClick()
        compose.onNodeWithText("Message").performTextInput("Hello")
        compose.onNodeWithText("5 / 2000").assertIsDisplayed()
        compose.onNodeWithText("Submit").performClick()

        assertEquals(listOf(FeedbackCategory.Suggestion to "Hello"), sent)
    }

    @Test fun showsWhyItWasNotSent() {
        compose.setContent { FeedbackSheet(FeedbackUiState(error = FeedbackError.EmptyMessage), onSend = { _, _ -> }, onDismiss = {}) }

        compose.onNodeWithText("Please enter a message.").assertIsDisplayed()
    }

    @Test fun lockedWhileSending() {
        compose.setContent { FeedbackSheet(FeedbackUiState(sending = true), onSend = { _, _ -> }, onDismiss = {}) }

        compose.onNodeWithText("Sending…").assertIsNotEnabled()
    }

    @Test fun thanksThenClosesItself() {
        var dismissed = false
        compose.setContent { FeedbackSheet(FeedbackUiState(sent = true), onSend = { _, _ -> }, onDismiss = { dismissed = true }) }

        compose.onNodeWithText("Thanks for your feedback!").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(2_100)
        compose.waitForIdle()
        assertEquals(true, dismissed)
    }
}
