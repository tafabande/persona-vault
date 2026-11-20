package com.pims.vault.presentation.hub

import com.pims.vault.presentation.ui.theme.PersonaIcons

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.sync.ConflictResolutionChoice
import com.pims.vault.core.sync.STRUCTURED_CONFLICT
import com.pims.vault.presentation.ui.components.PersonaAction
import com.pims.vault.presentation.ui.components.PersonaActionStyle
import com.pims.vault.data.local.entity.SyncConflictEntity
import com.pims.vault.presentation.ui.theme.PimsColors.error as StateError
import com.pims.vault.presentation.ui.theme.PimsColors.warning as StateWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncConflictSheet(
    conflicts: List<SyncConflictEntity>,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onResolveConflict: (conflictId: String, choice: ConflictResolutionChoice, resolvedValue: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val distinctConflicts = androidx.compose.runtime.remember(conflicts) {
        conflicts.distinctBy { "${it.entityType}_${it.entityId}_${it.fieldName}" }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = com.pims.vault.presentation.ui.theme.pimsPageGutter())
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = StateWarning.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PersonaIcons.Warning,
                            contentDescription = null,
                            tint = StateWarning,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Conflict Resolution",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (distinctConflicts.size == 1) "1 change needs your review" else "${distinctConflicts.size} changes need your review",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(PersonaIcons.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (distinctConflicts.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = PersonaIcons.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No conflicts",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "No conflicts detected between this device and the server.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(distinctConflicts, key = { it.id }) { conflict ->
                        ConflictCard(
                            conflict = conflict,
                            onKeepLocal = { onResolveConflict(conflict.id, ConflictResolutionChoice.KEEP_LOCAL, conflict.localValue) },
                            onUseRemote = { onResolveConflict(conflict.id, ConflictResolutionChoice.USE_REMOTE, conflict.remoteValue) },
                            onMerge = {
                                val merged = "${conflict.localValue} / ${conflict.remoteValue}"
                                onResolveConflict(conflict.id, ConflictResolutionChoice.MERGED, merged)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConflictCard(
    conflict: SyncConflictEntity,
    onKeepLocal: () -> Unit,
    onUseRemote: () -> Unit,
    onMerge: () -> Unit
) {
    val isNote = conflict.entityType == "PLAIN_NOTE" || conflict.entityType == "Note"
    val isStructuredRecord = conflict.entityType == STRUCTURED_CONFLICT
    val isRevisionConflict = isNote || isStructuredRecord
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = StateWarning.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Target Field & Notice
            Column {
                Text(
                    text = if (isNote) "Note changed elsewhere"
                        else if (isStructuredRecord) when (conflict.fieldName) {
                            "CONTACT" -> "Contact changed elsewhere"
                            "ADDRESS" -> "Address changed elsewhere"
                            "EDUCATION" -> "Education record changed elsewhere"
                            "EMPLOYMENT" -> "Work record changed elsewhere"
                            "MEDICAL_RECORD" -> "Medical record changed elsewhere"
                            else -> "Record changed elsewhere"
                        }
                        else "${conflict.entityType.lowercase().replaceFirstChar { it.uppercase() }} ${conflict.fieldName} changed elsewhere",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "For safety, this record was not automatically overwritten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Side-by-side comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Local version
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (isRevisionConflict) "This device" else "This device (v${conflict.localVersion})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isNote) "Saved on this device" else conflict.localValue.ifBlank { "(empty)" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Remote version
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (isRevisionConflict) "Other device" else "Other device (v${conflict.serverVersion})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = StateWarning
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isNote && conflict.remoteValue != "Deleted on another device") "Saved on another device"
                                else conflict.remoteValue.ifBlank { "(empty)" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Three Action Choices
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PersonaAction(
                    label = "Keep this",
                    onClick = onKeepLocal,
                    style = PersonaActionStyle.Quiet,
                    modifier = Modifier.weight(1f)
                )

                PersonaAction(
                    label = "Use other",
                    onClick = onUseRemote,
                    modifier = Modifier.weight(1f)
                )

                if (!isRevisionConflict) PersonaAction(
                    label = "Merge",
                    onClick = onMerge,
                    style = PersonaActionStyle.Text,
                    modifier = Modifier.weight(0.9f)
                )
            }
        }
    }
}
