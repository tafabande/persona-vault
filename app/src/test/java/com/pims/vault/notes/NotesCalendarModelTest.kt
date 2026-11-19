package com.pims.vault.notes

import com.pims.vault.domain.model.PlainNote
import com.pims.vault.presentation.notes.*
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class NotesCalendarModelTest {
    private val zone = ZoneId.of("America/New_York")
    private fun time(date: String, hour: Int = 12) = LocalDate.parse(date).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
    private fun note(id: String, created: Long = time("2026-11-01"), updated: Long = created, pinned: Boolean = false,
        due: Long? = null, done: Boolean = false) = PlainNote(id, "owner", id, "", createdAt = created,
        updatedAt = updated, isPinned = pinned, reminderAt = due, isReminderDone = done)

    @Test fun `overview returns four latest without dropping full source list`() {
        val notes = (1..8).map { note("$it", updated = it.toLong()) }
        assertEquals(listOf("8", "7", "6", "5"), notesOverview(notes).map { it.id })
        assertEquals(8, notes.size)
    }

    @Test fun `pinned overview excludes unpinned and limits to four`() {
        val notes = (1..6).map { note("$it", updated = it.toLong(), pinned = true) } + note("new", updated = 100)
        assertEquals(listOf("6", "5", "4", "3"), notesOverview(notes).map { it.id })
        assertEquals(listOf("one"), notesOverview(listOf(note("one", pinned = true), note("two"))).map { it.id })
    }

    @Test fun `displayed note and active reminder markers use separate dates`() {
        val markers = noteCalendarMarkers(listOf(note("a", due = time("2026-11-02")),
            note("b", due = time("2026-11-02"), done = true)), zone)
        assertEquals(NoteDayMarkers(2, 0), markers[LocalDate.parse("2026-11-01")])
        assertEquals(NoteDayMarkers(0, 1), markers[LocalDate.parse("2026-11-02")])
    }

    @Test fun `edited note date matches its row date and day selection`() {
        val note = note("edited", created = time("2026-11-01"), updated = time("2026-11-14"))
        val markers = noteCalendarMarkers(listOf(note), zone)
        assertNull(markers[LocalDate.parse("2026-11-01")])
        assertEquals(NoteDayMarkers(notes = 1), markers[LocalDate.parse("2026-11-14")])
        assertEquals(listOf("edited"), notesForCalendarDay(listOf(note), LocalDate.parse("2026-11-14"), zone).map { it.id })
    }

    @Test fun `every month and locale week start renders every date once in complete weeks`() {
        for (year in 2024..2028) for (monthNumber in 1..12) for (firstDay in DayOfWeek.values()) {
            val month = YearMonth.of(year, monthNumber)
            val dates = notesMonthDates(month, firstDay)
            assertEquals(firstDay, dates.first().dayOfWeek)
            assertEquals(0, dates.size % 7)
            assertEquals(dates.size, dates.distinct().size)
            assertEquals((1..month.lengthOfMonth()).toList(), dates.filter { YearMonth.from(it) == month }.map { it.dayOfMonth })
            assertTrue(dates.zipWithNext().all { (a, b) -> a.plusDays(1) == b })
        }
    }

    @Test fun `six week calendar uses real adjacent month dates instead of blank cells`() {
        val dates = notesMonthDates(YearMonth.of(2026, 8), DayOfWeek.MONDAY)
        assertEquals(42, dates.size)
        assertEquals(LocalDate.of(2026, 7, 27), dates.first())
        assertEquals(LocalDate.of(2026, 9, 6), dates.last())
        assertEquals(LocalDate.of(2026, 8, 31), dates[35])
    }

    @Test fun `day selection includes due notes once and omits completed reminders`() {
        val date = LocalDate.parse("2026-11-02")
        val notes = listOf(note("both", created = time("2026-11-02"), due = time("2026-11-02")),
            note("due", due = time("2026-11-02"), pinned = true),
            note("done", due = time("2026-11-02"), done = true))
        assertEquals(listOf("due", "both"), notesForCalendarDay(notes, date, zone).map { it.id })
    }

    @Test fun `fall daylight saving transition retains last hour of local day`() {
        val late = LocalDate.parse("2026-11-01").atTime(23, 30).atZone(zone).toInstant().toEpochMilli()
        val next = time("2026-11-02", 0)
        val notes = listOf(note("late", created = late), note("next", created = next))
        assertEquals(listOf("late"), notesForCalendarDay(notes, LocalDate.parse("2026-11-01"), zone).map { it.id })
    }
}
