package com.birddex.app.data.network

import com.birddex.app.data.birds.Bird
import com.birddex.app.data.sightings.Sighting
import retrofit2.http.GET

/** BirdDex REST API (`backend/lambda/api/index.ts`). Paths are relative to BuildConfig.BASE_URL. */
interface BirdDexApi {

    /** All 36 birds with their Pokédex stats, sorted by name. Public, no sign-in needed. */
    @GET("api/birds")
    suspend fun getBirds(): List<Bird>

    /** The signed-in user's sightings, newest first, with 1-hour photo links. 401 when signed out. */
    @GET("api/sightings")
    suspend fun getSightings(): List<Sighting>
}
