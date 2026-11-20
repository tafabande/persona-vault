package com.pims.vault.presentation.library

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.pims.vault.core.model.InformationCategory
import com.pims.vault.core.model.InformationSensitivity
import com.pims.vault.domain.model.normalizedNoteTag
import com.pims.vault.presentation.document.DocumentUiState
import com.pims.vault.presentation.medical.MedicalUiState
import com.pims.vault.presentation.notes.PlainNotesViewModel
import com.pims.vault.presentation.profile.ProfileUiState
import com.pims.vault.presentation.relationship.PublicRelationshipNotesViewModel
import com.pims.vault.presentation.vault.VaultUiState
import com.pims.vault.presentation.ui.components.PersonaAction
import com.pims.vault.presentation.ui.components.PersonaActionStyle
import com.pims.vault.presentation.ui.components.PersonaIconAction
import com.pims.vault.presentation.ui.components.PersonaInformationRow
import com.pims.vault.presentation.ui.components.PersonaMenu
import com.pims.vault.presentation.ui.components.PersonaMenuItem
import com.pims.vault.presentation.ui.components.PersonaSectionHeading
import com.pims.vault.presentation.ui.components.PersonaSheet
import com.pims.vault.presentation.ui.components.PersonaSheetHeader
import com.pims.vault.presentation.ui.components.PersonaTextInput
import com.pims.vault.presentation.ui.state.PersonaEmptyState
import com.pims.vault.presentation.ui.theme.PersonaIcon
import com.pims.vault.presentation.ui.theme.PersonaIcons
import com.pims.vault.presentation.ui.theme.PersonaIconSize
import com.pims.vault.presentation.ui.theme.PimsRadius
import com.pims.vault.presentation.ui.theme.PimsSpacing
import com.pims.vault.presentation.ui.theme.pimsPageGutter

sealed interface LibraryDestination {
    data class Note(val id: String) : LibraryDestination
    data class Person(val id: String, val noteId: String? = null) : LibraryDestination
    data class Vault(val id: String) : LibraryDestination
}

data class LibraryPersonLink(val relationshipId: String, val name: String)

data class LibraryField(val label: String, val value: String)

data class LibraryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: InformationCategory,
    val sensitivity: InformationSensitivity,
    val personName: String = "Me",
    val reviewValue: String? = null,
    val isFavorite: Boolean = false,
    val updatedAt: Long = 0,
    val metadata: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val relatedPeople: List<LibraryPersonLink> = emptyList(),
    val destination: LibraryDestination? = null,
    val displayValue: String = subtitle,
    val fields: List<LibraryField> = emptyList()
) {
    fun matches(query: String): Boolean {
        val searchable = listOf(title, subtitle, displayValue, category.displayName, personName) +
            metadata + tags + relatedPeople.map { it.name }
        return query.trim().split(searchWhitespace).all { token ->
            if (token.startsWith('#') && token.length > 1) {
                val prefix = normalizedNoteTag(token)
                tags.any { normalizedNoteTag(it).startsWith(prefix) }
            } else {
                searchable.any { field -> field.contains(token, ignoreCase = true) }
            }
        }
    }
}

private val searchWhitespace = Regex("\\s+")

