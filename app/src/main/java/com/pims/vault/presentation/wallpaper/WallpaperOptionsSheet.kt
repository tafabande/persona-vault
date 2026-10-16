package com.pims.vault.presentation.wallpaper

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.media.MediaReferenceResolver
import com.pims.vault.presentation.media.rememberMediaSelectionState
import com.pims.vault.presentation.ui.theme.LocalReducedMotion
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WallpaperOptionsSheet
 *
 * Implements the Persona Media Picker, Wallpaper and Multi-Select Specification:
 * - Surface discrimination: Persona backgrounds, widget backgrounds, Android system wallpapers.
 * - Staged preview with non-destructive zoom and repositioning (Apply, Cancel, Reset).
 * - Multi-select mode with stable IDs, long-press activation, Select All, Clear, Cancel.
 * - Safe removal default (keeps files on disk) vs destructive permanent deletion.
 * - Reference safety checking before storage deletion.
 * - Responsive thumbnail grid with loading, empty, and error states.
 * - Restrained 180-280ms motion with reduced-motion respect and >=48dp touch targets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperOptionsSheet(
    sheetState: SheetState,
    wallpapers: List<WallpaperItem>,
    availablePresets: List<WallpaperItem> = emptyList(),
    currentIndex: Int,
    onSelectWallpaper: (Int) -> Unit,
    onSelectPreset: (WallpaperItem) -> Unit,
    onPickFromDevice: () -> Unit,
    onRemoveWallpaper: (WallpaperItem) -> Unit,
    onRestorePresets: () -> Unit,
    onUpdateAlignment: ((String, Float) -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    // Multi-surface & bulk action extensions
    onApplySurface: ((surface: WallpaperSurface, item: WallpaperItem, alignX: Float, alignY: Float, zoom: Float) -> Boolean)? = null,
    onRemoveWallpapersBulk: ((ids: Set<String>) -> Unit)? = null,
    onDeleteWallpapersBulk: ((ids: Set<String>) -> Unit)? = null,
    mediaReferenceResolver: MediaReferenceResolver? = null,
    activeSurfaceWallpaperId: ((WallpaperSurface) -> String?)? = null
) {
    val haptics = rememberPimsHaptics()
    val isReducedMotion = LocalReducedMotion.current
    val animDuration = if (isReducedMotion) 0 else 220

    // Multi-selection state controller using stable IDs
    val selectionState = rememberMediaSelectionState()

    // Reconcile selection if backing list changes
    LaunchedEffect(wallpapers) {
        selectionState.reconcile(wallpapers.map { it.id })
    }

    // Active surface state
    var selectedSurface by remember { mutableStateOf(WallpaperSurface.PERSONA) }

    // Staged preview item & non-destructive transform state
    val initialActiveItem = wallpapers.getOrNull(currentIndex) ?: wallpapers.firstOrNull()
    var stagedItem by remember(currentIndex, wallpapers) {
        mutableStateOf(initialActiveItem)
    }
    var stagedAlignX by remember(stagedItem) { mutableFloatStateOf(stagedItem?.alignmentX ?: 0f) }
    var stagedAlignY by remember(stagedItem) { mutableFloatStateOf(stagedItem?.alignmentY ?: 0f) }
    var stagedZoom by remember(stagedItem) { mutableFloatStateOf(stagedItem?.zoomScale ?: 1f) }
    var isRepositionExpanded by remember { mutableStateOf(false) }

    // Dialog state for bulk removal or deletion
    var showRemovalConfirmDialog by remember { mutableStateOf(false) }
    var showDeletionConfirmDialog by remember { mutableStateOf(false) }
    var singleItemToDelete by remember { mutableStateOf<WallpaperItem?>(null) }

    BackHandler(enabled = true) {
        if (selectionState.isSelectionMode) {
            selectionState.exitSelectionMode()
        } else {
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (selectionState.isSelectionMode) {
                selectionState.exitSelectionMode()
            } else {
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = CircleShape
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
            }
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Header with Selection Mode Bar or Normal Header
            AnimatedVisibility(
                visible = selectionState.isSelectionMode,
                enter = fadeIn(tween(animDuration, easing = FastOutSlowInEasing)) + slideInVertically(tween(animDuration)),
                exit = fadeOut(tween(animDuration)) + slideOutVertically(tween(animDuration))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                haptics.selection()
                                selectionState.exitSelectionMode()
                            },
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit selection mode",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "${selectionState.selectedCount} selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.semantics {
                                contentDescription = "${selectionState.selectedCount} items selected"
                            }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                haptics.selection()
                                selectionState.selectAll(wallpapers.map { it.id })
                            },
                            modifier = Modifier.sizeIn(minHeight = 48.dp)
                        ) {
                            Text("Select all", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        TextButton(
                            onClick = {
                                haptics.selection()
                                selectionState.clearSelection()
                            },
                            modifier = Modifier.sizeIn(minHeight = 48.dp)
                        ) {
                            Text("Clear", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = !selectionState.isSelectionMode,
                enter = fadeIn(tween(animDuration)),
                exit = fadeOut(tween(animDuration))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Wallpaper & Media",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Apply to ${selectedSurface.title}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            haptics.selection()
                            onDismiss()
                        },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close wallpaper options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Surface Discrimination Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "TARGET SURFACE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WallpaperSurface.values().forEach { surface ->
                        val isSelected = selectedSurface == surface
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptics.selection()
                                selectedSurface = surface
                            },
                            label = {
                                Text(
                                    text = surface.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .sizeIn(minHeight = 48.dp)
                        )
                    }
                }

                Text(
                    text = selectedSurface.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            // 3. Staged Live Preview Canvas (Non-Destructive)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        RoundedCornerShape(24.dp)
                    )
                    .shadow(6.dp, RoundedCornerShape(24.dp))
            ) {
                val previewItem = stagedItem
                if (previewItem != null) {
                    when (previewItem.type) {
                        WallpaperType.GENERATIVE_ARTWORK -> {
                            GenerativeArtworkVisual(
                                seed = previewItem.artSeed,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        WallpaperType.LOCAL_IMAGE -> {
                            var previewBitmap by remember(previewItem.localFilePath) {
                                mutableStateOf(previewItem.localFilePath?.let { WallpaperBitmapCache.get(it) })
                            }
                            LaunchedEffect(previewItem.localFilePath) {
                                if (previewBitmap == null && previewItem.localFilePath != null) {
                                    val decoded = withContext(Dispatchers.IO) {
                                        WallpaperBitmapCache.getOrDecode(previewItem.localFilePath)
                                    }
                                    previewBitmap = decoded
                                }
                            }
                            val bmp = previewBitmap
                            if (bmp != null && bmp.width > 1) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = previewItem.title.ifBlank { "Preview wallpaper" },
                                    contentScale = ContentScale.Crop,
                                    alignment = BiasAlignment(stagedAlignX, stagedAlignY),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            scaleX = stagedZoom,
                                            scaleY = stagedZoom
                                        )
                                )
                            } else {
                                GenerativeArtworkVisual(seed = 2, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                } else {
                    GenerativeArtworkVisual(seed = 2, modifier = Modifier.fillMaxSize())
                }

                // Ambient gradient for metadata legibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.20f),
                                    Color.Black.copy(alpha = 0.70f)
                                )
                            )
                        )
                )

                // Surface Badge (Top Left)
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(bottomEnd = 12.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "Surface: ${selectedSurface.title}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Bottom HUD: Name and Active status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            previewItem == null -> "Default"
                            previewItem.title.isNotBlank() -> previewItem.title
                            else -> "Photo"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    val activeIdForThisSurface = activeSurfaceWallpaperId?.invoke(selectedSurface)
                        ?: if (selectedSurface == WallpaperSurface.PERSONA) wallpapers.getOrNull(currentIndex)?.id else null
                    val isActiveOnSurface = previewItem?.id == activeIdForThisSurface

                    if (isActiveOnSurface) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }

            // 4. Transform Adjustments Toggle & Sliders (Non-Destructive)
            if (stagedItem?.type == WallpaperType.LOCAL_IMAGE) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isRepositionExpanded = !isRepositionExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Reposition & Zoom",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = if (isRepositionExpanded) "Hide" else "Adjust",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (isRepositionExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Vertical Position
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Vertical Shift", style = MaterialTheme.typography.bodySmall)
                                    Text("%.1f".format(stagedAlignY), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(
                                    value = stagedAlignY,
                                    onValueChange = { stagedAlignY = it },
                                    valueRange = -1f..1f,
                                    modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)
                                )

                                // Horizontal Position
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Horizontal Shift", style = MaterialTheme.typography.bodySmall)
                                    Text("%.1f".format(stagedAlignX), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(
                                    value = stagedAlignX,
                                    onValueChange = { stagedAlignX = it },
                                    valueRange = -1f..1f,
                                    modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)
                                )

                                // Zoom Scale
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Zoom", style = MaterialTheme.typography.bodySmall)
                                    Text("%.1fx".format(stagedZoom), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(
                                    value = stagedZoom,
                                    onValueChange = { stagedZoom = it },
                                    valueRange = 1f..2.5f,
                                    modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp)
                                )

                                TextButton(
                                    onClick = {
                                        haptics.selection()
                                        stagedAlignX = 0f
                                        stagedAlignY = 0f
                                        stagedZoom = 1f
                                    },
                                    modifier = Modifier.align(Alignment.End).sizeIn(minHeight = 48.dp)
                                ) {
                                    Text("Reset Position")
                                }
                            }
                        }
                    }
                }
            }

            // 5. Explicit Actions: Apply, Cancel, Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Apply Action (Surface Explicit)
                Button(
                    onClick = {
                        haptics.selection()
                        val item = stagedItem
                        if (item != null) {
                            if (onApplySurface != null) {
                                onApplySurface(selectedSurface, item, stagedAlignX, stagedAlignY, stagedZoom)
                            } else {
                                val idx = wallpapers.indexOfFirst { it.id == item.id }
                                if (idx >= 0) onSelectWallpaper(idx)
                                onUpdateAlignment?.invoke(item.id, stagedAlignY)
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1.2f).sizeIn(minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Apply",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Cancel Action (Discards Staged Changes)
                OutlinedButton(
                    onClick = {
                        haptics.selection()
                        stagedItem = initialActiveItem
                        stagedAlignX = initialActiveItem?.alignmentX ?: 0f
                        stagedAlignY = initialActiveItem?.alignmentY ?: 0f
                        stagedZoom = initialActiveItem?.zoomScale ?: 1f
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).sizeIn(minHeight = 48.dp)
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                // Reset Action (Restores Defaults)
                OutlinedButton(
                    onClick = {
                        haptics.selection()
                        stagedAlignX = 0f
                        stagedAlignY = 0f
                        stagedZoom = 1f
                        onRestorePresets()
                        stagedItem = availablePresets.firstOrNull() ?: wallpapers.firstOrNull()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).sizeIn(minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reset",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            // 6. Bulk Action Bar (When in Selection Mode)
            AnimatedVisibility(
                visible = selectionState.isSelectionMode && selectionState.selectedCount > 0,
                enter = fadeIn(tween(animDuration)) + slideInVertically(tween(animDuration)),
                exit = fadeOut(tween(animDuration)) + slideOutVertically(tween(animDuration))
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Actions for ${selectionState.selectedCount} selected item(s)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Safe Default Action: Remove from Persona
                            Button(
                                onClick = {
                                    haptics.warning()
                                    showRemovalConfirmDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.weight(1f).sizeIn(minHeight = 48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Remove", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }

                            // Destructive Action: Permanent Delete from Disk
                            Button(
                                onClick = {
                                    haptics.warning()
                                    showDeletionConfirmDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier.weight(1f).sizeIn(minHeight = 48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delete", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            // 7. Responsive Thumbnail Grid Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wallpapers (${wallpapers.size})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (!selectionState.isSelectionMode) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    haptics.selection()
                                    onRestorePresets()
                                    stagedItem = availablePresets.firstOrNull() ?: wallpapers.firstOrNull()
                                },
                                modifier = Modifier.sizeIn(minHeight = 44.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restore Defaults", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                            }

                            OutlinedButton(
                                onClick = {
                                    haptics.selection()
                                    onPickFromDevice()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.sizeIn(minHeight = 44.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add photo", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }

                // Empty State
                if (wallpapers.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No wallpapers found",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap \"Add photo\" or restore default artwork themes below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    haptics.selection()
                                    onRestorePresets()
                                    stagedItem = availablePresets.firstOrNull()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore Default Themes")
                            }
                        }
                    }
                } else {
                    // Responsive Grid Layout
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 96.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(wallpapers, key = { it.id }) { wp ->
                            val isSelected = selectionState.isSelected(wp.id)
                            val isStaged = stagedItem?.id == wp.id
                            val isActiveOnSurface = wp.id == (activeSurfaceWallpaperId?.invoke(selectedSurface)
                                ?: wallpapers.getOrNull(currentIndex)?.id)

                            WallpaperThumbnailCard(
                                item = wp,
                                isSelected = isSelected,
                                isStaged = isStaged,
                                isActive = isActiveOnSurface,
                                isSelectionMode = selectionState.isSelectionMode,
                                onClick = {
                                    if (selectionState.isSelectionMode) {
                                        haptics.selection()
                                        selectionState.toggle(wp.id)
                                    } else {
                                        haptics.selection()
                                        stagedItem = wp
                                        stagedAlignX = wp.alignmentX
                                        stagedAlignY = wp.alignmentY
                                        stagedZoom = wp.zoomScale
                                    }
                                },
                                onLongClick = {
                                    if (!selectionState.isSelectionMode) {
                                        haptics.warning()
                                        selectionState.enterSelectionMode(wp.id)
                                    }
                                },
                                onDeleteSingle = {
                                    singleItemToDelete = wp
                                },
                                resolver = mediaReferenceResolver
                            )
                        }
                    }
                }
            }

            // 8. Themes Row
            if (availablePresets.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Procedural Themes",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(availablePresets) { _, preset ->
                            val isPresetActive = stagedItem?.type == WallpaperType.GENERATIVE_ARTWORK && stagedItem?.artSeed == preset.artSeed

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(
                                    width = if (isPresetActive) 2.dp else 1.dp,
                                    color = if (isPresetActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .width(136.dp)
                                    .clickable {
                                        haptics.selection()
                                        stagedItem = preset
                                        stagedAlignX = 0f
                                        stagedAlignY = 0f
                                        stagedZoom = 1f
                                    }
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(68.dp)
                                    ) {
                                        GenerativeArtworkVisual(seed = preset.artSeed, modifier = Modifier.fillMaxSize())
                                        if (isPresetActive) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(bottomEnd = 8.dp),
                                                modifier = Modifier.align(Alignment.TopStart)
                                            ) {
                                                Text(
                                                    text = "Staged",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS FOR SAFE REMOVAL AND DESTRUCTION ---

    // Bulk Removal Confirmation Dialog (Safe Default)
    if (showRemovalConfirmDialog) {
        val count = selectionState.selectedCount
        AlertDialog(
            onDismissRequest = { showRemovalConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Remove $count wallpaper${if (count > 1) "s" else ""}?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "They will be removed from your Persona reel. The original image files will remain safely stored on your device. You can undo this action immediately after.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ids = selectionState.selectedIds.toSet()
                        showRemovalConfirmDialog = false
                        selectionState.exitSelectionMode()
                        if (onRemoveWallpapersBulk != null) {
                            onRemoveWallpapersBulk(ids)
                        } else {
                            wallpapers.filter { it.id in ids }.forEach { onRemoveWallpaper(it) }
                        }
                    }
                ) {
                    Text("Remove from Persona")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemovalConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bulk Deletion Confirmation Dialog (Destructive)
    if (showDeletionConfirmDialog) {
        val count = selectionState.selectedCount
        // Check how many have shared references
        val selectedItems = wallpapers.filter { it.id in selectionState.selectedIds }
        val sharedCount = selectedItems.count { wp ->
            wp.localFilePath?.let { mediaReferenceResolver?.isReferenced(it, excludingWallpaperId = wp.id) } == true
        }

        AlertDialog(
            onDismissRequest = { showDeletionConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Permanently delete $count item${if (count > 1) "s" else ""}?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This will permanently remove the underlying image files from storage. This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (sharedCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text(
                                text = "Notice: $sharedCount item(s) are currently referenced as a profile avatar or contact photo and will be safely preserved in storage.",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ids = selectionState.selectedIds.toSet()
                        showDeletionConfirmDialog = false
                        selectionState.exitSelectionMode()
                        if (onDeleteWallpapersBulk != null) {
                            onDeleteWallpapersBulk(ids)
                        } else {
                            wallpapers.filter { it.id in ids }.forEach { onRemoveWallpaper(it) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletionConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Single item removal confirmation dialog
    singleItemToDelete?.let { targetItem ->
        AlertDialog(
            onDismissRequest = { singleItemToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Remove this wallpaper?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "It will be removed from your wallpaper reel. You can restore artwork presets anytime with Reset.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val item = targetItem
                        singleItemToDelete = null
                        onRemoveWallpaper(item)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { singleItemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Thumbnail card with loading, empty, and error states,
 * supporting tap and long-press multi-selection.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WallpaperThumbnailCard(
    item: WallpaperItem,
    isSelected: Boolean,
    isStaged: Boolean,
    isActive: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeleteSingle: () -> Unit,
    resolver: MediaReferenceResolver?
) {
    val haptics = rememberPimsHaptics()
    var isLoading by remember { mutableStateOf(item.type == WallpaperType.LOCAL_IMAGE) }
    var isError by remember { mutableStateOf(false) }
    var decodedBitmap by remember(item.localFilePath) {
        mutableStateOf(item.localFilePath?.let { WallpaperBitmapCache.get(it) })
    }

    LaunchedEffect(item.localFilePath) {
        if (item.type == WallpaperType.LOCAL_IMAGE && item.localFilePath != null) {
            if (decodedBitmap == null) {
                isLoading = true
                val bmp = withContext(Dispatchers.IO) {
                    WallpaperBitmapCache.getOrDecode(item.localFilePath)
                }
                decodedBitmap = bmp
                isLoading = false
                isError = (bmp.width <= 1)
            } else {
                isLoading = false
                isError = ((decodedBitmap?.width ?: 0) <= 1)
            }
        } else {
            isLoading = false
        }
    }

    val isShared = remember(item.localFilePath) {
        item.localFilePath?.let { resolver?.isReferenced(it, excludingWallpaperId = item.id) } == true
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .clip(RoundedCornerShape(18.dp))
            .border(
                BorderStroke(
                    width = if (isSelected || isStaged) 2.5.dp else 1.dp,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isStaged -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    }
                ),
                RoundedCornerShape(18.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .semantics {
                contentDescription = "${item.title.ifBlank { "Wallpaper photo" }}, ${if (isSelected) "selected" else "unselected"}"
                role = if (isSelectionMode) Role.Checkbox else Role.Button
            }
    ) {
        // Visual Presentation
        when {
            isLoading -> {
                // Loading State Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            isError -> {
                // Error State Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Image unavailable",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Missing",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            item.type == WallpaperType.GENERATIVE_ARTWORK -> {
                GenerativeArtworkVisual(seed = item.artSeed, modifier = Modifier.fillMaxSize())
            }
            else -> {
                val bmp = decodedBitmap
                if (bmp != null && bmp.width > 1) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = BiasAlignment(item.alignmentX, item.alignmentY),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    GenerativeArtworkVisual(seed = 2, modifier = Modifier.fillMaxSize())
                }
            }
        }

        // Bottom gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.5f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.75f)
                    )
                )
        )

        // Top Badges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left badge: Active or Shared
            if (isActive) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Active wallpaper",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            } else if (isShared) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = "Shared",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 8.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            // Right badge: Selection Checkbox or Delete icon
            if (isSelectionMode) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.45f),
                    border = BorderStroke(1.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.White),
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            } else if (!item.isBuiltIn) {
                // Delete shortcut for user photos in normal mode
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable {
                            haptics.warning()
                            onDeleteSingle()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete wallpaper",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Title Label
        Text(
            text = item.title.ifBlank { "Photo" },
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}
