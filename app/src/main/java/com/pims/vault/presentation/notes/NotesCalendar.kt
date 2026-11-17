package com.pims.vault.presentation.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
internal fun NotesCalendar(
    month: YearMonth,
    selectedDate: LocalDate?,
    today: LocalDate,
    markers: Map<LocalDate, NoteDayMarkers>,
    onMonthChange: (YearMonth) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = Locale.getDefault()
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val dates = notesMonthDates(month, firstDay)
    val noteColor = com.pims.vault.presentation.ui.theme.CalendarNoteColor
    val reminderColor = com.pims.vault.presentation.ui.theme.CalendarReminderColor
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp),
        color = Color.Transparent) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = onToday) { Text("Today") }
                IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                    Icon(Icons.Default.ChevronLeft, "Previous month")
                }
                IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
                    Icon(Icons.Default.ChevronRight, "Next month")
                }
            }
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { offset ->
                    Box(Modifier.weight(1f).height(24.dp), contentAlignment = Alignment.Center) {
                        Text(firstDay.plus(offset.toLong()).getDisplayName(TextStyle.SHORT, locale),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            dates.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        val marks = markers[date] ?: NoteDayMarkers()
                        val isSelected = date == selectedDate
                        val isCurrentMonth = YearMonth.from(date) == month
                        Column(
                            Modifier.weight(1f).heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable(role = Role.Button) { onSelectDate(date) }
                                .semantics(mergeDescendants = true) {
                                    selected = isSelected
                                    contentDescription = "${date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale))}, ${marks.notes} notes, ${marks.reminders} reminders, ${marks.events} device events${if (date == today) ", today" else ""}"
                                }.padding(vertical = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(
                                Modifier.fillMaxWidth().heightIn(min = 28.dp).padding(horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    modifier = if (date == today) Modifier.border(1.dp, noteColor, RoundedCornerShape(8.dp)).padding(horizontal = 4.dp, vertical = 2.dp) else Modifier.padding(vertical = 2.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    softWrap = false,
                                    fontWeight = if (date == today || isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                        isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    }
                                )
                            }
                            Row(Modifier.height(6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                if (marks.notes > 0) Box(Modifier.size(5.dp).background(noteColor, CircleShape))
                                if (marks.reminders > 0) Box(Modifier.size(5.dp).background(reminderColor, CircleShape))
                                if (marks.events > 0) Box(Modifier.size(5.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().heightIn(min = 28.dp).padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(6.dp).background(noteColor, CircleShape))
                Text("Note", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(6.dp))
                Box(Modifier.size(6.dp).background(reminderColor, CircleShape))
                Text("Reminder", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(6.dp))
                Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
                Text("Device event", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
