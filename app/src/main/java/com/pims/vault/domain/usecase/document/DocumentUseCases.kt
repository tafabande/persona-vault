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
import com.pims.vault.core.logging.VaultLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject

class GetDocumentsUseCase @Inject constructor(
    private val documentDao: DocumentDao
) {
    operator fun invoke(personId: String? = null): Flow<List<DocumentWithHistory>> {
        val baseFlow = if (personId.isNullOrBlank()) {
            documentDao.getAllDocumentsFlow()
        } else {
            documentDao.getDocumentsForPersonFlow(personId)
        }
        return baseFlow.map { docWithVersionsList ->
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
                    isLocked = item.document.isLocked,
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
                        originalFilename = v.originalFilename,
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
        notes: String? = null,
        importId: String? = null
    ): String {
        val document = DocumentEntity(
            id = importId ?: UUID.randomUUID().toString(),
            personId = personId,
            documentType = documentType,
            title = title.trim(),
            documentNumber = documentNumber?.trim()?.takeIf(String::isNotBlank),
            issuingAuthority = issuingAuthority?.trim()?.takeIf(String::isNotBlank),
            issuingCountry = issuingCountry?.trim()?.takeIf(String::isNotBlank),
            issueDate = issueDate?.trim()?.takeIf(String::isNotBlank),
            expirationDate = expirationDate?.trim()?.takeIf(String::isNotBlank),
            securityClassification = customClassification ?: DocumentRules.resolveDefaultClassification(documentType),
            notes = notes
        )
        return invoke(document, initialFileStream, mimeType, originalFilename, notes)
    }

    /** Repository callers retain their complete supplied metadata and stable import identity. */
    suspend operator fun invoke(
        document: DocumentEntity,
        initialFileStream: InputStream,
        mimeType: String,
        originalFilename: String? = null,
        versionNotes: String? = null
    ): String {
        val normalizedMime = DocumentRules.normalizeMimeType(mimeType, originalFilename)
        DocumentRules.validateIngestionMetadata(document.title, normalizedMime, filename = originalFilename)
        require(document.personId.isNotBlank()) { "Choose a document owner" }
        require(document.id.isNotBlank()) { "Invalid document import ID" }
        val docId = document.id
        val prior = documentDao.getDocumentWithVersions(docId)
        if (prior != null) {
            initialFileStream.close()
            require(prior.document.personId == document.personId) { "Document ownership does not match" }
            val saved = prior.document
            require(saved.documentType == document.documentType && saved.title == document.title &&
                saved.documentNumber == document.documentNumber && saved.issuingAuthority == document.issuingAuthority &&
                saved.issuingCountry == document.issuingCountry && saved.issueDate == document.issueDate &&
                saved.expirationDate == document.expirationDate && saved.notes == document.notes &&
                saved.securityClassification == document.securityClassification && saved.isLocked == document.isLocked) {
                "This import was already saved with different details. Open the saved document to review it."
            }
            val first = prior.versions.minByOrNull { it.versionNumber }
                ?: throw DocumentIngestionException("This document needs a readable version before the import can finish")
            check(fileStorage.verifyIntegrity(first.fileStoragePath, first.encryptionIv, first.sha256Hash)) {
                "The earlier import could not be verified. Keep the original file and retry recovery."
            }
            return docId
        }
        val versionId = UUID.randomUUID().toString()
        var storedPath: String? = "vault_documents/$versionId/v_1.penc"
        try {
            // The storage identity belongs to the immutable version, avoiding concurrent path reuse.
            val meta = fileStorage.storeEncryptedFile(versionId, 1, normalizedMime,
                LimitedDocumentInput(initialFileStream))
            storedPath = meta.relativePath
            validateStoredSize(meta.sizeBytes)
            val version = DocumentVersionEntity(versionId, docId, 1, meta.relativePath,
                meta.sizeBytes, meta.mimeType, meta.sha256Hex, meta.encryptionIvHex,
                notes = versionNotes, originalFilename = originalFilename)
            documentDao.createDocumentWithVersion(document, version)
            recordCommittedAudit(auditLogger, AuditEventType.CREATE, "Document", docId, "Imported document version 1")
            return docId
        } catch (cancelled: CancellationException) {
            cleanupUncommitted(documentDao, fileStorage, storedPath)
            throw cancelled
        } catch (failure: Exception) {
            cleanupUncommitted(documentDao, fileStorage, storedPath)
            throw DocumentIngestionException("The document could not be saved. Keep the original file and retry.", failure)
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
        notes: String? = null,
        originalFilename: String? = null,
        versionId: String? = null
    ): DocumentVersionEntity {
        val normalizedMime = DocumentRules.normalizeMimeType(mimeType, originalFilename)
        DocumentRules.validateIngestionMetadata("Version update", normalizedMime, filename = originalFilename)
        checkNotNull(documentDao.getDocumentWithVersions(documentId)) { "Document no longer exists" }
        val newVersionId = versionId ?: UUID.randomUUID().toString()
        require(newVersionId.matches(Regex("[A-Za-z0-9_-]+"))) { "Invalid version identity" }
        val prior = documentDao.getVersionById(newVersionId)
        if (prior != null) {
            fileStream.close()
            require(prior.documentId == documentId) { "The version identity belongs to another document" }
            check(fileStorage.verifyIntegrity(prior.fileStoragePath, prior.encryptionIv, prior.sha256Hash)) {
                "The saved version needs recovery before retrying"
            }
            return prior
        }
        var storedPath: String? = "vault_documents/$newVersionId/v_1.penc"
        try {
            val meta = fileStorage.storeEncryptedFile(newVersionId, 1, normalizedMime, LimitedDocumentInput(fileStream))
            storedPath = meta.relativePath
            validateStoredSize(meta.sizeBytes)
            val candidate = DocumentVersionEntity(newVersionId, documentId, 0, meta.relativePath,
                meta.sizeBytes, meta.mimeType, meta.sha256Hex, meta.encryptionIvHex,
                notes = notes, originalFilename = originalFilename)
            val version = documentDao.appendVersion(documentId, candidate)
            recordCommittedAudit(auditLogger, AuditEventType.UPDATE, "DocumentVersion", version.id,
                "Added document version ${version.versionNumber}")
            return version
        } catch (cancelled: CancellationException) {
            cleanupUncommitted(documentDao, fileStorage, storedPath)
            throw cancelled
        } catch (failure: Exception) {
            cleanupUncommitted(documentDao, fileStorage, storedPath)
            throw DocumentIngestionException("The new version could not be saved. Your earlier versions are preserved.", failure)
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
        require(targetVersion.documentId == documentId) { "The selected version belongs to another document" }
        val stored = documentDao.getVersionById(targetVersion.id)
            ?: throw DocumentIngestionException("This version is no longer available")
        require(stored.documentId == documentId) { "The selected version belongs to another document" }
        validateStoredSize(stored.fileSizeBytes)
        val buffer = WipingDocumentBuffer(stored.fileSizeBytes.toInt())
        try {
            // Read only the stored path/hash, never caller-supplied preview metadata.
            fileStorage.readDecryptedFile(stored.fileStoragePath, stored.encryptionIv, stored.sha256Hash, buffer)
            check(buffer.size == stored.fileSizeBytes.toInt()) { "Document size verification failed" }
            val restored = buffer.inputStream().use { input ->
                addDocumentVersionUseCase(documentId, input, stored.mimeType,
                    "Restored content from version ${stored.versionNumber}", stored.originalFilename)
            }
            recordCommittedAudit(auditLogger, AuditEventType.UPDATE, "DocumentVersion", restored.id,
                "Restored version ${stored.versionNumber} as version ${restored.versionNumber}")
            return restored
        } finally { buffer.close() }
    }
}

class VerifyDocumentIntegrityUseCase @Inject constructor(
    private val fileStorage: FileStorageService,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(version: DocumentVersionItem): IntegrityCheckStatus {
        val status = try {
            if (fileStorage.verifyIntegrity(version.storagePath, version.encryptionIvHex, version.sha256Hex)) {
                IntegrityCheckStatus.INTEGRITY_OK
            } else IntegrityCheckStatus.INTEGRITY_FAILED
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: java.io.FileNotFoundException) {
            IntegrityCheckStatus.UNAVAILABLE
        } catch (_: Exception) {
            IntegrityCheckStatus.INTEGRITY_FAILED
        }
        recordCommittedAudit(auditLogger,
            if (status == IntegrityCheckStatus.INTEGRITY_OK) AuditEventType.READ else AuditEventType.INTEGRITY_CHECK_FAILED,
            "DocumentVersion", version.id, "Document integrity result ${status.name}")
        return status
    }
}

class DeleteDocumentUseCase @Inject constructor(
    private val documentDao: DocumentDao,
    private val fileStorage: FileStorageService,
    private val auditLogger: HardenedAuditLogger,
    private val deletionStore: com.pims.vault.core.sync.DocumentDeletionStore? = null
) {
    suspend operator fun invoke(documentId: String) {
        val versions = documentDao.detachDocument(documentId) { document, _ ->
            deletionStore?.recordLocal(document)
        }
        versions.forEach { version ->
            try { fileStorage.deleteFile(version.fileStoragePath) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { VaultLogger.w("DocumentPersistence", "Encrypted document cleanup is pending") }
        }
        recordCommittedAudit(auditLogger, AuditEventType.DELETE, "Document", documentId,
            "Deleted document and ${versions.size} version references")
    }
}

class DecryptDocumentUseCase @Inject constructor(
    private val fileStorage: FileStorageService,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        version: DocumentVersionItem,
        outputStream: OutputStream
    ) {
        auditLogger.recordEvent(
            eventType = AuditEventType.READ,
            entityType = "DocumentVersion",
            entityId = version.id,
            description = "Requested document version ${version.versionNumber}"
        )
        fileStorage.readDecryptedFile(
            relativePath = version.storagePath,
            encryptionIvHex = version.encryptionIvHex,
            expectedSha256Hex = version.sha256Hex,
            outputStream = outputStream
        )
    }
}


private fun validateStoredSize(size: Long) {
    require(size > 0) { "The selected file is empty" }
    DocumentRules.validateIngestionMetadata("Document", "application/octet-stream", sizeBytes = size)
}

private suspend fun cleanupUncommitted(dao: DocumentDao, storage: FileStorageService, path: String?) {
    if (path == null) return
    withContext(NonCancellable) {
        val referenced = try { dao.getAllDocumentVersions().any { it.fileStoragePath == path } }
            catch (_: Exception) { true }
        if (!referenced) {
            try { storage.deleteFile(path) }
            catch (_: Exception) { VaultLogger.w("DocumentPersistence", "Uncommitted encrypted file cleanup is pending") }
        }
    }
}

private suspend fun recordCommittedAudit(
    audit: HardenedAuditLogger, type: AuditEventType, entityType: String, id: String, description: String
) {
    try { audit.recordEvent(type, entityType, id, description) }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { VaultLogger.w("DocumentPersistence", "Document audit event could not be recorded") }
}

private class LimitedDocumentInput(private val source: InputStream) : InputStream() {
    private var count = 0L
    override fun read(): Int {
        val value = source.read()
        if (value >= 0) checkSize(1)
        return value
    }
    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
        val read = source.read(bytes, offset, minOf(length.toLong(), DocumentRules.MAX_DOCUMENT_SIZE_BYTES - count + 1).toInt())
        if (read > 0) checkSize(read)
        return read
    }
    private fun checkSize(read: Int) {
        count += read
        require(count <= DocumentRules.MAX_DOCUMENT_SIZE_BYTES) { "Choose a document smaller than 50 MB" }
    }
    override fun close() = source.close()
}

/** A single bounded plaintext allocation whose bytes are wiped after append succeeds or fails. */
private class WipingDocumentBuffer(capacity: Int) : OutputStream() {
    private val bytes = ByteArray(capacity)
    var size = 0
        private set
    override fun write(value: Int) {
        require(size < bytes.size) { "Document exceeds its stored size" }
        bytes[size++] = value.toByte()
    }
    override fun write(source: ByteArray, offset: Int, length: Int) {
        require(length <= bytes.size - size) { "Document exceeds its stored size" }
        source.copyInto(bytes, size, offset, offset + length)
        size += length
    }
    fun inputStream(): InputStream = ByteArrayInputStream(bytes, 0, size)
    override fun close() { bytes.fill(0) }
}
