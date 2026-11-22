package com.birddex.app.ui.auth

import androidx.annotation.StringRes
import com.birddex.app.R

/**
 * Form checks run before calling the server, mirroring the website's LoginPage.vue and
 * RegisterPage.vue. Each returns an error message resource, or null when the field is fine.
 */
object AuthValidation {

    /** better-auth's default minimum, and what the website enforces. */
    const val MIN_PASSWORD_LENGTH = 8

    // Same pattern as the website: something@something.something, no spaces.
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    @StringRes
    fun name(value: String): Int? = if (value.isBlank()) R.string.error_name_required else null

    @StringRes
    fun email(value: String): Int? = when {
        value.isBlank() -> R.string.error_email_required
        !emailPattern.matches(value.trim()) -> R.string.error_email_invalid
        else -> null
    }

    /** Sign-in only checks presence; the server decides if it's right. */
    @StringRes
    fun currentPassword(value: String): Int? = if (value.isEmpty()) R.string.error_password_required else null

    @StringRes
    fun newPassword(value: String): Int? = when {
        value.isEmpty() -> R.string.error_password_required
        value.length < MIN_PASSWORD_LENGTH -> R.string.error_password_too_short
        else -> null
    }

    @StringRes
    fun confirmPassword(password: String, confirm: String): Int? = when {
        confirm.isEmpty() -> R.string.error_confirm_password_required
        confirm != password -> R.string.error_passwords_dont_match
        else -> null
    }
}
