package com.pims.vault.presentation.notes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import com.pims.vault.core.storage.decodePhotoPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.pims.vault.presentation.ui.components.PersonaAction
import com.pims.vault.presentation.ui.components.PersonaActionStyle
import com.pims.vault.presentation.ui.components.PersonaConfirmationDialog
import com.pims.vault.presentation.ui.components.PersonaIconAction
import com.pims.vault.presentation.ui.components.PersonaMenu
import com.pims.vault.presentation.ui.components.PersonaMenuItem
import com.pims.vault.presentation.ui.theme.PersonaIcon
import com.pims.vault.presentation.ui.theme.PersonaIconBadge
import com.pims.vault.presentation.ui.theme.PersonaIconSize
import com.pims.vault.presentation.ui.theme.PimsControlSize
import com.pims.vault.presentation.ui.theme.PimsSpacing
import com.pims.vault.presentation.ui.components.PersonaTextInput
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.pims.vault.presentation.ui.theme.LocalPimsTonalSurfaces
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pims.vault.domain.model.PlainNote
import com.pims.vault.presentation.ui.components.PersonaFAB
import com.pims.vault.presentation.ui.theme.PersonaIcons
import com.pims.vault.presentation.ui.theme.PimsDimensions
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.presentation.ui.theme.pimsPageGutter
import com.pims.vault.presentation.ui.util.rememberPimsFeedback
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlainNotesScreen(
    onOpenEditor: (String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlainNotesViewModel = hiltViewModel(),
    onRequestAuth: ((title: String, subtitle: String, onSuccess: () -> Unit) -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deviceCalendar by viewModel.calendarState.collectAsStateWithLifecycle()
    val calendarContext = LocalContext.current
    var showCumulativeOverview by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<PlainNote?>(null) }
    var searchQuery by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var isSearchVisible by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(NoteFilter.ALL) }
    var monthValue by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(java.time.YearMonth.now().toString()) }
    var selectedValue by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val zone = java.time.ZoneId.systemDefault()
    val today = java.time.LocalDate.now(zone)
    val month = java.time.YearMonth.parse(monthValue)
    val selectedDate = selectedValue?.let(java.time.LocalDate::parse)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, month) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.refreshCalendar(month)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    androidx.compose.runtime.LaunchedEffect(month) { viewModel.refreshCalendar(month) }
    val markers = remember(state.notes, deviceCalendar.events, month, zone) {
        val result = noteCalendarMarkers(state.notes, zone).toMutableMap()
        notesMonthDates(month, java.time.temporal.WeekFields.of(Locale.getDefault()).firstDayOfWeek).forEach { date ->
            result[date] = (result[date] ?: NoteDayMarkers()).copy(events = deviceCalendar.events.count { it.occursOn(date, zone) })
        }
        result
    }
    val eventDate = selectedDate ?: today
    val dayEvents = deviceCalendar.events.filter { it.occursOn(eventDate, zone) }
    val isFiltering = searchQuery.isNotBlank() || activeFilter != NoteFilter.ALL
    val filtered = remember(state.notes, searchQuery, activeFilter) {
        state.notes.filter { note ->
            val match = when (activeFilter) {
                NoteFilter.ALL -> true
                NoteFilter.REMINDERS -> note.reminderAt != null && !note.isReminderDone
                NoteFilter.PHOTOS -> note.attachments.isNotEmpty()
                NoteFilter.DONE -> note.isReminderDone
            }
            val query = searchQuery.trim().removePrefix("#")
            match && (query.isBlank() || (!note.isLocked &&
                (note.title.contains(query, true) || note.content.contains(query, true) || note.tags.any { it.contains(query, true) })))
        }.sortedByDescending { it.updatedAt }
    }
    val displayed = when {
        isFiltering -> filtered
        selectedDate != null -> notesForCalendarDay(state.notes, selectedDate, zone)
        else -> notesOverview(state.notes)
    }
    val heading = when {
        isFiltering -> "${displayed.size} results"
        selectedDate != null -> selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
        state.notes.any { it.isPinned } -> "Pinned notes"
        else -> "Recent notes"
    }
    val handleNoteClick: (PlainNote) -> Unit = { note -> onOpenEditor(note.id) }
    val dateFormat = remember { SimpleDateFormat("d MMM · h:mm a", Locale.getDefault()) }
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.align(Alignment.TopCenter).widthIn(max = 720.dp).fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = pimsPageGutter(), end = pimsPageGutter(), bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "header") {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Notes", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    }
                    PersonaIconAction(
                        if (isSearchVisible) PersonaIcons.Close else PersonaIcons.Search,
                        if (isSearchVisible) "Close search" else "Search notes",
                        onClick = {
                            isSearchVisible = !isSearchVisible
                            if (!isSearchVisible) searchQuery = ""
                        }
                    )
                }
            }
            if (isSearchVisible) item(key = "search") {
                PersonaTextInput(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search notes or #tags",
                    leadingIcon = { Icon(PersonaIcons.Search, null) }
                )
            }
            item(key = "calendar") {
                NotesCalendar(month, selectedDate, today, markers,
                    onMonthChange = { monthValue = it.toString(); selectedValue = null },
                    onSelectDate = { selectedValue = if (selectedDate == it) null else it.toString(); monthValue = java.time.YearMonth.from(it).toString(); searchQuery = ""; activeFilter = NoteFilter.ALL },
                    onToday = { monthValue = java.time.YearMonth.from(today).toString(); selectedValue = today.toString(); searchQuery = ""; activeFilter = NoteFilter.ALL })
            }
            item(key = "calendar_setup") {
                DeviceCalendarSetup(deviceCalendar, { viewModel.refreshCalendar(month) }, viewModel::selectCalendar, viewModel::disconnectCalendar)
            }
            if (!isFiltering && deviceCalendar.hasPermission && dayEvents.isNotEmpty()) {
                item(key = "device_events_heading") {
                    Text(if (selectedDate == null) "Today's calendar" else "Device calendar",
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                }
                items(dayEvents, key = { "event_${it.id}_${it.begin}" }) { event ->
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth().clickable {
                            runCatching {
                                calendarContext.startActivity(Intent(Intent.ACTION_VIEW,
                                    android.content.ContentUris.withAppendedId(android.provider.CalendarContract.Events.CONTENT_URI, event.id)))
                            }.onFailure { Toast.makeText(calendarContext, "No calendar app is available", Toast.LENGTH_SHORT).show() }
                        }) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(event.title, style = MaterialTheme.typography.titleSmall)
                            Text(if (event.allDay) "All day" else java.time.Instant.ofEpochMilli(event.begin).atZone(zone)
                                .format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm", Locale.getDefault())),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item(key = "filters") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoteFilter.values().forEach { filter ->
                        FilterChip(selected = activeFilter == filter,
                            onClick = { activeFilter = filter }, label = { Text(filter.label) },
                            leadingIcon = { Icon(filter.icon, null, Modifier.size(16.dp)) })
                    }
                }
            }
            state.error?.let { message -> item(key = "error") {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
                        PersonaIconAction(PersonaIcons.Close, "Dismiss error", viewModel::clearError)
                    }
                }
            } }
            item(key = "section") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(heading, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                    PersonaAction(
                        label = if (selectedDate != null || isFiltering) "Overview" else "All notes",
                        style = PersonaActionStyle.Text,
                        onClick = {
                            if (selectedDate != null || isFiltering) {
                                selectedValue = null
                                activeFilter = NoteFilter.ALL
                                searchQuery = ""
                            } else {
                                showCumulativeOverview = true
                            }
                        }
                    )
                }
            }
            if (displayed.isEmpty()) item(key = "empty") {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (isFiltering) "No matching notes" else if (selectedDate != null) "No notes for this day" else "No notes yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        PersonaAction("Write a note", { onOpenEditor(null) }, style = PersonaActionStyle.Quiet)
                    }
                }
            }
            items(displayed, key = { it.id }) { note ->
                    Column {
                        NoteListRow(note, dateFormat.format(Date(note.updatedAt)), { handleNoteClick(note) },
                            { noteToDelete = note }, viewModel,
                            onTagClick = { searchQuery = "#$it"; isSearchVisible = true }, onRequestAuth = onRequestAuth)
                    }
            }
        }
        PersonaFAB(text = "New note", icon = PersonaIcons.Edit, onClick = { onOpenEditor(null) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp))
        noteToDelete?.let { note ->
            PersonaConfirmationDialog(
                onDismissRequest = { noteToDelete = null },
                title = "Delete note?",
                message = "This note and its attachments will be deleted.",
                confirmText = "Delete",
                isDestructive = true,
                onConfirm = {
                    viewModel.delete(note)
                    noteToDelete = null
                }
            )
        }
        CumulativeNotesOverviewSheet(notes = state.notes, isVisible = showCumulativeOverview,
            onDismiss = { showCumulativeOverview = false },
            onNoteClick = { showCumulativeOverview = false; handleNoteClick(it) },
            onNewNote = { showCumulativeOverview = false; onOpenEditor(null) },
            onOpenSearch = { showCumulativeOverview = false; isSearchVisible = true })
    }
}

