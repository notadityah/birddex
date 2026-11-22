package com.birddex.app.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.birddex.app.data.sightings.SightingEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Database schema changes and the sightings table, on a real device/emulator. */
@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val migrations = MigrationTestHelper(instrumentation, BirdDexDatabase::class.java)

    @Test fun migrate1To2KeepsSavedBirdsAndAddsSightings() = runTest {
        val name = "migration-test.db"
        // A v1 database with one saved bird, written with the v1 schema in app/schemas/.
        migrations.createDatabase(name, 1).apply {
            execSQL(
                "INSERT INTO birds (id, name, scientificName, slug, rarity) " +
                    "VALUES (1, 'Australian King Parrot', 'Alisterus scapularis', 'australian_king_parrot', 'uncommon')",
            )
            close()
        }

        // Runs the automatic migration and checks the result matches the v2 schema exactly.
        migrations.runMigrationsAndValidate(name, 2, true).close()

        val db = Room.databaseBuilder(instrumentation.targetContext, BirdDexDatabase::class.java, name).build()
        try {
            assertEquals(listOf("australian_king_parrot"), db.birdDao().observeAll().first().map { it.slug })
            assertTrue(db.sightingDao().observeAll().first().isEmpty())
        } finally {
            db.close()
            // Tests run inside the app's process, so also remove the lock file deleteDatabase leaves.
            val context = instrumentation.targetContext
            context.deleteDatabase(name)
            java.io.File(context.getDatabasePath(name).path + ".lck").delete()
        }
    }

    @Test fun sightingsRoundTripNewestFirst() = runTest {
        val db = Room.inMemoryDatabaseBuilder(instrumentation.targetContext, BirdDexDatabase::class.java).build()
        try {
            val dao = db.sightingDao()
            val older = SightingEntity("a", 1, "australian_king_parrot", "Australian King Parrot", "images/a.jpg",
                "https://example/a.jpg", "2026-11-01T06:30:00.000Z", "notes", "2026-11-01T06:31:00.000Z", true)
            val newer = older.copy(id = "b", imageKey = null, imageUrl = null, detectedAt = null, notes = null,
                createdAt = "2026-11-18T03:13:02.123Z", isPublic = false)

            dao.replaceAll(listOf(older, newer))
            assertEquals(listOf(newer, older), dao.observeAll().first())

            dao.setPublic("b", true) // share toggle changes only that row
            assertEquals(listOf(true, true), dao.observeAll().first().map { it.isPublic })
            dao.setPublic("a", false)
            assertEquals(listOf(true, false), dao.observeAll().first().map { it.isPublic })

            dao.delete("b") // delete one
            assertEquals(listOf("a"), dao.observeAll().first().map { it.id })

            dao.replaceAll(emptyList()) // a user with no sightings
            assertTrue(dao.observeAll().first().isEmpty())
        } finally {
            db.close()
        }
    }
}
