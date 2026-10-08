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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.tactilePress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeyFormSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (label: String, key: String, service: String, environment: String, endpoint: String?, expires: String?) -> Unit,
    initialLabel: String = "",
    initialKey: String = "",
    initialService: String = "",
    initialEnvironment: String = "Production",
    initialEndpoint: String = "",
    initialExpires: String = "",
    isEditing: Boolean = false,
    modifier: Modifier = Modifier
) {
    var label by remember { mutableStateOf(initialLabel) }
    var key by remember { mutableStateOf(initialKey) }
    var service by remember { mutableStateOf(initialService) }
    var environment by remember { mutableStateOf(initialEnvironment) }
    var endpoint by remember { mutableStateOf(initialEndpoint) }
    var expires by remember { mutableStateOf(initialExpires) }
    var isKeyVisible by remember { mutableStateOf(false) }

    val environments = listOf("Dev", "Staging", "Production")

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
                text = if (isEditing) "Edit API key" else "New API key",
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
                    placeholder = "e.g. Stripe production key"
                )

                VaultUnderlineField(
                    label = "Key",
                    value = key,
                    onValueChange = { key = it },
                    placeholder = "sk_live_••••••••••••",
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingAction = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isKeyVisible) getVaultTextPrimary() else getVaultTextSecondary())
                                .clickable { isKeyVisible = !isKeyVisible }
                        )
                    }
                )

                VaultUnderlineField(
                    label = "Service or provider",
                    value = service,
                    onValueChange = { service = it },
                    placeholder = "e.g. Stripe"
                )

                // Environment selector pills
                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                    Text(
                        text = "Environment",
                        fontSize = 13.sp,
                        color = getVaultTextSecondary()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        environments.forEach { env ->
                            val isSelected = environment == env
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) getVaultPillButtonColor() else getVaultCardSurface(),
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, getVaultBorder()),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { environment = env }
                                    .tactilePress()
                            ) {
                                Text(
                                    text = env,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) getVaultPillTextColor() else getVaultTextPrimary(),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                VaultUnderlineField(
                    label = "Endpoint",
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    optional = true,
                    placeholder = "e.g. api.stripe.com/v1"
                )

                VaultUnderlineField(
                    label = "Expires",
                    value = expires,
                    onValueChange = { expires = it },
                    optional = true,
                    placeholder = "No expiry set"
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            VaultPillActionButton(
                text = "Save",
                enabled = label.isNotBlank() && key.isNotBlank(),
                onClick = {
                    onSave(label, key, service, environment, endpoint.takeIf { it.isNotBlank() }, expires.takeIf { it.isNotBlank() })
                    onDismiss()
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