@Composable
fun InformationLibraryRoute(
    profile: ProfileUiState,
    documents: DocumentUiState,
    vault: VaultUiState,
    medical: MedicalUiState,
    onBack: () -> Unit,
    onRequestAuthToReveal: (() -> Unit) -> Unit,
    onOpenDestination: (LibraryDestination) -> Unit,
    notesViewModel: PlainNotesViewModel = hiltViewModel(),
    publicNotesViewModel: PublicRelationshipNotesViewModel = hiltViewModel()
) {
    val plainNotes by notesViewModel.uiState.collectAsStateWithLifecycle()
    val ownerId = profile.person?.id
    val peopleNotesFlow = remember(ownerId, publicNotesViewModel) {
        publicNotesViewModel.observe(ownerId)
    }
    val peopleNotes by peopleNotesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val items = remember(profile, documents, vault.vaultItems, medical, plainNotes.notes, peopleNotes) {
        LibraryItemBuilder.build(
            profile, documents, vault, medical, plainNotes.notes.filter { it.ownerPersonId == ownerId }, peopleNotes
        )
    }
    InformationLibraryScreen(items, onBack, onRequestAuthToReveal, onOpenDestination)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformationLibraryScreen(
    items: List<LibraryItem>,
    onBack: () -> Unit,
    onRequestAuthToReveal: (onSuccess: () -> Unit) -> Unit,
    onOpenDestination: (LibraryDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<InformationCategory?>(null) }
    var onlyFavorites by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = items.firstOrNull { it.id == selectedId }
    val categories = remember(items) {
        val present = items.mapTo(mutableSetOf()) { it.category }
        InformationCategory.entries.filter { it in present }
    }
    val filtered = remember(items, searchQuery, category, onlyFavorites) {
        items.filter {
            (category == null || it.category == category) &&
                (!onlyFavorites || it.isFavorite) && it.matches(searchQuery)
        }.sortedByDescending { it.updatedAt }
    }
    val sections = remember(filtered) { filtered.groupBy { it.category } }
    val isFiltering = searchQuery.isNotBlank() || category != null || onlyFavorites

    BackHandler {
        when {
            selectedId != null -> selectedId = null
            showFilters -> showFilters = false
            category != null -> category = null
            searchQuery.isNotBlank() -> searchQuery = ""
            onlyFavorites -> onlyFavorites = false
            else -> onBack()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            val gutter = pimsPageGutter() + PimsSpacing.xs
            LazyColumn(
                modifier = Modifier.widthIn(max = 840.dp).fillMaxSize().imePadding(),
                contentPadding = PaddingValues(start = gutter, end = gutter, top = PimsSpacing.m, bottom = PimsSpacing.xxl)
            ) {
                item(key = "library-header") {
                    Column(verticalArrangement = Arrangement.spacedBy(PimsSpacing.sm)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(PimsSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PersonaIconAction(PersonaIcons.ArrowBack, "Back", onBack)
                            Text(
                                "Information library",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.weight(1f).semantics { heading() }
                            )
                        }
                        PersonaTextInput(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Search library",
                            leadingIcon = { PersonaIcon(PersonaIcons.Search, null, size = PersonaIconSize.Small) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    PersonaIconAction(PersonaIcons.Close, "Clear search", { searchQuery = "" })
                                }
                            }
                        )
                        LibraryCategoryMenu(
                            category = category,
                            onlyFavorites = onlyFavorites,
                            categories = categories,
                            hasFavorites = items.any { it.isFavorite },
                            expanded = showFilters,
                            onExpand = { showFilters = true },
                            onDismiss = { showFilters = false },
                            onSelect = { chosenCategory, favorites ->
                                category = chosenCategory
                                onlyFavorites = favorites
                                showFilters = false
                            }
                        )
                    }
                }
                if (sections.isEmpty()) {
                    item(key = "library-empty") {
                        PersonaEmptyState(
                            icon = PersonaIcons.Search,
                            title = if (isFiltering) "No matching information" else "No information here yet",
                            modifier = Modifier.padding(top = PimsSpacing.xl)
                        )
                    }
                }
                for (section in InformationCategory.entries) {
                    val entries = sections[section] ?: continue
                    item(key = "section-${section.name}") {
                        if (category == null) {
                            LibrarySectionHeading(section)
                        } else {
                            Spacer(Modifier.height(PimsSpacing.m))
                        }
                    }
                    itemsIndexed(entries, key = { _, record -> "record-${record.id}" }) { index, entry ->
                        LibraryRecordRow(
                            entry = entry,
                            first = index == 0,
                            last = index == entries.lastIndex,
                            onClick = { selectedId = entry.id }
                        )
                    }
                }
            }
        }
    }

    selected?.let { item ->
        LibraryReviewSheet(
            item = item,
            onRequestAuth = onRequestAuthToReveal,
            onOpen = { destination ->
                selectedId = null
                onOpenDestination(destination)
            },
            onDismiss = { selectedId = null }
        )
    }
}

