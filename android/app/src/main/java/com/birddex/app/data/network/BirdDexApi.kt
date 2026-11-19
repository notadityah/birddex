package com.birddex.app.data.network

import com.birddex.app.data.birds.Bird
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.GET

/** BirdDex REST API (`backend/lambda/api/index.ts`). Paths are relative to BuildConfig.BASE_URL. */
interface BirdDexApi {

    /** All 36 birds with their Pokédex stats, sorted by name. Public, no sign-in needed. */
    @GET("api/birds")
    suspend fun getBirds(): List<Bird>

    /** The signed-in user's sightings; 401 when signed out. Typed in A3.3. */
    @GET("api/sightings")
    suspend fun getSightings(): Response<JsonElement>
}
