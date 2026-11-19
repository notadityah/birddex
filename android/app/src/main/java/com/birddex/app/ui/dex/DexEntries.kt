package com.birddex.app.ui.dex

import com.birddex.app.data.birds.Bird
import com.birddex.app.data.sightings.Sighting

/** One card in the dex grid. */
data class DexEntry(
    val bird: Bird,
    val sightingCount: Int,
    /** Newest sighting of this bird that has a photo; shown on the card when found. */
    val photo: Sighting?,
) {
    val found: Boolean get() = sightingCount > 0
}

/** The website's filter chips (MyDexPage.vue): All / Found / Not Found. */
enum class DexFilter { All, Found, NotFound }

/** Pairs every species with the user's sightings of it, keeping the birds' order (by name). */
fun buildDexEntries(birds: List<Bird>, sightings: List<Sighting>): List<DexEntry> {
    val byBird = sightings.groupBy { it.birdId }
    return birds.map { bird ->
        val mine = byBird[bird.id].orEmpty()
        DexEntry(
            bird = bird,
            sightingCount = mine.size,
            // ISO-8601 timestamps compare correctly as text.
            photo = mine.filter { it.imageKey != null }.maxByOrNull { it.createdAt },
        )
    }
}

fun List<DexEntry>.filteredBy(filter: DexFilter): List<DexEntry> = when (filter) {
    DexFilter.All -> this
    DexFilter.Found -> filter { it.found }
    DexFilter.NotFound -> filter { !it.found }
}

/** Rounded down, so the dex never shows 100% until every bird is found. */
fun completionPercent(found: Int, total: Int): Int = if (total == 0) 0 else found * 100 / total
