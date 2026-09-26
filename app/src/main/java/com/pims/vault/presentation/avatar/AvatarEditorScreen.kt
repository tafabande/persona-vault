package com.pims.vault.presentation.avatar

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.R
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import com.pims.vault.presentation.ui.util.rememberPimsSoundManager
import kotlinx.coroutines.launch
import java.io.File

/**
 * AvatarEditorSheet:
 * Clean WhatsApp-style profile avatar picker and customizer.
 * Supports:
 * - Male default avatar (white silhouette on neutral slate)
 * - Female default avatar (white silhouette with long hair on neutral slate)
 * - Custom photo upload with interactive zoom and pinch-to-resize
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarEditorSheet(
    initialConfig: PersonaAvatarConfig,
    sheetState: SheetState,
    avatarManager: PersonaAvatarManager? = null,
    onDismissRequest: () -> Unit,
    onSaveAvatar: (PersonaAvatarConfig) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        AvatarEditorContent(
            initialConfig = initialConfig,
            avatarManager = avatarManager,
            onClose = onDismissRequest,
            onSave = {
                onSaveAvatar(it)
                onDismissRequest()
            }
        )
    }
}

@Composable
fun AvatarEditorContent(
    initialConfig: PersonaAvatarConfig,
    avatarManager: PersonaAvatarManager? = null,
    onClose: () -> Unit,
    onSave: (PersonaAvatarConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberPimsHaptics()
    val soundManager = rememberPimsSoundManager()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var config by remember { mutableStateOf(initialConfig) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    val customBitmap = remember(config.customAvatarPath) {
        config.customAvatarPath?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) BitmapFactory.decodeFile(path) else null
            } catch (_: Exception) {
                null
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            scope.launch {
                try {
                    context.contentResolver.openInputStream(pickedUri)?.use { stream ->
                        val original = BitmapFactory.decodeStream(stream)
                        if (original != null) {
                            val maxDim = 1200
                            val scaleFactor = minOf(1.0f, maxDim.toFloat() / maxOf(original.width, original.height))
                            val targetW = (original.width * scaleFactor).toInt().coerceAtLeast(1)
                            val targetH = (original.height * scaleFactor).toInt().coerceAtLeast(1)
                            val scaled = if (scaleFactor < 1.0f) Bitmap.createScaledBitmap(original, targetW, targetH, true) else original

                            val newPath = avatarManager?.saveCustomPhoto(scaled)
                            if (newPath != null) {
                                config = config.copy(
                                    avatarSource = AvatarSource.CUSTOM_IMAGE,
                                    customAvatarPath = newPath
                                )
                                haptics.success()
                            }
                        }
                    }
                } catch (_: Exception) {
                    haptics.warning()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 36.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profile Avatar",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Large Interactive Zoomable Avatar Circle
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(Color(0xFFB0BEC5))
                .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.5f, 3.0f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (config.avatarSource == AvatarSource.CUSTOM_IMAGE && customBitmap != null) {
                Image(
                    bitmap = customBitmap.asImageBitmap(),
                    contentDescription = "Profile Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(zoomScale)
                        .clip(CircleShape)
                )
            } else {
                val isFemale = config.gender == AvatarGender.FEMALE
                val drawableRes = if (isFemale) R.drawable.ic_default_avatar_female else R.drawable.ic_default_avatar_male
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = "Default Avatar",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(zoomScale)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Zoom Slider with label
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.ZoomIn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Slider(
                value = zoomScale,
                onValueChange = { zoomScale = it },
                valueRange = 0.5f..3.0f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = "${(zoomScale * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "CHOOSE AVATAR STYLE",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Avatar Style Selection Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Male Avatar Card
            val isMaleSelected = config.avatarSource != AvatarSource.CUSTOM_IMAGE && config.gender != AvatarGender.FEMALE
            Card(
                modifier = Modifier
                    .weight(1f)
                    .tactilePress {
                        haptics.selection()
                        config = config.copy(
                            avatarSource = AvatarSource.GENERATED,
                            gender = AvatarGender.MALE
                        )
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isMaleSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(
                    width = if (isMaleSelected) 2.dp else 1.dp,
                    color = if (isMaleSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_default_avatar_male),
                        contentDescription = "Male",
                        modifier = Modifier.size(52.dp).clip(CircleShape)
                    )
                    Text(
                        text = "Male",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Female Avatar Card (with longish hair)
            val isFemaleSelected = config.avatarSource != AvatarSource.CUSTOM_IMAGE && config.gender == AvatarGender.FEMALE
            Card(
                modifier = Modifier
                    .weight(1f)
                    .tactilePress {
                        haptics.selection()
                        config = config.copy(
                            avatarSource = AvatarSource.GENERATED,
                            gender = AvatarGender.FEMALE
                        )
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFemaleSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(
                    width = if (isFemaleSelected) 2.dp else 1.dp,
                    color = if (isFemaleSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_default_avatar_female),
                        contentDescription = "Female",
                        modifier = Modifier.size(52.dp).clip(CircleShape)
                    )
                    Text(
                        text = "Female",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Upload Photo Card
            val isPhotoSelected = config.avatarSource == AvatarSource.CUSTOM_IMAGE
            Card(
                modifier = Modifier
                    .weight(1f)
                    .tactilePress {
                        haptics.selection()
                        photoPickerLauncher.launch("image/*")
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPhotoSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(
                    width = if (isPhotoSelected) 2.dp else 1.dp,
                    color = if (isPhotoSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "Upload",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = "Photo",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (config.avatarSource == AvatarSource.CUSTOM_IMAGE) {
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = {
                    haptics.light()
                    config = config.copy(
                        avatarSource = AvatarSource.GENERATED,
                        customAvatarPath = null
                    )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().tactilePress()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Remove Custom Photo")
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Save Button
        Button(
            onClick = {
                haptics.success()
                soundManager.success()
                avatarManager?.saveConfig(config)
                onSave(config)
            },
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .tactilePress()
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Apply Avatar",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            )
        }
    }
}
