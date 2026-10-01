package com.pims.vault.data.repository

import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.data.local.dao.PlainNoteDao
import com.pims.vault.data.local.entity.PlainNoteAttachmentEntity
import com.pims.vault.data.local.entity.PlainNoteEntity
import com.pims.vault.domain.model.NoteAttachment
import com.pims.vault.domain.model.NoteFormat
import com.pims.vault.domain.model.PlainNote
import com.pims.vault.domain.repository.PlainNotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlainNotesRepositoryImpl @Inject constructor(
    private val dao: PlainNoteDao,
    private val fileStorage: FileStorageService,
    private val firestoreSyncService: com.pims.vault.core.sync.FirestoreSyncService? = null,
    private val reminderScheduler: com.pims.vault.core.reminder.NoteReminderScheduler? = null
) : PlainNotesRepository {

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
        isReminderDone: Boolean
    ): PlainNote {
        val noteId = id ?: UUID.randomUUID().toString()
        val cleanTitle = title.trim()
        val finalTitle = cleanTitle.ifBlank { "Untitled" }
        val existing = id?.let { dao.getById(it) }
        val now = System.currentTimeMillis()
        val entity = PlainNoteEntity(
            id = noteId,
            ownerPersonId = ownerPersonId,
            title = finalTitle,
            content = content,
            format = format.name,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            reminderAt = reminderAt ?: existing?.reminderAt,
            reminderTag = reminderTag ?: existing?.reminderTag,
            reminderRepeat = reminderRepeat ?: existing?.reminderRepeat,
            isReminderDone = isReminderDone
        )
        dao.upsert(entity)

        // Manage actual AlarmManager notification
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

        try {
            firestoreSyncService?.syncNote(null, entity, isDeleted = false)
        } catch (_: Exception) {}
        val attachments = dao.getAttachments(noteId).map { toDomain(it) }
        return toDomain(entity, attachments)
    }

    override suspend fun setReminder(
        noteId: String,
        reminderAt: Long?,
        reminderTag: String?,
        reminderRepeat: String?
    ) {
        dao.updateReminder(noteId, reminderAt, reminderTag, reminderRepeat)
        val note = dao.getById(noteId)
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
        note?.let {
            try { firestoreSyncService?.syncNote(null, it, isDeleted = false) } catch (_: Exception) {}
        }
    }

    override suspend fun markReminderDone(noteId: String, isDone: Boolean) {
        dao.setReminderDone(noteId, isDone)
        if (isDone) {
            reminderScheduler?.cancelReminder(noteId)
        }
        val note = dao.getById(noteId)
        note?.let {
            try { firestoreSyncService?.syncNote(null, it, isDeleted = false) } catch (_: Exception) {}
        }
    }

    override suspend fun clearReminder(noteId: String) {
        dao.clearReminder(noteId)
        reminderScheduler?.cancelReminder(noteId)
        val note = dao.getById(noteId)
        note?.let {
            try { firestoreSyncService?.syncNote(null, it, isDeleted = false) } catch (_: Exception) {}
        }
    }

    override suspend fun delete(id: String) {
        reminderScheduler?.cancelReminder(id)
        // Delete attachment binaries first so restores don't resurrect them.
        try {
            val attachments = dao.getAttachments(id)
            for (att in attachments) {
                try { fileStorage.deleteFile(att.storagePath) } catch (_: Exception) {}
                try { firestoreSyncService?.deleteRemoteNoteAttachment(id, att.id) } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        dao.deleteById(id)
        try {
            firestoreSyncService?.deleteNote(null, id)
        } catch (_: Exception) {}
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
        dao.upsertAttachment(entity)
        dao.touch(noteId, System.currentTimeMillis())
        try {
            firestoreSyncService?.syncNoteAttachment(null, noteId, entity)
        } catch (_: Exception) {}
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
            android.util.Log.e("PlainNotesRepository", "Failed to read attachment ${attachment.id}", e)
            null
        }
    }

    override suspend fun deleteAttachment(attachmentId: String) {
        val existing = dao.getAttachment(attachmentId)
        if (existing != null) {
            try {
                fileStorage.deleteFile(existing.storagePath)
            } catch (_: Exception) {}
            try {
                firestoreSyncService?.deleteRemoteNoteAttachment(existing.noteId, attachmentId)
            } catch (_: Exception) {}
            dao.deleteAttachment(attachmentId)
            dao.touch(existing.noteId, System.currentTimeMillis())
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
            isReminderDone = entity.isReminderDone
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
