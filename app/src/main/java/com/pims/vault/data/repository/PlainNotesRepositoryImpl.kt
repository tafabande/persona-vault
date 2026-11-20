package com.pims.vault.data.repository

import androidx.room.withTransaction
import com.pims.vault.core.logging.VaultLogger
import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.core.sync.SyncQueueManager
import com.pims.vault.data.local.dao.PlainNoteDao
import com.pims.vault.data.local.database.PimsDatabase
import com.pims.vault.data.local.entity.PlainNoteAttachmentEntity
import com.pims.vault.data.local.entity.PlainNoteEntity
import com.pims.vault.data.local.entity.SyncAction
import com.pims.vault.domain.model.NoteAttachment
import com.pims.vault.domain.model.NoteFormat
import com.pims.vault.domain.model.PlainNote
import com.pims.vault.domain.repository.PlainNotesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlainNotesRepositoryImpl @Inject constructor(
    private val dao: PlainNoteDao,
    private val fileStorage: FileStorageService,
    private val reminderScheduler: com.pims.vault.core.reminder.NoteReminderScheduler? = null,
    private val syncQueueManager: SyncQueueManager? = null,
    private val database: PimsDatabase? = null
) : PlainNotesRepository {

    private suspend fun <T> localTransaction(block: suspend () -> T): T =
        if (database == null) block() else database.withTransaction { block() }

    private fun nextRevision(previous: PlainNoteEntity?): Long =
        maxOf(System.currentTimeMillis(), (previous?.updatedAt ?: 0L) + 1L)

    override fun observe(ownerPersonId: String): Flow<List<PlainNote>> {
        return dao.observeForOwner(ownerPersonId).map { entities ->
            val noteIds = entities.map { it.id }
            val attachmentsByNote = dao.getAttachmentsForNotes(noteIds).groupBy { it.noteId }
            entities.map { entity ->
                val attachments = attachmentsByNote[entity.id].orEmpty().map { toDomain(it) }
                toDomain(entity, attachments)
            }
        }
    }

    override suspend fun save(
        id: String?,
        ownerPersonId: String,
        title: String,
        format: NoteFormat,
        content: String,
        reminderAt: Long?,
        reminderTag: String?,
        reminderRepeat: String?,
        isReminderDone: Boolean,
        isLocked: Boolean
    ): PlainNote {
        val noteId = id ?: UUID.randomUUID().toString()
        val cleanTitle = title.trim()
        val finalTitle = cleanTitle.ifBlank { "Untitled" }
        val now = System.currentTimeMillis()
        val entity = localTransaction {
            val existing = id?.let { dao.getById(it) }
            val updated = PlainNoteEntity(
                id = noteId,
                ownerPersonId = existing?.ownerPersonId ?: ownerPersonId,
                title = finalTitle,
                content = content,
                format = format.name,
                createdAt = existing?.createdAt ?: now,
                updatedAt = nextRevision(existing),
                reminderAt = reminderAt ?: existing?.reminderAt,
                reminderTag = reminderTag ?: existing?.reminderTag,
                reminderRepeat = reminderRepeat ?: existing?.reminderRepeat,
                isReminderDone = isReminderDone,
                isLocked = isLocked,
                isPinned = existing?.isPinned ?: false
            )
            dao.upsert(updated)
            syncQueueManager?.enqueueNoteMutation(updated, if (existing == null) SyncAction.INSERT else SyncAction.UPDATE,
                existing?.updatedAt ?: 0L)
            updated
        }
        syncQueueManager?.requestBackgroundSync()

        // 2. Local AlarmManager reminder scheduling
        val effectiveReminder = entity.reminderAt
        if (effectiveReminder != null && !entity.isReminderDone && effectiveReminder > now) {
            reminderScheduler?.scheduleReminder(
                noteId = noteId,
                title = finalTitle,
                contentSnippet = content.take(120),
                tag = entity.reminderTag,
                triggerAtMillis = effectiveReminder,
                repeat = entity.reminderRepeat
            )
        } else if (effectiveReminder == null || entity.isReminderDone) {
            reminderScheduler?.cancelReminder(noteId)
        }

        val attachments = dao.getAttachments(noteId).map { toDomain(it) }
        return toDomain(entity, attachments)
    }

    override suspend fun setPinned(noteId: String, isPinned: Boolean) {
        updateNote(noteId) { revision -> dao.setPinned(noteId, isPinned, revision) }
    }

    override suspend fun toggleLock(noteId: String, isLocked: Boolean) {
        updateNote(noteId) { revision -> dao.setNoteLocked(noteId, isLocked, revision) }
    }

    override suspend fun setReminder(
        noteId: String,
        reminderAt: Long?,
        reminderTag: String?,
        reminderRepeat: String?
    ) {
        val note = updateNote(noteId) { revision -> dao.updateReminder(noteId, reminderAt, reminderTag, reminderRepeat, revision) }
        if (note != null && reminderAt != null && reminderAt > System.currentTimeMillis()) {
            reminderScheduler?.scheduleReminder(
                noteId = noteId,
                title = note.title,
                contentSnippet = note.content.take(120),
                tag = reminderTag,
                triggerAtMillis = reminderAt,
                repeat = reminderRepeat
            )
        } else if (reminderAt == null) {
            reminderScheduler?.cancelReminder(noteId)
        }
    }

    override suspend fun markReminderDone(noteId: String, isDone: Boolean) {
        updateNote(noteId) { revision -> dao.setReminderDone(noteId, isDone, revision) }
        if (isDone) {
            reminderScheduler?.cancelReminder(noteId)
        }
    }

    override suspend fun clearReminder(noteId: String) {
        updateNote(noteId) { revision -> dao.clearReminder(noteId, revision) }
        reminderScheduler?.cancelReminder(noteId)
    }

    override suspend fun delete(id: String) {
        val removedAttachments = localTransaction {
            val existing = dao.getById(id) ?: return@localTransaction emptyList()
            val attachments = dao.getAttachments(id)
            syncQueueManager?.enqueueNoteMutation(existing.copy(updatedAt = nextRevision(existing)), SyncAction.DELETE,
                existing.updatedAt)
            dao.deleteById(id)
            attachments
        }
        syncQueueManager?.requestBackgroundSync()
        reminderScheduler?.cancelReminder(id)
        removedAttachments.forEach { removeUnreferencedFile(it.storagePath) }
    }

    override suspend fun addAttachment(
        noteId: String,
        displayName: String,
        bytes: ByteArray,
        mimeType: String
    ): NoteAttachment {
        require(bytes.isNotEmpty()) { "Attachment cannot be empty" }
        val effectiveMime = if (mimeType.isBlank() || mimeType == "application/octet-stream") "image/jpeg" else mimeType
        val note = dao.getById(noteId) ?: error("Cannot attach an image to a missing note")

        val attachmentId = UUID.randomUUID().toString()
        val metadata = fileStorage.storeEncryptedFile(
            documentId = "note_${noteId}_${attachmentId}",
            versionNumber = 1,
            mimeType = effectiveMime,
            inputStream = java.io.ByteArrayInputStream(bytes)
        )

        val entity = PlainNoteAttachmentEntity(
            id = attachmentId,
            noteId = noteId,
            storagePath = metadata.relativePath,
            mimeType = metadata.mimeType,
            fileSizeBytes = metadata.sizeBytes,
            sha256Hash = metadata.sha256Hex,
            caption = displayName.trim(),
            createdAt = metadata.timestamp
        )
        try {
            localTransaction {
                dao.upsertAttachment(entity)
                touchWithOutbox(noteId)
                syncQueueManager?.enqueueNoteAttachment(noteId, attachmentId, SyncAction.INSERT)
            }
        } catch (failure: Exception) {
            withContext(NonCancellable) {
                val unpublished = runCatching { dao.getAttachment(attachmentId) == null }.getOrDefault(false)
                if (unpublished) removeUnreferencedFile(entity.storagePath)
            }
            throw failure
        }
        syncQueueManager?.requestBackgroundSync()
        return toDomain(entity)
    }

    override suspend fun readAttachment(attachment: NoteAttachment): ByteArray? {
        return try {
            val baos = java.io.ByteArrayOutputStream()
            fileStorage.readDecryptedFile(
                relativePath = attachment.storagePath,
                encryptionIvHex = "",
                expectedSha256Hex = attachment.sha256Hash,
                outputStream = baos
            )
            baos.toByteArray()
        } catch (e: Exception) {
            VaultLogger.e("PlainNotesRepository", "Failed to read attachment ${attachment.id}", e)
            null
        }
    }

    override suspend fun deleteAttachment(attachmentId: String) {
        val existing = localTransaction {
            val attachment = dao.getAttachment(attachmentId) ?: return@localTransaction null
            dao.deleteAttachment(attachmentId)
            touchWithOutbox(attachment.noteId)
            syncQueueManager?.enqueueNoteAttachment(attachment.noteId, attachmentId, SyncAction.DELETE)
            attachment
        } ?: return
        syncQueueManager?.requestBackgroundSync()
        removeUnreferencedFile(existing.storagePath)
    }

    private suspend fun updateNote(noteId: String, update: suspend (Long) -> Unit): PlainNoteEntity? {
        val note = localTransaction {
            val existing = dao.getById(noteId)
            update(nextRevision(existing))
            val updated = dao.getById(noteId) ?: return@localTransaction null
            syncQueueManager?.enqueueNoteMutation(updated, SyncAction.UPDATE, existing?.updatedAt ?: 0L)
            updated
        }
        if (note != null) syncQueueManager?.requestBackgroundSync()
        return note
    }

    private suspend fun touchWithOutbox(noteId: String) {
        val previous = dao.getById(noteId) ?: return
        val revision = nextRevision(previous)
        dao.touch(noteId, revision)
        syncQueueManager?.enqueueNoteMutation(previous.copy(updatedAt = revision), SyncAction.UPDATE, previous.updatedAt)
    }

    private suspend fun removeUnreferencedFile(path: String) {
        try {
            fileStorage.deleteFile(path)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            VaultLogger.w("PlainNotesRepository", "Attachment cleanup could not finish")
        }
    }

    private fun toDomain(
        entity: PlainNoteEntity,
        attachments: List<NoteAttachment>
    ): PlainNote {
        return PlainNote(
            id = entity.id,
            ownerPersonId = entity.ownerPersonId,
            title = entity.title,
            content = entity.content,
            format = NoteFormat.fromString(entity.format),
            attachments = attachments,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            reminderAt = entity.reminderAt,
            reminderTag = entity.reminderTag,
            reminderRepeat = entity.reminderRepeat,
            isReminderDone = entity.isReminderDone,
            isLocked = entity.isLocked,
            isPinned = entity.isPinned
        )
    }

    private fun toDomain(entity: PlainNoteAttachmentEntity): NoteAttachment {
        return NoteAttachment(
            id = entity.id,
            noteId = entity.noteId,
            storagePath = entity.storagePath,
            mimeType = entity.mimeType,
            fileSizeBytes = entity.fileSizeBytes,
            sha256Hash = entity.sha256Hash,
            caption = entity.caption ?: "",
            createdAt = entity.createdAt
        )
    }
}
