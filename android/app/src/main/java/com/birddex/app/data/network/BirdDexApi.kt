package com.birddex.app.data.network

import com.birddex.app.data.birds.Bird
import com.birddex.app.data.sightings.Sighting
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

/** BirdDex REST API (`backend/lambda/api/index.ts`). Paths are relative to BuildConfig.BASE_URL. */
interface BirdDexApi {

    /** All 36 birds with their Pokédex stats, sorted by name. Public, no sign-in needed. */
    @GET("api/birds")
    suspend fun getBirds(): List<Bird>

    /** The signed-in user's sightings, newest first, with 1-hour photo links. 401 when signed out. */
    @GET("api/sightings")
    suspend fun getSightings(): List<Sighting>

    /** Shares (true) or unshares a sighting in the public gallery. 404 if it isn't the user's. */
    @PATCH("api/sightings/{id}")
    suspend fun setSightingPublic(@Path("id") id: String, @Body body: SetPublicRequest)

    /** Permanently deletes a sighting and its photo. 404 if it isn't the user's (or already gone). */
    @DELETE("api/sightings/{id}")
    suspend fun deleteSighting(@Path("id") id: String)
}

@Serializable
data class SetPublicRequest(val public: Boolean)
