package com.pims.vault.presentation.notes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Schedule

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
    var showCumulativeOverview by remember { mutableStateOf(false) }

    val handleNoteClick: (PlainNote) -> Unit = { note ->
        if (note.isLocked && onRequestAuth != null) {
            onRequestAuth("Unlock Protected Note", "Authentication required to open protected note") {
                onOpenEditor(note.id)
            }
        } else {
            onOpenEditor(note.id)
        }
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dayFormat = remember { SimpleDateFormat("EEE d MMM", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val haptics = rememberPimsHaptics()
    val focusManager = LocalFocusManager.current
    val tonalSurfaces = LocalPimsTonalSurfaces.current

    var noteToDelete by remember { mutableStateOf<PlainNote?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(NoteFilter.ALL) }

    data class DaySection(
        val dayLabel: String,
        val isToday: Boolean = false,
        val notes: List<PlainNote>
    ) {
        val displayCount: Int get() = notes.size
    }

    val currentWeekLabel = remember {
        val c = Calendar.getInstance()
        "Week ${c.get(Calendar.WEEK_OF_YEAR)}"
    }

    // Filter notes based on active filter and search query
    val filteredNotes = remember(state.notes, activeFilter, searchQuery) {
        state.notes.filter { note ->
            val matchesFilter = when (activeFilter) {
                NoteFilter.ALL -> true
                NoteFilter.REMINDERS -> note.reminderAt != null && !note.isReminderDone
                NoteFilter.PHOTOS -> note.attachments.isNotEmpty()
                NoteFilter.DONE -> note.isReminderDone
            }
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim()
                val tagQ = if (q.startsWith("#")) q.drop(1).trim() else q
                note.title.contains(searchQuery, ignoreCase = true) ||
                    note.content.contains(searchQuery, ignoreCase = true) ||
                    (note.reminderTag?.contains(searchQuery, ignoreCase = true) == true) ||
                    (note.reminderTag?.contains(tagQ, ignoreCase = true) == true) ||
                    note.tags.any { it.contains(tagQ, ignoreCase = true) || it.contains(q, ignoreCase = true) }
            }
            matchesFilter && matchesSearch
        }
    }

    // Rolling 3-day window: Today (0), Yesterday (-1), Day before yesterday (-2)
    val recentDays = remember(filteredNotes) {
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        listOf(0, -1, -2).map { offset ->
            val dayCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            val dayStart = dayCal.timeInMillis
            val dayEnd = dayStart + 86400000L

            val label = when (offset) {
                0 -> "Today"
                -1 -> "Yesterday"
                else -> dayFormat.format(dayCal.time)
            }
            val isToday = (offset == 0)

            val dayNotes = filteredNotes.filter { note ->
                note.updatedAt in dayStart until dayEnd
            }
            DaySection(label, isToday, dayNotes)
        }
    }

    // Historical notes prior to the 3-day rolling window
    val twoDaysAgoStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -2)
        }.timeInMillis
    }

    val historicalNotes = remember(filteredNotes, twoDaysAgoStart) {
        filteredNotes.filter { note ->
            note.updatedAt < twoDaysAgoStart
        }
    }

    val initialExpandedDay = remember(recentDays) {
        recentDays.firstOrNull { it.isToday }?.dayLabel
            ?: recentDays.firstOrNull()?.dayLabel
    }

    var expandedDayLabels by remember(recentDays) {
        mutableStateOf(setOfNotNull(initialExpandedDay))
    }

    val isSearchOrFilterActive = searchQuery.isNotBlank() || activeFilter != NoteFilter.ALL

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Search Toggle
                IconButton(
                    onClick = {
                        haptics.light()
                        isSearchVisible = !isSearchVisible
                        if (!isSearchVisible) {
                            searchQuery = ""
                            focusManager.clearFocus()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isSearchVisible) Icons.Default.Close else PersonaIcons.Search,
                        contentDescription = if (isSearchVisible) "Close search" else "Search notes",
                        tint = if (isSearchVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Center Week Label
                Text(
                    text = if (isSearchVisible && searchQuery.isNotBlank()) "Search Results" else currentWeekLabel,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Right: Pill Badge showing total notes count (Tapping opens cumulative overview)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.clickable {
                        haptics.light()
                        showCumulativeOverview = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (state.notes.isEmpty()) "0 Notes" else "${state.notes.size} Notes",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // In-line Search Bar
            AnimatedVisibility(visible = isSearchVisible) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search notes by title, content, tag...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Filter Chips Bar (All, Reminders, Photos, Done)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NoteFilter.values().forEach { filter ->
                    val isSelected = activeFilter == filter
                    val filterCount = when (filter) {
                        NoteFilter.ALL -> state.notes.size
                        NoteFilter.REMINDERS -> state.notes.count { it.reminderAt != null && !it.isReminderDone }
                        NoteFilter.PHOTOS -> state.notes.count { it.attachments.isNotEmpty() }
                        NoteFilter.DONE -> state.notes.count { it.isReminderDone }
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptics.selection()
                            activeFilter = filter
                        },
                        label = {
                            Text("${filter.label} ($filterCount)")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = filter.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Main Notes Content
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 120.dp
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                if (isSearchOrFilterActive) {
                    // Filtered / Search flat results list
                    item(key = "search_header") {
                        Text(
                            text = "${filteredNotes.size} matching note${if (filteredNotes.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 0.5.dp
                        )
                    }

                    if (filteredNotes.isEmpty()) {
                        item(key = "search_empty") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No notes found",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Try adjusting your search terms or filter",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        items(filteredNotes, key = { it.id }) { note ->
                            NoteListRow(
                                note = note,
                                timeStr = timeFormat.format(Date(note.updatedAt)),
                                onClick = { handleNoteClick(note) },
                                onDelete = { noteToDelete = note },
                                viewModel = viewModel,
                                onTagClick = { tag ->
                                    searchQuery = "#$tag"
                                    isSearchVisible = true
                                }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                thickness = 0.5.dp
                            )
                        }
                    }
                } else {
                    // Rolling 3-day recent timeline
                    recentDays.forEach { daySection ->
                        val isExpanded = daySection.dayLabel in expandedDayLabels

                        item(key = "header_${daySection.dayLabel}") {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (daySection.isToday) tonalSurfaces.surfaceElevated else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .tactilePress(targetScale = 0.98f) {
                                            expandedDayLabels = if (isExpanded) {
                                                expandedDayLabels - daySection.dayLabel
                                            } else {
                                                expandedDayLabels + daySection.dayLabel
                                            }
                                        }
                                        .padding(horizontal = if (daySection.isToday) 12.dp else 4.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = daySection.dayLabel,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.5.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        if (daySection.isToday) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "Today",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (!isExpanded) {
                                        Text(
                                            text = "${daySection.displayCount}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (!daySection.isToday) {
                                HorizontalDivider(
                                    color = tonalSurfaces.borderSubtle,
                                    thickness = 0.5.dp
                                )
                            }
                        }

                        if (isExpanded) {
                            if (daySection.notes.isNotEmpty()) {
                                items(daySection.notes, key = { it.id }) { note ->
                                    NoteListRow(
                                        note = note,
                                        timeStr = timeFormat.format(Date(note.updatedAt)),
                                        onClick = { handleNoteClick(note) },
                                        onDelete = { noteToDelete = note },
                                        viewModel = viewModel,
                                        onTagClick = { tag ->
                                            searchQuery = "#$tag"
                                            isSearchVisible = true
                                        }
                                    )
                                    HorizontalDivider(
                                        color = tonalSurfaces.borderSubtle,
                                        thickness = 0.5.dp
                                    )
                                }
                            } else {
                                item(key = "empty_${daySection.dayLabel}") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenEditor(null) }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = PersonaIcons.Edit,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "No notes · Tap to write",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Historical notes displayed directly beneath recent days without gatekeeper
                    if (historicalNotes.isNotEmpty()) {
                        item(key = "header_historical_notes") {
                            Text(
                                text = "Previous Notes",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                            )
                            HorizontalDivider(
                                color = tonalSurfaces.borderSubtle,
                                thickness = 0.5.dp
                            )
                        }

                        items(historicalNotes, key = { it.id }) { note ->
                            NoteListRow(
                                note = note,
                                timeStr = dayFormat.format(Date(note.updatedAt)),
                                onClick = { handleNoteClick(note) },
                                onDelete = { noteToDelete = note },
                                viewModel = viewModel,
                                onTagClick = { tag ->
                                    searchQuery = "#$tag"
                                    isSearchVisible = true
                                },
                                onRequestAuth = onRequestAuth
                            )
                            HorizontalDivider(
                                color = tonalSurfaces.borderSubtle,
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }

        // FAB — New Note
        PersonaFAB(
            text = "New Note",
            icon = Icons.Default.Edit,
            onClick = { onOpenEditor(null) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
        )

        noteToDelete?.let { note ->
            AlertDialog(
                onDismissRequest = { noteToDelete = null },
                title = { Text("Delete Note") },
                text = { Text("Are you sure you want to delete '${note.title.ifBlank { "Untitled" }}'?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.delete(note)
                            noteToDelete = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { noteToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        CumulativeNotesOverviewSheet(
            notes = state.notes,
            isVisible = showCumulativeOverview,
            onDismiss = { showCumulativeOverview = false },
            onNoteClick = { note ->
                showCumulativeOverview = false
                handleNoteClick(note)
            },
            onNewNote = {
                showCumulativeOverview = false
                onOpenEditor(null)
            },
            onOpenSearch = {
                showCumulativeOverview = false
                isSearchVisible = true
            }
        )
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
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
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
                            text = note.title.ifBlank { "Untitled" },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (note.isLocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Protected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
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
                                imageVector = Icons.Default.Alarm,
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
                                    imageVector = Icons.Default.AddPhotoAlternate,
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
                        imageVector = Icons.Default.Add,
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
                        imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
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
                    icon = Icons.Default.Lock,
                    isSelected = selectedFilter == TimelineFilter.PROTECTED,
                    onClick = { selectedFilter = TimelineFilter.PROTECTED }
                )
                TimelineFilterChip(
                    label = "Reminders",
                    count = reminderNotesCount,
                    icon = Icons.Default.Alarm,
                    isSelected = selectedFilter == TimelineFilter.REMINDERS,
                    onClick = { selectedFilter = TimelineFilter.REMINDERS }
                )
                TimelineFilterChip(
                    label = "Photos",
                    count = photoNotesCount,
                    icon = Icons.Default.AddPhotoAlternate,
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

    LaunchedEffect(firstAttachment?.id) {
        if (firstAttachment != null && viewModel != null) {
            val bytes = viewModel.readAttachment(firstAttachment)
            thumbnailBitmap = if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
        } else {
            thumbnailBitmap = null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tactilePress(targetScale = 0.97f, onClick = onClick)
            .padding(vertical = 12.dp),
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
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Protected note",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (note.isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked Note",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            val subtitleParts = buildList {
                add(timeStr)
                if (!note.isLocked) {
                    val cleanContent = note.content
                        .replace('\n', ' ')
                        .trim()
                        .let { if (it == "•" || it == "-") "" else it }
                    if (cleanContent.isNotBlank()) {
                        add(cleanContent.take(60))
                    } else if (firstAttachment != null) {
                        add("Photo")
                    }
                }
            }
            Text(
                text = subtitleParts.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Reminder status row with interactive toggle
            if (note.reminderAt != null) {
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
                    Icon(
                        imageVector = if (isDone) Icons.Default.Check else Icons.Default.Alarm,
                        contentDescription = "Toggle reminder done",
                        tint = if (isDone) MaterialTheme.colorScheme.outline else if (isPast) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${if (isDone) "Done: " else ""}$remText${if (!note.reminderTag.isNullOrBlank()) " • ${note.reminderTag}" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDone) MaterialTheme.colorScheme.outline else if (isPast) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Category & Hashtags as clean single metadata line
            if (note.tags.isNotEmpty()) {
                Text(
                    text = note.tags.joinToString("  ") { "#$it" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Overflow Actions Menu (Copy, Duplicate, Share, Lock, Delete)
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Note actions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                // Lock / Unlock Note
                DropdownMenuItem(
                    text = { Text(if (note.isLocked) "Unlock Note" else "Lock Note") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (note.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        showMenu = false
                        haptics.light()
                        if (note.isLocked) {
                            if (onRequestAuth != null) {
                                onRequestAuth("Unlock Note", "Authentication required to remove note protection") {
                                    viewModel?.toggleLock(note)
                                    Toast.makeText(context, "Note unlocked", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                viewModel?.toggleLock(note)
                                Toast.makeText(context, "Note unlocked", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            viewModel?.toggleLock(note)
                            Toast.makeText(context, "Note locked", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                // Copy Note
                DropdownMenuItem(
                    text = { Text("Copy Text") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        showMenu = false
                        haptics.light()
                        val full = buildString {
                            if (note.title.isNotBlank()) appendLine(note.title)
                            if (note.content.isNotBlank()) append(note.content)
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Persona Note", full))
                        Toast.makeText(context, "Note copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )

                // Duplicate Note
                DropdownMenuItem(
                    text = { Text("Duplicate") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        showMenu = false
                        haptics.light()
                        viewModel?.duplicateNote(note) {
                            Toast.makeText(context, "Note duplicated", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                // Share Note
                DropdownMenuItem(
                    text = { Text("Share") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        showMenu = false
                        haptics.light()
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, note.title.ifBlank { "Note" })
                            putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Note"))
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                // Delete Note
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
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

    LaunchedEffect(firstAttachment?.id) {
        if (firstAttachment != null && viewModel != null) {
            val bytes = viewModel.readAttachment(firstAttachment)
            thumbnailBitmap = if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
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
                            text = note.title.ifBlank { "Untitled" },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (note.isLocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked Note",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (note.isLocked) "🔒 Protected note content" else note.content,
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
                                imageVector = Icons.Default.AttachFile,
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
