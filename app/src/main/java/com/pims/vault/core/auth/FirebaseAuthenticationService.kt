package com.pims.vault.core.auth

import android.app.Activity
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthActionCodeException
import com.google.firebase.auth.FirebaseAuthEmailException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthenticationService @Inject constructor() : AuthenticationService {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _authState = MutableStateFlow<AuthUser?>(toAuthUser(firebaseAuth.currentUser))
    override val authState: StateFlow<AuthUser?> = _authState.asStateFlow()

    override val currentUser: AuthUser?
        get() = toAuthUser(firebaseAuth.currentUser)

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _authState.value = toAuthUser(auth.currentUser)
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = toAuthUser(result.user)
            if (user != null) AuthResult.Success(user) else AuthResult.Failure(AuthFailure.Unknown)
        } catch (e: Exception) {
            AuthResult.Failure(toAuthFailure(e))
        }
    }

    override suspend fun createEmailAccount(
        email: String,
        password: String,
        displayName: String?
    ): AuthResult {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user
            if (user != null) {
                if (!displayName.isNullOrBlank()) {
                    val profileUpdate = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    user.updateProfile(profileUpdate).await()
                }
                AuthResult.Success(toAuthUser(user)!!)
            } else {
                AuthResult.Failure(AuthFailure.Unknown)
            }
        } catch (e: Exception) {
            AuthResult.Failure(toAuthFailure(e))
        }
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return exchangeCredential(credential)
    }

    override suspend fun signInWithGoogleProvider(activity: Activity): AuthResult {
        return try {
            val provider = OAuthProvider.newBuilder("google.com")
            provider.addCustomParameter("prompt", "select_account")
            val pendingTask = firebaseAuth.pendingAuthResult
            val result = if (pendingTask != null) {
                pendingTask.await()
            } else {
                firebaseAuth.startActivityForSignInWithProvider(activity, provider.build()).await()
            }
            val user = toAuthUser(result.user)
            if (user != null) AuthResult.Success(user) else AuthResult.Failure(AuthFailure.Unknown)
        } catch (e: Exception) {
            AuthResult.Failure(toAuthFailure(e))
        }
    }

    override suspend fun signInWithCustomToken(token: String): AuthResult {
        return try {
            val result = firebaseAuth.signInWithCustomToken(token).await()
            val user = toAuthUser(result.user)
            if (user != null) AuthResult.Success(user) else AuthResult.Failure(AuthFailure.Unknown)
        } catch (e: Exception) {
            AuthResult.Failure(toAuthFailure(e))
        }
    }

    override suspend fun sendPasswordReset(email: String): AuthResult {
        return try {
            firebaseAuth.sendPasswordResetEmail(email.trim()).await()
            val current = currentUser
            if (current != null) AuthResult.Success(current) else AuthResult.Failure(AuthFailure.SignedOut)
        } catch (e: Exception) {
            AuthResult.Failure(toAuthFailure(e))
        }
    }

    override suspend fun sendVerificationEmail(): AuthResult {
        return try {
            val user = firebaseAuth.currentUser ?: return AuthResult.Failure(AuthFailure.SignedOut)
            user.sendEmailVerification().await()
            AuthResult.Success(toAuthUser(user)!!)
        } catch (e: Exception) {
            AuthResult.Failure(toAuthFailure(e))
        }
    }

    override suspend fun changePassword(newPassword: String): AuthResult {
        return try {
            val user = firebaseAuth.currentUser ?: return AuthResult.Failure(AuthFailure.SignedOut)
            user.updatePassword(newPassword).await()
            AuthResult.Success(toAuthUser(user)!!)
        } catch (e: Exception) {
            AuthResult.Failure(toAuthFailure(e))
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
        _authState.value = null
    }

    private suspend fun exchangeCredential(credential: AuthCredential): AuthResult {
        return try {
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = toAuthUser(result.user)
            if (user != null) AuthResult.Success(user) else AuthResult.Failure(AuthFailure.Unknown)
        } catch (e: Exception) {
            AuthResult.Failure(toAuthFailure(e))
        }
    }

    private fun toAuthUser(firebaseUser: FirebaseUser?): AuthUser? {
        if (firebaseUser == null) return null
        val providers = mutableSetOf<AuthProviderKind>()
        firebaseUser.providerData.forEach { data ->
            when (data.providerId) {
                GoogleAuthProvider.PROVIDER_ID -> providers.add(AuthProviderKind.GOOGLE)
                "password" -> providers.add(AuthProviderKind.EMAIL_PASSWORD)
            }
        }
        if (providers.isEmpty()) {
            providers.add(AuthProviderKind.EMAIL_PASSWORD)
        }
        return AuthUser(
            uid = firebaseUser.uid,
            email = firebaseUser.email,
            displayName = firebaseUser.displayName,
            photoUrl = firebaseUser.photoUrl?.toString(),
            isEmailVerified = firebaseUser.isEmailVerified,
            linkedProviders = providers
        )
    }

    private fun toAuthFailure(throwable: Throwable): AuthFailure {
        val msg = throwable.message.orEmpty()
        if (msg.contains("WEB_CONTEXT_CANCELED", ignoreCase = true) ||
            msg.contains("canceled", ignoreCase = true) ||
            msg.contains("cancelled", ignoreCase = true)) {
            return AuthFailure.Cancelled
        }
        return when (throwable) {
            is FirebaseAuthInvalidUserException -> AuthFailure.InvalidCredentials
            is FirebaseAuthInvalidCredentialsException -> AuthFailure.InvalidCredentials
            is FirebaseAuthUserCollisionException -> AuthFailure.EmailAlreadyInUse
            is FirebaseAuthWeakPasswordException -> AuthFailure.WeakPassword
            is FirebaseAuthRecentLoginRequiredException -> AuthFailure.RequiresRecentLogin
            is FirebaseAuthEmailException -> AuthFailure.InvalidEmail
            is FirebaseTooManyRequestsException -> AuthFailure.TooManyRequests
            is FirebaseNetworkException -> AuthFailure.Network
            is FirebaseAuthActionCodeException -> AuthFailure.InvalidCredentials
            else -> AuthFailure.Unknown
        }
    }
}
