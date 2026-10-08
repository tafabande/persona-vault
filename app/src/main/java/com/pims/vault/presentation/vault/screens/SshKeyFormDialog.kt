package com.pims.vault.presentation.vault.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.MessageDigest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SshKeyFormSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (label: String, host: String, username: String, privateKey: String, publicKey: String, fingerprint: String) -> Unit,
    initialLabel: String = "",
    initialHost: String = "",
    initialUsername: String = "",
    initialPrivateKey: String = "",
    initialPublicKey: String = "",
    initialFingerprint: String = "",
    isEditing: Boolean = false,
    modifier: Modifier = Modifier
) {
    var label by remember { mutableStateOf(initialLabel) }
    var host by remember { mutableStateOf(initialHost) }
    var username by remember { mutableStateOf(initialUsername) }
    var privateKey by remember { mutableStateOf(initialPrivateKey) }
    var publicKey by remember { mutableStateOf(initialPublicKey) }
    var fingerprint by remember { mutableStateOf(initialFingerprint) }
    var isPrivateRevealed by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        activity?.window?.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    fun calculateFingerprint(keyStr: String): String {
        if (keyStr.isBlank()) return "SHA256:---"
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(keyStr.toByteArray())
            "SHA256:" + android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING).take(20) + "..."
        } catch (_: Exception) {
            "SHA256:unknown"
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

            Text(
                text = if (isEditing) "Edit SSH key" else "New SSH key",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = getVaultTextPrimary()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                VaultUnderlineField(
                    label = "Label",
                    value = label,
                    onValueChange = { label = it },
                    placeholder = "e.g. Deploy server"
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        VaultUnderlineField(
                            label = "Host",
                            value = host,
                            onValueChange = { host = it },
                            placeholder = "e.g. deploy.myapp.com"
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        VaultUnderlineField(
                            label = "Username",
                            value = username,
                            onValueChange = { username = it },
                            placeholder = "e.g. deploy"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Private key rounded card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, getVaultBorder(), RoundedCornerShape(16.dp)),
                    color = getVaultCardSurface()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(getVaultTextPrimary())
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Private key",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = getVaultTextPrimary()
                                )
                                Text(
                                    text = if (privateKey.isBlank()) "No private key pasted" else if (isPrivateRevealed) privateKey.take(30) + "..." else "RSA 4096-bit · hidden",
                                    fontSize = 13.sp,
                                    color = getVaultTextSecondary(),
                                    fontFamily = if (isPrivateRevealed) FontFamily.Monospace else FontFamily.Default
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (isPrivateRevealed) getVaultTextPrimary() else getVaultTextSecondary())
                                    .clickable { isPrivateRevealed = !isPrivateRevealed }
                            )
                        }
                        if (isPrivateRevealed) {
                            Spacer(modifier = Modifier.height(8.dp))
                            VaultUnderlineField(
                                label = "Paste Private Key",
                                value = privateKey,
                                onValueChange = {
                                    privateKey = it
                                    if (fingerprint.isBlank()) fingerprint = calculateFingerprint(it)
                                },
                                placeholder = "-----BEGIN OPENSSH PRIVATE KEY-----..."
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Public key rounded card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, getVaultBorder(), RoundedCornerShape(16.dp)),
                    color = getVaultCardSurface()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(getVaultTextPrimary())
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Public key",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = getVaultTextPrimary()
                                )
                                Text(
                                    text = if (publicKey.isBlank()) "ssh-ed25519 or ssh-rsa..." else publicKey.take(45) + "...",
                                    fontSize = 12.sp,
                                    color = getVaultTextSecondary(),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        VaultUnderlineField(
                            label = "Public key text",
                            value = publicKey,
                            onValueChange = {
                                publicKey = it
                                fingerprint = calculateFingerprint(it)
                            },
                            placeholder = "ssh-rsa AAAA..."
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Fingerprint rounded card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, getVaultBorder(), RoundedCornerShape(16.dp)),
                    color = getVaultCardSurface()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Fingerprint",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = getVaultTextPrimary()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (fingerprint.isBlank()) calculateFingerprint(publicKey.ifBlank { privateKey }) else fingerprint,
                            fontSize = 13.sp,
                            color = getVaultTextSecondary(),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            VaultPillActionButton(
                text = "Save",
                enabled = label.isNotBlank() && (privateKey.isNotBlank() || publicKey.isNotBlank()),
                onClick = {
                    val finalFp = if (fingerprint.isBlank()) calculateFingerprint(publicKey.ifBlank { privateKey }) else fingerprint
                    onSave(label, host, username, privateKey, publicKey, finalFp)
                    onDismiss()
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
