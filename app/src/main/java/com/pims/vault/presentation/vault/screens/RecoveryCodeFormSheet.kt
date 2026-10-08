package com.pims.vault.presentation.vault.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.PimsError
import com.pims.vault.presentation.ui.theme.tactilePress
import java.security.SecureRandom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryCodeFormSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (title: String, accountRef: String, codes: List<String>) -> Unit,
    initialTitle: String = "",
    initialAccountRef: String = "",
    initialCodes: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(initialTitle) }
    var accountRef by remember { mutableStateOf(initialAccountRef) }
    var rawCodes by remember { mutableStateOf(initialCodes.joinToString("\n")) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = getVaultCardSurface(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = getVaultTextPrimary(),
                        modifier = Modifier.size(20.dp)
                    )
                }

                SecretTypeThemedBadge(
                    type = SecretPickerType.RECOVERY_CODE,
                    size = 40.dp,
                    iconSize = 22.dp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Add Recovery Codes",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = getVaultTextPrimary()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Store 2FA emergency backup and recovery codes safely.",
                fontSize = 13.5.sp,
                color = getVaultTextSecondary()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error Banner
            errorMessage?.let { err ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, PimsError, RoundedCornerShape(8.dp))
                        .background(PimsError.copy(alpha = 0.12f))
                        .padding(10.dp)
                ) {
                    Text(text = err, color = PimsError, fontSize = 12.5.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            VaultUnderlineField(
                label = "Service / Account Title",
                value = title,
                onValueChange = {
                    title = it
                    errorMessage = null
                },
                placeholder = "e.g. GitHub, Google, Proton"
            )

            VaultUnderlineField(
                label = "Account Identifier / Email",
                value = accountRef,
                onValueChange = {
                    accountRef = it
                    errorMessage = null
                },
                placeholder = "e.g. user@domain.com"
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recovery Codes (one per line)",
                    fontSize = 13.sp,
                    color = getVaultTextSecondary(),
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "Paste or type below",
                    fontSize = 11.5.sp,
                    color = getVaultTextSecondary().copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            androidx.compose.foundation.text.BasicTextField(
                value = rawCodes,
                onValueChange = {
                    rawCodes = it
                    errorMessage = null
                },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = getVaultTextPrimary(),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(getVaultBackground())
                    .border(1.dp, getVaultBorder(), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            VaultPillActionButton(
                text = "Save recovery codes",
                onClick = {
                    val codesList = rawCodes.split("\n", ",", " ")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    if (title.isBlank()) {
                        errorMessage = "Service / Title cannot be blank"
                        return@VaultPillActionButton
                    }
                    if (codesList.isEmpty()) {
                        errorMessage = "Please enter at least one recovery code"
                        return@VaultPillActionButton
                    }

                    onSave(title.trim(), accountRef.trim().ifBlank { title.trim() }, codesList)
                },
                modifier = Modifier.tactilePress { }
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
