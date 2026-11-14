package com.pims.vault.presentation.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Stable categorical markers remain distinguishable across all user-selected moods.
val CalendarNoteColor: Color
    @Composable get() = if (LocalPimsDarkTheme.current) Color(0xFF93C5FD) else Color(0xFF2459A6)
val CalendarReminderColor: Color
    @Composable get() = if (LocalPimsDarkTheme.current) Color(0xFFFDBA74) else Color(0xFFA44812)
