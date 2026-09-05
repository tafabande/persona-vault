package com.pims.vault.domain.usecase.document

import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.model.AuditEventType
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.data.local.dao.DocumentDao
import com.pims.vault.data.local.entity.DocumentEntity
import com.pims.vault.data.local.entity.DocumentVersionEntity
import com.pims.vault.domain.model.DocumentCategory
import com.pims.vault.domain.model.DocumentItem
import com.pims.vault.domain.model.DocumentVersionItem
import com.pims.vault.domain.model.DocumentWithHistory
import com.pims.vault.domain.model.IntegrityCheckStatus
import com.pims.vault.domain.rules.DocumentIngestionException
import com.pims.vault.domain.rules.DocumentRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject

class GetDocumentsUseCase @Inject constructor(
    private val documentDao: DocumentDao
) {
    operator fun invoke(personId: String): Flow<List<DocumentWithHistory>> {
        return documentDao.getDocumentsForPersonFlow(personId).map { docWithVersionsList ->
            docWithVersionsList.map { item ->
                val category = DocumentRules.resolveCategory(item.document.documentType)
                val domainDoc = DocumentItem(
                    id = item.document.id,
                    ownerId = item.document.personId,
                    category = category,
                    documentType = item.document.documentType,
                    title = item.document.title,
                    documentNumber = item.document.documentNumber,
                    issuingAuthority = item.document.issuingAuthority,
                    issuingCountry = item.document.issuingCountry,
                    issueDate = item.document.issueDate,
                    expirationDate = item.document.expirationDate,
                    securityClassification = item.document.securityClassification,
                    notes = item.document.notes,
                    createdAt = item.document.createdAt,
                    updatedAt = item.document.updatedAt
                )

                val domainVersions = item.versions.sortedByDescending { it.versionNumber }.map { v ->
                    DocumentVersionItem(
                        id = v.id,
                        documentId = v.documentId,
                        versionNumber = v.versionNumber,
                        storagePath = v.fileStoragePath,
                        mimeType = v.mimeType,
                        originalFilename = null,
                        sizeBytes = v.fileSizeBytes,
                        sha256Hex = v.sha256Hash,
                        encryptionIvHex = v.encryptionIv,
                        isCurrent = (v.versionNumber == item.latestVersion?.versionNumber),
                        notes = v.notes,
                        createdAt = v.createdAt
                    )
                }

                DocumentWithHistory(
                    document = domainDoc,
                    versions = domainVersions
                )
            }
        }
    }
}

class IngestDocumentUseCase @Inject constructor(
    private val documentDao: DocumentDao,
    private val fileStorage: FileStorageService,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        personId: String,
        documentType: DocumentType,
        title: String,
        documentNumber: String?,
        issuingAuthority: String?,
        issuingCountry: String?,
        issueDate: String?,
        expirationDate: String?,
        customClassification: SecurityClassification?,
        initialFileStream: InputStream,
        mimeType: String,
        originalFilename: String? = null,
        notes: String? = null
    ): String {
        DocumentRules.validateIngestionMetadata(title = title, mimeType = mimeType)
        val classification = customClassification ?: DocumentRules.resolveDefaultClassification(documentType)
        val docId = UUID.randomUUID().toString()

        var storedPath: String? = null
        try {
            // 1. Stream into chunked encrypted storage & compute SHA-256 on the fly
            val meta = fileStorage.storeEncryptedFile(
                documentId = docId,
                versionNumber = 1,
                mimeType = mimeType,
                inputStream = initialFileStream
            )
            storedPath = meta.relativePath

            // 2. Commit Document Entity
            val docEntity = DocumentEntity(
                id = docId,
                personId = personId,
                documentType = documentType,
                title = title.trim(),
                documentNumber = documentNumber?.trim()?.takeIf { it.isNotBlank() },
                issuingAuthority = issuingAuthority?.trim()?.takeIf { it.isNotBlank() },
                issuingCountry = issuingCountry?.trim()?.takeIf { it.isNotBlank() },
                issueDate = issueDate?.trim()?.takeIf { it.isNotBlank() },
                expirationDate = expirationDate?.trim()?.takeIf { it.isNotBlank() },
                securityClassification = classification,
                notes = notes
            )
            documentDao.insertDocument(docEntity)

            // 3. Commit Version 1 Record
            val versionEntity = DocumentVersionEntity(
                id = UUID.randomUUID().toString(),
                documentId = docId,
                versionNumber = 1,
                fileStoragePath = meta.relativePath,
                fileSizeBytes = meta.sizeBytes,
                mimeType = meta.mimeType,
                sha256Hash = meta.sha256Hex,
                encryptionIv = meta.encryptionIvHex,
                notes = notes
            )
            documentDao.insertVersion(versionEntity)

            auditLogger.recordEvent(
                eventType = AuditEventType.CREATE,
                entityType = "Document",
                entityId = docId,
                description = "Ingested document '$title' v1 (SHA-256: ${meta.sha256Hex.take(8)}...)"
            )
            return docId
        } catch (e: Exception) {
            // Rollback: cleanup any partial ciphertext file on storage failure
            storedPath?.let { fileStorage.deleteFile(it) }
            throw DocumentIngestionException("Failed to securely ingest document: ${e.message}", e)
        }
    }
}

