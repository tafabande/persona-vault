package com.pims.vault.notes

import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.core.sync.FirestoreSyncService
import com.pims.vault.core.sync.NetworkStateMonitor
import com.pims.vault.core.sync.SyncQueueManager
import com.pims.vault.data.local.dao.PlainNoteDao
import com.pims.vault.data.local.entity.PlainNoteEntity
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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PlainNotesOfflineTest {

    @Test
    fun testSavingNoteWhenOfflineSucceedsAndEnqueuesSync() {
        runBlocking {
            val dao = mock<PlainNoteDao>()
            val fileStorage = mock<FileStorageService>()
            val firestoreSyncService = mock<FirestoreSyncService>()
            val syncQueueManager = mock<SyncQueueManager>()
            val networkMonitor = mock<NetworkStateMonitor>()

            // Simulate device is OFFLINE
            whenever(networkMonitor.isCurrentlyConnected()).thenReturn(false)

            val repository = PlainNotesRepositoryImpl(
                dao = dao,
                fileStorage = fileStorage,
                firestoreSyncService = firestoreSyncService,
                reminderScheduler = null,
                syncQueueManager = syncQueueManager,
                networkMonitor = networkMonitor
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

            // 1. Verify note was immediately returned and not null
            assertNotNull(savedNote)
            assertEquals("note_offline_123", savedNote.id)
            assertEquals("My Offline Idea", savedNote.title)
            assertEquals("Works without internet!", savedNote.content)

            // 2. Verify DAO upsert was called
            verify(dao).upsert(any())

            // Allow async scope to execute
            kotlinx.coroutines.delay(100)

            // 3. Verify Firestore immediate sync was NOT called because device is offline
            verify(firestoreSyncService, never()).syncNote(any(), any(), any())

            // 4. Verify operation was enqueued in SyncQueueManager
            verify(syncQueueManager).enqueueOperation(
                eq("PLAIN_NOTE"),
                eq("note_offline_123"),
                eq(SyncAction.INSERT),
                any(),
                any()
            )
        }
    }

    @Test
    fun testSavingNoteWhenOnlineSyncsDirectly() {
        runBlocking {
            val dao = mock<PlainNoteDao>()
            val fileStorage = mock<FileStorageService>()
            val firestoreSyncService = mock<FirestoreSyncService>()
            val syncQueueManager = mock<SyncQueueManager>()
            val networkMonitor = mock<NetworkStateMonitor>()

            // Simulate device is ONLINE
            whenever(networkMonitor.isCurrentlyConnected()).thenReturn(true)

            val repository = PlainNotesRepositoryImpl(
                dao = dao,
                fileStorage = fileStorage,
                firestoreSyncService = firestoreSyncService,
                reminderScheduler = null,
                syncQueueManager = syncQueueManager,
                networkMonitor = networkMonitor
            )

            val savedNote = repository.save(
                id = "note_online_456",
                ownerPersonId = "user_primary",
                title = "Online Note",
                format = NoteFormat.PLAIN,
                content = "Direct sync test",
                reminderAt = null,
                reminderTag = null,
                reminderRepeat = null,
                isLocked = false
            )

            assertNotNull(savedNote)
            verify(dao).upsert(any())

            // Allow async scope to execute
            kotlinx.coroutines.delay(100)

            // Verify Firestore immediate sync WAS called
            verify(firestoreSyncService).syncNote(eq(null), any(), eq(false))
        }
    }
}
