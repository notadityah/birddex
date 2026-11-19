package com.birddex.app.ui.auth

import com.birddex.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {

    @Test fun name() {
        assertEquals(R.string.error_name_required, AuthValidation.name("   "))
        assertNull(AuthValidation.name("Jane Doe"))
    }

    @Test fun email() {
        assertEquals(R.string.error_email_required, AuthValidation.email(""))
        assertEquals(R.string.error_email_invalid, AuthValidation.email("jane"))
        assertEquals(R.string.error_email_invalid, AuthValidation.email("jane@example"))
        assertEquals(R.string.error_email_invalid, AuthValidation.email("jane doe@example.com"))
        assertNull(AuthValidation.email("jane@example.com"))
        assertNull(AuthValidation.email("  jane+birddex@example.com  ")) // trimmed before sending
    }

    @Test fun currentPasswordOnlyNeedsToBePresent() {
        assertEquals(R.string.error_password_required, AuthValidation.currentPassword(""))
        assertNull(AuthValidation.currentPassword("x"))
    }

    @Test fun newPasswordNeedsEightCharacters() {
        assertEquals(R.string.error_password_required, AuthValidation.newPassword(""))
        assertEquals(R.string.error_password_too_short, AuthValidation.newPassword("1234567"))
        assertNull(AuthValidation.newPassword("12345678"))
    }

    @Test fun confirmPassword() {
        assertEquals(R.string.error_confirm_password_required, AuthValidation.confirmPassword("secret12", ""))
        assertEquals(R.string.error_passwords_dont_match, AuthValidation.confirmPassword("secret12", "secret13"))
        assertNull(AuthValidation.confirmPassword("secret12", "secret12"))
    }
}
