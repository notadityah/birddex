package com.birddex.app.ui.navigation

import androidx.navigation.NavController
import kotlinx.serialization.Serializable

// Routes of the sign-in flow, shown by the session gate when signed out (no tabs).
// Property names are read back by the ViewModels through SavedStateHandle ("email").

/** Sign in, optionally with the email already filled in. */
@Serializable data class LoginRoute(val email: String? = null)

@Serializable data object RegisterRoute

/** "Check your inbox" for [email]. */
@Serializable data class VerifyEmailRoute(val email: String)

/** Request a password-reset email, starting from whatever was typed on sign-in. */
@Serializable data class ForgotPasswordRoute(val email: String? = null)

/** A tapped email-verification link (EmailLink.VerifyEmail): verifies, then offers sign-in. */
@Serializable data class VerifyLinkRoute(val token: String)

/** A tapped password-reset link (EmailLink.ResetPassword): "Set new password". */
@Serializable data class ResetPasswordRoute(val token: String)

/** Opens sign-in, replacing any sign-in screen already on the stack. */
fun NavController.openLogin(email: String? = null) =
    navigate(LoginRoute(email)) { popUpTo<LoginRoute> { inclusive = true } }

/** Opens register, replacing any register screen already on the stack. */
fun NavController.openRegister() =
    navigate(RegisterRoute) { popUpTo<RegisterRoute> { inclusive = true } }
