package com.pims.vault.design

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pims.vault.domain.model.RelationshipNote
import com.pims.vault.presentation.relationship.AddOrEditRelationshipNoteDialog
import com.pims.vault.presentation.ui.theme.PimsVaultTheme
import com.pims.vault.presentation.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RelationshipNoteEditorDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun privateDraftDisappearsWhenTheVaultLocksAndReturnsAfterUnlock() {
        var unlocked by mutableStateOf(true)
        compose.setContent {
            PimsVaultTheme(themeMode = ThemeMode.DARK, isReducedMotion = true) {
                AddOrEditRelationshipNoteDialog(
                    initialNote = RelationshipNote("fixture", "person", "Private title", "Private body", isPrivate = true),
                    isVaultUnlocked = unlocked,
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _ -> true }
                )
            }
        }
        compose.onNodeWithText("Private body").assertIsDisplayed()
        compose.runOnIdle { unlocked = false }
        compose.onNodeWithText("Private title").assertDoesNotExist()
        compose.onNodeWithText("Private body").assertDoesNotExist()
        compose.onNodeWithText("Unlock").assertIsDisplayed()
        compose.runOnIdle { unlocked = true }
        compose.onNodeWithText("Private body").assertIsDisplayed()
    }

    @Test
    fun failedSaveRetainsTheDraftAndRetryClosesOnlyAfterPersistence() {
        var attempts = 0
        var dismissals = 0
        compose.setContent {
            PimsVaultTheme(themeMode = ThemeMode.LIGHT, isReducedMotion = true) {
                AddOrEditRelationshipNoteDialog(
                    initialNote = RelationshipNote("fixture", "person", "Plans", "Original draft"),
                    onDismiss = { dismissals++ },
                    onSave = { _, content, _, _, _, _ ->
                        assertEquals("Changed draft", content)
                        attempts++
                        attempts > 1
                    }
                )
            }
        }
        compose.onNodeWithText("Original draft").performTextReplacement("Changed draft")
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("Could not save this note. Check your details and try again.").assertIsDisplayed()
        compose.onNodeWithText("Changed draft").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, dismissals) }
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle {
            assertEquals(2, attempts)
            assertEquals(1, dismissals)
        }
    }
}
