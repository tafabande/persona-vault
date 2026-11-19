package com.pims.vault.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.pims.vault.presentation.ui.theme.DarkTextSecondary
import com.pims.vault.presentation.ui.theme.PersonaMood
import com.pims.vault.presentation.ui.theme.personaAccentContentColor
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonaThemeContrastTest {
    private fun contrast(first: Color, second: Color): Float {
        val high = maxOf(first.luminance(), second.luminance())
        val low = minOf(first.luminance(), second.luminance())
        return (high + 0.05f) / (low + 0.05f)
    }

    @Test fun accentControlsHaveReadableInkForEveryMoodAndMode() {
        PersonaMood.values().forEach { mood ->
            listOf(mood.accentColor, mood.darkAccentColor).forEach { accent ->
                assertTrue(mood.name, contrast(accent, personaAccentContentColor(accent)) >= 4.5f)
            }
        }
    }

    @Test fun accentInkStaysReadableDuringMoodTransitions() {
        val accents = PersonaMood.values().flatMap { listOf(it.accentColor, it.darkAccentColor) }
        accents.forEach { start ->
            accents.forEach { end ->
                (0..20).forEach { step ->
                    val color = lerp(start, end, step / 20f)
                    assertTrue(contrast(color, personaAccentContentColor(color)) >= 4.5f)
                }
            }
        }
    }

    @Test fun uncheckedDarkSwitchThumbIsVisibleAgainstItsTrack() {
        PersonaMood.values().forEach { mood ->
            assertTrue(mood.name, contrast(DarkTextSecondary, mood.darkSurfaceVariantColor) >= 3f)
        }
    }
}
