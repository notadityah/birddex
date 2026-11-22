package com.birddex.app.data.birds

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Offline copy of one [Bird]. Column names follow the API so the two stay easy to compare. */
@Entity(tableName = "birds", indices = [Index(value = ["slug"], unique = true)])
data class BirdEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val scientificName: String,
    val slug: String,
    val wingspan: String?,
    val length: String?,
    val weight: String?,
    val conservationStatus: String?,
    val habitat: String?,
    val diet: String?,
    val callDescription: String?,
    val funFact: String?,
    val rarity: String?,
    val appearance: String?,
)

fun BirdEntity.toBird() = Bird(
    id = id,
    name = name,
    scientificName = scientificName,
    slug = slug,
    wingspan = wingspan,
    length = length,
    weight = weight,
    conservationStatus = conservationStatus,
    habitat = habitat,
    diet = diet,
    callDescription = callDescription,
    funFact = funFact,
    rarityValue = rarity,
    appearance = appearance,
)

fun Bird.toEntity() = BirdEntity(
    id = id,
    name = name,
    scientificName = scientificName,
    slug = slug,
    wingspan = wingspan,
    length = length,
    weight = weight,
    conservationStatus = conservationStatus,
    habitat = habitat,
    diet = diet,
    callDescription = callDescription,
    funFact = funFact,
    rarity = rarityValue,
    appearance = appearance,
)

@Dao
interface BirdDao {

    /** Sorted like the API (by name). Emits again whenever the table changes. */
    @Query("SELECT * FROM birds ORDER BY name")
    fun observeAll(): Flow<List<BirdEntity>>

    @Query("DELETE FROM birds")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(birds: List<BirdEntity>)

    /** Swaps the whole list atomically, so birds removed on the server disappear here too. */
    @Transaction
    suspend fun replaceAll(birds: List<BirdEntity>) {
        deleteAll()
        insertAll(birds)
    }
}
