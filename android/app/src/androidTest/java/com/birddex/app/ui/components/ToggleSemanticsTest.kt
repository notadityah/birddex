package com.birddex.app.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.birddex.app.data.birds.Bird
import com.birddex.app.ui.account.AnonymitySetting
import com.birddex.app.ui.account.GallerySettings
import com.birddex.app.ui.detect.SightingSaver
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The on/off state screen readers get from the app's toggle rows (a whole row that toggles,
 * with a Switch or Checkbox drawn inside), read from Compose's semantics directly.
 */
@RunWith(AndroidJUnit4::class)
class ToggleSemanticsTest {

    @get:Rule val compose = createComposeRule()

    private val sparrow = Bird(id = 14, name = "House Sparrow", scientificName = "Passer domesticus", slug = "house_sparrow")

    @Test fun shareCheckboxReportsWhatItShows() {
        compose.setContent {
            SightingSaver(bird = sparrow, alreadyFound = false, saving = false, error = null, onSave = { _, _ -> })
        }
        val share = compose.onNodeWithText("Share to Community Gallery")

        share.assertIsOff()
        share.performClick()
        share.assertIsOn()
        share.performClick()
        share.assertIsOff()
    }

    @Test fun anonymitySwitchReportsWhatItShows() {
        compose.setContent {
            var setting by remember { mutableStateOf(AnonymitySetting(anonymous = false)) }
            GallerySettings(setting, onToggle = { setting = setting.copy(anonymous = setting.anonymous != true) }, onRetry = {})
        }
        val switch = compose.onNodeWithText("Appear as Anonymous in gallery")

        switch.assertIsOff()
        switch.assertIsEnabled()
        switch.performClick()
        switch.assertIsOn()
        switch.performClick()
        switch.assertIsOff()
    }

    @Test fun anonymitySwitchIsDisabledUntilLoaded() {
        compose.setContent { GallerySettings(AnonymitySetting(loading = true), onToggle = {}, onRetry = {}) }

        compose.onNodeWithText("Appear as Anonymous in gallery").assertIsNotEnabled()
    }
}
