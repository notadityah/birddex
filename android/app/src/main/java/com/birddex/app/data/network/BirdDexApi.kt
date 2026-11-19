package com.birddex.app.data.network

import kotlinx.serialization.json.JsonArray
import retrofit2.http.GET

/** BirdDex REST API (`backend/lambda/api/index.ts`). Paths are relative to BuildConfig.BASE_URL. */
interface BirdDexApi {

    /** All birds with their Pokédex stats. Public, no sign-in needed. Typed in A3.1. */
    @GET("api/birds")
    suspend fun getBirds(): JsonArray
}
