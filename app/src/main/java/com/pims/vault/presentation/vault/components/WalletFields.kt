package com.pims.vault.presentation.vault.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pims.vault.domain.model.VaultItemHeader
import com.pims.vault.presentation.ui.components.PersonaTextInput
import com.pims.vault.presentation.vault.card.CardSkinCatalogue

@Composable
internal fun BankNameInput(value: String, onValueChange: (String) -> Unit, knownNames: List<String> = emptyList(), enabled: Boolean = true) {
    var showSuggestions by remember { mutableStateOf(false) }
    val suggestions = remember(value, knownNames) {
        (knownNames + CardSkinCatalogue.SKINS.filterNot { it.id.startsWith("default") }.map { it.name })
            .distinctBy { it.lowercase() }
            .filter { value.isNotBlank() && it.contains(value.trim(), true) && !it.equals(value.trim(), true) }
            .take(4)
    }
    Column {
        PersonaTextInput(value = value, onValueChange = { onValueChange(it); showSuggestions = true }, label = "Bank or provider", enabled = enabled)
        if (showSuggestions) suggestions.forEach { bank ->
            TextButton(enabled = enabled, onClick = { onValueChange(bank); showSuggestions = false }) { Text(bank) }
        }
    }
}

@Composable
internal fun WalletLinksPicker(
    title: String,
    records: List<VaultItemHeader>,
    selectedIds: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    enabled: Boolean = true
) {
    if (records.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        records.forEach { record ->
            val selected = record.id in selectedIds
            Row(
                modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) {
                    onSelectionChange(if (selected) selectedIds - record.id else selectedIds + record.id)
                }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = selected, onCheckedChange = null, enabled = enabled)
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(record.title, style = MaterialTheme.typography.bodyLarge)
                    record.accountIdentifier?.takeIf { it.isNotBlank() && it != record.title }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
