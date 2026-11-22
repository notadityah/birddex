package com.birddex.app.data.birds

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.birddex.app.data.local.BirdDexDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The real Room database on a device/emulator (in memory, so nothing touches the app's data). */
@RunWith(AndroidJUnit4::class)
class BirdDaoTest {

    private lateinit var db: BirdDexDatabase
    private lateinit var dao: BirdDao

    private fun bird(id: Int, name: String, slug: String = name.lowercase().replace(' ', '_')) =
        BirdEntity(id, name, "Avis $id", slug, "1 cm", null, null, "Least Concern", null, null, null, null, "rare", null)

    @Before fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, BirdDexDatabase::class.java).build()
        dao = db.birdDao()
    }

    @After fun tearDown() = db.close()

    @Test fun replaceAllStoresAndSortsByName() = runTest {
        dao.replaceAll(listOf(bird(2, "Zebra Finch"), bird(1, "Australian Magpie")))

        val rows = dao.observeAll().first()
        assertEquals(listOf("Australian Magpie", "Zebra Finch"), rows.map { it.name })
        assertEquals(bird(1, "Australian Magpie"), rows.first()) // every column round-trips
    }

    @Test fun replaceAllRemovesBirdsThatAreGone() = runTest {
        dao.replaceAll(listOf(bird(1, "Australian Magpie"), bird(2, "Zebra Finch")))
        dao.replaceAll(listOf(bird(2, "Zebra Finch")))

        assertEquals(listOf(2), dao.observeAll().first().map { it.id })
    }

    @Test fun duplicateSlugRollsBackTheWholeReplace() = runTest {
        dao.replaceAll(listOf(bird(1, "Australian Magpie")))

        val failed = runCatching {
            dao.replaceAll(listOf(bird(2, "A", slug = "same"), bird(3, "B", slug = "same")))
        }

        assertTrue(failed.isFailure)
        // The transaction kept the old list instead of leaving the table empty.
        assertEquals(listOf("Australian Magpie"), dao.observeAll().first().map { it.name })
    }
}
