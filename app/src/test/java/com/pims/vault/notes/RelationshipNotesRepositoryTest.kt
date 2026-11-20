package com.pims.vault.notes

import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.StandardCryptoEngine
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.core.storage.StoredFileMetadata
import com.pims.vault.data.local.dao.RelationshipNoteDao
import com.pims.vault.data.local.entity.RelationshipNoteAttachmentEntity
import com.pims.vault.data.local.entity.RelationshipNoteEntity
import com.pims.vault.data.local.relation.RelationshipNoteWithAttachments
import com.pims.vault.data.repository.RelationshipNotesRepositoryImpl
import com.pims.vault.domain.model.NoteFormat
import com.pims.vault.domain.model.RelationshipNoteAttachmentDraft
import com.pims.vault.domain.rules.VaultRules
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

class RelationshipNotesRepositoryTest {
    private val dao = MemoryDao()
    private val session = mock<BiometricSessionManager>()
    private val files = mock<FileStorageService>()
    private val crypto = StandardCryptoEngine()
    private val currentKey = ByteArray(32) { 7 }
    private val legacyKey = ByteArray(32) { 9 }
    private val repo = RelationshipNotesRepositoryImpl(dao, crypto, session, mock<HardenedAuditLogger>(), files)

    private fun unlock() {
        whenever(session.isZone4Unlocked()).thenReturn(true)
        whenever(session.getZone4VaultKey()).thenAnswer { currentKey.copyOf() }
        whenever(session.getDatabaseKey()).thenAnswer { legacyKey.copyOf() }
    }

    private fun note(id: String = "note", isPrivate: Boolean = false) = RelationshipNoteEntity(
        id = id, relationshipId = "person-relationship", topic = "Topic", contentPlaintext = "Original",
        encryptedPayload = null, encryptionIv = null, format = NoteFormat.PLAIN, isPrivate = isPrivate,
        createdAt = 1L, updatedAt = 2L
    )

    private fun attachment(id: String = "attachment") = RelationshipNoteAttachmentEntity(
        id = id, noteId = "note", storagePath = "old.enc", mimeType = "image/png", fileSizeBytes = 3L,
        sha256Hash = "hash"
    )

    @Test fun editPreservesAttachmentIdsAndCreatedTime(): Unit = runBlocking {
        dao.notes["note"] = note()
        val originalAttachment = attachment()
        dao.attachments["attachment"] = originalAttachment
        repo.saveNote("person-relationship", "Topic", "Changed", NoteFormat.PLAIN, false, "note")
        assertEquals("Changed", dao.notes["note"]?.contentPlaintext)
        assertEquals(1L, dao.notes["note"]?.createdAt)
        assertEquals(originalAttachment, dao.attachments["attachment"])
        assertEquals("old.enc", dao.attachments["attachment"]?.storagePath)
        verify(files, never()).deleteFile(any())
    }

    @Test fun wrongRelationshipCannotOverwriteNote() = runBlocking {
        dao.notes["note"] = note()
        val draft = RelationshipNoteAttachmentDraft(byteArrayOf(1, 2, 3), "image/png")
        try {
            repo.saveNote("another-relationship", null, "Changed", NoteFormat.PLAIN, false, "note", listOf(draft))
            fail("Reparenting must fail")
        } catch (_: IllegalArgumentException) { }
        assertEquals("Original", dao.notes["note"]?.contentPlaintext)
        assertTrue(draft.bytes.all { it == 0.toByte() })
        verifyNoInteractions(files)
    }

    @Test fun failedSecondFileLeavesOriginalNoteAndAttachmentsIntact() = runBlocking {
        dao.notes["note"] = note()
        dao.attachments["attachment"] = attachment()
        whenever(files.storeEncryptedFile(any(), any(), any(), any()))
            .thenReturn(StoredFileMetadata("staged.enc", 3, "image/png", "hash", "iv", 1))
            .thenThrow(IllegalStateException("Storage unavailable"))
        val drafts = List(2) { RelationshipNoteAttachmentDraft(byteArrayOf(1, 2, 3), "image/png") }
        try {
            repo.saveNote("person-relationship", null, "Changed", NoteFormat.PLAIN, false, "note", drafts, setOf("attachment"))
            fail("Storage failure must abort")
        } catch (_: IllegalStateException) { }
        assertEquals("Original", dao.notes["note"]?.contentPlaintext)
        assertNotNull(dao.attachments["attachment"])
        verify(files).deleteFile("staged.enc")
        verify(files, never()).deleteFile("old.enc")
        assertTrue(drafts.all { draft -> draft.bytes.all { it == 0.toByte() } })
    }

    @Test fun concurrentEditRejectsStagedSaveAndCleansNewFile(): Unit = runBlocking {
        dao.notes["note"] = note()
        whenever(files.storeEncryptedFile(any(), any(), any(), any())).thenAnswer {
            dao.notes["note"] = note().copy(contentPlaintext = "Concurrent edit", updatedAt = 3L)
            StoredFileMetadata("staged.enc", 3, "image/png", "hash", "iv", 1)
        }
        try {
            repo.saveNote("person-relationship", null, "Changed", NoteFormat.PLAIN, false, "note",
                listOf(RelationshipNoteAttachmentDraft(byteArrayOf(1, 2, 3), "image/png")))
            fail("Stale write must abort")
        } catch (_: IllegalStateException) { }
        assertEquals("Concurrent edit", dao.notes["note"]?.contentPlaintext)
        assertTrue(dao.attachments.isEmpty())
        verify(files).deleteFile("staged.enc")
    }

