package com.pims.vault.presentation.hub

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.presentation.ui.util.rememberPimsFeedback
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import java.io.File
import java.io.FileOutputStream

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
    var relName by remember { mutableStateOf("") }
    var primaryPhone by remember { mutableStateOf("") }

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

    val relationshipChips = remember {
        listOf("Mother", "Father", "Sibling", "Partner", "Friend", "Other")
    }

    val textPrimary = Color(0xFF20201E)
    val textSecondary = Color(0xFF6F6B63)
    val textHint = Color(0xFF8E8A82)
    val dividerColor = Color(0xFFEBE6DC)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Start
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // Header Titles
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Add someone",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        ),
                        color = textPrimary
                    )
                    Text(
                        text = "A little about them for now — you can add more any time.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = textSecondary
                    )
                }

                // Avatar Photo Picker
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF7F5F0))
                            .border(
                                BorderStroke(1.dp, Color(0xFFD5D1C8)),
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
                        } else {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Add a photo",
                                tint = textSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Text(
                        text = "Add a photo, or pick an avatar",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp),
                        color = textSecondary
                    )

                    // Avatar presets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEAE7DF))
                                .clickable {
                                    haptics.selection()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Avatar preset 1",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEAE7DF))
                                .clickable {
                                    haptics.selection()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Avatar preset 2",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // "How do you know them?"
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "How do you know them?",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                        color = textSecondary
                    )

                    // Wrap Chips
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            relationshipChips.take(4).forEach { role ->
                                val isSelected = relRole.equals(role, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) textPrimary else Color.White,
                                    border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFD5D1C8)),
                                    modifier = Modifier
                                        .tactilePress(targetScale = 0.94f) {
                                            haptics.selection()
                                            relRole = role
                                        }
                                ) {
                                    Text(
                                        text = role,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            fontSize = 14.5.sp
                                        ),
                                        color = if (isSelected) Color.White else textPrimary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            relationshipChips.drop(4).forEach { role ->
                                val isSelected = relRole.equals(role, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) textPrimary else Color.White,
                                    border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFD5D1C8)),
                                    modifier = Modifier
                                        .tactilePress(targetScale = 0.94f) {
                                            haptics.selection()
                                            relRole = role
                                        }
                                ) {
                                    Text(
                                        text = role,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            fontSize = 14.5.sp
                                        ),
                                        color = if (isSelected) Color.White else textPrimary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Name Field
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Name",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = textHint
                    )
                    BasicTextField(
                        value = relName,
                        onValueChange = { relName = it },
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = textPrimary
                        ),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (relName.isEmpty()) {
                                Text(
                                    text = "Their name",
                                    style = TextStyle(fontSize = 16.sp, color = textHint)
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                    HorizontalDivider(color = dividerColor, thickness = 1.dp)
                }

                // Phone Field
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Phone · optional",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = textHint
                    )
                    BasicTextField(
                        value = primaryPhone,
                        onValueChange = { primaryPhone = it },
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = textPrimary
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (primaryPhone.isEmpty()) {
                                Text(
                                    text = "+263 77 123 4567",
                                    style = TextStyle(fontSize = 16.sp, color = textHint)
                                )
                            }
                            innerTextField()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                    HorizontalDivider(color = dividerColor, thickness = 1.dp)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Save Button
            Button(
                onClick = {
                    if (relName.isBlank()) {
                        haptics.warning()
                        feedback.warning()
                        return@Button
                    }
                    haptics.success()
                    feedback.success()
                    onSavePerson(
                        relRole,
                        relName.trim(),
                        if (relRole.equals("Mother", true) || relRole.equals("Sister", true)) "Female" else "Male",
                        primaryPhone.trim(),
                        "",
                        "",
                        "",
                        "",
                        "",
                        selectedPhotoPath
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(bottom = 8.dp)
                    .tactilePress(),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = textPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}
