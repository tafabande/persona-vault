package com.pims.vault.presentation.avatar

import com.pims.vault.presentation.ui.theme.PersonaIcons

import com.pims.vault.presentation.media.rememberPhotoPreview
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.pims.vault.presentation.ui.theme.PersonaIcon
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pims.vault.core.res.AppResources
import com.pims.vault.presentation.ui.components.PersonaAction
import com.pims.vault.presentation.ui.components.PersonaActionStyle
import com.pims.vault.presentation.ui.components.PersonaConfirmationDialog
import com.pims.vault.presentation.ui.components.PersonaFilterChip
import com.pims.vault.presentation.ui.components.PersonaSheet
import com.pims.vault.presentation.ui.components.PersonaSheetHeader
import com.pims.vault.presentation.ui.theme.PimsSpacing
import com.pims.vault.presentation.ui.theme.pimsPageGutter
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import com.pims.vault.presentation.ui.util.rememberPimsSoundManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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
    PersonaSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
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
    var photoRemoval by remember { mutableStateOf<AvatarPhotoRemoval?>(null) }

    val customBitmap = rememberPhotoPreview(config.customAvatarPath, maxDimension = 1024)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            scope.launch {
                try {
                    val manager = checkNotNull(avatarManager)
                    val newPath = manager.importAvatarPhoto(pickedUri)
                    config = config.copy(avatarSource = AvatarSource.CUSTOM_IMAGE, customAvatarPath = newPath)
                    haptics.success()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    haptics.warning()
                    Toast.makeText(context, "Photo could not be saved. Please choose another image.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = pimsPageGutter())
            .padding(bottom = PimsSpacing.xl)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PersonaSheetHeader("Profile avatar", onClose)

        Spacer(modifier = Modifier.height(20.dp))

        // Large Interactive Zoomable Avatar Circle
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
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
                val drawableRes = if (isFemale) AppResources.Drawable.ic_default_avatar_female else AppResources.Drawable.ic_default_avatar_male
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
            PersonaIcon(PersonaIcons.ZoomIn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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

        Spacer(Modifier.height(PimsSpacing.sectionGap))
        Text("Avatar style", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(PimsSpacing.s)
        ) {
            val showingDefault = config.avatarSource != AvatarSource.CUSTOM_IMAGE
            val styles = listOf("Male" to AvatarGender.MALE, "Female" to AvatarGender.FEMALE)
            styles.forEach { (label, gender) ->
                PersonaFilterChip(
                    label,
                    selected = showingDefault && if (gender == AvatarGender.FEMALE) {
                        config.gender == AvatarGender.FEMALE
                    } else {
                        config.gender != AvatarGender.FEMALE
                    },
                    onClick = {
                        haptics.selection()
                        config = config.copy(avatarSource = AvatarSource.GENERATED, gender = gender, customAvatarPath = null)
                    }
                )
            }
        }
        PersonaAction(
            if (config.avatarSource == AvatarSource.CUSTOM_IMAGE) "Change photo" else "Choose photo",
            onClick = {
                haptics.selection()
                photoPickerLauncher.launch("image/*")
            },
            modifier = Modifier.fillMaxWidth(),
            style = PersonaActionStyle.Quiet,
            icon = PersonaIcons.Photo,
            enabled = avatarManager != null
        )

        if (config.avatarSource == AvatarSource.CUSTOM_IMAGE) {
            Spacer(Modifier.height(PimsSpacing.sm))
            PersonaAction(
                "Remove photo",
                { photoRemoval = AvatarPhotoRemoval.Remove },
                modifier = Modifier.fillMaxWidth(),
                style = PersonaActionStyle.Quiet,
                icon = PersonaIcons.DeleteOutline
            )
        }
        Spacer(Modifier.height(PimsSpacing.sm))
        PersonaAction(
            "Reset to default avatar",
            onClick = {
                haptics.selection()
                if (config.avatarSource == AvatarSource.CUSTOM_IMAGE) {
                    photoRemoval = AvatarPhotoRemoval.Reset
                } else {
                    config = PersonaAvatarConfig.default()
                    zoomScale = 1.0f
                }
            },
            modifier = Modifier.fillMaxWidth(),
            style = PersonaActionStyle.Quiet,
            icon = PersonaIcons.Refresh
        )
        Spacer(Modifier.height(PimsSpacing.sectionGap))
        PersonaAction(
            "Apply avatar",
            onClick = {
                haptics.success()
                soundManager.success()
                avatarManager?.saveConfig(config)
                onSave(config)
            },
            modifier = Modifier.fillMaxWidth(),
            icon = PersonaIcons.Check
        )
    }

    photoRemoval?.let { action ->
        PersonaConfirmationDialog(
            onDismissRequest = { photoRemoval = null },
            title = if (action == AvatarPhotoRemoval.Reset) "Reset your avatar?" else "Remove your avatar photo?",
            message = "This removes the photo saved by Persona Vault.",
            confirmText = if (action == AvatarPhotoRemoval.Reset) "Reset avatar" else "Remove photo",
            onConfirm = {
                photoRemoval = null
                avatarManager?.clearCustomPhoto()
                config = if (action == AvatarPhotoRemoval.Reset) {
                    PersonaAvatarConfig.default()
                } else {
                    config.copy(avatarSource = AvatarSource.GENERATED, customAvatarPath = null)
                }
                zoomScale = 1.0f
            },
            isDestructive = true
        )
    }
}

private enum class AvatarPhotoRemoval { Remove, Reset }
