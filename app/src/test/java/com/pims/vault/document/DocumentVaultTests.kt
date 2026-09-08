package com.pims.vault.document

import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.core.storage.StoredFileMetadata
import com.pims.vault.data.local.dao.DocumentDao
import com.pims.vault.domain.model.DocumentVersionItem
import com.pims.vault.domain.model.IntegrityCheckStatus
import com.pims.vault.domain.rules.DocumentIngestionException
import com.pims.vault.domain.rules.DocumentRules
import com.pims.vault.domain.rules.DocumentValidationException
import com.pims.vault.domain.usecase.document.AddDocumentVersionUseCase
import com.pims.vault.domain.usecase.document.IngestDocumentUseCase
import com.pims.vault.domain.usecase.document.RestoreVersionUseCase
import com.pims.vault.domain.usecase.document.VerifyDocumentIntegrityUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import java.io.ByteArrayInputStream

class DocumentVaultTests {

    private lateinit var documentDao: DocumentDao
    private lateinit var fileStorage: FileStorageService
    private lateinit var auditLogger: HardenedAuditLogger

    private lateinit var ingestDocumentUseCase: IngestDocumentUseCase
    private lateinit var addDocumentVersionUseCase: AddDocumentVersionUseCase
    private lateinit var restoreVersionUseCase: RestoreVersionUseCase
    private lateinit var verifyDocumentIntegrityUseCase: VerifyDocumentIntegrityUseCase

    @Before
    fun setUp() {
        documentDao = Mockito.mock(DocumentDao::class.java)
        fileStorage = Mockito.mock(FileStorageService::class.java)
        auditLogger = Mockito.mock(HardenedAuditLogger::class.java)

        ingestDocumentUseCase = IngestDocumentUseCase(documentDao, fileStorage, auditLogger)
        addDocumentVersionUseCase = AddDocumentVersionUseCase(documentDao, fileStorage, auditLogger)
        restoreVersionUseCase = RestoreVersionUseCase(documentDao, fileStorage, addDocumentVersionUseCase, auditLogger)
        verifyDocumentIntegrityUseCase = VerifyDocumentIntegrityUseCase(fileStorage, auditLogger)
    }

    @Test
    fun testDefaultClassificationMapping() {
        assertEquals(SecurityClassification.ZONE_3_SENSITIVE, DocumentRules.resolveDefaultClassification(DocumentType.PASSPORT))
        assertEquals(SecurityClassification.ZONE_3_SENSITIVE, DocumentRules.resolveDefaultClassification(DocumentType.NATIONAL_ID))
        assertEquals(SecurityClassification.ZONE_2_PRIVATE, DocumentRules.resolveDefaultClassification(DocumentType.CURRICULUM_VITAE))
        assertEquals(SecurityClassification.ZONE_2_PRIVATE, DocumentRules.resolveDefaultClassification(DocumentType.ACADEMIC_CERTIFICATE))
        assertEquals(SecurityClassification.ZONE_1_PERSONAL, DocumentRules.resolveDefaultClassification(DocumentType.PASSPORT_PHOTO))
    }

    @Test(expected = DocumentValidationException::class)
    fun testUnsupportedMimeTypeThrowsValidationException() {
        DocumentRules.validateIngestionMetadata(
            title = "MaliciousScript",
            mimeType = "application/x-executable"
        )
    }

    @Test(expected = DocumentValidationException::class)
    fun testOverSizedDocumentThrowsValidationException() {
        DocumentRules.validateIngestionMetadata(
            title = "EnormousScan",
            mimeType = "application/pdf",
            sizeBytes = 60 * 1024 * 1024L // 60MB > 50MB limit
        )
    }

