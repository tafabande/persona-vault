package com.pims.vault.presentation.vault.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.tactilePress

enum class SecretPickerType(val displayName: String) {
    PAYMENT_CARD("Card"),
    BANK_ACCOUNT("Bank account"),
    PASSWORD("Password"),
    IDENTITY_CREDENTIAL("ID / Member"),
    LOGIN("Login"),
    LICENSE_KEY("License key"),
    PRODUCT_KEY("Product key"),
    SERIAL_NUMBER("Serial number"),
    API_KEY("API key"),
    ACCESS_TOKEN("Access token"),
    RECOVERY_CODE("Recovery code"),
    DEVICE_KEY("Device key"),
    SSH_KEY("SSH key"),
    CLOUD_CREDENTIAL("Cloud credential"),
    GAME_CODE("Game code"),
    CERTIFICATE("Certificate"),
    CUSTOM_SECRET("Custom secret")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSecretTypePickerSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSelectType: (SecretPickerType) -> Unit,
    modifier: Modifier = Modifier
) {
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
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Close Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = getVaultTextPrimary(),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Subtitle
            Text(
                text = "Add a secret",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = getVaultTextPrimary()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pick what kind, and the form matches it.",
                fontSize = 15.sp,
                color = getVaultTextSecondary()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3-Column Grid
            val types = SecretPickerType.values().toList()

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                items(types) { type ->
                    SecretTypeGridCard(
                        type = type,
                        onClick = {
                            onDismiss()
                            onSelectType(type)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SecretTypeGridCard(
    type: SecretPickerType,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, getVaultBorder(), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .tactilePress(),
        shape = RoundedCornerShape(16.dp),
        color = getVaultCardSurface()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SecretTypeThemedBadge(
                type = type,
                size = 36.dp,
                iconSize = 18.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = type.displayName,
                fontSize = 12.sp,
                color = getVaultTextPrimary(),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
