package com.birddex.app.data.birds

import com.birddex.app.data.network.ApiError
import com.birddex.app.data.network.ApiResult
import com.birddex.app.data.network.BirdDexApi
import com.birddex.app.data.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * The species list, offline first: screens observe the on-device copy and ask for a
 * [refresh], which downloads the list and updates that copy.
 */
interface BirdRepository {

    /** Saved birds sorted by name; empty until the first successful [refresh]. */
    fun observeBirds(): Flow<List<Bird>>

    /** Downloads the list and replaces the saved copy. On failure the saved copy is kept. */
    suspend fun refresh(): ApiResult<Unit>
}

@Singleton
class OfflineFirstBirdRepository @Inject constructor(
    private val api: BirdDexApi,
    private val dao: BirdDao,
    private val json: Json,
) : BirdRepository {

    override fun observeBirds(): Flow<List<Bird>> = dao.observeAll().map { rows -> rows.map { it.toBird() } }

    override suspend fun refresh(): ApiResult<Unit> =
        when (val result = apiCall(json) { api.getBirds() }) {
            is ApiResult.Failure -> result
            // An empty list would wipe the offline copy; the real list always has species.
            is ApiResult.Success -> if (result.value.isEmpty()) {
                ApiResult.Failure(ApiError.Unexpected("Server returned no birds"))
            } else {
                dao.replaceAll(result.value.map { it.toEntity() })
                ApiResult.Success(Unit)
            }
        }
}
