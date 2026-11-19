package com.pims.vault.document

import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.core.storage.StoredFileMetadata
import com.pims.vault.data.local.dao.DocumentDao
import com.pims.vault.data.local.entity.DocumentEntity
import com.pims.vault.data.local.entity.DocumentVersionEntity
import com.pims.vault.data.local.relation.DocumentWithVersions
import com.pims.vault.domain.model.DocumentVersionItem
import com.pims.vault.domain.rules.DocumentIngestionException
import com.pims.vault.domain.rules.DocumentRules
import com.pims.vault.domain.usecase.document.*
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

class DocumentDurabilityRegressionTest {
    private val dao = InMemoryDocumentDao()
    private val storage = FixtureDocumentStorage()
    private val audit: HardenedAuditLogger = mock()
    private val ingest = IngestDocumentUseCase(dao, storage, audit)
    private val add = AddDocumentVersionUseCase(dao, storage, audit)

    private suspend fun import(bytes: ByteArray = "initial fixture".toByteArray(), id: String? = null): String =
        ingest("owner", DocumentType.PASSPORT, "Fixture passport", null, null, null,
            null, null, null, bytes.inputStream(), "application/pdf", "original scan.pdf", importId = id)

    @Test fun importPublishesTheOwnedCopyAndPreservesItsOriginalFilename(): Unit = runBlocking {
        val id = import()
        val version = dao.versions.values.single()
        assertEquals(id, version.documentId)
        assertEquals(1, version.versionNumber)
        assertEquals("original scan.pdf", version.originalFilename)
        assertEquals("vault_documents/${version.id}/v_1.penc", version.fileStoragePath)
        assertNotEquals(id, version.id)
        assertEquals("original scan.pdf", GetDocumentsUseCase(dao)("owner").first().single().currentVersion?.originalFilename)
    }

    @Test fun failedFirstVersionRollsBackParentAndRemovesOnlyTheUncommittedFile(): Unit = runBlocking {
        dao.failVersionInsert = true
        try { import(); fail("Expected import failure") } catch (_: DocumentIngestionException) { }
        assertTrue(dao.documents.isEmpty())
        assertTrue(dao.versions.isEmpty())
        assertTrue(storage.files.isEmpty())
        assertEquals(1, storage.deleted.size)
    }

    @Test fun auditFailureCannotRemoveACommittedDocument(): Unit = runBlocking {
        whenever(audit.recordEvent(any(), any(), any(), any(), any())).thenThrow(IllegalStateException("audit unavailable"))
        val id = import()
        assertTrue(dao.documents.containsKey(id))
        assertTrue(storage.files.containsKey(dao.versions.values.single().fileStoragePath))
        assertTrue(storage.deleted.isEmpty())
    }

    @Test fun concurrentAppendsKeepDistinctPayloadsAndAllocateConsecutiveVersions(): Unit = runBlocking {
        val id = import()
        val results = coroutineScope {
            val first = async { add(id, "second".byteInputStream(), "application/pdf") }
            val second = async { add(id, "third".byteInputStream(), "application/pdf") }
            listOf(first.await(), second.await())
        }
        assertEquals(setOf(2, 3), results.map { it.versionNumber }.toSet())
        assertEquals(3, dao.versions.values.map { it.fileStoragePath }.distinct().size)
        assertEquals(setOf("second", "third"), results.map { String(storage.files.getValue(it.fileStoragePath)) }.toSet())
        assertEquals("initial fixture", String(storage.files.getValue(dao.versions.values.first { it.versionNumber == 1 }.fileStoragePath)))
    }

    @Test fun cancellationAfterCommitPreservesDataAndRetryKeepsTheSameImportId(): Unit = runBlocking {
        dao.cancelAfterCreateCommit = true
        try { import(id = "stable-import"); fail("Expected cancellation") } catch (_: CancellationException) { }
        assertTrue(dao.documents.containsKey("stable-import"))
        assertEquals(1, storage.files.size)
        assertTrue(storage.deleted.isEmpty())
        assertEquals("stable-import", import(id = "stable-import"))
        assertEquals(1, storage.storeCount)
    }

    @Test fun uncertainDatabaseStatePreservesCiphertextForRecovery(): Unit = runBlocking {
        dao.failVersionInsert = true
        dao.failVersionRead = true
        try { import(); fail("Expected import failure") } catch (_: DocumentIngestionException) { }
        assertEquals(1, storage.files.size)
        assertTrue(storage.deleted.isEmpty())
    }

    @Test fun restoreUsesStoredMetadataAndAppendsWithoutReplacingHistory(): Unit = runBlocking {
        val id = import()
        val first = dao.versions.values.single()
        val display = first.domain().copy(storagePath = "untrusted/path", sha256Hex = "untrusted", sizeBytes = 1)
        val restored = RestoreVersionUseCase(dao, storage, add, audit)(id, display)
        assertEquals(2, restored.versionNumber)
        assertEquals("original scan.pdf", restored.originalFilename)
        assertEquals(first, dao.versions[first.id])
        assertArrayEquals(storage.files.getValue(first.fileStoragePath), storage.files.getValue(restored.fileStoragePath))
        assertEquals(listOf(first.fileStoragePath), storage.readPaths)
    }

