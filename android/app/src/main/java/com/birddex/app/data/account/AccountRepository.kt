package com.birddex.app.data.account

import com.birddex.app.data.network.ApiResult
import com.birddex.app.data.network.BirdDexApi
import com.birddex.app.data.network.apiCall
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** `GET`/`PATCH /api/account/settings`: just the gallery anonymity flag for now. */
@Serializable
data class AccountSettings(
    /**
     * Shared sightings show "Anonymous" instead of the user's name. No default on purpose:
     * the JSON setup leaves out values equal to their default, so `false` would be sent as
     * `{}`, which the server rejects.
     */
    val galleryAnonymous: Boolean,
)

/** Settings stored on the server for the signed-in user (website: AccountPage.vue). */
interface AccountRepository {
    suspend fun getSettings(): ApiResult<AccountSettings>

    /** Returns the settings the server saved. */
    suspend fun setGalleryAnonymous(anonymous: Boolean): ApiResult<AccountSettings>
}

class NetworkAccountRepository @Inject constructor(
    private val api: BirdDexApi,
    private val json: Json,
) : AccountRepository {

    override suspend fun getSettings(): ApiResult<AccountSettings> =
        apiCall(json) { api.getAccountSettings() }

    override suspend fun setGalleryAnonymous(anonymous: Boolean): ApiResult<AccountSettings> =
        apiCall(json) { api.updateAccountSettings(AccountSettings(galleryAnonymous = anonymous)) }
}
