package com.pims.vault.storage

import android.content.Context
import com.pims.vault.core.crypto.HardenedCryptoEngine
import com.pims.vault.data.local.storage.EncryptedFileStorageImpl
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito

class LegacyFileKeyCompatibilityTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun authenticatesLegacyDocumentBeforeEmittingAndWipesBothKeyCopies(): Unit = runBlocking {
        val context = Mockito.mock(Context::class.java)
        Mockito.`when`(context.filesDir).thenReturn(temporary.newFolder())
        val old = ByteArray(32) { 11 }
        val fresh = ByteArray(32) { 22 }
        val crypto = HardenedCryptoEngine()
        val writer = EncryptedFileStorageImpl(context, crypto, { old.copyOf() })
        val data = ByteArray(130_000) { (it % 251).toByte() }
        val metadata = writer.storeEncryptedFile("stable-document", 1, "application/pdf", ByteArrayInputStream(data))
        val currentCopy = fresh.copyOf()
        val legacyCopy = old.copyOf()
        val reader = EncryptedFileStorageImpl(context, crypto, { currentCopy }, legacyKeyProvider = { legacyCopy })
        val output = ByteArrayOutputStream()
        reader.readDecryptedFile(metadata.relativePath, metadata.encryptionIvHex, metadata.sha256Hex, output)
        assertArrayEquals(data, output.toByteArray())
        assertTrue(currentCopy.all { it == 0.toByte() })
        assertTrue(legacyCopy.all { it == 0.toByte() })
    }

    @Test fun checksumFailureEmitsNoPlaintext(): Unit = runBlocking {
        val context = Mockito.mock(Context::class.java)
        Mockito.`when`(context.filesDir).thenReturn(temporary.newFolder())
        val key = ByteArray(32) { 11 }
        val storage = EncryptedFileStorageImpl(context, HardenedCryptoEngine(), { key.copyOf() })
        val metadata = storage.storeEncryptedFile("document", 1, "application/pdf", ByteArrayInputStream(ByteArray(80_000) { 7 }))
        val output = ByteArrayOutputStream()
        try {
            storage.readDecryptedFile(metadata.relativePath, metadata.encryptionIvHex, "incorrect", output)
            fail("Expected integrity rejection")
        } catch (_: com.pims.vault.core.crypto.CryptoIntegrityException) { }
        assertEquals(0, output.size())
    }
}
