package com.pims.vault.design

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pims.vault.presentation.ui.components.PersonaFormSection
import com.pims.vault.presentation.ui.components.PersonaSwitch
import com.pims.vault.presentation.ui.components.PersonaTextInput
import com.pims.vault.presentation.ui.components.PersonaToggleRow
import com.pims.vault.presentation.notes.NoteListRow
import com.pims.vault.domain.model.PlainNote
import com.pims.vault.core.model.InformationCategory
import com.pims.vault.core.model.InformationSensitivity
import com.pims.vault.presentation.library.InformationLibraryScreen
import com.pims.vault.presentation.library.LibraryItem
import com.pims.vault.presentation.ui.theme.PersonaMood
import com.pims.vault.presentation.ui.theme.PimsVaultTheme
import com.pims.vault.presentation.ui.theme.ThemeMode
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Renders synthetic values in the Compose test activity, without reading vault records. */
@RunWith(AndroidJUnit4::class)
class SharedControlsDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun darkLibraryInheritsReadableInkAndUsesOneCategoryFilter() {
        compose.setContent {
            PimsVaultTheme(themeMode = ThemeMode.DARK, isReducedMotion = true) {
                assertEquals(MaterialTheme.colorScheme.onSurface, LocalContentColor.current)
                InformationLibraryScreen(
                    items = listOf(LibraryItem(
                        id = "synthetic-name",
                        title = "Legal name",
                        subtitle = "Example Person",
                        category = InformationCategory.PERSONAL,
                        sensitivity = InformationSensitivity.NORMAL
                    )),
                    onBack = {},
                    onRequestAuthToReveal = { error("A legal name should be readable without another prompt") },
                    onOpenDestination = {},
                    modifier = Modifier.testTag("fixture")
                )
            }
        }
        compose.onNodeWithText("Example Person").assertIsDisplayed()
        compose.onNodeWithText("All information").performClick()
        compose.onNode(hasText("Personal") and hasClickAction()).performClick()
        compose.onNodeWithText("Example Person").assertIsDisplayed()
        saveSyntheticPreview("information-library-dark.png")
    }

    @Test fun darkSwitchTogglesOnceAndDisabledStateIsDistinct() {
        var checked by mutableStateOf(false)
        var changes = 0
        renderFixture(ThemeMode.DARK, 1f, { checked }, {
            changes++
            checked = it
        })
        compose.onNodeWithTag("sound_toggle").performClick().assertIsOn()
        compose.runOnIdle { assertEquals(1, changes) }
        compose.onNodeWithTag("disabled_switch").assertIsNotEnabled()
        saveSyntheticPreview("shared-controls-dark.png")
    }

    @Test fun lightControlsRemainReachableWithLargeText() {
        renderFixture(ThemeMode.LIGHT, 1.6f, { false }, {})
        compose.onNodeWithText("Full name").assertIsDisplayed()
        compose.onNodeWithTag("sound_toggle").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("disabled_switch").performScrollTo().assertIsNotEnabled()
        saveSyntheticPreview("shared-controls-light-large.png")
    }

    @Test fun gojoDarkThemeUsesSharedReadableControls() {
        renderFixture(ThemeMode.DARK, 1f, { true }, {}, PersonaMood.JUJUTSU_KAISEN)
        compose.onNodeWithTag("sound_toggle").assertIsOn()
        saveSyntheticPreview("gojo-controls-dark.png")
    }

    @Test fun gojoLightThemeSupportsLargeText() {
        renderFixture(ThemeMode.LIGHT, 1.6f, { false }, {}, PersonaMood.JUJUTSU_KAISEN)
        compose.onNodeWithTag("sound_toggle").performScrollTo().assertIsDisplayed()
        saveSyntheticPreview("gojo-controls-light-large.png")
    }

    @Test fun gojoMotionKeepsControlsInteractive() {
        compose.mainClock.autoAdvance = false
        var enabled by mutableStateOf(false)
        compose.setContent {
            PimsVaultTheme(themeMode = ThemeMode.DARK, mood = PersonaMood.JUJUTSU_KAISEN, isReducedMotion = false) {
                Surface(Modifier.fillMaxSize().testTag("fixture"), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.padding(24.dp)) {
                        Text("Persona", style = MaterialTheme.typography.headlineLarge)
                        PersonaToggleRow("In-app sounds", enabled, { enabled = it }, Modifier.testTag("motion_toggle"))
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithTag("motion_toggle").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("motion_toggle").assertIsOn()
        saveSyntheticPreview("gojo-motion.png")
    }

    @Test fun protectedNotesSuppressTheirPreviewAndPrivateMetadata() {
        compose.setContent {
            PimsVaultTheme(themeMode = ThemeMode.DARK, isReducedMotion = true) {
                Surface(Modifier.fillMaxSize().testTag("fixture"), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Notes", style = MaterialTheme.typography.headlineLarge)
                        NoteListRow(
                            PlainNote("public", "fixture", "Weekend plans", "Take the morning train and spend an afternoon by the lake.", isPinned = true),
                            "14 Nov", {}, {}
                        )
                        NoteListRow(
                            PlainNote(
                                "private", "fixture", "Private subject", "Protected sample",
                                isLocked = true, reminderAt = 2_000_000_000_000L, reminderTag = "private-tag"
                            ),
                            "14 Nov", {}, {}
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("Protected note").assertIsDisplayed()
        compose.onNodeWithText("Private subject").assertDoesNotExist()
        compose.onNodeWithText("Protected sample").assertDoesNotExist()
        compose.onNodeWithText("#private-tag").assertDoesNotExist()
        saveSyntheticPreview("shared-notes-dark.png")
    }

    private fun renderFixture(
        mode: ThemeMode,
        fontScale: Float,
        checked: () -> Boolean,
        onCheckedChange: (Boolean) -> Unit,
        mood: PersonaMood = PersonaMood.WARM
    ) {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                PimsVaultTheme(themeMode = mode, mood = mood, isReducedMotion = true) {
                    Surface(Modifier.fillMaxSize().testTag("fixture"), color = MaterialTheme.colorScheme.background) {
                        Column(
                            Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Text("Persona", style = MaterialTheme.typography.headlineLarge)
                            PersonaFormSection("About you", icon = Icons.Default.Person) {
                                PersonaTextInput("Alex Rivera", {}, label = "Full name")
                                PersonaTextInput("alex@example.com", {}, label = "Email")
                            }
                            PersonaFormSection("Preferences") {
                                PersonaToggleRow("In-app sounds", checked(), onCheckedChange, Modifier.testTag("sound_toggle"))
                                PersonaToggleRow("Haptics", false, {}, enabled = true)
                                Text("Disabled", style = MaterialTheme.typography.bodyMedium)
                                PersonaSwitch(true, {}, Modifier.testTag("disabled_switch"), enabled = false)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveSyntheticPreview(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = compose.onNodeWithTag("fixture").captureToImage().asAndroidBitmap()
        File(context.cacheDir, name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
