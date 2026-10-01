package com.pims.vault.presentation.notes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pims.vault.domain.model.NoteAttachment
import com.pims.vault.domain.model.NoteFormat
import com.pims.vault.domain.model.PlainNote
import com.pims.vault.presentation.ui.components.ux.WarmTooltip
import com.pims.vault.presentation.ui.theme.PersonaIcons
import com.pims.vault.presentation.ui.theme.PimsDimensions
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
    viewModel: PlainNotesViewModel = hiltViewModel()
) {
    val haptics = rememberPimsHaptics()
    val feedback = rememberPimsFeedback()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val existingNote = remember(noteId, state.notes) {
        state.notes.find { it.id == noteId }
    }

    var currentNoteId by remember(noteId) { mutableStateOf(noteId) }
    var localIsSaving by remember { mutableStateOf(false) }
    val isBusy = state.isSaving || localIsSaving

    BackHandler(enabled = !isBusy, onBack = onBack)

    var title by remember { mutableStateOf(existingNote?.title ?: "") }
    var contentValue by remember {
        mutableStateOf(TextFieldValue(existingNote?.content ?: ""))
    }
    var format by remember { mutableStateOf(existingNote?.format ?: NoteFormat.PLAIN) }
    var reminderAt by remember(existingNote) { mutableStateOf(existingNote?.reminderAt) }
    var reminderTag by remember(existingNote) { mutableStateOf(existingNote?.reminderTag) }
    var reminderRepeat by remember(existingNote) { mutableStateOf(existingNote?.reminderRepeat) }
    var isReminderDone by remember(existingNote) { mutableStateOf(existingNote?.isReminderDone ?: false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var previewImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewImageTitle by remember { mutableStateOf<String?>(null) }

    // Multiple pending attachments: list of (ByteArray, DisplayName)
    val pendingAttachments = remember { mutableStateListOf<Pair<ByteArray, String>>() }
    val deletedAttachmentIds = remember { mutableStateListOf<String>() }

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

    val visualMediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            processAndAttachImage(context, it) { bytes, name ->
                pendingAttachments.add(bytes to name)
            }
        }
    }

    val contentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            processAndAttachImage(context, it) { bytes, name ->
                pendingAttachments.add(bytes to name)
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = PimsDimensions.paddingMedium, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // X Close
                IconButton(
                    onClick = { if (!isBusy) onBack() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = PersonaIcons.Close,
                        contentDescription = "Close editor",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Center/Right actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Live Note Stats Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clickable { showStatsDialog = true }
                            .padding(end = 4.dp)
                    ) {
                        Text(
                            text = "${noteStats.wordCount} words",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Copy Note Text Button
                    IconButton(
                        onClick = {
                            haptics.light()
                            val fullText = buildString {
                                if (title.isNotBlank()) appendLine(title)
                                if (contentValue.text.isNotBlank()) append(contentValue.text)
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Persona Note", fullText))
                            Toast.makeText(context, "Note copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy note text",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Share Note Button
                    IconButton(
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
                            context.startActivity(Intent.createChooser(shareIntent, "Share Note"))
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share note",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Reminder Button
                    IconButton(
                        onClick = {
                            haptics.selection()
                            showReminderDialog = true
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (reminderAt != null) Icons.Default.NotificationsActive else Icons.Default.Alarm,
                            contentDescription = "Set Reminder",
                            tint = if (reminderAt != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
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
                                        imageVector = Icons.AutoMirrored.Filled.Undo,
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
                                        imageVector = Icons.AutoMirrored.Filled.Redo,
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
                                        imageVector = Icons.Default.CheckBox,
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
                                        imageVector = Icons.Default.FormatListBulleted,
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
                                        imageVector = Icons.Default.FormatListNumbered,
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
                                        imageVector = Icons.Default.FormatBold,
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
                                        imageVector = Icons.Default.Title,
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
                                        imageVector = Icons.Default.Schedule,
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
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = "Add Photo",
                                        tint = if (pendingAttachments.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Cancel & Save actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = onBack,
                            enabled = !isBusy,
                            modifier = Modifier.tactilePress()
                        ) {
                            Text(
                                text = "Cancel",
                                color = if (isBusy) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        Button(
                            onClick = {
                                if (isBusy) return@Button
                                localIsSaving = true
                                val targetId = currentNoteId ?: UUID.randomUUID().toString().also { currentNoteId = it }
                                haptics.success()
                                feedback.success()

                                if (pendingAttachments.isNotEmpty()) {
                                    viewModel.saveWithMultipleAttachments(
                                        id = targetId,
                                        title = title,
                                        format = format,
                                        content = contentValue.text,
                                        reminderAt = reminderAt,
                                        reminderTag = reminderTag,
                                        reminderRepeat = reminderRepeat,
                                        attachments = pendingAttachments.toList(),
                                        onComplete = onBack
                                    )
                                } else {
                                    viewModel.save(
                                        id = targetId,
                                        title = title,
                                        format = format,
                                        content = contentValue.text,
                                        reminderAt = reminderAt,
                                        reminderTag = reminderTag,
                                        reminderRepeat = reminderRepeat,
                                        onComplete = onBack
                                    )
                                }
                            },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            ),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                            modifier = Modifier.tactilePress()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isBusy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Text(
                                        text = "Saving...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Save",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = PimsDimensions.paddingLarge, vertical = PimsDimensions.paddingSmall)
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

            // "Remind me" Chip
            val hasActiveReminder = reminderAt != null && reminderAt!! > System.currentTimeMillis()
            val isReminderPast = reminderAt != null && reminderAt!! <= System.currentTimeMillis()
            val remSdf = remember { SimpleDateFormat("EEE, MMM d, h:mm a", Locale.getDefault()) }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (hasActiveReminder) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    1.dp,
                    if (hasActiveReminder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.clickable {
                    haptics.selection()
                    showReminderDialog = true
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (hasActiveReminder) Icons.Default.NotificationsActive
                        else if (isReminderPast) Icons.Default.Check
                        else Icons.Default.Alarm,
                        contentDescription = null,
                        tint = if (hasActiveReminder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = when {
                            hasActiveReminder -> "Reminder: ${remSdf.format(Date(reminderAt!!))}${if (!reminderTag.isNullOrBlank()) " • [$reminderTag]" else ""}${if (!reminderRepeat.isNullOrBlank() && reminderRepeat != "NONE") " (${reminderRepeat?.lowercase()})" else ""}"
                            isReminderPast -> "Reminder completed${if (!reminderTag.isNullOrBlank()) " • [$reminderTag]" else ""}"
                            else -> "Remind me"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = if (hasActiveReminder) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )

                    if (reminderAt != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear reminder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    haptics.light()
                                    reminderAt = null
                                    reminderTag = null
                                    reminderRepeat = null
                                    isReminderDone = false
                                    currentNoteId?.let { viewModel.clearReminder(it) }
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Attached Photos section (horizontal scroll if multiple)
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
                    // Pending photos
                    pendingAttachments.forEachIndexed { index, (bytes, name) ->
                        val bmp = remember(bytes) {
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                        bmp?.let {
                            Box(
                                modifier = Modifier
                                    .size(width = 150.dp, height = 110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        previewImageBitmap = it
                                        previewImageTitle = name
                                    }
                            ) {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = {
                                        haptics.light()
                                        pendingAttachments.removeAt(index)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.65f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Existing saved photos
                    visibleExisting.forEach { att ->
                        var bmp by remember(att.id) { mutableStateOf<Bitmap?>(null) }
                        LaunchedEffect(att.id) {
                            val bytes = viewModel.readAttachment(att)
                            if (bytes != null) {
                                bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            }
                        }
                        bmp?.let { loadedBmp ->
                            Box(
                                modifier = Modifier
                                    .size(width = 150.dp, height = 110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        previewImageBitmap = loadedBmp
                                        previewImageTitle = att.caption
                                    }
                            ) {
                                Image(
                                    bitmap = loadedBmp.asImageBitmap(),
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
                                        .background(Color.Black.copy(alpha = 0.65f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
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
                    contentValue = NoteEditorLogic.handleEnterKeyForBullets(contentValue, newValue)
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

private fun processAndAttachImage(
    context: Context,
    uri: Uri,
    onSuccess: (ByteArray, String) -> Unit
) {
    try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val rawBytes = stream.readBytes()
            if (rawBytes.isEmpty()) return

            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)

            val maxDimension = 1920
            var sampleSize = 1
            var w = boundsOptions.outWidth
            var h = boundsOptions.outHeight
            while (w > maxDimension || h > maxDimension) {
                sampleSize *= 2
                w /= 2
                h /= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
            if (bmp != null) {
                val baos = java.io.ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                val compressedBytes = baos.toByteArray()
                onSuccess(compressedBytes, "photo_${System.currentTimeMillis()}.jpg")
            } else {
                onSuccess(rawBytes, "photo_${System.currentTimeMillis()}.jpg")
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("PlainNoteEditor", "Failed to process photo", e)
        Toast.makeText(context, "Failed to load selected photo", Toast.LENGTH_SHORT).show()
    }
}
