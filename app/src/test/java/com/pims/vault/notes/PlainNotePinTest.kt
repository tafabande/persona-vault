package com.pims.vault.notes

import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.data.local.dao.PlainNoteDao
import com.pims.vault.data.local.entity.PlainNoteEntity
import com.pims.vault.data.repository.PlainNotesRepositoryImpl
import com.pims.vault.domain.model.NoteFormat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.*

class PlainNotePinTest {
    @Test fun `editing content preserves an existing pin`() = runBlocking {
        val dao = mock<PlainNoteDao>()
        whenever(dao.getById("note")).thenReturn(PlainNoteEntity("note", "owner", "Old", "Body", isPinned = true))
        whenever(dao.getAttachments("note")).thenReturn(emptyList())
        val repository = PlainNotesRepositoryImpl(dao, mock<FileStorageService>())
        val saved = repository.save("note", "owner", "Edited", NoteFormat.PLAIN, "Changed")
        assertTrue(saved.isPinned)
        verify(dao).upsert(check { assertTrue(it.isPinned) })
    }

    @Test fun `pin action patches metadata without replacing note or attachments`() = runBlocking {
        val dao = mock<PlainNoteDao>()
        val repository = PlainNotesRepositoryImpl(dao, mock<FileStorageService>())
        repository.setPinned("note", true)
        verify(dao).setPinned(eq("note"), eq(true), any())
        verify(dao, never()).upsert(any())
        verify(dao, never()).upsertAttachment(any())
        verify(dao, never()).deleteById(any())
    }
}
