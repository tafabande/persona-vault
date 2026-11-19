package com.pims.vault.core.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Calendar provider access is always called on the coordinator's IO dispatcher. */
@Singleton
class AndroidDeviceCalendar @Inject constructor(@ApplicationContext private val context: Context) : CalendarReminderStore {
    private val resolver get() = context.contentResolver
    private var observer: ContentObserver? = null
    fun hasPermission(): Boolean = listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        .all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    fun observeChanges(onChange: () -> Unit) {
        if (observer != null || !hasPermission()) return
        val value = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { onChange() }
        }
        resolver.registerContentObserver(CalendarContract.CONTENT_URI, true, value)
        observer = value
    }

    fun calendars(): List<WritableCalendar> = buildList {
        resolver.query(CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.ACCOUNT_NAME),
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ? AND ${CalendarContract.Calendars.SYNC_EVENTS} = 1",
            arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()), CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)?.use { cursor ->
            while (cursor.moveToNext()) add(WritableCalendar(cursor.getLong(0), cursor.getString(1).orEmpty(), cursor.getString(2).orEmpty()))
        }
    }

    private fun marker(noteId: String): String = "pims://note-reminder/${Uri.encode(noteId)}"

    /** Validate ownership even for restored Room event IDs; never alter an unrelated device event. */
    override fun ownedEvent(eventId: Long, calendarId: Long, noteId: String): Boolean = resolver.query(
        ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId), arrayOf(CalendarContract.Events._ID),
        "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.CUSTOM_APP_PACKAGE} = ? AND ${CalendarContract.Events.CUSTOM_APP_URI} = ? AND ${CalendarContract.Events.DELETED} = 0",
        arrayOf(calendarId.toString(), context.packageName, marker(noteId)), null
    )?.use { it.moveToFirst() } ?: false

    override fun findEvent(calendarId: Long, noteId: String): Long? = resolver.query(
        CalendarContract.Events.CONTENT_URI, arrayOf(CalendarContract.Events._ID),
        "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.CUSTOM_APP_PACKAGE} = ? AND ${CalendarContract.Events.CUSTOM_APP_URI} = ? AND ${CalendarContract.Events.DELETED} = 0",
        arrayOf(calendarId.toString(), context.packageName, marker(noteId)), null
    )?.use { if (it.moveToFirst()) it.getLong(0) else null }

    override fun save(calendarId: Long, reminder: CalendarReminder, existingId: Long?): Long {
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, reminder.title)
            put(CalendarContract.Events.DESCRIPTION, "Reminder from Persona Vault")
            put(CalendarContract.Events.DTSTART, reminder.at)
            put(CalendarContract.Events.EVENT_TIMEZONE, reminder.zone)
            put(CalendarContract.Events.ALL_DAY, 0)
            put(CalendarContract.Events.HAS_ALARM, 1)
            put(CalendarContract.Events.CUSTOM_APP_PACKAGE, context.packageName)
            put(CalendarContract.Events.CUSTOM_APP_URI, marker(reminder.noteId))
            if (reminder.repeatRule == null) {
                put(CalendarContract.Events.DTEND, reminder.at + 30 * 60 * 1000L)
                putNull(CalendarContract.Events.DURATION)
                putNull(CalendarContract.Events.RRULE)
            } else {
                putNull(CalendarContract.Events.DTEND)
                put(CalendarContract.Events.DURATION, "PT30M")
                put(CalendarContract.Events.RRULE, reminder.repeatRule)
            }
        }
        val id = if (existingId != null && ownedEvent(existingId, calendarId, reminder.noteId)) {
            check(resolver.update(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingId), values,
                "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.CUSTOM_APP_PACKAGE} = ? AND ${CalendarContract.Events.CUSTOM_APP_URI} = ?",
                arrayOf(calendarId.toString(), context.packageName, marker(reminder.noteId))) == 1)
            existingId
        } else ContentUris.parseId(checkNotNull(resolver.insert(CalendarContract.Events.CONTENT_URI, values)))
        val hasAlarm = resolver.query(CalendarContract.Reminders.CONTENT_URI, arrayOf(CalendarContract.Reminders._ID),
            "${CalendarContract.Reminders.EVENT_ID} = ?", arrayOf(id.toString()), null)?.use { it.moveToFirst() } ?: false
        if (!hasAlarm) checkNotNull(resolver.insert(CalendarContract.Reminders.CONTENT_URI, ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, id)
            put(CalendarContract.Reminders.MINUTES, 0)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }))
        return id
    }

    override fun delete(eventId: Long, calendarId: Long, noteId: String) {
        if (ownedEvent(eventId, calendarId, noteId)) resolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
            "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.CUSTOM_APP_PACKAGE} = ? AND ${CalendarContract.Events.CUSTOM_APP_URI} = ?",
            arrayOf(calendarId.toString(), context.packageName, marker(noteId)))
    }

    fun events(start: Long, end: Long): List<DeviceCalendarEvent> = buildList {
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, start); ContentUris.appendId(it, end)
        }.build()
        resolver.query(uri, arrayOf(CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN, CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CUSTOM_APP_PACKAGE), "${CalendarContract.Instances.VISIBLE} = 1", null,
            "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
            while (cursor.moveToNext()) {
                // Note reminders already appear as note rows/markers.
                if (cursor.getString(5) != context.packageName) add(DeviceCalendarEvent(cursor.getLong(0),
                    cursor.getString(1).orEmpty().ifBlank { "Calendar event" }, cursor.getLong(2), cursor.getLong(3), cursor.getInt(4) != 0))
            }
        }
    }
}
