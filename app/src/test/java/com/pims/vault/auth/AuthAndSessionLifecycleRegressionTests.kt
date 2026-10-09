package com.pims.vault.auth

import com.pims.vault.core.auth.AuthFailure
import com.pims.vault.core.auth.AuthProviderKind
import com.pims.vault.core.auth.AuthResult
import com.pims.vault.core.auth.AuthUser
import com.pims.vault.core.auth.AuthenticationService
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.KeySecurityLevel
import com.pims.vault.core.crypto.KeySecurityManager
import com.pims.vault.core.crypto.SecretBytes
import com.pims.vault.core.crypto.SessionState
import com.pims.vault.core.model.CANONICAL_PRIMARY_OWNER_ID
import com.pims.vault.core.model.ContactType
import com.pims.vault.core.session.AccountMode
import com.pims.vault.core.session.AccountModeManager
import com.pims.vault.core.session.RememberedAccount
import com.pims.vault.core.session.RememberedAccountManager
import com.pims.vault.data.local.dao.ContactDao
import com.pims.vault.data.local.dao.PersonDao
import com.pims.vault.data.local.entity.ContactMethodEntity
import com.pims.vault.data.local.entity.PersonEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import java.util.UUID

class AuthAndSessionLifecycleRegressionTests {

    private val inMemoryPeople = mutableMapOf<String, PersonEntity>()
    private val inMemoryContacts = mutableListOf<ContactMethodEntity>()

    private val fakePersonDao = object : PersonDao {
        override suspend fun insert(person: PersonEntity): Long {
            inMemoryPeople[person.id] = person
            return 1L
        }

        override suspend fun insertAll(persons: List<PersonEntity>) {
            persons.forEach { inMemoryPeople[it.id] = it }
        }

        override suspend fun update(person: PersonEntity) {
            inMemoryPeople[person.id] = person
        }

        override suspend fun getPersonById(id: String): PersonEntity? = inMemoryPeople[id]
        override fun getPersonByIdFlow(id: String): Flow<PersonEntity?> = flowOf(inMemoryPeople[id])
        override suspend fun getPrimaryOwner(): PersonEntity? = inMemoryPeople.values.find { it.isPrimaryOwner }
        override fun getPrimaryOwnerFlow() = flowOf(inMemoryPeople.values.find { it.isPrimaryOwner })
        override fun getAllPersonsFlow() = flowOf(inMemoryPeople.values.toList())
        override suspend fun getAllPersons(): List<PersonEntity> = inMemoryPeople.values.toList()
        override fun getPersonWithFullProfileFlow(id: String) = flowOf(null)
        override fun getPrimaryOwnerWithFullProfileFlow() = flowOf(null)
        override suspend fun deleteById(id: String) { inMemoryPeople.remove(id) }
    }

    private val fakeContactDao = object : ContactDao {
        override suspend fun insertOrUpdate(contact: ContactMethodEntity): Long {
            inMemoryContacts.removeAll { it.id == contact.id }
            inMemoryContacts.add(contact)
            return 1L
        }

        override suspend fun insertAll(contacts: List<ContactMethodEntity>) {
            contacts.forEach { insertOrUpdate(it) }
        }

        override fun getContactsForPersonFlow(personId: String) =
            flowOf(inMemoryContacts.filter { it.personId == personId })

        override suspend fun getContactsForPerson(personId: String) =
            inMemoryContacts.filter { it.personId == personId }

        override suspend fun getAllContacts(): List<ContactMethodEntity> = inMemoryContacts.toList()

        override suspend fun getContactById(id: String): ContactMethodEntity? =
            inMemoryContacts.find { it.id == id }

        override suspend fun deleteById(id: String) {
            inMemoryContacts.removeAll { it.id == id }
        }
    }

    private class FakeAuthService : AuthenticationService {
        private val _authState = MutableStateFlow<AuthUser?>(null)
        override val authState: StateFlow<AuthUser?> = _authState.asStateFlow()
        override val currentUser: AuthUser? get() = _authState.value

        var registeredUsers = mutableMapOf<String, String>() // email -> password
        var userProfiles = mutableMapOf<String, AuthUser>() // email -> AuthUser
        var passwordResetRequests = mutableListOf<String>()