    @Test fun foreignOrOversizedRestoreFailsBeforeReadingAnyFile(): Unit = runBlocking {
        val id = import()
        val first = dao.versions.values.single()
        val restore = RestoreVersionUseCase(dao, storage, add, audit)
        try { restore("other-document", first.domain()); fail("Expected owner rejection") } catch (_: IllegalArgumentException) { }
        dao.versions[first.id] = first.copy(fileSizeBytes = DocumentRules.MAX_DOCUMENT_SIZE_BYTES + 1)
        try { restore(id, first.domain()); fail("Expected size rejection") } catch (_: IllegalArgumentException) { }
        assertTrue(storage.readPaths.isEmpty())
        assertEquals(1, storage.files.size)
    }

    @Test fun failedMetadataDeletionPreservesAllFiles(): Unit = runBlocking {
        val id = import()
        dao.failDocumentDelete = true
        try { DeleteDocumentUseCase(dao, storage, audit)(id); fail("Expected deletion failure") }
        catch (_: IllegalStateException) { }
        assertTrue(dao.documents.containsKey(id))
        assertEquals(1, storage.files.size)
        assertTrue(storage.deleted.isEmpty())
    }

    @Test fun repositoryUsesTheSameSafeWriterWithoutDroppingSuppliedMetadata(): Unit = runBlocking {
        val repository = com.pims.vault.data.repository.DocumentRepositoryImpl(dao, storage, audit)
        val metadata = DocumentEntity("profile-photo", "owner", DocumentType.PASSPORT_PHOTO, "Fixture photo",
            notes = "Parent note", createdAt = 10, updatedAt = 20, isLocked = true)
        val id = repository.createDocument(metadata, "photo fixture".byteInputStream(), "image/jpeg", "Version note")
        assertEquals(metadata, dao.documents[id])
        assertEquals("Version note", dao.versions.values.single().notes)
        val next = repository.addDocumentVersion(id, "updated photo".byteInputStream(), "image/jpeg", null)
        assertEquals(2, next.versionNumber)
        assertEquals(2, storage.files.size)
        assertEquals(2, dao.versions.size)
    }

    @Test fun repositoryFailureAlsoRollsBackTheParentAndUncommittedFile(): Unit = runBlocking {
        val repository = com.pims.vault.data.repository.DocumentRepositoryImpl(dao, storage, audit)
        dao.failVersionInsert = true
        val metadata = DocumentEntity("profile-photo", "owner", DocumentType.PASSPORT_PHOTO, "Fixture photo")
        try {
            repository.createDocument(metadata, "photo fixture".byteInputStream(), "image/jpeg", null)
            fail("Expected repository import failure")
        } catch (_: DocumentIngestionException) { }
        assertTrue(dao.documents.isEmpty())
        assertTrue(storage.files.isEmpty())
    }

    private fun DocumentVersionEntity.domain() = DocumentVersionItem(id, documentId, versionNumber,
        fileStoragePath, mimeType, originalFilename, fileSizeBytes, sha256Hash, encryptionIv)
}

internal class InMemoryDocumentDao : DocumentDao {
    val documents = linkedMapOf<String, DocumentEntity>()
    val versions = linkedMapOf<String, DocumentVersionEntity>()
    var failVersionInsert = false
    var failVersionRead = false
    var failDocumentDelete = false
    var cancelAfterCreateCommit = false
    private val mutex = Mutex()

    private fun history(id: String): DocumentWithVersions? = documents[id]?.let { document ->
        DocumentWithVersions(document, versions.values.filter { it.documentId == id })
    }

