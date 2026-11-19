package com.birddex.app.ui.detect

import com.birddex.app.data.birds.Bird
import com.birddex.app.data.detect.Detection
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class DetectFormatTest {

    @Test fun labelBecomesANameLikeTheWebsite() {
        assertEquals("House Sparrow", labelToName("house_sparrow"))
        assertEquals("Galah", labelToName("galah"))
        assertEquals("Red Browed Finch", labelToName("red_browed_finch"))
    }

    @Test fun nameComesFromTheBirdRowWhenThereIsOne() {
        val detection = Detection(
            birds = mapOf("red_browed_finch" to Bird(id = 25, name = "Red-browed Finch", scientificName = "Neochmia temporalis", slug = "red_browed_finch")),
        )
        assertEquals("Red-browed Finch", detection.nameOf("red_browed_finch"))
        assertEquals("Removed Bird", detection.nameOf("removed_bird")) // no row: made from the label
    }

    @Test fun confidenceShowsUpToTwoDecimals() {
        assertEquals("91.37%", formatConfidence(91.37, Locale.US))
        assertEquals("4.2%", formatConfidence(4.2, Locale.US))
        assertEquals("100%", formatConfidence(100.0, Locale.US))
        assertEquals("0.5%", formatConfidence(0.5, Locale.US))
    }

    @Test fun confidenceFollowsThePhonesLanguage() {
        assertEquals("91,37 %", formatConfidence(91.37, Locale.GERMANY).replace(' ', ' '))
    }
}
