package com.pims.vault.core.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.pims.vault.data.local.database.PimsDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun scheduleReminder(
        noteId: String,
        title: String,
        contentSnippet: String,
        tag: String?,
        triggerAtMillis: Long,
        repeat: String? = null
    ) {
        if (alarmManager == null || triggerAtMillis <= System.currentTimeMillis()) {
            return
        }

        val intent = Intent(context, NoteReminderReceiver::class.java).apply {
            action = NoteReminderReceiver.ACTION_NOTE_REMINDER
            putExtra(NoteReminderReceiver.EXTRA_NOTE_ID, noteId)
            putExtra(NoteReminderReceiver.EXTRA_TITLE, title)
            putExtra(NoteReminderReceiver.EXTRA_SNIPPET, contentSnippet)
            putExtra(NoteReminderReceiver.EXTRA_TAG, tag)
            putExtra(NoteReminderReceiver.EXTRA_TRIGGER_AT, triggerAtMillis)
            putExtra(NoteReminderReceiver.EXTRA_REPEAT, repeat)
        }

        val requestCode = noteId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    try {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    } catch (se: SecurityException) {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } catch (se: SecurityException) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d("NoteReminderScheduler", "Scheduled reminder for $noteId at $triggerAtMillis")
        } catch (e: Exception) {
            Log.e("NoteReminderScheduler", "Failed to schedule alarm: ${e.message}", e)
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } catch (_: Exception) {}
        }
    }

    fun triggerImmediateTestNotification(
        noteId: String,
        title: String,
        contentSnippet: String,
        tag: String?
    ) {
        val intent = Intent(context, NoteReminderReceiver::class.java).apply {
            action = NoteReminderReceiver.ACTION_NOTE_REMINDER
            putExtra(NoteReminderReceiver.EXTRA_NOTE_ID, noteId)
            putExtra(NoteReminderReceiver.EXTRA_TITLE, title.ifBlank { "Note Reminder" })
            putExtra(NoteReminderReceiver.EXTRA_SNIPPET, contentSnippet.ifBlank { "Test reminder notification" })
            putExtra(NoteReminderReceiver.EXTRA_TAG, tag ?: "Personal")
            putExtra(NoteReminderReceiver.EXTRA_TRIGGER_AT, System.currentTimeMillis())
            putExtra(NoteReminderReceiver.EXTRA_REPEAT, "NONE")
        }
        context.sendBroadcast(intent)
    }

    fun cancelReminder(noteId: String) {
        if (alarmManager == null) return
        val intent = Intent(context, NoteReminderReceiver::class.java).apply {
            action = NoteReminderReceiver.ACTION_NOTE_REMINDER
        }
        val requestCode = noteId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (_: Exception) {}
    }

    suspend fun rescheduleAllActiveReminders() = withContext(Dispatchers.IO) {
        try {
            val db = PimsDatabase.buildDatabase(context)
            val active = db.plainNoteDao().getActiveReminders()
            val now = System.currentTimeMillis()
            for (note in active) {
                val remAt = note.reminderAt ?: continue
                if (remAt > now) {
                    scheduleReminder(
                        noteId = note.id,
                        title = note.title,
                        contentSnippet = note.content.take(120),
                        tag = note.reminderTag,
                        triggerAtMillis = remAt,
                        repeat = note.reminderRepeat
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("NoteReminderScheduler", "Failed to reschedule reminders: ${e.message}", e)
        }
    }
}
