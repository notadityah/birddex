package com.birddex.app.data.detect

import com.birddex.app.data.birds.Bird
import com.birddex.app.data.network.ApiResult
import com.birddex.app.data.network.BirdDexApi
import com.birddex.app.data.network.apiCall
import java.util.Base64
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class DetectRequest(val imageBase64: String, val topN: Int)

/** One match from the model (`backend/lambda/detect/index.ts`, `parseDetections`). */
@Serializable
data class Prediction(
    /** The bird's slug: the model's class label (`backend/classes.txt`). */
    val label: String,
    /** Percent, 0–100, rounded to two decimals by the server. */
    val confidence: Double,
)

/**
 * `POST /api/detect`'s answer: the matches, best first (empty when no bird scored at least
 * 25%), plus every matched bird's full stats so another match can be picked without asking
 * the server again (website: `birdsBySlug` in DetectPage.vue).
 */
@Serializable
data class Detection(
    val predictions: List<Prediction> = emptyList(),
    /** The top match's bird, or null when nothing was detected. */
    @SerialName("bird") val bestMatch: Bird? = null,
    /** Keyed by slug. */
    val birds: Map<String, Bird> = emptyMap(),
)

interface DetectRepository {
    /** Identifies the bird in a prepared JPEG (`PreparedPhoto.detectJpeg`). */
    suspend fun detect(jpeg: ByteArray): ApiResult<Detection>
}

class NetworkDetectRepository @Inject constructor(
    private val api: BirdDexApi,
    private val json: Json,
) : DetectRepository {

    override suspend fun detect(jpeg: ByteArray): ApiResult<Detection> = apiCall(json) {
        api.detect(DetectRequest(imageBase64 = Base64.getEncoder().encodeToString(jpeg), topN = TOP_N))
    }

    companion object {
        /** The website asks for 5: enough to pick the right bird without crowding the screen. */
        const val TOP_N = 5
    }
}
