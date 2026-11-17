package com.pims.vault.core.calendar

import com.pims.vault.data.local.entity.PlainNoteEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.security.MessageDigest

data class WritableCalendar(val id: Long, val name: String, val account: String)

data class DeviceCalendarEvent(
    val id: Long, val title: String, val begin: Long, val end: Long, val allDay: Boolean
) {
    fun occursOn(date: LocalDate, zone: ZoneId): Boolean {
        val eventZone = if (allDay) ZoneOffset.UTC else zone
        val first = Instant.ofEpochMilli(begin).atZone(eventZone).toLocalDate()
        val last = Instant.ofEpochMilli((end - 1).coerceAtLeast(begin)).atZone(eventZone).toLocalDate()
        return date >= first && date <= last
    }
}

data class CalendarReminder(val noteId: String, val title: String, val at: Long, val repeatRule: String?, val zone: String) {
    val fingerprint: String get() = MessageDigest.getInstance("SHA-256")
        .digest(listOf(noteId, title, at.toString(), repeatRule.orEmpty(), zone).joinToString("\u0000").toByteArray())
        .joinToString("") { "%02x".format(it) }
}

interface CalendarReminderStore {
    fun ownedEvent(eventId: Long, calendarId: Long, noteId: String): Boolean
    fun findEvent(calendarId: Long, noteId: String): Long?
    fun save(calendarId: Long, reminder: CalendarReminder, existingId: Long?): Long
    fun delete(eventId: Long, calendarId: Long, noteId: String)
}

internal fun PlainNoteEntity.calendarReminder(zone: ZoneId): CalendarReminder? {
    val time = reminderAt ?: return null
    if (isReminderDone) return null
    return CalendarReminder(id, if (isLocked) "Private note reminder" else title.ifBlank { "Note reminder" }, time,
        when (reminderRepeat?.uppercase()) {
            "DAILY" -> "FREQ=DAILY"
            "WEEKLY" -> "FREQ=WEEKLY"
            "MONTHLY" -> "FREQ=MONTHLY"
            "YEARLY" -> "FREQ=YEARLY"
            else -> null
        }, zone.id)
}

data class DeviceCalendarState(
    val hasPermission: Boolean = false,
    val calendars: List<WritableCalendar> = emptyList(),
    val selectedCalendarId: Long? = null,
    val events: List<DeviceCalendarEvent> = emptyList(),
    val message: String? = null,
    val isRefreshing: Boolean = false
)
