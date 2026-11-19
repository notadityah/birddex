package com.birddex.app.data.birds

import com.birddex.app.data.network.ApiResult
import com.birddex.app.data.network.BirdDexApi
import com.birddex.app.data.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/** The species list. A3.2 adds an offline cache in front of the network. */
interface BirdRepository {
    suspend fun getBirds(): ApiResult<List<Bird>>
}

@Singleton
class NetworkBirdRepository @Inject constructor(
    private val api: BirdDexApi,
    private val json: Json,
) : BirdRepository {
    override suspend fun getBirds(): ApiResult<List<Bird>> = apiCall(json) { api.getBirds() }
}
