package com.pims.vault.core.calendar

import android.content.Context
import com.pims.vault.data.local.dao.CalendarReminderLinkDao
import com.pims.vault.data.local.dao.PlainNoteDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteCalendarCoordinator @Inject constructor(
    @ApplicationContext context: Context,
    private val calendar: AndroidDeviceCalendar,
    private val notes: PlainNoteDao,
    private val links: CalendarReminderLinkDao
) {
    private val preferences = context.getSharedPreferences("device_calendar", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var month = YearMonth.now()
    private val providerChanges = Channel<Unit>(Channel.CONFLATED)
    private var retry: Job? = null
    private val _state = MutableStateFlow(DeviceCalendarState())
    val state = _state.asStateFlow()

    init {
        // Local saves, remote restores, completion and deletion all invalidate this Room query.
        scope.launch {
            notes.observeActiveReminders().retryWhen { _, _ ->
                delay(30_000)
                true
            }.collect { refreshNow() }
        }
        scope.launch {
            for (change in providerChanges) {
                delay(350)
                refreshNow()
            }
        }
    }

    fun refresh(month: YearMonth = this.month) {
        this.month = month
        scope.launch { refreshNow() }
    }

    fun selectCalendar(id: Long) {
        scope.launch {
            mutex.withLock {
                try {
                    check(calendar.hasPermission()) { "Allow calendar access to connect your reminders" }
                    require(calendar.calendars().any { it.id == id }) { "Choose an available writable calendar" }
                    check(preferences.edit().putLong("selected_calendar", id).commit()) { "Could not save calendar selection" }
                } catch (error: CancellationException) { throw error
                } catch (_: Exception) {
                    _state.update { it.copy(message = "Could not connect this calendar. Check access and retry.") }
                    return@launch
                }
            }
            refreshNow()
        }
    }

    fun disconnect() {
        scope.launch {
            mutex.withLock {
                try {
                    // Stop creating events before cleanup so a failed deletion cannot recreate old reminders.
                    check(preferences.edit().remove("selected_calendar").putBoolean("cleanup_pending", true).commit())
                    _state.update { it.copy(selectedCalendarId = null, message = null) }
                } catch (error: CancellationException) { throw error
                } catch (_: Exception) {
                    _state.update { it.copy(message = "Could not disconnect this calendar. Please retry.") }
                    return@launch
                }
            }
            refreshNow()
        }
    }

    private suspend fun refreshNow(): Unit = mutex.withLock {
        val selected = preferences.getLong("selected_calendar", -1L).takeIf { it >= 0 }
        if (!calendar.hasPermission()) {
            _state.value = DeviceCalendarState(selectedCalendarId = selected,
                message = when {
                    preferences.getBoolean("cleanup_pending", false) -> "Allow calendar access to remove previously synced reminders."
                    selected != null -> "Calendar access was removed. Allow access to resume syncing reminders."
                    else -> null
                })
            return@withLock
        }
        _state.update { it.copy(isRefreshing = true, hasPermission = true, selectedCalendarId = selected) }
        try {
            calendar.observeChanges { providerChanges.trySend(Unit) }
            val calendars = calendar.calendars()
            var message: String? = null
            if (selected != null && calendars.any { it.id == selected }) {
                val reminders = notes.getActiveReminders().mapNotNull { it.calendarReminder(ZoneId.systemDefault()) }
                synchronizeNoteReminders(selected, reminders, calendar, links)
            } else if (selected == null) {
                links.getAll().forEach { link ->
                    calendar.delete(link.eventId, link.calendarId, link.noteId)
                    links.delete(link.noteId)
                }
            } else message = "Your selected calendar is unavailable. Choose another calendar."
            if (preferences.getBoolean("cleanup_pending", false)) {
                check(preferences.edit().remove("cleanup_pending").commit())
            }
            val zone = ZoneId.systemDefault()
            val start = month.atDay(1).minusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = month.plusMonths(1).atDay(1).plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli()
            _state.value = DeviceCalendarState(true, calendars, selected, calendar.events(start, end), message)
        } catch (error: CancellationException) { throw error
        } catch (_: SecurityException) {
            _state.value = DeviceCalendarState(selectedCalendarId = selected, message = "Allow calendar access to view events and sync reminders.")
        } catch (_: Exception) {
            _state.update { it.copy(events = emptyList(), isRefreshing = false,
                message = "Calendar sync is pending. Your notes are saved; retry when the calendar is available.") }
            if (retry?.isActive != true) {
                retry = scope.launch {
                    delay(30_000)
                    retry = null
                    refreshNow()
                }
            }
        }
    }
}
