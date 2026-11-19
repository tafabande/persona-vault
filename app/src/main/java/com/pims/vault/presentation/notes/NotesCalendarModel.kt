package com.pims.vault.presentation.notes

import com.pims.vault.domain.model.PlainNote
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal fun Long.noteDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

internal data class NoteDayMarkers(val notes: Int = 0, val reminders: Int = 0, val events: Int = 0)

internal fun noteCalendarMarkers(notes: List<PlainNote>, zone: ZoneId): Map<LocalDate, NoteDayMarkers> {
    val markers = mutableMapOf<LocalDate, NoteDayMarkers>()
    notes.forEach { note ->
        val displayedDate = note.updatedAt.noteDate(zone)
        markers[displayedDate] = (markers[displayedDate] ?: NoteDayMarkers()).let { it.copy(notes = it.notes + 1) }
        note.reminderAt?.takeUnless { note.isReminderDone }?.let { time ->
            val due = time.noteDate(zone)
            markers[due] = (markers[due] ?: NoteDayMarkers()).let { it.copy(reminders = it.reminders + 1) }
        }
    }
    return markers
}

internal fun notesForCalendarDay(notes: List<PlainNote>, date: LocalDate, zone: ZoneId): List<PlainNote> =
    notes.filter { note ->
        note.updatedAt.noteDate(zone) == date ||
            (!note.isReminderDone && note.reminderAt?.noteDate(zone) == date)
    }.distinctBy { it.id }.sortedWith(compareByDescending<PlainNote> { it.isPinned }.thenByDescending { it.updatedAt })

internal fun notesOverview(notes: List<PlainNote>): List<PlainNote> {
    val pinned = notes.filter { it.isPinned }
    return pinned.ifEmpty { notes }.sortedWith(compareByDescending<PlainNote> { it.updatedAt }.thenBy { it.id }).take(4)
}

internal fun notesMonthDates(month: java.time.YearMonth, firstDay: java.time.DayOfWeek): List<LocalDate> {
    val leading = (month.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
    val count = ((leading + month.lengthOfMonth() + 6) / 7) * 7
    val start = month.atDay(1).minusDays(leading.toLong())
    return List(count) { start.plusDays(it.toLong()) }
}
