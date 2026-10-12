package com.pims.vault.data.repository

import com.pims.vault.core.logging.VaultLogger
import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.data.local.dao.PlainNoteDao
import com.pims.vault.data.local.entity.PlainNoteAttachmentEntity
import com.pims.vault.data.local.entity.PlainNoteEntity
import com.pims.vault.domain.model.NoteAttachment
import com.pims.vault.domain.model.NoteFormat
import com.pims.vault.domain.model.PlainNote
import com.pims.vault.domain.repository.PlainNotesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlainNotesRepositoryImpl @Inject constructor(
    private val dao: PlainNoteDao,
    private val fileStorage: FileStorageService,
    private val firestoreSyncService: com.pims.vault.core.sync.FirestoreSyncService? = null,
    private val reminderScheduler: com.pims.vault.core.reminder.NoteReminderScheduler? = null,
    private val syncQueueManager: com.pims.vault.core.sync.SyncQueueManager? = null,
    private val networkMonitor: com.pims.vault.core.sync.NetworkStateMonitor? = null
) : PlainNotesRepository {

    private val repositoryScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob()
    )

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
            isReminderDone = isReminderDone,
            isLocked = isLocked
        )
        // 1. Primary local database save (Room/SQLCipher) — runs synchronously and immediately
        dao.upsert(entity)

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

        // 3. Remote sync or offline queue — non-blocking to guarantee local-first offline availability
        dispatchRemoteSyncOrQueue(
            entity = entity,
            action = if (existing == null) com.pims.vault.data.local.entity.SyncAction.INSERT else com.pims.vault.data.local.entity.SyncAction.UPDATE
        )

        val attachments = dao.getAttachments(noteId).map { toDomain(it) }
        return toDomain(entity, attachments)
    }

    override suspend fun toggleLock(noteId: String, isLocked: Boolean) {
        dao.setNoteLocked(noteId, isLocked)
        val note = dao.getById(noteId)
        note?.let {
            dispatchRemoteSyncOrQueue(it, com.pims.vault.data.local.entity.SyncAction.UPDATE)
        }
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
            dispatchRemoteSyncOrQueue(it, com.pims.vault.data.local.entity.SyncAction.UPDATE)
        }
    }

    override suspend fun markReminderDone(noteId: String, isDone: Boolean) {
        dao.setReminderDone(noteId, isDone)
        if (isDone) {
            reminderScheduler?.cancelReminder(noteId)
        }
        val note = dao.getById(noteId)
        note?.let {
            dispatchRemoteSyncOrQueue(it, com.pims.vault.data.local.entity.SyncAction.UPDATE)
        }
    }

    override suspend fun clearReminder(noteId: String) {
        dao.clearReminder(noteId)
        reminderScheduler?.cancelReminder(noteId)
        val note = dao.getById(noteId)
        note?.let {
            dispatchRemoteSyncOrQueue(it, com.pims.vault.data.local.entity.SyncAction.UPDATE)
        }
    }

    override suspend fun delete(id: String) {
        reminderScheduler?.cancelReminder(id)
        val existing = dao.getById(id)
        // Delete attachment binaries first so restores don't resurrect them.
        try {
            val attachments = dao.getAttachments(id)
            for (att in attachments) {
                try { fileStorage.deleteFile(att.storagePath) } catch (_: Exception) {}
                repositoryScope.launch {
                    if (networkMonitor?.isCurrentlyConnected() == true) {
                        try {
                            kotlinx.coroutines.withTimeoutOrNull(3000L) {
                                firestoreSyncService?.deleteRemoteNoteAttachment(id, att.id)
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {}
        dao.deleteById(id)

        existing?.let {
            dispatchRemoteSyncOrQueue(it, com.pims.vault.data.local.entity.SyncAction.DELETE)
        } ?: run {
            repositoryScope.launch {
                if (networkMonitor?.isCurrentlyConnected() == true) {
                    try {
                        kotlinx.coroutines.withTimeoutOrNull(3000L) {
                            firestoreSyncService?.deleteNote(null, id)
                        }
                    } catch (_: Exception) {}
                }
            }
        }
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

        repositoryScope.launch {
            if (networkMonitor?.isCurrentlyConnected() == true) {
                try {
                    kotlinx.coroutines.withTimeoutOrNull(8000L) {
                        firestoreSyncService?.syncNoteAttachment(null, noteId, entity)
                    }
                } catch (_: Exception) {}
            }
        }
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
        val existing = dao.getAttachment(attachmentId)
        if (existing != null) {
            try {
                fileStorage.deleteFile(existing.storagePath)
            } catch (_: Exception) {}
            dao.deleteAttachment(attachmentId)
            dao.touch(existing.noteId, System.currentTimeMillis())

            repositoryScope.launch {
                if (networkMonitor?.isCurrentlyConnected() == true) {
                    try {
                        kotlinx.coroutines.withTimeoutOrNull(3000L) {
                            firestoreSyncService?.deleteRemoteNoteAttachment(existing.noteId, attachmentId)
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun dispatchRemoteSyncOrQueue(
        entity: PlainNoteEntity,
        action: com.pims.vault.data.local.entity.SyncAction
    ) {
        repositoryScope.launch {
            val isOnline = networkMonitor?.isCurrentlyConnected() ?: false
            if (isOnline) {
                try {
                    val syncSucceeded = kotlinx.coroutines.withTimeoutOrNull(3000L) {
                        firestoreSyncService?.syncNote(null, entity, isDeleted = (action == com.pims.vault.data.local.entity.SyncAction.DELETE))
                        true
                    } ?: false
                    if (!syncSucceeded) {
                        VaultLogger.w("PlainNotesRepository", "Immediate sync timed out; queuing offline mutation")
                        enqueueSync(entity, action)
                    }
                } catch (e: Exception) {
                    VaultLogger.w("PlainNotesRepository", "Immediate sync failed (${e.message}); queuing offline mutation")
                    enqueueSync(entity, action)
                }
            } else {
                VaultLogger.i("PlainNotesRepository", "Device is offline; queuing note mutation for ${entity.id}")
                enqueueSync(entity, action)
            }
        }
    }

    private suspend fun enqueueSync(
        entity: PlainNoteEntity,
        action: com.pims.vault.data.local.entity.SyncAction
    ) {
        try {
            syncQueueManager?.enqueueOperation(
                entityType = "PLAIN_NOTE",
                entityId = entity.id,
                action = action,
                payloadJson = entityToJson(entity)
            )
        } catch (e: Exception) {
            VaultLogger.w("PlainNotesRepository", "Failed to enqueue note sync mutation: ${e.message}")
        }
    }

    private fun entityToJson(entity: PlainNoteEntity): String {
        val json = org.json.JSONObject()
        json.put("id", entity.id)
        json.put("ownerPersonId", entity.ownerPersonId)
        json.put("title", entity.title)
        json.put("content", entity.content)
        json.put("format", entity.format)
        json.put("createdAt", entity.createdAt)
        json.put("updatedAt", entity.updatedAt)
        entity.reminderAt?.let { json.put("reminderAt", it) }
        entity.reminderTag?.let { json.put("reminderTag", it) }
        entity.reminderRepeat?.let { json.put("reminderRepeat", it) }
        json.put("isReminderDone", entity.isReminderDone)
        json.put("isLocked", entity.isLocked)
        return json.toString()
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
            isLocked = entity.isLocked
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
