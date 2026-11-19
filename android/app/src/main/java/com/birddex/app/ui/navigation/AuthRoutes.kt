package com.birddex.app.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.serialization.Serializable

// Auth screens open full-screen on top of the tabs (no bottom bar).
// Property names are read back by the ViewModels through SavedStateHandle ("email").

/** Sign in, optionally with the email already filled in. */
@Serializable data class LoginRoute(val email: String? = null)

@Serializable data object RegisterRoute

/** "Check your inbox" for [email]. */
@Serializable data class VerifyEmailRoute(val email: String)

fun NavDestination?.isAuthScreen(): Boolean =
    this != null && (hasRoute<LoginRoute>() || hasRoute<RegisterRoute>() || hasRoute<VerifyEmailRoute>())

/** Leaves the sign-in flow entirely (e.g. after signing in), back to the tab underneath. */
fun NavController.leaveAuthFlow() {
    while (currentBackStackEntry?.destination.isAuthScreen()) {
        if (!popBackStack()) break
    }
}

/** Opens sign-in, replacing any sign-in screen already on the stack. */
fun NavController.openLogin(email: String? = null) =
    navigate(LoginRoute(email)) { popUpTo<LoginRoute> { inclusive = true } }

/** Opens register, replacing any register screen already on the stack. */
fun NavController.openRegister() =
    navigate(RegisterRoute) { popUpTo<RegisterRoute> { inclusive = true } }
