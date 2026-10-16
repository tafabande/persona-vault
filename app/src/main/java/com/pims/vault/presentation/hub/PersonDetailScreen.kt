package com.pims.vault.presentation.hub

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.sp
import com.pims.vault.domain.model.NoteFormat
import com.pims.vault.domain.model.RelationshipNote
import com.pims.vault.presentation.avatar.PersonaAvatarManager
import com.pims.vault.presentation.profile.KinRelationshipItem
import com.pims.vault.presentation.relationship.AddOrEditRelationshipNoteDialog
import com.pims.vault.presentation.relationship.RelationshipNotesSection
import com.pims.vault.presentation.ui.components.DefaultAvatar
import com.pims.vault.presentation.ui.theme.PersonaIcons
import com.pims.vault.presentation.ui.theme.StateError
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import java.io.File

/**
 * Dedicated full-screen page for displaying and managing all information about a person:
 * - Profile avatar with photo changer
 * - Name, relationship role, trusted contact toggle
 * - Contact methods (phone, email, address, DOB)
 * - Complete memory notes with in-place Add, Edit, Delete, and privacy controls
 * - Edit person and Delete connection actions
 */
@Composable
fun PersonDetailScreen(
    person: KinRelationshipItem,
    relationshipNotes: List<RelationshipNote>,
    onBack: () -> Unit,
    onEditPerson: (KinRelationshipItem) -> Unit,
    onDeletePerson: (KinRelationshipItem) -> Unit,
    onAddNote: (topic: String?, content: String, format: NoteFormat, isPrivate: Boolean) -> Unit,
    onUpdateNote: (noteId: String, topic: String?, content: String, format: NoteFormat, isPrivate: Boolean) -> Unit,
    onDeleteNote: (noteId: String) -> Unit,
    onToggleTrusted: (Boolean) -> Unit,
    onToggleNotePrivacy: (noteId: String, makePrivate: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberPimsHaptics()
    val avatarManager = remember { PersonaAvatarManager.getInstance(context) }
    val photosVersion by avatarManager.personPhotosVersion.collectAsState()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showAvatarActionChooser by remember { mutableStateOf(false) }

    // Note dialog state
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<RelationshipNote?>(null) }
    var noteToDelete by remember { mutableStateOf<RelationshipNote?>(null) }

    val contactPhotoPath = remember(person.id, photosVersion) {
        avatarManager.getPersonPhotoPath(person.id)
    }

    val saveUriToPhoto: (Uri) -> Unit = { uri ->
        try {
            val photosDir = java.io.File(context.filesDir, "contact_photos").apply { mkdirs() }
            val destFile = java.io.File(photosDir, "contact_${person.id}_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            avatarManager.setPersonPhotoPath(person.id, destFile.absolutePath)
            haptics.success()
        } catch (_: Exception) {}
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { saveUriToPhoto(it) }
    }

    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraTempUri != null) {
            saveUriToPhoto(cameraTempUri!!)
        }
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
        ) {
            // TOP APP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = person.fullName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onEditPerson(person) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Person",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Details") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onEditPerson(person)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Remove from People", color = StateError) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StateError) },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    }
                }
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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // AVATAR & HERO
                Box(contentAlignment = Alignment.BottomEnd) {
                    val hasPhoto = !contactPhotoPath.isNullOrBlank() && File(contactPhotoPath).exists()
                    val photoBitmap = remember(contactPhotoPath) {
                        if (hasPhoto) {
                            try { BitmapFactory.decodeFile(contactPhotoPath) } catch (_: Exception) { null }
                        } else null
                    }

                    if (photoBitmap != null) {
                        Image(
                            bitmap = photoBitmap.asImageBitmap(),
                            contentDescription = person.fullName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                                .clickable { showAvatarActionChooser = true }
                        )
                    } else {
                        DefaultAvatar(
                            name = person.fullName,
                            size = 96.dp,
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
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change photo",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Name & Role
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = person.fullName,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = person.relationRole.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        color = tonalSurfaces.surfaceGrouped,
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
                    isVaultUnlocked = true,
                    onAddNoteClick = {
                        noteToEdit = null
                        showNoteDialog = true
                    },
                    onUnlockVaultClick = {},
                    onEditNote = { note ->
                        noteToEdit = note
                        showNoteDialog = true
                    },
                    onDeleteNote = { note ->
                        noteToDelete = note
                    },
                    onTogglePrivacy = onToggleNotePrivacy,
                    modifier = Modifier.fillMaxWidth()
                )

                // Fallback note snippet from person record if no structured notes yet
                if (relationshipNotes.isEmpty() && person.notes.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = tonalSurfaces.surfaceGrouped,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Additional Notes",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = person.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom actions: edit is in top app bar, only quiet remove button here
                TextButton(
                    onClick = { showDeleteConfirmDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = StateError)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Remove from People")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // ADD / EDIT NOTE DIALOG
    if (showNoteDialog) {
        AddOrEditRelationshipNoteDialog(
            initialNote = noteToEdit,
            onDismiss = { showNoteDialog = false },
            onSave = { topic, content, format, isPrivate ->
                if (noteToEdit != null) {
                    onUpdateNote(noteToEdit!!.id, topic, content, format, isPrivate)
                } else {
                    onAddNote(topic, content, format, isPrivate)
                }
                showNoteDialog = false
                haptics.success()
            }
        )
    }

    // DELETE NOTE CONFIRMATION DIALOG
    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StateError) },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete this memory note? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteNote(note.id)
                        noteToDelete = null
                        haptics.warning()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StateError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // DELETE PERSON CONFIRMATION DIALOG
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StateError) },
            title = { Text("Remove from People") },
            text = {
                Text(
                    "Are you sure you want to remove ${person.fullName} from People? " +
                    "This disconnects their relationship link without affecting your vault secrets or other personal records."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        haptics.warning()
                        onDeletePerson(person)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StateError)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // PHOTO ACTION CHOOSER DIALOG
    if (showAvatarActionChooser) {
        AlertDialog(
            onDismissRequest = { showAvatarActionChooser = false },
            title = { Text("Contact Photo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showAvatarActionChooser = false
                            try {
                                val photoFile = File(context.cacheDir, "camera_person_${person.id}_${System.currentTimeMillis()}.jpg")
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    photoFile
                                )
                                cameraTempUri = uri
                                cameraLauncher.launch(uri)
                            } catch (_: Exception) {
                                photoPickerLauncher.launch("image/*")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Text("Take Photo")
                        }
                    }

                    TextButton(
                        onClick = {
                            showAvatarActionChooser = false
                            photoPickerLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Text("Choose from Gallery")
                        }
                    }

                    if (!contactPhotoPath.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                showAvatarActionChooser = false
                                avatarManager.setPersonPhotoPath(person.id, null)
                                haptics.warning()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = StateError)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Text("Remove Photo")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAvatarActionChooser = false }) {
                    Text("Cancel")
                }
            }
        )
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
