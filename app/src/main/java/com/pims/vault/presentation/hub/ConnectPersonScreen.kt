package com.pims.vault.presentation.hub

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.isRomanticOrMaritalRelationship
import com.pims.vault.presentation.ui.components.PersonaFormSection
import com.pims.vault.presentation.ui.components.PersonaTextInput
import com.pims.vault.presentation.ui.components.PersonaToggleRow
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.presentation.ui.util.rememberPimsFeedback
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnectPersonScreen(
    onBack: () -> Unit,
    onSavePerson: (
        role: String,
        fullName: String,
        gender: String,
        phone: String,
        email: String,
        address: String,
        dob: String,
        anniversary: String,
        notes: String,
        photoPath: String?,
        isNextOfKin: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberPimsHaptics()
    val feedback = rememberPimsFeedback()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    BackHandler(onBack = onBack)

    // Identity state
    var relRole by remember { mutableStateOf("Partner") }
    var customRole by remember { mutableStateOf("") }
    var relName by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("Unspecified") }
    var nameError by remember { mutableStateOf(false) }

    // Contact state
    var primaryPhone by remember { mutableStateOf("") }
    val additionalPhones = remember { mutableStateListOf<String>() }
    var primaryEmail by remember { mutableStateOf("") }
    val additionalEmails = remember { mutableStateListOf<String>() }
    var relAddress by remember { mutableStateOf("") }

    // Dates & Notes state
    var relDob by remember { mutableStateOf("") }
    var relAnniversary by remember { mutableStateOf("") }
    var relNotes by remember { mutableStateOf("") }
    var isNextOfKin by remember { mutableStateOf(false) }

    // Section visibility toggles for adding optional fields
    var showAddressField by remember { mutableStateOf(false) }
    var showEmailField by remember { mutableStateOf(false) }
    var showDobField by remember { mutableStateOf(false) }
    var showAnniversaryField by remember { mutableStateOf(false) }
    var showNotesField by remember { mutableStateOf(false) }

    // Photo state
    var selectedPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedPhotoPath by remember { mutableStateOf<String?>(null) }
    var selectedAvatarPreset by remember { mutableStateOf<Int?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        selectedPhotoBitmap = bmp
                        selectedAvatarPreset = null
                        val photosDir = File(context.filesDir, "contact_photos").apply { mkdirs() }
                        val destFile = File(photosDir, "contact_${System.currentTimeMillis()}.jpg")
                        FileOutputStream(destFile).use { out ->
                            bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
                        }
                        selectedPhotoPath = destFile.absolutePath
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val relationshipChips = remember {
        listOf("Partner", "Mother", "Father", "Child", "Sibling", "Friend", "Colleague", "Other")
    }

    val genderOptions = remember {
        listOf("Female", "Male", "Other", "Unspecified")
    }

    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textHint = MaterialTheme.colorScheme.outline
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    val isRomantic = isRomanticOrMaritalRelationship(
        if (relRole == "Other" && customRole.isNotBlank()) customRole else relRole
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "New Connection",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = textPrimary
                )

                // Placeholder to balance close button
                Spacer(modifier = Modifier.size(36.dp))
            }

            // Scrollable Form Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header Titles
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Add someone",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        ),
                        color = textPrimary
                    )
                    Text(
                        text = "Build their profile with as much or as little detail as you like.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                        color = textSecondary
                    )
                }

                // Avatar Photo Picker & Presets
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                BorderStroke(
                                    2.dp,
                                    if (selectedPhotoBitmap != null || selectedAvatarPreset != null)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outlineVariant
                                ),
                                CircleShape
                            )
                            .clickable {
                                haptics.selection()
                                photoPickerLauncher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedPhotoBitmap != null) {
                            Image(
                                bitmap = selectedPhotoBitmap!!.asImageBitmap(),
                                contentDescription = "Contact photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (selectedAvatarPreset != null) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Selected avatar",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Add a photo",
                                    tint = textSecondary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    text = "Photo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textSecondary
                                )
                            }
                        }
                    }

                    Text(
                        text = if (selectedPhotoBitmap != null) "Tap photo to change" else "Upload a photo or choose an avatar",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = textSecondary
                    )

                    // Avatar presets row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(1, 2, 3, 4).forEach { presetId ->
                            val isPresetSelected = selectedAvatarPreset == presetId && selectedPhotoBitmap == null
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isPresetSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        BorderStroke(
                                            if (isPresetSelected) 2.dp else 1.dp,
                                            if (isPresetSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        CircleShape
                                    )
                                    .clickable {
                                        haptics.selection()
                                        selectedPhotoBitmap = null
                                        selectedPhotoPath = null
                                        selectedAvatarPreset = presetId
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Avatar preset $presetId",
                                    tint = if (isPresetSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        if (selectedPhotoBitmap != null || selectedAvatarPreset != null) {
                            TextButton(
                                onClick = {
                                    selectedPhotoBitmap = null
                                    selectedPhotoPath = null
                                    selectedAvatarPreset = null
                                }
                            ) {
                                Text("Clear", fontSize = 12.sp, color = textSecondary)
                            }
                        }
                    }
                }

                // Section 1: Relationship & Identity
                PersonaFormSection(
                    title = "Relationship & Identity",
                    icon = Icons.Default.Person
                ) {
                    Text(
                        text = "How do you know them?",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        relationshipChips.forEach { role ->
                            val isSelected = relRole.equals(role, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.tactilePress(targetScale = 0.94f) {
                                    haptics.selection()
                                    relRole = role
                                    // Intelligent gender suggestion
                                    if (role in listOf("Mother", "Sister", "Daughter")) {
                                        selectedGender = "Female"
                                    } else if (role in listOf("Father", "Brother", "Son")) {
                                        selectedGender = "Male"
                                    }
                                }
                            ) {
                                Text(
                                    text = role,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 13.5.sp
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    // Custom role text input when "Other" is chosen
                    AnimatedVisibility(
                        visible = relRole.equals("Other", ignoreCase = true),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        PersonaTextInput(
                            value = customRole,
                            onValueChange = { customRole = it },
                            label = "Specify Relationship",
                            placeholder = "e.g. Mentor, Doctor, Cousin, Lawyer"
                        )
                    }

                    // Full Name Field (Required)
                    PersonaTextInput(
                        value = relName,
                        onValueChange = {
                            relName = it
                            if (it.isNotBlank()) nameError = false
                        },
                        label = "Full Name *",
                        placeholder = "e.g. Sarah Jenkins",
                        isError = nameError,
                        errorMessage = if (nameError) "Name is required" else null
                    )

                    // Gender Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Gender",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            genderOptions.forEach { g ->
                                val isSelected = selectedGender.equals(g, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .tactilePress(targetScale = 0.95f) {
                                            haptics.selection()
                                            selectedGender = g
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = g,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Contact Information
                PersonaFormSection(
                    title = "Contact Information",
                    icon = Icons.Default.Phone
                ) {
                    // Primary Phone
                    PersonaTextInput(
                        value = primaryPhone,
                        onValueChange = { primaryPhone = it },
                        label = "Primary Phone",
                        placeholder = "+1 (555) 000-0000",
                        keyboardType = KeyboardType.Phone,
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
                        }
                    )

                    // Additional Phones List
                    additionalPhones.forEachIndexed { idx, extraPhone ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                PersonaTextInput(
                                    value = extraPhone,
                                    onValueChange = { newVal ->
                                        additionalPhones[idx] = newVal
                                    },
                                    label = "Phone #${idx + 2}",
                                    placeholder = "Secondary / Work Phone",
                                    keyboardType = KeyboardType.Phone
                                )
                            }
                            IconButton(
                                onClick = {
                                    haptics.light()
                                    additionalPhones.removeAt(idx)
                                },
                                modifier = Modifier
                                    .padding(top = 18.dp)
                                    .size(36.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove phone", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    // Add another phone button
                    TextButton(
                        onClick = {
                            haptics.light()
                            additionalPhones.add("")
                        },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add another phone", fontSize = 13.sp)
                    }

                    HorizontalDivider(color = dividerColor.copy(alpha = 0.7f), thickness = 0.8.dp)

                    // Primary Email
                    if (showEmailField || primaryEmail.isNotBlank()) {
                        PersonaTextInput(
                            value = primaryEmail,
                            onValueChange = { primaryEmail = it },
                            label = "Primary Email",
                            placeholder = "name@example.com",
                            keyboardType = KeyboardType.Email,
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
                            }
                        )

                        // Additional Emails List
                        additionalEmails.forEachIndexed { idx, extraEmail ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    PersonaTextInput(
                                        value = extraEmail,
                                        onValueChange = { newVal ->
                                            additionalEmails[idx] = newVal
                                        },
                                        label = "Email #${idx + 2}",
                                        placeholder = "Work / Alternative Email",
                                        keyboardType = KeyboardType.Email
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        haptics.light()
                                        additionalEmails.removeAt(idx)
                                    },
                                    modifier = Modifier
                                        .padding(top = 18.dp)
                                        .size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove email", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        TextButton(
                            onClick = {
                                haptics.light()
                                additionalEmails.add("")
                            },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add another email", fontSize = 13.sp)
                        }
                    }

                    // Address Field
                    if (showAddressField || relAddress.isNotBlank()) {
                        PersonaTextInput(
                            value = relAddress,
                            onValueChange = { relAddress = it },
                            label = "Physical Address / Location",
                            placeholder = "e.g. 124 Main St, Apt 4B, City",
                            leadingIcon = {
                                Icon(Icons.Default.Home, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
                            }
                        )
                    }
                }

                // Section 3: Important Dates
                val showDatesSection = showDobField || showAnniversaryField || isRomantic || relDob.isNotBlank() || relAnniversary.isNotBlank()
                if (showDatesSection) {
                    PersonaFormSection(
                        title = "Important Dates",
                        icon = Icons.Default.Cake
                    ) {
                        if (showDobField || relDob.isNotBlank()) {
                            PersonaTextInput(
                                value = relDob,
                                onValueChange = { relDob = it },
                                label = "Birthday / Date of Birth",
                                placeholder = "YYYY-MM-DD (e.g. 1994-06-15)",
                                leadingIcon = {
                                    Icon(Icons.Default.Cake, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
                                }
                            )
                        }

                        if (isRomantic || showAnniversaryField || relAnniversary.isNotBlank()) {
                            PersonaTextInput(
                                value = relAnniversary,
                                onValueChange = { relAnniversary = it },
                                label = "Anniversary",
                                placeholder = "YYYY-MM-DD (e.g. 2020-09-12)",
                                leadingIcon = {
                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
                                }
                            )
                        }
                    }
                }

                // Section 4: Personal Notes & Memories
                if (showNotesField || relNotes.isNotBlank()) {
                    PersonaFormSection(
                        title = "Notes & Memory Layer",
                        icon = Icons.Default.Description
                    ) {
                        PersonaTextInput(
                            value = relNotes,
                            onValueChange = { relNotes = it },
                            label = "Private Notes",
                            placeholder = "Favorite things, coffee order, key milestones, how you met, dietary preferences...",
                            singleLine = false,
                            maxLines = 5
                        )
                    }
                }



                // Quick Action Bar: "+ Add More Fields"
                val unaddedFields = buildList {
                    if (!showEmailField && primaryEmail.isBlank()) add("Email" to { showEmailField = true })
                    if (!showAddressField && relAddress.isBlank()) add("Address" to { showAddressField = true })
                    if (!showDobField && relDob.isBlank()) add("Birthday" to { showDobField = true })
                    if (!isRomantic && !showAnniversaryField && relAnniversary.isBlank()) add("Anniversary" to { showAnniversaryField = true })
                    if (!showNotesField && relNotes.isBlank()) add("Notes" to { showNotesField = true })
                }

                if (unaddedFields.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Add more details",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = textSecondary
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            unaddedFields.forEach { (label, action) ->
                                OutlinedButton(
                                    onClick = {
                                        haptics.selection()
                                        action()
                                        coroutineScope.launch {
                                            scrollState.animateScrollTo(scrollState.maxValue)
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // Sticky Bottom Save Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Button(
                    onClick = {
                        if (relName.isBlank()) {
                            nameError = true
                            haptics.warning()
                            feedback.warning()
                            coroutineScope.launch {
                                scrollState.animateScrollTo(0)
                            }
                            return@Button
                        }

                        haptics.success()
                        feedback.success()

                        val effectiveRole = if (relRole.equals("Other", ignoreCase = true) && customRole.isNotBlank()) {
                            customRole.trim()
                        } else {
                            relRole.trim()
                        }

                        val combinedPhones = (listOf(primaryPhone) + additionalPhones)
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .joinToString(", ")

                        val combinedEmails = (listOf(primaryEmail) + additionalEmails)
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .joinToString(", ")

                        onSavePerson(
                            effectiveRole,
                            relName.trim(),
                            selectedGender,
                            combinedPhones,
                            combinedEmails,
                            relAddress.trim(),
                            relDob.trim(),
                            relAnniversary.trim(),
                            relNotes.trim(),
                            selectedPhotoPath,
                            isNextOfKin
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .tactilePress(),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = "Save Connection",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
