package com.pims.vault.presentation.hub

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.pims.vault.presentation.DateHelper
import com.pims.vault.presentation.profile.KinRelationshipItem
import com.pims.vault.presentation.ui.components.PersonaAvatar
import com.pims.vault.presentation.ui.state.PersonaEmptyState
import com.pims.vault.presentation.ui.theme.StateError

@Composable
fun PeopleView(
    relationships: List<KinRelationshipItem>,
    onAddPersonClick: () -> Unit,
    onSelectPerson: (KinRelationshipItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    var showLinkAccountDialog by remember { mutableStateOf(false) }
    var linkCodeInput by remember { mutableStateOf("") }
    var linkSuccessMessage by remember { mutableStateOf<String?>(null) }

    val filteredList = if (searchQuery.isBlank()) {
        relationships
    } else {
        relationships.filter {
            it.fullName.contains(searchQuery, ignoreCase = true) ||
            it.relationRole.contains(searchQuery, ignoreCase = true)
        }
    }

    val familyRoles = setOf("MOTHER", "FATHER", "PARENT", "SIBLING", "BROTHER", "SISTER", "CHILD", "SON", "DAUGHTER", "PARTNER", "SPOUSE", "WIFE", "HUSBAND", "RELATIVE")
    val familyMembers = filteredList.filter { it.relationRole.uppercase() in familyRoles }
    val otherConnections = filteredList.filter { it.relationRole.uppercase() !in familyRoles }

    val haptics = com.pims.vault.presentation.ui.util.rememberPimsHaptics()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                placeholder = { Text("Search...") },
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

        // Empty state
        if (relationships.isEmpty()) {
            item {
                PersonaEmptyState(
                    icon = Icons.Default.Person,
                    title = "No people yet",
                    description = "People you add will appear here.",
                    actionLabel = "Add person",
                    onActionClick = {
                        haptics.light()
                        onAddPersonClick()
                    }
                )
            }
        }

        // FAMILY GROUP
        if (familyMembers.isNotEmpty()) {
            item {
                Text(
                    text = "FAMILY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(familyMembers, key = { it.id }) { person ->
                PersonListItem(
                    person = person,
                    onClick = { onSelectPerson(person) }
                )
            }
        }

        // OTHER CONNECTIONS GROUP
        if (otherConnections.isNotEmpty()) {
            item {
                Text(
                    text = "CONNECTIONS & FRIENDS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(otherConnections, key = { it.id }) { person ->
                PersonListItem(
                    person = person,
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
                    }
                ) {
                    Text("Send Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkAccountDialog = false }) {
                    Text("Cancel")
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
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PersonaAvatar(
                name = person.fullName,
                config = com.pims.vault.presentation.avatar.PersonaAvatarConfig.fromRelationship(person.fullName, person.relationRole),
                size = 42.dp,
                avatarTextSize = 16.sp
            )

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
    onEditPerson: (KinRelationshipItem) -> Unit = {}
) {
    val context = LocalContext.current

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

            // Person visual anchor
            PersonaAvatar(
                name = person.fullName,
                config = com.pims.vault.presentation.avatar.PersonaAvatarConfig.fromRelationship(person.fullName, person.relationRole),
                size = 76.dp,
                avatarTextSize = 28.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                "MOTHER", "FATHER", "PARENT" -> "You are their child"
                "CHILD", "SON", "DAUGHTER" -> "You are their parent"
                "SIBLING", "BROTHER", "SISTER" -> "You are their sibling"
                "PARTNER", "SPOUSE", "WIFE", "HUSBAND" -> "You are their partner"
                "DOCTOR" -> "You are their patient"
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

            // CONTACT ACTIONS
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "CONTACT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                    color = MaterialTheme.colorScheme.tertiary
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (person.phone.isNotBlank()) {
                    DetailActionRow(
                        icon = Icons.Default.Phone,
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
                        icon = Icons.Default.Email,
                        title = person.email,
                        subtitle = "Tap to send email",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${person.email}"))
                            context.startActivity(intent)
                        }
                    )
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

            // PRIVATE MEMORY LAYER / NOTES
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "YOUR PRIVATE MEMORY LAYER",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                    color = MaterialTheme.colorScheme.tertiary
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                            text = if (person.notes.isNotBlank()) person.notes else "No private notes yet. E.g. gift ideas, preferences, reminders.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (person.notes.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Private to you • Never shared",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(top = 6.dp)
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
            .clickable(onClick = onClick)
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
