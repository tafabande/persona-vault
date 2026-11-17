package com.pims.vault.design

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pims.vault.presentation.notes.NoteDayMarkers
import com.pims.vault.presentation.notes.NotesCalendar
import com.pims.vault.presentation.ui.theme.PersonaMood
import com.pims.vault.presentation.ui.theme.PimsVaultTheme
import com.pims.vault.presentation.ui.theme.ThemeMode
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A six-week synthetic month verifies late dates and touch targets at large font scale. */
@RunWith(AndroidJUnit4::class)
class NotesCalendarLayoutDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allDatesRemainAvailableAndTheLastDayCanBeSelectedAtLargeFontScale() {
        val month = YearMonth.of(2026, 8)
        val today = month.atDay(1)
        val lastDay = month.atEndOfMonth()
        var selected by mutableStateOf<LocalDate?>(null)
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.6f)) {
                PimsVaultTheme(themeMode = ThemeMode.DARK, mood = PersonaMood.WARM, isReducedMotion = true) {
                    Surface(Modifier.fillMaxSize().testTag("calendar_fixture"), color = MaterialTheme.colorScheme.background) {
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                            NotesCalendar(month, selected, today, mapOf(lastDay to NoteDayMarkers(notes = 1)), {},
                                onSelectDate = { selected = it }, onToday = { selected = today })
                        }
                    }
                }
            }
        }
        for (day in 1..month.lengthOfMonth()) {
            compose.onNodeWithContentDescription(description(month.atDay(day), today, lastDay)).assertExists()
        }
        compose.onNodeWithContentDescription(description(lastDay, today, lastDay))
            .performScrollTo().assertIsDisplayed().performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(lastDay, selected) }
        val bitmap = compose.onNodeWithTag("calendar_fixture").captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.cacheDir, "notes-calendar-large-text.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun description(date: LocalDate, today: LocalDate, noteDay: LocalDate): String =
        "${date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault()))}, ${if (date == noteDay) 1 else 0} notes, 0 reminders, 0 device events${if (date == today) ", today" else ""}"
}
