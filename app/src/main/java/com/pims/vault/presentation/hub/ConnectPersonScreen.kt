package com.pims.vault.presentation.hub

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.avatar.PersonaAvatarManager
import com.pims.vault.presentation.ui.components.InternationalPhoneInput
import com.pims.vault.presentation.ui.components.PersonaDropdownSelector
import com.pims.vault.presentation.ui.components.PersonaFormSection
import com.pims.vault.presentation.ui.components.PersonaSearchableCombobox
import com.pims.vault.presentation.ui.components.PersonaTextInput
import com.pims.vault.presentation.ui.components.StandardDateInput
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.presentation.ui.util.rememberPimsFeedback
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
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
        photoPath: String?
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberPimsHaptics()
    val feedback = rememberPimsFeedback()

    BackHandler(onBack = onBack)

    var relRole by remember { mutableStateOf("Mother") }
    var personIsFemale by remember { mutableStateOf(true) }
    var relName by remember { mutableStateOf("") }

    var selectedPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedPhotoPath by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        selectedPhotoBitmap = bmp
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

    val relRoles = remember {
        listOf(
            "Mother", "Father", "Spouse", "Partner", "Son", "Daughter",
            "Brother", "Sister", "Friend", "Colleague", "Manager", "Doctor",
            "Lawyer", "Accountant", "Emergency Contact", "Mentor", "Other"
        )
    }

    var primaryPhone by remember { mutableStateOf("") }
    var additionalPhones by remember { mutableStateOf<List<String>>(emptyList()) }
    var primaryEmail by remember { mutableStateOf("") }
    var relAddress by remember { mutableStateOf("") }
    var relDob by remember { mutableStateOf("") }
    var relAnniversary by remember { mutableStateOf("") }
    var relNotes by remember { mutableStateOf("") }

    fun isRomanticRelationship(r: String): Boolean {
        val lower = r.lowercase()
        return lower.contains("spouse") || lower.contains("partner") ||
                lower.contains("wife") || lower.contains("husband")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Connect Person",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .tactilePress()
                    ) {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            if (relName.isBlank()) {
                                haptics.error()
                                feedback.warning()
                                return@Button
                            }
                            haptics.success()
                            feedback.success()
                            val allPhones = listOf(primaryPhone) + additionalPhones
                            val fullPhone = allPhones.filter { it.isNotBlank() }.joinToString(" • ")
                            onSavePerson(
                                relRole,
                                relName.trim(),
                                if (personIsFemale) "Female" else "Male",
                                fullPhone,
                                primaryEmail.trim(),
                                relAddress.trim(),
                                relDob.trim(),
                                relAnniversary.trim(),
                                relNotes.trim(),
                                selectedPhotoPath
                            )
                        },
                        enabled = relName.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .tactilePress()
                    ) {
                        Text(
                            text = "Save Person",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Profile Photo Upload Hero
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .border(
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
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
                            contentDescription = "Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Add Photo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Photo",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Text(
                    text = if (selectedPhotoBitmap != null) "Tap to change photo" else "Upload person photo",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Section 1: Relationship & Identity
            PersonaFormSection(
                title = "Relationship & Identity",
                icon = Icons.Default.Person
            ) {
                PersonaSearchableCombobox(
                    label = "How are you connected?",
                    value = relRole,
                    options = relRoles,
                    onValueChange = {
                        relRole = it
                        if (it.equals("Mother", ignoreCase = true) || it.equals("Sister", ignoreCase = true) || it.equals("Daughter", ignoreCase = true)) {
                            personIsFemale = true
                        } else if (it.equals("Father", ignoreCase = true) || it.equals("Brother", ignoreCase = true) || it.equals("Son", ignoreCase = true)) {
                            personIsFemale = false
                        }
                    },
                    placeholder = "Search or type relation (e.g. Mother, Partner)...",
                    allowCustom = true
                )

                PersonaDropdownSelector(
                    label = "Person's Gender",
                    selectedOption = if (personIsFemale) "Female" else "Male",
                    options = listOf("Female", "Male"),
                    onOptionSelected = { label ->
                        personIsFemale = label == "Female"
                    }
                )

                PersonaTextInput(
                    value = relName,
                    onValueChange = { relName = it },
                    label = "Full name *",
                    placeholder = "Full name"
                )
            }

            // Section 2: Contact Information
            PersonaFormSection(
                title = "Contact Information",
                icon = Icons.Default.Phone
            ) {
                InternationalPhoneInput(
                    value = primaryPhone,
                    onValueChange = { primaryPhone = it },
                    label = "Primary Phone"
                )

                additionalPhones.forEachIndexed { idx, phone ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            InternationalPhoneInput(
                                value = phone,
                                onValueChange = { newVal ->
                                    additionalPhones = additionalPhones.toMutableList().also { it[idx] = newVal }
                                },
                                label = "Phone ${idx + 2}"
                            )
                        }
                        IconButton(
                            onClick = {
                                additionalPhones = additionalPhones.toMutableList().also { it.removeAt(idx) }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                TextButton(
                    onClick = { additionalPhones = additionalPhones + "" },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add another phone", fontSize = 12.sp)
                }

                PersonaTextInput(
                    value = primaryEmail,
                    onValueChange = { primaryEmail = it },
                    label = "Primary Email",
                    placeholder = "name@example.com",
                    keyboardType = KeyboardType.Email
                )

                PersonaTextInput(
                    value = relAddress,
                    onValueChange = { relAddress = it },
                    label = "Address",
                    placeholder = "Physical address or city"
                )
            }

            // Section 3: Important Dates & Notes
            PersonaFormSection(
                title = "Dates & Notes",
                icon = Icons.Default.CalendarToday
            ) {
                StandardDateInput(
                    isoDate = relDob,
                    onDateChange = { relDob = it },
                    label = "Birthday"
                )

                AnimatedVisibility(visible = isRomanticRelationship(relRole)) {
                    StandardDateInput(
                        isoDate = relAnniversary,
                        onDateChange = { relAnniversary = it },
                        label = "Anniversary"
                    )
                }

                PersonaTextInput(
                    value = relNotes,
                    onValueChange = { relNotes = it },
                    label = "Private Notes",
                    placeholder = "Any personal details, memories, or preferences...",
                    singleLine = false
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