class AddDocumentVersionUseCase @Inject constructor(
    private val documentDao: DocumentDao,
    private val fileStorage: FileStorageService,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        documentId: String,
        fileStream: InputStream,
        mimeType: String,
        notes: String? = null
    ): DocumentVersionEntity {
        DocumentRules.validateIngestionMetadata(title = "VersionUpdate", mimeType = mimeType)
        val currentMax = documentDao.getLatestVersionNumber(documentId) ?: 0
        val nextVersion = currentMax + 1

        var storedPath: String? = null
        try {
            val meta = fileStorage.storeEncryptedFile(
                documentId = documentId,
                versionNumber = nextVersion,
                mimeType = mimeType,
                inputStream = fileStream
            )
            storedPath = meta.relativePath

            val versionEntity = DocumentVersionEntity(
                id = UUID.randomUUID().toString(),
                documentId = documentId,
                versionNumber = nextVersion,
                fileStoragePath = meta.relativePath,
                fileSizeBytes = meta.sizeBytes,
                mimeType = meta.mimeType,
                sha256Hash = meta.sha256Hex,
                encryptionIv = meta.encryptionIvHex,
                notes = notes
            )
            documentDao.insertVersion(versionEntity)

            auditLogger.recordEvent(
                eventType = AuditEventType.UPDATE,
                entityType = "DocumentVersion",
                entityId = versionEntity.id,
                description = "Added Version $nextVersion to document $documentId"
            )
            return versionEntity
        } catch (e: Exception) {
            storedPath?.let { fileStorage.deleteFile(it) }
            throw DocumentIngestionException("Failed to add document version: ${e.message}", e)
        }
    }
}

class RestoreVersionUseCase @Inject constructor(
    private val documentDao: DocumentDao,
    private val fileStorage: FileStorageService,
    private val addDocumentVersionUseCase: AddDocumentVersionUseCase,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(documentId: String, targetVersion: DocumentVersionItem): DocumentVersionEntity {
        // Read & authenticate target version content into temporary buffer
        val buffer = ByteArrayOutputStream()
        fileStorage.readDecryptedFile(
            relativePath = targetVersion.storagePath,
            encryptionIvHex = targetVersion.encryptionIvHex,
            expectedSha256Hex = targetVersion.sha256Hex,
            outputStream = buffer
        )

        // Restore as a brand new appended version (preserving immutable audit history)
        val newVersion = addDocumentVersionUseCase(
            documentId = documentId,
            fileStream = ByteArrayInputStream(buffer.toByteArray()),
            mimeType = targetVersion.mimeType,
            notes = "Restored content from Version ${targetVersion.versionNumber}"
        )

        auditLogger.recordEvent(
            eventType = AuditEventType.UPDATE,
            entityType = "DocumentVersion",
            entityId = newVersion.id,
            description = "Restored Version ${targetVersion.versionNumber} as new Version ${newVersion.versionNumber}"
        )
        return newVersion
    }
}

class VerifyDocumentIntegrityUseCase @Inject constructor(
    private val fileStorage: FileStorageService,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(version: DocumentVersionItem): IntegrityCheckStatus {
        return try {
            val isValid = fileStorage.verifyIntegrity(
                relativePath = version.storagePath,
                encryptionIvHex = version.encryptionIvHex,
                expectedSha256Hex = version.sha256Hex
            )
            if (isValid) {
                auditLogger.recordEvent(
                    eventType = AuditEventType.READ,
                    entityType = "DocumentVersion",
                    entityId = version.id,
                    description = "Integrity check PASSED for doc ${version.documentId} v${version.versionNumber}"
                )
                IntegrityCheckStatus.INTEGRITY_OK
            } else {
                auditLogger.recordEvent(
                    eventType = AuditEventType.INTEGRITY_CHECK_FAILED,
                    entityType = "DocumentVersion",
                    entityId = version.id,
                    description = "Integrity check FAILED (Digest Mismatch) for doc ${version.documentId} v${version.versionNumber}"
                )
                IntegrityCheckStatus.INTEGRITY_FAILED
            }
        } catch (e: java.io.FileNotFoundException) {
            IntegrityCheckStatus.UNAVAILABLE
        } catch (e: Exception) {
            IntegrityCheckStatus.INTEGRITY_FAILED
        }
    }
}

class DeleteDocumentUseCase @Inject constructor(
    private val documentDao: DocumentDao,
    private val fileStorage: FileStorageService,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(documentId: String) {
        val versions = documentDao.getAllVersions(documentId)
        versions.forEach { v -> fileStorage.deleteFile(v.fileStoragePath) }
        documentDao.deleteDocumentById(documentId)

        auditLogger.recordEvent(
            eventType = AuditEventType.DELETE,
            entityType = "Document",
            entityId = documentId,
            description = "Permanently deleted document $documentId and ${versions.size} version payloads"
        )
    }
}
