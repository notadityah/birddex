package com.birddex.app.data

import com.birddex.app.data.auth.AuthRepository
import com.birddex.app.data.auth.SessionState
import com.birddex.app.data.sightings.SightingDao
import com.birddex.app.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * Deletes the signed-in user's saved data whenever the app becomes signed out (sign-out,
 * expired session, or a launch with no session), so the next account on this phone never
 * sees it. Started once from BirdDexApplication.
 */
@Singleton
class UserDataCleaner @Inject constructor(
    private val auth: AuthRepository,
    private val sightings: SightingDao,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch { run() }
    }

    /** Runs until cancelled. Separate from [start] for tests. */
    suspend fun run() {
        auth.sessionState
            .filter { it == SessionState.SignedOut }
            .collect { sightings.deleteAll() }
    }
}
