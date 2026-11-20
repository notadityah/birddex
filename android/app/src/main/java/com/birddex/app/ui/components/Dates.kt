package com.birddex.app.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * A long, localised date in the phone's time zone, e.g. "18 November 2026" (en-AU) or
 * "November 18, 2026" (en-US); the website's formatDateLong() does the same in the browser.
 */
fun formatLongDate(
    instant: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).withZone(zone).format(instant)

/** How long ago something happened, in the website's steps (`relativeDate` in utils/dateFormat.js). */
sealed interface RelativeDate {
    data object Today : RelativeDate
    data object Yesterday : RelativeDate
    data class DaysAgo(val days: Long) : RelativeDate
    data class WeeksAgo(val weeks: Long) : RelativeDate
    data class MonthsAgo(val months: Long) : RelativeDate
    data class YearsAgo(val years: Long) : RelativeDate
}

/**
 * Whole 24-hour periods between [instant] and [now], like the website (not calendar days):
 * under 7 days, then weeks, 30-day months and 365-day years. A time slightly in the future
 * (a fast server clock) counts as today rather than the website's "-1 days ago".
 */
fun relativeDate(instant: Instant, now: Instant): RelativeDate {
    val days = java.time.Duration.between(instant, now).toDays().coerceAtLeast(0)
    return when {
        days == 0L -> RelativeDate.Today
        days == 1L -> RelativeDate.Yesterday
        days < 7 -> RelativeDate.DaysAgo(days)
        days < 30 -> RelativeDate.WeeksAgo(days / 7)
        days < 365 -> RelativeDate.MonthsAgo(days / 30)
        else -> RelativeDate.YearsAgo(days / 365)
    }
}

/** Month and year in the phone's language, e.g. "November 2026" (website: "Member since"). */
fun formatMonthYear(
    instant: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String = DateTimeFormatter.ofPattern("LLLL yyyy", locale).withZone(zone).format(instant)
