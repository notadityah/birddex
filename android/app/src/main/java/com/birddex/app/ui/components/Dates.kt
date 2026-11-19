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
