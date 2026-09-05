package com.pims.vault.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pims.vault.core.crypto.KeySecurityLevel
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.presentation.ui.components.PimsOutlinedInput
import com.pims.vault.presentation.ui.components.PimsSectionHeader
import com.pims.vault.presentation.ui.components.PimsSkeletonAccordion
import com.pims.vault.presentation.ui.theme.AmoledBackground
import com.pims.vault.presentation.ui.theme.PimsDimensions
import com.pims.vault.presentation.ui.theme.StateSuccess

@Composable
fun PersonaDashboardScreen(
    securityLevel: KeySecurityLevel,
    onLockClicked: () -> Unit
) {
    // Local form state holders for demo/presentation
    var fullName by remember { mutableStateOf("Bleigh Tafadzwa") }
    var preferredName by remember { mutableStateOf("Bleigh") }
    var dob by remember { mutableStateOf("1998-04-12") }
    var nationality by remember { mutableStateOf("Zimbabwean") }
    var occupation by remember { mutableStateOf("Telecommunications Engineer") }

    var primaryPhone by remember { mutableStateOf("+263 77 123 4567") }
    var secondaryPhone by remember { mutableStateOf("+263 71 987 6543") }
    var primaryEmail by remember { mutableStateOf("bleigh@example.com") }

    var residentialStreet by remember { mutableStateOf("14 Enterprise Road, Newlands") }
    var residentialCity by remember { mutableStateOf("Harare") }
    var residentialCountry by remember { mutableStateOf("Zimbabwe") }

    var bloodGroup by remember { mutableStateOf("O+") }
    var allergies by remember { mutableStateOf("Penicillin (Severe)") }
    var emergencyContact by remember { mutableStateOf("Mother (+263 77 555 1234)") }

    var institution by remember { mutableStateOf("Midlands State University") }
    var qualification by remember { mutableStateOf("BSc Telecommunications Engineering") }

    var employer by remember { mutableStateOf("Econet Wireless") }
    var jobTitle by remember { mutableStateOf("Network Operations Engineer") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = PimsDimensions.paddingMedium, vertical = PimsDimensions.paddingMedium)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PERSONA",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your information, secured locally",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onLockClicked,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text("Lock", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Hardware Security Status Bar
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = StateSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Hardware Key Vault: ${securityLevel.levelName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = PimsDimensions.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ==========================================
            // GROUP: PROFILE
            // ==========================================
            item { PimsSectionHeader("Profile") }

            item {
                PimsSkeletonAccordion(
                    title = "Personal Information",
                    icon = Icons.Default.Person,
                    subtitle = "Name, demographics, and citizenship",
                    classification = SecurityClassification.ZONE_1_PERSONAL,
                    initiallyExpanded = true
                ) {
                    PimsOutlinedInput(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = "Full Legal Name",
                        placeholder = "e.g. John Michael Doe"
                    )

                    PimsOutlinedInput(
                        value = preferredName,
                        onValueChange = { preferredName = it },
                        label = "Preferred / Display Name",
                        placeholder = "e.g. John"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PimsOutlinedInput(
                            value = dob,
                            onValueChange = { dob = it },
                            label = "Date of Birth",
                            placeholder = "YYYY-MM-DD",
                            modifier = Modifier.weight(1f)
                        )
                        PimsOutlinedInput(
                            value = nationality,
                            onValueChange = { nationality = it },
                            label = "Nationality",
                            placeholder = "e.g. Zimbabwean",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    PimsOutlinedInput(
                        value = occupation,
                        onValueChange = { occupation = it },
                        label = "Occupation",
                        placeholder = "e.g. Software Engineer"
                    )
                }
            }

            item {
                PimsSkeletonAccordion(
                    title = "Contact Information",
                    icon = Icons.Default.ContactPhone,
                    subtitle = "Phone numbers, emails & communication channels",
                    classification = SecurityClassification.ZONE_1_PERSONAL
                ) {
                    PimsOutlinedInput(
                        value = primaryPhone,
                        onValueChange = { primaryPhone = it },
                        label = "Primary Mobile Phone",
                        placeholder = "e.g. +263 77 123 4567"
                    )

                    PimsOutlinedInput(
                        value = secondaryPhone,
                        onValueChange = { secondaryPhone = it },
                        label = "Secondary Phone / WhatsApp",
                        placeholder = "e.g. +263 71 987 6543"
                    )

                    PimsOutlinedInput(
                        value = primaryEmail,
                        onValueChange = { primaryEmail = it },
                        label = "Personal Email Address",
                        placeholder = "e.g. user@example.com"
                    )
                }
            }

            item {
                PimsSkeletonAccordion(
                    title = "Addresses",
                    icon = Icons.Default.LocationOn,
                    subtitle = "Residential history & postal locations",
                    classification = SecurityClassification.ZONE_2_PRIVATE
                ) {
                    PimsOutlinedInput(
                        value = residentialStreet,
                        onValueChange = { residentialStreet = it },
                        label = "Current Residential Address",
                        placeholder = "e.g. 123 Main Street"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PimsOutlinedInput(
                            value = residentialCity,
                            onValueChange = { residentialCity = it },
                            label = "City",
                            placeholder = "e.g. Harare",
                            modifier = Modifier.weight(1f)
                        )
                        PimsOutlinedInput(
                            value = residentialCountry,
                            onValueChange = { residentialCountry = it },
                            label = "Country",
                            placeholder = "e.g. Zimbabwe",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                PimsSkeletonAccordion(
                    title = "Relationships",
                    icon = Icons.Default.AccountTree,
                    subtitle = "Kinship graph, household & emergency contacts",
                    classification = SecurityClassification.ZONE_2_PRIVATE
                ) {
                    Text(
                        text = "Linked Persons in Graph (3)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "• Mother: Jane Tafadzwa (Verified)\n• Father: Thomas Tafadzwa\n• Spouse: Sarah Tafadzwa",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                PimsSkeletonAccordion(
                    title = "Medical Information",
                    icon = Icons.Default.MedicalServices,
                    subtitle = "Allergies, conditions, blood type & emergency directive",
                    classification = SecurityClassification.ZONE_3_SENSITIVE
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PimsOutlinedInput(
                            value = bloodGroup,
                            onValueChange = { bloodGroup = it },
                            label = "Blood Group",
                            placeholder = "e.g. O+",
                            modifier = Modifier.weight(0.8f)
                        )
                        PimsOutlinedInput(
                            value = allergies,
                            onValueChange = { allergies = it },
                            label = "Critical Allergies",
                            placeholder = "e.g. Penicillin",
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    PimsOutlinedInput(
                        value = emergencyContact,
                        onValueChange = { emergencyContact = it },
                        label = "Emergency ICE Contact",
                        placeholder = "e.g. Next of Kin Phone Number"
                    )
                }
            }

            // ==========================================
            // GROUP: RECORDS
            // ==========================================
            item { PimsSectionHeader("Records") }

            item {
                PimsSkeletonAccordion(
                    title = "Education",
                    icon = Icons.Default.School,
                    subtitle = "Institutions, qualifications & certifications",
                    classification = SecurityClassification.ZONE_2_PRIVATE
                ) {
                    PimsOutlinedInput(
                        value = institution,
                        onValueChange = { institution = it },
                        label = "Institution",
                        placeholder = "e.g. Midlands State University"
                    )
                    PimsOutlinedInput(
                        value = qualification,
                        onValueChange = { qualification = it },
                        label = "Degree / Qualification",
                        placeholder = "e.g. BEng Telecommunications"
                    )
                }
            }

            item {
                PimsSkeletonAccordion(
                    title = "Employment",
                    icon = Icons.Default.Work,
                    subtitle = "Career timeline, companies & positions",
                    classification = SecurityClassification.ZONE_2_PRIVATE
                ) {
                    PimsOutlinedInput(
                        value = employer,
                        onValueChange = { employer = it },
                        label = "Company / Employer",
                        placeholder = "e.g. Econet"
                    )
                    PimsOutlinedInput(
                        value = jobTitle,
                        onValueChange = { jobTitle = it },
                        label = "Position / Role",
                        placeholder = "e.g. Systems Engineer"
                    )
                }
            }

            item {
                PimsSkeletonAccordion(
                    title = "Documents Vault",
                    icon = Icons.Default.Description,
                    subtitle = "Versioned passports, national IDs & certificates",
                    classification = SecurityClassification.ZONE_3_SENSITIVE
                ) {
                    Text(
                        text = "Encrypted Document Objects (3 stored)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "• National ID (v2) — SHA-256 verified ✓\n• Passport Scan (v1) — SHA-256 verified ✓\n• Degree Certificate (v1) — SHA-256 verified ✓",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ==========================================
            // GROUP: SECURITY & SHARING
            // ==========================================
            item { PimsSectionHeader("Security") }

            item {
                PimsSkeletonAccordion(
                    title = "Security Vault",
                    icon = Icons.Default.Lock,
                    subtitle = "Re-auth protected 2FA TOTP & master credentials",
                    classification = SecurityClassification.ZONE_4_CRITICAL
                ) {
                    Text(
                        text = "🔒 Zone 4 Isolated Storage",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Contains 6 credentials, 3 TOTP 2FA authenticator seeds, and 1 recovery phrase. Requires biometric re-authorization.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                PimsSkeletonAccordion(
                    title = "Selective Sharing & QR",
                    icon = Icons.Default.QrCode,
                    subtitle = "Generate time-bound ephemeral encrypted share packages",
                    classification = SecurityClassification.ZONE_0_PUBLIC
                ) {
                    Text(
                        text = "Selective Disclosure Engine",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select fields above to generate an offline, time-limited Curve25519 encrypted QR code or 15-minute relay token.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                PimsSkeletonAccordion(
                    title = "Audit Trail & Tamper Verification",
                    icon = Icons.Default.History,
                    subtitle = "Cryptographically chained event history",
                    classification = SecurityClassification.ZONE_3_SENSITIVE
                ) {
                    Text(
                        text = "Monotonic HMAC Hash Chain: VALID (24 Events Verified)",
                        style = MaterialTheme.typography.titleMedium,
                        color = StateSuccess
                    )
                    Text(
                        text = "Genesis Root Hash: PIMS_GENESIS_ROOT_HASH_V1\nLatest Sequence: #24\nNo gaps, deletions, or reordering detected.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(PimsDimensions.paddingExtraLarge))
            }
        }
    }
}
