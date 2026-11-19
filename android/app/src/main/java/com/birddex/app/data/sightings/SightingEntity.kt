package com.birddex.app.data.sightings

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Offline copy of one of the signed-in user's [Sighting]s. Personal data: the whole table is
 * wiped on sign-out (UserDataCleaner), and the database is excluded from backups.
 */
@Entity(tableName = "sightings", indices = [Index("birdId")])
data class SightingEntity(
    @PrimaryKey val id: String,
    val birdId: Int,
    val slug: String,
    val birdName: String,
    val imageKey: String?,
    /** Presigned link from the last refresh; expires after an hour. Images cache by [imageKey]. */
    val imageUrl: String?,
    val detectedAt: String?,
    val notes: String?,
    val createdAt: String,
    val isPublic: Boolean,
)

fun SightingEntity.toSighting() = Sighting(
    id = id,
    birdId = birdId,
    slug = slug,
    birdName = birdName,
    imageKey = imageKey,
    imageUrl = imageUrl,
    detectedAt = detectedAt,
    notes = notes,
    createdAt = createdAt,
    publicValue = isPublic,
)

fun Sighting.toEntity() = SightingEntity(
    id = id,
    birdId = birdId,
    slug = slug,
    birdName = birdName,
    imageKey = imageKey,
    imageUrl = imageUrl,
    detectedAt = detectedAt,
    notes = notes,
    createdAt = createdAt,
    isPublic = isPublic,
)

@Dao
interface SightingDao {

    /** Newest first, like the API (ISO-8601 timestamps sort correctly as text). */
    @Query("SELECT * FROM sightings ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SightingEntity>>

    @Query("DELETE FROM sightings")
    suspend fun deleteAll()

    @Query("UPDATE sightings SET isPublic = :isPublic WHERE id = :id")
    suspend fun setPublic(id: String, isPublic: Boolean)

    @Query("DELETE FROM sightings WHERE id = :id")
    suspend fun delete(id: String)

    @Insert
    suspend fun insertAll(sightings: List<SightingEntity>)

    /** Swaps the whole list atomically (an empty list is valid: no sightings yet). */
    @Transaction
    suspend fun replaceAll(sightings: List<SightingEntity>) {
        deleteAll()
        insertAll(sightings)
    }
}
