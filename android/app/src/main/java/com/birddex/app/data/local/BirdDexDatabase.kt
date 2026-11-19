package com.birddex.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.birddex.app.data.birds.BirdDao
import com.birddex.app.data.birds.BirdEntity

/**
 * The app's local database (file "birddex.db"). Bump [version] and add a migration whenever a
 * table changes; Room writes each version's schema to android/app/schemas/.
 */
@Database(entities = [BirdEntity::class], version = 1, exportSchema = true)
abstract class BirdDexDatabase : RoomDatabase() {
    abstract fun birdDao(): BirdDao
}
