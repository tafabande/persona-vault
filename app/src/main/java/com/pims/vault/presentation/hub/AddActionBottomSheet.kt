package com.pims.vault.presentation.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AddActionType {
    PHONE, EMAIL, ADDRESS,
    EDUCATION, CERTIFICATION, EXPERIENCE,
    DOCUMENT, HEALTH_RECORD,
    PASSWORD,
    PERSON
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActionBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onSelectAction: (AddActionType) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Add something",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose what facet of your life to update",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // PERSONAL
            AddSectionHeader("Personal")
            AddActionRow(
                icon = Icons.Default.Phone,
                title = "Phone number",
                subtitle = "Personal, work, or mobile contact",
                onClick = { onSelectAction(AddActionType.PHONE) }
            )
            AddActionRow(
                icon = Icons.Default.Email,
                title = "Email address",
                subtitle = "Reachability & contact point",
                onClick = { onSelectAction(AddActionType.EMAIL) }
            )
            AddActionRow(
                icon = Icons.Default.Home,
                title = "Physical address",
                subtitle = "Residential or postal location",
                onClick = { onSelectAction(AddActionType.ADDRESS) }
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(14.dp))

            // PROFESSIONAL
            AddSectionHeader("Professional")
            AddActionRow(
                icon = Icons.Default.School,
                title = "Education & Degree",
                subtitle = "University, college, or school record",
                onClick = { onSelectAction(AddActionType.EDUCATION) }
            )
            AddActionRow(
                icon = Icons.Default.Star,
                title = "Certification",
                subtitle = "Professional credentials, licenses & certs",
                onClick = { onSelectAction(AddActionType.CERTIFICATION) }
            )
            AddActionRow(
                icon = Icons.Default.Work,
                title = "Work experience & projects",
                subtitle = "Roles, engineering projects, achievements",
                onClick = { onSelectAction(AddActionType.EXPERIENCE) }
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(14.dp))

            // LIFE & SECURITY
            AddSectionHeader("Life & Security")
            AddActionRow(
                icon = Icons.Default.Description,
                title = "Document",
                subtitle = "Attach PDF/image to your identity or education",
                onClick = { onSelectAction(AddActionType.DOCUMENT) }
            )
            AddActionRow(
                icon = Icons.Default.MedicalServices,
                title = "Health record",
                subtitle = "Allergy, condition, medication or blood type",
                onClick = { onSelectAction(AddActionType.HEALTH_RECORD) }
            )
            AddActionRow(
                icon = Icons.Default.Key,
                title = "Password or secret",
                subtitle = "Store in your encrypted offline fortress",
                onClick = { onSelectAction(AddActionType.PASSWORD) }
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(14.dp))

            // PEOPLE
            AddSectionHeader("People & Relationships")
            AddActionRow(
                icon = Icons.Default.PersonAdd,
                title = "Connect person",
                subtitle = "Add parent, sibling, partner, doctor, or friend",
                onClick = { onSelectAction(AddActionType.PERSON) }
            )
        }
    }
}

@Composable
private fun AddSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 0.8.sp,
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
private fun AddActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

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
