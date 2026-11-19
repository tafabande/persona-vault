package com.pims.vault.data.repository

import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.EncryptedPayload
import com.pims.vault.core.crypto.ZeroizingByteArrayOutputStream
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.model.AuditEventType
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.data.local.dao.RelationshipNoteDao
import com.pims.vault.data.local.entity.RelationshipNoteEntity
import com.pims.vault.domain.model.NoteFormat
import com.pims.vault.domain.model.RelationshipNote
import com.pims.vault.domain.model.RelationshipNoteAttachment
import com.pims.vault.domain.model.RelationshipNoteAttachmentDraft
import com.pims.vault.domain.repository.RelationshipNotesRepository
import com.pims.vault.domain.rules.VaultRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.data.local.entity.RelationshipNoteAttachmentEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.io.ByteArrayInputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RelationshipNotesRepositoryImpl @Inject constructor(
    private val relationshipNoteDao: RelationshipNoteDao,
    private val cryptoEngine: CryptoEngine,
    private val sessionManager: BiometricSessionManager,
    private val auditLogger: HardenedAuditLogger,
    private val fileStorage: FileStorageService,
    private val firestoreSyncService: com.pims.vault.core.sync.FirestoreSyncService? = null
) : RelationshipNotesRepository {

    override fun getNotesForRelationshipFlow(
        relationshipId: String,
        isUnlocked: Boolean
    ): Flow<List<RelationshipNote>> {
        return relationshipNoteDao.getNotesWithAttachmentsForRelationshipFlow(relationshipId).map { entities ->
            entities.map { row ->
                val entity = row.note
                val attachments = row.attachments.map {
                    RelationshipNoteAttachment(it.id, it.mimeType, it.fileSizeBytes, it.createdAt)
                }
                if (!entity.isPrivate) {
                    RelationshipNote(
                        id = entity.id,
                        relationshipId = entity.relationshipId,
                        topic = entity.topic?.takeIf { it.isNotBlank() } ?: "Note",
                        content = entity.contentPlaintext ?: "",
                        format = entity.format,
                        isPrivate = false,
                        isDecrypted = true,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt,
                        attachments = attachments
                    )
                } else if (!isUnlocked || !sessionManager.isZone4Unlocked()) {
                    RelationshipNote(
                        id = entity.id,
                        relationshipId = entity.relationshipId,
                        topic = entity.topic?.takeIf { it.isNotBlank() } ?: "Note",
                        content = "",
                        format = entity.format,
                        isPrivate = true,
                        isDecrypted = false,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt
                    )
                } else {
                    val decryptedContent = try {
                        decryptPrivateContent(entity)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        null
                    }

                    RelationshipNote(
                        id = entity.id,
                        relationshipId = entity.relationshipId,
                        topic = entity.topic?.takeIf { it.isNotBlank() } ?: "Note",
                        content = decryptedContent ?: "",
                        format = entity.format,
                        isPrivate = true,
                        isDecrypted = decryptedContent != null,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt,
                        attachments = if (decryptedContent != null) attachments else emptyList()
                    )
                }
            }
        }
    }

    override fun getPrivateNoteCountFlow(relationshipId: String): Flow<Int> {
        return relationshipNoteDao.getPrivateNoteCountFlow(relationshipId)
    }

    override suspend fun saveNote(
        relationshipId: String,
        topic: String?,
        content: String,
        format: NoteFormat,
        isPrivate: Boolean,
        existingId: String?,
        attachmentsToAdd: List<RelationshipNoteAttachmentDraft>,
        attachmentIdsToRemove: Set<String>
    ): String {
        val staged = mutableListOf<RelationshipNoteAttachmentEntity>()
        var committed = false
        try {
            val existing = existingId?.let {
                requireNotNull(relationshipNoteDao.getNoteById(it)) { "Note no longer exists" }
            }
            require(existing == null || existing.relationshipId == relationshipId) { "Note belongs to another person" }
            if (existing?.isPrivate == true) {
                // Authenticate the source before replacing a private record, including privacy changes.
                decryptPrivateContent(existing)
            }
            attachmentsToAdd.forEach { validateAttachment(it) }
            val id = existingId ?: UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val encrypted = if (isPrivate) encryptPrivateContent(id, relationshipId, content) else null
            val entity = RelationshipNoteEntity(
                id = id,
                relationshipId = relationshipId,
                topic = topic?.trim(),
                contentPlaintext = if (isPrivate) null else content,
                encryptedPayload = encrypted?.combinedCiphertextWithTag,
                encryptionIv = encrypted?.iv?.joinToString("") { "%02x".format(it) },
                format = format,
                isPrivate = isPrivate,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            val removed = attachmentIdsToRemove.map { attachmentId ->
                requireNotNull(relationshipNoteDao.getAttachmentById(attachmentId)?.takeIf { it.noteId == id }) {
                    "Attachment belongs to another note or was removed"
                }
            }
            attachmentsToAdd.forEach { staged.add(stageAttachment(id, it)) }
            if (isPrivate || existing?.isPrivate == true) requireZone4()
            relationshipNoteDao.saveWithAttachments(entity, existing, staged, attachmentIdsToRemove)
            committed = true
            // File deletion follows the transaction: failed saves must preserve existing files.
            removed.forEach { attachment ->
                bestEffort { fileStorage.deleteFile(attachment.storagePath) }
                bestEffort { firestoreSyncService?.deleteRemoteRelationshipNoteAttachment(id, attachment.id) }
            }
            syncNote(entity)
            bestEffort {
                auditLogger.recordEvent(
                    eventType = if (existingId == null) AuditEventType.CREATE else AuditEventType.UPDATE,
                    entityType = "RelationshipNote",
                    entityId = id,
                    description = "Saved relationship note (private=$isPrivate, format=${format.name})"
                )
            }
            return id
        } finally {
            attachmentsToAdd.forEach { it.bytes.fill(0) }
            if (!committed) withContext(NonCancellable) {
                staged.forEach { attachment -> bestEffort { fileStorage.deleteFile(attachment.storagePath) } }
            }
        }
    }

    override suspend fun setNotePrivacy(id: String, makePrivate: Boolean): Boolean {
        val existing = relationshipNoteDao.getNoteById(id) ?: return false
        if (existing.isPrivate == makePrivate) return true

        requireZone4()
        val now = System.currentTimeMillis()
        val updated = if (makePrivate) {
            val encrypted = encryptPrivateContent(id, existing.relationshipId, existing.contentPlaintext.orEmpty())
            existing.copy(
                contentPlaintext = null,
                encryptedPayload = encrypted.combinedCiphertextWithTag,
                encryptionIv = encrypted.iv.joinToString("") { "%02x".format(it) },
                isPrivate = true,
                updatedAt = now
            )
        } else {
            existing.copy(
                contentPlaintext = decryptPrivateContent(existing),
                encryptedPayload = null,
                encryptionIv = null,
                isPrivate = false,
                updatedAt = now
            )
        }
        requireZone4()
        relationshipNoteDao.saveWithAttachments(updated, existing, emptyList(), emptySet())
        syncNote(updated)
        bestEffort {
            auditLogger.recordEvent(
                eventType = AuditEventType.UPDATE,
                entityType = "RelationshipNote",
                entityId = id,
                description = "Toggled relationship note privacy to isPrivate=$makePrivate"
            )
        }
        return true
    }

    override suspend fun deleteNote(id: String) {
        val note = relationshipNoteDao.getNoteById(id) ?: return
        if (note.isPrivate) requireZone4()
        val attachments = relationshipNoteDao.getAttachments(id)
        relationshipNoteDao.deleteById(id)
        attachments.forEach { attachment -> bestEffort { fileStorage.deleteFile(attachment.storagePath) } }
        bestEffort { firestoreSyncService?.deleteRemoteRelationshipNote(id) }
        bestEffort {
            auditLogger.recordEvent(
                eventType = AuditEventType.DELETE,
                entityType = "RelationshipNote",
                entityId = id,
                description = "Deleted relationship note"
            )
        }
    }

    override suspend fun readAttachment(noteId: String, attachmentId: String): Pair<ByteArray, String>? {
        val note = relationshipNoteDao.getNoteById(noteId) ?: return null
        val attachment = relationshipNoteDao.getAttachmentById(attachmentId)?.takeIf { it.noteId == noteId } ?: return null
        if (note.isPrivate && !sessionManager.isZone4Unlocked()) return null
        require(attachment.fileSizeBytes in 1..MAX_ATTACHMENT_BYTES.toLong() && attachment.mimeType in SUPPORTED_ATTACHMENT_TYPES) {
            "Invalid attachment metadata"
        }
        val output = ZeroizingByteArrayOutputStream()
        var count = 0
        val bounded = object : OutputStream() {
            override fun write(value: Int) {
                require(count < MAX_ATTACHMENT_BYTES) { "Attachment is too large" }
                output.write(value)
                count++
            }
            override fun write(bytes: ByteArray, offset: Int, length: Int) {
                require(length <= MAX_ATTACHMENT_BYTES - count) { "Attachment is too large" }
                output.write(bytes, offset, length)
                count += length
            }
        }
        return try {
            fileStorage.readDecryptedFile(attachment.storagePath, "", attachment.sha256Hash, bounded)
            check(count.toLong() == attachment.fileSizeBytes) { "Attachment size did not match" }
            if (note.isPrivate) requireZone4()
            output.takeBytesAndWipe() to attachment.mimeType
        } finally {
            output.wipe()
        }
    }

    private fun validateAttachment(draft: RelationshipNoteAttachmentDraft) {
        require(draft.bytes.isNotEmpty() && draft.bytes.size <= MAX_ATTACHMENT_BYTES) { "Attachment is empty or too large" }
        require(draft.mimeType in SUPPORTED_ATTACHMENT_TYPES) { "Unsupported attachment type" }
    }

    private suspend fun stageAttachment(noteId: String, draft: RelationshipNoteAttachmentDraft): RelationshipNoteAttachmentEntity {
        val attachmentId = UUID.randomUUID().toString()
        val metadata = fileStorage.storeEncryptedFile(
            documentId = "rel_note_${noteId}_$attachmentId",
            versionNumber = 1,
            mimeType = draft.mimeType,
            inputStream = ByteArrayInputStream(draft.bytes)
        )
        return RelationshipNoteAttachmentEntity(
            id = attachmentId, noteId = noteId, storagePath = metadata.relativePath,
            mimeType = metadata.mimeType, fileSizeBytes = metadata.sizeBytes, sha256Hash = metadata.sha256Hex
        )
    }

    private fun requireZone4() {
        if (!sessionManager.isZone4Unlocked()) throw SecurityException("Unlock the security vault to access private notes")
    }

    private fun encryptPrivateContent(id: String, relationshipId: String, content: String): EncryptedPayload {
        requireZone4()
        val root = sessionManager.getZone4VaultKey()
        val plain = content.toByteArray(Charsets.UTF_8)
        try {
            val itemKey = VaultRules.deriveItemKey(root, VaultCategory.SECURE_NOTE, id)
            try {
                return cryptoEngine.encrypt(plain, itemKey, constructAad(id, relationshipId))
            } finally {
                itemKey.fill(0)
            }
        } finally {
            root.fill(0)
            plain.fill(0)
        }
    }

    /** Historical private notes used the database key or the root-wrapped v1 vault key.
     * These are read-only candidates and remain gated by fresh Zone 4 authorization. */
    private fun decryptPrivateContent(note: RelationshipNoteEntity): String {
        requireZone4()
        val ivHex = requireNotNull(note.encryptionIv) { "Missing note IV" }
        require(ivHex.length == 24 && ivHex.all { it.digitToIntOrNull(16) != null }) { "Malformed note IV" }
        val payload = EncryptedPayload(
            requireNotNull(note.encryptedPayload).also { require(it.size >= 16) },
            ivHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        )
        val candidates = mutableListOf<ByteArray>()
        try {
            candidates.add(sessionManager.getZone4VaultKey())
            candidates.add(sessionManager.getDatabaseKey())
            sessionManager.getHardwareSubkey(isZone4 = false)?.let { candidates.add(it) }
            for (root in candidates) {
                val itemKey = VaultRules.deriveItemKey(root, VaultCategory.SECURE_NOTE, note.id)
                val plain = try {
                    cryptoEngine.decrypt(payload, itemKey, constructAad(note.id, note.relationshipId))
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    continue
                } finally {
                    itemKey.fill(0)
                }
                try {
                    requireZone4()
                    return String(plain, Charsets.UTF_8)
                } finally {
                    plain.fill(0)
                }
            }
            throw SecurityException("Private note could not be authenticated")
        } finally {
            candidates.forEach { it.fill(0) }
        }
    }

    private suspend fun syncNote(note: RelationshipNoteEntity) {
        if (note.isPrivate) {
            bestEffort { firestoreSyncService?.deleteRemoteRelationshipNote(note.id) }
        } else {
            bestEffort { firestoreSyncService?.syncRelationshipNote(null, note) }
            relationshipNoteDao.getAttachments(note.id).forEach { attachment ->
                bestEffort { firestoreSyncService?.syncRelationshipNoteAttachment(null, note.id, attachment) }
            }
        }
    }

    private suspend fun bestEffort(action: suspend () -> Unit) {
        try {
            action()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Local success must remain success when optional cloud/audit work is unavailable.
        }
    }

    private fun constructAad(noteId: String, relationshipId: String): ByteArray {
        return "PIMS|rel-note|v1\nid=${noteId.trim()}\nrelId=${relationshipId.trim()}".toByteArray(Charsets.UTF_8)
    }

    private companion object {
        const val MAX_ATTACHMENT_BYTES = 10 * 1024 * 1024
        val SUPPORTED_ATTACHMENT_TYPES = setOf("image/jpeg", "image/png", "application/pdf")
    }
}
