package com.pims.vault.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pims.vault.data.local.entity.CalendarReminderLinkEntity

@Dao
interface CalendarReminderLinkDao {
    @Query("SELECT * FROM calendar_reminder_links")
    suspend fun getAll(): List<CalendarReminderLinkEntity>

    @Upsert suspend fun upsert(link: CalendarReminderLinkEntity)

    @Query("DELETE FROM calendar_reminder_links WHERE note_id = :noteId")
    suspend fun delete(noteId: String)
}
