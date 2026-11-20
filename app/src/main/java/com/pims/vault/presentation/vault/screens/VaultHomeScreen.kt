package com.pims.vault.presentation.vault.screens

import com.pims.vault.presentation.ui.theme.PersonaIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.domain.model.VaultItemHeader
import com.pims.vault.presentation.ui.components.PersonaAction
import com.pims.vault.presentation.ui.components.PersonaActionStyle
import com.pims.vault.presentation.ui.components.PersonaIconAction
import com.pims.vault.presentation.ui.components.PersonaInformationRow
import com.pims.vault.presentation.ui.components.PersonaSectionHeading
import com.pims.vault.presentation.ui.theme.PimsSpacing
import com.pims.vault.presentation.ui.theme.pimsPageGutter
import com.pims.vault.presentation.ui.theme.tactilePress

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VaultHomeScreen(
    vaultItems: List<VaultItemHeader>,
    onOpenCard: (VaultItemHeader) -> Unit,
    onOpenBankAccount: (VaultItemHeader) -> Unit,
    onOpenPasswordsAndKeys: () -> Unit,
    onOpenSecret: (VaultItemHeader) -> Unit = {},
    onOpenIdCategory: (String) -> Unit,
    onOpenImportRestore: () -> Unit,
    onAddSecret: () -> Unit,
    onAddCard: () -> Unit,
    onAddBankAccount: () -> Unit,
    onBack: () -> Unit,
    isMigratingLegacyItems: Boolean = false,
    legacyMigrationPendingCount: Int = 0,
    modifier: Modifier = Modifier,
    onManageMePins: (() -> Unit)? = null
) {
    val paymentCards = vaultItems.filter { it.category == VaultCategory.PAYMENT_REFERENCE }
    val bankAccounts = vaultItems.filter { it.category == VaultCategory.BANK_ACCOUNT }
    val idCount = vaultItems.count { it.category == VaultCategory.IDENTITY_CREDENTIAL }
    val savedCredentials = vaultItems.filterNot {
        it.category == VaultCategory.PAYMENT_REFERENCE ||
            it.category == VaultCategory.BANK_ACCOUNT ||
            it.category == VaultCategory.IDENTITY_CREDENTIAL
    }
    val cardWidth = (LocalConfiguration.current.screenWidthDp.dp * 0.58f).coerceAtMost(320.dp)
    val cardsListState = rememberLazyListState()
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = cardsListState)

    Column(
        modifier = modifier.fillMaxSize().background(getVaultBackground()).padding(horizontal = pimsPageGutter())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PersonaIconAction(PersonaIcons.ArrowBack, "Back", onBack)
            Text("Vault", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            PersonaIconAction(PersonaIcons.Add, "Add to Vault", onAddSecret)
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                .padding(bottom = PimsSpacing.l)
        ) {
            if (isMigratingLegacyItems || legacyMigrationPendingCount > 0) {
                Text(
                    text = if (isMigratingLegacyItems) "Updating vault protection…" else
                        "$legacyMigrationPendingCount older ${if (legacyMigrationPendingCount == 1) "item" else "items"} will update when their current key is available.",
                    modifier = Modifier.padding(vertical = PimsSpacing.s),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (onManageMePins != null) {
                PersonaAction(
                    label = "Pin to Me",
                    onClick = onManageMePins,
                    modifier = Modifier.padding(top = PimsSpacing.s, bottom = PimsSpacing.sectionGap),
                    style = PersonaActionStyle.Quiet,
                    icon = PersonaIcons.PushPin
                )
            }

            PersonaSectionHeading("Cards & accounts")
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = PimsSpacing.s),
                horizontalArrangement = Arrangement.spacedBy(PimsSpacing.s),
                verticalArrangement = Arrangement.spacedBy(PimsSpacing.s)
            ) {
                PersonaAction("Add card", onAddCard, style = PersonaActionStyle.Quiet, icon = PersonaIcons.CreditCard)
                PersonaAction("Add account", onAddBankAccount, style = PersonaActionStyle.Quiet, icon = PersonaIcons.AccountBalance)
            }

            if (paymentCards.isNotEmpty()) {
                LazyRow(
                    state = cardsListState,
                    flingBehavior = snapBehavior,
                    horizontalArrangement = Arrangement.spacedBy(PimsSpacing.sm)
                ) {
                    items(paymentCards, key = { it.id }) { card ->
                        val identifier = card.accountIdentifier.orEmpty()
                        val brandStyle = getCardBrandStyle(card.title, identifier)
                        val lastFour = identifier.filter(Char::isDigit).takeLast(4).ifEmpty { "••••" }
                        VaultCardItem(
                            cardWidth = cardWidth,
                            displayIssuer = getCardDisplayIssuer(card.title, brandStyle.brandName),
                            displayType = getCardDisplayType(card.title, card.accountIdentifier, isBankAccount = false),
                            maskedNumber = "•••• $lastFour",
                            brandStyle = brandStyle,
                            isLinked = identifier.contains("Linked", ignoreCase = true) ||
                                identifier.contains("Bank:", ignoreCase = true),
                            onClick = { onOpenCard(card) }
                        )
                    }
                }
            }
            bankAccounts.forEach { account ->
                val identifier = account.accountIdentifier.orEmpty()
                val lastFour = accountMask.find(identifier)?.groupValues?.get(1)
                    ?.takeLast(4) ?: identifier.filter(Char::isDigit).takeLast(4).ifEmpty { "••••" }
                val accountType = getCardDisplayType(account.title, account.accountIdentifier, isBankAccount = true)
                val isLinked = identifier.contains("Card:") || identifier.contains("💳") ||
                    identifier.contains("Linked", ignoreCase = true)
                val detail = listOfNotNull(accountType, "•••• $lastFour", "Linked".takeIf { isLinked })
                PersonaInformationRow(
                    title = getCardDisplayIssuer(account.title, ""),
                    icon = PersonaIcons.AccountBalance,
                    detail = detail.joinToString(" · "),
                    onClick = { onOpenBankAccount(account) }
                )
            }

            Spacer(Modifier.height(PimsSpacing.sectionGap))
            PersonaInformationRow(
                title = "Passwords & keys",
                icon = PersonaIcons.Key,
                detail = "${savedCredentials.size} saved",
                onClick = onOpenPasswordsAndKeys
            )
            savedCredentials.take(4).forEach { item ->
                val detail = when (item.category) {
                    VaultCategory.PASSWORD -> "Login"
                    VaultCategory.API_KEY -> "API key"
                    VaultCategory.SSH_KEY -> "SSH key"
                    VaultCategory.ACCESS_TOKEN -> "Access token"
                    VaultCategory.RECOVERY_CODE -> "Recovery codes"
                    VaultCategory.SECURE_NOTE -> "Encrypted note"
                    VaultCategory.GENERIC_SECRET -> "Secret"
                    VaultCategory.TOTP_2FA -> "Authenticator"
                    else -> "Credential"
                }
                PersonaInformationRow(
                    title = item.title,
                    icon = PersonaIcons.Key,
                    detail = item.accountIdentifier.takeUnless { item.category == VaultCategory.SECURE_NOTE } ?: detail,
                    onClick = { onOpenSecret(item) }
                )
            }

            Spacer(Modifier.height(PimsSpacing.s))
            PersonaInformationRow(
                title = "IDs & memberships",
                icon = PersonaIcons.Identity,
                detail = "$idCount saved",
                onClick = { onOpenIdCategory("All") }
            )
            PersonaInformationRow(
                title = "Import & restore",
                icon = PersonaIcons.CloudDownload,
                onClick = onOpenImportRestore
            )
        }
    }
}

private val accountMask = Regex("""••••\s*(\d+)""")

@Composable
private fun VaultCardItem(
    cardWidth: androidx.compose.ui.unit.Dp,
    displayIssuer: String,
    displayType: String,
    maskedNumber: String,
    brandStyle: CardBrandStyle,
    isLinked: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(cardWidth)
            .aspectRatio(1.586f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = brandStyle.gradientColors,
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .tactilePress()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP CORNERS: Issuer (top-left) & Linked pill (top-right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayIssuer,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isLinked) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Linked",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // BOTTOM CORNERS: Masked number (bottom-left) & Type (bottom-right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = maskedNumber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )

                Text(
                    text = displayType,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

        }
    }
}
