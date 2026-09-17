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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.pims.vault.presentation.ui.theme.tactilePress
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.pims.vault.presentation.avatar.AvatarBehaviorMode
import com.pims.vault.presentation.avatar.ClothingColor
import com.pims.vault.presentation.avatar.HairStyle
import com.pims.vault.presentation.avatar.PersonaAvatar
import com.pims.vault.presentation.avatar.PersonaAvatarConfig
import com.pims.vault.presentation.avatar.PersonaAvatarManager
import com.pims.vault.presentation.avatar.SkinTone
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
    val avatarManager = remember { PersonaAvatarManager(context) }
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
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }

        // Screen Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "People",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Managing other people • Separate from your own profile",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            haptics.light()
                            showLinkAccountDialog = true
                        },
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Link Person Account",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    IconButton(
                        onClick = {
                            haptics.light()
                            onAddPersonClick()
                        },
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Person",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name or relationship...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.tertiary
                ),
                singleLine = true
            )
        }

        // Category Filter Chips
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PeopleCategory.values()) { category ->
                    val badgeCount = when (category) {
                        PeopleCategory.ALL -> relationships.size
                        PeopleCategory.FAMILY -> familyCount
                        PeopleCategory.FRIENDS -> friendsCount
                        PeopleCategory.WORK -> workCount
                        PeopleCategory.MEDICAL -> medicalCount
                    }
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptics.selection()
                            selectedCategory = category
                        },
                        label = {
                            Text("${category.label} ($badgeCount)", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                }
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
                    avatarConfig = remember(person.id, person.fullName, person.relationRole) {
                        avatarManager.getAvatarConfigForRelationship(person.id, person.fullName, person.relationRole)
                    },
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
    avatarConfig: PersonaAvatarConfig,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .tactilePress(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        val context = LocalContext.current
        val avatarManager = remember { PersonaAvatarManager(context) }
        val contactPhoto = remember(person.id) { avatarManager.getPersonPhotoPath(person.id) }
        val contactBitmap = remember(contactPhoto) {
            contactPhoto?.let { path ->
                try {
                    val f = File(path)
                    if (f.exists()) BitmapFactory.decodeFile(path) else null
                } catch (_: Exception) { null }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (contactBitmap != null) {
                Image(
                    bitmap = contactBitmap.asImageBitmap(),
                    contentDescription = person.fullName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                )
            } else {
                PersonaAvatar(
                    name = person.fullName,
                    config = avatarConfig,
                    size = 46.dp,
                    avatarTextSize = 18.sp,
                    behaviorMode = AvatarBehaviorMode.STATIC
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // Name first, visually dominant
                Text(
                    text = person.fullName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Relationship second, subordinate
                Text(
                    text = person.relationRole.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (person.dateOfBirth.isNotBlank()) {
                Icon(
                    imageVector = Icons.Default.Cake,
                    contentDescription = "Has birthday",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
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
    val avatarManager = remember { PersonaAvatarManager(context) }
    var currentAvatarConfig by remember(person.id) {
        mutableStateOf(avatarManager.getAvatarConfigForRelationship(person.id, person.fullName, person.relationRole))
    }
    var contactPhotoPath by remember(person.id) {
        mutableStateOf(avatarManager.getPersonPhotoPath(person.id))
    }
    var showAvatarActionChooser by remember { mutableStateOf(false) }
    var showAvatarCustomizer by remember { mutableStateOf(false) }

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
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Persistent Breadcrumb / Context Anchor
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "People → ${person.fullName}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Person visual anchor with interactive photo / style customizer
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
                            .size(84.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable { showAvatarActionChooser = true }
                    )
                } else {
                    PersonaAvatar(
                        name = person.fullName,
                        config = currentAvatarConfig,
                        size = 84.dp,
                        avatarTextSize = 28.sp,
                        behaviorMode = AvatarBehaviorMode.ALIVE,
                        onClick = { showAvatarActionChooser = true }
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { showAvatarActionChooser = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Edit photo or avatar",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Text(
                text = "Tap to customize photo or avatar",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clickable { showAvatarActionChooser = true }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = person.fullName,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = person.relationRole.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Link status indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (person.isVerified) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(
                        1.dp,
                        if (person.isVerified) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (person.isVerified) Icons.Default.CheckCircle else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (person.isVerified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (person.isVerified) "Linked Account" else "Manual Person",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (person.isVerified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // RECIPROCAL RELATIONSHIP BADGE
            val reciprocalText = when (person.relationRole.uppercase()) {
                "MOTHER", "FATHER", "PARENT", "MOM", "DAD", "PAPA", "MAMA" -> "You are their child"
                "CHILD", "SON", "DAUGHTER" -> "You are their parent"
                "SIBLING", "BROTHER", "SISTER" -> "You are their sibling"
                "PARTNER", "SPOUSE", "WIFE", "HUSBAND" -> "You are their partner"
                "DOCTOR", "PHYSICIAN", "SURGEON", "DENTIST" -> "You are their patient"
                "COLLEAGUE", "COWORKER" -> "Work connection"
                "FRIEND", "BEST FRIEND" -> "Friendship connection"
                else -> "Connected via relationship"
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
            ) {
                Text(
                    text = reciprocalText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(18.dp))

            // CONTACT ACTIONS (Handles multiple phones and emails)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "CONTACT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                    color = MaterialTheme.colorScheme.tertiary
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (person.phone.isNotBlank()) {
                    val phoneList = person.phone.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    phoneList.forEachIndexed { idx, ph ->
                        DetailActionRow(
                            icon = Icons.Default.Phone,
                            title = ph,
                            subtitle = if (phoneList.size > 1) "Phone #${idx + 1} • Tap to call" else "Tap to call",
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$ph"))
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                if (person.email.isNotBlank()) {
                    val emailList = person.email.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    emailList.forEachIndexed { idx, em ->
                        DetailActionRow(
                            icon = Icons.Default.Email,
                            title = em,
                            subtitle = if (emailList.size > 1) "Email #${idx + 1} • Tap to send" else "Tap to send email",
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$em"))
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                if (person.address.isNotBlank()) {
                    DetailActionRow(
                        icon = Icons.Default.Home,
                        title = person.address,
                        subtitle = "Location",
                        onClick = {}
                    )
                }

                if (person.phone.isBlank() && person.email.isBlank() && person.address.isBlank()) {
                    Text(
                        text = "No contact details provided",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(18.dp))

            // IMPORTANT DATES
            val showAnniversary = person.anniversary.isNotBlank() && com.pims.vault.presentation.isRomanticOrMaritalRelationship(person.relationRole)
            if (person.dateOfBirth.isNotBlank() || showAnniversary) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "IMPORTANT DATES",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (person.dateOfBirth.isNotBlank()) {
                        val bdayInfo = DateHelper.formatBirthdayInfo(person.dateOfBirth)
                        DetailActionRow(icon = Icons.Default.Cake, title = bdayInfo, subtitle = "Birthday", onClick = {})
                    }

                    if (showAnniversary) {
                        val annivInfo = DateHelper.formatAnniversaryInfo(person.anniversary)
                        DetailActionRow(icon = Icons.Default.Cake, title = annivInfo, subtitle = "Anniversary", onClick = {})
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(18.dp))
            }

            // GRANULAR PRIVATE MEMORY LAYER / RELATIONSHIP NOTES
            RelationshipNotesSection(
                notes = relationshipNotes,
                isVaultUnlocked = isVaultUnlocked,
                onAddNoteClick = {
                    noteToEdit = null
                    showNoteDialog = true
                },
                onUnlockVaultClick = onUnlockVaultSession,
                onEditNote = { note ->
                    noteToEdit = note
                    showNoteDialog = true
                },
                onDeleteNote = onDeleteNote,
                onTogglePrivacy = onToggleNotePrivacy,
                modifier = Modifier.fillMaxWidth()
            )

            if (person.notes.isNotBlank() && relationshipNotes.isEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = person.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Legacy note • Tap + Add Note to migrate",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Edit Person button
            Button(
                onClick = {
                    onDismissRequest()
                    onEditPerson(person)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Edit ${person.fullName}'s Profile", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Delete connection button
            Button(
                onClick = {
                    onDismissRequest()
                    onDeletePerson(person)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StateError.copy(alpha = 0.1f),
                    contentColor = StateError
                ),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Disconnect Person", fontWeight = FontWeight.SemiBold)
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

    if (showAvatarCustomizer) {
        QuickAvatarCustomizerDialog(
            initialConfig = currentAvatarConfig,
            onDismiss = { showAvatarCustomizer = false },
            onSave = { updated ->
                avatarManager.saveAvatarConfigForRelationship(person.id, updated)
                currentAvatarConfig = updated
                showAvatarCustomizer = false
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

                    OutlinedButton(
                        onClick = {
                            showAvatarActionChooser = false
                            showAvatarCustomizer = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Customize Vector Avatar")
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
                            Text("Remove Custom Photo (Use Avatar)", color = MaterialTheme.colorScheme.error)
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
fun QuickAvatarCustomizerDialog(
    initialConfig: PersonaAvatarConfig,
    onDismiss: () -> Unit,
    onSave: (PersonaAvatarConfig) -> Unit
) {
    var config by remember { mutableStateOf(initialConfig) }
    var hairIndex by remember { mutableIntStateOf(HairStyle.values().indexOf(config.hairStyle).coerceAtLeast(0)) }
    var skinIndex by remember { mutableIntStateOf(SkinTone.values().indexOf(config.skinTone).coerceAtLeast(0)) }
    var colorIndex by remember { mutableIntStateOf(ClothingColor.values().indexOf(config.clothingColor).coerceAtLeast(0)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize Avatar Style") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Live preview
                PersonaAvatar(
                    config = config,
                    size = 90.dp,
                    behaviorMode = AvatarBehaviorMode.STATIC
                )

                // Hair Style
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Hairstyle: ${config.hairStyle.label}", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = {
                            val styles = HairStyle.values()
                            hairIndex = (hairIndex - 1 + styles.size) % styles.size
                            config = config.copy(hairStyle = styles[hairIndex])
                        }) { Text("Prev") }
                        Button(onClick = {
                            val styles = HairStyle.values()
                            hairIndex = (hairIndex + 1) % styles.size
                            config = config.copy(hairStyle = styles[hairIndex])
                        }) { Text("Next") }
                    }
                }

                // Skin Tone
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Skin: ${config.skinTone.label}", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = {
                            val skins = SkinTone.values()
                            skinIndex = (skinIndex - 1 + skins.size) % skins.size
                            config = config.copy(skinTone = skins[skinIndex])
                        }) { Text("Prev") }
                        Button(onClick = {
                            val skins = SkinTone.values()
                            skinIndex = (skinIndex + 1) % skins.size
                            config = config.copy(skinTone = skins[skinIndex])
                        }) { Text("Next") }
                    }
                }

                // Outfit Color
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Outfit: ${config.clothingColor.label}", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = {
                            val colors = ClothingColor.values()
                            colorIndex = (colorIndex - 1 + colors.size) % colors.size
                            config = config.copy(clothingColor = colors[colorIndex])
                        }) { Text("Prev") }
                        Button(onClick = {
                            val colors = ClothingColor.values()
                            colorIndex = (colorIndex + 1) % colors.size
                            config = config.copy(clothingColor = colors[colorIndex])
                        }) { Text("Next") }
                    }
                }

                // Randomize look button
                OutlinedButton(
                    onClick = {
                        val rand = java.util.Random()
                        val styles = HairStyle.values()
                        val skins = SkinTone.values()
                        val colors = ClothingColor.values()
                        config = config.copy(
                            hairStyle = styles[rand.nextInt(styles.size)],
                            skinTone = skins[rand.nextInt(skins.size)],
                            clothingColor = colors[rand.nextInt(colors.size)]
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🎲 Re-roll Style")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(config) }) {
                Text("Save Style")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
            .tactilePress(onClick = onClick)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
