package com.pims.vault.presentation.vault.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.domain.model.VaultItemHeader
import com.pims.vault.presentation.ui.theme.PimsError
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.presentation.vault.VaultUiState
import com.pims.vault.presentation.vault.VaultViewModel
import kotlinx.coroutines.delay

@Composable
fun IdCredentialDetailScreen(
    item: VaultItemHeader,
    uiState: VaultUiState,
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(item.id) {
        viewModel.openItem(item)
    }

    var isRevealed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(isRevealed) {
        if (isRevealed) {
            delay(15_000L)
            isRevealed = false
        }
    }

    val decrypted = uiState.activeDecryptedIdentity
    val idType = decrypted?.idType ?: item.accountIdentifier?.substringBefore("•")?.trim() ?: "ID"
    val fullName = decrypted?.fullName ?: item.title.substringAfter("—").trim()
    val idNumber = decrypted?.identifierNumber ?: item.accountIdentifier?.substringAfter("•")?.trim() ?: ""
    val authority = decrypted?.issuingAuthority
    val issueDate = decrypted?.issueDate
    val expiryDate = decrypted?.expiryDate
    val secondary = decrypted?.secondaryIdentifier
    val notes = decrypted?.notes

    val maskedNumber = if (isRevealed) {
        idNumber
    } else {
        if (idNumber.length > 4) {
            "•••• •••• " + idNumber.takeLast(4)
        } else {
            "••••"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(getVaultBackground())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = getVaultTextSecondary(),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = getVaultTextSecondary(),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(getVaultCardSurface())
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete Credential", color = PimsError) },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = PimsError, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Holographic ID Card Preview
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, getVaultBorder(), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = getVaultCardSurface()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IdCategoryThemedBadge(
                            categoryName = idType,
                            size = 44.dp,
                            iconSize = 24.dp
                        )

                        Text(
                            text = idType.replace("_", " "),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VaultDesignTokens.AccentGreen,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = fullName.ifBlank { "Cardholder Name" },
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = getVaultTextPrimary()
                    )

                    authority?.let {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = it,
                            fontSize = 13.sp,
                            color = getVaultTextSecondary()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "IDENTIFIER NUMBER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = getVaultTextSecondary().copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = maskedNumber,
                            fontSize = 17.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = getVaultTextPrimary()
                        )

                        Row {
                            IconButton(
                                onClick = { isRevealed = !isRevealed },
                                modifier = Modifier.size(32.dp).tactilePress()
                            ) {
                                Icon(
                                    imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Reveal",
                                    tint = getVaultTextSecondary(),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.copyToClipboard(context, idNumber, "$fullName ID Number")
                                },
                                modifier = Modifier.size(32.dp).tactilePress()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = getVaultTextSecondary(),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    expiryDate?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "VALID THRU: ",
                                fontSize = 11.sp,
                                color = getVaultTextSecondary().copy(alpha = 0.7f)
                            )
                            Text(
                                text = it,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = getVaultTextPrimary()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Metadata Detail Rows
            Text(
                text = "Credential Details",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = getVaultTextSecondary()
            )

            Spacer(modifier = Modifier.height(10.dp))

            VaultGroupedCard {
                DetailItemRow(label = "Full Name", value = fullName)
                HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))
                DetailItemRow(label = "ID / Member Number", value = idNumber)

                authority?.let {
                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))
                    DetailItemRow(label = "Issuing Authority", value = it)
                }

                secondary?.let {
                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))
                    DetailItemRow(label = "Secondary Reference", value = it)
                }

                issueDate?.let {
                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))
                    DetailItemRow(label = "Issue Date", value = it)
                }

                expiryDate?.let {
                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))
                    DetailItemRow(label = "Expiry Date", value = it)
                }

                notes?.let {
                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))
                    DetailItemRow(label = "Notes", value = it)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun DetailItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.5.sp, color = getVaultTextSecondary())
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = getVaultTextPrimary(),
            fontFamily = if (value.any { it.isDigit() }) FontFamily.Monospace else FontFamily.Default
        )
    }
}
