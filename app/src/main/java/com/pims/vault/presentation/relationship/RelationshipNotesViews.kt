package com.pims.vault.presentation.relationship

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import com.pims.vault.presentation.ui.components.PersonaSwitch as Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import com.pims.vault.domain.model.NoteFormat
import com.pims.vault.domain.model.RelationshipNote
import com.pims.vault.domain.model.RelationshipNoteAttachmentDraft
import com.pims.vault.domain.model.RelationshipNoteAttachment
import com.pims.vault.core.crypto.ZeroizingByteArrayOutputStream
import com.pims.vault.presentation.ui.theme.PimsDimensions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

/**
 * Renders the Granular Relationship Notes system:
 * - Public and Private notes separated cleanly
 * - Locked notes state with biometric / PIN unlock trigger
 * - Plain and Bullet note formatting
 * - Simple Add/Edit note bottom sheet
 */
@Composable
fun RelationshipNotesSection(
    notes: List<RelationshipNote>,
    isVaultUnlocked: Boolean,
    onAddNoteClick: () -> Unit,
    onUnlockVaultClick: () -> Unit,
    onEditNote: (RelationshipNote) -> Unit,
    onDeleteNote: (RelationshipNote) -> Unit,
    onTogglePrivacy: (noteId: String, makePrivate: Boolean) -> Unit,
    onOpenAttachment: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val publicNotes = notes.filter { !it.isPrivate }
    val privateNotes = notes.filter { it.isPrivate }
    val lockedCount = privateNotes.count { !it.isDecrypted }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Notes,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                )
            }

            OutlinedButton(
                onClick = onAddNoteClick,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Note",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Add Note",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (notes.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No notes recorded yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                }
            }
        }

        // 1. Render Public Notes
        publicNotes.forEach { note ->
            RelationshipNoteCard(
                note = note,
                onEdit = { onEditNote(note) },
                onDelete = { onDeleteNote(note) },
                onTogglePrivacy = { onTogglePrivacy(note.id, !note.isPrivate) },
                onOpenAttachment = onOpenAttachment
            )
        }

        // 2. Render Private Notes: Either Locked Overview Card OR Decrypted Cards
        if (!isVaultUnlocked && lockedCount > 0) {
            LockedNotesBanner(
                lockedCount = lockedCount,
                onUnlockClick = onUnlockVaultClick
            )
        } else {
            privateNotes.forEach { note ->
                RelationshipNoteCard(
                    note = note,
                    onEdit = { onEditNote(note) },
                    onDelete = { onDeleteNote(note) },
                    onTogglePrivacy = { onTogglePrivacy(note.id, !note.isPrivate) },
                    onOpenAttachment = onOpenAttachment
                )
            }
        }
    }
}

/**
 * Individual Note Card with Plain or Bullet formatting and sensitivity reveal
 */