    @Test
    fun testIngestDocumentSuccess(): Unit = runBlocking {
        val testBytes = "EncryptedPassportBinary".toByteArray(Charsets.UTF_8)
        Mockito.`when`(fileStorage.storeEncryptedFile(any(), eq(1), any(), any()))
            .thenReturn(
                StoredFileMetadata(
                    relativePath = "vault_documents/doc_1/v_1.penc",
                    sizeBytes = testBytes.size.toLong(),
                    mimeType = "application/pdf",
                    sha256Hex = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                    encryptionIvHex = "00".repeat(12),
                    timestamp = System.currentTimeMillis()
                )
            )

        val docId = ingestDocumentUseCase(
            personId = "person_123",
            documentType = DocumentType.PASSPORT,
            title = "Passport Scan 2026",
            documentNumber = "FN999888",
            issuingAuthority = "Registrar General",
            issuingCountry = "Zimbabwe",
            issueDate = "2024-01-01",
            expirationDate = "2034-01-01",
            customClassification = null,
            initialFileStream = ByteArrayInputStream(testBytes),
            mimeType = "application/pdf"
        )

        assertNotNull(docId)
        verify(documentDao).insertDocument(any())
        verify(documentDao).insertVersion(any())
        verify(auditLogger).recordEvent(any(), eq("Document"), eq(docId), any(), any())
    }

    @Test
    fun testIngestionFailureDeletesTemporaryStorage(): Unit = runBlocking {
        Mockito.`when`(fileStorage.storeEncryptedFile(any(), eq(1), any(), any()))
            .thenReturn(
                StoredFileMetadata(
                    relativePath = "vault_documents/doc_fail/v_1.penc",
                    sizeBytes = 100L,
                    mimeType = "application/pdf",
                    sha256Hex = "fakehash",
                    encryptionIvHex = "00".repeat(12),
                    timestamp = System.currentTimeMillis()
                )
            )

        Mockito.`when`(fileStorage.deleteFile(any())).thenReturn(true)

        // Simulate database crash during metadata insert
        Mockito.`when`(documentDao.insertDocument(any()))
            .thenThrow(RuntimeException("Database disk full or constraint violation"))

        try {
            ingestDocumentUseCase(
                personId = "person_123",
                documentType = DocumentType.PASSPORT,
                title = "Failing Doc",
                documentNumber = null,
                issuingAuthority = null,
                issuingCountry = null,
                issueDate = null,
                expirationDate = null,
                customClassification = null,
                initialFileStream = ByteArrayInputStream(byteArrayOf(1, 2, 3)),
                mimeType = "application/pdf"
            )
            fail("Must throw DocumentIngestionException")
        } catch (e: DocumentIngestionException) {
            // Verify rollback triggered
            verify(fileStorage).deleteFile("vault_documents/doc_fail/v_1.penc")
        }
    }

    @Test
    fun testVerifyDocumentIntegrityOk(): Unit = runBlocking {
        Mockito.`when`(fileStorage.verifyIntegrity(any(), any(), any())).thenReturn(true)

        val version = DocumentVersionItem(
            id = "ver_1",
            documentId = "doc_1",
            versionNumber = 1,
            storagePath = "vault_documents/doc_1/v_1.penc",
            mimeType = "application/pdf",
            originalFilename = null,
            sizeBytes = 1024L,
            sha256Hex = "validsha256",
            encryptionIvHex = "00".repeat(12)
        )

        val status = verifyDocumentIntegrityUseCase(version)
        assertEquals(IntegrityCheckStatus.INTEGRITY_OK, status)
    }

    @Test
    fun testVerifyDocumentIntegrityTampered(): Unit = runBlocking {
        Mockito.`when`(fileStorage.verifyIntegrity(any(), any(), any())).thenReturn(false)

        val version = DocumentVersionItem(
            id = "ver_1",
            documentId = "doc_1",
            versionNumber = 1,
            storagePath = "vault_documents/doc_1/v_1.penc",
            mimeType = "application/pdf",
            originalFilename = null,
            sizeBytes = 1024L,
            sha256Hex = "validsha256",
            encryptionIvHex = "00".repeat(12)
        )

        val status = verifyDocumentIntegrityUseCase(version)
        assertEquals(IntegrityCheckStatus.INTEGRITY_FAILED, status)
    }
}
