package com.pims.vault.notes

import com.pims.vault.core.auth.AuthFailure
import com.pims.vault.core.auth.AuthProviderKind
import com.pims.vault.core.auth.AuthResult
import com.pims.vault.core.auth.AuthUser
import com.pims.vault.core.auth.AuthenticationService
import com.pims.vault.core.auth.GoogleIdTokenProvider
import com.pims.vault.core.auth.GoogleIdTokenResult
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.HardenedCryptoEngine
import com.pims.vault.core.session.AccountModeManager
import com.pims.vault.core.session.RememberedAccountManager
import com.pims.vault.core.security.AccountSecurityManager
import com.pims.vault.data.local.dao.PlainNoteDao
import com.pims.vault.data.local.entity.PlainNoteAttachmentEntity
import com.pims.vault.data.local.entity.PlainNoteEntity
import com.pims.vault.data.local.storage.EncryptedFileStorageImpl
import com.pims.vault.data.repository.PlainNotesRepositoryImpl
import com.pims.vault.domain.model.NoteFormat
import com.pims.vault.domain.repository.PersonRepository
import com.pims.vault.presentation.auth.AccountsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

class PlainNotesAttachmentTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testBiometricSessionManagerKeyRecoveryDoesNotThrow() = runBlocking {
        val sessionManager = BiometricSessionManager(keySecurityManager = null)
        sessionManager.onAuthenticationSuccess()
        val fileKey = sessionManager.getFileStorageKey()
        assertNotNull(fileKey)
        assertEquals(32, fileKey.size)

        val dbKey = sessionManager.getDatabaseKey()
        assertNotNull(dbKey)
        assertEquals(32, dbKey.size)

        val auditKey = sessionManager.getAuditKey()
        assertNotNull(auditKey)
        assertEquals(32, auditKey.size)
    }

    @Test
    fun testMultipleNoteAttachmentsDoNotOverwriteEachOther() = runBlocking {
        val rootDir = tempFolder.newFolder("pims_files")
        val context = mock<android.content.Context>()
        whenever(context.filesDir).thenReturn(rootDir)

        val cryptoEngine = HardenedCryptoEngine()
        val staticKey = ByteArray(32) { (it + 1).toByte() }
        val fileStorage = EncryptedFileStorageImpl(
            context = context,
            cryptoEngine = cryptoEngine,
            keyProvider = { staticKey }
        )

        val noteId = "test_note_1"
        val attachmentsInDb = mutableMapOf<String, PlainNoteAttachmentEntity>()

        val dao = mock<PlainNoteDao>()
        val existingNote = PlainNoteEntity(
            id = noteId,
            ownerPersonId = "primary",
            title = "Vacation Photo Note",
            content = "Family trip notes",
            format = "PLAIN",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        whenever(dao.getById(noteId)).thenReturn(existingNote)
        whenever(dao.upsertAttachment(any())).thenAnswer { invocation ->
            val entity = invocation.getArgument<PlainNoteAttachmentEntity>(0)
            attachmentsInDb[entity.id] = entity
            Unit
        }

        val repository = PlainNotesRepositoryImpl(dao, fileStorage)

        val photo1Bytes = "IMAGE_BYTES_PHOTO_1_SUNSET_BEACH".toByteArray(Charsets.UTF_8)
        val photo2Bytes = "IMAGE_BYTES_PHOTO_2_MOUNTAIN_HIKE_PANORAMA".toByteArray(Charsets.UTF_8)

        val att1 = repository.addAttachment(noteId, "Sunset", photo1Bytes, "image/jpeg")
        val att2 = repository.addAttachment(noteId, "Mountain", photo2Bytes, "image/jpeg")

        // 1. Verify attachments got unique IDs and storage paths
        assertTrue(att1.id != att2.id)
        assertTrue(att1.storagePath != att2.storagePath)

        // 2. Read both back and verify exact byte equality
        val readBack1 = repository.readAttachment(att1)
        val readBack2 = repository.readAttachment(att2)

        assertNotNull("Photo 1 should decrypt successfully", readBack1)
        assertNotNull("Photo 2 should decrypt successfully", readBack2)
        assertArrayEquals("Photo 1 bytes must match exactly", photo1Bytes, readBack1)
        assertArrayEquals("Photo 2 bytes must match exactly", photo2Bytes, readBack2)
    }

    @Test
    fun testGoogleSignInFallbackToOAuthProviderWhenCredentialManagerFails() = runBlocking {
        val testDispatcher = kotlinx.coroutines.test.StandardTestDispatcher()
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        try {
            val authService = mock<AuthenticationService>()
            val googleIdTokenProvider = mock<GoogleIdTokenProvider>()
            val accountModeManager = mock<AccountModeManager>()
            val accountSecurityManager = mock<AccountSecurityManager>()
            val rememberedAccountManager = mock<RememberedAccountManager>()
            val personRepository = mock<PersonRepository>()
            val firestoreSyncService = mock<com.pims.vault.core.sync.FirestoreSyncService>()
            val context = mock<android.content.Context>()
            val activity = mock<android.app.Activity>()

            whenever(rememberedAccountManager.isRememberMeEnabled).thenReturn(MutableStateFlow(true))
            whenever(rememberedAccountManager.rememberedAccount).thenReturn(MutableStateFlow(null))
            whenever(rememberedAccountManager.boundVaultUid).thenReturn(MutableStateFlow(null))
            whenever(rememberedAccountManager.isVaultBoundToDifferentAccount(any())).thenReturn(false)
            whenever(accountModeManager.accountMode).thenReturn(MutableStateFlow(com.pims.vault.core.session.AccountMode.UNSET))
            whenever(authService.authState).thenReturn(MutableStateFlow(null))

            // Simulate Credential Manager rejection (e.g. SHA-1 signature mismatch in Play Services)
            whenever(googleIdTokenProvider.requestGoogleIdToken(activity)).thenReturn(
                GoogleIdTokenResult.Failure(AuthFailure.GooglePlayServicesRejected("10: Developer Error"))
            )

            val authenticatedUser = AuthUser(
                uid = "firebase_user_google_123",
                email = "user@gmail.com",
                displayName = "Jane Doe",
                photoUrl = "https://lh3.googleusercontent.com/photo.jpg",
                isEmailVerified = true,
                linkedProviders = setOf(AuthProviderKind.GOOGLE)
            )

            // Real Firebase OAuth Provider succeeds
            whenever(authService.signInWithGoogleProvider(activity)).thenReturn(
                AuthResult.Success(authenticatedUser)
            )

            val viewModel = AccountsViewModel(
                authenticationService = authService,
                googleIdTokenProvider = googleIdTokenProvider,
                accountModeManager = accountModeManager,
                accountSecurityManager = accountSecurityManager,
                rememberedAccountManager = rememberedAccountManager,
                personRepository = personRepository,
                firestoreSyncService = firestoreSyncService,
                context = context
            )

            viewModel.signInWithGoogle(activity)

            // Advance scheduler to execute the coroutines
            testDispatcher.scheduler.advanceUntilIdle()

            // Verify successful login
            assertEquals(authenticatedUser, viewModel.uiState.value.user)
            assertEquals(false, viewModel.uiState.value.isBusy)
            assertEquals(null, viewModel.uiState.value.error)
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }
}