@Composable
fun RelationshipNoteCard(
    note: RelationshipNote,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePrivacy: () -> Unit,
    onOpenAttachment: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var isRevealed by remember(note.id, note.isPrivate, note.isDecrypted) { mutableStateOf(!note.isPrivate) }

    Surface(
        shape = RoundedCornerShape(0.dp),
        color = Color.Transparent,
        border = null,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = note.topic.ifBlank { "Note" },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    if (note.isPrivate) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Private Note",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "Private",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPrivate) {
                        IconButton(
                            onClick = { isRevealed = !isRevealed },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isRevealed) "Hide value" else "Show value",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Note options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (note.isPrivate) "Make Public" else "Lock & Encrypt") },
                                leadingIcon = {
                                    Icon(
                                        if (note.isPrivate) Icons.Default.LockOpen else Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onTogglePrivacy()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Note Content
            val privateContentVisible = note.isDecrypted && isRevealed
            if (note.isPrivate && !privateContentVisible) {
                Text(
                    text = "••••••••••••",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp
                )
            } else {
                when (note.format) {
                    NoteFormat.PLAIN -> {
                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    NoteFormat.BULLETS -> {
                        val lines = note.content.lines().filter { it.isNotBlank() }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            lines.forEach { line ->
                                val cleanLine = line.trim().removePrefix("•").trim()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "• ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = cleanLine,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    NoteFormat.NUMBERED -> {
                        val lines = note.content.lines().filter { it.isNotBlank() }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            lines.forEachIndexed { index, line ->
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                    Text("${index + 1}. ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(line.trim().replaceFirst(Regex("^\\d+\\.\\s*"), ""), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
                if (note.tags.isNotEmpty() && (!note.isPrivate || privateContentVisible)) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        note.tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "#$tag",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
                if ((!note.isPrivate || privateContentVisible) && note.attachments.isNotEmpty()) {
                    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        note.attachments.forEach { attachment ->
                            OutlinedButton(
                                onClick = { onOpenAttachment(note.id, attachment.id) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Open ${attachment.mimeType.substringAfter('/').uppercase()} · ${attachment.fileSizeBytes / 1024} KB")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Locked Private Notes Banner (3 private notes - Tap to unlock)
 */
@Composable
fun LockedNotesBanner(
    lockedCount: Int,
    onUnlockClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onUnlockClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked notes",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "Private Notes",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$lockedCount protected ${if (lockedCount == 1) "note" else "notes"} • Tap to unlock",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Unlock",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Clean & Minimal Add / Edit Relationship Note Dialog
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddOrEditRelationshipNoteDialog(
    initialNote: RelationshipNote? = null,
    isVaultUnlocked: Boolean = false,
    onUnlockVault: () -> Unit = {},
    onDismiss: () -> Unit,
    onSave: suspend (topic: String?, content: String, format: NoteFormat, isPrivate: Boolean, additions: List<RelationshipNoteAttachmentDraft>, removals: Set<String>) -> Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var isPicking by remember { mutableStateOf(false) }
    val attachments = remember { mutableStateListOf<RelationshipNoteAttachmentDraft>() }
    val removals = remember { mutableStateListOf<String>() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null && !isSaving && !isPicking) {
            isPicking = true
            coroutineScope.launch {
                var importedBytes: ByteArray? = null
                try {
                    val mime = context.contentResolver.getType(uri).orEmpty().lowercase()
                    require(mime in setOf("image/jpeg", "image/png", "application/pdf")) { "Choose a JPG, PNG, or PDF file" }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            val output = ZeroizingByteArrayOutputStream(32 * 1024)
                            val buffer = ByteArray(64 * 1024)
                            var total = 0
                            try {
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    total += read
                                    require(total <= 10 * 1024 * 1024) { "Files must be 10 MB or smaller" }
                                    output.write(buffer, 0, read)
                                }
                                require(total > 0) { "File is empty" }
                                importedBytes = output.takeBytesAndWipe()
                            } finally {
                                buffer.fill(0)
                                output.wipe()
                            }
                        } ?: error("Could not read file")
                    }
                    attachments.add(RelationshipNoteAttachmentDraft(checkNotNull(importedBytes), mime))
                    importedBytes = null // Draft now owns the buffer until save/discard.
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Toast.makeText(context, error.message ?: "Could not add file", Toast.LENGTH_SHORT).show()
                } finally {
                    importedBytes?.fill(0)
                    isPicking = false
                }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { attachments.forEach { it.bytes.fill(0) } }
    }
    var topic by remember { mutableStateOf(initialNote?.topic?.takeIf { it != "Note" } ?: "") }
    var contentValue by remember {
        val initialText = initialNote?.content ?: ""
        mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length)))
    }
    var format by remember { mutableStateOf(initialNote?.format ?: NoteFormat.PLAIN) }
    var isPrivate by remember { mutableStateOf(initialNote?.isPrivate ?: false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var showDiscardConfirmation by remember { mutableStateOf(false) }
    val originalTopic = initialNote?.topic?.takeIf { it != "Note" }.orEmpty()
    val hasUnsavedChanges = topic != originalTopic || contentValue.text != (initialNote?.content ?: "") ||
        format != (initialNote?.format ?: NoteFormat.PLAIN) || isPrivate != (initialNote?.isPrivate ?: false) ||
        attachments.isNotEmpty() || removals.isNotEmpty()
    val requestDismiss = {
        if (!isSaving && !isPicking) {
            if (hasUnsavedChanges) showDiscardConfirmation = true else onDismiss()
        }
    }

    var editMenuExpanded by remember { mutableStateOf(false) }
    val undo = remember { mutableStateListOf<TextFieldValue>() }
    val redo = remember { mutableStateListOf<TextFieldValue>() }
    val changeContent: (TextFieldValue) -> Unit = { next ->
        if (next.text != contentValue.text) {
            undo.add(contentValue)
            if (undo.size > 50) undo.removeAt(0)
            redo.clear()
        }
        contentValue = next
    }
    val privateLocked = (isPrivate || initialNote?.isPrivate == true) && !isVaultUnlocked
    Dialog(
        onDismissRequest = requestDismiss,
        properties = DialogProperties(
            securePolicy = SecureFlagPolicy.SecureOn,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = requestDismiss, enabled = !isSaving && !isPicking) {
                        Icon(Icons.Default.Close, "Close note")
                    }
                    Text(
                        if (initialNote == null) "New note" else "Edit note",
                        Modifier.weight(1f), style = MaterialTheme.typography.titleLarge
                    )
                    Box {
                        IconButton(onClick = { editMenuExpanded = true }, enabled = !isSaving && !privateLocked) {
                            Icon(Icons.Default.MoreVert, "Editing actions")
                        }
                        DropdownMenu(editMenuExpanded, { editMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Undo") }, enabled = undo.isNotEmpty(),
                                onClick = { redo.add(contentValue); contentValue = undo.removeAt(undo.lastIndex); editMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Redo") }, enabled = redo.isNotEmpty(),
                                onClick = { undo.add(contentValue); contentValue = redo.removeAt(redo.lastIndex); editMenuExpanded = false }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            if (isSaving || isPicking || ((isPrivate || initialNote?.isPrivate == true) && !isVaultUnlocked)) return@Button
                            isSaving = true
                            saveError = null
                            val finalContent = contentValue.text.trim()
                            if (finalContent.isNotBlank()) {
                                val resolvedFormat = when {
                                    finalContent.lines().any { it.trimStart().matches(Regex("^\\d+\\.\\s.*")) } -> NoteFormat.NUMBERED
                                    finalContent.contains("•") -> NoteFormat.BULLETS
                                    else -> format
                                }
                                coroutineScope.launch {
                                    val additions = attachments.map { RelationshipNoteAttachmentDraft(it.bytes.copyOf(), it.mimeType) }
                                    val saved = try {
                                        onSave(topic.takeIf { it.isNotBlank() }, finalContent, resolvedFormat, isPrivate, additions, removals.toSet())
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        false
                                    } finally {
                                        additions.forEach { it.bytes.fill(0) }
                                        isSaving = false
                                    }
                                    if (saved) onDismiss()
                                    else saveError = "Could not save this note. Check your details and try again."
                                }
                            }
                        },
                        enabled = !isSaving && !isPicking && hasUnsavedChanges && contentValue.text.isNotBlank() &&
                            (!(isPrivate || initialNote?.isPrivate == true) || isVaultUnlocked)
                    ) { Text(if (isSaving) "Saving…" else "Save") }
                }
                saveError?.let {
                    Text(it, Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.error)
                }
                if ((isPrivate || initialNote?.isPrivate == true) && !isVaultUnlocked) {
                    Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Unlock the vault to edit this private note.", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onUnlockVault) { Text("Unlock") }
                    }
                }
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (!privateLocked) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = topic, onValueChange = { topic = it }, enabled = !isSaving,
                            textStyle = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { field ->
                                if (topic.isEmpty()) Text("Title", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                field()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        androidx.compose.foundation.text.BasicTextField(
                            value = contentValue,
                            onValueChange = { next -> changeContent(com.pims.vault.presentation.notes.NoteEditorLogic.handleEnterKeyForBullets(contentValue, next)) },
                            enabled = !isSaving,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, lineHeight = 26.sp),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { field ->
                                if (contentValue.text.isEmpty()) Text("Write a note…", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                field()
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp)
                        )
                        initialNote?.attachments?.filterNot { it.id in removals }?.forEach { attachment ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("${attachment.mimeType.substringAfter('/').uppercase()} · ${attachment.fileSizeBytes / 1024} KB", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = { removals.add(attachment.id) }, enabled = !isSaving) {
                                    Icon(Icons.Default.DeleteOutline, "Remove attachment")
                                }
                            }
                        }
                        attachments.forEachIndexed { index, attachment ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("${attachment.mimeType.substringAfter('/').uppercase()} · ${attachment.bytes.size / 1024} KB", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = { attachments.removeAt(index).bytes.fill(0) }, enabled = !isSaving) {
                                    Icon(Icons.Default.DeleteOutline, "Remove attachment")
                                }
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(enabled = !isSaving && !privateLocked, onClick = {
                        changeContent(com.pims.vault.presentation.notes.NoteEditorLogic.toggleBulletAtCurrentLine(contentValue))
                        format = NoteFormat.BULLETS
                    }) { Icon(Icons.Default.FormatListBulleted, "Bullet list") }
                    IconButton(enabled = !isSaving && !privateLocked, onClick = {
                        changeContent(com.pims.vault.presentation.notes.NoteEditorLogic.toggleNumberedListAtCurrentLine(contentValue))
                        format = NoteFormat.NUMBERED
                    }) { Icon(Icons.Default.FormatListNumbered, "Numbered list") }
                    IconButton(enabled = !isSaving && !isPicking && !privateLocked, onClick = {
                        picker.launch(arrayOf("image/jpeg", "image/png", "application/pdf"))
                    }) { Icon(Icons.Default.AttachFile, if (isPicking) "Adding attachment" else "Attach photo or PDF") }
                    Spacer(Modifier.weight(1f))
                    Text("Private", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = isPrivate, onCheckedChange = { isPrivate = it }, enabled = !isSaving)
                }
            }
        }
    }
    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
            title = { Text("Discard this note?") },
            text = { Text("Your changes have not been saved.") },
            confirmButton = {
                Button(onClick = { showDiscardConfirmation = false; onDismiss() }) { Text("Discard") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDiscardConfirmation = false }) { Text("Keep editing") }
            }
        )
    }
}
