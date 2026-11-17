package com.pims.vault.storage

import android.content.Context
import android.net.Uri
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.CryptoIntegrityException
import com.pims.vault.core.crypto.HardenedCryptoEngine
import com.pims.vault.core.storage.VaultEvidenceStore
import com.pims.vault.data.local.dao.VaultProofDao
import com.pims.vault.data.local.entity.VaultProofEntity
import com.pims.vault.data.local.storage.EncryptedFileStorageImpl
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.*

class VaultEvidenceStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    private val context = mock(Context::class.java)
    private val dao = mock(VaultProofDao::class.java)
    private val session = mock(BiometricSessionManager::class.java)
    private val crypto = HardenedCryptoEngine()
    private val key = ByteArray(32) { 17 }

    private fun store(): VaultEvidenceStore {
        `when`(context.filesDir).thenReturn(temporary.root)
        `when`(session.isZone4Unlocked()).thenReturn(true)
        `when`(session.getZone4VaultKey()).thenAnswer { key.copyOf() }
        return VaultEvidenceStore(context, crypto, session, dao)
    }

    @Test fun lockedVaultRejectsImportBeforeOpeningProvider(): Unit = runBlocking {
        val store = store()
        `when`(session.isZone4Unlocked()).thenReturn(false)
        try {
            store.add("card", "owner", mock(Uri::class.java))
            fail("Locked image import was accepted")
        } catch (_: IllegalStateException) { }
        verify(context, never()).contentResolver
        verifyNoInteractions(dao)
    }

    @Test fun ownerMismatchRejectsImportBeforeOpeningProvider(): Unit = runBlocking {
        val store = store()
        `when`(dao.isOwnedItem("card", "other-person")).thenReturn(false)
        try {
            store.add("card", "other-person", mock(Uri::class.java))
            fail("Foreign image import was accepted")
        } catch (_: IllegalStateException) { }
        verify(context, never()).contentResolver
    }

    @Test fun tamperedImageCannotReachBitmapDecoder(): Unit = runBlocking {
        val store = store()
        val id = VaultEvidenceStore.documentId("card", "proof")
        val writer = EncryptedFileStorageImpl(context, crypto, { key.copyOf() }, "vault_proofs")
        val metadata = writer.storeEncryptedFile(id, 1, "image/png", ByteArray(90_000) { 7 }.inputStream())
        val proof = VaultProofEntity("proof", "card", metadata.relativePath, "image/png", metadata.sizeBytes,
            metadata.sha256Hex, 1, 1)
        `when`(dao.isOwnedItem("card", "owner")).thenReturn(true)
        `when`(dao.getById("proof")).thenReturn(proof)
        val encrypted = File(temporary.root, metadata.relativePath)
        val bytes = encrypted.readBytes().apply { this[lastIndex] = (last().toInt() xor 1).toByte() }
        encrypted.writeBytes(bytes)
        try {
            store.preview(proof, "owner")
            fail("Tampered image was displayed")
        } catch (_: CryptoIntegrityException) { }
    }

    @Test fun delayedCloudImageCannotResurrectLocalRemoval(): Unit = runBlocking {
        val store = store()
        val proof = VaultProofEntity("proof", "card", "vault_proofs/unused/v_1.penc", "image/png", 12, "hash", 1, 1, isDeleted = true)
        `when`(dao.getById("proof")).thenReturn(proof)
        store.restoreCiphertext(proof.copy(isDeleted = false), "owner", byteArrayOf(1))
        verify(dao, never()).insertForOwnedItem(org.mockito.kotlin.any<VaultProofEntity>(), org.mockito.kotlin.eq("owner"))
        assertFalse(File(temporary.root, proof.storagePath).exists())
    }

    @Test fun parentDeletionRetainsTombstonesAndRemovesOnlyCiphertext(): Unit = runBlocking {
        val store = store()
        val path = "vault_proofs/${VaultEvidenceStore.documentId("card", "proof")}/v_1.penc"
        val proof = VaultProofEntity("proof", "card", path, "image/png", 12, "hash", 1, 1)
        val file = File(temporary.root, path).apply { checkNotNull(parentFile).mkdirs(); writeBytes(byteArrayOf(1, 2, 3)) }
        `when`(dao.getForItem("card")).thenReturn(listOf(proof.copy(isDeleted = true)))
        store.deleteItem("card")
        verify(dao).deleteItemAndTombstone(org.mockito.kotlin.eq("card"), anyLong(), org.mockito.kotlin.any())
        verify(dao, never()).delete("proof")
        assertFalse(file.exists())
    }

    @Test fun anotherAccountsTombstoneCannotRemoveLocalEvidence(): Unit = runBlocking {
        val store = store()
        val proof = VaultProofEntity("proof", "card", "vault_proofs/fixture/v_1.penc", "image/png", 12, "hash", 1, 1, "account-a")
        `when`(dao.getById("proof")).thenReturn(proof)
        try {
            store.applyRemoteDeletion("proof", "card", 2, "account-b")
            fail("Foreign deletion was accepted")
        } catch (_: IllegalArgumentException) { }
        verify(dao, never()).markDeleted(org.mockito.kotlin.any(), anyLong())
    }

    @Test fun failedCloudRepairPreservesPreviousCiphertext(): Unit = runBlocking {
        val store = store()
        val path = "vault_proofs/${VaultEvidenceStore.documentId("card", "proof")}/v_1.penc"
        val proof = VaultProofEntity("proof", "card", path, "image/png", 12, "hash", 1, 1)
        `when`(dao.getById("proof")).thenReturn(proof)
        val original = byteArrayOf(9, 8, 7)
        val file = File(temporary.root, path).apply { checkNotNull(parentFile).mkdirs(); writeBytes(original) }
        try {
            store.restoreCiphertext(proof, "owner", byteArrayOf(1), replaceDamagedFile = true)
            fail("Unauthenticated repair was accepted")
        } catch (_: Exception) { }
        assertArrayEquals(original, file.readBytes())
        assertFalse(File(file.parentFile, "v_2.penc").exists())
    }

    @Test fun lockedRepairCannotReplaceAnExistingValidProof(): Unit = runBlocking {
        val store = store()
        val proof = validProof(ByteArray(200) { 4 })
        val file = File(temporary.root, proof.storagePath)
        val original = file.readBytes()
        `when`(session.isZone4Unlocked()).thenReturn(false)

        runCatching { store.restoreCiphertext(proof, "owner", byteArrayOf(1, 2, 3), replaceDamagedFile = true) }

        assertArrayEquals(original, file.readBytes())
        verify(dao, never()).insertForOwnedItem(org.mockito.kotlin.any(), org.mockito.kotlin.any())
        assertFalse(File(file.parentFile, "v_2.penc").exists())
    }

    @Test fun aValidLocalProofSurvivesDifferentAuthenticatedCloudCiphertext(): Unit = runBlocking {
        val store = store()
        val plain = ByteArray(200) { 5 }
        val proof = validProof(plain)
        val file = File(temporary.root, proof.storagePath)
        val original = file.readBytes()
        val remote = ciphertextFor(proof, plain)
        assertFalse(original.contentEquals(remote))

        store.restoreCiphertext(proof, "owner", remote, replaceDamagedFile = true)

        assertArrayEquals(original, file.readBytes())
        verify(dao, never()).insertForOwnedItem(org.mockito.kotlin.any(), org.mockito.kotlin.any())
        assertFalse(File(file.parentFile, "v_2.penc").exists())
    }

    @Test fun elevatedRepairAuthenticatesTheReplacementBeforeReplacingCorruptProof(): Unit = runBlocking {
        val store = store()
        val plain = ByteArray(200) { 6 }
        val proof = validProof(plain)
        val file = File(temporary.root, proof.storagePath)
        val remote = ciphertextFor(proof, plain)
        file.writeBytes(file.readBytes().apply { this[lastIndex] = (last().toInt() xor 1).toByte() })

        store.restoreCiphertext(proof, "owner", remote, replaceDamagedFile = true)

        assertArrayEquals(remote, file.readBytes())
        val reader = EncryptedFileStorageImpl(context, crypto, { key.copyOf() }, "vault_proofs")
        assertTrue(reader.verifyIntegrity(proof.storagePath, "", proof.sha256Hash))
        verify(dao).insertForOwnedItem(proof, "owner")
        assertFalse(File(file.parentFile, "v_2.penc").exists())
    }

    @Test fun anInvalidCloudRepairRetainsTheCorruptSourceForLaterRecovery(): Unit = runBlocking {
        val store = store()
        val proof = validProof(ByteArray(200) { 7 })
        val file = File(temporary.root, proof.storagePath)
        val damaged = file.readBytes().apply { this[lastIndex] = (last().toInt() xor 1).toByte() }
        file.writeBytes(damaged)

        val failure = runCatching {
            store.restoreCiphertext(proof, "owner", byteArrayOf(1, 2, 3), replaceDamagedFile = true)
        }.exceptionOrNull()

        assertNotNull(failure)
        assertArrayEquals(damaged, file.readBytes())
        verify(dao, never()).insertForOwnedItem(org.mockito.kotlin.any(), org.mockito.kotlin.any())
        assertFalse(File(file.parentFile, "v_2.penc").exists())
    }

    private suspend fun validProof(plain: ByteArray): VaultProofEntity {
        val id = VaultEvidenceStore.documentId("card", "proof")
        val writer = EncryptedFileStorageImpl(context, crypto, { key.copyOf() }, "vault_proofs")
        val metadata = writer.storeEncryptedFile(id, 1, "image/png", plain.inputStream())
        return VaultProofEntity("proof", "card", metadata.relativePath, "image/png", metadata.sizeBytes,
            metadata.sha256Hex, 1, 1).also { proof ->
            `when`(dao.getById(proof.id)).thenReturn(proof)
            `when`(dao.isOwnedItem(proof.itemId, "owner")).thenReturn(true)
        }
    }

    private fun ciphertextFor(proof: VaultProofEntity, plain: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        crypto.encryptChunkedStream(plain.inputStream(), output, key,
            VaultEvidenceStore.documentId(proof.itemId, proof.id).toByteArray(Charsets.UTF_8))
        return output.toByteArray()
    }
}
