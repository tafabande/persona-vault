package com.pims.vault.presentation.hub

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.pims.vault.presentation.media.rememberPhotoPreview
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pims.vault.domain.model.NoteFormat
import com.pims.vault.domain.model.RelationshipNote
import com.pims.vault.presentation.avatar.PersonaAvatarManager
import com.pims.vault.presentation.profile.KinRelationshipItem
import com.pims.vault.presentation.notes.PlainNotesViewModel
import com.pims.vault.presentation.relationship.PersonNoteLinks
import com.pims.vault.presentation.relationship.PublicRelationshipNotesViewModel
import com.pims.vault.presentation.relationship.AddOrEditRelationshipNoteDialog
import com.pims.vault.presentation.relationship.RelationshipNotesSection
import com.pims.vault.presentation.ui.components.DefaultAvatar
import com.pims.vault.presentation.ui.components.PersonaAction
import com.pims.vault.presentation.ui.components.PersonaActionStyle
import com.pims.vault.presentation.ui.components.PersonaDialog
import com.pims.vault.presentation.ui.components.PersonaIconAction
import com.pims.vault.presentation.ui.components.PersonaInformationRow
import com.pims.vault.presentation.ui.components.PersonaSectionHeading
import com.pims.vault.presentation.ui.components.PersonaSheet
import com.pims.vault.presentation.ui.components.PersonaSheetHeader
import com.pims.vault.presentation.ui.components.ux.PersonaToastHost
import com.pims.vault.presentation.ui.components.ux.rememberPersonaToastController
import com.pims.vault.presentation.ui.theme.PersonaIcons
import com.pims.vault.presentation.ui.theme.PimsSpacing
import com.pims.vault.presentation.ui.theme.pimsPageGutter
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Dedicated full-screen page for displaying and managing all information about a person:
 * - Profile avatar with photo changer
 * - Name, relationship role, trusted contact toggle
 * - Contact methods (phone, email, address, DOB)
 * - Complete memory notes with in-place Add, Edit, Delete, and privacy controls
 * - Edit person and Delete connection actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    person: KinRelationshipItem,
    relationshipNotes: List<RelationshipNote>,
    onBack: () -> Unit,
    onEditPerson: (KinRelationshipItem) -> Unit,
    onDeletePerson: (KinRelationshipItem) -> Unit,
    onAddNote: suspend (topic: String?, content: String, format: NoteFormat, isPrivate: Boolean, List<com.pims.vault.domain.model.RelationshipNoteAttachmentDraft>, Set<String>) -> Boolean,
    onUpdateNote: suspend (noteId: String, topic: String?, content: String, format: NoteFormat, isPrivate: Boolean, List<com.pims.vault.domain.model.RelationshipNoteAttachmentDraft>, Set<String>) -> Boolean,
    onDeleteNote: (noteId: String) -> Unit,
    onToggleTrusted: (Boolean) -> Unit,
    onToggleNotePrivacy: (noteId: String, makePrivate: Boolean) -> Unit = { _, _ -> },
    onOpenAttachment: (String, String) -> Unit = { _, _ -> },
    isVaultUnlocked: Boolean = false,
    onUnlockVault: () -> Unit = {},
    modifier: Modifier = Modifier,
    people: List<KinRelationshipItem> = emptyList(),
    onOpenTaggedNote: (String) -> Unit = {},
    ownerId: String? = null,
    requestedNoteId: String? = null,
    onRequestedNoteOpened: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = rememberPimsHaptics()
    val noteFeedback = rememberPersonaToastController()
    val avatarManager = remember { PersonaAvatarManager.getInstance(context) }
    val photosVersion by avatarManager.personPhotosVersion.collectAsState()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAvatarActionChooser by remember { mutableStateOf(false) }
    var showRemovePhotoConfirmation by remember { mutableStateOf(false) }

    // Note dialog state
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<RelationshipNote?>(null) }
    var noteToDelete by remember { mutableStateOf<RelationshipNote?>(null) }

    val requestedNote = relationshipNotes.firstOrNull { it.id == requestedNoteId && !it.isPrivate }
    LaunchedEffect(requestedNote) {
        if (requestedNote != null) {
            noteToEdit = requestedNote
            showNoteDialog = true
            onRequestedNoteOpened()
        }
    }

    val effectivePersonId = person.targetPersonId.ifBlank { person.id }
    val contactPhotoPath = remember(effectivePersonId, person.id, photosVersion) {
        avatarManager.getPersonPhotoPath(effectivePersonId, person.id)
    }

    val photoScope = rememberCoroutineScope()
    var pendingPhotoOwnerId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPhotoRelationshipId by rememberSaveable { mutableStateOf<String?>(null) }
    val saveUriToPhoto: (Uri, Pair<String, String>, File?) -> Unit = { uri, owner, capture ->
        photoScope.launch {
            try {
                avatarManager.importPersonPhoto(owner.first, uri, owner.second)
                haptics.success()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                haptics.warning()
                Toast.makeText(context, "Photo could not be saved. Please choose another image.", Toast.LENGTH_LONG).show()
            } finally {
                if (capture != null) withContext(NonCancellable + Dispatchers.IO) { capture.delete() }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val owner = pendingPhotoOwnerId?.let { it to pendingPhotoRelationshipId.orEmpty() }
        pendingPhotoOwnerId = null
        pendingPhotoRelationshipId = null
        if (uri != null && owner != null) saveUriToPhoto(uri, owner, null)
    }

    var cameraTempUri by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraTempPath by rememberSaveable { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val owner = pendingPhotoOwnerId?.let { it to pendingPhotoRelationshipId.orEmpty() }
        val uri = cameraTempUri?.let(Uri::parse)
        val capture = cameraTempPath?.let(::File)
        pendingPhotoOwnerId = null
        pendingPhotoRelationshipId = null
        cameraTempUri = null
        cameraTempPath = null
        if (success && uri != null && owner != null) saveUriToPhoto(uri, owner, capture)
        else if (capture != null) photoScope.launch(Dispatchers.IO) { capture.delete() }
    }

    BackHandler(onBack = onBack)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // TOP APP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PersonaIconAction(PersonaIcons.ArrowBack, "Back", onBack)

                Text(
                    text = "People",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )

                PersonaIconAction(PersonaIcons.Edit, "Edit person", { onEditPerson(person) })
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                thickness = 0.5.dp
            )

            // SCROLLABLE CONTENT
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = pimsPageGutter(), vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // AVATAR & HERO
                    Box(contentAlignment = Alignment.BottomEnd) {
                        val photoBitmap = rememberPhotoPreview(contactPhotoPath)

                        if (photoBitmap != null) {
                            Image(
                                bitmap = photoBitmap.asImageBitmap(),
                                contentDescription = person.fullName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                                    .clickable { showAvatarActionChooser = true }
                            )
                        } else {
                            DefaultAvatar(
                                name = person.fullName,
                                size = 72.dp,
                                onClick = { showAvatarActionChooser = true }
                            )
                        }

                        // Edit camera badge
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { showAvatarActionChooser = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = PersonaIcons.CameraAlt,
                                contentDescription = "Change photo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Name & Role
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                        Text(
                            text = person.fullName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = person.relationRole.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val tonalSurfaces = com.pims.vault.presentation.ui.theme.LocalPimsTonalSurfaces.current

                // UNIFIED CONTACT DETAILS TONAL GROUP
                val hasContactInfo = person.phone.isNotBlank() || person.email.isNotBlank() ||
                    person.address.isNotBlank() || person.dateOfBirth.isNotBlank() || person.anniversary.isNotBlank()

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Contact Information",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent,
                        border = null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            var itemIndex = 0

                            if (person.phone.isNotBlank()) {
                                if (itemIndex > 0) {
                                    HorizontalDivider(color = tonalSurfaces.borderSubtle, modifier = Modifier.padding(horizontal = 14.dp))
                                }
                                DetailRowItem(
                                    icon = PersonaIcons.Phone,
                                    value = person.phone,
                                    label = "Phone",
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${person.phone}"))
                                        context.startActivity(intent)
                                    }
                                )
                                itemIndex++
                            }

                            if (person.email.isNotBlank()) {
                                if (itemIndex > 0) {
                                    HorizontalDivider(color = tonalSurfaces.borderSubtle, modifier = Modifier.padding(horizontal = 14.dp))
                                }
                                DetailRowItem(
                                    icon = PersonaIcons.Email,
                                    value = person.email,
                                    label = "Email",
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${person.email}"))
                                        context.startActivity(intent)
                                    }
                                )
                                itemIndex++
                            }

                            if (person.address.isNotBlank()) {
                                if (itemIndex > 0) {
                                    HorizontalDivider(color = tonalSurfaces.borderSubtle, modifier = Modifier.padding(horizontal = 14.dp))
                                }
                                DetailRowItem(
                                    icon = PersonaIcons.Home,
                                    value = person.address,
                                    label = "Address",
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Address", person.address))
                                        Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                itemIndex++
                            }

                            if (person.dateOfBirth.isNotBlank()) {
                                if (itemIndex > 0) {
                                    HorizontalDivider(color = tonalSurfaces.borderSubtle, modifier = Modifier.padding(horizontal = 14.dp))
                                }
                                DetailRowItem(
                                    icon = PersonaIcons.Calendar,
                                    value = person.dateOfBirth,
                                    label = "Date of Birth",
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Date of Birth", person.dateOfBirth))
                                        Toast.makeText(context, "Date of birth copied", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                itemIndex++
                            }

                            if (person.anniversary.isNotBlank()) {
                                if (itemIndex > 0) {
                                    HorizontalDivider(color = tonalSurfaces.borderSubtle, modifier = Modifier.padding(horizontal = 14.dp))
                                }
                                DetailRowItem(
                                    icon = PersonaIcons.Calendar,
                                    value = person.anniversary,
                                    label = "Anniversary",
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Anniversary", person.anniversary))
                                        Toast.makeText(context, "Anniversary copied", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                itemIndex++
                            }

                            if (!hasContactInfo) {
                                DetailRowItem(
                                    icon = PersonaIcons.Add,
                                    value = "Add contact info",
                                    label = "Phone, email, or address",
                                    onClick = { onEditPerson(person) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // RELATIONSHIP NOTES SECTION (In-Place Add, Edit, Delete)
                RelationshipNotesSection(
                    notes = relationshipNotes,
                    isVaultUnlocked = isVaultUnlocked,
                    onAddNoteClick = {
                        noteToEdit = null
                        showNoteDialog = true
                    },
                    onUnlockVaultClick = onUnlockVault,
                    onEditNote = { note ->
                        noteToEdit = note
                        showNoteDialog = true
                    },
                    onDeleteNote = { note ->
                        noteToDelete = note
                    },
                    onTogglePrivacy = onToggleNotePrivacy,
                    onOpenAttachment = onOpenAttachment,
                    modifier = Modifier.fillMaxWidth(),
                    restrictedLegacyNotes = person.restrictedLegacyNotes,
                    linkedNotes = { TaggedPersonNotes(person.id, ownerId, people, onOpenTaggedNote) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom actions: edit is in top app bar, only quiet remove button here
                PersonaAction(
                    label = "Remove from People",
                    onClick = { showDeleteConfirmDialog = true },
                    style = PersonaActionStyle.Quiet,
                    icon = PersonaIcons.Delete
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        PersonaToastHost(noteFeedback)
    }

    // ADD / EDIT NOTE DIALOG
    if (showNoteDialog) {
        AddOrEditRelationshipNoteDialog(
            initialNote = noteToEdit,
            isVaultUnlocked = isVaultUnlocked,
            onUnlockVault = onUnlockVault,
            onDismiss = { showNoteDialog = false },
            onSave = { topic, content, format, isPrivate, attachments, removals ->
                val saved = if (noteToEdit != null) {
                    onUpdateNote(noteToEdit!!.id, topic, content, format, isPrivate, attachments, removals)
                } else {
                    onAddNote(topic, content, format, isPrivate, attachments, removals)
                }
                if (saved) {
                    showNoteDialog = false
                    noteFeedback.showSuccess("Note saved locally")
                }
                saved
            }
        )
    }

    // DELETE NOTE CONFIRMATION DIALOG
    noteToDelete?.let { note ->
        PersonaDialog(
            onDismissRequest = { noteToDelete = null },
            title = "Delete note",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                onDeleteNote(note.id)
                noteToDelete = null
                haptics.warning()
            }
        ) {
            Text("Delete this note and its attachments?", style = MaterialTheme.typography.bodyMedium)
        }
    }

    // DELETE PERSON CONFIRMATION DIALOG
    if (showDeleteConfirmDialog) {
        PersonaDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = "Remove from People",
            confirmText = "Remove",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirmDialog = false
                haptics.warning()
                onDeletePerson(person)
            }
        ) {
            Text(
                "Remove ${person.fullName} and their relationship notes from People?",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    if (showAvatarActionChooser) {
        PersonaSheet(
            onDismissRequest = { showAvatarActionChooser = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = pimsPageGutter(), vertical = PimsSpacing.m),
                verticalArrangement = Arrangement.spacedBy(PimsSpacing.xs)
            ) {
                    PersonaSheetHeader("Contact photo", { showAvatarActionChooser = false })
                    PersonaInformationRow(
                        title = "Take photo",
                        icon = PersonaIcons.CameraAlt,
                        onClick = {
                            showAvatarActionChooser = false
                            try {
                                val directory = File(context.cacheDir, "photo_capture").apply { check(isDirectory || mkdirs()) }
                                val photoFile = File.createTempFile("person-", ".jpg", directory)
                                cameraTempPath = photoFile.absolutePath
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    photoFile
                                )
                                cameraTempUri = uri.toString()
                                pendingPhotoOwnerId = effectivePersonId
                                pendingPhotoRelationshipId = person.id
                                cameraLauncher.launch(uri)
                            } catch (_: Exception) {
                                cameraTempPath?.let(::File)?.delete()
                                cameraTempPath = null
                                cameraTempUri = null
                                pendingPhotoOwnerId = effectivePersonId
                                pendingPhotoRelationshipId = person.id
                                photoPickerLauncher.launch("image/*")
                            }
                        }
                    )
                    PersonaInformationRow(
                        title = "Choose from gallery",
                        icon = PersonaIcons.Photo,
                        onClick = {
                            showAvatarActionChooser = false
                            pendingPhotoOwnerId = effectivePersonId
                            pendingPhotoRelationshipId = person.id
                            photoPickerLauncher.launch("image/*")
                        }
                    )
                    if (!contactPhotoPath.isNullOrBlank()) {
                        PersonaInformationRow(
                            title = "Remove photo",
                            icon = PersonaIcons.Delete,
                            onClick = {
                                showAvatarActionChooser = false
                                showRemovePhotoConfirmation = true
                            }
                        )
                    }
            }
        }
    }

    if (showRemovePhotoConfirmation) {
        PersonaDialog(
            onDismissRequest = { showRemovePhotoConfirmation = false },
            title = "Remove contact photo?",
            confirmText = "Remove",
            isDestructive = true,
            onConfirm = {
                showRemovePhotoConfirmation = false
                avatarManager.setPersonPhotoPath(effectivePersonId, null, person.id)
                haptics.warning()
            }
        ) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaggedPersonNotes(
    relationshipId: String,
    ownerId: String?,
    people: List<KinRelationshipItem>,
    onOpen: (String) -> Unit
) {
    if (people.isEmpty()) return
    val viewModel: PlainNotesViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    val links = remember(people) { PersonNoteLinks(people) }
    val references = remember(links, state.notes, relationshipId) {
        state.notes.filter { note -> links.peopleFor(note).any { it.id == relationshipId } }
    }
    val publicNotesViewModel: PublicRelationshipNotesViewModel = hiltViewModel()
    val publicNotesFlow = remember(ownerId, publicNotesViewModel) { publicNotesViewModel.observe(ownerId) }
    val publicNotes by publicNotesFlow.collectAsState(initial = emptyList())
    val relatedNotes = remember(people, links, publicNotes, relationshipId) {
        val peopleByRelationship = people.associateBy { it.id }
        publicNotes.mapNotNull { entry ->
            if (entry.relationshipId == relationshipId || entry.isPrivate) return@mapNotNull null
            val author = peopleByRelationship[entry.relationshipId] ?: return@mapNotNull null
            val note = RelationshipNote(entry.id, entry.relationshipId, entry.topic.orEmpty(),
                entry.contentPlaintext.orEmpty(), entry.format)
            (author to note).takeIf { links.peopleFor(note).any { it.id == relationshipId } }
        }
    }
    var selectedNoteId by remember { mutableStateOf<String?>(null) }
    if (references.isNotEmpty() || relatedNotes.isNotEmpty()) {
        PersonaSectionHeading("Tagged notes")
        references.forEach { note ->
            PersonaInformationRow(
                title = note.title.ifBlank { "Note" },
                detail = note.content.takeIf(String::isNotBlank),
                icon = PersonaIcons.Notes,
                onClick = { onOpen(note.id) }
            )
        }
        relatedNotes.forEach { (author, note) ->
            PersonaInformationRow(
                title = note.topic.ifBlank { "Note" },
                detail = author.fullName,
                icon = PersonaIcons.Notes,
                onClick = { selectedNoteId = note.id }
            )
        }
    }
    relatedNotes.firstOrNull { it.second.id == selectedNoteId }?.let { (author, note) ->
        PersonaSheet({ selectedNoteId = null }, rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = pimsPageGutter(), vertical = PimsSpacing.l),
                verticalArrangement = Arrangement.spacedBy(PimsSpacing.s)
            ) {
                PersonaSheetHeader(note.topic.ifBlank { "Note" }, { selectedNoteId = null })
                Text(author.fullName, style = MaterialTheme.typography.labelLarge)
                Text(note.content, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun DetailRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (label.isNotBlank()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
