package com.pims.vault.core.auth

import kotlinx.coroutines.flow.StateFlow

enum class AuthProviderKind {
    EMAIL_PASSWORD,
    GOOGLE
}

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean,
    val linkedProviders: Set<AuthProviderKind>
) {
    val providerLabel: String
        get() = linkedProviders.joinToString(", ") { it.name }

    fun hasProvider(kind: AuthProviderKind): Boolean = linkedProviders.contains(kind)
}

sealed interface AuthFailure {
    val userMessage: String

    data object Cancelled : AuthFailure {
        override val userMessage: String = "Sign in was cancelled"
    }
    data object EmailAlreadyInUse : AuthFailure {
        override val userMessage: String = "Email is already registered"
    }
    data class GooglePlayServicesRejected(val detail: String) : AuthFailure {
        override val userMessage: String = "Google Play Services error: $detail"
    }
    data object GoogleTokenMissing : AuthFailure {
        override val userMessage: String = "Google ID token was missing"
    }
    data object GoogleUnavailable : AuthFailure {
        override val userMessage: String = "Google Sign In is currently unavailable"
    }
    data object InvalidCredentials : AuthFailure {
        override val userMessage: String = "Invalid email or password"
    }
    data object InvalidEmail : AuthFailure {
        override val userMessage: String = "Please enter a valid email address"
    }
    data object Network : AuthFailure {
        override val userMessage: String = "Network error. Please check your connection"
    }
    data object ProviderDisabled : AuthFailure {
        override val userMessage: String = "This sign in method is disabled"
    }
    data object RequiresRecentLogin : AuthFailure {
        override val userMessage: String = "Please log in again to continue"
    }
    data object SignedOut : AuthFailure {
        override val userMessage: String = "You have been signed out"
    }
    data object TooManyRequests : AuthFailure {
        override val userMessage: String = "Too many attempts. Please try again later"
    }
    data object Unknown : AuthFailure {
        override val userMessage: String = "An unexpected error occurred"
    }
    data object WeakPassword : AuthFailure {
        override val userMessage: String = "Password is too weak"
    }
}

sealed interface AuthResult {
    data class Success(val user: AuthUser) : AuthResult
    data class Failure(val error: AuthFailure) : AuthResult
}

interface AuthenticationService {
    val authState: StateFlow<AuthUser?>
    val currentUser: AuthUser?

    suspend fun signInWithEmail(email: String, password: String): AuthResult
    suspend fun createEmailAccount(email: String, password: String, displayName: String? = null): AuthResult
    suspend fun signInWithGoogle(idToken: String): AuthResult
    suspend fun signInWithCustomToken(token: String): AuthResult
    suspend fun sendPasswordReset(email: String): AuthResult
    suspend fun sendVerificationEmail(): AuthResult
    suspend fun changePassword(newPassword: String): AuthResult
    fun signOut()
}
