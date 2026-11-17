package com.pims.vault.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_reminder_links")
data class CalendarReminderLinkEntity(
    @PrimaryKey @ColumnInfo(name = "note_id") val noteId: String,
    @ColumnInfo(name = "calendar_id") val calendarId: Long,
    @ColumnInfo(name = "event_id") val eventId: Long,
    @ColumnInfo(name = "content_hash") val contentHash: String
)