private enum class TimelineFilter(val label: String) {
    ALL("All"),
    PROTECTED("Protected"),
    REMINDERS("Reminders"),
    PHOTOS("Photos")
}

@Composable
private fun TimelineFilterChip(
    label: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberPimsHaptics()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            width = if (isSelected) 1.dp else 0.5.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier.clickable {
            haptics.selection()
            onClick()
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                PersonaIcon(
                    imageVector = icon,
                    contentDescription = null,
                    size = PersonaIconSize.Micro,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactTimelineNoteRow(
    note: PlainNote,
    timeStr: String,
    onClick: () -> Unit
) {
    val haptics = rememberPimsHaptics()
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.light()
                onClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Minimalist status accent indicator
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(26.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                        if (note.isLocked) MaterialTheme.colorScheme.primary
                        else if (note.reminderAt != null) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
            )

            // Main Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Row 1: Title + Lock + Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = if (note.isLocked) "Protected note" else note.title.ifBlank { "Untitled" },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (note.isLocked) {
                            PersonaIcon(
                                imageVector = PersonaIcons.LockOutlined,
                                contentDescription = null,
                                size = PersonaIconSize.Micro,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Row 2: Preview snippet + Tags + Photos / Reminders badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val previewSnippet = if (note.isLocked) {
                        "Protected content"
                    } else {
                        note.content.replace('\n', ' ').trim().ifBlank { "No text content" }
                    }

                    Text(
                        text = previewSnippet,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Inline micro-badges
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        if (note.reminderAt != null) {
                            Icon(
                                imageVector = PersonaIcons.Alarm,
                                contentDescription = "Reminder",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        if (note.attachments.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = PersonaIcons.Photo,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                if (note.attachments.size > 1) {
                                    Text(
                                        text = "${note.attachments.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CumulativeNotesOverviewSheet(
    notes: List<PlainNote>,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onNoteClick: (PlainNote) -> Unit,
    onNewNote: () -> Unit = {},
    onOpenSearch: () -> Unit = {}
) {
    if (!isVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    var selectedFilter by remember { mutableStateOf(TimelineFilter.ALL) }
    var inSheetSearchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val totalNotesCount = notes.size
    val lockedNotesCount = remember(notes) { notes.count { it.isLocked } }
    val reminderNotesCount = remember(notes) { notes.count { it.reminderAt != null } }
    val photoNotesCount = remember(notes) { notes.count { it.attachments.isNotEmpty() } }

    val filteredNotes = remember(notes, selectedFilter, inSheetSearchQuery) {
        notes.filter { note ->
            val matchesFilter = when (selectedFilter) {
                TimelineFilter.ALL -> true
                TimelineFilter.PROTECTED -> note.isLocked
                TimelineFilter.REMINDERS -> note.reminderAt != null
                TimelineFilter.PHOTOS -> note.attachments.isNotEmpty()
            }
            val matchesSearch = if (inSheetSearchQuery.isBlank()) true else {
                val q = inSheetSearchQuery.trim()
                val tagQ = if (q.startsWith("#")) q.drop(1).trim() else q
                note.title.contains(q, ignoreCase = true) ||
                    note.content.contains(q, ignoreCase = true) ||
                    note.tags.any { it.contains(tagQ, ignoreCase = true) || it.contains(q, ignoreCase = true) }
            }
            matchesFilter && matchesSearch
        }
    }

    // Format clean relative / concise date labels
    val calendar = remember { Calendar.getInstance() }
    val currentYear = remember { calendar.get(Calendar.YEAR) }
    val currentDayOfYear = remember { calendar.get(Calendar.DAY_OF_YEAR) }

    fun formatGroupDate(timestamp: Long): String {
        val noteCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val isSameYear = noteCal.get(Calendar.YEAR) == currentYear
        val dayDiff = currentDayOfYear - noteCal.get(Calendar.DAY_OF_YEAR)

        return when {
            isSameYear && dayDiff == 0 -> "Today"
            isSameYear && dayDiff == 1 -> "Yesterday"
            isSameYear -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(timestamp))
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    // Group chronologically
    val groupedNotes = remember(filteredNotes) {
        filteredNotes.sortedByDescending { it.updatedAt }
            .groupBy { formatGroupDate(it.updatedAt) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(32.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            // Level 0: Top Header & Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Notes Overview",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "$totalNotesCount notes",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = PersonaIcons.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Level 1: Primary Action Buttons (Clear Call to Action)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onNewNote,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.4f)
                ) {
                    Icon(
                        imageVector = PersonaIcons.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "New Note",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) inSheetSearchQuery = ""
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isSearchActive) PersonaIcons.Close else PersonaIcons.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSearchActive) "Close" else "Search",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }

            // Quick in-sheet search bar
            AnimatedVisibility(visible = isSearchActive) {
                OutlinedTextField(
                    value = inSheetSearchQuery,
                    onValueChange = { inSheetSearchQuery = it },
                    placeholder = { Text("Filter timeline...", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )
            }

            // Level 2: Categorization & Status Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TimelineFilterChip(
                    label = "All",
                    count = totalNotesCount,
                    icon = null,
                    isSelected = selectedFilter == TimelineFilter.ALL,
                    onClick = { selectedFilter = TimelineFilter.ALL }
                )
                TimelineFilterChip(
                    label = "Protected",
                    count = lockedNotesCount,
                    icon = PersonaIcons.LockOutlined,
                    isSelected = selectedFilter == TimelineFilter.PROTECTED,
                    onClick = { selectedFilter = TimelineFilter.PROTECTED }
                )
                TimelineFilterChip(
                    label = "Reminders",
                    count = reminderNotesCount,
                    icon = PersonaIcons.Alarm,
                    isSelected = selectedFilter == TimelineFilter.REMINDERS,
                    onClick = { selectedFilter = TimelineFilter.REMINDERS }
                )
                TimelineFilterChip(
                    label = "Photos",
                    count = photoNotesCount,
                    icon = PersonaIcons.Photo,
                    isSelected = selectedFilter == TimelineFilter.PHOTOS,
                    onClick = { selectedFilter = TimelineFilter.PHOTOS }
                )
            }

            // Level 3: Section Title for Content Feed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Timeline Activity (${filteredNotes.size})",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                if (selectedFilter != TimelineFilter.ALL || inSheetSearchQuery.isNotBlank()) {
                    Text(
                        text = "Reset filter",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .clickable {
                                selectedFilter = TimelineFilter.ALL
                                inSheetSearchQuery = ""
                                isSearchActive = false
                            }
                            .padding(4.dp)
                    )
                }
            }

            // Level 4: Timeline Feed
            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = PersonaIcons.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = if (notes.isEmpty()) "No notes recorded yet" else "No matching notes found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (notes.isEmpty()) {
                            Button(
                                onClick = onNewNote,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Write your first note")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    groupedNotes.forEach { (dateLabel, dateNotes) ->
                        item(key = "timeline_date_$dateLabel") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = dateLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "· ${dateNotes.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )
                            }
                        }

                        items(dateNotes, key = { "timeline_note_${it.id}" }) { note ->
                            CompactTimelineNoteRow(
                                note = note,
                                timeStr = timeFormat.format(Date(note.updatedAt)),
                                onClick = { onNoteClick(note) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Enhanced NoteListRow with:
 * - Color accent strip
 * - Attachment thumbnail
 * - Direct tap reminder toggle (Check / Alarm)
 * - 3-dots overflow menu (Copy text, Duplicate, Share, Delete)
 */
@Composable
fun NoteListRow(
    note: PlainNote,
    timeStr: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    viewModel: PlainNotesViewModel? = null,
    onTagClick: ((String) -> Unit)? = null,
    onRequestAuth: ((title: String, subtitle: String, onSuccess: () -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current
    val haptics = rememberPimsHaptics()
    val firstAttachment = remember(note.attachments) { note.attachments.firstOrNull() }
    var thumbnailBitmap by remember(firstAttachment?.id) { mutableStateOf<Bitmap?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    val withUnlockedNote: (() -> Unit) -> Unit = { action ->
        if (note.isLocked) onRequestAuth?.invoke("Unlock protected note", "Authentication required for this action", action)
        else action()
    }
    LaunchedEffect(firstAttachment?.id, note.isLocked) {
        if (firstAttachment != null && viewModel != null && !note.isLocked) {
            val bytes = viewModel.readAttachment(firstAttachment)
            thumbnailBitmap = bytes?.let {
                try { withContext(Dispatchers.IO) { decodePhotoPreview(it, 256) } } finally { it.fill(0) }
            }
        } else {
            thumbnailBitmap = null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tactilePress(targetScale = 0.97f, onClick = onClick)
            .padding(vertical = PimsSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PimsDimensions.paddingMedium)
    ) {
        // Thumbnail if photo attached
        if (!note.isLocked) {
            thumbnailBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        } else {
            PersonaIconBadge(
                imageVector = PersonaIcons.LockOutlined,
                contentDescription = null,
                size = PimsControlSize.informationRow
            )
        }

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (note.isPinned) {
                    Icon(PersonaIcons.PushPin, "Pinned", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = if (note.isLocked) "Protected note" else note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            if (!note.isLocked && note.content.isNotBlank()) {
                Text(
                    note.content.replace('\n', ' ').trim(),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val metadata = buildList {
                add(timeStr)
                if (!note.isLocked && note.tags.isNotEmpty()) {
                    add(note.tags.take(2).joinToString(" ") { "#$it" })
                }
            }.joinToString(" · ")
            Text(
                metadata, modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )

            // Reminder status row with interactive toggle
            if (note.reminderAt != null && !note.isLocked) {
                val remSdf = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
                val remText = remSdf.format(Date(note.reminderAt))
                val isPast = note.reminderAt <= System.currentTimeMillis()
                val isDone = note.isReminderDone

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clickable {
                            haptics.selection()
                            viewModel?.toggleReminderDone(note)
                        }
                ) {
                    PersonaIcon(
                        imageVector = if (isDone) PersonaIcons.Check else PersonaIcons.Alarm,
                        contentDescription = if (isDone) "Mark reminder incomplete" else "Mark reminder done",
                        size = PersonaIconSize.Micro,
                        tint = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant
                            else if (isPast) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${if (isDone) "Done: " else ""}$remText${if (!note.reminderTag.isNullOrBlank()) " • ${note.reminderTag}" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant
                            else if (isPast) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }


        }

        Box {
            PersonaIconAction(
                icon = PersonaIcons.MoreActions,
                label = "Note actions",
                onClick = { showMenu = true }
            )
            PersonaMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                PersonaMenuItem(
                    label = if (note.isPinned) "Unpin note" else "Pin note",
                    icon = PersonaIcons.PushPin,
                    onClick = {
                        showMenu = false
                        viewModel?.togglePin(note)
                    }
                )
                PersonaMenuItem(
                    label = if (note.isLocked) "Unlock note" else "Lock note",
                    icon = if (note.isLocked) PersonaIcons.LockOpen else PersonaIcons.LockOutlined,
                    onClick = {
                        showMenu = false
                        haptics.light()
                        if (note.isLocked) {
                            onRequestAuth?.invoke("Unlock note", "Verify your identity to remove note protection") {
                                viewModel?.toggleLock(note)
                            }
                        } else {
                            viewModel?.toggleLock(note)
                        }
                    }
                )
                PersonaMenuItem(
                    label = "Copy text",
                    icon = PersonaIcons.ContentCopy,
                    onClick = {
                        showMenu = false
                        haptics.light()
                        withUnlockedNote {
                            val full = buildString {
                                if (note.title.isNotBlank()) appendLine(note.title)
                                if (note.content.isNotBlank()) append(note.content)
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Persona Note", full))
                            Toast.makeText(context, "Note copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                PersonaMenuItem(
                    label = "Duplicate",
                    icon = PersonaIcons.ContentPaste,
                    onClick = {
                        showMenu = false
                        haptics.light()
                        withUnlockedNote {
                            viewModel?.duplicateNote(note) {
                                Toast.makeText(context, "Note duplicated", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                PersonaMenuItem(
                    label = "Share",
                    icon = PersonaIcons.Share,
                    onClick = {
                        showMenu = false
                        haptics.light()
                        withUnlockedNote {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, note.title.ifBlank { "Note" })
                                putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share note"))
                        }
                    }
                )
                PersonaMenuItem(label = "Delete", icon = PersonaIcons.Delete, onClick = {
                    showMenu = false
                    onDelete()
                })
            }
        }
    }
}

/**
 * Legacy grid card — retained for backward compatibility.
 */
@Composable
fun NoteGridCard(
    note: PlainNote,
    dateStr: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    viewModel: PlainNotesViewModel? = null
) {
    val firstAttachment = remember(note.attachments) { note.attachments.firstOrNull() }
    var thumbnailBitmap by remember(firstAttachment?.id) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(firstAttachment?.id, note.isLocked) {
        if (firstAttachment != null && viewModel != null && !note.isLocked) {
            val bytes = viewModel.readAttachment(firstAttachment)
            thumbnailBitmap = bytes?.let {
                try { withContext(Dispatchers.IO) { decodePhotoPreview(it, 512) } } finally { it.fill(0) }
            }
        } else {
            thumbnailBitmap = null
        }
    }

    Card(
        shape = RoundedCornerShape(PimsDimensions.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .tactilePress(onClick = onClick)
    ) {
        Column {
            thumbnailBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(105.dp)
                )
            }
            Column(modifier = Modifier.padding(PimsDimensions.paddingMedium)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (note.isLocked) "Protected note" else note.title.ifBlank { "Untitled" },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (note.isLocked) {
                            PersonaIcon(
                                imageVector = PersonaIcons.LockOutlined,
                                contentDescription = null,
                                size = PersonaIconSize.Micro,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = PersonaIcons.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (note.isLocked) "Protected note" else note.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    if (note.attachments.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = PersonaIcons.AttachFile,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${note.attachments.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
