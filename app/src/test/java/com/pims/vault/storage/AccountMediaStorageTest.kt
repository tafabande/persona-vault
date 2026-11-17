package com.pims.vault.storage

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.PortableFileKeyManager
import com.pims.vault.core.storage.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*
import java.io.IOException
import java.security.MessageDigest

class AccountMediaStorageTest {
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private val blob = "549df0fa-5c57-4980-9d44-2fbd3c1a9833"
    private fun fields(size: Long, hash: String) = mapOf("ownerUid" to "account-a", "storageVersion" to 1,
        "isDeleted" to false, "cipherSize" to size, "cipherHash" to hash,
        "chunkCount" to ((size + CHUNK_BYTES - 1) / CHUNK_BYTES).toInt())

    @Test fun mediaAddressesRoundTripWithoutAcceptingOtherSchemesOrPaths() {
        val reference = AccountMediaReference("account-a", blob)
        assertEquals(reference, AccountMediaReference.parse(reference.uri()))
        assertNull(AccountMediaReference.parse("https://media/account-a/$blob"))
        assertNull(AccountMediaReference.parse("pims-cloud://other/account-a/$blob"))
        assertNull(AccountMediaReference.parse("pims-cloud://media/account-a/$blob/extra"))
        assertNull(AccountMediaReference.parse("pims-cloud://media/account-a/$blob?token=value"))
        assertNull(AccountMediaReference.parse("pims-cloud://media/account%2Fa/$blob"))
    }

    @Test fun manifestRejectsForeignOwnerDeletedObjectsAndImpossibleBounds() {
        val good = fields(1, hash(byteArrayOf(1)))
        val invalid = listOf(good + ("ownerUid" to "account-b"), good + ("isDeleted" to true),
            good + ("storageVersion" to 2), good + ("cipherSize" to MAX_CIPHER_BYTES + 1),
            good + ("chunkCount" to 2), good + ("cipherHash" to "not-a-checksum"))
        invalid.forEach { metadata ->
            try { AccountMediaManifest.from(metadata, "account-a"); fail("Invalid object was accepted") }
            catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun chunkedDownloadReadsTheWholeCiphertextAndWipesOwnedBuffers() {
        val bytes = ByteArray(CHUNK_BYTES + 37) { (it % 251).toByte() }
        val manifest = AccountMediaManifest.from(fields(bytes.size.toLong(), hash(bytes)), "account-a")
        val fetched = mutableListOf<ByteArray>()
        val stream = AccountMediaChunkInputStream(manifest) { index ->
            bytes.copyOfRange(index * CHUNK_BYTES, minOf(bytes.size, (index + 1) * CHUNK_BYTES)).also(fetched::add)
        }
        assertArrayEquals(bytes, stream.use { it.readBytes() })
        assertEquals(2, fetched.size)
        assertTrue(fetched.all { chunk -> chunk.all { it == 0.toByte() } })
    }

    @Test fun checksumFailureAndTruncatedChunksCannotProduceACompleteDownload() {
        val data = byteArrayOf(1, 2, 3)
        val wrongHash = AccountMediaManifest.from(fields(3, hash(byteArrayOf(9, 9, 9))), "account-a")
        try { AccountMediaChunkInputStream(wrongHash) { data.copyOf() }.use { it.readBytes() }; fail("Checksum mismatch accepted") }
        catch (_: IOException) { }
        val valid = AccountMediaManifest.from(fields(3, hash(data)), "account-a")
        try { AccountMediaChunkInputStream(valid) { byteArrayOf(1) }.use { it.readBytes() }; fail("Truncated chunk accepted") }
        catch (_: IOException) { }
    }

    @Test fun cancellingAnEarlyReadDoesNotFetchRemainingChunks() {
        val size = CHUNK_BYTES.toLong() + 1
        val first = ByteArray(CHUNK_BYTES) { 3 }
        val manifest = AccountMediaManifest(size, "a".repeat(64), 2)
        var fetched = 0
        val stream = AccountMediaChunkInputStream(manifest) { fetched++; first }
        stream.read(ByteArray(10))
        stream.close()
        assertEquals(1, fetched)
        assertTrue(first.all { it == 0.toByte() })
    }

    @Test fun uploadsAndPreferredAddressesRequireTheAuthenticatedOwner(): Unit = runBlocking {
        val auth: FirebaseAuth = mock()
        val user: FirebaseUser = mock()
        val crypto: CryptoEngine = mock()
        val portable: PortableFileKeyManager = mock()
        val legacy: B2StorageUploadService = mock()
        val service = AccountMediaStorageUploadService(auth, crypto, portable, legacy)
        assertFalse(service.isConfigured())
        assertFalse(service.isPreferredRemote("", "https://legacy.example/file"))
        whenever(auth.currentUser).thenReturn(user)
        whenever(user.uid).thenReturn("account-a")
        assertTrue(service.isConfigured())
        val ownUri = AccountMediaReference("account-a", blob).uri()
        assertTrue(service.isPreferredRemote(ownUri, ownUri))
        assertFalse(service.isPreferredRemote("", ownUri))
        assertFalse(service.isPreferredRemote("", AccountMediaReference("account-b", blob).uri()))
        try { service.uploadProfilePhoto("account-b", "avatar", "fixture", "image/png", byteArrayOf(1)); fail("Foreign upload accepted") }
        catch (_: IllegalStateException) { }
        verifyNoInteractions(crypto, portable, legacy)
    }
}
