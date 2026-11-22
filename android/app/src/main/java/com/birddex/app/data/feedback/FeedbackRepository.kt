package com.birddex.app.data.feedback

import com.birddex.app.data.network.ApiError
import com.birddex.app.data.network.ApiResult
import com.birddex.app.data.network.BirdDexApi
import com.birddex.app.data.network.apiCall
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/** The `category` values `POST /api/feedback` accepts (`FEEDBACK_CATEGORIES` in the API). */
enum class FeedbackCategory(val apiValue: String) { Bug("bug"), Detection("detection"), Suggestion("suggestion"), Other("other") }

@Serializable
data class FeedbackRequest(
    val category: String,
    /** At most [FeedbackRepository.MAX_MESSAGE] characters after trimming. */
    val message: String,
    /** Where it was sent from; shown to admins as plain text. */
    val pageUrl: String? = null,
    /** Asks for a photo upload link; left out when there's no photo. */
    val imageExt: String? = null,
)

@Serializable
data class FeedbackCreated(val id: String, val uploadUrl: String? = null)

/** A report the server accepted. [photoUploaded] is false if its photo couldn't be sent. */
data class FeedbackSent(val photoUploaded: Boolean)

/**
 * Feedback to the BirdDex team, read in the admin panel: wrong-detection reports from
 * Detect and the general feedback form on the account tab.
 */
interface FeedbackRepository {
    /**
     * Sends a report, then its optional photo (a JPEG) to the upload link it gets back.
     * Users can't delete feedback, so a failed photo upload still counts as sent: the text
     * is what matters, and trying again would file the same report twice.
     */
    suspend fun send(category: FeedbackCategory, message: String, page: String, photoJpeg: ByteArray?): ApiResult<FeedbackSent>

    companion object {
        /** The API rejects longer messages. */
        const val MAX_MESSAGE = 2000

        /** Marks reports from the app; the website sends its route (e.g. `/detect`). */
        fun appPage(route: String): String = "android:$route"
    }
}

class NetworkFeedbackRepository @Inject constructor(
    private val api: BirdDexApi,
    private val json: Json,
) : FeedbackRepository {

    override suspend fun send(
        category: FeedbackCategory,
        message: String,
        page: String,
        photoJpeg: ByteArray?,
    ): ApiResult<FeedbackSent> {
        val request = FeedbackRequest(
            category = category.apiValue,
            message = message.take(FeedbackRepository.MAX_MESSAGE),
            pageUrl = page,
            imageExt = photoJpeg?.let { "jpg" },
        )
        val created = when (val result = apiCall(json) { api.sendFeedback(request) }) {
            is ApiResult.Failure -> return result
            is ApiResult.Success -> result.value
        }
        if (photoJpeg == null) return ApiResult.Success(FeedbackSent(photoUploaded = true))
        val uploaded = created.uploadUrl?.let { url ->
            apiCall(json) { api.uploadPhoto(url, photoJpeg.toRequestBody(JPEG)) }
        } ?: ApiResult.Failure(ApiError.Unexpected("No upload URL"))
        return ApiResult.Success(FeedbackSent(photoUploaded = uploaded is ApiResult.Success))
    }

    private companion object {
        val JPEG = "image/jpeg".toMediaType()
    }
}
