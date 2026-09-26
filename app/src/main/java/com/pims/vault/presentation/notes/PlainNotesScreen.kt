package com.pims.vault.presentation.notes

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
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
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.presentation.ui.util.rememberPimsFeedback
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlainNotesScreen(
    onOpenEditor: (String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlainNotesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    var noteToDelete by remember { mutableStateOf<PlainNote?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (state.notes.isEmpty()) {
            // Replicates screen_notes.png: warm background with centered terracotta icon and text
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "No notes yet",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Button(
                        onClick = { onOpenEditor(null) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.tactilePress()
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Note", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 48.dp,
                    bottom = 120.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.notes, key = { it.id }) { note ->
                    NoteGridCard(
                        note = note,
                        dateStr = dateFormat.format(Date(note.updatedAt)),
                        onClick = { onOpenEditor(note.id) },
                        onDelete = { noteToDelete = note },
                        viewModel = viewModel
                    )
                }
            }
        }

        noteToDelete?.let { note ->
            AlertDialog(
                onDismissRequest = { noteToDelete = null },
                title = { Text("Delete Note") },
                text = { Text("Are you sure you want to delete '${note.title.ifBlank { "Untitled" }}'?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.delete(note)
                            noteToDelete = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { noteToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun NoteGridCard(
    note: PlainNote,
    dateStr: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    viewModel: PlainNotesViewModel? = null
) {
    val firstAttachment = remember(note.attachments) { note.attachments.firstOrNull() }
    var thumbnailBitmap by remember(firstAttachment?.id) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(firstAttachment?.id) {
        if (firstAttachment != null && viewModel != null) {
            val bytes = viewModel.readAttachment(firstAttachment)
            if (bytes != null) {
                thumbnailBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .tactilePress(onClick = onClick)
    ) {
        Column {
            thumbnailBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(105.dp)
                )
            }
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                if (note.attachments.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${note.attachments.size}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
}

/**
 * Note Editor Screen - Replicates screen_new_note.png
 */
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
    val existingNote = remember(noteId, state.notes) {
        state.notes.find { it.id == noteId }
    }

    // Stable ID for the note during this editor session (prevents duplicate creations on repeated clicks)
    var currentNoteId by remember(noteId) { mutableStateOf(noteId) }
    var localIsSaving by remember { mutableStateOf(false) }
    val isBusy = state.isSaving || localIsSaving

    BackHandler(enabled = !isBusy, onBack = onBack)

    var title by remember { mutableStateOf(existingNote?.title ?: "") }
    var contentValue by remember {
        mutableStateOf(TextFieldValue(existingNote?.content ?: ""))
    }
    var format by remember { mutableStateOf(existingNote?.format ?: NoteFormat.PLAIN) }
    var pendingAttachmentBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingAttachmentName by remember { mutableStateOf("") }

    // Undo / Redo history
    val undoStack = remember { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember { mutableStateListOf<TextFieldValue>() }

    val context = LocalContext.current
    LaunchedEffect(state.error) {
        state.error?.let {
            localIsSaving = false
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    val visualMediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            processAndAttachImage(context, it) { bytes, name ->
                pendingAttachmentBytes = bytes
                pendingAttachmentName = name
            }
        }
    }

    val contentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            processAndAttachImage(context, it) { bytes, name ->
                pendingAttachmentBytes = bytes
                pendingAttachmentName = name
            }
        }
    }

    var activeTooltip by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            // Clean minimal title header: no heavy back bar or 'New note' subtitle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        bottomBar = {
            // Minimalist, low-profile bottom bar with zero prominent background color, sitting just 3% above keyboard/bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minimal Tool Icons with WarmTooltip (Zero prominent background color)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.pims.vault.presentation.ui.components.ux.WarmTooltip(
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
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        com.pims.vault.presentation.ui.components.ux.WarmTooltip(
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
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        com.pims.vault.presentation.ui.components.ux.WarmTooltip(
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
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        com.pims.vault.presentation.ui.components.ux.WarmTooltip(
                            text = if (pendingAttachmentBytes != null) "Photo attached" else "Attach photo",
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
                                    tint = if (pendingAttachmentBytes != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Cancel and Save actions (just 3% above bottom/keyboard)
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
                                val targetId = currentNoteId ?: java.util.UUID.randomUUID().toString().also { currentNoteId = it }
                                haptics.success()
                                feedback.success()
                                if (pendingAttachmentBytes != null) {
                                    viewModel.saveWithAttachment(
                                        id = targetId,
                                        title = title,
                                        format = format,
                                        content = contentValue.text,
                                        attachmentBytes = pendingAttachmentBytes,
                                        displayName = pendingAttachmentName,
                                        onComplete = onBack
                                    )
                                } else {
                                    viewModel.save(
                                        id = targetId,
                                        title = title,
                                        format = format,
                                        content = contentValue.text,
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
                .padding(horizontal = 24.dp, vertical = 6.dp)
        ) {
            // Big Bold Title TextField
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = TextStyle(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (title.isEmpty()) {
                        Text(
                            text = "Untitled",
                            style = TextStyle(
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f)
                            )
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Attached Photo Preview (No redundant subtitle above it)
            if (pendingAttachmentBytes != null) {
                val bmp = remember(pendingAttachmentBytes) {
                    pendingAttachmentBytes?.let {
                        BitmapFactory.decodeByteArray(it, 0, it.size)
                    }
                }
                bmp?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Attached photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { pendingAttachmentBytes = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            existingNote?.attachments?.forEach { att ->
                NoteAttachmentPreview(
                    attachment = att,
                    viewModel = viewModel,
                    onDelete = { viewModel.deleteAttachment(att) }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Body TextField (replicates screen_new_note.png)
            BasicTextField(
                value = contentValue,
                onValueChange = { newValue ->
                    if (newValue.text != contentValue.text) {
                        undoStack.add(contentValue)
                        redoStack.clear()
                    }
                    contentValue = if (format == NoteFormat.BULLETS) {
                        NoteEditorLogic.handleEnterKeyForBullets(contentValue, newValue)
                    } else {
                        newValue
                    }
                },
                textStyle = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (contentValue.text.isEmpty()) {
                        Text(
                            text = "Write something down",
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
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

private fun processAndAttachImage(
    context: android.content.Context,
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
        android.util.Log.e("PlainNotesScreen", "Failed to process photo", e)
        android.widget.Toast.makeText(context, "Failed to load selected photo", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun NoteAttachmentPreview(
    attachment: NoteAttachment,
    viewModel: PlainNotesViewModel,
    onDelete: (() -> Unit)? = null
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(attachment.id) {
        val bytes = viewModel.readAttachment(attachment)
        if (bytes != null) {
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }

    bitmap?.let {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = attachment.caption,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove photo",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
