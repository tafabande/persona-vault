package com.pims.vault.presentation.notes

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.pims.vault.core.reminder.NoteReminderScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteReminderDialog(
    initialReminderAt: Long? = null,
    initialTag: String? = null,
    initialRepeat: String? = null,
    noteTitle: String = "Note Reminder",
    onDismissRequest: () -> Unit,
    onSetReminder: (triggerAt: Long, tag: String?, repeat: String?) -> Unit,
    onClearReminder: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    // Default to tomorrow 9:00 AM if no initial reminder
    val defaultCal = remember(initialReminderAt) {
        Calendar.getInstance().apply {
            if (initialReminderAt != null && initialReminderAt > System.currentTimeMillis()) {
                timeInMillis = initialReminderAt
            } else {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
    }

    var selectedYear by remember { mutableIntStateOf(defaultCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(defaultCal.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(defaultCal.get(Calendar.DAY_OF_MONTH)) }
    var selectedHour by remember { mutableIntStateOf(defaultCal.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(defaultCal.get(Calendar.MINUTE)) }

    val presetTags = listOf(
        "Personal", "Work", "Urgent", "Health", "Follow Up", "Ideas", "Finance", "Family", "Shopping", "Meeting"
    )
    val isInitialPreset = initialTag == null || presetTags.contains(initialTag)

    var selectedTag by remember { mutableStateOf(if (isInitialPreset) (initialTag ?: "Personal") else "") }
    var customTagInput by remember { mutableStateOf(if (!isInitialPreset) (initialTag ?: "") else "") }
    var isCustomTagActive by remember { mutableStateOf(!isInitialPreset) }

    var selectedRepeat by remember { mutableStateOf(initialRepeat ?: "NONE") }

    val repeatOptions = listOf(
        "NONE" to "Does not repeat",
        "DAILY" to "Daily",
        "WEEKLY" to "Weekly",
        "MONTHLY" to "Monthly"
    )

    // Compute actual target timestamp
    val targetMillis = remember(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, selectedDay)
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val isFuture = targetMillis > System.currentTimeMillis()
    val formattedDate = remember(selectedYear, selectedMonth, selectedDay) {
        val cal = Calendar.getInstance().apply { set(selectedYear, selectedMonth, selectedDay) }
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(cal.time)
    }
    val formattedTime = remember(selectedHour, selectedMinute) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
        }
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        SimpleDateFormat(pattern, Locale.getDefault()).format(cal.time)
    }

    val finalEffectiveTag = if (isCustomTagActive && customTagInput.isNotBlank()) {
        customTagInput.trim()
    } else {
        selectedTag.ifBlank { "Personal" }
    }

    // Android Native DatePickerDialog launcher
    fun showDatePicker() {
        val dpd = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedYear = year
                selectedMonth = month
                selectedDay = dayOfMonth
            },
            selectedYear,
            selectedMonth,
            selectedDay
        )
        // Allow picking from today onwards
        dpd.datePicker.minDate = System.currentTimeMillis() - 1000L
        dpd.show()
    }

    // Android Native TimePickerDialog launcher
    fun showTimePicker() {
        val tpd = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute
            },
            selectedHour,
            selectedMinute,
            DateFormat.is24HourFormat(context)
        )
        tpd.show()
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with bell icon & close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Set Reminder",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Get notified with alarms & alerts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Notification permission banner if not granted
                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Notification permission needed for alarms",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            ) {
                                Text("Allow", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // =========================================================================
                // SECTION 1: CUSTOM DATE
                // =========================================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CUSTOM DATE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    TextButton(
                        onClick = { showDatePicker() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick Calendar Date", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Selected Date Card (Interactive: tap to open DatePicker)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap to choose exact calendar day",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Steppers
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val c = Calendar.getInstance().apply {
                                        set(selectedYear, selectedMonth, selectedDay)
                                        add(Calendar.DAY_OF_YEAR, -1)
                                    }
                                    selectedYear = c.get(Calendar.YEAR)
                                    selectedMonth = c.get(Calendar.MONTH)
                                    selectedDay = c.get(Calendar.DAY_OF_MONTH)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("-1d", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val c = Calendar.getInstance().apply {
                                        set(selectedYear, selectedMonth, selectedDay)
                                        add(Calendar.DAY_OF_YEAR, 1)
                                    }
                                    selectedYear = c.get(Calendar.YEAR)
                                    selectedMonth = c.get(Calendar.MONTH)
                                    selectedDay = c.get(Calendar.DAY_OF_MONTH)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("+1d", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Quick Date Presets
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val todayCal = Calendar.getInstance()
                    val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                    val in2DaysCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }
                    val thisWeekendCal = Calendar.getInstance().apply {
                        val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                        val daysUntilSaturday = (Calendar.SATURDAY - dayOfWeek + 7) % 7
                        add(Calendar.DAY_OF_YEAR, if (daysUntilSaturday == 0) 7 else daysUntilSaturday)
                    }
                    val nextWeekCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }

                    listOf(
                        "Today" to todayCal,
                        "Tomorrow" to tomorrowCal,
                        "In 2 days" to in2DaysCal,
                        "This Weekend" to thisWeekendCal,
                        "Next week" to nextWeekCal
                    ).forEach { (label, cal) ->
                        val isSelected = selectedYear == cal.get(Calendar.YEAR) &&
                                selectedMonth == cal.get(Calendar.MONTH) &&
                                selectedDay == cal.get(Calendar.DAY_OF_MONTH)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedYear = cal.get(Calendar.YEAR)
                                selectedMonth = cal.get(Calendar.MONTH)
                                selectedDay = cal.get(Calendar.DAY_OF_MONTH)
                            },
                            label = label
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // =========================================================================
                // SECTION 2: CUSTOM TIME
                // =========================================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CUSTOM TIME",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    TextButton(
                        onClick = { showTimePicker() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick Exact Time", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Selected Time Card (Interactive: tap to open TimePicker)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = formattedTime,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Tap to choose exact hour & minute",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Steppers
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = {
                                    selectedMinute = (selectedMinute + 15) % 60
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("+15m", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    selectedHour = (selectedHour + 1) % 24
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("+1h", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Quick Time Presets (including 1 min and 5 min for testing alarms!)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val in1m = Calendar.getInstance().apply { add(Calendar.MINUTE, 1) }
                    val in5m = Calendar.getInstance().apply { add(Calendar.MINUTE, 5) }
                    val in15m = Calendar.getInstance().apply { add(Calendar.MINUTE, 15) }

                    FilterChip(
                        selected = false,
                        onClick = {
                            selectedYear = in1m.get(Calendar.YEAR)
                            selectedMonth = in1m.get(Calendar.MONTH)
                            selectedDay = in1m.get(Calendar.DAY_OF_MONTH)
                            selectedHour = in1m.get(Calendar.HOUR_OF_DAY)
                            selectedMinute = in1m.get(Calendar.MINUTE)
                        },
                        label = "In 1 min (Test)"
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            selectedYear = in5m.get(Calendar.YEAR)
                            selectedMonth = in5m.get(Calendar.MONTH)
                            selectedDay = in5m.get(Calendar.DAY_OF_MONTH)
                            selectedHour = in5m.get(Calendar.HOUR_OF_DAY)
                            selectedMinute = in5m.get(Calendar.MINUTE)
                        },
                        label = "In 5 min"
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            selectedYear = in15m.get(Calendar.YEAR)
                            selectedMonth = in15m.get(Calendar.MONTH)
                            selectedDay = in15m.get(Calendar.DAY_OF_MONTH)
                            selectedHour = in15m.get(Calendar.HOUR_OF_DAY)
                            selectedMinute = in15m.get(Calendar.MINUTE)
                        },
                        label = "In 15 min"
                    )

                    listOf(
                        Triple("Morning", 9, 0),
                        Triple("Afternoon", 13, 0),
                        Triple("Evening", 18, 0),
                        Triple("Night", 21, 0)
                    ).forEach { (label, h, m) ->
                        val isSelected = selectedHour == h && selectedMinute == m
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedHour = h
                                selectedMinute = m
                            },
                            label = "$label (${if (h > 12) h - 12 else if (h == 0) 12 else h}:${if (m < 10) "0$m" else m} ${if (h >= 12) "PM" else "AM"})"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // =========================================================================
                // SECTION 3: REMINDER TAG
                // =========================================================================
                Text(
                    text = "REMINDER TAG",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetTags.forEach { tag ->
                        val isSelected = !isCustomTagActive && selectedTag == tag
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                isCustomTagActive = false
                                selectedTag = tag
                            },
                            label = tag
                        )
                    }
                    FilterChip(
                        selected = isCustomTagActive,
                        onClick = {
                            isCustomTagActive = true
                        },
                        label = "+ Custom Tag"
                    )
                }

                if (isCustomTagActive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customTagInput,
                        onValueChange = { customTagInput = it },
                        placeholder = { Text("Enter custom tag (e.g. Travel, Taxes, Meeting)") },
                        trailingIcon = {
                            if (customTagInput.isNotEmpty()) {
                                IconButton(onClick = { customTagInput = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // =========================================================================
                // SECTION 4: REPEAT RULE
                // =========================================================================
                Text(
                    text = "REPEAT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeatOptions.forEach { (key, label) ->
                        val isSelected = selectedRepeat == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedRepeat = key },
                            label = label
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // =========================================================================
                // SECTION 5: LIVE PREVIEW & TEST NOTIFICATION BUTTON
                // =========================================================================
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isFuture) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    border = BorderStroke(
                        1.dp,
                        if (isFuture) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isFuture) Icons.Default.NotificationsActive else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (isFuture) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                val repeatText = if (selectedRepeat != "NONE") " • Repeats $selectedRepeat" else ""
                                Text(
                                    text = if (isFuture) "Will alert on: $formattedDate at $formattedTime"
                                    else "Selected time has already passed! Pick a future time.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFuture) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Tag: $finalEffectiveTag$repeatText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Test notification button
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    val scheduler = NoteReminderScheduler(context)
                                    val testId = "test_${System.currentTimeMillis()}"
                                    scheduler.triggerImmediateTestNotification(
                                        noteId = testId,
                                        title = "🔔 Reminder Test: $noteTitle",
                                        contentSnippet = "Alarm & notification test triggered successfully. Tag: [$finalEffectiveTag]",
                                        tag = finalEffectiveTag
                                    )
                                    Toast.makeText(context, "Test notification triggered! Check status bar.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🔔 Test Alarm & Notification Now", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // =========================================================================
                // SECTION 6: ACTIONS (REMOVE, CANCEL, SET)
                // =========================================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (onClearReminder != null && initialReminderAt != null) {
                        TextButton(
                            onClick = {
                                onClearReminder()
                                onDismissRequest()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismissRequest) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                onSetReminder(targetMillis, finalEffectiveTag, selectedRepeat)
                                onDismissRequest()
                            },
                            enabled = isFuture,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Set Reminder")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