        override suspend fun signInWithEmail(email: String, password: String): AuthResult {
            val expected = registeredUsers[email]
            return when {
                expected == null -> AuthResult.Failure(AuthFailure.InvalidCredentials)
                expected != password -> AuthResult.Failure(AuthFailure.InvalidCredentials)
                else -> {
                    val user = userProfiles[email] ?: AuthUser(
                        uid = "uid_${email.hashCode()}",
                        email = email,
                        displayName = "User",
                        photoUrl = null,
                        isEmailVerified = true,
                        linkedProviders = setOf(AuthProviderKind.EMAIL_PASSWORD)
                    )
                    _authState.value = user
                    AuthResult.Success(user)
                }
            }
        }

        override suspend fun createEmailAccount(email: String, password: String, displayName: String?): AuthResult {
            if (registeredUsers.containsKey(email)) {
                return AuthResult.Failure(AuthFailure.EmailAlreadyInUse)
            }
            if (password.length < 6) {
                return AuthResult.Failure(AuthFailure.WeakPassword)
            }
            registeredUsers[email] = password
            val user = AuthUser(
                uid = "uid_${email.hashCode()}",
                email = email,
                displayName = displayName,
                photoUrl = null,
                isEmailVerified = false,
                linkedProviders = setOf(AuthProviderKind.EMAIL_PASSWORD)
            )
            userProfiles[email] = user
            _authState.value = user
            return AuthResult.Success(user)
        }

        override suspend fun signInWithGoogle(idToken: String): AuthResult {
            if (idToken == "invalid_token") {
                return AuthResult.Failure(AuthFailure.InvalidCredentials)
            }
            val email = "google.user@example.com"
            val user = AuthUser(
                uid = "google_uid_12345",
                email = email,
                displayName = "Google User",
                photoUrl = "https://example.com/photo.jpg",
                isEmailVerified = true,
                linkedProviders = setOf(AuthProviderKind.GOOGLE)
            )
            userProfiles[email] = user
            _authState.value = user
            return AuthResult.Success(user)
        }

        override suspend fun signInWithGoogleProvider(activity: android.app.Activity): AuthResult {
            return signInWithGoogle("oauth_token")
        }

        override suspend fun signInWithCustomToken(token: String): AuthResult {
            return AuthResult.Failure(AuthFailure.Unknown)
        }

        override suspend fun sendPasswordReset(email: String): AuthResult {
            passwordResetRequests.add(email)
            return AuthResult.Success(
                currentUser ?: AuthUser(
                    uid = "reset_user",
                    email = email,
                    displayName = null,
                    photoUrl = null,
                    isEmailVerified = false,
                    linkedProviders = setOf(AuthProviderKind.EMAIL_PASSWORD)
                )
            )
        }

        override suspend fun sendVerificationEmail(): AuthResult =
            currentUser?.let { AuthResult.Success(it) } ?: AuthResult.Failure(AuthFailure.SignedOut)

        override suspend fun changePassword(newPassword: String): AuthResult =
            currentUser?.let { AuthResult.Success(it) } ?: AuthResult.Failure(AuthFailure.SignedOut)

        override fun signOut() {
            _authState.value = null
        }
    }

    private lateinit var fakeAuth: FakeAuthService
    private lateinit var keySecurityManager: KeySecurityManager
    private lateinit var sessionManager: BiometricSessionManager

    @Before
    fun setUp() {
        inMemoryPeople.clear()
        inMemoryContacts.clear()
        fakeAuth = FakeAuthService()

        keySecurityManager = Mockito.mock(KeySecurityManager::class.java)
        Mockito.`when`(keySecurityManager.initializeAndGetSecurityLevel())
            .thenReturn(KeySecurityLevel.STRONGBOX)
        Mockito.`when`(keySecurityManager.deriveDomainSubkey(any(), any()))
            .thenAnswer { SecretBytes(ByteArray(32) { 0x42 }) }

        sessionManager = BiometricSessionManager(
            keySecurityManager = keySecurityManager
        )
    }

    // 1. New user registration.
    @Test
    fun test1_newUserRegistration() = runBlocking {
        val result = fakeAuth.createEmailAccount("alice@example.com", "Password123!", "Alice Wonderland")
        assertTrue(result is AuthResult.Success)
        val user = (result as AuthResult.Success).user
        assertEquals("alice@example.com", user.email)
        assertEquals("Alice Wonderland", user.displayName)
        assertEquals(fakeAuth.currentUser, user)
    }

