package com.birddex.app.ui.detect

import com.birddex.app.data.detect.Detection
import java.text.NumberFormat
import java.util.Locale

/** The website's `formatLabel`: "house_sparrow" → "House Sparrow". */
fun labelToName(label: String): String =
    label.split('_').joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

/** A match's bird name: from the server's bird row, or made from the label if it sent none. */
fun Detection.nameOf(label: String): String = birds[label]?.name ?: labelToName(label)

/**
 * A confidence percentage with up to two decimals, as the website shows it ("91.37%",
 * "4.2%", "100%"), but with the phone's own decimal mark and percent style.
 */
fun formatConfidence(percent: Double, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = 2 }.format(percent / 100)
