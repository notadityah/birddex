package com.birddex.app.ui.detect

import com.birddex.app.data.birds.Bird
import com.birddex.app.data.detect.Prediction
import org.junit.Assert.assertEquals
import org.junit.Test

/** The report text must read exactly like the website's (ReportMatchModal.vue), for the admins. */
class ReportMessageTest {

    private val wattlebird = Bird(id = 17, name = "Little Wattlebird", scientificName = "Anthochaera chrysoptera", slug = "little_wattlebird")
    private val predictions = listOf(
        Prediction("little_wattlebird", 70.39),
        Prediction("red_wattlebird", 56.9),
        Prediction("galah", 100.0),
    )

    @Test fun detailsListTheMatchShownAndEveryPrediction() {
        assertEquals(
            """
            Best match shown: Little Wattlebird (little_wattlebird)
            Predictions:
              - Little Wattlebird — 70.39%
              - Red Wattlebird — 56.9%
              - Galah — 100%
            """.trimIndent(),
            detectionReportDetails(wattlebird, predictions),
        )
    }

    @Test fun detailsWithoutAMatchOrPredictions() {
        assertEquals("Predictions:\n  - Galah — 100%", detectionReportDetails(null, predictions.takeLast(1)))
        assertEquals("", detectionReportDetails(null, emptyList()))
    }

    @Test fun messageAddsTheHeaderAndTheTrimmedNote() {
        assertEquals(
            "Wrong / missing match reported from the detect screen.\n\nDETAILS\n\nUser note: It's a Noisy Miner",
            detectionReportMessage("DETAILS", "  It's a Noisy Miner \n"),
        )
        assertEquals(
            "Wrong / missing match reported from the detect screen.\n\nDETAILS",
            detectionReportMessage("DETAILS", "   "),
        )
    }

    @Test fun messageIsCutToTheApisLimit() {
        assertEquals(2000, detectionReportMessage("DETAILS", "x".repeat(REPORT_NOTE_MAX * 3)).length)
    }
}
