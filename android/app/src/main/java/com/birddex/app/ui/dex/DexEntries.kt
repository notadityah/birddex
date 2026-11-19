package com.birddex.app.ui.dex

import com.birddex.app.data.birds.Bird
import com.birddex.app.data.sightings.Sighting

/** One card in the dex grid, with everything the bird detail sheet needs. */
data class DexEntry(
    val bird: Bird,
    /** The user's sightings of this bird, newest first. */
    val sightings: List<Sighting>,
) {
    val sightingCount: Int get() = sightings.size
    val found: Boolean get() = sightings.isNotEmpty()

    /** Newest sighting that has a photo; shown on the card and the detail sheet. */
    val photo: Sighting? get() = sightings.firstOrNull { it.imageKey != null }
}

/** The website's filter chips (MyDexPage.vue): All / Found / Not Found. */
enum class DexFilter { All, Found, NotFound }

/** Pairs every species with the user's sightings of it, keeping the birds' order (by name). */
fun buildDexEntries(birds: List<Bird>, sightings: List<Sighting>): List<DexEntry> {
    // ISO-8601 timestamps compare correctly as text.
    val byBird = sightings.sortedByDescending { it.createdAt }.groupBy { it.birdId }
    return birds.map { bird -> DexEntry(bird = bird, sightings = byBird[bird.id].orEmpty()) }
}

fun List<DexEntry>.filteredBy(filter: DexFilter): List<DexEntry> = when (filter) {
    DexFilter.All -> this
    DexFilter.Found -> filter { it.found }
    DexFilter.NotFound -> filter { !it.found }
}

/** Rounded down, so the dex never shows 100% until every bird is found. */
fun completionPercent(found: Int, total: Int): Int = if (total == 0) 0 else found * 100 / total
