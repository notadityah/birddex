package com.birddex.app.data.birds

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One species with its Pokédex stats, as `GET /api/birds` returns it (`bird` table in
 * backend/lambda/migrate/schema.sql). Only id/name/scientific name/slug are guaranteed;
 * the stats were added later and may be null.
 */
@Serializable
data class Bird(
    val id: Int,
    val name: String,
    @SerialName("scientific_name") val scientificName: String,
    /** Stable key; also the detection model's class label (backend/classes.txt). */
    val slug: String,
    val wingspan: String? = null,
    val length: String? = null,
    val weight: String? = null,
    @SerialName("conservation_status") val conservationStatus: String? = null,
    val habitat: String? = null,
    val diet: String? = null,
    @SerialName("call_description") val callDescription: String? = null,
    @SerialName("fun_fact") val funFact: String? = null,
    /** Raw value from the API; use [rarityLevel]. */
    @SerialName("rarity") val rarityValue: String? = null,
    val appearance: String? = null,
) {
    val rarityLevel: Rarity get() = Rarity.from(rarityValue)
}

/** Matches the website's rarity badges (BirdStatsCard.vue). */
enum class Rarity {
    Common,
    Uncommon,
    Rare,
    ;

    companion object {
        /** Unknown or missing values count as common, the database default. */
        fun from(value: String?): Rarity = when (value?.trim()?.lowercase()) {
            "rare" -> Rare
            "uncommon" -> Uncommon
            else -> Common
        }
    }
}
