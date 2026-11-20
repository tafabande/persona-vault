package com.pims.vault.presentation.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pims.vault.core.model.ContactType
import com.pims.vault.core.model.InformationSensitivity
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.domain.model.PlainNote
import com.pims.vault.domain.model.VaultItemHeader
import com.pims.vault.presentation.notes.PlainNotesViewModel
import com.pims.vault.presentation.profile.ProfileUiState
import com.pims.vault.presentation.ui.components.PersonaAction
import com.pims.vault.presentation.ui.components.PersonaActionStyle
import com.pims.vault.presentation.ui.components.PersonaIconAction
import com.pims.vault.presentation.ui.components.PersonaInformationRow
import com.pims.vault.presentation.ui.components.PersonaMenu
import com.pims.vault.presentation.ui.components.PersonaMenuItem
import com.pims.vault.presentation.ui.components.PersonaSectionHeading
import com.pims.vault.presentation.ui.components.PersonaSheet
import com.pims.vault.presentation.ui.components.PersonaSheetHeader
import com.pims.vault.presentation.ui.theme.PersonaIcon
import com.pims.vault.presentation.ui.theme.PersonaIcons
import com.pims.vault.presentation.ui.theme.PimsSpacing
import com.pims.vault.presentation.ui.theme.pimsPageGutter

private data class OverviewItem(
    val pin: OverviewPin,
    val label: String,
    val icon: ImageVector,
    val value: String? = null,
    val requiresApproval: Boolean = false,
    val pickerLabel: String = label
)

private enum class OverviewPicker { Information, Vault }

