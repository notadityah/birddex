package com.birddex.app.data.auth

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmailLinkTest {

    private fun parse(url: String?) = EmailLink.parse(url, "birddex.fun")

    /** A JWT-shaped token with [payload] (the signature isn't checked here). */
    private fun jwt(payload: String): String {
        val enc = Base64.getUrlEncoder().withoutPadding()
        return enc.encodeToString("""{"alg":"HS256"}""".toByteArray()) + "." +
            enc.encodeToString(payload.toByteArray()) + ".c2ln"
    }

    @Test fun verificationLinkAsBetterAuthSendsIt() {
        val token = jwt("""{"email":"jane@example.com","iat":1,"exp":2}""")
        val link = parse("https://birddex.fun/api/auth/verify-email?token=$token&callbackURL=https://birddex.fun/login")

        assertEquals(EmailLink.VerifyEmail(token), link)
        assertEquals("jane@example.com", (link as EmailLink.VerifyEmail).email)
    }

    @Test fun tokenIsUrlDecoded() {
        assertEquals(EmailLink.VerifyEmail("a+b/c"), parse("https://birddex.fun/api/auth/verify-email?callbackURL=x&token=a%2Bb%2Fc"))
    }

    @Test fun resetLinkAsBetterAuthSendsIt() {
        assertEquals(
            EmailLink.ResetPassword("Xy12abCD34efGH56ijKL78mn"),
            parse("https://birddex.fun/api/auth/reset-password/Xy12abCD34efGH56ijKL78mn?callbackURL=https%3A%2F%2Fbirddex.fun%2Freset-password"),
        )
    }

    @Test fun hostIsCaseInsensitive() {
        assertEquals(EmailLink.ResetPassword("t"), parse("https://BirdDex.fun/api/auth/reset-password/t"))
    }

    @Test fun anythingElseIsNotAnEmailLink() {
        assertNull(parse(null))
        assertNull(parse("not a url"))
        assertNull(parse("https://birddex.fun/"))
        assertNull(parse("https://birddex.fun/mydex"))
        assertNull(parse("https://birddex.fun/reset-password?token=t")) // the website page, not the email link
        assertNull(parse("https://birddex.fun/api/auth/verify-email")) // no token
        assertNull(parse("https://birddex.fun/api/auth/verify-email?token="))
        assertNull(parse("https://birddex.fun/api/auth/verify-email-other?token=t"))
        assertNull(parse("https://birddex.fun/api/auth/reset-password/"))
        assertNull(parse("https://birddex.fun/api/auth/reset-password/a/b"))
        assertNull(parse("http://birddex.fun/api/auth/reset-password/t")) // not https
        assertNull(parse("https://evil.example/api/auth/reset-password/t"))
        assertNull(parse("https://birddex.fun.evil.example/api/auth/reset-password/t"))
    }

    @Test fun emailIsNullWhenTheTokenDoesNotSay() {
        assertNull(EmailLink.VerifyEmail("not-a-jwt").email)
        assertNull(EmailLink.VerifyEmail("a.%%%.c").email)
        assertNull(EmailLink.VerifyEmail(jwt("[1,2]")).email)
        assertNull(EmailLink.VerifyEmail(jwt("""{"email":{"x":1}}""")).email)
        assertNull(EmailLink.VerifyEmail(jwt("""{"sub":"u1"}""")).email)
    }
}
