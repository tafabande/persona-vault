package com.pims.vault.presentation.vault.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.domain.model.VaultIdentityType
import com.pims.vault.domain.model.VaultItemHeader
import com.pims.vault.presentation.ui.components.PersonaIconAction
import com.pims.vault.presentation.ui.components.PersonaFilterChip
import com.pims.vault.presentation.ui.components.PersonaInformationRow
import com.pims.vault.presentation.ui.components.PersonaSectionHeading
import com.pims.vault.presentation.ui.components.PersonaTextInput
import com.pims.vault.presentation.ui.state.PersonaEmptyState
import com.pims.vault.presentation.ui.theme.PersonaIcon
import com.pims.vault.presentation.ui.theme.PersonaIcons
import com.pims.vault.presentation.ui.theme.PersonaIconSize
import com.pims.vault.presentation.ui.theme.PimsSpacing
import com.pims.vault.presentation.ui.theme.pimsPageGutter

@Composable
fun IdMembershipListScreen(
    initialCategory: String = "All",
    items: List<VaultItemHeader>,
    onOpenItem: (VaultItemHeader) -> Unit,
    onAddNew: (String) -> Unit,
    onBack: () -> Unit,
    onSelectCategory: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedType = VaultIdentityType.fromValue(initialCategory)
    var query by rememberSaveable { mutableStateOf("") }
    val groups = items.asSequence()
        .filter { it.category == VaultCategory.IDENTITY_CREDENTIAL }
        .map { VaultIdentityType.fromHeader(it) to it }
        .filter { (type, item) ->
            (selectedType == null || selectedType == type) &&
                (item.title.contains(query.trim(), ignoreCase = true) || type.label.contains(query.trim(), ignoreCase = true))
        }
        .groupBy({ it.first }, { it.second })

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = pimsPageGutter()),
        verticalArrangement = Arrangement.spacedBy(PimsSpacing.s)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PersonaIconAction(PersonaIcons.ArrowBack, "Back", onBack)
            Text(
                "IDs & memberships",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            PersonaIconAction(
                PersonaIcons.Add,
                "Add ID",
                onClick = { onAddNew((selectedType ?: VaultIdentityType.NATIONAL_ID).name) }
            )
        }

        PersonaTextInput(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search IDs",
            leadingIcon = { PersonaIcon(PersonaIcons.Search, null, size = PersonaIconSize.Small) },
            trailingIcon = {
                if (query.isNotEmpty()) PersonaIconAction(PersonaIcons.Close, "Clear search", { query = "" })
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(PimsSpacing.s)
        ) {
            PersonaFilterChip("All", selectedType == null, { onSelectCategory("All") })
            VaultIdentityType.entries.forEach { type ->
                PersonaFilterChip(type.label, selectedType == type, { onSelectCategory(type.name) })
            }
        }

        if (groups.isEmpty()) {
            PersonaEmptyState(
                icon = PersonaIcons.Documents,
                title = if (query.isNotBlank()) "No matching IDs" else "No IDs saved",
                actionLabel = if (query.isBlank()) "Add ID" else null,
                onActionClick = { onAddNew((selectedType ?: VaultIdentityType.NATIONAL_ID).name) }
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                VaultIdentityType.entries.forEach { type ->
                    val records = groups[type].orEmpty()
                    if (records.isNotEmpty()) {
                        item(key = type.name) {
                            PersonaSectionHeading(
                                title = type.label,
                                modifier = Modifier.padding(top = PimsSpacing.m, bottom = PimsSpacing.xs)
                            )
                        }
                        items(records, key = { it.id }) { item ->
                            PersonaInformationRow(
                                title = item.title,
                                icon = PersonaIcons.Documents,
                                onClick = { onOpenItem(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}
