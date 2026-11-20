package com.pims.vault.presentation.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.domain.model.VaultIdentityType
import com.pims.vault.domain.model.VaultItemHeader
import com.pims.vault.presentation.ui.components.PersonaAction
import com.pims.vault.presentation.ui.components.PersonaActionStyle
import com.pims.vault.presentation.ui.components.PersonaInformationRow
import com.pims.vault.presentation.ui.components.PersonaSheet
import com.pims.vault.presentation.ui.components.PersonaSheetHeader
import com.pims.vault.presentation.ui.theme.PersonaIcons
import com.pims.vault.presentation.ui.theme.PimsSpacing
import com.pims.vault.presentation.ui.theme.pimsPageGutter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VaultMePinPicker(
    pinState: MeOverviewPinState,
    vaultItems: List<VaultItemHeader>,
    isVaultUnlocked: Boolean,
    onDismiss: () -> Unit
) {
    if (!isVaultUnlocked) return
    val ownedItems = remember(vaultItems, pinState.ownerId) {
        vaultItems.filter { item -> item.personId == pinState.ownerId }
    }
    val pinned = pinState.pins
    val pinnedKeys = remember(pinned) { pinned.orEmpty().mapTo(hashSetOf()) { it.key } }
    PersonaSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = pimsPageGutter()),
            verticalArrangement = Arrangement.spacedBy(PimsSpacing.s)
        ) {
            PersonaSheetHeader("Pin from Vault", onDismiss)
            pinState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            when {
                pinState.loading -> CircularProgressIndicator(modifier = Modifier.padding(PimsSpacing.m))
                pinned == null && pinState.error != null -> PersonaAction(
                    "Try again", pinState::load, style = PersonaActionStyle.Quiet
                )
                pinned == null -> Text(
                    "Pinning is available when your profile loads.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = PimsSpacing.xl)
                )
                ownedItems.isEmpty() -> Text(
                    "No Vault items to pin yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = PimsSpacing.xl)
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(bottom = PimsSpacing.xl),
                    verticalArrangement = Arrangement.spacedBy(PimsSpacing.s)
                ) {
                    items(ownedItems, key = { it.id }) { item ->
                        val pin = OverviewPin(OverviewSource.Vault, item.id)
                        val isPinned = pin.key in pinnedKeys
                        val isCard = item.category == VaultCategory.PAYMENT_REFERENCE
                        val title = item.title.ifBlank { vaultOverviewLabel(item) }
                        val label = if (isPinned) "Unpin" else "Pin"
                        PersonaInformationRow(
                            title = title,
                            icon = if (isCard) PersonaIcons.CreditCard else PersonaIcons.LockOutlined,
                            detail = if (isCard) maskedCardPreview(item.accountIdentifier) ?: "Payment card"
                                else vaultOverviewLabel(item),
                            trailing = {
                                PersonaAction(
                                    label,
                                    onClick = {
                                        pinState.update { current ->
                                            if (isPinned) current.filterNot { it.key == pin.key } else current + pin
                                        }
                                    },
                                    modifier = Modifier.semantics { contentDescription = "$label $title" },
                                    style = if (isPinned) PersonaActionStyle.Text else PersonaActionStyle.Quiet,
                                    icon = if (isPinned) PersonaIcons.Check else PersonaIcons.PushPin,
                                    enabled = !pinState.saving
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

internal fun vaultOverviewLabel(item: VaultItemHeader): String =
    when (item.category) {
        VaultCategory.IDENTITY_CREDENTIAL -> VaultIdentityType.fromHeader(item).label
        VaultCategory.PAYMENT_REFERENCE -> "Payment card"
        else -> item.category.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
