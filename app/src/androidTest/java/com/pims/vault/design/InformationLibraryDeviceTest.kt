package com.pims.vault.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pims.vault.core.model.InformationCategory
import com.pims.vault.core.model.InformationSensitivity
import com.pims.vault.presentation.library.InformationLibraryScreen
import com.pims.vault.presentation.library.LibraryItem
import com.pims.vault.presentation.ui.theme.PimsVaultTheme
import com.pims.vault.presentation.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InformationLibraryDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun standaloneLibraryUsesDarkThemeInkAndOneCategoryMenu() {
        var screenInk = Color.Unspecified
        var authRequests = 0
        val items = listOf(
            LibraryItem("name", "Legal name", "Alex Rivera", InformationCategory.PERSONAL, InformationSensitivity.NORMAL),
            LibraryItem("note", "Weekend plans", "Bring a book", InformationCategory.NOTE, InformationSensitivity.PRIVATE)
        )
        compose.setContent {
            PimsVaultTheme(themeMode = ThemeMode.DARK, isReducedMotion = true) {
                screenInk = MaterialTheme.colorScheme.onBackground
                InformationLibraryScreen(
                    items = items,
                    onBack = {},
                    onRequestAuthToReveal = { authRequests++ },
                    onOpenDestination = {}
                )
            }
        }
        for (text in listOf("Information library", "Legal name", "Weekend plans")) {
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(text, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals(screenInk, layouts.single().layoutInput.style.color)
        }
        compose.onNodeWithText("Alex Rivera").assertIsDisplayed()
        compose.onNodeWithText("Personal").assertIsDisplayed()
        compose.onNodeWithText("All information").performClick()
        compose.onNode(hasText("Notes") and hasClickAction()).performClick()
        compose.onNodeWithText("Weekend plans").assertIsDisplayed()
        compose.onNodeWithText("Legal name").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, authRequests) }
    }
}
