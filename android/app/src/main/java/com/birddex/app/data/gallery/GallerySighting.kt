package com.birddex.app.data.gallery

import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A sighting someone shared to the Community Gallery, as `GET /api/gallery` returns it. There
 * is deliberately no image key: keys contain the owner's user id, which would undo
 * "Anonymous". So photos are cached by [id] instead (see [photoCacheKey]).
 */
@Serializable
data class GallerySighting(
    val id: String,
    @SerialName("bird_id") val birdId: Int,
    @SerialName("bird_name") val birdName: String,
    @SerialName("scientific_name") val scientificName: String? = null,
    val slug: String,
    @SerialName("detected_at") val detectedAt: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String,
    /** The sharer's name, or "Anonymous" if they chose that in their account settings. */
    @SerialName("user_name") val userName: String? = null,
    /** Presigned S3 link, valid for an hour. */
    @SerialName("image_url") val imageUrl: String? = null,
) {
    /** When it was shared (the gallery's order, and the website's "2 days ago"). */
    val sharedAt: Instant? get() = parseInstant(createdAt)

    /** When the bird was seen, falling back to when it was shared (the detail view's date). */
    val seenAt: Instant? get() = parseInstant(detectedAt) ?: sharedAt

    /** Stable across fetches, unlike [imageUrl], so Coil's cache keeps working. */
    val photoCacheKey: String get() = "gallery/$id"
}

/** One page of the gallery. */
@Serializable
data class GalleryPage(
    val sightings: List<GallerySighting> = emptyList(),
    /** How many shared sightings there are in all (for this filter). */
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
)

private fun parseInstant(value: String?): Instant? = value?.let { runCatching { Instant.parse(it) }.getOrNull() }
