package com.pims.vault.presentation.hub

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import com.pims.vault.presentation.ui.theme.PersonaIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.pims.vault.presentation.ui.theme.tactilePress
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.domain.model.NoteFormat
import com.pims.vault.domain.model.RelationshipNote
import com.pims.vault.presentation.DateHelper
import com.pims.vault.presentation.ui.components.DefaultAvatar
import com.pims.vault.presentation.avatar.PersonaAvatarManager
import com.pims.vault.presentation.profile.KinRelationshipItem
import com.pims.vault.presentation.relationship.AddOrEditRelationshipNoteDialog
import com.pims.vault.presentation.relationship.RelationshipNotesSection
import com.pims.vault.presentation.ui.state.PersonaEmptyState
import com.pims.vault.presentation.ui.theme.StateError

private enum class PeopleCategory(val label: String) {
    ALL("All"),
    FAMILY("Family"),
    FRIENDS("Friends"),
    WORK("Work"),
    MEDICAL("Medical")
}

@Composable
fun PeopleView(
    relationships: List<KinRelationshipItem>,
    onAddPersonClick: () -> Unit,
    onSelectPerson: (KinRelationshipItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val avatarManager = remember { PersonaAvatarManager.getInstance(context) }
    val photosVersion by avatarManager.personPhotosVersion.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PeopleCategory.ALL) }

    var showLinkAccountDialog by remember { mutableStateOf(false) }
    var linkCodeInput by remember { mutableStateOf("") }
    var linkSuccessMessage by remember { mutableStateOf<String?>(null) }

    val familyRoles = remember {
        setOf("MOTHER", "FATHER", "PARENT", "SIBLING", "BROTHER", "SISTER", "CHILD", "SON", "DAUGHTER", "PARTNER", "SPOUSE", "WIFE", "HUSBAND", "RELATIVE", "COUSIN", "AUNT", "UNCLE", "GRANDMOTHER", "GRANDFATHER", "GRANDMA", "GRANDPA", "NANA", "PAPA", "MOM", "DAD")
    }
    val friendRoles = remember {
        setOf("FRIEND", "BEST FRIEND", "BEST_FRIEND", "ROOMMATE", "PAL", "BUDDY", "NEIGHBOR")
    }
    val workRoles = remember {
        setOf("COLLEAGUE", "COWORKER", "MANAGER", "BOSS", "DIRECTOR", "MENTOR", "SUPERVISOR", "LAWYER", "ATTORNEY", "ACCOUNTANT", "ADVISOR", "BUSINESS_PARTNER")
    }
    val medicalRoles = remember {
        setOf("DOCTOR", "PHYSICIAN", "SURGEON", "DENTIST", "NURSE", "THERAPIST", "PSYCHIATRIST", "PSYCHOLOGIST", "SPECIALIST", "PEDIATRICIAN")
    }

    val filteredList = remember(relationships, searchQuery, selectedCategory) {
        relationships.filter { person ->
            val matchesQuery = if (searchQuery.isBlank()) true else {
                person.fullName.contains(searchQuery, ignoreCase = true) ||
                person.relationRole.contains(searchQuery, ignoreCase = true)
            }
            val roleUpper = person.relationRole.trim().uppercase()
            val matchesCategory = when (selectedCategory) {
                PeopleCategory.ALL -> true
                PeopleCategory.FAMILY -> roleUpper in familyRoles
                PeopleCategory.FRIENDS -> roleUpper in friendRoles
                PeopleCategory.WORK -> roleUpper in workRoles
                PeopleCategory.MEDICAL -> roleUpper in medicalRoles
            }
            matchesQuery && matchesCategory
        }
    }

    val familyCount = remember(relationships) { relationships.count { it.relationRole.trim().uppercase() in familyRoles } }
    val friendsCount = remember(relationships) { relationships.count { it.relationRole.trim().uppercase() in friendRoles } }
    val workCount = remember(relationships) { relationships.count { it.relationRole.trim().uppercase() in workRoles } }
    val medicalCount = remember(relationships) { relationships.count { it.relationRole.trim().uppercase() in medicalRoles } }

    val haptics = com.pims.vault.presentation.ui.util.rememberPimsHaptics()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Screen Header: "People" + Search + Add buttons (matching design screenshot)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "People",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = Color(0xFF20201E)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search Button
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF0EBE1),
                        modifier = Modifier
                            .size(42.dp)
                            .tactilePress(targetScale = 0.90f) {
                                haptics.selection()
                                if (searchQuery.isNotBlank()) searchQuery = ""
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF20201E),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Add Person Button (+)
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF20201E),
                        modifier = Modifier
                            .size(42.dp)
                            .tactilePress(targetScale = 0.90f) {
                                haptics.light()
                                onAddPersonClick()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add someone",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // Inline Search Bar when searching
        if (searchQuery.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search connections...") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Empty state
        if (filteredList.isEmpty()) {
            item {
                PersonaEmptyState(
                    icon = Icons.Default.Person,
                    title = if (relationships.isEmpty()) "No people yet" else "No matching connections",
                    description = if (relationships.isEmpty()) "People you add will appear here with encrypted memory notes." else "Try adjusting your search or category filter.",
                    actionLabel = if (relationships.isEmpty()) "Add person" else null,
                    onActionClick = {
                        haptics.light()
                        onAddPersonClick()
                    }
                )
            }
        } else {
            items(filteredList, key = { it.id }) { person ->
                PersonListItem(
                    person = person,
                    photosVersion = photosVersion,
                    onClick = { onSelectPerson(person) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showLinkAccountDialog) {
        AlertDialog(
            onDismissRequest = { showLinkAccountDialog = false },
            icon = { Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Link Existing Person Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter the person's secure Link Code. A linking request will be sent and requires their explicit approval before profiles synchronize.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = linkCodeInput,
                        onValueChange = { linkCodeInput = it.uppercase().take(12) },
                        placeholder = { Text("e.g. PV-7482-X9") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (linkCodeInput.isNotBlank()) {
                            haptics.success()
                            showLinkAccountDialog = false
                            linkSuccessMessage = "Link request sent to $linkCodeInput. Waiting for approval."
                            linkCodeInput = ""
                        }
                    },
                    enabled = linkCodeInput.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                    )
                ) {
                    Text("Send Request", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                FilledTonalButton(
                    onClick = { showLinkAccountDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    linkSuccessMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { linkSuccessMessage = null },
            icon = { Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Link Request Pending") },
            text = {
                Text(
                    text = "$msg\n\nWhen they approve, the connection will update from Manual to Linked without duplicating their record.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = { linkSuccessMessage = null }) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
private fun PersonListItem(
    person: KinRelationshipItem,
    photosVersion: Long = 0L,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .tactilePress(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.5.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        val context = LocalContext.current
        val avatarManager = remember { PersonaAvatarManager.getInstance(context) }
        val contactPhoto = remember(person.id, photosVersion) { avatarManager.getPersonPhotoPath(person.id) }
        val contactBitmap = remember(contactPhoto) {
            contactPhoto?.let { path ->
                try {
                    val f = File(path)
                    if (f.exists()) BitmapFactory.decodeFile(path) else null
                } catch (_: Exception) { null }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Avatar + Name & Relationship Role
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (contactBitmap != null) {
                    Image(
                        bitmap = contactBitmap.asImageBitmap(),
                        contentDescription = person.fullName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                } else {
                    DefaultAvatar(
                        name = person.fullName,
                        size = 52.dp
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = person.fullName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = person.relationRole.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (person.isNextOfKin) {
                            Icon(
                                imageVector = PersonaIcons.Success,
                                contentDescription = "Trusted contact",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Lower Info Row: Display Birthday & Notes OR Add Birthday/Notes Chips
            val hasDob = person.dateOfBirth.isNotBlank()
            val hasNotes = person.notes.isNotBlank()

            if (hasDob || hasNotes) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 2.dp)
                ) {
                    if (hasDob) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = PersonaIcons.Calendar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = person.dateOfBirth,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (hasNotes) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = PersonaIcons.Comment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = person.notes.replace('\n', ' ').take(50),
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.tactilePress(targetScale = 0.94f) { onClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = PersonaIcons.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Add birthday",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.tactilePress(targetScale = 0.94f) { onClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = PersonaIcons.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Add notes",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}


// =========================================================================
// PERSON DETAIL SHEET (Reciprocal kinship, private memory notes, dates)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailSheet(
    person: KinRelationshipItem,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onDeletePerson: (KinRelationshipItem) -> Unit,
    onEditPerson: (KinRelationshipItem) -> Unit = {},
    relationshipNotes: List<RelationshipNote> = emptyList(),
    isVaultUnlocked: Boolean = false,
    onAddNote: (topic: String?, content: String, format: NoteFormat, isPrivate: Boolean) -> Unit = { _, _, _, _ -> },
    onEditNote: (RelationshipNote) -> Unit = {},
    onDeleteNote: (RelationshipNote) -> Unit = {},
    onToggleNotePrivacy: (noteId: String, makePrivate: Boolean) -> Unit = { _, _ -> },
    onUnlockVaultSession: () -> Unit = {}
) {
    val context = LocalContext.current
    val avatarManager = remember { PersonaAvatarManager.getInstance(context) }
    var contactPhotoPath by remember(person.id) {
        mutableStateOf(avatarManager.getPersonPhotoPath(person.id))
    }
    var showAvatarActionChooser by remember { mutableStateOf(false) }

    val contactPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val photosDir = File(context.filesDir, "contact_photos").apply { mkdirs() }
                val destFile = File(photosDir, "contact_${person.id}_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                avatarManager.setPersonPhotoPath(person.id, destFile.absolutePath)
                contactPhotoPath = destFile.absolutePath
            } catch (_: Exception) {}
        }
    }

    var showNoteDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<RelationshipNote?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Back/Close arrow (left) + Options (right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF20201E)
                    )
                }

                IconButton(onClick = { onEditPerson(person) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Options",
                        tint = Color(0xFF20201E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Person visual anchor with interactive photo
            Box(contentAlignment = Alignment.BottomEnd) {
                val hasPhoto = !contactPhotoPath.isNullOrBlank() && File(contactPhotoPath!!).exists()
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
                            .size(92.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color(0xFFD5D1C8), CircleShape)
                            .clickable { showAvatarActionChooser = true }
                    )
                } else {
                    DefaultAvatar(
                        name = person.fullName,
                        size = 92.dp,
                        onClick = { showAvatarActionChooser = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Full Name
            Text(
                text = person.fullName,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = Color(0xFF20201E)
            )

            // Relationship Role
            Text(
                text = person.relationRole.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = Color(0xFF6F6B63)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Trusted Contact Chip (matching Screenshot_20260929-135310)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF0EBE1),
                border = BorderStroke(1.dp, Color(0xFFE5DFD4)),
                modifier = Modifier.clickable {
                    // Toggle trusted status
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = PersonaIcons.Success,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Trusted contact",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // CONTACT CARDS (Phone + Email)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (person.phone.isNotBlank()) {
                    DetailActionRow(
                        icon = PersonaIcons.Phone,
                        title = person.phone,
                        subtitle = "Tap to call",
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${person.phone}"))
                            context.startActivity(intent)
                        }
                    )
                }

                if (person.email.isNotBlank()) {
                    DetailActionRow(
                        icon = PersonaIcons.Email,
                        title = person.email,
                        subtitle = "Tap to email",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${person.email}"))
                            context.startActivity(intent)
                        }
                    )
                }

                if (person.dateOfBirth.isNotBlank()) {
                    DetailActionRow(
                        icon = PersonaIcons.Calendar,
                        title = person.dateOfBirth,
                        subtitle = "Date of birth (tap to copy)",
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Date of Birth", person.dateOfBirth)
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "Copied date of birth to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                if (person.phone.isBlank() && person.email.isBlank()) {
                    DetailActionRow(
                        icon = PersonaIcons.Add,
                        title = "Add contact info",
                        subtitle = "Phone or email address",
                        onClick = {
                            onDismissRequest()
                            onEditPerson(person)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // NOTES SECTION
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.clickable {
                            noteToEdit = null
                            showNoteDialog = true
                        }
                    ) {
                        Text(
                            text = "+ Add note",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Dynamic/User notes
                if (relationshipNotes.isNotEmpty()) {
                    relationshipNotes.forEach { note ->
                        NoteCardItem(
                            title = note.topic?.ifBlank { "Note" } ?: "Note",
                            content = note.content,
                            onClick = {
                                noteToEdit = note
                                showNoteDialog = true
                            }
                        )
                    }
                } else if (person.notes.isNotBlank()) {
                    NoteCardItem(
                        title = "Personal",
                        content = person.notes,
                        onClick = {
                            noteToEdit = null
                            showNoteDialog = true
                        }
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                noteToEdit = null
                                showNoteDialog = true
                            }
                    ) {
                        Text(
                            text = "No encrypted notes yet · Tap + Add note to save reminders",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // "Edit profile" Button
            Button(
                onClick = {
                    onDismissRequest()
                    onEditPerson(person)
                },
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .tactilePress()
            ) {
                Text(
                    text = "Edit profile",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // "Remove relationship" Button
            TextButton(
                onClick = {
                    onDismissRequest()
                    onDeletePerson(person)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = PersonaIcons.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Remove relationship",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp
                        ),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showNoteDialog) {
        AddOrEditRelationshipNoteDialog(
            initialNote = noteToEdit,
            onDismiss = {
                showNoteDialog = false
                noteToEdit = null
            },
            onSave = { topic, content, format, isPrivate ->
                onAddNote(topic, content, format, isPrivate)
                showNoteDialog = false
                noteToEdit = null
            }
        )
    }

    if (showAvatarActionChooser) {
        AlertDialog(
            onDismissRequest = { showAvatarActionChooser = false },
            title = { Text("Contact Visual Identity", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Choose how ${person.fullName} appears across the app:", style = MaterialTheme.typography.bodySmall)

                    OutlinedButton(
                        onClick = {
                            showAvatarActionChooser = false
                            contactPhotoPicker.launch("image/*")
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose Photo from Gallery")
                    }

                    if (!contactPhotoPath.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                avatarManager.setPersonPhotoPath(person.id, null)
                                contactPhotoPath = null
                                showAvatarActionChooser = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Remove Custom Photo (Use Default Icon)", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAvatarActionChooser = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun DetailActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .tactilePress(targetScale = 0.98f) { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NoteCardItem(
    title: String,
    content: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .tactilePress(targetScale = 0.98f) { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

