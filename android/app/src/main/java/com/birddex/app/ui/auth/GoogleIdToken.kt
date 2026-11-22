package com.birddex.app.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

sealed interface GoogleIdTokenResult {
    data class Token(val idToken: String) : GoogleIdTokenResult
    /** The user closed the Google sheet. Not an error. */
    data object Cancelled : GoogleIdTokenResult
    /** No Google account on the device (or Google Play services can't offer one). */
    data object NoGoogleAccount : GoogleIdTokenResult
    data class Failed(val reason: String?) : GoogleIdTokenResult
}

/**
 * Shows Android's "Sign in with Google" sheet and returns a Google ID token whose audience
 * is [webClientId] (the backend's Google web client), ready for better-auth.
 * [context] must be an Activity context because the sheet is UI.
 */
suspend fun requestGoogleIdToken(context: Context, webClientId: String): GoogleIdTokenResult {
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId = webClientId).build())
        .build()
    return try {
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleIdTokenResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            GoogleIdTokenResult.Failed("Unexpected credential type: ${credential.type}")
        }
    } catch (e: GetCredentialCancellationException) {
        GoogleIdTokenResult.Cancelled
    } catch (e: NoCredentialException) {
        GoogleIdTokenResult.NoGoogleAccount
    } catch (e: GoogleIdTokenParsingException) {
        GoogleIdTokenResult.Failed(e.message)
    } catch (e: GetCredentialException) {
        GoogleIdTokenResult.Failed("${e.type}: ${e.message}")
    }
}
