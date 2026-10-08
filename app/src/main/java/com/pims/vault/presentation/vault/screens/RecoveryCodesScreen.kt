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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.domain.model.RecoveryCodeItem
import com.pims.vault.presentation.ui.theme.tactilePress

import com.pims.vault.domain.model.VaultItemHeader
import com.pims.vault.presentation.vault.VaultUiState
import com.pims.vault.presentation.vault.VaultViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect

@Composable
fun RecoveryCodesScreen(
    item: VaultItemHeader,
    uiState: VaultUiState,
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(item.id) {
        viewModel.openItem(item)
    }

    val codes = uiState.activeDecryptedRecoveryCodes?.codes ?: emptyList()

    RecoveryCodesScreen(
        title = item.title,
        codes = codes,
        onToggleCodeUsed = { code ->
            viewModel.consumeRecoveryCode(item.id, code)
        },
        onCopyRemainingCodes = { remCodes ->
            viewModel.copyToClipboard(context, remCodes.joinToString("\n"), "${item.title} Recovery Codes")
        },
        onGenerateNewCodes = {
            val newBatch = (1..8).map {
                val p1 = (1000..9999).random()
                val p2 = (1000..9999).random()
                "$p1-$p2"
            }
            viewModel.saveRecoveryCodes(
                title = item.title,
                accountReference = item.accountIdentifier ?: "",
                codes = newBatch,
                existingId = item.id
            )
        },
        onDelete = {
            viewModel.deleteItem(item.id)
            onBack()
        },
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun RecoveryCodesScreen(
    title: String,
    codes: List<RecoveryCodeItem>,
    onToggleCodeUsed: (String) -> Unit,
    onCopyRemainingCodes: (List<String>) -> Unit,
    onGenerateNewCodes: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val remainingCount = codes.count { !it.isUsed }
    val totalCount = codes.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(getVaultBackground())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = getVaultTextPrimary(),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = getVaultTextPrimary(),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title & Subtitle
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = getVaultTextPrimary()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Recovery codes · $remainingCount of $totalCount left",
                fontSize = 15.sp,
                color = getVaultTextSecondary()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Use one of these if you lose access to your authenticator. Each code works once.",
                fontSize = 14.sp,
                color = getVaultTextSecondary(),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2-Column Code Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(codes) { codeItem ->
                    RecoveryCodeCard(
                        code = codeItem.code,
                        isUsed = codeItem.isUsed,
                        onClick = { onToggleCodeUsed(codeItem.code) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Copy Remaining Codes Button
            VaultPillActionButton(
                text = "Copy remaining codes",
                enabled = remainingCount > 0,
                onClick = {
                    val remaining = codes.filter { !it.isUsed }.map { it.code }
                    onCopyRemainingCodes(remaining)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Generate New Codes Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGenerateNewCodes() }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(getVaultTextSecondary())
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generate new codes",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = getVaultTextSecondary()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun RecoveryCodeCard(
    code: String,
    isUsed: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (isUsed) getVaultBorder().copy(alpha = 0.4f) else getVaultBorder(),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .tactilePress(),
        shape = RoundedCornerShape(12.dp),
        color = if (isUsed) getVaultCardSurface().copy(alpha = 0.45f) else getVaultCardSurface()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = code,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = if (isUsed) getVaultTextSecondary().copy(alpha = 0.5f) else getVaultTextPrimary(),
                textDecoration = if (isUsed) TextDecoration.LineThrough else TextDecoration.None
            )

            if (isUsed) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Used",
                    tint = getVaultTextSecondary().copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(getVaultTextSecondary().copy(alpha = 0.6f))
                )
            }
        }
    }
}
