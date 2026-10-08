package com.pims.vault.presentation.vault.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.tactilePress
import java.security.SecureRandom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericSecretFormSheet(
    secretType: SecretPickerType,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (label: String, secretValue: String, secretType: String, provider: String?, notes: String?) -> Unit,
    initialLabel: String = "",
    initialSecretValue: String = "",
    initialProvider: String = "",
    initialNotes: String = "",
    isEditing: Boolean = false,
    modifier: Modifier = Modifier
) {
    var label by remember { mutableStateOf(initialLabel) }
    var secretValue by remember { mutableStateOf(initialSecretValue) }
    var provider by remember { mutableStateOf(initialProvider) }
    var notes by remember { mutableStateOf(initialNotes) }
    var isValueVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val (titleLabel, valueLabel, providerLabel, labelPlaceholder, valuePlaceholder, providerPlaceholder) = remember(secretType) {
        when (secretType) {
            SecretPickerType.LICENSE_KEY -> Tuple6(
                "Software / App Name",
                "License Key",
                "Vendor / Publisher",
                "e.g. JetBrains All Products",
                "e.g. JB-98765-XXXX-YYYY",
                "e.g. JetBrains"
            )
            SecretPickerType.PRODUCT_KEY -> Tuple6(
                "Product / OS Name",
                "Product Key",
                "Edition / Distributor",
                "e.g. Windows 11 Pro",
                "e.g. VK7JG-NPHTM-C97JM-9MPGT-3V66T",
                "e.g. Microsoft"
            )
            SecretPickerType.SERIAL_NUMBER -> Tuple6(
                "Device / Item Name",
                "Serial Number",
                "Manufacturer / Brand",
                "e.g. MacBook Pro 16\"",
                "e.g. C02G1234MD6R",
                "e.g. Apple"
            )
            SecretPickerType.DEVICE_KEY -> Tuple6(
                "Hardware / Token Name",
                "Key / Secret / PIN",
                "Model / Slot",
                "e.g. YubiKey 5C NFC",
                "e.g. 123456 or FIDO2 PIN",
                "e.g. Slot 1 (Yubico)"
            )
            SecretPickerType.CLOUD_CREDENTIAL -> Tuple6(
                "Service / Environment",
                "Secret Access Key",
                "Account ID / Project",
                "e.g. AWS Production S3",
                "e.g. wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
                "e.g. 123456789012"
            )
            SecretPickerType.GAME_CODE -> Tuple6(
                "Game Title",
                "Activation Code / CD Key",
                "Platform / Store",
                "e.g. Elden Ring",
                "e.g. 3ABCD-EFGHI-JKLMN",
                "e.g. Steam / PlayStation / Xbox"
            )
            SecretPickerType.CERTIFICATE -> Tuple6(
                "Certificate Name",
                "Private Key / PEM Content",
                "Issuer / CA",
                "e.g. Internal Root CA Client",
                "Paste PEM or key payload...",
                "e.g. DigiCert / Let's Encrypt"
            )
            SecretPickerType.CUSTOM_SECRET -> Tuple6(
                "Secret Name",
                "Secret Value",
                "Category / Identifier",
                "e.g. Safe Combination",
                "e.g. 14-22-38-L-R",
                "e.g. Personal Security"
            )
            else -> Tuple6(
                "Title",
                "Secret Value",
                "Provider / Context",
                "e.g. My Secret",
                "e.g. Sensitive data",
                "e.g. Personal"
            )
        }
    }

    fun generateRandomKey(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = SecureRandom()
        return if (secretType == SecretPickerType.PRODUCT_KEY) {
            (1..5).map {
                (1..5).map { chars[random.nextInt(chars.length)] }.joinToString("")
            }.joinToString("-")
        } else {
            (1..24).map { chars[random.nextInt(chars.length)] }.joinToString("")
        }
    }

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
                modifier = Modifier
                    .size(32.dp)
                    .tactilePress { onDismiss() }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = getVaultTextPrimary(),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecretTypeThemedBadge(type = secretType, size = 36.dp)
                Text(
                    text = if (isEditing) "Edit ${secretType.displayName.lowercase()}" else "New ${secretType.displayName.lowercase()}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = getVaultTextPrimary()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                VaultUnderlineField(
                    label = titleLabel,
                    value = label,
                    onValueChange = {
                        label = it
                        if (errorMessage != null) errorMessage = null
                    },
                    placeholder = labelPlaceholder
                )

                VaultUnderlineField(
                    label = valueLabel,
                    value = secretValue,
                    onValueChange = {
                        secretValue = it
                        if (errorMessage != null) errorMessage = null
                    },
                    placeholder = valuePlaceholder,
                    visualTransformation = if (isValueVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingAction = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { isValueVisible = !isValueVisible },
                                modifier = Modifier
                                    .size(32.dp)
                                    .tactilePress { isValueVisible = !isValueVisible }
                            ) {
                                Icon(
                                    imageVector = if (isValueVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isValueVisible) "Hide value" else "Show value",
                                    tint = getVaultTextSecondary(),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { secretValue = generateRandomKey() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .tactilePress { secretValue = generateRandomKey() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Generate random key",
                                    tint = getVaultTextSecondary(),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                )

                VaultUnderlineField(
                    label = providerLabel,
                    value = provider,
                    onValueChange = { provider = it },
                    optional = true,
                    placeholder = providerPlaceholder
                )

                VaultUnderlineField(
                    label = "Notes",
                    value = notes,
                    onValueChange = { notes = it },
                    optional = true,
                    placeholder = "Additional references or recovery details"
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            VaultPillActionButton(
                text = if (isEditing) "Save changes" else "Add secret",
                onClick = {
                    if (label.isBlank()) {
                        errorMessage = "Please enter a $titleLabel"
                        return@VaultPillActionButton
                    }
                    if (secretValue.isBlank()) {
                        errorMessage = "Please enter $valueLabel"
                        return@VaultPillActionButton
                    }
                    onSave(
                        label.trim(),
                        secretValue.trim(),
                        secretType.name,
                        provider.trim().ifBlank { null },
                        notes.trim().ifBlank { null }
                    )
                },
                modifier = Modifier.tactilePress { }
            )
        }
    }
}

private data class Tuple6<A, B, C, D, E, F>(
    val a: A, val b: B, val c: C, val d: D, val e: E, val f: F
)
