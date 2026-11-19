package com.birddex.app.data.auth

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lets the HTTP layer tell AuthRepository "the server says this session is gone" (a 401)
 * without the OkHttp client depending on the repository (which itself depends on OkHttp).
 */
@Singleton
class SessionExpiry @Inject constructor() {

    @Volatile private var listener: (() -> Unit)? = null

    fun setListener(listener: () -> Unit) {
        this.listener = listener
    }

    /** Called from OkHttp threads. */
    fun onUnauthorized() {
        listener?.invoke()
    }
}
