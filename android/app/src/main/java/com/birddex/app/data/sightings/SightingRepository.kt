package com.birddex.app.data.sightings

import com.birddex.app.data.auth.AuthRepository
import com.birddex.app.data.auth.SessionState
import com.birddex.app.data.network.ApiError
import com.birddex.app.data.network.ApiResult
import com.birddex.app.data.network.BirdDexApi
import com.birddex.app.data.network.SetPublicRequest
import com.birddex.app.data.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * The signed-in user's sightings, offline first like the birds. The saved copy is wiped on
 * sign-out by UserDataCleaner.
 */
interface SightingRepository {

    /** Saved sightings, newest first; empty until the first successful [refresh]. */
    fun observeSightings(): Flow<List<Sighting>>

    /**
     * Downloads the user's sightings and replaces the saved copy. On failure the saved copy
     * is kept; a 401 also signs the app out (UnauthorizedInterceptor).
     */
    suspend fun refresh(): ApiResult<Unit>

    /**
     * Shares or unshares a sighting in the gallery. Optimistic like the website: the saved
     * copy changes at once and is put back if the server refuses.
     */
    suspend fun setPublic(id: String, isPublic: Boolean): ApiResult<Unit>

    /**
     * Permanently deletes a sighting and its photo on the server, then from the saved copy.
     * Not optimistic: nothing disappears unless the server confirms.
     */
    suspend fun delete(id: String): ApiResult<Unit>
}

@Singleton
class OfflineFirstSightingRepository @Inject constructor(
    private val api: BirdDexApi,
    private val dao: SightingDao,
    private val auth: AuthRepository,
    private val json: Json,
) : SightingRepository {

    override fun observeSightings(): Flow<List<Sighting>> =
        dao.observeAll().map { rows -> rows.map { it.toSighting() } }

    override suspend fun refresh(): ApiResult<Unit> =
        when (val result = apiCall(json) { api.getSightings() }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> {
                // Signed out while the download was running: don't bring that account's
                // sightings back after UserDataCleaner wiped them.
                if (auth.sessionState.value != SessionState.SignedOut) {
                    dao.replaceAll(result.value.map { it.toEntity() })
                }
                ApiResult.Success(Unit)
            }
        }

    override suspend fun setPublic(id: String, isPublic: Boolean): ApiResult<Unit> {
        dao.setPublic(id, isPublic)
        val result = apiCall(json) { api.setSightingPublic(id, SetPublicRequest(isPublic)) }
        when {
            result is ApiResult.Success -> Unit
            result.isNotFound() -> dao.delete(id) // deleted elsewhere (e.g. on the website)
            else -> dao.setPublic(id, !isPublic) // roll back
        }
        return result
    }

    override suspend fun delete(id: String): ApiResult<Unit> {
        val result = apiCall(json) { api.deleteSighting(id) }
        // 404: already gone on the server, so it should go from the phone too.
        if (result is ApiResult.Success || result.isNotFound()) dao.delete(id)
        return if (result.isNotFound()) ApiResult.Success(Unit) else result
    }

    private fun ApiResult<*>.isNotFound() =
        this is ApiResult.Failure && (error as? ApiError.Http)?.code == 404
}
