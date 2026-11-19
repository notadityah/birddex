package com.birddex.app.data.network

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.GET

/** BirdDex REST API (`backend/lambda/api/index.ts`). Paths are relative to BuildConfig.BASE_URL. */
interface BirdDexApi {

    /** All birds with their Pokédex stats. Public, no sign-in needed. Typed in A3.1. */
    @GET("api/birds")
    suspend fun getBirds(): JsonArray

    /** The signed-in user's sightings; 401 when signed out. Typed in A3.3. */
    @GET("api/sightings")
    suspend fun getSightings(): Response<JsonElement>
}
