package com.pims.vault.design

import android.graphics.Bitmap
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.domain.model.VaultItemHeader
import com.pims.vault.presentation.ui.theme.*
import com.pims.vault.presentation.vault.banking.AddBankAccountScreen
import com.pims.vault.presentation.vault.banking.BankingFormResult
import com.pims.vault.presentation.vault.card.AddCardScreen
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Compose layouts with synthetic inputs and callbacks that never persist data. */
@RunWith(AndroidJUnit4::class)
class BankCardDesignDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun bankCurrencyAndCardLinksRemainReachableWithLargerText() {
        val card = VaultItemHeader("fixture-card", "fixture-owner", VaultCategory.PAYMENT_REFERENCE, "Example card")
        var saved: BankingFormResult? = null
        fixture(ThemeMode.DARK) {
            AddBankAccountScreen(onNavigateBack = {}, onSaveAccount = { saved = it },
                availableCards = listOf(card), initialBankName = "Example Bank",
                initialAccountHolder = "Alex Rivera", initialAccountNumber = "1234567890",
                initialAccountType = "Nostro", initialCurrency = "USD")
        }
        capture("bank-account-form-dark.png")
        compose.onNodeWithText("ZWG", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithText("Example card").performScrollTo().performClick()
        compose.onNodeWithText("Save account").performScrollTo().assertIsDisplayed().assertIsEnabled()
        capture("bank-account-links-dark.png")
        compose.onNodeWithText("Save account").performClick()
        compose.runOnIdle {
            assertEquals("ZWG", saved?.currency)
            assertEquals(listOf(card.id), saved?.linkedCardIds)
            assertEquals("1234567890", saved?.accountNumber)
        }
    }

    @Test fun cardFormKeepsRawInputAndSupportsIndependentCurrencyLinks() {
        val accounts = listOf(
            VaultItemHeader("fixture-usd", "fixture-owner", VaultCategory.BANK_ACCOUNT, "Example Bank USD"),
            VaultItemHeader("fixture-zwg", "fixture-owner", VaultCategory.BANK_ACCOUNT, "Example Bank ZWG"))
        var savedNumber: String? = null
        var savedLinks: List<String>? = null
        fixture(ThemeMode.LIGHT) {
            AddCardScreen(onNavigateBack = {}, initialBankName = "Example Bank", initialCardHolder = "Alex Rivera",
                initialCardNumber = "4111111111111111", initialExpiryDate = "1228", initialCvv = "123",
                availableBankAccounts = accounts, initialLinkedBankAccountIds = accounts.map { it.id },
                onSaveCard = { _, _, number, _, _, _, _, _, ids -> savedNumber = number; savedLinks = ids })
        }
        capture("card-form-light.png")
        compose.onNodeWithText("Example Bank USD").performScrollTo().performClick()
        compose.onNodeWithText("Add card").performScrollTo().assertIsDisplayed().assertIsEnabled()
        capture("card-links-light.png")
        compose.onNodeWithText("Add card").performClick()
        compose.runOnIdle {
            assertEquals("4111111111111111", savedNumber)
            assertEquals(listOf("fixture-zwg"), savedLinks)
        }
    }

    private fun fixture(mode: ThemeMode, content: @Composable () -> Unit) {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.25f)) {
                PimsVaultTheme(themeMode = mode, mood = PersonaMood.WARM, isReducedMotion = true) {
                    Surface(Modifier.fillMaxSize().testTag("wallet_fixture")) { content() }
                }
            }
        }
    }

    private fun capture(name: String) {
        val bitmap = compose.onNodeWithTag("wallet_fixture").captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.cacheDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
