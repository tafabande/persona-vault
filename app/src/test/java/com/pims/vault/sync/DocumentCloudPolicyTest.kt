package com.pims.vault.sync

import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.core.sync.DocumentSyncPolicy
import com.pims.vault.core.sync.VerifiedDocumentCipherInputStream
import com.pims.vault.data.local.entity.DocumentEntity
import com.pims.vault.data.local.entity.DocumentVersionEntity
import com.pims.vault.document.InMemoryDocumentDao
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DocumentCloudPolicyTest {
    @Test fun completeLegacyMetadataAndSparsePatchPreserveCollectedFields() {
        val local = DocumentEntity("doc", "owner", DocumentType.PASSPORT, "Passport", documentNumber = "NUMBER",
            issuingAuthority = "Issuer", issuingCountry = "ZW", issueDate = "2024-01-01", expirationDate = "2034-01-01",
            securityClassification = SecurityClassification.ZONE_3_SENSITIVE, notes = "Private", createdAt = 10, updatedAt = 20, isLocked = true)
        val fields = DocumentSyncPolicy.headerPatch(local)
        assertEquals(local, DocumentSyncPolicy.restoreHeader("doc", "owner", fields))
        val remote = fields + mapOf("updatedAt" to 30L, "documentNumber" to "REMOTE")
        val patch = DocumentSyncPolicy.headerPatchForRemote(local.copy(documentNumber = null), remote)
        assertFalse(patch.containsKey("documentNumber"))
    }

    @Test fun metadataRecoveryFillsMissingFieldsWithoutReplacingLocalEdits(): Unit = runBlocking {
        val dao = InMemoryDocumentDao()
        val local = DocumentEntity("doc", "owner", DocumentType.PASSPORT, "My title", documentNumber = "LOCAL",
            securityClassification = SecurityClassification.ZONE_4_CRITICAL, isLocked = true, createdAt = 0)
        dao.insertDoc(local)
        val remote = local.copy(title = "Remote title", documentNumber = "REMOTE", issuingAuthority = "Recovered issuer",
            securityClassification = SecurityClassification.ZONE_1_PERSONAL, isLocked = false, createdAt = 5)
        assertTrue(dao.fillMissingDocumentMetadata(local, remote))
        val restored = dao.documents.getValue("doc")
        assertEquals("My title", restored.title)
        assertEquals("LOCAL", restored.documentNumber)
        assertEquals("Recovered issuer", restored.issuingAuthority)
        assertEquals(SecurityClassification.ZONE_4_CRITICAL, restored.securityClassification)
        assertTrue(restored.isLocked)
        assertEquals(5L, restored.createdAt)
    }

    @Test fun immutableVersionFilenameRecoveryKeepsExistingMetadata(): Unit = runBlocking {
        val dao = InMemoryDocumentDao()
        val document = DocumentEntity("doc", "owner", DocumentType.OTHER, "Document")
        dao.insertDoc(document)
        val version = DocumentVersionEntity("version", "doc", 1, "vault_documents/version/v_1.penc", 4,
            "application/pdf", "a".repeat(64), "00".repeat(12), notes = "LOCAL", createdAt = 0)
        dao.insertVersion(version)
        assertEquals(1, dao.fillMissingVersionMetadata(version.id, "doc", version.sha256Hash, "passport.pdf", "REMOTE", 10))
        assertEquals("passport.pdf", dao.versions.getValue(version.id).originalFilename)
        assertEquals("LOCAL", dao.versions.getValue(version.id).notes)
        assertEquals(0, dao.fillMissingVersionMetadata(version.id, "doc", "b".repeat(64), "foreign.pdf", null, 15))
        assertEquals("passport.pdf", dao.versions.getValue(version.id).originalFilename)
    }

    @Test fun unsupportedOrIncompleteBinaryDoesNotCountAsConfirmed() {
        assertFalse(DocumentSyncPolicy.hasConfirmedBinary(mapOf("transportFormatVersion" to 99L)))
        assertFalse(DocumentSyncPolicy.hasConfirmedBinary(mapOf("transportFormatVersion" to 2L,
            "storageFormat" to "PCSF1", "b2DownloadUrl" to "pims-cloud://media/account/file")))
    }

    @Test fun truncatedAndCorruptedCipherDownloadsFailBeforePublication() {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        assertArrayEquals(bytes, VerifiedDocumentCipherInputStream(bytes.inputStream(), bytes.size.toLong(), hash).use { it.readBytes() })
        for (source in listOf(byteArrayOf(1, 2), byteArrayOf(9, 2, 3, 4))) {
            try { VerifiedDocumentCipherInputStream(source.inputStream(), 4, hash).use { it.readBytes() }; fail("Unverified cipher accepted") }
            catch (_: IllegalArgumentException) { }
        }
    }
}
