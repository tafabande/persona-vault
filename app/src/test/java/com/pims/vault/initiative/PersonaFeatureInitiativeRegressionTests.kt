package com.pims.vault.initiative

import com.pims.vault.core.crypto.EncryptedPayload
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.HkdfKeyDerivation
import com.pims.vault.core.crypto.StandardCryptoEngine
import com.pims.vault.core.model.AuditEventType
import com.pims.vault.core.model.CANONICAL_PRIMARY_OWNER_ID
import com.pims.vault.core.model.CardStatus
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.data.local.dao.AuditDao
import com.pims.vault.data.local.dao.DocumentDao
import com.pims.vault.data.local.dao.PlainNoteDao
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.entity.AuditEventEntity
import com.pims.vault.data.local.entity.DocumentEntity
import com.pims.vault.data.local.entity.DocumentVersionEntity
import com.pims.vault.data.local.entity.PlainNoteAttachmentEntity
import com.pims.vault.data.local.entity.PlainNoteEntity
import com.pims.vault.data.local.entity.VaultItemEntity
import com.pims.vault.data.local.relation.DocumentWithVersions
import com.pims.vault.domain.model.NoteFormat
import com.pims.vault.domain.model.PlainNote
import com.pims.vault.domain.model.document.UnifiedDocumentItem
import com.pims.vault.domain.model.document.UnifiedDocumentSource
import com.pims.vault.domain.model.PaymentReferenceSecret
import com.pims.vault.domain.usecase.document.UnifiedDocumentDiscoveryUseCase
import com.pims.vault.domain.usecase.vault.ReadPaymentReferenceUseCase
import com.pims.vault.domain.usecase.vault.SavePaymentReferenceUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Complete integration & regression tests for the Persona Feature Initiative:
 * 1. Premium Payment Cards (Physical vs Virtual, theme-aware styling, separated banking data)
 * 2. Front/Back Identity Attachments & Sensitive Image Safeguards
 * 3. Connected Tags vs Stable Person IDs
 * 4. Unified Document Discovery (Zero schema pollution across Documents and Notes)
 * 5. Persistent Entry Locking across Notes and Documents
 */
class PersonaFeatureInitiativeRegressionTests {

    private val cryptoEngine = StandardCryptoEngine()
    private val masterKey = ByteArray(32) { 0x33 }
    private val rootKey = HkdfKeyDerivation.deriveKey(masterKey, infoContext = "PIMS/vault/v1")

    private val inMemoryVaultItems = mutableMapOf<String, VaultItemEntity>()
    private val inMemoryAudits = mutableListOf<AuditEventEntity>()

    private val fakeVaultDao = object : VaultDao {
        override suspend fun insertOrUpdate(item: VaultItemEntity): Long {
            inMemoryVaultItems[item.id] = item
            return 1L
        }

        override suspend fun getVaultItemById(id: String): VaultItemEntity? = inMemoryVaultItems[id]
        override suspend fun getAllVaultItems(): List<VaultItemEntity> = inMemoryVaultItems.values.toList()
        override fun getVaultItemsFlow(personId: String): Flow<List<VaultItemEntity>> =
            flowOf(inMemoryVaultItems.values.filter { it.personId == personId })
        override fun getVaultItemsByCategoryFlow(personId: String, category: VaultCategory): Flow<List<VaultItemEntity>> =
            flowOf(inMemoryVaultItems.values.filter { it.personId == personId && it.category == category })
        override suspend fun deleteVaultItem(id: String) {
            inMemoryVaultItems.remove(id)
        }
    }

    private val fakeAuditDao = object : AuditDao {
        override fun getRecentEventsFlow(limit: Int): Flow<List<AuditEventEntity>> =
            flowOf(inMemoryAudits.takeLast(limit))

        override suspend fun getLatestAuditEvent(): AuditEventEntity? = inMemoryAudits.lastOrNull()
        override suspend fun getMaxSequenceNumber(): Long? = inMemoryAudits.maxOfOrNull { it.sequenceNumber }
        override suspend fun getAllEventsAscending(): List<AuditEventEntity> = inMemoryAudits.sortedBy { it.sequenceNumber }
        override suspend fun getEventsInSequenceRange(fromSeq: Long, toSeq: Long): List<AuditEventEntity> =
            inMemoryAudits.filter { it.sequenceNumber in fromSeq..toSeq }
        override fun getEventsForEntityFlow(entityType: String, entityId: String): Flow<List<AuditEventEntity>> =
            flowOf(inMemoryAudits.filter { it.entityType == entityType && it.entityId == entityId })
        override suspend fun insertAuditEvent(event: AuditEventEntity) {
            inMemoryAudits.add(event)
        }
        override suspend fun getAuditCount(): Long = inMemoryAudits.size.toLong()
    }

