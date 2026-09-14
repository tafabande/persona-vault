package com.pims.vault.presentation.hub

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.components.PersonaAvatar
import com.pims.vault.presentation.ui.components.PersonaShareCard
import com.pims.vault.presentation.ui.theme.StateError
import com.pims.vault.presentation.ui.theme.StateSuccess
import com.pims.vault.presentation.ui.theme.StateWarning

enum class SharePreset {
    GENERAL,
    MEDICAL,
    PROFESSIONAL,
    CONTACT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePreviewModal(
    personName: String,
    occupation: String,
    country: String,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onCopyShareLink: (String) -> Unit
) {
    var selectedPresetIndex by remember { mutableIntStateOf(0) }
    val presets = listOf("General", "Medical ICE", "Professional", "Contact Card")

    // State for granular disclosure permissions
    var shareName by remember { mutableStateOf(true) }
    var sharePhoto by remember { mutableStateOf(true) }
    var shareProfession by remember { mutableStateOf(true) }
    var sharePhone by remember { mutableStateOf(true) }
    var shareEmail by remember { mutableStateOf(true) }
    var shareAddress by remember { mutableStateOf(false) }
    var shareEducation by remember { mutableStateOf(false) }
    var sharePortfolio by remember { mutableStateOf(false) }
    var shareEmergencyIce by remember { mutableStateOf(false) }

    // When preset tab changes, configure defaults adhering to minimal disclosure principle
    fun updatePreset(index: Int) {
        selectedPresetIndex = index
        when (index) {
            0 -> { // General Card
                shareName = true
                sharePhoto = true
                shareProfession = true
                sharePhone = true
                shareEmail = true
                shareAddress = false
                shareEducation = false
                sharePortfolio = false
                shareEmergencyIce = false
            }
            1 -> { // Medical ICE Card
                shareName = true
                sharePhoto = true
                shareProfession = false
                sharePhone = true
                shareEmail = false
                shareAddress = false
                shareEducation = false
                sharePortfolio = false
                shareEmergencyIce = true
            }
            2 -> { // Professional Card
                shareName = true
                sharePhoto = true
                shareProfession = true
                sharePhone = false
                shareEmail = true
                shareAddress = false
                shareEducation = true
                sharePortfolio = true
                shareEmergencyIce = false
            }
            3 -> { // Contact Card
                shareName = true
                sharePhoto = true
                shareProfession = false
                sharePhone = true
                shareEmail = true
                shareAddress = true
                shareEducation = false
                sharePortfolio = false
                shareEmergencyIce = false
            }
        }
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Digital Person Card",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets TabRow
            TabRow(
                selectedTabIndex = selectedPresetIndex,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedPresetIndex]),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            ) {
                presets.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedPresetIndex == index,
                        onClick = { updatePreset(index) },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedPresetIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedPresetIndex == index) MaterialTheme.colorScheme.tertiary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Photographic Instant-Film Share Card with quiet-zone QR
            PersonaShareCard(
                personName = personName,
                occupation = when (selectedPresetIndex) {
                    1 -> "Emergency Responder Info"
                    3 -> "Direct Contact Card"
                    else -> occupation.ifBlank { "Personal Profile" }
                },
                country = country,
                presetTitle = presets[selectedPresetIndex],
                qrSeed = "$personName;${presets[selectedPresetIndex]};name=$shareName;phone=$sharePhone;email=$shareEmail;addr=$shareAddress;med=$shareEmergencyIce;edu=$shareEducation",
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PREVIEW-FIRST PERMISSIONS BREAKDOWN
            Text(
                text = "DISCLOSURE PREVIEW",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // You're sharing
            Text(
                text = "Shared via this QR",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            PermissionToggleRow("Name & Display Photo", shareName) { shareName = it }
            if (selectedPresetIndex != 2) {
                PermissionToggleRow("Occupation & Profession", shareProfession) { shareProfession = it }
            }
            if (selectedPresetIndex == 0) {
                PermissionToggleRow("Phone Number", sharePhone) { sharePhone = it }
                PermissionToggleRow("Email Address", shareEmail) { shareEmail = it }
                PermissionToggleRow("Residential Address", shareAddress) { shareAddress = it }
            }
            if (selectedPresetIndex == 1) {
                PermissionToggleRow("Education & Qualifications", shareEducation) { shareEducation = it }
                PermissionToggleRow("Projects & Portfolio links", sharePortfolio) { sharePortfolio = it }
                PermissionToggleRow("Work Email", shareEmail) { shareEmail = it }
            }
            if (selectedPresetIndex == 2) {
                PermissionToggleRow("Blood type & critical allergies", shareEmergencyIce) { shareEmergencyIce = it }
                PermissionToggleRow("ICE Emergency phone", sharePhone) { sharePhone = it }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Never shared / Locked
            Text(
                text = "Protected & Never Shared",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            LockedInfoRow("Account Passwords & Vault Credentials")
            LockedInfoRow("National Identity & Passport Documents")
            if (selectedPresetIndex != 2) {
                LockedInfoRow("Confidential Medical Records & History")
            }
            if (selectedPresetIndex == 1) {
                LockedInfoRow("Personal Residential Address")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Button(
                onClick = {
                    val shareUrl = "https://persona.vault/p/${personName.lowercase().replace(" ", "")}?p=${presets[selectedPresetIndex].lowercase()}"
                    onCopyShareLink(shareUrl)
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Share ${presets[selectedPresetIndex]} Profile",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = StateSuccess,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.tertiary,
                checkmarkColor = MaterialTheme.colorScheme.onTertiary
            )
        )
    }
}

@Composable
private fun LockedInfoRow(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CleanQrCodeVisualizer(
    seed: String,
    modifier: Modifier = Modifier
) {
    val darkColor = MaterialTheme.colorScheme.primary
    val lightColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .background(color = lightColor, shape = RoundedCornerShape(12.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val cols = 17
            val cellSize = size.width / cols
            val hash = seed.hashCode()

            for (r in 0 until cols) {
                for (c in 0 until cols) {
                    val isCorner = (r < 4 && c < 4) || (r < 4 && c >= cols - 4) || (r >= cols - 4 && c < 4)
                    val isCenter = (r in 7..9 && c in 7..9)
                    val filled = if (isCorner) {
                        (r == 0 || r == 3 || c == 0 || c == 3) || (r in 1..2 && c in 1..2)
                    } else if (isCenter) {
                        false
                    } else {
                        ((hash + (r * 31 + c * 17)) % 3 == 0)
                    }

                    if (filled) {
                        drawRect(
                            color = darkColor,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize * 0.9f, cellSize * 0.9f)
                        )
                    }
                }
            }
        }

        // Quiet center person mark
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(color = MaterialTheme.colorScheme.tertiary, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "P",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
