package com.birddex.app.ui.dex

import com.birddex.app.data.birds.Bird
import com.birddex.app.data.sightings.Sighting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DexEntriesTest {

    private val parrot = Bird(1, "Australian King Parrot", "Alisterus scapularis", "australian_king_parrot")
    private val magpie = Bird(2, "Australian Magpie", "Gymnorhina tibicen", "australian_magpie")
    private val raven = Bird(3, "Australian Raven", "Corvus coronoides", "australian_raven")

    private fun sighting(id: String, birdId: Int, createdAt: String, imageKey: String? = null) =
        Sighting(id = id, birdId = birdId, slug = "x", birdName = "x", imageKey = imageKey, createdAt = createdAt)

    @Test fun foundMeansAtLeastOneSighting() {
        val entries = buildDexEntries(
            listOf(parrot, magpie, raven),
            listOf(
                sighting("a", 1, "2026-11-01T00:00:00Z"),
                sighting("b", 1, "2026-11-02T00:00:00Z"),
                sighting("c", 3, "2026-11-03T00:00:00Z"),
            ),
        )

        assertEquals(listOf(parrot, magpie, raven), entries.map { it.bird }) // order kept
        assertEquals(listOf(2, 0, 1), entries.map { it.sightingCount })
        assertEquals(listOf(true, false, true), entries.map { it.found })
    }

    @Test fun photoIsTheNewestSightingThatHasOne() {
        val entry = buildDexEntries(
            listOf(parrot),
            listOf(
                sighting("old-photo", 1, "2026-11-01T00:00:00Z", imageKey = "images/old.jpg"),
                sighting("new-photo", 1, "2026-11-05T00:00:00Z", imageKey = "images/new.jpg"),
                // Newest overall but no photo: the website would show the placeholder here.
                sighting("newest-no-photo", 1, "2026-11-09T00:00:00Z"),
            ),
        ).single()

        assertEquals("new-photo", entry.photo?.id)
    }

    @Test fun foundWithoutAnyPhoto() {
        val entry = buildDexEntries(listOf(parrot), listOf(sighting("a", 1, "2026-11-01T00:00:00Z"))).single()
        assertTrue(entry.found)
        assertNull(entry.photo)
    }

    @Test fun sightingsOfUnknownBirdsAreIgnored() {
        val entries = buildDexEntries(listOf(parrot), listOf(sighting("a", 99, "2026-11-01T00:00:00Z")))
        assertFalse(entries.single().found)
    }

    @Test fun filters() {
        val entries = buildDexEntries(listOf(parrot, magpie), listOf(sighting("a", 1, "2026-11-01T00:00:00Z")))

        assertEquals(listOf(parrot, magpie), entries.filteredBy(DexFilter.All).map { it.bird })
        assertEquals(listOf(parrot), entries.filteredBy(DexFilter.Found).map { it.bird })
        assertEquals(listOf(magpie), entries.filteredBy(DexFilter.NotFound).map { it.bird })
    }

    @Test fun completionRoundsDownAndNeverShows100Early() {
        assertEquals(0, completionPercent(0, 36))
        assertEquals(5, completionPercent(2, 36)) // 5.55… → 5
        assertEquals(97, completionPercent(35, 36)) // 97.2… → 97, not 100
        assertEquals(100, completionPercent(36, 36))
        assertEquals(0, completionPercent(0, 0)) // no birds loaded yet
    }
}