@Composable
internal fun MeOverviewRoute(
    name: String,
    pinState: MeOverviewPinState,
    profile: ProfileUiState,
    vaultItems: List<VaultItemHeader>,
    isVaultUnlocked: Boolean,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onAuthorizeProfile: (() -> Unit) -> Unit,
    onAuthorizeVault: (() -> Unit) -> Unit,
    onOpenNote: (String) -> Unit,
    onOpenVault: (VaultItemHeader) -> Unit
) {
    val personId = profile.person?.id
    val notesViewModel: PlainNotesViewModel = hiltViewModel()
    val notes by notesViewModel.uiState.collectAsStateWithLifecycle()
    key(pinState) {
        MeOverview(
            name = name,
            pinState = pinState,
            profile = profile,
            notes = notes.notes.filter { it.ownerPersonId == personId },
            vaultItems = vaultItems.filter { it.personId == personId },
            isVaultUnlocked = isVaultUnlocked,
            onSettings = onSettings,
            onProfile = onProfile,
            onAuthorizeProfile = onAuthorizeProfile,
            onAuthorizeVault = onAuthorizeVault,
            onOpenNote = onOpenNote,
            onOpenVault = onOpenVault
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeOverview(
    name: String,
    pinState: MeOverviewPinState,
    profile: ProfileUiState,
    notes: List<PlainNote>,
    vaultItems: List<VaultItemHeader>,
    isVaultUnlocked: Boolean,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onAuthorizeProfile: (() -> Unit) -> Unit,
    onAuthorizeVault: (() -> Unit) -> Unit,
    onOpenNote: (String) -> Unit,
    onOpenVault: (VaultItemHeader) -> Unit
) {
    var picker by remember { mutableStateOf<OverviewPicker?>(null) }
    var accessRequest by remember { mutableStateOf<OverviewPicker?>(null) }
    var profileAuthorized by remember { mutableStateOf(false) }
    var activeMenu by remember { mutableStateOf<OverviewPin?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        profileAuthorized = false
        picker = null
        activeMenu = null
    }
    LaunchedEffect(isVaultUnlocked) {
        if (!isVaultUnlocked && picker == OverviewPicker.Vault) picker = null
    }
    LaunchedEffect(accessRequest) {
        val requested = accessRequest ?: return@LaunchedEffect
        accessRequest = null
        when (requested) {
            OverviewPicker.Vault -> onAuthorizeVault { picker = OverviewPicker.Vault }
            OverviewPicker.Information -> onAuthorizeProfile {
                profileAuthorized = true
                picker = OverviewPicker.Information
            }
        }
    }

    val items = remember(profile, notes, vaultItems, profileAuthorized, isVaultUnlocked) {
        buildOverviewItems(profile, notes, vaultItems, profileAuthorized, isVaultUnlocked)
    }
    val itemsByPin = remember(items) { items.associateBy { it.pin.key } }
    val pinned = pinState.pins.orEmpty()
    val gutter = pimsPageGutter()
    val screen = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = gutter, end = gutter, top = PimsSpacing.m, bottom = PimsSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(PimsSpacing.xs)
    ) {
        item(key = "me-header") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Me", style = MaterialTheme.typography.headlineLarge)
                    Text(name, color = screen.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                }
                PersonaIconAction(PersonaIcons.SettingsOutlined, "Settings", onSettings)
            }
        }
        item(key = "me-profile") {
            PersonaInformationRow("Profile", PersonaIcons.MeOutlined, onClick = onProfile)
        }
        item(key = "me-pinned") {
            PersonaSectionHeading(
                "Pinned information",
                modifier = Modifier.padding(top = PimsSpacing.m),
                onAdd = if (pinState.pins != null) ({ picker = OverviewPicker.Information }) else null
            )
        }
        if (pinState.loading || pinState.saving) item(key = "me-progress") { CircularProgressIndicator() }
        pinState.error?.let { message ->
            item(key = "me-error") {
                Text(message, color = screen.error)
                if (pinState.pins == null) {
                    PersonaAction("Try again", pinState::load, style = PersonaActionStyle.Quiet)
                }
            }
        }
        if (!pinState.loading && pinned.isEmpty() && pinState.error == null) {
            item(key = "me-empty") {
                Text("Pin information you want to find quickly.", color = screen.onSurfaceVariant)
            }
        }
        itemsIndexed(pinned, key = { _, pin -> pin.key }) { index, pin ->
            val item = itemsByPin[pin.key]
            val needsApproval = item?.requiresApproval == true && !pin.showValue
            val approveDisplay: (() -> Unit)? = if (needsApproval) ({
                onAuthorizeProfile {
                    pinState.update { current -> current.map { if (it.key == pin.key) it.copy(showValue = true) else it } }
                }
            }) else null
            val onOpen = when (pin.source) {
                OverviewSource.Vault -> vaultItems.firstOrNull { it.id == pin.id }?.let { header -> { onOpenVault(header) } }
                OverviewSource.Note -> notes.firstOrNull { it.id == pin.id }?.let { record -> { onOpenNote(record.id) } }
                else -> approveDisplay
            }
            PersonaInformationRow(
                title = when {
                    needsApproval && pin.source == OverviewSource.Custom -> "Protected information"
                    item != null -> item.label
                    pin.source == OverviewSource.Vault -> "Vault item"
                    else -> "Unavailable information"
                },
                icon = item?.icon ?: PersonaIcons.LockOutlined,
                detail = if (needsApproval) "Show on Me" else item?.value,
                onClick = onOpen,
                trailing = {
                    Box {
                        PersonaIconAction(
                            PersonaIcons.MoreActions, "Pin options", { activeMenu = pin }, enabled = !pinState.saving
                        )
                        PersonaMenu(expanded = activeMenu == pin, onDismissRequest = { activeMenu = null }) {
                            if (approveDisplay != null) {
                                PersonaMenuItem("Show on Me", PersonaIcons.Visibility, onClick = {
                                    activeMenu = null
                                    approveDisplay()
                                })
                            }
                            PersonaMenuItem("Move up", PersonaIcons.MoveUp, enabled = index > 0, onClick = {
                                activeMenu = null
                                pinState.update { it.movePin(pin, -1) }
                            })
                            PersonaMenuItem("Move down", PersonaIcons.MoveDown, enabled = index < pinned.lastIndex, onClick = {
                                activeMenu = null
                                pinState.update { it.movePin(pin, 1) }
                            })
                            PersonaMenuItem("Unpin", PersonaIcons.Close, onClick = {
                                activeMenu = null
                                pinState.update { it - pin }
                            })
                        }
                    }
                }
            )
        }
    }
    if (picker == OverviewPicker.Information) {
        PersonaSheet(onDismissRequest = { picker = null }, sheetState = rememberModalBottomSheetState()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = gutter)) {
                PersonaSheetHeader("Pin information", { picker = null })
                LazyColumn(contentPadding = PaddingValues(bottom = PimsSpacing.xl)) {
                    item {
                        PersonaInformationRow("Vault items", PersonaIcons.LockOutlined, onClick = {
                            if (isVaultUnlocked) {
                                picker = OverviewPicker.Vault
                            } else {
                                picker = null
                                accessRequest = OverviewPicker.Vault
                            }
                        })
                    }
                    if (!profileAuthorized) {
                        item {
                            PersonaInformationRow("Show sensitive fields", PersonaIcons.LockOutlined, onClick = {
                                picker = null
                                accessRequest = OverviewPicker.Information
                            })
                        }
                    }
                    val pinnedKeys = pinned.mapTo(hashSetOf()) { it.key }
                    val available = items.filter {
                        it.pin.key !in pinnedKeys && when {
                            it.pin.source == OverviewSource.Vault -> false
                            it.requiresApproval -> profileAuthorized
                            it.pin.source == OverviewSource.Note && it.value == null -> profileAuthorized
                            else -> it.value != null
                        }
                    }
                    itemsIndexed(available, key = { _, item -> item.pin.key }) { _, item ->
                        PersonaInformationRow(
                            item.pickerLabel, item.icon,
                            detail = item.value,
                            onClick = if (pinState.saving) null else ({
                                pinState.update { current ->
                                    current + item.pin.copy(showValue = item.requiresApproval)
                                }
                            }),
                            trailing = { PersonaIcon(PersonaIcons.Add, null) }
                        )
                    }
                    if (available.isEmpty() && profileAuthorized) {
                        item { Text("No more information to pin.", style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }
    }
    if (picker == OverviewPicker.Vault) {
        VaultMePinPicker(pinState, vaultItems, isVaultUnlocked, onDismiss = { picker = null })
    }
}
private fun buildOverviewItems(
    profile: ProfileUiState,
    notes: List<PlainNote>,
    vaultItems: List<VaultItemHeader>,
    profileAuthorized: Boolean,
    vaultAuthorized: Boolean
): List<OverviewItem> = buildList {
    fun addValue(source: OverviewSource, id: String, label: String, icon: ImageVector, value: String?, requiresApproval: Boolean = false) {
        if (!value.isNullOrBlank()) {
            add(OverviewItem(OverviewPin(source, id), label, icon, value, requiresApproval))
        }
    }
    profile.person?.let { person ->
        val legalName = listOfNotNull(person.firstName, person.middleName, person.lastName)
            .filter(String::isNotBlank).joinToString(" ")
        addValue(OverviewSource.Profile, "legal-name", "Legal name", PersonaIcons.Me, legalName)
        addValue(OverviewSource.Profile, "occupation", "Occupation", PersonaIcons.Work, person.occupation)
        addValue(OverviewSource.Profile, "nationality", "Nationality", PersonaIcons.Me, person.nationality)
        addValue(OverviewSource.Profile, "residence", "Country of residence", PersonaIcons.Location, person.countryOfResidence)
        addValue(OverviewSource.Profile, "birthday", "Date of birth", PersonaIcons.Calendar, person.dateOfBirth, requiresApproval = true)
        addValue(OverviewSource.Profile, "national-id", "National ID", PersonaIcons.Identity, person.nationalIdNumber, requiresApproval = true)
    }
    profile.contacts.forEach { contact ->
        val (type, icon) = when (contact.contactType) {
            ContactType.EMAIL -> "Email" to PersonaIcons.Email
            ContactType.PHONE -> "Phone" to PersonaIcons.Phone
            ContactType.MESSAGING -> "Messaging" to PersonaIcons.NotesOutlined
            ContactType.OTHER -> "Contact" to PersonaIcons.Me
        }
        val label = if (contact.label.isNotBlank()) "$type (${contact.label})" else type
        addValue(OverviewSource.Contact, contact.id, label, icon, contact.value)
    }
    profile.addresses.forEach { address ->
        val value = listOfNotNull(address.streetLine1, address.streetLine2, address.city, address.stateProvince,
            address.postalCode, address.country).filter(String::isNotBlank).joinToString(", ")
        addValue(OverviewSource.Address, address.id, "Address", PersonaIcons.Location, value)
    }
    profile.customFields.forEach { field ->
        val sensitivity = maxOf(field.sensitivity, field.category.defaultSensitivity)
        if (sensitivity != InformationSensitivity.HIGHLY_PROTECTED) {
            addValue(OverviewSource.Custom, field.id, field.label, PersonaIcons.Bookmark, field.value,
                requiresApproval = sensitivity.requiresAuthToReveal)
        }
    }
    (profile.educationRecords + profile.certificates).distinctBy { it.id }.forEach { record ->
        addValue(OverviewSource.Education, record.id, record.qualification, PersonaIcons.Education, record.institution)
    }
    profile.employmentRecords.forEach { record ->
        addValue(OverviewSource.Employment, record.id, record.position, PersonaIcons.Work, record.company)
    }
    notes.forEach { note ->
        val title = note.title.ifBlank { "Untitled note" }
        if (note.isLocked) {
            add(OverviewItem(OverviewPin(OverviewSource.Note, note.id), "Protected note", PersonaIcons.LockOutlined,
                pickerLabel = if (profileAuthorized) title else "Protected note"))
        } else {
            addValue(OverviewSource.Note, note.id, title, PersonaIcons.NotesOutlined, note.content.ifBlank { "Open note" })
        }
    }
    vaultItems.forEach { item ->
        val pin = OverviewPin(OverviewSource.Vault, item.id)
        if (item.category == VaultCategory.PAYMENT_REFERENCE) {
            add(OverviewItem(pin, "Payment card", PersonaIcons.CreditCard, maskedCardPreview(item.accountIdentifier),
                pickerLabel = if (vaultAuthorized) item.title.ifBlank { "Payment card" } else "Payment card"))
        } else {
            add(OverviewItem(
                pin,
                if (vaultAuthorized) item.title.ifBlank { "Vault item" } else "Protected Vault item",
                PersonaIcons.LockOutlined,
                if (vaultAuthorized) vaultOverviewLabel(item) else null
            ))
        }
    }
}
