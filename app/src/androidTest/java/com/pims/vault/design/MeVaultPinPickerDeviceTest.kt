package com.pims.vault.design

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.domain.model.VaultItemHeader
import com.pims.vault.presentation.hub.MeOverviewPins
import com.pims.vault.presentation.hub.OverviewPin
import com.pims.vault.presentation.hub.OverviewSource
import com.pims.vault.presentation.hub.VaultMePinPicker
import com.pims.vault.presentation.hub.rememberMeOverviewPins
import com.pims.vault.presentation.ui.theme.PimsVaultTheme
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeVaultPinPickerDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun vaultPickerPinsOwnedItemsAndHidesWhenTheSessionEnds() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val account = "test-pin/${UUID.randomUUID()}"
        val owner = "test-owner/${UUID.randomUUID()}"
        val storeScope = "$account/$owner"
        val store = MeOverviewPins(context)
        val unlocked = mutableStateOf(false)
        val items = listOf(
            VaultItemHeader("card", owner, VaultCategory.PAYMENT_REFERENCE, "Sample card", "Sample issuer •••• 1234"),
            VaultItemHeader("key", owner, VaultCategory.PASSWORD, "Sample login"),
            VaultItemHeader("other", "other-owner", VaultCategory.PASSWORD, "Other person's login")
        )

        try {
            compose.setContent {
                PimsVaultTheme(isReducedMotion = true) {
                    VaultMePinPicker(rememberMeOverviewPins(account, owner), items, unlocked.value, onDismiss = {})
                }
            }
            compose.onNodeWithText("Sample card").assertDoesNotExist()
            compose.runOnIdle { unlocked.value = true }
            compose.waitUntil(10_000) {
                compose.onAllNodesWithContentDescription("Pin Sample card").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Ending in 1234").assertIsDisplayed()
            compose.onNodeWithText("Other person's login").assertDoesNotExist()
            compose.onNodeWithContentDescription("Pin Sample card").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithContentDescription("Unpin Sample card").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Pin Sample login").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithContentDescription("Unpin Sample login").fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals(
                listOf(OverviewPin(OverviewSource.Vault, "card"), OverviewPin(OverviewSource.Vault, "key")),
                MeOverviewPins(context).read(storeScope)
            )
            compose.runOnIdle { unlocked.value = false }
            compose.onNodeWithText("Sample login").assertDoesNotExist()
        } finally {
            store.write(storeScope, emptyList())
        }
    }
}
