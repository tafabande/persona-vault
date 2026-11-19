package com.pims.vault.storage

import android.content.Context
import com.pims.vault.core.crypto.HardenedCryptoEngine
import com.pims.vault.core.crypto.CryptoIntegrityException
import com.pims.vault.core.crypto.KeySecurityManager
import com.pims.vault.core.crypto.PortableFileKeyManager
import com.pims.vault.crypto.KeySecurityManagerSafetyRegressionTest
import com.pims.vault.data.local.storage.EncryptedFileStorageImpl
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.security.KeyStore
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.crypto.KeyGenerator
import kotlin.coroutines.CoroutineContext

class AtomicMediaStorageTest {
    @get:Rule val temporary = TemporaryFolder()
    private val key = ByteArray(32) { 7 }
    private val crypto = HardenedCryptoEngine()
    private fun storage(provider: () -> ByteArray = { key.copyOf() }): EncryptedFileStorageImpl {
        val context: Context = mock()
        whenever(context.filesDir).thenReturn(temporary.root)
        whenever(context.cacheDir).thenReturn(temporary.newFolder())
        return EncryptedFileStorageImpl(context, crypto, provider)
    }
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    @Test fun interruptedWritePreservesPublishedCiphertext(): Unit = runBlocking {
        val files = storage()
        val content = "Previously saved document".toByteArray()
        val old = files.storeEncryptedFile("version-id", 1, "application/pdf", content.inputStream())
        val failed = object : InputStream() {
            override fun read(): Int = throw java.io.IOException("Source disappeared")
        }
        try { files.storeEncryptedFile("version-id", 1, "application/pdf", failed); fail("Import should fail") }
        catch (_: java.io.IOException) { }
        val result = ByteArrayOutputStream()
        files.readDecryptedFile(old.relativePath, old.encryptionIvHex, old.sha256Hex, result)
        assertArrayEquals(content, result.toByteArray())
        assertEquals(1, File(temporary.root, "vault_documents/version-id").listFiles()!!.size)
    }

    @Test fun deletingOriginalLeavesOwnedCopyAndPortableExportReadable(): Unit = runBlocking {
        val files = storage()
        val content = ByteArray(170_000) { (it % 251).toByte() }
        val original = temporary.newFile().apply { writeBytes(content) }
        val stored = files.storeEncryptedFile("owned-version", 1, "image/png", original.inputStream())
        assertTrue(original.delete())
        files.prepareEncryptedExport(stored.relativePath, stored.encryptionIvHex, stored.sha256Hex).use { export ->
            val encrypted = export.openStream().use { it.readBytes() }
            assertFalse(encrypted.contentEquals(content))
            val restored = files.storeRestoredChunkedCiphertext(export.storageId, 2, "image/png", encrypted.inputStream(), hash(content), content.size.toLong())
            val result = ByteArrayOutputStream()
            files.readDecryptedFile(restored.relativePath, restored.encryptionIvHex, restored.sha256Hex, result)
            assertArrayEquals(content, result.toByteArray())
        }
        assertTrue(files.verifyIntegrity(stored.relativePath, stored.encryptionIvHex, stored.sha256Hex))
    }

    @Test fun legacySingleShotCopiesRemainReadableAndAllCandidateKeysAreWiped(): Unit = runBlocking {
        val current = ByteArray(32) { 5 }
        val legacy = ByteArray(32) { 9 }
        val currentCopy = current.copyOf()
        val legacyCopy = legacy.copyOf()
        val context: Context = mock()
        whenever(context.filesDir).thenReturn(temporary.root)
        val files = EncryptedFileStorageImpl(context, crypto, { currentCopy }, legacyKeyProvider = { legacyCopy })
        val data = "Legacy private document".toByteArray()
        val encrypted = crypto.encrypt(data, legacy)
        val path = "vault_documents/historical/v_1.penc"
        File(temporary.root, path).apply { parentFile!!.mkdirs(); writeBytes(encrypted.combinedCiphertextWithTag) }
        val result = ByteArrayOutputStream()
        files.readDecryptedFile(path, encrypted.iv.joinToString("") { "%02x".format(it) }, hash(data), result)
        assertArrayEquals(data, result.toByteArray())
        assertTrue(currentCopy.all { it == 0.toByte() })
        assertTrue(legacyCopy.all { it == 0.toByte() })
    }

