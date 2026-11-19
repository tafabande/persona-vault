package com.pims.vault.notes

import com.pims.vault.core.calendar.CalendarReminder
import com.pims.vault.core.calendar.CalendarReminderStore
import com.pims.vault.core.calendar.DeviceCalendarEvent
import com.pims.vault.core.calendar.calendarReminder
import com.pims.vault.core.calendar.synchronizeNoteReminders
import com.pims.vault.data.local.dao.CalendarReminderLinkDao
import com.pims.vault.data.local.entity.CalendarReminderLinkEntity
import com.pims.vault.data.local.entity.PlainNoteEntity
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class NoteCalendarSyncTest {
    private val reminder = CalendarReminder("note-1", "Lunch", 1_800_000_000_000L, "FREQ=WEEKLY", "Africa/Harare")
    private val calendar = FakeCalendar()
    private val links = FakeLinks()

    @Test fun `all reminders create events and unchanged retry keeps event IDs`() = runTest {
        val second = reminder.copy(noteId = "note-2", title = "Call")
        synchronizeNoteReminders(3, listOf(reminder, second), calendar, links)
        val saved = links.getAll()
        synchronizeNoteReminders(3, listOf(reminder, second), calendar, links)
        assertEquals(2, calendar.saves)
        assertEquals(saved, links.getAll())
        assertEquals(setOf("note-1", "note-2"), calendar.events.values.map { it.reminder.noteId }.toSet())
    }

    @Test fun `edits retain event identity and completion removes its mapping`() = runTest {
        synchronizeNoteReminders(3, listOf(reminder), calendar, links)
        val eventId = links.getAll().single().eventId
        synchronizeNoteReminders(3, listOf(reminder.copy(at = reminder.at + 60_000)), calendar, links)
        assertEquals(eventId, links.getAll().single().eventId)
        assertEquals(reminder.at + 60_000, calendar.events[eventId]?.reminder?.at)
        synchronizeNoteReminders(3, emptyList(), calendar, links)
        assertTrue(calendar.events.isEmpty())
        assertTrue(links.getAll().isEmpty())
    }

    @Test fun `destination change removes owned old event and creates a new mapping`() = runTest {
        synchronizeNoteReminders(3, listOf(reminder), calendar, links)
        val oldId = links.getAll().single().eventId
        synchronizeNoteReminders(8, listOf(reminder), calendar, links)
        assertFalse(calendar.events.containsKey(oldId))
        assertEquals(8L, links.getAll().single().calendarId)
        assertEquals(8L, calendar.events.values.single().calendar)
    }

    @Test fun `stale mapping cannot change an unrelated device event`() = runTest {
        calendar.events[17] = FakeCalendar.Event(3, reminder.copy(noteId = "unrelated", title = "Other event"))
        links.upsert(CalendarReminderLinkEntity(reminder.noteId, 3, 17, "stale"))
        synchronizeNoteReminders(3, listOf(reminder), calendar, links)
        assertEquals("Other event", calendar.events[17]?.reminder?.title)
        assertNotEquals(17L, links.getAll().single().eventId)
        synchronizeNoteReminders(3, emptyList(), calendar, links)
        assertEquals(setOf(17L), calendar.events.keys)
    }

    @Test fun `provider insert before mapping failure is reused on retry`() = runTest {
        links.failNextWrite = true
        assertTrue(runCatching { synchronizeNoteReminders(3, listOf(reminder), calendar, links) }.isFailure)
        val eventId = calendar.events.keys.single()
        synchronizeNoteReminders(3, listOf(reminder), calendar, links)
        assertEquals(1, calendar.events.size)
        assertEquals(eventId, links.getAll().single().eventId)
    }

    @Test fun `provider deletion failure retains mapping for retry`() = runTest {
        synchronizeNoteReminders(3, listOf(reminder), calendar, links)
        calendar.failDelete = true
        assertTrue(runCatching { synchronizeNoteReminders(3, emptyList(), calendar, links) }.isFailure)
        assertEquals(1, links.getAll().size)
        calendar.failDelete = false
        synchronizeNoteReminders(3, emptyList(), calendar, links)
        assertTrue(links.getAll().isEmpty())
    }

    @Test fun `protected reminder omits content and personal title and completed reminders are omitted`() {
        val note = PlainNoteEntity("private", "owner", "Sensitive title", "Sensitive content",
            reminderAt = reminder.at, reminderRepeat = "monthly", isLocked = true)
        val event = checkNotNull(note.calendarReminder(ZoneId.of("Africa/Harare")))
        assertEquals("Private note reminder", event.title)
        assertEquals("FREQ=MONTHLY", event.repeatRule)
        assertFalse(event.toString().contains(note.content))
        assertFalse(event.toString().contains(note.title))
        assertNull(note.copy(isReminderDone = true).calendarReminder(ZoneOffset.UTC))
        assertNull(note.copy(reminderAt = null).calendarReminder(ZoneOffset.UTC))
        assertNotEquals(event.fingerprint, note.copy(isLocked = false).calendarReminder(ZoneOffset.UTC)?.fingerprint)
    }

    @Test fun `all day events retain UTC calendar date across device timezones and exclusive midnight end`() {
        val start = LocalDate.of(2026, 11, 16).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val event = DeviceCalendarEvent(1, "Holiday", start, start + 86_400_000L, true)
        for (zone in listOf(ZoneId.of("America/Los_Angeles"), ZoneId.of("Pacific/Auckland"))) {
            assertTrue(event.occursOn(LocalDate.of(2026, 11, 16), zone))
            assertFalse(event.occursOn(LocalDate.of(2026, 11, 15), zone))
            assertFalse(event.occursOn(LocalDate.of(2026, 11, 17), zone))
        }
    }

    private class FakeLinks : CalendarReminderLinkDao {
        val values = linkedMapOf<String, CalendarReminderLinkEntity>()
        var failNextWrite = false
        override suspend fun getAll() = values.values.toList()
        override suspend fun upsert(link: CalendarReminderLinkEntity) {
            if (failNextWrite) { failNextWrite = false; error("mapping unavailable") }
            values[link.noteId] = link
        }
        override suspend fun delete(noteId: String) { values.remove(noteId) }
    }

    private class FakeCalendar : CalendarReminderStore {
        data class Event(val calendar: Long, val reminder: CalendarReminder)
        val events = linkedMapOf<Long, Event>()
        var saves = 0
        var nextId = 100L
        var failDelete = false
        override fun ownedEvent(eventId: Long, calendarId: Long, noteId: String) =
            events[eventId]?.let { it.calendar == calendarId && it.reminder.noteId == noteId } == true
        override fun findEvent(calendarId: Long, noteId: String) =
            events.entries.find { it.value.calendar == calendarId && it.value.reminder.noteId == noteId }?.key
        override fun save(calendarId: Long, reminder: CalendarReminder, existingId: Long?): Long {
            saves++
            val id = existingId ?: nextId++
            events[id] = Event(calendarId, reminder)
            return id
        }
        override fun delete(eventId: Long, calendarId: Long, noteId: String) {
            check(!failDelete) { "calendar unavailable" }
            if (ownedEvent(eventId, calendarId, noteId)) events.remove(eventId)
        }
    }
}
