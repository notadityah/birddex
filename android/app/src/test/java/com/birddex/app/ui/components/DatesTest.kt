package com.birddex.app.ui.components

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class DatesTest {

    private val sydney = ZoneId.of("Australia/Sydney")
    private val australia = Locale.forLanguageTag("en-AU")

    @Test fun longDateInTheUsersLanguage() {
        val seen = Instant.parse("2026-11-18T03:12:45Z")
        assertEquals("18 November 2026", formatLongDate(seen, sydney, australia))
        assertEquals("November 18, 2026", formatLongDate(seen, ZoneId.of("UTC"), Locale.US))
    }

    @Test fun usesThePhonesTimeZoneNotUtc() {
        // 8 pm UTC on the 18th is already the morning of the 19th in Sydney (AEDT, UTC+11).
        val lateUtc = Instant.parse("2026-11-18T20:00:00Z")
        assertEquals("19 November 2026", formatLongDate(lateUtc, sydney, australia))
        assertEquals("18 November 2026", formatLongDate(lateUtc, ZoneId.of("UTC"), australia))
    }
}
