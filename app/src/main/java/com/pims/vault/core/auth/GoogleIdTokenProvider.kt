package com.pims.vault.core.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface GoogleIdTokenResult {
    data class Success(val idToken: String) : GoogleIdTokenResult
    data class Failure(val error: AuthFailure) : GoogleIdTokenResult
}

@Singleton
class GoogleIdTokenProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val DEFAULT_WEB_CLIENT_ID = "321349557206-9nkjevjc5vqplv1eoo4a7du29qqo7u9e.apps.googleusercontent.com"
    }

    private val credentialManager = CredentialManager.create(context)

    suspend fun requestGoogleIdToken(activity: Activity): GoogleIdTokenResult {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(DEFAULT_WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                context = activity,
                request = request
            )
            extractIdToken(response)
        } catch (e: GetCredentialCancellationException) {
            GoogleIdTokenResult.Failure(AuthFailure.Cancelled)
        } catch (e: GetCredentialException) {
            GoogleIdTokenResult.Failure(AuthFailure.GooglePlayServicesRejected(e.localizedMessage ?: "Credential retrieval failed"))
        } catch (e: Exception) {
            GoogleIdTokenResult.Failure(AuthFailure.Unknown)
        }
    }

    suspend fun savePasswordCredential(
        activity: Activity,
        username: String,
        password: String
    ) {
        // Optional password credential persistence
    }

    private fun extractIdToken(response: GetCredentialResponse): GoogleIdTokenResult {
        val credential = response.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                return GoogleIdTokenResult.Success(googleIdTokenCredential.idToken)
            } catch (e: Exception) {
                return GoogleIdTokenResult.Failure(AuthFailure.GoogleTokenMissing)
            }
        }
        return GoogleIdTokenResult.Failure(AuthFailure.GoogleTokenMissing)
    }
}
