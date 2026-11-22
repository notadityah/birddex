package com.birddex.app.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.birddex.app.data.birds.BirdDao
import com.birddex.app.data.birds.BirdEntity
import com.birddex.app.data.sightings.SightingDao
import com.birddex.app.data.sightings.SightingEntity

/**
 * The app's local database (file "birddex.db"). Bump [version] and add a migration whenever a
 * table changes; Room writes each version's schema to android/app/schemas/.
 *
 * - v1: birds
 * - v2: + sightings (automatic migration: new table only)
 */
@Database(
    entities = [BirdEntity::class, SightingEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class BirdDexDatabase : RoomDatabase() {
    abstract fun birdDao(): BirdDao
    abstract fun sightingDao(): SightingDao

    companion object {
        const val FILE_NAME = "birddex.db"
    }
}
