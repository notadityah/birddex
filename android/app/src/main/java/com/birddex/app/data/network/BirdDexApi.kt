package com.birddex.app.data.network

import com.birddex.app.data.account.AccountSettings
import com.birddex.app.data.birds.Bird
import com.birddex.app.data.detect.DetectRequest
import com.birddex.app.data.detect.Detection
import com.birddex.app.data.feedback.FeedbackCreated
import com.birddex.app.data.feedback.FeedbackRequest
import com.birddex.app.data.gallery.GalleryPage
import com.birddex.app.data.sightings.CreateSightingRequest
import com.birddex.app.data.sightings.CreatedSighting
import com.birddex.app.data.sightings.DeletedSightings
import com.birddex.app.data.sightings.Sighting
import kotlinx.serialization.Serializable
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/** BirdDex REST API (`backend/lambda/api/index.ts`). Paths are relative to BuildConfig.BASE_URL. */
interface BirdDexApi {

    /** All 36 birds with their Pokédex stats, sorted by name. Public, no sign-in needed. */
    @GET("api/birds")
    suspend fun getBirds(): List<Bird>

    /** The signed-in user's sightings, newest first, with 1-hour photo links. 401 when signed out. */
    @GET("api/sightings")
    suspend fun getSightings(): List<Sighting>

    /**
     * Records a sighting (201). With `imageExt`, the answer carries a presigned S3 `uploadUrl`
     * (valid 5 minutes) for [uploadPhoto]; the row exists from now on, photo or not.
     */
    @POST("api/sightings")
    suspend fun createSighting(@Body body: CreateSightingRequest): CreatedSighting

    /**
     * Files feedback for the admins (201). With `imageExt`, the answer carries a presigned
     * `uploadUrl` for [uploadPhoto], as for sightings. 400 if the message is empty or too long.
     */
    @POST("api/feedback")
    suspend fun sendFeedback(@Body body: FeedbackRequest): FeedbackCreated

    /**
     * Uploads a sighting or feedback photo straight to S3 with the presigned URL from
     * [createSighting] or [sendFeedback].
     * Not a BirdDex endpoint: no cookies match S3's host, and the /api/ interceptors skip it.
     */
    @PUT
    suspend fun uploadPhoto(@Url uploadUrl: String, @Body photo: RequestBody)

    /**
     * The Community Gallery: shared sightings, newest first, with 1-hour photo links. The
     * server caps [limit] at 100; [birdId] filters to one species. 401 when signed out.
     */
    @GET("api/gallery")
    suspend fun getGallery(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("birdId") birdId: Int?,
    ): GalleryPage

    /** Shares (true) or unshares a sighting in the public gallery. 404 if it isn't the user's. */
    @PATCH("api/sightings/{id}")
    suspend fun setSightingPublic(@Path("id") id: String, @Body body: SetPublicRequest)

    /** Permanently deletes every sighting the user has, and their photos ("Reset Collection"). */
    @DELETE("api/sightings")
    suspend fun deleteAllSightings(): DeletedSightings

    /** The user's account settings (gallery anonymity). 401 when signed out. */
    @GET("api/account/settings")
    suspend fun getAccountSettings(): AccountSettings

    /** Saves the account settings; answers with what was saved. 400 if the flag isn't a boolean. */
    @PATCH("api/account/settings")
    suspend fun updateAccountSettings(@Body settings: AccountSettings): AccountSettings

    /** Permanently deletes a sighting and its photo. 404 if it isn't the user's (or already gone). */
    @DELETE("api/sightings/{id}")
    suspend fun deleteSighting(@Path("id") id: String)

    /**
     * Identifies the bird in a photo (base64 JPEG). Runs the model on the server, which can
     * take 10–15 s when it has to load first; the client's timeouts allow for that. 401 when
     * signed out.
     */
    @POST("api/detect")
    suspend fun detect(@Body body: DetectRequest): Detection
}

@Serializable
data class SetPublicRequest(val public: Boolean)