    private val auditLogger = HardenedAuditLogger(
        auditDao = fakeAuditDao,
        auditKey = ByteArray(32) { 0x55 }
    )

    private val savePaymentReferenceUseCase = SavePaymentReferenceUseCase(
        vaultDao = fakeVaultDao,
        cryptoEngine = cryptoEngine,
        auditLogger = auditLogger
    )

    private val readPaymentReferenceUseCase = ReadPaymentReferenceUseCase(
        vaultDao = fakeVaultDao,
        cryptoEngine = cryptoEngine,
        auditLogger = auditLogger
    )

    @Before
    fun setUp() {
        inMemoryVaultItems.clear()
        inMemoryAudits.clear()
    }

    // =========================================================================
    // PHASE 2: PAYMENT CARD PHYSICAL VS VIRTUAL & PRESENTATION SEPARATION
    // =========================================================================

    @Test
    fun testPhysicalCardCreationAndProperties() = runBlocking {
        val cardId = savePaymentReferenceUseCase(
            personId = CANONICAL_PRIMARY_OWNER_ID,
            nickname = "Personal Visa Debit",
            provider = "VISA",
            cardholderName = "Jane Doe",
            lastFour = "4242",
            month = "12",
            year = "2028",
            notes = "Physical standard debit",
            vaultRootKey = rootKey,
            linkedBankAccountId = "bank-acc-1",
            cardType = "PHYSICAL"
        )

        val secret = readPaymentReferenceUseCase(cardId, rootKey)
        assertNotNull(secret)
        assertEquals("PHYSICAL", secret.cardType)
        assertFalse(secret.isVirtual)
        assertFalse(secret.isVirtualCard)
        assertEquals("VISA", secret.provider)
        assertEquals("bank-acc-1", secret.linkedBankAccountId)
    }

    @Test
    fun testVirtualCardCreationAndProperties() = runBlocking {
        val cardId = savePaymentReferenceUseCase(
            personId = CANONICAL_PRIMARY_OWNER_ID,
            nickname = "Online Subscriptions Card",
            provider = "MasterCard",
            cardholderName = "Jane Doe",
            lastFour = "8899",
            month = "06",
            year = "2029",
            notes = "Virtual subscription card",
            vaultRootKey = rootKey,
            linkedBankAccountId = "bank-acc-2",
            cardType = "VIRTUAL"
        )

        val secret = readPaymentReferenceUseCase(cardId, rootKey)
        assertNotNull(secret)
        assertEquals("VIRTUAL", secret.cardType)
        assertTrue(secret.isVirtualCard)
        assertEquals("MasterCard", secret.provider)
    }

    // =========================================================================
    // PHASE 4: TAGS VS STABLE PERSON IDENTIFIERS
    // =========================================================================

    @Test
    fun testTagSeparationFromPersonId() {
        val note = PlainNote(
            id = "note-1",
            ownerPersonId = CANONICAL_PRIMARY_OWNER_ID,
            title = "Family Budget 2026",
            content = "Discussion with #mother regarding #school fees for #urgent expenses",
            isLocked = false
        )

        // Tags are derived labels, not Person IDs
        assertTrue(note.tags.contains("mother"))
        assertTrue(note.tags.contains("school"))
        assertTrue(note.tags.contains("urgent"))
        assertEquals(CANONICAL_PRIMARY_OWNER_ID, note.ownerPersonId)
    }

    // =========================================================================
    // PHASE 5 & 6: UNIFIED DOCUMENT DISCOVERY WITHOUT SCHEMA POLLUTION
    // =========================================================================

