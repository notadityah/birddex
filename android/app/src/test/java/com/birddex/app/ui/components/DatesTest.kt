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

    // --- relative dates (gallery cards) ---

    private val now = Instant.parse("2026-11-20T09:00:00Z")
    private fun ago(days: Long, hours: Long = 0) = relativeDate(now.minusSeconds((days * 24 + hours) * 3600), now)

    @Test fun relativeDateStepsMatchTheWebsite() {
        assertEquals(RelativeDate.Today, ago(0, hours = 23)) // whole 24-hour periods, not calendar days
        assertEquals(RelativeDate.Yesterday, ago(1))
        assertEquals(RelativeDate.DaysAgo(2), ago(2))
        assertEquals(RelativeDate.DaysAgo(6), ago(6, hours = 23))
        assertEquals(RelativeDate.WeeksAgo(1), ago(7))
        assertEquals(RelativeDate.WeeksAgo(4), ago(29))
        assertEquals(RelativeDate.MonthsAgo(1), ago(30))
        assertEquals(RelativeDate.MonthsAgo(12), ago(364))
        assertEquals(RelativeDate.YearsAgo(1), ago(365))
        assertEquals(RelativeDate.YearsAgo(2), ago(800))
    }

    @Test fun memberSinceShowsMonthAndYear() {
        val joined = Instant.parse("2026-02-28T20:00:00Z") // already March 1 in Sydney
        assertEquals("March 2026", formatMonthYear(joined, sydney, australia))
        assertEquals("February 2026", formatMonthYear(joined, ZoneId.of("UTC"), Locale.US))
    }

    @Test fun aSlightlyFutureTimeIsToday() {
        assertEquals(RelativeDate.Today, relativeDate(now.plusSeconds(90), now))
    }
}