    @Test fun siblingPrefixTraversalIsRejectedWithoutCreatingFolders(): Unit = runBlocking {
        val files = storage()
        try { files.storeEncryptedFile("../vault_documents_other", 1, "image/png", byteArrayOf(1).inputStream()); fail("Unsafe path accepted") }
        catch (_: com.pims.vault.core.crypto.PathTraversalException) { }
        assertFalse(File(temporary.root, "vault_documents_other").exists())
    }

    @Test fun corruptCloudCopyCannotReplacePreviouslySavedMedia(): Unit = runBlocking {
        val files = storage()
        val content = "Retain this version".toByteArray()
        val stored = files.storeEncryptedFile("stable", 1, "application/pdf", content.inputStream())
        val before = File(temporary.root, stored.relativePath).readBytes()
        try {
            files.storeRestoredChunkedCiphertext("stable", 1, "application/pdf", ByteArray(32).inputStream(), hash(content), content.size.toLong())
            fail("Malformed cloud file was accepted")
        } catch (_: Exception) { }
        assertArrayEquals(before, File(temporary.root, stored.relativePath).readBytes())
    }

    @Test fun historicalAbsoluteAppPathStillFindsTheOwnedCopy(): Unit = runBlocking {
        val files = storage()
        val data = "Historical path".toByteArray()
        val metadata = files.storeEncryptedFile("legacy-path", 1, "application/pdf", data.inputStream())
        val result = ByteArrayOutputStream()
        files.readDecryptedFile(File(temporary.root, metadata.relativePath).absolutePath,
            metadata.encryptionIvHex, metadata.sha256Hex, result)
        assertArrayEquals(data, result.toByteArray())
    }

    @Test fun unavailableRestoreKeysRemoveStagingAndCloseTheDownload(): Unit = runBlocking {
        val files = storage { throw IllegalStateException("Keys unavailable") }
        var closed = false
        val input = object : java.io.ByteArrayInputStream(byteArrayOf(1, 2, 3)) {
            override fun close() { closed = true; super.close() }
        }
        try {
            files.storeRestoredChunkedCiphertext("pending-restore", 1, "application/pdf", input,
                hash(byteArrayOf(1)), 1)
            fail("Restore should fail while keys are unavailable")
        } catch (_: CryptoIntegrityException) { }
        assertTrue("A failed restore must close the download", closed)
        assertTrue(File(temporary.root, "vault_documents/pending-restore").listFiles().orEmpty().isEmpty())
    }

    @Test fun cancelledExportHandoffRemovesItsOwnedEncryptedTemporaryFile() = runBlocking {
        val context: Context = mock()
        val cache = temporary.newFolder("export-cache")
        whenever(context.filesDir).thenReturn(temporary.root)
        whenever(context.cacheDir).thenReturn(cache)
        val files = EncryptedFileStorageImpl(context, crypto, { key.copyOf() })
        val content = ByteArray(80_000) { 13 }
        val stored = files.storeEncryptedFile("cancelled-export", 1, "application/pdf", content.inputStream())
        val dispatcher = QueuedDispatcher()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        var handedOff = false
        val job = scope.launch {
            files.prepareEncryptedExport(stored.relativePath, stored.encryptionIvHex, stored.sha256Hex).use {
                handedOff = true
            }
        }
        try {
            // Run the caller until it suspends on the IO dispatcher.
            dispatcher.next().run()
            // The next queued dispatch is the completed IO result, before the caller can own it.
            val resultDelivery = dispatcher.next()
            job.cancel()
            resultDelivery.run()
            dispatcher.drain()
            assertTrue(job.isCompleted)
            assertFalse(handedOff)
            assertTrue("Cancelled result delivery must clean the encrypted export",
                File(cache, "encrypted_uploads").listFiles().orEmpty().isEmpty())
            assertTrue(files.verifyIntegrity(stored.relativePath, stored.encryptionIvHex, stored.sha256Hex))
        } finally { scope.cancel(); dispatcher.drain() }
    }