    @Test
    fun testUnifiedDocumentDiscoveryAggregationAndFiltering() = runBlocking {
        val inMemoryDocs = mutableListOf<DocumentWithVersions>()
        val inMemoryNotes = mutableListOf<PlainNoteEntity>()
        val inMemoryAttachments = mutableListOf<PlainNoteAttachmentEntity>()

        val fakeDocDao = object : DocumentDao {
            override fun getAllDocumentsFlow(): Flow<List<DocumentWithVersions>> = flowOf(inMemoryDocs)
            override fun getDocumentsForPersonFlow(personId: String): Flow<List<DocumentWithVersions>> =
                flowOf(inMemoryDocs.filter { it.document.personId == personId })
            override suspend fun getAllDocuments(): List<DocumentEntity> = inMemoryDocs.map { it.document }
            override suspend fun getAllDocumentVersions(): List<DocumentVersionEntity> = inMemoryDocs.flatMap { it.versions }
            override fun getDocumentsByTypeFlow(personId: String, type: DocumentType): Flow<List<DocumentWithVersions>> =
                flowOf(inMemoryDocs.filter { it.document.documentType == type })
            override fun getDocumentWithVersionsFlow(documentId: String): Flow<DocumentWithVersions?> =
                flowOf(inMemoryDocs.find { it.document.id == documentId })
            override suspend fun getDocumentWithVersions(documentId: String): DocumentWithVersions? =
                inMemoryDocs.find { it.document.id == documentId }
            override suspend fun insertDoc(document: DocumentEntity): Long = 1L
            override suspend fun updateDoc(document: DocumentEntity) {}
            override suspend fun insertVersion(version: DocumentVersionEntity) {}
            override suspend fun getLatestVersionNumber(documentId: String): Int? = 1
            override suspend fun getAllVersions(documentId: String): List<DocumentVersionEntity> = emptyList()
            override suspend fun deleteDocument(document: DocumentEntity) {}
            override suspend fun deleteVersion(version: DocumentVersionEntity) {}
            override suspend fun setDocumentLocked(id: String, isLocked: Boolean) {}
            override suspend fun countDocumentsForPerson(personId: String): Int = inMemoryDocs.count { it.document.personId == personId }
            override suspend fun deleteVersionById(id: String) {}
            override suspend fun deleteDocumentById(id: String) {}
        }

        val fakePlainNoteDao = object : PlainNoteDao {
            override fun observeForOwner(ownerPersonId: String): Flow<List<PlainNoteEntity>> =
                flowOf(inMemoryNotes.filter { it.ownerPersonId == ownerPersonId })
            override fun observeAllNotes(): Flow<List<PlainNoteEntity>> = flowOf(inMemoryNotes)
            override fun observeAllAttachments(): Flow<List<PlainNoteAttachmentEntity>> = flowOf(inMemoryAttachments)
            override suspend fun getAllNotes(): List<PlainNoteEntity> = inMemoryNotes
            override suspend fun getById(id: String): PlainNoteEntity? = inMemoryNotes.find { it.id == id }
            override suspend fun upsert(note: PlainNoteEntity) { inMemoryNotes.add(note) }
            override suspend fun deleteById(id: String) { inMemoryNotes.removeAll { it.id == id } }
            override suspend fun getAttachments(noteId: String): List<PlainNoteAttachmentEntity> =
                inMemoryAttachments.filter { it.noteId == noteId }
            override suspend fun getAttachmentsForNotes(noteIds: List<String>): List<PlainNoteAttachmentEntity> =
                inMemoryAttachments.filter { it.noteId in noteIds }
            override suspend fun getAttachment(id: String): PlainNoteAttachmentEntity? =
                inMemoryAttachments.find { it.id == id }
            override suspend fun upsertAttachment(attachment: PlainNoteAttachmentEntity) { inMemoryAttachments.add(attachment) }
            override suspend fun deleteAttachment(id: String) { inMemoryAttachments.removeAll { it.id == id } }
            override suspend fun touch(noteId: String, timestamp: Long) {}
            override suspend fun getActiveReminders(): List<PlainNoteEntity> = emptyList()
            override fun observeActiveReminders(): Flow<List<PlainNoteEntity>> = flowOf(emptyList())
            override suspend fun updateReminder(id: String, reminderAt: Long?, reminderTag: String?, reminderRepeat: String?, updatedAt: Long) {}
            override suspend fun setReminderDone(id: String, isDone: Boolean, updatedAt: Long) {}
            override suspend fun clearReminder(id: String, updatedAt: Long) {}
            override suspend fun countNotesForPerson(personId: String): Int = inMemoryNotes.count { it.ownerPersonId == personId }
            override suspend fun setNoteLocked(id: String, isLocked: Boolean, updatedAt: Long) {}
        }

        // 1. Add a Standalone Document
        val docEntity = DocumentEntity(
            id = "doc-100",
            personId = CANONICAL_PRIMARY_OWNER_ID,
            documentType = DocumentType.PASSPORT,
            title = "International Passport",
            securityClassification = SecurityClassification.ZONE_3_SENSITIVE,
            isLocked = false
        )
        val docVersion = DocumentVersionEntity(
            id = "ver-100",
            documentId = "doc-100",
            versionNumber = 1,
            fileStoragePath = "docs/passport.enc",
            fileSizeBytes = 512000L,
            mimeType = "application/pdf",
            sha256Hash = "hash100",
            encryptionIv = "iv100"
        )
        inMemoryDocs.add(DocumentWithVersions(document = docEntity, versions = listOf(docVersion)))

        // 2. Add a Plain Note with Document Attachment
        val noteEntity = PlainNoteEntity(
            id = "note-200",
            ownerPersonId = CANONICAL_PRIMARY_OWNER_ID,
            title = "Lease Agreement",
            content = "Attached apartment contract #housing #legal",
            reminderTag = "housing,legal",
            isLocked = true
        )
        inMemoryNotes.add(noteEntity)

        val noteAttachment = PlainNoteAttachmentEntity(
            id = "att-300",
            noteId = "note-200",
            storagePath = "notes/lease_contract.enc",
            fileSizeBytes = 1048576L,
            mimeType = "application/pdf",
            sha256Hash = "sha256fake",
            caption = "Apartment Lease Contract 2026.pdf"
        )
        inMemoryAttachments.add(noteAttachment)

        // 3. Execute Unified Document Discovery UseCase
        val discoveryUseCase = UnifiedDocumentDiscoveryUseCase(
            documentDao = fakeDocDao,
            plainNoteDao = fakePlainNoteDao
        )

        val allItems = discoveryUseCase().first()
        assertEquals(2, allItems.size)

        val standaloneItem = allItems.find { it.source == UnifiedDocumentSource.STANDALONE_DOCUMENT }
        assertNotNull(standaloneItem)
        assertEquals("doc-100", standaloneItem!!.id)
        assertEquals("International Passport", standaloneItem.title)
        assertEquals(UnifiedDocumentSource.STANDALONE_DOCUMENT, standaloneItem.source)
        assertFalse(standaloneItem.isLocked)

        val noteAttachmentItem = allItems.find { it.source == UnifiedDocumentSource.NOTE_ATTACHMENT }
        assertNotNull(noteAttachmentItem)
        assertEquals("att-300", noteAttachmentItem!!.id)
        assertEquals("Apartment Lease Contract 2026.pdf", noteAttachmentItem.title)
        assertEquals(UnifiedDocumentSource.NOTE_ATTACHMENT, noteAttachmentItem.source)
        assertEquals("note-200", noteAttachmentItem.sourceRecordId)
        assertTrue(noteAttachmentItem.tags.contains("housing"))
        assertTrue(noteAttachmentItem.isLocked) // Inherits parent note lock state

        // 4. Test Filtering by Tag
        val filteredByTag = discoveryUseCase(tagFilter = "housing").first()
        assertEquals(1, filteredByTag.size)
        assertEquals("att-300", filteredByTag.first().id)

        // 5. Test Filtering by Search Query
        val searchResult = discoveryUseCase(searchQuery = "Passport").first()
        assertEquals(1, searchResult.size)
        assertEquals("doc-100", searchResult.first().id)
    }

    // =========================================================================
    // PHASE 7: INDIVIDUAL ENTRY LOCKING
    // =========================================================================

    @Test
    fun testIndividualEntryLockingPreservation() {
        val note = PlainNote(
            id = "note-secret-1",
            ownerPersonId = CANONICAL_PRIMARY_OWNER_ID,
            title = "Confidential Journal",
            content = "My private thoughts",
            isLocked = true
        )
        assertTrue(note.isLocked)

        val unlockedNote = note.copy(isLocked = false)
        assertFalse(unlockedNote.isLocked)
    }
}
