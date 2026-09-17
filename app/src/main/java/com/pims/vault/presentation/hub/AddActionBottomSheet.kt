package com.pims.vault.presentation.hub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.presentation.ui.util.rememberPimsHaptics

/**
 * Granular action types covering Persona's personal information universe.
 */
enum class AddActionType {
    // Identity
    PERSONAL_DETAILS,
    IDENTIFICATION_DOC,
    NAMES_HISTORY,
    EMERGENCY_IDENTITY,

    // Contact
    PHONE,
    EMAIL,
    ADDRESS,
    ONLINE_PRESENCE,
    COMMUNICATION_PREFS,

    // Education
    EDUCATION,
    QUALIFICATION,
    CERTIFICATION,
    ACADEMIC_ACHIEVEMENT,
    COURSES_TRAINING,
    SKILLS_LANGUAGES,

    // Career
    EXPERIENCE,
    PROJECTS,
    PORTFOLIO,
    AWARDS,
    VOLUNTEERING,
    PROFESSIONAL_MEMBERSHIPS,
    REFERENCES,

    // Health
    MEDICAL_HISTORY,
    ALLERGIES,
    MEDICATIONS,
    EMERGENCY_INFO,
    DOCTORS_PROVIDERS,
    HEALTH_DOCUMENTS,

    // Personal
    INTERESTS_HOBBIES,
    PREFERENCES,
    PERSONAL_NOTES,
    GOALS,
    IMPORTANT_DATES,
    FAVOURITE_THINGS,
    MEMBERSHIPS_AFFILIATIONS,

    // Top-Level / Direct
    DOCUMENT,
    CUSTOM_FIELD,
    PERSON,
    PASSWORD,
    PAYMENT_CARD,
    BANK_ACCOUNT,
    SOCIAL_PROFILE
}

private data class ActionTileItem(
    val icon: ImageVector,
    val label: String,
    val action: AddActionType,
    val keywords: String = ""
)

/**
 * AddActionBottomSheet
 *
 * Streamlined 3x3 icon grid with 3 high-level buckets, top 4 Quick Add pill chips,
 * and rapid search filtering with 0.2s visual cognitive recognition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActionBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onSelectAction: (AddActionType) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val haptics = rememberPimsHaptics()

    // 3 High-Level Buckets (3x3 Grid)
    val credentialsMoneyBucket = remember {
        listOf(
            ActionTileItem(Icons.Default.Key, "Password", AddActionType.PASSWORD, "password login credential website pin auth key secret"),
            ActionTileItem(Icons.Default.CreditCard, "Card", AddActionType.PAYMENT_CARD, "card credit debit payment visa mastercard expiry cvv cc"),
            ActionTileItem(Icons.Default.Shield, "Bank", AddActionType.BANK_ACCOUNT, "bank account routing branch iban wire financial money institution")
        )
    }

    val personalIdentityBucket = remember {
        listOf(
            ActionTileItem(Icons.Default.Badge, "Identity", AddActionType.PERSONAL_DETAILS, "identity id personal driver license dl passport national citizenship name details"),
            ActionTileItem(Icons.Default.Phone, "Contact", AddActionType.PHONE, "contact phone mobile email number cell call message"),
            ActionTileItem(Icons.Default.Home, "Address", AddActionType.ADDRESS, "address home street postal residence city location postal zip house")
        )
    }

    val filesVaultBucket = remember {
        listOf(
            ActionTileItem(Icons.Default.Description, "Document", AddActionType.DOCUMENT, "document driver license dl pdf file scan upload diploma cert identification records"),
            ActionTileItem(Icons.Default.MedicalServices, "Medical", AddActionType.ALLERGIES, "medical health medicine allergy condition doctor prescription emergency hospital blood aid"),
            ActionTileItem(Icons.Default.Public, "Social", AddActionType.SOCIAL_PROFILE, "social profile online presence instagram linkedin github twitter web handle website")
        )
    }

    // Extended searchable catalog for full discoverability
    val searchableItems = remember {
        credentialsMoneyBucket + personalIdentityBucket + filesVaultBucket + listOf(
            ActionTileItem(Icons.Default.School, "Education", AddActionType.EDUCATION, "education school university college degree certification diploma course training study"),
            ActionTileItem(Icons.Default.Work, "Career", AddActionType.EXPERIENCE, "career job work company employment resume experience project reference"),
            ActionTileItem(Icons.Default.PersonAdd, "Person", AddActionType.PERSON, "person kin relative emergency contact friend family mother father peer connection"),
            ActionTileItem(Icons.Default.Extension, "Custom", AddActionType.CUSTOM_FIELD, "custom note field arbitrary information extra")
        )
    }

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
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Title & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Information",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Top 4 "Quick Add" Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "QUICK ADD",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickAddPill(
                        icon = Icons.Default.Key,
                        label = "Password",
                        onClick = {
                            haptics.selection()
                            onSelectAction(AddActionType.PASSWORD)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickAddPill(
                        icon = Icons.Default.Phone,
                        label = "Contact",
                        onClick = {
                            haptics.selection()
                            onSelectAction(AddActionType.PHONE)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickAddPill(
                        icon = Icons.Default.CreditCard,
                        label = "Card",
                        onClick = {
                            haptics.selection()
                            onSelectAction(AddActionType.PAYMENT_CARD)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickAddPill(
                        icon = Icons.Default.Description,
                        label = "Document",
                        onClick = {
                            haptics.selection()
                            onSelectAction(AddActionType.DOCUMENT)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Search Bar
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Quick search (e.g. Card, License, Bank)...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 0.8.dp,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            val query = searchQuery.trim().lowercase()

            if (query.isNotEmpty()) {
                // Real-time Search Results in 3-Column Grid
                Text(
                    text = "MATCHING CATEGORIES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val filtered = searchableItems.filter {
                    it.label.lowercase().contains(query) || it.keywords.contains(query)
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No category matching \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    filtered.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { item ->
                                CompactActionTile(
                                    icon = item.icon,
                                    label = item.label,
                                    onClick = {
                                        haptics.selection()
                                        onSelectAction(item.action)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                // 3 High-Level Buckets (3x3 Grid)

                // 1. Credentials & Money
                ActionBucketSection(
                    title = "CREDENTIALS & MONEY",
                    items = credentialsMoneyBucket,
                    onSelectAction = onSelectAction
                )

                // 2. Personal & Identity
                ActionBucketSection(
                    title = "PERSONAL & IDENTITY",
                    items = personalIdentityBucket,
                    onSelectAction = onSelectAction
                )

                // 3. Files & Vault
                ActionBucketSection(
                    title = "FILES & VAULT",
                    items = filesVaultBucket,
                    onSelectAction = onSelectAction
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Compact Components (0.2s Cognitive Recognition)
// ---------------------------------------------------------------------------

@Composable
private fun QuickAddPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier.tactilePress(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ActionBucketSection(
    title: String,
    items: List<ActionTileItem>,
    onSelectAction: (AddActionType) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberPimsHaptics()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.forEach { item ->
                CompactActionTile(
                    icon = item.icon,
                    label = item.label,
                    onClick = {
                        haptics.selection()
                        onSelectAction(item.action)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CompactActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier.tactilePress(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
