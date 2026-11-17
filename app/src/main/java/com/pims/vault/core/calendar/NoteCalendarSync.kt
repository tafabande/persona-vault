package com.pims.vault.core.calendar

import com.pims.vault.data.local.dao.CalendarReminderLinkDao
import com.pims.vault.data.local.entity.CalendarReminderLinkEntity

/** The app's note IDs and provider ownership markers make interrupted writes safe to retry. */
internal suspend fun synchronizeNoteReminders(
    selectedCalendar: Long,
    reminders: List<CalendarReminder>,
    calendar: CalendarReminderStore,
    links: CalendarReminderLinkDao
) {
    val remindersById = reminders.associateBy { it.noteId }
    val existing = links.getAll().associateBy { it.noteId }
    existing.values.filter { it.noteId !in remindersById || it.calendarId != selectedCalendar }.forEach { link ->
        calendar.delete(link.eventId, link.calendarId, link.noteId)
        links.delete(link.noteId)
    }
    reminders.forEach { reminder ->
        val link = existing[reminder.noteId]?.takeIf { it.calendarId == selectedCalendar }
        val owned = link?.takeIf { calendar.ownedEvent(it.eventId, selectedCalendar, reminder.noteId) }
        val eventId = owned?.eventId ?: calendar.findEvent(selectedCalendar, reminder.noteId)
        if (owned?.contentHash != reminder.fingerprint || eventId == null) {
            val savedId = calendar.save(selectedCalendar, reminder, eventId)
            links.upsert(CalendarReminderLinkEntity(reminder.noteId, selectedCalendar, savedId, reminder.fingerprint))
        }
    }
}
