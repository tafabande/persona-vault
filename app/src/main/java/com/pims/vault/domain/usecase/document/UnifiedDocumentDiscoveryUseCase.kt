package com.pims.vault.domain.usecase.document

import com.pims.vault.data.local.dao.DocumentDao
import com.pims.vault.data.local.dao.PlainNoteDao
import com.pims.vault.domain.model.document.UnifiedDocumentItem
import com.pims.vault.domain.model.document.UnifiedDocumentSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Aggregates standalone document records and plain note document attachments
 * into a single unified discovery flow without altering or merging underlying schemas.
 */
class UnifiedDocumentDiscoveryUseCase @Inject constructor(
    private val documentDao: DocumentDao,
    private val plainNoteDao: PlainNoteDao
) {
    operator fun invoke(
        personIdFilter: String? = null,
        tagFilter: String? = null,
        searchQuery: String? = null
    ): Flow<List<UnifiedDocumentItem>> {
        val standaloneDocsFlow = documentDao.getAllDocumentsFlow()
        val notesFlow = plainNoteDao.observeAllNotes()
        val noteAttachmentsFlow = plainNoteDao.observeAllAttachments()

        return combine(standaloneDocsFlow, notesFlow, noteAttachmentsFlow) { docsWithVersions, notes, noteAttachments ->
            val noteMap = notes.associateBy { it.id }

            // 1. Map standalone documents
            val standaloneItems = docsWithVersions.mapNotNull { docWithVersions ->
                val doc = docWithVersions.document
                val latestVersion = docWithVersions.latestVersion ?: docWithVersions.versions.maxByOrNull { it.versionNumber }

                UnifiedDocumentItem(
                    id = doc.id,
                    source = UnifiedDocumentSource.STANDALONE_DOCUMENT,
                    sourceRecordId = doc.id,
                    attachmentId = latestVersion?.id,
                    title = doc.title,
                    mimeType = latestVersion?.mimeType ?: "application/pdf",
                    fileSizeBytes = latestVersion?.fileSizeBytes ?: 0L,
                    storagePath = latestVersion?.fileStoragePath ?: "",
                    tags = emptyList(),
                    personId = doc.personId,
                    isLocked = doc.isLocked,
                    createdAt = doc.createdAt,
                    notesOrCaption = doc.notes
                )
            }

            // 2. Map note attachments
            val noteAttachmentItems = noteAttachments.mapNotNull { attachment ->
                val parentNote = noteMap[attachment.noteId]
                val displayTitle = if (!attachment.caption.isNullOrBlank()) {
                    attachment.caption
                } else if (!parentNote?.title.isNullOrBlank()) {
                    "${parentNote?.title} Attachment"
                } else {
                    "Note Attachment"
                }

                val noteTags = parentNote?.reminderTag?.split(",")?.map { it.trim().removePrefix("#") }?.filter { it.isNotEmpty() } ?: emptyList()
                UnifiedDocumentItem(
                    id = attachment.id,
                    source = UnifiedDocumentSource.NOTE_ATTACHMENT,
                    sourceRecordId = attachment.noteId,
                    attachmentId = attachment.id,
                    title = displayTitle,
                    mimeType = attachment.mimeType,
                    fileSizeBytes = attachment.fileSizeBytes,
                    storagePath = attachment.storagePath,
                    tags = noteTags,
                    personId = parentNote?.ownerPersonId,
                    isLocked = parentNote?.isLocked ?: false,
                    createdAt = attachment.createdAt,
                    notesOrCaption = attachment.caption ?: parentNote?.content
                )
            }

            // 3. Combine and sort
            val allItems = (standaloneItems + noteAttachmentItems).sortedByDescending { it.createdAt }

            // 4. Apply filters
            allItems.filter { item ->
                val matchesPerson = personIdFilter.isNullOrBlank() || item.personId == personIdFilter
                val matchesTag = tagFilter.isNullOrBlank() || item.tags.any { it.equals(tagFilter, ignoreCase = true) }
                val matchesSearch = searchQuery.isNullOrBlank() ||
                        item.title.contains(searchQuery, ignoreCase = true) ||
                        item.tags.any { it.contains(searchQuery, ignoreCase = true) } ||
                        (item.notesOrCaption?.contains(searchQuery, ignoreCase = true) == true)

                matchesPerson && matchesTag && matchesSearch
            }
        }
    }
}