@Composable
private fun LibraryCategoryMenu(
    category: InformationCategory?,
    onlyFavorites: Boolean,
    categories: List<InformationCategory>,
    hasFavorites: Boolean,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (InformationCategory?, Boolean) -> Unit
) {
    Box {
        PersonaAction(
            label = if (onlyFavorites) "Favorites" else category?.displayName ?: "All information",
            onClick = onExpand,
            style = PersonaActionStyle.Quiet,
            icon = PersonaIcons.ExpandMore
        )
        PersonaMenu(expanded = expanded, onDismissRequest = onDismiss) {
            PersonaMenuItem(
                label = "All information",
                icon = if (category == null && !onlyFavorites) PersonaIcons.Check else PersonaIcons.Library,
                onClick = { onSelect(null, false) }
            )
            if (hasFavorites || onlyFavorites) {
                PersonaMenuItem(
                    label = "Favorites",
                    icon = if (onlyFavorites) PersonaIcons.Check else PersonaIcons.Bookmark,
                    onClick = { onSelect(null, true) }
                )
            }
            categories.forEach { itemCategory ->
                PersonaMenuItem(
                    label = itemCategory.displayName,
                    icon = if (category == itemCategory) PersonaIcons.Check else itemCategory.libraryIcon(),
                    onClick = { onSelect(itemCategory, false) }
                )
            }
        }
    }
}

@Composable
private fun LibrarySectionHeading(category: InformationCategory) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = PimsSpacing.xs, top = PimsSpacing.l, bottom = PimsSpacing.s),
        horizontalArrangement = Arrangement.spacedBy(PimsSpacing.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PersonaIcon(category.libraryIcon(), null, size = PersonaIconSize.Small, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            category.displayName,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() }
        )
    }
}

@Composable
private fun LibraryRecordRow(entry: LibraryItem, first: Boolean, last: Boolean, onClick: () -> Unit) {
    val topRadius = if (first) PimsRadius.surface else 0.dp
    val bottomRadius = if (last) PimsRadius.surface else 0.dp
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topRadius, topRadius, bottomRadius, bottomRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PimsSpacing.m, vertical = PimsSpacing.m),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PimsSpacing.m)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PimsSpacing.xs)) {
                    Text(entry.title, style = MaterialTheme.typography.titleMedium, overflow = TextOverflow.Ellipsis, maxLines = 2)
                    val preview = entry.displayValue.takeIf(String::isNotBlank)
                    if (preview != null) {
                        Text(
                            preview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (entry.category == InformationCategory.ADDRESS) 3 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (entry.sensitivity.maskedByDefault) {
                    PersonaIcon(PersonaIcons.LockOutlined, "Protected", size = PersonaIconSize.Small, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    PersonaIcon(PersonaIcons.ChevronRight, null, size = PersonaIconSize.Small, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!last) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = PimsSpacing.m),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
            }
        }
    }
}

