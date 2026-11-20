package com.pims.vault.notes

import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.core.sync.SyncQueueManager
import com.pims.vault.data.local.dao.PlainNoteDao
import com.pims.vault.data.local.entity.SyncAction
import com.pims.vault.data.repository.PlainNotesRepositoryImpl
import com.pims.vault.domain.model.NoteFormat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.argThat
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.whenever

class PlainNotesOfflineTest {

    @Test
    fun `save persists note and outbox before requesting network work`() {
        runBlocking {
            val dao = mock<PlainNoteDao>()
            val fileStorage = mock<FileStorageService>()
            val syncQueueManager = mock<SyncQueueManager>()

            whenever(dao.getAttachments(any())).thenReturn(emptyList())

            val repository = PlainNotesRepositoryImpl(
                dao = dao,
                fileStorage = fileStorage,
                syncQueueManager = syncQueueManager
            )

            val savedNote = repository.save(
                id = "note_offline_123",
                ownerPersonId = "user_primary",
                title = "My Offline Idea",
                format = NoteFormat.PLAIN,
                content = "Works without internet!",
                reminderAt = null,
                reminderTag = null,
                reminderRepeat = null,
                isLocked = false
            )

            assertNotNull(savedNote)
            assertEquals("note_offline_123", savedNote.id)
            assertEquals("My Offline Idea", savedNote.title)
            assertEquals("Works without internet!", savedNote.content)

            inOrder(dao, syncQueueManager) {
                verify(dao).upsert(argThat { id == savedNote.id })
                verify(syncQueueManager).enqueueNoteMutation(argThat { id == savedNote.id }, eq(SyncAction.INSERT), eq(0L))
                verify(syncQueueManager).requestBackgroundSync()
            }
        }
    }
}
