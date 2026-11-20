package com.pims.vault.presentation.hub

import com.pims.vault.presentation.ui.theme.PersonaIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.domain.model.PublicResumeData
import com.pims.vault.presentation.ui.components.PersonaAction
import com.pims.vault.presentation.ui.components.PersonaActionStyle
import com.pims.vault.presentation.ui.components.PersonaSheet
import com.pims.vault.presentation.ui.components.PersonaSheetHeader
import com.pims.vault.presentation.ui.theme.PimsSpacing
import com.pims.vault.presentation.ui.theme.pimsPageGutter
import com.pims.vault.presentation.ui.util.rememberPimsHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicResumeModal(
    data: PublicResumeData,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismissRequest: () -> Unit,
    onExportPdf: () -> Unit,
    onEditProfile: () -> Unit = {}
) {
    val haptics = rememberPimsHaptics()
    val clipboardManager = LocalClipboardManager.current
    val gutter = pimsPageGutter()

    PersonaSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        showDragHandle = false,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Modal Bar with proper status bar safe-area padding
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                PersonaSheetHeader(
                    "Public résumé",
                    onDismissRequest = {
                        haptics.light()
                        onDismissRequest()
                    },
                    modifier = Modifier.padding(horizontal = gutter, vertical = PimsSpacing.xs)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Body: Unified Resume Document View (Scrollable)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = gutter, vertical = PimsSpacing.m),
                verticalArrangement = Arrangement.spacedBy(PimsSpacing.m)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(PimsSpacing.m)
                ) {
                        // 1. RESUME HEADER
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = data.fullName.ifBlank { "Personal Dossier" },
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 26.sp,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (data.headline.isNotBlank()) {
                                Text(
                                    text = data.headline,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Structured Contact Info (individual rows with icons, no crammed single line)
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                if (!data.primaryEmail.isNullOrBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = PersonaIcons.Email,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = data.primaryEmail,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                if (!data.primaryPhone.isNullOrBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = PersonaIcons.PhoneOutlined,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = data.primaryPhone,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                if (data.location.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = PersonaIcons.Home,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = data.location,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                        // Segment content: Direct listing without 'Professional Summary' subtitle/header
                        if (!data.bioOrSummary.isNullOrBlank()) {
                            Text(
                                text = data.bioOrSummary,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 3. EXPERIENCE
                        if (data.hasCareerHistory) {
                            ResumeSection(title = "Experience", icon = PersonaIcons.Work) {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    data.employments.forEach { emp ->
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = emp.position,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                val dates = listOfNotNull(
                                                    emp.startDate?.takeIf { it.isNotBlank() },
                                                    if (emp.isCurrent) "Present" else emp.endDate?.takeIf { it.isNotBlank() }
                                                ).joinToString(" – ")
                                                if (dates.isNotBlank()) {
                                                    Text(
                                                        text = dates,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }

                                            val companyLine = listOfNotNull(
                                                emp.company.takeIf { it.isNotBlank() },
                                                emp.location?.takeIf { it.isNotBlank() },
                                                emp.department?.takeIf { it.isNotBlank() }
                                            ).joinToString(" | ")
                                            if (companyLine.isNotBlank()) {
                                                Text(
                                                    text = companyLine,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            if (!emp.responsibilities.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                emp.responsibilities.split("\n", ";").filter { it.isNotBlank() }.forEach { bullet ->
                                                    Row(
                                                        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                        Text(
                                                            text = bullet.trim().removePrefix("•").trim(),
                                                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. EDUCATION & QUALIFICATIONS
                        if (data.educations.isNotEmpty()) {
                            ResumeSection(title = "Education", icon = PersonaIcons.Education) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    data.educations.forEach { edu ->
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = edu.qualification,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                val dates = listOfNotNull(edu.startDate, edu.endDate).joinToString(" – ")
                                                if (dates.isNotBlank()) {
                                                    Text(dates, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                }
                                            }

                                            val instLine = listOfNotNull(edu.institution, edu.fieldOfStudy, edu.country).joinToString(" | ")
                                            Text(instLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                            if (!edu.grade.isNullOrBlank()) {
                                                Text("Honors / Grade: ${edu.grade}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 5. CERTIFICATIONS
                        if (data.certificates.isNotEmpty()) {
                            ResumeSection(title = "Certifications", icon = PersonaIcons.Verified) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    data.certificates.forEach { cert ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(cert.qualification, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                                if (cert.institution.isNotBlank()) {
                                                    Text(cert.institution, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            if (!cert.endDate.isNullOrBlank()) {
                                                Text(cert.endDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 6. SKILLS & ATTRIBUTES
                        if (data.hasAttributes) {
                            ResumeSection(title = "Key Skills & Attributes") {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    data.customFields.forEach { field ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(field.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
                                            Text(field.value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }

                        // 7. ONLINE PRESENCE & LINKS
                        if (data.hasSocialLinks) {
                            ResumeSection(title = "Online & Social Presence", icon = PersonaIcons.Link) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    data.socialAccounts.forEach { acc ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = PersonaIcons.Link,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            val name = acc.customPlatformName ?: acc.platform.displayName
                                            Text(name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                            Text(acc.handleOrUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Sticky Bottom Action Bar (pinned above navigation bars)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = gutter, vertical = PimsSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(PimsSpacing.sm)
                ) {
                    PersonaAction(
                        label = "Edit information",
                        onClick = {
                            haptics.light()
                            onEditProfile()
                        },
                        modifier = Modifier.weight(1f),
                        style = PersonaActionStyle.Quiet,
                        icon = PersonaIcons.Edit
                    )

                    PersonaAction(
                        label = "Export to PDF",
                        onClick = {
                            haptics.light()
                            onExportPdf()
                        },
                        modifier = Modifier.weight(1f),
                        icon = PersonaIcons.FileDownload
                    )
                }
            }
        }
    }
}

@Composable
private fun ResumeSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), thickness = 1.dp)
        content()
    }
}