private fun InformationCategory.libraryIcon() = when (this) {
    InformationCategory.PERSONAL -> PersonaIcons.MeOutlined
    InformationCategory.CONTACT -> PersonaIcons.Phone
    InformationCategory.ADDRESS -> PersonaIcons.Location
    InformationCategory.IDENTIFICATION -> PersonaIcons.Identity
    InformationCategory.DOCUMENT -> PersonaIcons.DocumentsOutlined
    InformationCategory.FINANCIAL -> PersonaIcons.CreditCard
    InformationCategory.PASSWORD, InformationCategory.SECURITY -> PersonaIcons.LockOutlined
    InformationCategory.MEDICAL, InformationCategory.EMERGENCY -> PersonaIcons.MedicalServicesOutlined
    InformationCategory.EDUCATION -> PersonaIcons.Education
    InformationCategory.EMPLOYMENT -> PersonaIcons.Work
    InformationCategory.RELATIONSHIP -> PersonaIcons.People
    InformationCategory.SOCIAL -> PersonaIcons.Link
    InformationCategory.NOTE -> PersonaIcons.NotesOutlined
    InformationCategory.OTHER -> PersonaIcons.Library
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryReviewSheet(
    item: LibraryItem,
    onRequestAuth: (() -> Unit) -> Unit,
    onOpen: (LibraryDestination) -> Unit,
    onDismiss: () -> Unit
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    var revealed by remember(item.id) { mutableStateOf(false) }
    var canReveal by remember(item.id) { mutableStateOf(true) }
    DisposableEffect(lifecycle, item.id) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) revealed = false
        }
        lifecycle.addObserver(observer)
        onDispose {
            canReveal = false
            lifecycle.removeObserver(observer)
        }
    }
    val protected = item.sensitivity.requiresAuthToReveal
    val visibleFields = if (!protected || revealed) item.fields else emptyList()
    val displayValue = if (revealed) item.reviewValue?.takeIf { visibleFields.isEmpty() } else item.displayValue
    val copyValue = (if (revealed) item.reviewValue else item.subtitle).takeIf { !it.isNullOrBlank() }

    PersonaSheet(onDismiss, rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = pimsPageGutter()).padding(bottom = PimsSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PimsSpacing.m)
        ) {
            PersonaSheetHeader(item.title, onDismiss)
            if (!displayValue.isNullOrBlank() || visibleFields.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(PimsRadius.surface),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Column(
                        modifier = Modifier.padding(PimsSpacing.m),
                        verticalArrangement = Arrangement.spacedBy(PimsSpacing.m)
                    ) {
                        if (!displayValue.isNullOrBlank()) {
                            Text(displayValue, style = MaterialTheme.typography.bodyLarge)
                        }
                        visibleFields.forEach { field ->
                            Column(verticalArrangement = Arrangement.spacedBy(PimsSpacing.xs)) {
                                Text(
                                    field.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(field.value, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
            if ((!protected || revealed) && copyValue != null) {
                PersonaAction("Copy", {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(item.title, copyValue)
                    if (protected) {
                        clip.description.extras = PersistableBundle().apply {
                            putBoolean("android.content.extra.IS_SENSITIVE", true)
                        }
                    }
                    clipboard.setPrimaryClip(clip)
                }, style = PersonaActionStyle.Quiet)
            }
            if (item.reviewValue != null && protected) {
                PersonaAction(if (revealed) "Hide" else "Reveal", {
                    if (revealed) revealed = false
                    else onRequestAuth {
                        if (canReveal && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) revealed = true
                    }
                }, style = PersonaActionStyle.Quiet, icon = PersonaIcons.Lock)
            }
            item.destination?.let { destination ->
                val label = when (destination) {
                    is LibraryDestination.Note -> "Open note"
                    is LibraryDestination.Person -> if (destination.noteId == null) "Open person" else "Open note"
                    is LibraryDestination.Vault -> "Open in Vault"
                }
                PersonaAction(label, { onOpen(destination) })
            }
            val extraPeople = item.relatedPeople.filterNot {
                it.relationshipId == (item.destination as? LibraryDestination.Person)?.takeIf { person -> person.noteId == null }?.id
            }
            if (extraPeople.isNotEmpty()) {
                PersonaSectionHeading("Related people")
                extraPeople.forEach { person ->
                    PersonaInformationRow(
                        title = person.name,
                        icon = PersonaIcons.People,
                        onClick = { onOpen(LibraryDestination.Person(person.relationshipId)) }
                    )
                }
            }
        }
    }
}
