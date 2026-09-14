package com.pims.vault.presentation.avatar

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.LocalPersonaMood
import com.pims.vault.presentation.ui.theme.LocalReducedMotion
import com.pims.vault.presentation.ui.theme.PersonaMotion
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import com.pims.vault.presentation.ui.util.rememberPimsSoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class EditorTab(val label: String) {
    GENDER("Gender"),
    HAIR("Hair"),
    FACE("Face"),
    SKIN("Skin"),
    OUTFIT("Outfit"),
    ACCESSORIES("Accessories"),
    BACKDROP("Backdrop"),
    BEHAVIOR("Behavior")
}

/**
 * AvatarEditorScreen (ModalBottomSheet or standalone).
 *
 * Implements the Avatar Redesign Experience:
 * - Dual Avatar Source: Generated | Custom photo
 * - Explicit Gender selection without locking customizations
 * - Expanded realistic hairstyle and hair color catalog
 * - Local-first photo upload & crop
 * - Live synchronized preview
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
    val activeMood = LocalPersonaMood.current
    val isReducedMotion = LocalReducedMotion.current
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var config by remember { mutableStateOf(initialConfig) }
    var activeSource by remember { mutableStateOf(initialConfig.avatarSource) }
    var selectedTab by remember { mutableStateOf(EditorTab.HAIR) }
    var showAllHairs by remember { mutableStateOf(false) }

    var currentBehaviorMode by remember {
        mutableStateOf(avatarManager?.avatarBehavior?.value ?: AvatarBehaviorMode.ALIVE)
    }
    var liveExpression by remember { mutableStateOf<AvatarExpression?>(null) }
    var randomizeTrigger by remember { mutableIntStateOf(0) }

    // Gallery Image Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            scope.launch {
                try {
                    context.contentResolver.openInputStream(pickedUri)?.use { stream ->
                        val original = BitmapFactory.decodeStream(stream)
                        if (original != null) {
                            // Center-crop to square
                            val side = minOf(original.width, original.height)
                            val x = (original.width - side) / 2
                            val y = (original.height - side) / 2
                            val cropped = Bitmap.createBitmap(original, x, y, side, side)

                            val newPath = avatarManager?.saveCustomPhoto(cropped)
                            if (newPath != null) {
                                config = config.copy(
                                    avatarSource = AvatarSource.CUSTOM_IMAGE,
                                    customAvatarPath = newPath
                                )
                                activeSource = AvatarSource.CUSTOM_IMAGE
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

    // Live preview scale animation on randomize
    val previewScale = remember { androidx.compose.animation.core.Animatable(1f) }
    androidx.compose.runtime.LaunchedEffect(randomizeTrigger) {
        if (randomizeTrigger > 0) {
            previewScale.snapTo(0.88f)
            previewScale.animateTo(1f, PersonaMotion.bouncySpring(isReducedMotion))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Personalize Persona",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Your identity, your appearance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Dual Avatar Source Segmented Selector (Generated | Custom photo)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AvatarSource.values().forEach { source ->
                val isSelected = activeSource == source
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable {
                            if (!isSelected) {
                                haptics.light()
                                activeSource = source
                                config = config.copy(avatarSource = source)
                                avatarManager?.setAvatarSource(source)
                            }
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = source.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. Live Avatar Preview Container
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .scale(previewScale.value),
            contentAlignment = Alignment.Center
        ) {
            PersonaAvatar(
                config = config.copy(avatarSource = activeSource),
                expression = liveExpression,
                size = 156.dp,
                showBackground = true,
                customMood = activeMood,
                behaviorMode = currentBehaviorMode
            )
        }

        if (activeSource == AvatarSource.CUSTOM_IMAGE) {
            // =========================================================================
            // CUSTOM PHOTO CONTROLS (Local-First, Privacy-Respecting)
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (!config.customAvatarPath.isNullOrBlank()) "Custom Photo Active" else "No Photo Selected",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Your photograph stays 100% private and offline on your device. It is never automatically uploaded to the cloud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                haptics.selection()
                                photoPickerLauncher.launch("image/*")
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (config.customAvatarPath.isNullOrBlank()) Icons.Default.AddAPhoto else Icons.Default.Photo,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (config.customAvatarPath.isNullOrBlank()) "Choose photo" else "Replace photo")
                        }

                        if (!config.customAvatarPath.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    haptics.selection()
                                    avatarManager?.removeCustomPhoto()
                                    config = config.copy(
                                        customAvatarPath = null,
                                        avatarSource = AvatarSource.GENERATED
                                    )
                                    activeSource = AvatarSource.GENERATED
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        } else {
            // =========================================================================
            // GENERATED AVATAR CONTROLS
            // =========================================================================

            // Randomize Action
            OutlinedButton(
                onClick = {
                    haptics.selection()
                    soundManager.navigation()
                    scope.launch {
                        liveExpression = AvatarExpression.HAPPY_SQUISH
                        config = PersonaAvatarConfig.random(style = config.style, gender = config.gender)
                        randomizeTrigger++
                        kotlinx.coroutines.delay(650)
                        liveExpression = null
                    }
                },
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                modifier = Modifier.tactilePress()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Randomize",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Style Selector: Soft | Cute | Sketch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AvatarStyle.values().forEach { style ->
                    val isSelected = config.style == style
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable {
                                if (!isSelected) {
                                    haptics.light()
                                    config = config.copy(style = style)
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = style.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Category Tabs: Gender | Hair | Face | Skin | Outfit | Accessories | Backdrop | Behavior
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EditorTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.clickable {
                            haptics.light()
                            selectedTab = tab
                        }
                    ) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            // Customization Controls for Active Category
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        EditorTab.GENDER -> {
                            Text("Gender Appearance", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                text = "Sets visual defaults & recommendations without locking or restricting any hairstyle, clothing, or color customization.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AvatarGender.values().forEach { gender ->
                                    val isSelected = config.gender == gender
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                haptics.light()
                                                val autoShape = if (gender == AvatarGender.MALE) HeadShape.CHISELED_ANGULAR else if (gender == AvatarGender.FEMALE) HeadShape.SOFT_OVAL else config.headShape
                                                config = config.copy(gender = gender, headShape = autoShape)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = gender.label,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Base Jaw Silhouette", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HeadShape.values().forEach { shape ->
                                    ChoiceChip(label = shape.label, isSelected = config.headShape == shape) {
                                        haptics.light()
                                        config = config.copy(headShape = shape)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                        }

                        EditorTab.HAIR -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Hairstyle", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text(
                                    text = if (showAllHairs) "Showing all" else "Filter: Recommended",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { showAllHairs = !showAllHairs }
                                )
                            }

                            val availableHairs = remember(config.gender, showAllHairs) {
                                if (showAllHairs || config.gender == AvatarGender.UNSPECIFIED || config.gender == AvatarGender.NON_BINARY) {
                                    HairStyle.values().toList()
                                } else if (config.gender == AvatarGender.FEMALE) {
                                    listOf(
                                        HairStyle.DEFAULT,
                                        HairStyle.BRAIDS, HairStyle.BOX_BRAIDS, HairStyle.CORNROWS,
                                        HairStyle.LOCS, HairStyle.PONYTAIL, HairStyle.WAVY_LONG,
                                        HairStyle.LONG_STRAIGHT, HairStyle.BOB, HairStyle.CURLY_AFRO,
                                        HairStyle.SHORT_CROP, HairStyle.TOP_BUN, HairStyle.TEXTURED_FADE
                                    )
                                } else {
                                    listOf(
                                        HairStyle.TEXTURED_FADE, HairStyle.DEFAULT,
                                        HairStyle.SHORT_CROP, HairStyle.LOW_FADE, HairStyle.HIGH_FADE,
                                        HairStyle.SHORT_CURS, HairStyle.CURLY_AFRO, HairStyle.LOCS,
                                        HairStyle.BRAIDS, HairStyle.BUZZ, HairStyle.SIDE_PART
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                availableHairs.forEach { hair ->
                                    val isSelected = config.hairStyle == hair
                                    ChoiceChip(label = hair.label, isSelected = isSelected) {
                                        haptics.light()
                                        config = config.copy(hairStyle = hair)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }

                            Text("Hair Color", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HairColor.values().forEach { color ->
                                    val isSelected = config.hairColor == color
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(color.color)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.15f),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                haptics.light()
                                                config = config.copy(hairColor = color)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = if (color == HairColor.PLATINUM_WHITE) Color.Black else Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                        }

                        EditorTab.FACE -> {
                            Text("Eyebrows", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                EyebrowType.values().forEach { brow ->
                                    ChoiceChip(label = brow.label, isSelected = config.eyebrowType == brow) {
                                        haptics.light()
                                        config = config.copy(eyebrowType = brow)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }

                            Text("Eyes", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                EyeType.values().forEach { eye ->
                                    ChoiceChip(label = eye.label, isSelected = config.eyeType == eye) {
                                        haptics.light()
                                        config = config.copy(eyeType = eye)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }

                            Text("Mouth", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MouthType.values().forEach { mouth ->
                                    ChoiceChip(label = mouth.label, isSelected = config.mouthType == mouth) {
                                        haptics.light()
                                        config = config.copy(mouthType = mouth)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }

                            Text("Facial Details", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FacialFeature.values().forEach { feature ->
                                    ChoiceChip(label = feature.label, isSelected = config.facialFeature == feature) {
                                        haptics.light()
                                        config = config.copy(facialFeature = feature)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                        }

                        EditorTab.SKIN -> {
                            Text("Skin Tone", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SkinTone.values().forEach { tone ->
                                    val isSelected = config.skinTone == tone
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(tone.color)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.15f),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                haptics.light()
                                                config = config.copy(skinTone = tone)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = if (tone == SkinTone.DEEP_ESPRESSO) Color.White else Color.Black.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                        }

                        EditorTab.OUTFIT -> {
                            Text("Clothing Style", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ClothingStyle.values().forEach { outfit ->
                                    ChoiceChip(label = outfit.label, isSelected = config.clothingStyle == outfit) {
                                        haptics.light()
                                        config = config.copy(clothingStyle = outfit)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }

                            Text("Color", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ClothingColor.values().forEach { c ->
                                    val isSelected = config.clothingColor == c
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(c.color)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.15f),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                haptics.light()
                                                config = config.copy(clothingColor = c)
                                            }
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                        }

                        EditorTab.ACCESSORIES -> {
                            Text("Glasses & Accessories", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Accessory.values().forEach { acc ->
                                    ChoiceChip(label = acc.label, isSelected = config.accessory == acc) {
                                        haptics.light()
                                        config = config.copy(accessory = acc)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                        }

                        EditorTab.BACKDROP -> {
                            Text("Backdrop Shape", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BackgroundShape.values().forEach { shape ->
                                    ChoiceChip(label = shape.label, isSelected = config.backgroundShape == shape) {
                                        haptics.light()
                                        config = config.copy(backgroundShape = shape)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                        }

                        EditorTab.BEHAVIOR -> {
                            Text("Avatar Behavior", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AvatarBehaviorMode.values().forEach { mode ->
                                    val isSelected = currentBehaviorMode == mode
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                haptics.light()
                                                currentBehaviorMode = mode
                                                avatarManager?.setBehaviorMode(mode)
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = mode.label,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = mode.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Confirm Action: "Use this Persona"
        Button(
            onClick = {
                haptics.success()
                soundManager.success()
                avatarManager?.setBehaviorMode(currentBehaviorMode)
                onSave(config.copy(avatarSource = activeSource))
            },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .tactilePress()
        ) {
            Text(
                text = "Use this Persona",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