    @Test fun privateWriteRequiresElevatedSession() = runBlocking {
        try {
            repo.saveNote("person-relationship", null, "Sensitive", NoteFormat.PLAIN, true)
            fail("Private write without elevation must fail")
        } catch (_: SecurityException) { }
        assertTrue(dao.notes.isEmpty())
        verify(session, never()).getDatabaseKey()
        verifyNoInteractions(files)
    }

    @Test fun legacyDatabaseKeyReadRemainsGatedAndNewWritesUseZone4Key() = runBlocking {
        val itemKey = VaultRules.deriveItemKey(legacyKey, VaultCategory.SECURE_NOTE, "note")
        val encrypted = crypto.encrypt("Legacy content".toByteArray(), itemKey,
            "PIMS|rel-note|v1\nid=note\nrelId=person-relationship".toByteArray())
        itemKey.fill(0)
        dao.notes["note"] = note(isPrivate = true).copy(contentPlaintext = null,
            encryptedPayload = encrypted.combinedCiphertextWithTag,
            encryptionIv = encrypted.iv.joinToString("") { "%02x".format(it) })
        assertFalse(repo.getNotesForRelationshipFlow("person-relationship", true).first().single().isDecrypted)
        unlock()
        assertEquals("Legacy content", repo.getNotesForRelationshipFlow("person-relationship", true).first().single().content)
        repo.saveNote("person-relationship", "Topic", "Edited", NoteFormat.PLAIN, true, "note")
        val current = dao.notes.getValue("note")
        val newKey = VaultRules.deriveItemKey(currentKey, VaultCategory.SECURE_NOTE, "note")
        val decoded = crypto.decrypt(com.pims.vault.core.crypto.EncryptedPayload(current.encryptedPayload!!,
            current.encryptionIv!!.chunked(2).map { it.toInt(16).toByte() }.toByteArray()), newKey,
            "PIMS|rel-note|v1\nid=note\nrelId=person-relationship".toByteArray())
        assertEquals("Edited", String(decoded))
        decoded.fill(0)
        newKey.fill(0)
    }

    @Test fun corruptedPrivateSourceCannotBeOverwritten() = runBlocking {
        unlock()
        dao.notes["note"] = note(isPrivate = true).copy(contentPlaintext = null,
            encryptedPayload = ByteArray(32), encryptionIv = "00".repeat(12))
        try {
            repo.saveNote("person-relationship", null, "Replacement", NoteFormat.PLAIN, false, "note")
            fail("Corrupted source must be retained")
        } catch (_: SecurityException) { }
        assertTrue(dao.notes.getValue("note").isPrivate)
        assertArrayEquals(ByteArray(32), dao.notes.getValue("note").encryptedPayload)
    }

    @Test fun attachmentCannotBeReadFromAnotherNoteOrWithUnboundedSize() = runBlocking {
        dao.notes["note"] = note()
        dao.attachments["attachment"] = attachment().copy(noteId = "other")
        assertNull(repo.readAttachment("note", "attachment"))
        dao.attachments["attachment"] = attachment().copy(fileSizeBytes = Long.MAX_VALUE)
        try {
            repo.readAttachment("note", "attachment")
            fail("Invalid attachment metadata must be rejected")
        } catch (_: IllegalArgumentException) { }
        verifyNoInteractions(files)
    }

    private class MemoryDao : RelationshipNoteDao {
        val notes = mutableMapOf<String, RelationshipNoteEntity>()
        val attachments = mutableMapOf<String, RelationshipNoteAttachmentEntity>()
        override fun getNotesForRelationshipFlow(relationshipId: String) = flowOf(notes.values.filter { it.relationshipId == relationshipId })
        override suspend fun getNotesForRelationship(relationshipId: String) = notes.values.filter { it.relationshipId == relationshipId }
        override fun getNotesWithAttachmentsForRelationshipFlow(relationshipId: String) = flowOf(notes.values
            .filter { it.relationshipId == relationshipId }.map { note -> RelationshipNoteWithAttachments(note, attachments.values.filter { it.noteId == note.id }) })
        override fun getPublicNotesForOwnerFlow(ownerId: String) = flowOf(notes.values.filterNot { it.isPrivate })
        override fun getPrivateNoteCountFlow(relationshipId: String) = flowOf(notes.values.count { it.relationshipId == relationshipId && it.isPrivate })
        override suspend fun getNoteById(id: String) = notes[id]
        override suspend fun getAllNotes() = notes.values.toList()
        override suspend fun getAttachments(noteId: String) = attachments.values.filter { it.noteId == noteId }
        override suspend fun getAttachmentById(id: String) = attachments[id]
        override suspend fun upsertAttachment(attachment: RelationshipNoteAttachmentEntity) { attachments[attachment.id] = attachment }
        override suspend fun deleteAttachmentById(id: String) { attachments.remove(id) }
        override suspend fun insertOrUpdate(note: RelationshipNoteEntity): Long { notes[note.id] = note; return 1L }
        override suspend fun update(note: RelationshipNoteEntity) { notes[note.id] = note }
        override suspend fun deleteById(id: String) { notes.remove(id) }
        override suspend fun deleteForRelationship(relationshipId: String) { notes.values.removeAll { it.relationshipId == relationshipId } }
    }
}
