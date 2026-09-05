package com.pims.vault.storage

import android.content.Context
import com.pims.vault.core.crypto.HardenedCryptoEngine
import com.pims.vault.core.crypto.PathTraversalException
import com.pims.vault.data.local.storage.EncryptedFileStorageImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

class FileSandboxPathTraversalTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var storage: EncryptedFileStorageImpl
    private val testKey = ByteArray(32) { 0x33 }

    @Before
    fun setUp() {
        val rootFilesDir = tempFolder.newFolder("mock_files_dir")
        mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.filesDir).thenReturn(rootFilesDir)

        storage = EncryptedFileStorageImpl(
            context = mockContext,
            cryptoEngine = HardenedCryptoEngine(),
            keyProvider = { testKey },
            baseDirectoryName = "vault_documents"
        )
    }

    @Test
    fun testStoreAndReadValidFileWithinSandbox() = runBlocking {
        val inputData = "EncryptedCertificateData".toByteArray(Charsets.UTF_8)
        val meta = storage.storeEncryptedFile(
            documentId = "doc_123",
            versionNumber = 1,
            mimeType = "application/pdf",
            inputStream = ByteArrayInputStream(inputData)
        )

        val readOut = ByteArrayOutputStream()
        storage.readDecryptedFile(
            relativePath = meta.relativePath,
            encryptionIvHex = meta.encryptionIvHex,
            expectedSha256Hex = meta.sha256Hex,
            outputStream = readOut
        )

        assertTrue(inputData.contentEquals(readOut.toByteArray()))
    }

    @Test
    fun testPathTraversalWithDotDotIsBlocked() = runBlocking {
        val maliciousPath = "vault_documents/../../system_file.txt"
        val out = ByteArrayOutputStream()

        try {
            storage.readDecryptedFile(
                relativePath = maliciousPath,
                encryptionIvHex = "00".repeat(12),
                expectedSha256Hex = "fakehash",
                outputStream = out
            )
            fail("Path traversal attempt must throw PathTraversalException")
        } catch (e: PathTraversalException) {
            assertTrue(e.message!!.contains("outside"))
        }
    }
}
