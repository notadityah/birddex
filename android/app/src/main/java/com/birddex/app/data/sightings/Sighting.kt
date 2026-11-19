package com.birddex.app.data.sightings

import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One of the signed-in user's sightings, as `GET /api/sightings` returns it
 * (`sighting` table joined with `bird`, backend/lambda/api/index.ts).
 */
@Serializable
data class Sighting(
    /** UUID. */
    val id: String,
    @SerialName("bird_id") val birdId: Int,
    val slug: String,
    @SerialName("bird_name") val birdName: String,
    /**
     * Stable S3 key of the photo. Use it as the image cache key: [imageUrl] is a presigned
     * link that changes on every fetch and expires after an hour.
     */
    @SerialName("image_key") val imageKey: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    /** ISO-8601 instant when the bird was seen, if the user gave one. */
    @SerialName("detected_at") val detectedAt: String? = null,
    val notes: String? = null,
    /** ISO-8601 instant when the sighting was saved. */
    @SerialName("created_at") val createdAt: String,
    /** Raw value (the column is nullable); use [isPublic]. */
    @SerialName("public") val publicValue: Boolean? = null,
) {
    /** Shared in the community gallery. */
    val isPublic: Boolean get() = publicValue == true

    /** When it was seen: [detectedAt], or [createdAt] if the user didn't give a time. */
    val seenAt: Instant? get() = parseInstant(detectedAt) ?: parseInstant(createdAt)
}

private fun parseInstant(value: String?): Instant? =
    value?.let { runCatching { Instant.parse(it) }.getOrNull() }

/** `POST /api/sightings` body. `detectedAt` is left out: the server uses now, as for the website. */
@Serializable
data class CreateSightingRequest(
    val birdId: Int,
    /** Asks for a photo upload link; the app always uploads a JPEG. */
    val imageExt: String,
    /** Left out when null (the website sends none for an empty note). */
    val notes: String? = null,
    val public: Boolean,
)

/** `POST /api/sightings` answer (201). */
@Serializable
data class CreatedSighting(
    val id: String,
    val imageKey: String? = null,
    /** Presigned S3 PUT link, valid 5 minutes. */
    val uploadUrl: String? = null,
)