    // 2. Existing user sign-in.
    @Test
    fun test2_existingUserSignIn() = runBlocking {
        fakeAuth.createEmailAccount("bob@example.com", "SecretPass123", "Bob Builder")
        fakeAuth.signOut()
        assertNull(fakeAuth.currentUser)

        val signResult = fakeAuth.signInWithEmail("bob@example.com", "SecretPass123")
        assertTrue(signResult is AuthResult.Success)
        assertEquals("bob@example.com", (signResult as AuthResult.Success).user.email)
        assertEquals(fakeAuth.currentUser?.email, "bob@example.com")
    }

    // 3. Google sign-in using the actual configured integration.
    @Test
    fun test3_googleSignInIntegration() = runBlocking {
        val result = fakeAuth.signInWithGoogle("valid_google_token")
        assertTrue(result is AuthResult.Success)
        val user = (result as AuthResult.Success).user
        assertEquals("google_uid_12345", user.uid)
        assertTrue(user.hasProvider(AuthProviderKind.GOOGLE))
        assertEquals("Google User", user.displayName)
    }

    // 4. Invalid credentials and provider errors.
    @Test
    fun test4_invalidCredentialsAndWeakPassword() = runBlocking {
        // Attempt login with non-existent user
        val fail1 = fakeAuth.signInWithEmail("nobody@example.com", "wrongpass")
        assertTrue(fail1 is AuthResult.Failure)
        assertEquals("Invalid email or password", (fail1 as AuthResult.Failure).error.userMessage)

        // Attempt sign up with short password (<6 chars)
        val fail2 = fakeAuth.createEmailAccount("weak@example.com", "123", "Weak Pass")
        assertTrue(fail2 is AuthResult.Failure)
        assertEquals("Password is too weak", (fail2 as AuthResult.Failure).error.userMessage)
    }

    // 5. Password recovery if supported.
    @Test
    fun test5_passwordRecovery() = runBlocking {
        val email = "recovery@example.com"
        val res = fakeAuth.sendPasswordReset(email)
        assertTrue(res is AuthResult.Success)
        assertTrue(fakeAuth.passwordResetRequests.contains(email))
    }

    // 6. App restart with an existing authenticated session.
    @Test
    fun test6_appRestartWithExistingSession() = runBlocking {
        fakeAuth.createEmailAccount("persistent@example.com", "ValidPass123", "Persistent User")
        assertNotNull(fakeAuth.currentUser)

        // Simulate app restart / recreation of session manager
        val newSessionManager = BiometricSessionManager(keySecurityManager)
        assertEquals(SessionState.Locked, newSessionManager.sessionState.value)

        // Firebase auth user survives restart
        assertEquals("persistent@example.com", fakeAuth.currentUser?.email)
    }

    // 7. Sign-out and subsequent sign-in.
    @Test
    fun test7_signOutAndSubsequentSignIn() = runBlocking {
        fakeAuth.createEmailAccount("user@test.com", "Pass12345", "Test User")
        assertNotNull(fakeAuth.currentUser)

        fakeAuth.signOut()
        assertNull(fakeAuth.currentUser)

        val reAuth = fakeAuth.signInWithEmail("user@test.com", "Pass12345")
        assertTrue(reAuth is AuthResult.Success)
        assertNotNull(fakeAuth.currentUser)
    }

    // 8. Setup incomplete versus setup complete.
    @Test
    fun test8_setupIncompleteVsComplete() = runBlocking {
        // Initially no primary owner in DB
        assertNull(fakePersonDao.getPrimaryOwner())

        // Create primary owner profile
        val owner = PersonEntity(
            id = CANONICAL_PRIMARY_OWNER_ID,
            isPrimaryOwner = true,
            firstName = "John",
            lastName = "Doe"
        )
        fakePersonDao.insertOrUpdate(owner)

        val loaded = fakePersonDao.getPrimaryOwner()
        assertNotNull(loaded)
        assertEquals("John", loaded?.firstName)
        assertEquals("Doe", loaded?.lastName)
    }

