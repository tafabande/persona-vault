package com.pims.vault.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.pims.vault.core.storage.VaultEvidenceStore
import com.pims.vault.core.sync.FirestoreSyncService
import com.pims.vault.core.sync.VaultProofSyncService
import com.pims.vault.data.local.dao.VaultDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import javax.inject.Provider

class VaultProofSyncServiceTest {
    @Test
    fun proofSyncRequiresAccountKeyAgreementBeforeReadingProofsOrVaultRows(): Unit = runBlocking {
        val auth: FirebaseAuth = mock()
        val user: FirebaseUser = mock()
        whenever(auth.currentUser).thenReturn(user)
        whenever(user.uid).thenReturn("account-A")
        val store: VaultEvidenceStore = mock()
        val vault: VaultDao = mock()
        val accountSync: FirestoreSyncService = mock()
        whenever(accountSync.syncAccountFileKey("account-A")).thenReturn(false)
        val service = VaultProofSyncService(store, vault, Provider { accountSync })

        // The initial auth lookup and rejected agreement execute on this test thread.
        Mockito.mockStatic(FirebaseAuth::class.java).use { firebaseAuth ->
            firebaseAuth.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }.thenReturn(auth)
            try {
                service.syncCurrentItem("proof-item")
                fail("Proof backup must remain pending until the account key is confirmed")
            } catch (failure: IllegalStateException) {
                assertTrue(failure.message.orEmpty().contains("Recover the account file key"))
            }
        }

        verify(accountSync).syncAccountFileKey("account-A")
        verifyNoInteractions(store, vault)
    }
}
