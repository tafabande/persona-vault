package com.pims.vault.presentation.vault.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.domain.model.VaultItemHeader
import com.pims.vault.presentation.ui.theme.tactilePress

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
    modifier: Modifier = Modifier
) {
    val paymentCards = vaultItems.filter { it.category == VaultCategory.PAYMENT_REFERENCE }
    val bankAccounts = vaultItems.filter { it.category == VaultCategory.BANK_ACCOUNT }

    val passwordsAndKeysCount = vaultItems.count {
        it.category in setOf(
            VaultCategory.PASSWORD,
            VaultCategory.API_KEY,
            VaultCategory.SSH_KEY,
            VaultCategory.ACCESS_TOKEN,
            VaultCategory.RECOVERY_CODE,
            VaultCategory.GENERIC_SECRET,
            VaultCategory.SECURE_NOTE,
            VaultCategory.TOTP_2FA
        )
    }

    val idItems = vaultItems.filter { it.category == VaultCategory.IDENTITY_CREDENTIAL }
    val nationalIdCount = idItems.count { it.title.contains("National", ignoreCase = true) || it.title.contains("ID", ignoreCase = true) || it.accountIdentifier?.contains("NATIONAL_ID", ignoreCase = true) == true }
    val schoolCount = idItems.count { it.title.contains("School", ignoreCase = true) || it.title.contains("Student", ignoreCase = true) || it.accountIdentifier?.contains("SCHOOL_ID", ignoreCase = true) == true }
    val libraryCount = idItems.count { it.title.contains("Library", ignoreCase = true) || it.accountIdentifier?.contains("LIBRARY_ID", ignoreCase = true) == true }
    val medicalCount = idItems.count { it.title.contains("Medical", ignoreCase = true) || it.title.contains("Health", ignoreCase = true) || it.accountIdentifier?.contains("MEDICAL_AID", ignoreCase = true) == true }
    val customCount = idItems.count { it.title.contains("Custom", ignoreCase = true) || it.accountIdentifier?.contains("CUSTOM", ignoreCase = true) == true }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val cardWidth = screenWidth * 0.58f
    val cardsListState = rememberLazyListState()
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = cardsListState)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(getVaultBackground())
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar: Back, "Vault", "+"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = getVaultTextPrimary(),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Vault",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = getVaultTextPrimary()
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = onAddSecret, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = getVaultTextPrimary(),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // SECTION 1: Cards & accounts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cards & accounts",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = getVaultTextSecondary()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onAddCard)
                                .tactilePress { onAddCard() },
                            color = getVaultCardSurface(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, getVaultBorder()),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = getVaultTextPrimary(), modifier = Modifier.size(13.dp))
                                Text("Card", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = getVaultTextPrimary())
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onAddBankAccount)
                                .tactilePress { onAddBankAccount() },
                            color = getVaultCardSurface(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, getVaultBorder()),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = getVaultTextPrimary(), modifier = Modifier.size(13.dp))
                                Text("Bank", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = getVaultTextPrimary())
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (paymentCards.isEmpty() && bankAccounts.isEmpty()) {
                    // Empty Card Slot with action to add card or bank
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, getVaultBorder(), RoundedCornerShape(18.dp)),
                            shape = RoundedCornerShape(18.dp),
                            color = getVaultCardSurface()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(onClick = onAddCard)
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = getVaultTextPrimary(), modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Add Card", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = getVaultTextPrimary())
                                }
                                Box(modifier = Modifier.width(1.dp).height(40.dp).background(getVaultBorder()))
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(onClick = onAddBankAccount)
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = getVaultTextPrimary(), modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Add Bank", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = getVaultTextPrimary())
                                }
                            }
                        }
                    }
                } else {
                    // Payment cards carousel
                    if (paymentCards.isNotEmpty()) {
                        LazyRow(
                            state = cardsListState,
                            flingBehavior = snapBehavior,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(paymentCards, key = { "card_${it.id}" }) { card ->
                                val brandStyle = getCardBrandStyle(card.title, card.accountIdentifier ?: "")
                                val displayIssuer = getCardDisplayIssuer(card.title, brandStyle.brandName)
                                val displayType = getCardDisplayType(card.title, card.accountIdentifier, isBankAccount = false)
                                val last4 = card.accountIdentifier?.filter { it.isDigit() }?.takeLast(4)?.ifEmpty { "••••" } ?: "••••"
                                val isLinked = card.accountIdentifier?.contains("Linked", ignoreCase = true) == true ||
                                        card.accountIdentifier?.contains("Bank:", ignoreCase = true) == true

                                VaultCardItem(
                                    cardWidth = cardWidth,
                                    displayIssuer = displayIssuer,
                                    displayType = displayType,
                                    maskedNumber = "•••• $last4",
                                    brandStyle = brandStyle,
                                    isLinked = isLinked,
                                    onClick = { onOpenCard(card) }
                                )
                            }
                        }
                    }

                    // Bank accounts rendered as distinct list rows
                    if (bankAccounts.isNotEmpty()) {
                        if (paymentCards.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            bankAccounts.forEach { bank ->
                                val bankNumberMatch = Regex("••••\\s*(\\d+)").find(bank.accountIdentifier ?: "")
                                val last4Acc = bankNumberMatch?.groupValues?.get(1)
                                    ?: bank.accountIdentifier?.filter { it.isDigit() }?.takeLast(4)?.ifEmpty { "••••" }
                                    ?: "••••"
                                val isLinked = bank.accountIdentifier?.contains("Card:") == true ||
                                        bank.accountIdentifier?.contains("💳") == true ||
                                        bank.accountIdentifier?.contains("Linked", ignoreCase = true) == true

                                VaultBankAccountRow(
                                    bankName = getCardDisplayIssuer(bank.title, ""),
                                    maskedNumber = "•••• $last4Acc",
                                    accountType = getCardDisplayType(bank.title, bank.accountIdentifier, isBankAccount = true),
                                    isLinked = isLinked,
                                    onClick = { onOpenBankAccount(bank) }
                                )
                            }
                        }
                    }
                }

                // Remaining content sections wrapped with 18.dp padding
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                ) {
                    Spacer(modifier = Modifier.height(28.dp))

                    // SECTION 2: Passwords & keys
                    val credentialCategories = setOf(
                        VaultCategory.PASSWORD,
                        VaultCategory.API_KEY,
                        VaultCategory.SSH_KEY,
                        VaultCategory.ACCESS_TOKEN,
                        VaultCategory.RECOVERY_CODE,
                        VaultCategory.GENERIC_SECRET,
                        VaultCategory.SECURE_NOTE,
                        VaultCategory.TOTP_2FA
                    )
                    val savedCredentials = vaultItems.filter { it.category in credentialCategories }

                    Text(
                        text = "Passwords & keys",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = getVaultTextSecondary()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    VaultGroupedCard {
                        if (savedCredentials.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onOpenPasswordsAndKeys)
                                    .tactilePress()
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = getVaultTextPrimary(),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Passwords & keys",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = getVaultTextPrimary()
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "0 saved · Tap to add",
                                        fontSize = 13.sp,
                                        color = getVaultTextSecondary()
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = getVaultTextSecondary().copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            val previewCredentials = savedCredentials.take(4)
                            previewCredentials.forEachIndexed { idx, item ->
                                val itemIcon = when (item.category) {
                                    VaultCategory.PASSWORD -> Icons.Default.Key
                                    VaultCategory.SECURE_NOTE -> Icons.Default.Key
                                    else -> Icons.Default.Key
                                }
                                val subtitleText = when (item.category) {
                                    VaultCategory.PASSWORD -> item.accountIdentifier ?: "Login"
                                    VaultCategory.API_KEY -> item.accountIdentifier ?: "API key"
                                    VaultCategory.SSH_KEY -> item.accountIdentifier ?: "SSH key"
                                    VaultCategory.ACCESS_TOKEN -> item.accountIdentifier ?: "Access token"
                                    VaultCategory.RECOVERY_CODE -> item.accountIdentifier ?: "Recovery codes"
                                    VaultCategory.SECURE_NOTE -> "Encrypted note"
                                    VaultCategory.GENERIC_SECRET -> item.accountIdentifier ?: "Secret"
                                    else -> item.accountIdentifier ?: "Credential"
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenSecret(item) }
                                        .tactilePress()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = itemIcon,
                                        contentDescription = null,
                                        tint = getVaultTextPrimary(),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = getVaultTextPrimary()
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = subtitleText,
                                            fontSize = 13.sp,
                                            color = getVaultTextSecondary()
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = getVaultTextSecondary().copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                if (idx < previewCredentials.size - 1 || savedCredentials.size > previewCredentials.size) {
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = getVaultBorder().copy(alpha = 0.6f)
                                    )
                                }
                            }

                            if (savedCredentials.size > 4) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = onOpenPasswordsAndKeys)
                                        .tactilePress()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "View all ${savedCredentials.size} passwords & keys",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = getVaultTextPrimary()
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = getVaultTextPrimary(),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // SECTION 3: IDs & memberships
                    Text(
                        text = "IDs & memberships",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = getVaultTextSecondary()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    VaultGroupedCard {
                        IdMembershipRow(
                            title = "National ID",
                            statusText = if (nationalIdCount > 0) "$nationalIdCount saved" else "Not added",
                            onClick = { onOpenIdCategory("National ID") }
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))

                        IdMembershipRow(
                            title = "School",
                            statusText = if (schoolCount > 0) "$schoolCount saved" else "Not added",
                            onClick = { onOpenIdCategory("School") }
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))

                        IdMembershipRow(
                            title = "Library",
                            statusText = if (libraryCount > 0) "$libraryCount saved" else "Not added",
                            onClick = { onOpenIdCategory("Library") }
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))

                        IdMembershipRow(
                            title = "Medical aid",
                            statusText = if (medicalCount > 0) "$medicalCount saved" else "Not added",
                            onClick = { onOpenIdCategory("Medical aid") }
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))

                        IdMembershipRow(
                            title = "Custom & Memberships",
                            statusText = if (customCount > 0) "$customCount saved" else "Not added",
                            onClick = { onOpenIdCategory("Custom") }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 4: Import & restore row
                    VaultGroupedCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onOpenImportRestore)
                                .tactilePress()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = getVaultTextSecondary(),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Import & restore",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = getVaultTextSecondary(),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = getVaultTextSecondary().copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

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
            .clickable(onClick = onClick)
            .tactilePress { onClick() }
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isLinked) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(VaultDesignTokens.AccentGreen.copy(alpha = 0.20f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Linked",
                            fontSize = 10.sp,
                            color = VaultDesignTokens.AccentGreen,
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

@Composable
private fun VaultBankAccountRow(
    bankName: String,
    maskedNumber: String,
    accountType: String,
    isLinked: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .tactilePress { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = getVaultCardSurface(),
        border = androidx.compose.foundation.BorderStroke(1.dp, getVaultBorder())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bank icon badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(VaultDesignTokens.AccentGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = VaultDesignTokens.AccentGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = bankName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = getVaultTextPrimary(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isLinked) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(VaultDesignTokens.AccentGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Linked",
                                fontSize = 9.sp,
                                color = VaultDesignTokens.AccentGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = maskedNumber,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = getVaultTextSecondary()
                )
            }

            // Account type badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(getVaultBorder().copy(alpha = 0.5f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = accountType,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = getVaultTextSecondary()
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = getVaultTextSecondary().copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}


@Composable
private fun IdMembershipRow(
    title: String,
    statusText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .tactilePress()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IdCategoryThemedBadge(
            categoryName = title,
            size = 38.dp,
            iconSize = 20.dp
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = getVaultTextPrimary()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = statusText,
                fontSize = 12.5.sp,
                color = if (statusText != "Not added") VaultDesignTokens.AccentGreen else getVaultTextSecondary()
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = getVaultTextSecondary().copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}
