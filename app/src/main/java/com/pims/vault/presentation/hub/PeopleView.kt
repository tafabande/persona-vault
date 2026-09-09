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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
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
                Column {
                    Text(
                        text = "People",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Your private circle & relationships",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onAddPersonClick,
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

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search people...") },
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
                    title = "Keep your important people close.",
                    description = "Add family, friends, colleagues and other relationships with private memory notes and birthdays.",
                    actionLabel = "Add person",
                    onActionClick = onAddPersonClick
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
            PersonaAvatar(name = person.fullName, size = 42.dp, avatarTextSize = 16.sp)

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
    onDeletePerson: (KinRelationshipItem) -> Unit
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
            // Close bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Person visual anchor
            PersonaAvatar(name = person.fullName, size = 76.dp, avatarTextSize = 28.sp)

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
            if (person.dateOfBirth.isNotBlank() || person.anniversary.isNotBlank()) {
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

                    if (person.anniversary.isNotBlank()) {
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
