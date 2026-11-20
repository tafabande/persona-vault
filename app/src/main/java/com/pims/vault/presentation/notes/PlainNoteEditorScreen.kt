package com.pims.vault.presentation.notes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import com.pims.vault.core.storage.MAX_ORIGINAL_PHOTO_BYTES
import com.pims.vault.core.storage.decodePhotoPreview
import com.pims.vault.core.storage.inspectPhoto
import com.pims.vault.core.storage.photoExtension
import com.pims.vault.core.storage.readBoundedBytes
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import com.pims.vault.presentation.ui.components.PersonaConfirmationDialog
import com.pims.vault.presentation.ui.components.PersonaAction
import com.pims.vault.presentation.ui.components.PersonaActionStyle
import com.pims.vault.presentation.ui.components.PersonaIconAction
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pims.vault.domain.model.NoteAttachment
import com.pims.vault.domain.model.NoteFormat
import com.pims.vault.domain.model.PlainNote
import com.pims.vault.presentation.ui.components.ux.WarmTooltip
import com.pims.vault.presentation.ui.theme.PersonaIconBadge
import com.pims.vault.presentation.ui.theme.PersonaIconSize
import com.pims.vault.presentation.ui.theme.PersonaIcons
import com.pims.vault.presentation.ui.theme.PimsControlSize
import com.pims.vault.presentation.ui.theme.PimsDimensions
import com.pims.vault.presentation.ui.theme.PimsColors
import com.pims.vault.presentation.ui.theme.PimsSpacing
import com.pims.vault.presentation.ui.theme.pimsPageGutter
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.presentation.ui.util.rememberPimsFeedback
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlainNoteEditorScreen(
    noteId: String?,
    onBack: () -> Unit,
    viewModel: PlainNotesViewModel = hiltViewModel(),
    onRequestAuth: (title: String, subtitle: String, onSuccess: () -> Unit) -> Unit,
    onSavedLocally: () -> Unit = {}
) {
    val haptics = rememberPimsHaptics()
    val feedback = rememberPimsFeedback()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val existingNote = remember(noteId, state.notes) {
        state.notes.find { it.id == noteId }
    }
    if (noteId != null && existingNote == null) {
        BackHandler(onBack = onBack)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(state.error ?: "This note is unavailable.")
                }
                PersonaAction("Back", onBack, style = PersonaActionStyle.Text)
            }
        }
        return
    }

    var currentNoteId by remember(noteId) { mutableStateOf(noteId) }
    var localIsSaving by remember { mutableStateOf(false) }
    val isBusy = state.isSaving || localIsSaving
    var showDiscardConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(state.error) {
        state.error?.let { err ->
            localIsSaving = false
            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.isSaving) {
        if (!state.isSaving && localIsSaving) {
            localIsSaving = false
        }
    }

    var isUnlocked by remember(noteId, existingNote?.isLocked) {
        mutableStateOf(existingNote == null || !existingNote.isLocked)
    }
    var title by remember(isUnlocked) {
        mutableStateOf(if (isUnlocked) (existingNote?.title ?: "") else "")
    }
    var contentValue by remember(isUnlocked) {
        mutableStateOf(TextFieldValue(if (isUnlocked) (existingNote?.content ?: "") else ""))
    }
    var format by remember { mutableStateOf(existingNote?.format ?: NoteFormat.PLAIN) }
    var isLocked by remember(existingNote) { mutableStateOf(existingNote?.isLocked ?: false) }
    var reminderAt by remember(existingNote) { mutableStateOf(existingNote?.reminderAt) }
    var reminderTag by remember(existingNote) { mutableStateOf(existingNote?.reminderTag) }
    var reminderRepeat by remember(existingNote) { mutableStateOf(existingNote?.reminderRepeat) }
    var isReminderDone by remember(existingNote) { mutableStateOf(existingNote?.isReminderDone ?: false) }
    // Encoded source bytes, display name, MIME type.
    val pendingAttachments = remember { mutableStateListOf<Triple<ByteArray, String, String>>() }
    val deletedAttachmentIds = remember { mutableStateListOf<String>() }
    val isDirty = isUnlocked && (title != (existingNote?.title ?: "") ||
        contentValue.text != (existingNote?.content ?: "") ||
        format != (existingNote?.format ?: NoteFormat.PLAIN) ||
        isLocked != (existingNote?.isLocked ?: false) ||
        reminderAt != existingNote?.reminderAt ||
        reminderTag != existingNote?.reminderTag ||
        reminderRepeat != existingNote?.reminderRepeat ||
        isReminderDone != (existingNote?.isReminderDone ?: false) ||
        pendingAttachments.isNotEmpty() || deletedAttachmentIds.isNotEmpty())
    val requestClose = {
        if (isDirty) showDiscardConfirmation = true else onBack()
    }
    BackHandler(enabled = !isBusy, onBack = requestClose)

    val unlockNote: () -> Unit = {
        if (existingNote?.isLocked == true && !isUnlocked) {
            onRequestAuth("Unlock Protected Note", "Verify your identity to open this note") {
                isUnlocked = true
                title = existingNote.title
                contentValue = TextFieldValue(existingNote.content)
            }
        }
    }
    LaunchedEffect(existingNote?.id, existingNote?.isLocked) { unlockNote() }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var previewImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewImageTitle by remember { mutableStateOf<String?>(null) }

    // Undo / Redo history
    val undoStack = remember { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember { mutableStateListOf<TextFieldValue>() }

    // Active tooltip tag for bottom bar
    var activeTooltip by remember { mutableStateOf<String?>(null) }

    val noteStats by remember(contentValue.text) {
        derivedStateOf { NoteEditorLogic.calculateStats(contentValue.text) }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            localIsSaving = false
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    val attachmentScope = rememberCoroutineScope()
    fun importAttachment(uri: Uri?, imageOnly: Boolean) {
        if (uri == null) return
        attachmentScope.launch {
            var imported: Triple<ByteArray, String, String>? = null
            try {
                withContext(Dispatchers.IO) { imported = readOriginalNoteAttachment(context, uri, imageOnly) }
                pendingAttachments.add(checkNotNull(imported))
                imported = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                Toast.makeText(context, "Could not add this attachment. Choose a supported file up to 16 MB.", Toast.LENGTH_SHORT).show()
            } finally {
                imported?.first?.fill(0)
            }
        }
    }

    val visualMediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        importAttachment(uri, imageOnly = true)
    }

    val contentPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        importAttachment(uri, imageOnly = true)
    }

    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        importAttachment(uri, imageOnly = false)
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = pimsPageGutter(), vertical = PimsSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PersonaIconAction(PersonaIcons.Close, "Close editor", requestClose, enabled = !isBusy)

                if (isUnlocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PimsSpacing.xs)
                    ) {
                        PersonaIconAction(
                            icon = if (isLocked) PersonaIcons.LockOutlined else PersonaIcons.LockOpen,
                            label = if (isLocked) "Unlock note" else "Lock note",
                            enabled = !isBusy,
                            onClick = {
                                haptics.light()
                                if (isLocked) {
                                    onRequestAuth("Unlock note", "Verify your identity to remove note protection") {
                                        isLocked = false
                                    }
                                } else {
                                    isLocked = true
                                }
                            }
                        )

                        PersonaIconAction(
                            icon = PersonaIcons.ContentCopy,
                            label = "Copy note text",
                            enabled = !isBusy,
                            onClick = {
                                haptics.light()
                                val fullText = buildString {
                                    if (title.isNotBlank()) appendLine(title)
                                    if (contentValue.text.isNotBlank()) append(contentValue.text)
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Persona Note", fullText))
                                Toast.makeText(context, "Note copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )

                        PersonaIconAction(
                            icon = PersonaIcons.Share,
                            label = "Share note",
                            enabled = !isBusy,
                            onClick = {
                                haptics.light()
                                val fullText = buildString {
                                    if (title.isNotBlank()) appendLine(title)
                                    if (contentValue.text.isNotBlank()) append(contentValue.text)
                                }
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, title.ifBlank { "Note" })
                                    putExtra(Intent.EXTRA_TEXT, fullText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share note"))
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (isUnlocked) {
                Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                // Interactive Hashtag Auto-Suggest Bar
                val cursor = contentValue.selection.start.coerceIn(0, contentValue.text.length)
                val textBeforeCursor = contentValue.text.take(cursor)
                val lastHashIndex = textBeforeCursor.lastIndexOf('#')
                val isTypingTag = if (lastHashIndex >= 0) {
                    val tagSub = textBeforeCursor.substring(lastHashIndex + 1)
                    (lastHashIndex == 0 || textBeforeCursor[lastHashIndex - 1].isWhitespace()) &&
                        !tagSub.contains(' ') && !tagSub.contains('\n') && !tagSub.contains(',')
                } else false

                val activeTagPrefix = if (isTypingTag) textBeforeCursor.substring(lastHashIndex + 1) else ""
                val tagSuggestions = remember(activeTagPrefix, isTypingTag, state.availableTags) {
                    if (!isTypingTag) emptyList()
                    else if (activeTagPrefix.isEmpty()) state.availableTags.take(8)
                    else state.availableTags.filter { tag ->
                        tag.startsWith(activeTagPrefix, ignoreCase = true) || tag.contains(activeTagPrefix, ignoreCase = true)
                    }.take(8)
                }

                if (isTypingTag && tagSuggestions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tagSuggestions.forEach { suggestion ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .tactilePress()
                                    .clickable {
                                        haptics.light()
                                        val beforeHash = textBeforeCursor.substring(0, lastHashIndex)
                                        val afterCursor = contentValue.text.substring(cursor)
                                        val replacement = "#$suggestion "
                                        contentValue = TextFieldValue(
                                            text = beforeHash + replacement + afterCursor,
                                            selection = androidx.compose.ui.text.TextRange(beforeHash.length + replacement.length)
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = PersonaIcons.Tag,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = suggestion,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Horizontally scrollable formatting toolbar
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(end = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            // 1. Undo
                            WarmTooltip(
                                text = "Undo",
                                visible = activeTooltip == "undo",
                                onDismissRequest = { activeTooltip = null }
                            ) {
                                IconButton(
                                    onClick = {
                                        activeTooltip = "undo"
                                        if (undoStack.isNotEmpty()) {
                                            haptics.light()
                                            redoStack.add(contentValue)
                                            contentValue = undoStack.removeAt(undoStack.lastIndex)
                                        }
                                    },
                                    enabled = undoStack.isNotEmpty(),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = PersonaIcons.Undo,
                                        contentDescription = "Undo",
                                        tint = if (undoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            // 2. Redo
                            WarmTooltip(
                                text = "Redo",
                                visible = activeTooltip == "redo",
                                onDismissRequest = { activeTooltip = null }
                            ) {
                                IconButton(
                                    onClick = {
                                        activeTooltip = "redo"
                                        if (redoStack.isNotEmpty()) {
                                            haptics.light()
                                            undoStack.add(contentValue)
                                            contentValue = redoStack.removeAt(redoStack.lastIndex)
                                        }
                                    },
                                    enabled = redoStack.isNotEmpty(),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = PersonaIcons.Redo,
                                        contentDescription = "Redo",
                                        tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            // 3. Checklist
                            WarmTooltip(
                                text = "Checklist",
                                visible = activeTooltip == "checklist",
                                onDismissRequest = { activeTooltip = null }
                            ) {
                                IconButton(
                                    onClick = {
                                        activeTooltip = "checklist"
                                        haptics.light()
                                        undoStack.add(contentValue)
                                        contentValue = NoteEditorLogic.toggleChecklistAtCurrentLine(contentValue)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = PersonaIcons.CheckBox,
                                        contentDescription = "Checklist item",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            // 4. Bullet List
                            WarmTooltip(
                                text = if (format == NoteFormat.BULLETS) "List active" else "Bullet list",
                                visible = activeTooltip == "bullet",
                                onDismissRequest = { activeTooltip = null }
                            ) {
                                IconButton(
                                    onClick = {
                                        activeTooltip = "bullet"
                                        haptics.light()
                                        undoStack.add(contentValue)
                                        format = if (format == NoteFormat.BULLETS) NoteFormat.PLAIN else NoteFormat.BULLETS
                                        contentValue = NoteEditorLogic.toggleBulletAtCurrentLine(contentValue)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = PersonaIcons.FormatListBulleted,
                                        contentDescription = "Bullet List",
                                        tint = if (format == NoteFormat.BULLETS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            // 5. Numbered List
                            WarmTooltip(
                                text = "Numbered list",
                                visible = activeTooltip == "numbered",
                                onDismissRequest = { activeTooltip = null }
                            ) {
                                IconButton(
                                    onClick = {
                                        activeTooltip = "numbered"
                                        haptics.light()
                                        undoStack.add(contentValue)
                                        contentValue = NoteEditorLogic.toggleNumberedListAtCurrentLine(contentValue)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = PersonaIcons.FormatListNumbered,
                                        contentDescription = "Numbered List",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            // 6. Bold Text
                            WarmTooltip(
                                text = "Bold text",
                                visible = activeTooltip == "bold",
                                onDismissRequest = { activeTooltip = null }
                            ) {
                                IconButton(
                                    onClick = {
                                        activeTooltip = "bold"
                                        haptics.light()
                                        undoStack.add(contentValue)
                                        contentValue = NoteEditorLogic.wrapSelectionWith(contentValue, "**")
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = PersonaIcons.FormatBold,
                                        contentDescription = "Bold",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            // 7. Heading
                            WarmTooltip(
                                text = "Heading",
                                visible = activeTooltip == "heading",
                                onDismissRequest = { activeTooltip = null }
                            ) {
                                IconButton(
                                    onClick = {
                                        activeTooltip = "heading"
                                        haptics.light()
                                        undoStack.add(contentValue)
                                        contentValue = NoteEditorLogic.toggleHeadingAtCurrentLine(contentValue, level = 1)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = PersonaIcons.Title,
                                        contentDescription = "Heading",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            // 8. Insert Timestamp
                            WarmTooltip(
                                text = "Insert date & time",
                                visible = activeTooltip == "timestamp",
                                onDismissRequest = { activeTooltip = null }
                            ) {
                                IconButton(
                                    onClick = {
                                        activeTooltip = "timestamp"
                                        haptics.light()
                                        undoStack.add(contentValue)
                                        val timestamp = SimpleDateFormat("[MMM d, h:mm a] ", Locale.getDefault()).format(Date())
                                        val cursor = contentValue.selection.start
                                        val newText = contentValue.text.substring(0, cursor) + timestamp + contentValue.text.substring(cursor)
                                        contentValue = TextFieldValue(
                                            text = newText,
                                            selection = TextRange(cursor + timestamp.length)
                                        )
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = PersonaIcons.Schedule,
                                        contentDescription = "Insert Date & Time",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            // 9. Add Photo
                            WarmTooltip(
                                text = "Attach photo",
                                visible = activeTooltip == "photo",
                                onDismissRequest = { activeTooltip = null }
                            ) {
                                IconButton(
                                    onClick = {
                                        activeTooltip = "photo"
                                        haptics.light()
                                        try {
                                            visualMediaPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        } catch (_: Exception) {
                                            contentPicker.launch("image/*")
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = PersonaIcons.Photo,
                                        contentDescription = "Add Photo",
                                        tint = if (pendingAttachments.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            // 10. Add Document / PDF
                            WarmTooltip(
                                text = "Attach document / PDF",
                                visible = activeTooltip == "document",
                                onDismissRequest = { activeTooltip = null }
                            ) {
                                IconButton(
                                    onClick = {
                                        activeTooltip = "document"
                                        haptics.light()
                                        try {
                                            documentPicker.launch("*/*")
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Could not open the document picker.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = PersonaIcons.AttachFile,
                                        contentDescription = "Attach Document",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            // 11. Set Reminder
                            WarmTooltip(
                                text = if (reminderAt != null) "Reminder set" else "Set reminder",
                                visible = activeTooltip == "reminder",
                                onDismissRequest = { activeTooltip = null }
                            ) {
                                IconButton(
                                    onClick = {
                                        activeTooltip = "reminder"
                                        haptics.selection()
                                        showReminderDialog = true
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (reminderAt != null) PersonaIcons.NotificationsActive else PersonaIcons.Alarm,
                                        contentDescription = "Set Reminder",
                                        tint = if (reminderAt != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Cancel & Save actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PimsSpacing.s)
                    ) {
                        PersonaAction(
                            label = "Cancel",
                            onClick = requestClose,
                            enabled = !isBusy,
                            style = PersonaActionStyle.Text
                        )

                        PersonaAction(
                            label = if (isBusy) "Saving…" else "Save",
                            icon = PersonaIcons.Check,
                            inProgress = isBusy,
                            onClick = {
                                if (isBusy) return@PersonaAction
                                localIsSaving = true
                                val targetId = currentNoteId ?: UUID.randomUUID().toString().also { currentNoteId = it }
                                val finishSave = {
                                    localIsSaving = false
                                    haptics.success()
                                    feedback.success()
                                    onBack()
                                    onSavedLocally()
                                }

                                val hashtagRegex = Regex("""#([A-Za-z0-9_]+)""")
                                val extractedTags = hashtagRegex.findAll("$title ${contentValue.text}").map { it.groupValues[1].trim() }.filter { it.isNotEmpty() }.toList()
                                val existingTags = reminderTag?.split(",")?.map { it.trim().removePrefix("#") }?.filter { it.isNotEmpty() } ?: emptyList()
                                val allStoredTags = (existingTags + extractedTags).distinct()
                                val finalTagsToStore = allStoredTags.joinToString(",").ifBlank { null }

                                if (pendingAttachments.isNotEmpty()) {
                                    viewModel.saveWithMultipleAttachments(
                                        id = targetId,
                                        title = title,
                                        format = format,
                                        content = contentValue.text,
                                        reminderAt = reminderAt,
                                        reminderTag = finalTagsToStore,
                                        reminderRepeat = reminderRepeat,
                                        isLocked = isLocked,
                                        attachments = pendingAttachments.toList(),
                                        onComplete = {
                                            pendingAttachments.forEach { (bytes, _) -> bytes.fill(0) }
                                            pendingAttachments.clear()
                                            finishSave()
                                        }
                                    )
                                } else {
                                    viewModel.save(
                                        id = targetId,
                                        title = title,
                                        format = format,
                                        content = contentValue.text,
                                        reminderAt = reminderAt,
                                        reminderTag = finalTagsToStore,
                                        reminderRepeat = reminderRepeat,
                                        isLocked = isLocked,
                                        onComplete = finishSave
                                    )
                                }
                            }
                        )
                    }
                }
            }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (!isUnlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(PimsSpacing.l),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(PimsSpacing.actionGap),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PersonaIconBadge(
                        imageVector = PersonaIcons.LockOutlined,
                        contentDescription = null,
                        size = PimsControlSize.informationRow,
                        iconSize = PersonaIconSize.Large
                    )
                    Text("Protected note", style = MaterialTheme.typography.titleLarge)
                    PersonaAction("Unlock note", unlockNote, icon = PersonaIcons.LockOpen)
                    PersonaAction("Back", requestClose, style = PersonaActionStyle.Text)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = com.pims.vault.presentation.ui.theme.pimsPageGutter(), vertical = PimsDimensions.paddingSmall)
            ) {
            // Big Bold Title TextField
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (title.isEmpty()) {
                        Text(
                            text = "Untitled",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Thin outlineVariant divider beneath title
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Active Note Tags Row (#tags)
            val activeNoteTags = remember(title, contentValue.text, reminderTag) {
                val hashtagRegex = Regex("""#([A-Za-z0-9_]+)""")
                val extracted = hashtagRegex.findAll("$title ${contentValue.text}").map { it.groupValues[1].trim() }.filter { it.isNotEmpty() }.toList()
                val fromRem = reminderTag?.split(",")?.map { it.trim().removePrefix("#") }?.filter { it.isNotEmpty() } ?: emptyList()
                (fromRem + extracted).distinct()
            }

            if (activeNoteTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    activeNoteTags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = PersonaIcons.Tag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Attached Photos and Documents section (horizontal scroll if multiple)
            val visibleExisting = remember(existingNote?.attachments, deletedAttachmentIds.toList()) {
                existingNote?.attachments.orEmpty().filter { it.id !in deletedAttachmentIds }
            }

            if (pendingAttachments.isNotEmpty() || visibleExisting.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pending attachments
                    pendingAttachments.forEachIndexed { index, (bytes, name) ->
                        val bmp = remember(bytes) { decodePhotoPreview(bytes) }
                        if (bmp != null) {
                            Box(
                                modifier = Modifier
                                    .size(width = 150.dp, height = 110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        attachmentScope.launch {
                                            previewImageBitmap = withContext(Dispatchers.IO) { decodePhotoPreview(bytes, 1920) }
                                            previewImageTitle = name
                                        }
                                    }
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = {
                                        haptics.light()
                                        pendingAttachments.removeAt(index).first.fill(0)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(PimsColors.mediaScrim.copy(alpha = 0.65f))
                                ) {
                                    Icon(
                                        imageVector = PersonaIcons.Close,
                                        contentDescription = "Remove attachment",
                                        tint = PimsColors.onMediaScrim,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        } else {
                            // Non-image document attachment card
                            Surface(
                                modifier = Modifier
                                    .size(width = 150.dp, height = 110.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                    Column(
                                        modifier = Modifier.align(Alignment.Center),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = PersonaIcons.Documents,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            haptics.light()
                                            pendingAttachments.removeAt(index).first.fill(0)
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                    ) {
                                        Icon(
                                            imageVector = PersonaIcons.Close,
                                            contentDescription = "Remove attachment",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Existing saved attachments
                    visibleExisting.forEach { att ->
                        var bmp by remember(att.id) { mutableStateOf<Bitmap?>(null) }
                        var isDocType by remember(att.id) {
                            mutableStateOf(att.mimeType == "application/pdf" || att.caption?.endsWith(".pdf", ignoreCase = true) == true)
                        }
                        LaunchedEffect(att.id) {
                            if (!isDocType) {
                                val bytes = viewModel.readAttachment(att)
                                if (bytes != null) {
                                    val decoded = try {
                                        withContext(Dispatchers.IO) { decodePhotoPreview(bytes) }
                                    } finally {
                                        bytes.fill(0)
                                    }
                                    if (decoded != null) bmp = decoded else isDocType = true
                                }
                            }
                        }
                        if (bmp != null) {
                            Box(
                                modifier = Modifier
                                    .size(width = 150.dp, height = 110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        attachmentScope.launch {
                                            val bytes = viewModel.readAttachment(att) ?: return@launch
                                            previewImageBitmap = try {
                                                withContext(Dispatchers.IO) { decodePhotoPreview(bytes, 1920) }
                                            } finally {
                                                bytes.fill(0)
                                            }
                                            previewImageTitle = att.caption
                                        }
                                    }
                            ) {
                                Image(
                                    bitmap = bmp!!.asImageBitmap(),
                                    contentDescription = att.caption,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = {
                                        haptics.light()
                                        deletedAttachmentIds.add(att.id)
                                        viewModel.deleteAttachment(att)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(PimsColors.mediaScrim.copy(alpha = 0.65f))
                                ) {
                                    Icon(
                                        imageVector = PersonaIcons.Close,
                                        contentDescription = "Remove attachment",
                                        tint = PimsColors.onMediaScrim,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        } else {
                            Surface(
                                modifier = Modifier
                                    .size(width = 150.dp, height = 110.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                    Column(
                                        modifier = Modifier.align(Alignment.Center),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = PersonaIcons.Documents,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = att.caption ?: "Document",
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            haptics.light()
                                            deletedAttachmentIds.add(att.id)
                                            viewModel.deleteAttachment(att)
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                    ) {
                                        Icon(
                                            imageVector = PersonaIcons.Close,
                                            contentDescription = "Remove attachment",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Body TextField with 26.sp line height
            BasicTextField(
                value = contentValue,
                onValueChange = { newValue ->
                    if (newValue.text != contentValue.text) {
                        undoStack.add(contentValue)
                        redoStack.clear()
                    }
                    val textWithCommaHandled = if (newValue.text.contains(Regex("""#([A-Za-z0-9_]+),"""))) {
                        val replaced = newValue.text.replace(Regex("""#([A-Za-z0-9_]+),""")) { match ->
                            "#${match.groupValues[1]} "
                        }
                        newValue.copy(text = replaced)
                    } else newValue
                    contentValue = NoteEditorLogic.handleEnterKeyForBullets(contentValue, textWithCommaHandled)
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 26.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (contentValue.text.isEmpty()) {
                        Text(
                            text = "Write something down...",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                lineHeight = 26.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                            )
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
        }
    }

    if (showDiscardConfirmation) {
        PersonaConfirmationDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = "Discard your changes?",
            message = "Your note edits and added attachments have not been saved.",
            confirmText = "Discard changes",
            dismissText = "Keep editing",
            isDestructive = true,
            onConfirm = {
                pendingAttachments.forEach { (bytes, _) -> bytes.fill(0) }
                pendingAttachments.clear()
                showDiscardConfirmation = false
                onBack()
            }
        )
    }

    if (showStatsDialog) {
        NoteStatsDialog(
            stats = noteStats,
            content = contentValue.text,
            onDismissRequest = { showStatsDialog = false }
        )
    }

    if (previewImageBitmap != null) {
        FullscreenImageViewerDialog(
            bitmap = previewImageBitmap,
            title = previewImageTitle,
            onDismissRequest = {
                previewImageBitmap = null
                previewImageTitle = null
            }
        )
    }

    if (showReminderDialog) {
        NoteReminderDialog(
            initialReminderAt = reminderAt,
            initialTag = reminderTag,
            initialRepeat = reminderRepeat,
            noteTitle = title.ifBlank { "Untitled Note" },
            onDismissRequest = { showReminderDialog = false },
            onSetReminder = { triggerAt, tag, repeat ->
                reminderAt = triggerAt
                reminderTag = tag
                reminderRepeat = repeat
                isReminderDone = false
                val targetId = currentNoteId ?: UUID.randomUUID().toString().also { currentNoteId = it }
                viewModel.setReminder(targetId, triggerAt, tag, repeat)
                val formatted = SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault()).format(Date(triggerAt))
                Toast.makeText(context, "Reminder scheduled for $formatted", Toast.LENGTH_SHORT).show()
            },
            onClearReminder = {
                reminderAt = null
                reminderTag = null
                reminderRepeat = null
                isReminderDone = false
                currentNoteId?.let { id ->
                    viewModel.clearReminder(id)
                }
                Toast.makeText(context, "Reminder cleared", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

private fun readOriginalNoteAttachment(context: Context, uri: Uri, imageOnly: Boolean): Triple<ByteArray, String, String> {
    val resolver = context.contentResolver
    val bytes = checkNotNull(resolver.openInputStream(uri)) { "Attachment is unavailable" }
        .use { it.readBoundedBytes(MAX_ORIGINAL_PHOTO_BYTES.toInt()) }
    try {
        require(bytes.size.toLong() in 1..MAX_ORIGINAL_PHOTO_BYTES)
        val photoMime = inspectPhoto(bytes)
        if (imageOnly) {
            checkNotNull(photoMime) { "Unknown photo format" }
            checkNotNull(decodePhotoPreview(bytes, 256)) { "Invalid photo" }.recycle()
        }
        val displayName = resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            ?.takeIf(String::isNotBlank)
            ?: if (photoMime != null) "photo_${System.currentTimeMillis()}.${photoExtension(photoMime)}" else "Attachment"
        val mimeType = photoMime ?: resolver.getType(uri)
            ?: java.net.URLConnection.guessContentTypeFromName(displayName)
            ?: "application/octet-stream"
        return Triple(bytes, displayName, mimeType)
    } catch (failure: Exception) {
        bytes.fill(0)
        throw failure
    }
}