    override fun getDocumentsForPersonFlow(personId: String): Flow<List<DocumentWithVersions>> =
        flowOf(documents.values.filter { it.personId == personId }.mapNotNull { history(it.id) })
    override fun getAllDocumentsFlow(): Flow<List<DocumentWithVersions>> = flowOf(documents.keys.mapNotNull(::history))
    override suspend fun getAllDocuments() = documents.values.toList()
    override suspend fun getAllDocumentVersions(): List<DocumentVersionEntity> {
        check(!failVersionRead) { "Database unavailable" }
        return versions.values.toList()
    }
    override fun getDocumentsByTypeFlow(personId: String, type: DocumentType): Flow<List<DocumentWithVersions>> =
        flowOf(documents.values.filter { it.personId == personId && it.documentType == type }.mapNotNull { history(it.id) })
    override fun getDocumentWithVersionsFlow(documentId: String) = flowOf(history(documentId))
    override suspend fun getDocumentWithVersions(documentId: String) = history(documentId)
    override suspend fun insertDoc(document: DocumentEntity): Long {
        if (documents.containsKey(document.id)) return -1
        documents[document.id] = document
        return documents.size.toLong()
    }
    override suspend fun updateDoc(document: DocumentEntity) { documents[document.id] = document }
    override suspend fun insertVersion(version: DocumentVersionEntity) {
        check(!failVersionInsert) { "Version insert failed" }
        check(documents.containsKey(version.documentId))
        check(versions[version.id] == null && versions.values.none { it.documentId == version.documentId && it.versionNumber == version.versionNumber })
        versions[version.id] = version
    }
    override suspend fun updateVersionStorage(id: String, path: String, size: Long, iv: String): Int {
        val current = versions[id] ?: return 0
        versions[id] = current.copy(fileStoragePath = path, fileSizeBytes = size, encryptionIv = iv)
        return 1
    }
    override suspend fun fillMissingVersionMetadata(id: String, documentId: String, hash: String, filename: String?, notes: String?, createdAt: Long): Int {
        val current = versions[id] ?: return 0
        if (current.documentId != documentId || !current.sha256Hash.equals(hash, true)) return 0
        versions[id] = current.copy(originalFilename = current.originalFilename ?: filename, notes = current.notes ?: notes,
            createdAt = if (current.createdAt > 0) current.createdAt else createdAt)
        return 1
    }
    override suspend fun getLatestVersionNumber(documentId: String) = versions.values.filter { it.documentId == documentId }.maxOfOrNull { it.versionNumber }
    override suspend fun getAllVersions(documentId: String) = versions.values.filter { it.documentId == documentId }
    override suspend fun deleteDocument(document: DocumentEntity) = deleteDocumentById(document.id)
    override suspend fun deleteVersion(version: DocumentVersionEntity) { versions.remove(version.id) }
    override suspend fun setDocumentLocked(id: String, isLocked: Boolean) { documents[id]?.let { documents[id] = it.copy(isLocked = isLocked) } }
    override suspend fun countDocumentsForPerson(personId: String) = documents.values.count { it.personId == personId }
    override suspend fun deleteVersionById(id: String) { versions.remove(id) }
    override suspend fun deleteDocumentById(id: String) {
        check(!failDocumentDelete) { "Document delete failed" }
        documents.remove(id)
        versions.entries.removeAll { it.value.documentId == id }
    }

    override suspend fun createDocumentWithVersion(document: DocumentEntity, version: DocumentVersionEntity) = mutex.withLock {
        val previousDocuments = documents.toMap()
        val previousVersions = versions.toMap()
        try { super<DocumentDao>.createDocumentWithVersion(document, version) }
        catch (failure: Exception) {
            documents.clear(); documents.putAll(previousDocuments)
            versions.clear(); versions.putAll(previousVersions)
            throw failure
        }
        if (cancelAfterCreateCommit) {
            cancelAfterCreateCommit = false
            throw CancellationException("Cancelled after commit acknowledgement")
        }
    }

    override suspend fun appendVersion(documentId: String, version: DocumentVersionEntity): DocumentVersionEntity =
        mutex.withLock { super<DocumentDao>.appendVersion(documentId, version) }
}

private class FixtureDocumentStorage : FileStorageService {
    val files = linkedMapOf<String, ByteArray>()
    val deleted = mutableListOf<String>()
    val readPaths = mutableListOf<String>()
    var storeCount = 0
    override suspend fun storeEncryptedFile(documentId: String, versionNumber: Int, mimeType: String, inputStream: InputStream): StoredFileMetadata {
        val bytes = inputStream.readBytes()
        yield()
        val path = "vault_documents/$documentId/v_$versionNumber.penc"
        check(!files.containsKey(path)) { "Immutable payload was replaced" }
        files[path] = bytes
        storeCount++
        return StoredFileMetadata(path, bytes.size.toLong(), mimeType, hash(bytes), "fixture-iv", 1)
    }
    override suspend fun readDecryptedFile(relativePath: String, encryptionIvHex: String, expectedSha256Hex: String, outputStream: OutputStream) {
        readPaths.add(relativePath)
        val bytes = files.getValue(relativePath)
        check(hash(bytes) == expectedSha256Hex)
        outputStream.write(bytes)
    }
    override suspend fun verifyIntegrity(relativePath: String, encryptionIvHex: String, expectedSha256Hex: String) =
        files[relativePath]?.let { hash(it) == expectedSha256Hex } ?: false
    override suspend fun storePreEncryptedFile(documentId: String, versionNumber: Int, mimeType: String, inputStream: InputStream): StoredFileMetadata =
        error("Not used by these persistence tests")
    override suspend fun deleteFile(relativePath: String): Boolean {
        deleted.add(relativePath)
        return files.remove(relativePath) != null
    }
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
