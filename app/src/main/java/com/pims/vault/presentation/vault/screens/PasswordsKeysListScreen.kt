package com.pims.vault.presentation.vault.screens

import com.pims.vault.presentation.ui.components.PersonaIconAction
import com.pims.vault.presentation.ui.theme.PersonaIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.domain.model.VaultItemHeader
import com.pims.vault.presentation.ui.theme.tactilePress

@Composable
fun PasswordsKeysListScreen(
    items: List<VaultItemHeader>,
    onOpenItem: (VaultItemHeader) -> Unit,
    onAddSecret: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Filter relevant credential categories
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

    val filteredItems = items.filter { item ->
        item.category in credentialCategories &&
            (searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                (item.accountIdentifier?.contains(searchQuery, ignoreCase = true) == true))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(getVaultBackground())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = com.pims.vault.presentation.ui.theme.pimsPageGutter())
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PersonaIconAction(PersonaIcons.ArrowBack, "Back", onBack)

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Passwords & keys",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = getVaultTextPrimary()
                )

                Spacer(modifier = Modifier.weight(1f))

                // Search toggle icon
                PersonaIconAction(if (isSearchActive) PersonaIcons.Close else PersonaIcons.Search, "Search", { isSearchActive = !isSearchActive })

                PersonaIconAction(PersonaIcons.Add, "Add", onAddSecret)
            }

            if (isSearchActive) {
                VaultUnderlineField(
                    label = "Search credentials",
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Type service or label...",
                    trailingAction = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(PersonaIcons.Close, contentDescription = "Clear", tint = getVaultTextSecondary(), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No credentials match '$searchQuery'" else "No passwords or keys saved yet",
                            fontSize = 15.sp,
                            color = getVaultTextSecondary()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        VaultPillActionButton(
                            text = "Add a secret",
                            onClick = onAddSecret,
                            modifier = Modifier.width(200.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    item {
                        VaultGroupedCard {
                            filteredItems.forEachIndexed { index, item ->
                                val subtitleText = when (item.category) {
                                    VaultCategory.PASSWORD -> item.accountIdentifier ?: "Login"
                                    VaultCategory.API_KEY -> item.accountIdentifier ?: "API key"
                                    VaultCategory.SSH_KEY -> item.accountIdentifier ?: "SSH key"
                                    VaultCategory.ACCESS_TOKEN -> item.accountIdentifier ?: "Access token"
                                    VaultCategory.RECOVERY_CODE -> item.accountIdentifier ?: "Recovery codes"
                                    VaultCategory.SECURE_NOTE -> "Secure note"
                                    VaultCategory.GENERIC_SECRET -> item.accountIdentifier ?: "Secret"
                                    else -> item.accountIdentifier ?: ""
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenItem(item) }
                                        .tactilePress()
                                        .padding(horizontal = 16.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val itemIcon = when (item.category) {
                                        VaultCategory.PASSWORD -> PersonaIcons.Key
                                        VaultCategory.API_KEY -> PersonaIcons.Code
                                        VaultCategory.SSH_KEY -> PersonaIcons.Terminal
                                        VaultCategory.ACCESS_TOKEN -> PersonaIcons.Security
                                        VaultCategory.RECOVERY_CODE -> PersonaIcons.LockReset
                                        VaultCategory.SECURE_NOTE -> PersonaIcons.Documents
                                        VaultCategory.GENERIC_SECRET -> PersonaIcons.Lock
                                        else -> PersonaIcons.Key
                                    }

                                    Icon(
                                        imageVector = itemIcon,
                                        contentDescription = null,
                                        tint = getVaultTextSecondary(),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
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
                                        imageVector = PersonaIcons.ChevronRight,
                                        contentDescription = null,
                                        tint = getVaultTextSecondary().copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                if (index < filteredItems.lastIndex) {
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = getVaultBorder().copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
