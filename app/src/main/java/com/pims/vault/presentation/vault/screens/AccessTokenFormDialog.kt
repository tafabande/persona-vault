package com.pims.vault.presentation.vault.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.tactilePress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessTokenFormSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (label: String, token: String, service: String, scopes: List<String>, expires: String?) -> Unit,
    initialLabel: String = "",
    initialToken: String = "",
    initialService: String = "",
    initialScopes: List<String> = emptyList(),
    initialExpires: String = "",
    isEditing: Boolean = false,
    modifier: Modifier = Modifier
) {
    var label by remember { mutableStateOf(initialLabel) }
    var token by remember { mutableStateOf(initialToken) }
    var service by remember { mutableStateOf(initialService) }
    var selectedScopes by remember { mutableStateOf(initialScopes.toSet()) }
    var expires by remember { mutableStateOf(initialExpires) }
    var isTokenVisible by remember { mutableStateOf(false) }

    val defaultScopes = listOf("repo", "workflow", "read:user", "admin:org", "gist", "notifications")

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
                text = if (isEditing) "Edit access token" else "New access token",
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
                    placeholder = "e.g. GitHub personal access token"
                )

                VaultUnderlineField(
                    label = "Token",
                    value = token,
                    onValueChange = { token = it },
                    placeholder = "ghp_••••••••••••",
                    visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingAction = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isTokenVisible) getVaultTextPrimary() else getVaultTextSecondary())
                                .clickable { isTokenVisible = !isTokenVisible }
                        )
                    }
                )

                VaultUnderlineField(
                    label = "Service",
                    value = service,
                    onValueChange = { service = it },
                    placeholder = "e.g. GitHub"
                )

                // Scopes selector chips
                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                    Text(
                        text = "Scopes",
                        fontSize = 13.sp,
                        color = getVaultTextSecondary()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        defaultScopes.forEach { scope ->
                            val isSelected = selectedScopes.contains(scope)
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) getVaultPillButtonColor() else getVaultCardSurface(),
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, getVaultBorder()),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        selectedScopes = if (isSelected) {
                                            selectedScopes - scope
                                        } else {
                                            selectedScopes + scope
                                        }
                                    }
                                    .tactilePress()
                            ) {
                                Text(
                                    text = scope,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) getVaultPillTextColor() else getVaultTextPrimary(),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }

                VaultUnderlineField(
                    label = "Expires",
                    value = expires,
                    onValueChange = { expires = it },
                    optional = true,
                    placeholder = "e.g. 7 Jan 2027"
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            VaultPillActionButton(
                text = "Save",
                enabled = label.isNotBlank() && token.isNotBlank(),
                onClick = {
                    onSave(label, token, service, selectedScopes.toList(), expires.takeIf { it.isNotBlank() })
                    onDismiss()
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
