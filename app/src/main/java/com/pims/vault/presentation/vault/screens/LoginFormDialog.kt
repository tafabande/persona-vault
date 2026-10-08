package com.pims.vault.presentation.vault.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
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
import java.security.SecureRandom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginFormSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (service: String, website: String, username: String, password: String, notes: String) -> Unit,
    initialService: String = "",
    initialWebsite: String = "",
    initialUsername: String = "",
    initialPassword: String = "",
    initialNotes: String = "",
    isEditing: Boolean = false,
    modifier: Modifier = Modifier
) {
    var service by remember { mutableStateOf(initialService) }
    var website by remember { mutableStateOf(initialWebsite) }
    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf(initialPassword) }
    var notes by remember { mutableStateOf(initialNotes) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    fun generateRandomPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+"
        val random = SecureRandom()
        return (1..18).map { chars[random.nextInt(chars.length)] }.joinToString("")
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
                text = if (isEditing) "Edit login" else "New login",
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
                    label = "Service",
                    value = service,
                    onValueChange = { service = it },
                    placeholder = "e.g. Gmail"
                )

                VaultUnderlineField(
                    label = "Website",
                    value = website,
                    onValueChange = { website = it },
                    optional = true,
                    placeholder = "e.g. mail.google.com"
                )

                VaultUnderlineField(
                    label = "Username or email",
                    value = username,
                    onValueChange = { username = it },
                    placeholder = "e.g. name@example.com"
                )

                VaultUnderlineField(
                    label = "Password",
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "••••••••••••",
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingAction = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Password Generator Dot
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(getVaultTextSecondary().copy(alpha = 0.5f))
                                    .clickable {
                                        password = generateRandomPassword()
                                        isPasswordVisible = true
                                    }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            // Password Reveal Dot
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (isPasswordVisible) getVaultTextPrimary() else getVaultTextSecondary())
                                    .clickable {
                                        isPasswordVisible = !isPasswordVisible
                                    }
                            )
                        }
                    }
                )

                VaultUnderlineField(
                    label = "Notes",
                    value = notes,
                    onValueChange = { notes = it },
                    optional = true,
                    placeholder = "Add a note"
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            VaultPillActionButton(
                text = "Save",
                enabled = service.isNotBlank() && password.isNotEmpty(),
                onClick = {
                    onSave(service, website, username, password, notes)
                    onDismiss()
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