    @Test fun historicalPcsfExportUsesCurrentPortableKeyWithoutChangingOriginal(): Unit = runBlocking {
        val content = "Kept across the protocol upgrade.".toByteArray()
        val historical = java.util.Base64.getDecoder().decode(
            "UENTRgMDAwMDAwMDAwMDAwAAADFum9N3ekk9MBUzMHyfP5osnU6Pm9v2+xwPq/fWkYQk3Z9a1Bsc/1KwcGvYD9ZM0Fum")
        val path = "vault_documents/historical-version/v_1.penc"
        val original = File(temporary.root, path).apply { parentFile!!.mkdirs(); writeBytes(historical) }
        val portableKey = ByteArray(32) { 5 }
        val context: Context = mock()
        whenever(context.filesDir).thenReturn(temporary.root)
        whenever(context.cacheDir).thenReturn(temporary.newFolder())
        val legacyReader = EncryptedFileStorageImpl(context, crypto, { portableKey.copyOf() },
            legacyKeyProvider = { key.copyOf() })
        val portableReader = EncryptedFileStorageImpl(context, crypto, { portableKey.copyOf() })
        legacyReader.prepareEncryptedExport(path, "03".repeat(12), hash(content)).use { export ->
            val restored = portableReader.storeRestoredChunkedCiphertext(export.storageId, 2, "application/pdf",
                export.openStream(), hash(content), content.size.toLong())
            val output = ByteArrayOutputStream()
            portableReader.readDecryptedFile(restored.relativePath, restored.encryptionIvHex, restored.sha256Hex, output)
            assertArrayEquals(content, output.toByteArray())
        }
        assertArrayEquals(historical, original.readBytes())
    }

    @Test fun explicitKeyRecoveryKeepsOwnedMediaReadableAndExportsWithTheRecoveredKey(): Unit = runBlocking {
        val context: Context = mock()
        whenever(context.filesDir).thenReturn(temporary.root)
        whenever(context.cacheDir).thenReturn(temporary.newFolder())
        val masterKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val keyStore: KeyStore = mock()
        whenever(keyStore.containsAlias(anyOrNull())).thenReturn(true)
        whenever(keyStore.getKey(anyOrNull(), anyOrNull())).thenReturn(masterKey)
        val keySecurityManager = KeySecurityManager(
            keyStoreInstance = keyStore,
            sharedPreferences = KeySecurityManagerSafetyRegressionTest.FakeSharedPreferences()
        )
        val keyManager = PortableFileKeyManager(context, keySecurityManager, crypto,
            KeySecurityManagerSafetyRegressionTest.FakeSharedPreferences())
        val providedCopies = CopyOnWriteArrayList<ByteArray>()
        val files = EncryptedFileStorageImpl(
            context, crypto,
            keyProvider = { keyManager.copyKeyBytes().also { providedCopies.add(it) } },
            historicalKeyProvider = {
                keyManager.historicalKeyBytes().onEach { providedCopies.add(it) }
            }
        )
        val content = ByteArray(90_000) { (it % 247).toByte() }
        val stored = files.storeEncryptedFile("before-recovery", 1, "application/pdf", content.inputStream())
        val originalCiphertext = File(temporary.root, stored.relativePath).readBytes()
        val recoveredKey = ByteArray(32) { (it + 17).toByte() }
        try {
            keyManager.importRecoveredKey(recoveredKey, "recovery-test-key")
            keyManager.lock()

            val local = ByteArrayOutputStream()
            files.readDecryptedFile(stored.relativePath, stored.encryptionIvHex, stored.sha256Hex, local)
            assertArrayEquals(content, local.toByteArray())

            // The online copy must be readable using only the recovered key on another device.
            val recoveredReader = EncryptedFileStorageImpl(context, crypto, { recoveredKey.copyOf() })
            files.prepareEncryptedExport(stored.relativePath, stored.encryptionIvHex, stored.sha256Hex).use { export ->
                val restored = recoveredReader.storeRestoredChunkedCiphertext(export.storageId, 2, "application/pdf",
                    export.openStream(), stored.sha256Hex, content.size.toLong())
                val remote = ByteArrayOutputStream()
                recoveredReader.readDecryptedFile(restored.relativePath, restored.encryptionIvHex, restored.sha256Hex, remote)
                assertArrayEquals(content, remote.toByteArray())
            }

            assertArrayEquals(originalCiphertext, File(temporary.root, stored.relativePath).readBytes())
            assertTrue(providedCopies.isNotEmpty())
            assertTrue("Current and historical key copies must be wiped", providedCopies.all { copy ->
                copy.all { it == 0.toByte() }
            })
        } finally {
            recoveredKey.fill(0)
            keyManager.lock()
        }
    }

    private class QueuedDispatcher : CoroutineDispatcher() {
        private val work = LinkedBlockingQueue<Runnable>()
        override fun dispatch(context: CoroutineContext, block: Runnable) { work.add(block) }
        fun next(): Runnable = checkNotNull(work.poll(10, TimeUnit.SECONDS)) { "Coroutine dispatch did not arrive" }
        fun drain() { while (true) (work.poll() ?: return).run() }
    }
}
