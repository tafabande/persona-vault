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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
    viewModel: PlainNotesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dayFormat = remember { SimpleDateFormat("EEE d MMM", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val haptics = rememberPimsHaptics()
    val focusManager = LocalFocusManager.current

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
                note.title.contains(searchQuery, ignoreCase = true) ||
                        note.content.contains(searchQuery, ignoreCase = true) ||
                        (note.reminderTag?.contains(searchQuery, ignoreCase = true) == true)
            }
            matchesFilter && matchesSearch
        }
    }

    // Calculate start & end of current week
    val (weekStartMillis, weekEndMillis) = remember {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val end = start + (7 * 86400000L)
        start to end
    }

    // 7 days of the current week
    val weekDays = remember(filteredNotes, weekStartMillis) {
        val todayCal = Calendar.getInstance()
        val currentYear = todayCal.get(Calendar.YEAR)
        val currentDayOfYear = todayCal.get(Calendar.DAY_OF_YEAR)

        val cal = Calendar.getInstance().apply {
            timeInMillis = weekStartMillis
        }

        (0..6).map { dayOffset ->
            val dayCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
            val label = dayFormat.format(dayCal.time)
            val isToday = dayCal.get(Calendar.YEAR) == currentYear && dayCal.get(Calendar.DAY_OF_YEAR) == currentDayOfYear
            val dayStart = dayCal.timeInMillis
            val dayEnd = dayStart + 86400000L

            val dayNotes = filteredNotes.filter { note ->
                note.updatedAt in dayStart until dayEnd
            }
            DaySection(label, isToday, dayNotes)
        }
    }

    // Notes earlier than this week but within current month
    val monthStartMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val earlierThisMonthNotes = remember(filteredNotes, weekStartMillis, monthStartMillis) {
        filteredNotes.filter { note ->
            note.updatedAt in monthStartMillis until weekStartMillis
        }
    }

    // Notes prior to current month
    val olderNotes = remember(filteredNotes, monthStartMillis) {
        filteredNotes.filter { note ->
            note.updatedAt < monthStartMillis
        }
    }

    val initialExpandedDay = remember(weekDays) {
        weekDays.firstOrNull { it.isToday }?.dayLabel
            ?: weekDays.firstOrNull { it.notes.isNotEmpty() }?.dayLabel
            ?: weekDays.firstOrNull()?.dayLabel
    }

    var expandedDayLabels by remember(weekDays) {
        mutableStateOf(setOfNotNull(initialExpandedDay))
    }
    var earlierMonthExpanded by remember { mutableStateOf(true) }
    var olderExpanded by remember { mutableStateOf(false) }

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

                // Right: Pill Badge showing total notes count
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.clickable { onOpenEditor(null) }
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
                        Icon(
                            imageVector = PersonaIcons.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
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
                                onClick = { onOpenEditor(note.id) },
                                onDelete = { noteToDelete = note },
                                viewModel = viewModel
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                thickness = 0.5.dp
                            )
                        }
                    }
                } else {
                    // Weekly structured timeline
                    weekDays.forEach { daySection ->
                        val isExpanded = daySection.dayLabel in expandedDayLabels

                        item(key = "header_${daySection.dayLabel}") {
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
                                    .padding(vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = daySection.dayLabel,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                if (isExpanded) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Collapse",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "${daySection.displayCount}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Icon(
                                            imageVector = PersonaIcons.ChevronRight,
                                            contentDescription = "Expand",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 0.5.dp
                            )
                        }

                        if (isExpanded) {
                            if (daySection.notes.isNotEmpty()) {
                                items(daySection.notes, key = { it.id }) { note ->
                                    NoteListRow(
                                        note = note,
                                        timeStr = timeFormat.format(Date(note.updatedAt)),
                                        onClick = { onOpenEditor(note.id) },
                                        onDelete = { noteToDelete = note },
                                        viewModel = viewModel
                                    )
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
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

                    // Earlier This Month Section
                    if (earlierThisMonthNotes.isNotEmpty()) {
                        item(key = "header_earlier_month") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tactilePress(targetScale = 0.98f) {
                                        earlierMonthExpanded = !earlierMonthExpanded
                                    }
                                    .padding(vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Earlier This Month",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${earlierThisMonthNotes.size}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        imageVector = if (earlierMonthExpanded) Icons.Default.KeyboardArrowDown else PersonaIcons.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 0.5.dp
                            )
                        }

                        if (earlierMonthExpanded) {
                            items(earlierThisMonthNotes, key = { it.id }) { note ->
                                NoteListRow(
                                    note = note,
                                    timeStr = timeFormat.format(Date(note.updatedAt)),
                                    onClick = { onOpenEditor(note.id) },
                                    onDelete = { noteToDelete = note },
                                    viewModel = viewModel
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }

                    // Older Notes Section
                    if (olderNotes.isNotEmpty()) {
                        item(key = "header_older_notes") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tactilePress(targetScale = 0.98f) {
                                        olderExpanded = !olderExpanded
                                    }
                                    .padding(vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Older Notes",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${olderNotes.size}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        imageVector = if (olderExpanded) Icons.Default.KeyboardArrowDown else PersonaIcons.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 0.5.dp
                            )
                        }

                        if (olderExpanded) {
                            items(olderNotes, key = { it.id }) { note ->
                                NoteListRow(
                                    note = note,
                                    timeStr = timeFormat.format(Date(note.updatedAt)),
                                    onClick = { onOpenEditor(note.id) },
                                    onDelete = { noteToDelete = note },
                                    viewModel = viewModel
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    thickness = 0.5.dp
                                )
                            }
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
    viewModel: PlainNotesViewModel? = null
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
        // Leading accent strip
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(34.dp)
                .background(
                    if (note.isReminderDone) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(2.dp)
                )
        )

        // Thumbnail if photo attached
        thumbnailBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = note.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val subtitleParts = buildList {
                add(timeStr)
                if (note.content.isNotBlank()) {
                    add(note.content.replace('\n', ' ').trim().take(60))
                }
            }
            Text(
                text = subtitleParts.joinToString("  "),
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
        }

        // Overflow Actions Menu (Copy, Duplicate, Share, Delete)
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
                    Text(
                        text = note.title.ifBlank { "Untitled" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
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
                    text = note.content,
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
