package com.pims.vault.presentation.notes

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pims.vault.core.calendar.DeviceCalendarState

@Composable
internal fun DeviceCalendarSetup(state: DeviceCalendarState, onRefresh: () -> Unit, onSelect: (Long) -> Unit, onDisconnect: () -> Unit) {
    val context = LocalContext.current
    var showCalendars by rememberSaveable { mutableStateOf(false) }
    var wasDenied by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result[Manifest.permission.READ_CALENDAR] == true && result[Manifest.permission.WRITE_CALENDAR] == true
        wasDenied = !granted
        showCalendars = granted
        onRefresh()
    }
    val selected = state.calendars.find { it.id == state.selectedCalendarId }
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = {
                if (state.hasPermission) { showCalendars = true; onRefresh() }
                else permission.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
            }, modifier = Modifier.weight(1f)) {
                Text(if (selected != null) "Calendar: ${selected.name}" else "Connect device calendar")
            }
            if (state.hasPermission) TextButton(onClick = onRefresh, enabled = !state.isRefreshing) { Text("Refresh") }
        }
        state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        if (wasDenied && !state.hasPermission) TextButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }) { Text("Calendar permission settings") }
        if (state.isRefreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    if (showCalendars && state.hasPermission) AlertDialog(
        onDismissRequest = { showCalendars = false },
        title = { Text("Reminder calendar") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("All note reminders sync automatically to this calendar and its account. Protected reminders use a private title.")
                if (state.calendars.isEmpty()) Text("No writable calendars found. Add a calendar account in your device settings, then refresh.")
                state.calendars.forEach { calendar ->
                    TextButton(onClick = { onSelect(calendar.id); showCalendars = false }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(calendar.name)
                            Text(calendar.account, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onRefresh() }) { Text("Refresh calendars") } },
        dismissButton = {
            Row {
                if (state.selectedCalendarId != null) TextButton(onClick = { onDisconnect(); showCalendars = false }) { Text("Disconnect") }
                TextButton(onClick = { showCalendars = false }) { Text("Close") }
            }
        }
    )
}