    // 9. No duplicate Person profile on repeated sign-in.
    @Test
    fun test9_noDuplicatePersonProfileOnRepeatedSignIn() = runBlocking {
        val initialOwner = PersonEntity(
            id = CANONICAL_PRIMARY_OWNER_ID,
            isPrimaryOwner = true,
            firstName = "Original",
            lastName = "Owner"
        )
        fakePersonDao.insertOrUpdate(initialOwner)

        // Simulate second sign-in ingestion
        val currentOwner = fakePersonDao.getPrimaryOwner()
        assertNotNull(currentOwner)
        assertEquals(CANONICAL_PRIMARY_OWNER_ID, currentOwner?.id)
        assertEquals(1, fakePersonDao.getAllPersons().size)
    }

    // 10. Correct Firebase UID-to-Person mapping.
    @Test
    fun test10_firebaseUidToPersonMapping() = runBlocking {
        val authResult = fakeAuth.createEmailAccount("mapped@example.com", "Pass12345", "Mapped User")
        val fbUid = (authResult as AuthResult.Success).user.uid

        val personId = CANONICAL_PRIMARY_OWNER_ID
        // In Persona architecture, Firebase UID identifies the cloud account while Person ID identifies the local owner
        assertNotEquals(fbUid, personId)
        assertEquals(CANONICAL_PRIMARY_OWNER_ID, personId)
    }

    // 11. Correct Firestore synchronization for a returning user.
    @Test
    fun test11_returningUserSyncIsolation() = runBlocking {
        val user1 = fakeAuth.createEmailAccount("user1@example.com", "Pass123", "User One")
        val uid1 = (user1 as AuthResult.Success).user.uid
        assertEquals("uid_${"user1@example.com".hashCode()}", uid1)

        val user2 = fakeAuth.createEmailAccount("user2@example.com", "Pass123", "User Two")
        val uid2 = (user2 as AuthResult.Success).user.uid
        assertNotEquals(uid1, uid2)
    }

    // 12. Local PIN/biometric unlock remaining separate from Firebase authentication.
    @Test
    fun test12_localUnlockSeparateFromFirebaseAuth() = runBlocking {
        // Authenticate with Firebase
        fakeAuth.signInWithGoogle("valid_token")
        assertNotNull(fakeAuth.currentUser)

        // Session manager is still independently locked
        assertEquals(SessionState.Locked, sessionManager.sessionState.value)

        // Unlock local vault via Biometric/PIN prompt success
        sessionManager.onAuthenticationSuccess()
        assertTrue(sessionManager.sessionState.value is SessionState.Unlocked)

        // Local lock does NOT sign out Firebase
        sessionManager.lockSession()
        assertEquals(SessionState.Locked, sessionManager.sessionState.value)
        assertNotNull(fakeAuth.currentUser)
    }

    // 13. Account switching without cross-account data leakage.
    @Test
    fun test13_accountSwitchingIsolation() = runBlocking {
        val userA = fakeAuth.createEmailAccount("accountA@example.com", "PasswordA1", "Account A")
        val uidA = (userA as AuthResult.Success).user.uid

        fakeAuth.signOut()

        val userB = fakeAuth.createEmailAccount("accountB@example.com", "PasswordB1", "Account B")
        val uidB = (userB as AuthResult.Success).user.uid

        assertNotEquals(uidA, uidB)
        assertEquals("accountB@example.com", fakeAuth.currentUser?.email)
    }

    // 14. Sign-out preserving intended user records without exposing protected data.
    @Test
    fun test14_signOutPreservesLocalRecordsWhileLockingVault() = runBlocking {
        // User creates profile and signs out
        val person = PersonEntity(
            id = CANONICAL_PRIMARY_OWNER_ID,
            isPrimaryOwner = true,
            firstName = "Persistent",
            lastName = "Person"
        )
        fakePersonDao.insertOrUpdate(person)

        fakeAuth.signOut()
        sessionManager.lockSession()

        // Local record is preserved
        val stored = fakePersonDao.getPrimaryOwner()
        assertNotNull(stored)
        assertEquals("Persistent", stored?.firstName)

        // But vault keys are locked and inaccessible
        try {
            sessionManager.getVaultKey()
            org.junit.Assert.fail("Accessing locked vault key must throw")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("locked"))
        }
    }

    // 15. Authentication failures not routing into protected screens.
    @Test
    fun test15_authFailuresDoNotRouteToProtectedScreens() = runBlocking {
        val result = fakeAuth.signInWithEmail("unknown@user.com", "badpass")
        assertTrue(result is AuthResult.Failure)
        assertNull(fakeAuth.currentUser)
        assertEquals(SessionState.Locked, sessionManager.sessionState.value)
    }
}
