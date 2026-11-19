package com.birddex.app.data.birds

import com.birddex.app.data.network.ApiClients
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BirdImagesTest {

    /** The live list of 36 birds (GET /api/birds), as saved for the bird repository tests. */
    private val birds: List<Bird> = ApiClients.json.decodeFromString(
        javaClass.classLoader!!.getResource("fixtures/birds.json")!!.readText(Charsets.UTF_8),
    )

    @Test fun everyBirdHasACreditedPhoto() {
        assertEquals(36, birds.size)
        val missing = birds.map { it.slug }.filter { BirdImages.credit(it) == null }
        assertTrue("No photo credit for $missing", missing.isEmpty())
    }

    @Test fun creditsLinkToWikimediaUnderACreativeCommonsLicence() {
        birds.mapNotNull { BirdImages.credit(it.slug) }.forEach { credit ->
            assertTrue(credit.author.isNotBlank())
            // CC BY / CC BY-SA, plus one public-domain (CC0) photo, credited all the same.
            assertTrue(credit.license, credit.license.startsWith("CC BY") || credit.license == "CC0")
            assertTrue(credit.sourceUrl, credit.sourceUrl.startsWith("https://commons.wikimedia.org/wiki/File:"))
        }
    }

    @Test fun photoUrlIsTheWebsitesOwn() {
        assertEquals("https://birddex.fun/birds/house_sparrow.jpg", BirdImages.url("https://birddex.fun", "house_sparrow"))
        assertEquals("http://10.0.2.2:5173/birds/galah.jpg", BirdImages.url("http://10.0.2.2:5173/", "galah"))
    }

    @Test fun unknownBirdHasNoCredit() {
        assertNull(BirdImages.credit("removed_bird"))
    }
}
