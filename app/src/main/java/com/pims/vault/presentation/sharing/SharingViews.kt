package com.pims.vault.presentation.sharing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pims.vault.domain.model.DecryptedShareResult
import com.pims.vault.domain.model.ShareDuration
import com.pims.vault.presentation.ui.components.PimsButton
import com.pims.vault.presentation.ui.components.PimsCard
import com.pims.vault.presentation.ui.theme.PimsBackground
import com.pims.vault.presentation.ui.theme.PimsBorder
import com.pims.vault.presentation.ui.theme.PimsError
import com.pims.vault.presentation.ui.theme.PimsSurface
import com.pims.vault.presentation.ui.theme.PimsTextPrimary
import com.pims.vault.presentation.ui.theme.PimsTextSecondary
import com.pims.vault.presentation.ui.theme.PimsWarning
import com.pims.vault.presentation.ui.theme.StateSuccess

@Composable
fun SelectiveSharingDashboardView(
    viewModel: SharingViewModel,
    profileFields: Map<String, String>,
    medicalFields: Map<String, String>,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PimsBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = PimsTextPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SELECTIVE SHARING",
                            color = PimsTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                    Text(
                        text = "EPHEMERAL ENCRYPTED QR EXCHANGE",
                        color = PimsTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Scan button
                IconButton(
                    onClick = {
                        // Demo scan simulation: generate test QR envelope and scan it
                        viewModel.scanAndDecryptQr("PIMS1;testEpk;${System.currentTimeMillis() + 300000};0102030405060708090a0b0c0d0e0f10;dGVzdENpcGhlcnRleHQ=;senderFingerprint;testSig")
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, PimsBorder, RoundedCornerShape(6.dp))
                        .background(PimsSurface)
                ) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scan QR", tint = PimsTextPrimary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Duration selector
            Text(text = "EXPIRATION DURATION", color = PimsTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShareDuration.values().forEach { dur ->
                    val isSelected = uiState.policy.duration == dur
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, if (isSelected) PimsTextPrimary else PimsBorder, RoundedCornerShape(6.dp))
                            .background(if (isSelected) PimsSurface else Color.Transparent)
                            .clickable { viewModel.setDuration(dur) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = dur.label,
                            color = if (isSelected) PimsTextPrimary else PimsTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error banner
            uiState.errorMessage?.let { err ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, PimsError, RoundedCornerShape(6.dp))
                        .background(PimsSurface)
                        .padding(10.dp)
                ) {
                    Text(text = err, color = PimsError, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Field Selection Tree
            Text(text = "SELECT FIELDS TO DISCLOSE", color = PimsTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            SharingCategorySection(
                icon = Icons.Default.Person,
                title = "Personal & Profile",
                items = listOf(
                    SharingItemState("Full Legal Name", uiState.fieldSelection.includeFullName, "fullName"),
                    SharingItemState("Preferred Name", uiState.fieldSelection.includePreferredName, "preferredName"),
                    SharingItemState("Date of Birth", uiState.fieldSelection.includeDob, "dob"),
                    SharingItemState("Nationality", uiState.fieldSelection.includeNationality, "nationality")
                ),
                onToggle = { viewModel.toggleField(it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SharingCategorySection(
                icon = Icons.Default.ContactPhone,
                title = "Contact Information",
                items = listOf(
                    SharingItemState("Primary Mobile Phone", uiState.fieldSelection.includePrimaryPhone, "primaryPhone"),
                    SharingItemState("Secondary / WhatsApp", uiState.fieldSelection.includeSecondaryPhone, "secondaryPhone"),
                    SharingItemState("Personal Email Address", uiState.fieldSelection.includePrimaryEmail, "primaryEmail"),
                    SharingItemState("Residential Address", uiState.fieldSelection.includeResidentialAddress, "residentialAddress")
                ),
                onToggle = { viewModel.toggleField(it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SharingCategorySection(
                icon = Icons.Default.MedicalServices,
                title = "Medical & Emergency",
                items = listOf(
                    SharingItemState("Blood Group", uiState.fieldSelection.includeBloodGroup, "bloodGroup"),
                    SharingItemState("Critical Allergies", uiState.fieldSelection.includeAllergies, "allergies"),
                    SharingItemState("Emergency ICE Contact", uiState.fieldSelection.includeEmergencyContact, "emergencyContact")
                ),
                onToggle = { viewModel.toggleField(it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SharingCategorySection(
                icon = Icons.Default.Work,
                title = "Professional & Education",
                items = listOf(
                    SharingItemState("Occupation", uiState.fieldSelection.includeOccupation, "occupation"),
                    SharingItemState("Education / Qualification", uiState.fieldSelection.includeEducation, "education"),
                    SharingItemState("Employer / Role", uiState.fieldSelection.includeEmployment, "employment")
                ),
                onToggle = { viewModel.toggleField(it) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            PimsButton(
                text = "GENERATE EPHEMERAL QR PACKAGE",
                onClick = { viewModel.generateSharePackage(profileFields, medicalFields) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Modal QR Code Display Dialog
    if (uiState.activeQrEnvelope != null) {
        Dialog(onDismissRequest = { viewModel.dismissQr() }) {
            PimsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ENCRYPTED QR PACKAGE",
                            color = PimsTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { viewModel.dismissQr() }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = PimsTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated High-Contrast AMOLED QR Box
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, PimsTextPrimary, RoundedCornerShape(8.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = Color.Black, modifier = Modifier.size(140.dp))
                            Text(text = "SCAN WITH PERSONA", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Expiry Ring & Counter
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, PimsBorder, RoundedCornerShape(6.dp))
                            .background(PimsSurface)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = PimsWarning, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        val m = uiState.remainingCountdownSeconds / 60
                        val s = uiState.remainingCountdownSeconds % 60
                        Text(
                            text = "EXPIRES IN %02d:%02d".format(m, s),
                            color = PimsTextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Encrypted with Ephemeral X25519 + ChaCha20-Poly1305. Self-destructs upon timer expiration.",
                        color = PimsTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    PimsButton(text = "DONE", onClick = { viewModel.dismissQr() }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    // Modal Scanned Result Dialog
    uiState.scannedResult?.let { result ->
        Dialog(onDismissRequest = { viewModel.dismissScannedResult() }) {
            PimsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = StateSuccess, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "VERIFIED DISCLOSURE", color = StateSuccess, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { viewModel.dismissScannedResult() }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = PimsTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Sender: ${result.senderFingerprint}", color = PimsTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "DISCLOSED PROFILE FIELDS", color = PimsTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    result.profileFields.forEach { (k, v) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = k, color = PimsTextSecondary, fontSize = 12.sp)
                            Text(text = v, color = PimsTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (result.medicalFields.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "DISCLOSED MEDICAL EMERGENCY FIELDS", color = PimsTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        result.medicalFields.forEach { (k, v) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = k, color = PimsTextSecondary, fontSize = 12.sp)
                                Text(text = v, color = PimsTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    PimsButton(text = "DISMISS", onClick = { viewModel.dismissScannedResult() }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

data class SharingItemState(val label: String, val isChecked: Boolean, val fieldKey: String)

@Composable
private fun SharingCategorySection(
    icon: ImageVector,
    title: String,
    items: List<SharingItemState>,
    onToggle: (String) -> Unit
) {
    PimsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = PimsTextPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title.uppercase(), color = PimsTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))

            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onToggle(item.fieldKey) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = item.label, color = if (item.isChecked) PimsTextPrimary else PimsTextSecondary, fontSize = 13.sp)
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, if (item.isChecked) PimsTextPrimary else PimsBorder, RoundedCornerShape(4.dp))
                            .background(if (item.isChecked) PimsSurface else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.isChecked) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = PimsTextPrimary, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
    }
}
