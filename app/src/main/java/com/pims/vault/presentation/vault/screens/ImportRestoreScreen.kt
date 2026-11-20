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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun ImportRestoreScreen(
    onRestoreEncryptedBackup: () -> Unit,
    onImportExportedVault: () -> Unit,
    onMigrateFromAnotherVault: () -> Unit,
    onUseRecoveryCode: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Import & restore",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = getVaultTextPrimary()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Actions Grouped Card
                VaultGroupedCard {
                    ImportOptionRow(
                        title = "Restore backup",
                        subtitle = "Restore data from cloud or local backup archive",
                        icon = PersonaIcons.CloudDownload,
                        onClick = onRestoreEncryptedBackup
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))

                    ImportOptionRow(
                        title = "Import exported vault",
                        subtitle = "Load items from a compatible exported vault file",
                        icon = PersonaIcons.FileUpload,
                        onClick = onImportExportedVault
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))

                    ImportOptionRow(
                        title = "Migrate from another vault",
                        subtitle = "Transfer passwords and credentials from other services",
                        icon = PersonaIcons.Sync,
                        onClick = onMigrateFromAnotherVault
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))

                    ImportOptionRow(
                        title = "Use a recovery code",
                        subtitle = "Enter your master recovery key to regain vault access",
                        icon = PersonaIcons.LockReset,
                        onClick = onUseRecoveryCode
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Information Box: About secret vault
                VaultGroupedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = PersonaIcons.TrustedContact,
                                contentDescription = null,
                                tint = getVaultTextPrimary(),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "About secret vault",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = getVaultTextPrimary()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Backups and exports are verified locally with your master keys. Only import and restore vault files from trusted sources.",
                            fontSize = 13.sp,
                            color = getVaultTextSecondary(),
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ImportOptionRow(
    title: String,
    subtitle: String = "",
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = getVaultTextSecondary(),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = getVaultTextPrimary()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = getVaultTextSecondary()
            )
        }
        Icon(
            imageVector = PersonaIcons.ChevronRight,
            contentDescription = null,
            tint = getVaultTextSecondary().copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}
