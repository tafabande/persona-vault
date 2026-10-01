package com.pims.vault.core.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pims.vault.R
import com.pims.vault.data.local.database.PimsDatabase
import com.pims.vault.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NoteReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "Received action: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            ACTION_RESCHEDULE_ALL -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    try {
                        val scheduler = NoteReminderScheduler(context)
                        scheduler.rescheduleAllActiveReminders()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_NOTE_REMINDER -> {
                val noteId = intent.getStringExtra(EXTRA_NOTE_ID) ?: return
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Note Reminder"
                val snippet = intent.getStringExtra(EXTRA_SNIPPET) ?: ""
                val tag = intent.getStringExtra(EXTRA_TAG)
                val repeat = intent.getStringExtra(EXTRA_REPEAT)
                val triggerAt = intent.getLongExtra(EXTRA_TRIGGER_AT, System.currentTimeMillis())

                showReminderNotification(context, noteId, title, snippet, tag)

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    try {
                        val db = PimsDatabase.buildDatabase(context)
                        if (repeat.isNullOrBlank() || repeat == "NONE") {
                            // Single-shot: mark done or keep for history
                        } else {
                            val nextTrigger = when (repeat) {
                                "DAILY" -> triggerAt + 24 * 60 * 60 * 1000L
                                "WEEKLY" -> triggerAt + 7 * 24 * 60 * 60 * 1000L
                                "MONTHLY" -> triggerAt + 30 * 24 * 60 * 60 * 1000L
                                else -> null
                            }
                            if (nextTrigger != null) {
                                db.plainNoteDao().updateReminder(noteId, nextTrigger, tag, repeat)
                                val scheduler = NoteReminderScheduler(context)
                                scheduler.scheduleReminder(noteId, title, snippet, tag, nextTrigger, repeat)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update repeating reminder: ${e.message}", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_SNOOZE_REMINDER -> {
                val noteId = intent.getStringExtra(EXTRA_NOTE_ID) ?: return
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Note Reminder"
                val snippet = intent.getStringExtra(EXTRA_SNIPPET) ?: ""
                val tag = intent.getStringExtra(EXTRA_TAG)

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(noteId.hashCode())

                val snoozeTime = System.currentTimeMillis() + 15 * 60 * 1000L
                val scheduler = NoteReminderScheduler(context)
                scheduler.scheduleReminder(noteId, title, snippet, tag, snoozeTime)

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    try {
                        val db = PimsDatabase.buildDatabase(context)
                        db.plainNoteDao().updateReminder(noteId, snoozeTime, tag, "NONE")
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_DISMISS_REMINDER -> {
                val noteId = intent.getStringExtra(EXTRA_NOTE_ID) ?: return
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(noteId.hashCode())

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    try {
                        val db = PimsDatabase.buildDatabase(context)
                        db.plainNoteDao().setReminderDone(noteId, true)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun showReminderNotification(
        context: Context,
        noteId: String,
        title: String,
        snippet: String,
        tag: String?
    ) {
        createNotificationChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_NOTE_ID, noteId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            noteId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NoteReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_REMINDER
            putExtra(EXTRA_NOTE_ID, noteId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_SNIPPET, snippet)
            putExtra(EXTRA_TAG, tag)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            noteId.hashCode() + 1,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, NoteReminderReceiver::class.java).apply {
            action = ACTION_DISMISS_REMINDER
            putExtra(EXTRA_NOTE_ID, noteId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            noteId.hashCode() + 2,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subtext = if (!tag.isNullOrBlank()) "[$tag]" else null
        val body = when {
            snippet.isNotBlank() -> snippet
            !tag.isNullOrBlank() -> "Reminder for your $tag note"
            else -> "Reminder: Tap to open note"
        }

        val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
        val vibrationPattern = longArrayOf(0, 350, 200, 350)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setSubText(subtext)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setVibrate(vibrationPattern)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Mark Done", donePendingIntent)
            .addAction(0, "Snooze 15m", snoozePendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(noteId.hashCode(), builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted: ${e.message}")
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val vibrationPattern = longArrayOf(0, 350, 200, 350)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Note Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders, alerts and scheduled tasks for your encrypted notes"
                enableVibration(true)
                this.vibrationPattern = vibrationPattern
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(defaultSoundUri, audioAttributes)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val TAG = "NoteReminderReceiver"
        const val CHANNEL_ID = "note_reminders_channel"

        const val ACTION_NOTE_REMINDER = "com.pims.vault.ACTION_NOTE_REMINDER"
        const val ACTION_SNOOZE_REMINDER = "com.pims.vault.ACTION_SNOOZE_REMINDER"
        const val ACTION_DISMISS_REMINDER = "com.pims.vault.ACTION_DISMISS_REMINDER"
        const val ACTION_RESCHEDULE_ALL = "com.pims.vault.ACTION_RESCHEDULE_ALL"

        const val EXTRA_NOTE_ID = "extra_note_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SNIPPET = "extra_snippet"
        const val EXTRA_TAG = "extra_tag"
        const val EXTRA_TRIGGER_AT = "extra_trigger_at"
        const val EXTRA_REPEAT = "extra_repeat"
        const val EXTRA_OPEN_NOTE_ID = "open_note_id"
    }
}
