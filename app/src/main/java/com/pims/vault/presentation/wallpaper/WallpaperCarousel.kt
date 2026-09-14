package com.pims.vault.presentation.wallpaper

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * WallpaperCarousel
 *
 * Implements the Home visual hero area (~50-60% upper content) with:
 * - Rich, vibrant, warm landscape artworks (Terracotta Dunes, Lake Kariba, Graduation Day)
 * - Automatic 7-second rotation with smooth FastOutSlow crossfade
 * - Manual horizontal swipe gestures
 * - Subtle Ken Burns motion (1.00 -> 1.03 scale)
 * - Natural unboxed metadata presentation at lower edge
 * - Edge fade melting seamlessly into warm background (#F7F5F0)
 * - Zero empty ghost rings, zero unnecessary pagination dots
 * - Long-press for local artwork options
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WallpaperCarousel(
    wallpapers: List<WallpaperItem>,
    activeIndex: Int,
    onActiveIndexChanged: (Int) -> Unit,
    onOpenOptions: () -> Unit,
    modifier: Modifier = Modifier,
    carouselHeight: Dp = 340.dp
) {
    val haptics = rememberPimsHaptics()
    val backgroundColor = MaterialTheme.colorScheme.background

    if (wallpapers.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .combinedClickable(
                    onClick = {
                        haptics.selection()
                        onOpenOptions()
                    },
                    onLongClick = {
                        haptics.warning()
                        onOpenOptions()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = "No photos yet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap to add your photograph or select ambient artwork",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = activeIndex.coerceIn(0, (wallpapers.size - 1).coerceAtLeast(0)),
        pageCount = { wallpapers.size }
    )

    // Synchronize external index changes with pager
    LaunchedEffect(activeIndex) {
        if (pagerState.currentPage != activeIndex && activeIndex in wallpapers.indices) {
            pagerState.animateScrollToPage(activeIndex)
        }
    }

    // Report user swipe changes
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page != activeIndex) {
                onActiveIndexChanged(page)
            }
        }
    }

    // Calm automatic rotation: 7-second interval (pauses when user interacts)
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(pagerState.pageCount, isDragged) {
        while (!isDragged && pagerState.pageCount > 1) {
            delay(7000L)
            if (!pagerState.isScrollInProgress) {
                val nextPage = (pagerState.currentPage + 1) % pagerState.pageCount
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    // Subtle Ken Burns motion: gentle 1.00 -> 1.03 breathing scale over a slow 7s cycle
    val infiniteTransition = rememberInfiniteTransition(label = "KenBurns")
    val kenBurnsScale by infiniteTransition.animateFloat(
        initialValue = 1.00f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "kenBurnsScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(carouselHeight)
            .combinedClickable(
                onClick = {
                    haptics.selection()
                    if (wallpapers.isNotEmpty()) {
                        val next = (pagerState.currentPage + 1) % wallpapers.size
                        onActiveIndexChanged(next)
                    }
                },
                onLongClick = {
                    haptics.warning()
                    onOpenOptions()
                }
            )
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val wallpaper = wallpapers.getOrNull(page)
            if (wallpaper != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = kenBurnsScale
                            scaleY = kenBurnsScale
                        }
                ) {
                    when (wallpaper.type) {
                        WallpaperType.GENERATIVE_ARTWORK -> {
                            GenerativeArtworkVisual(seed = wallpaper.artSeed)
                        }
                        WallpaperType.LOCAL_IMAGE -> {
                            val bitmap = remember(wallpaper.localFilePath) {
                                wallpaper.localFilePath?.let { BitmapFactory.decodeFile(it) }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = wallpaper.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                GenerativeArtworkVisual(seed = 2)
                            }
                        }
                    }

                    // Soft vertical fade dissolving directly into warm background (#F7F5F0)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.55f to Color.Transparent,
                                    0.82f to backgroundColor.copy(alpha = 0.75f),
                                    1.0f to backgroundColor
                                )
                            )
                    )
                }
            }
        }

        // Image Metadata for ambient artworks (custom photos display clean with no distracting overlays)
        val currentWp = wallpapers.getOrNull(pagerState.currentPage)
        if (currentWp != null && currentWp.type != WallpaperType.LOCAL_IMAGE && currentWp.title.isNotBlank()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 16.dp, end = 24.dp)
            ) {
                AnimatedContent(
                    targetState = currentWp,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(400))
                    },
                    label = "WpMeta"
                ) { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        item.subtitle?.takeIf { it.isNotBlank() }?.let { sub ->
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Rich, evocative generative visual artwork:
 * Seed 2: Terracotta Dunes - Layered rolling desert sunset with glowing sun
 * Seed 1: Lake Kariba - Golden dawn lake horizon with island silhouettes
 * Seed 3: Graduation Day - Radiant twilight horizon with majestic hills
 */
@Composable
private fun GenerativeArtworkVisual(
    seed: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "GenerativeHarmonics")

    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        when (seed) {
            1 -> {
                // =============================================================
                // LAKE KARIBA: Golden dawn horizon over tranquil water
                // =============================================================
                // Sky gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF6ED),
                            Color(0xFFF9E4CB),
                            Color(0xFFE8BD82),
                            Color(0xFFC98944)
                        ),
                        startY = 0f,
                        endY = h * 0.65f
                    )
                )

                // Radiant morning sun
                val sunCenter = Offset(w * 0.52f + sin(drift) * 12f, h * 0.36f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFAF0),
                            Color(0xFFFFD59E),
                            Color(0x88FFA834),
                            Color.Transparent
                        ),
                        center = sunCenter,
                        radius = w * 0.38f
                    ),
                    center = sunCenter,
                    radius = w * 0.38f
                )
                drawCircle(
                    color = Color(0xFFFFF8EE),
                    radius = 28.dp.toPx(),
                    center = sunCenter
                )

                // Distant mountain ridges across the water
                val ridge1 = Path().apply {
                    moveTo(0f, h * 0.50f)
                    cubicTo(w * 0.25f, h * 0.44f, w * 0.40f, h * 0.48f, w * 0.60f, h * 0.42f)
                    cubicTo(w * 0.78f, h * 0.46f, w * 0.90f, h * 0.43f, w, h * 0.47f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = ridge1,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFBA7A3D), Color(0xFF8F4D18)),
                        startY = h * 0.42f,
                        endY = h * 0.65f
                    )
                )

                // Tranquil water horizon surface
                val water = Path().apply {
                    moveTo(0f, h * 0.55f)
                    lineTo(w, h * 0.55f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = water,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFD4924A),
                            Color(0xFFA86326),
                            Color(0xFF6B360E)
                        ),
                        startY = h * 0.55f,
                        endY = h
                    )
                )

                // Island silhouette
                val island = Path().apply {
                    moveTo(w * 0.18f, h * 0.58f)
                    cubicTo(w * 0.30f, h * 0.52f, w * 0.42f, h * 0.54f, w * 0.55f, h * 0.58f)
                    lineTo(w * 0.18f, h * 0.58f)
                    close()
                }
                drawPath(island, color = Color(0xFF4A250B))
            }

            3 -> {
                // =============================================================
                // GRADUATION DAY: Majestic dawn horizon over rolling green/sage ridges
                // =============================================================
                // Dawn Sky
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF7F3E9),
                            Color(0xFFE5DDD0),
                            Color(0xFFBFD1C1),
                            Color(0xFF8BA690)
                        ),
                        startY = 0f,
                        endY = h * 0.60f
                    )
                )

                // Radiant beacon sun
                val sunCenter = Offset(w * 0.65f, h * 0.28f + sin(drift) * 8f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFDF5),
                            Color(0xFFFFE8B8),
                            Color(0x66E2C98A),
                            Color.Transparent
                        ),
                        center = sunCenter,
                        radius = w * 0.35f
                    ),
                    center = sunCenter,
                    radius = w * 0.35f
                )
                drawCircle(
                    color = Color(0xFFFFF9EA),
                    radius = 24.dp.toPx(),
                    center = sunCenter
                )

                // Back Hill Ridge
                val hill1 = Path().apply {
                    moveTo(0f, h * 0.48f)
                    cubicTo(w * 0.30f, h * 0.38f, w * 0.65f, h * 0.52f, w, h * 0.42f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = hill1,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF7A977F), Color(0xFF56725B)),
                        startY = h * 0.38f,
                        endY = h * 0.70f
                    )
                )

                // Foreground Hill Ridge
                val hill2 = Path().apply {
                    moveTo(0f, h * 0.58f)
                    cubicTo(w * 0.35f, h * 0.62f, w * 0.68f, h * 0.48f, w, h * 0.56f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = hill2,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF455C4A), Color(0xFF2E4032)),
                        startY = h * 0.48f,
                        endY = h
                    )
                )
            }

            else -> {
                // =============================================================
                // TERRACOTTA DUNES: Layered rolling sunset desert in warm terracotta
                // =============================================================
                // Warm Dusk Sky
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF9EFE7),
                            Color(0xFFF4DDD0),
                            Color(0xFFEABEA8),
                            Color(0xFFDC8B62)
                        ),
                        startY = 0f,
                        endY = h * 0.60f
                    )
                )

                // Radiant Sunset Orb
                val sunCenter = Offset(w * 0.40f - sin(drift) * 10f, h * 0.30f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFF9F5),
                            Color(0xFFFFD5BE),
                            Color(0x88E27845),
                            Color.Transparent
                        ),
                        center = sunCenter,
                        radius = w * 0.42f
                    ),
                    center = sunCenter,
                    radius = w * 0.42f
                )
                drawCircle(
                    color = Color(0xFFFFF3EC),
                    radius = 32.dp.toPx(),
                    center = sunCenter
                )

                // Dune 1 (Distant undulating ridge)
                val dune1 = Path().apply {
                    moveTo(0f, h * 0.46f)
                    cubicTo(w * 0.32f, h * 0.38f, w * 0.62f, h * 0.50f, w, h * 0.42f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = dune1,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFD47A4D), Color(0xFFB65F3A)),
                        startY = h * 0.38f,
                        endY = h * 0.65f
                    )
                )

                // Dune 2 (Midground crest)
                val dune2 = Path().apply {
                    moveTo(0f, h * 0.56f)
                    cubicTo(w * 0.38f, h * 0.62f, w * 0.70f, h * 0.47f, w, h * 0.54f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = dune2,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFA34D27), Color(0xFF8A3D1B)),
                        startY = h * 0.47f,
                        endY = h * 0.80f
                    )
                )

                // Dune 3 (Foreground warm shadow)
                val dune3 = Path().apply {
                    moveTo(0f, h * 0.68f)
                    cubicTo(w * 0.45f, h * 0.64f, w * 0.75f, h * 0.72f, w, h * 0.66f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = dune3,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF6E2D11), Color(0xFF4D1C09)),
                        startY = h * 0.64f,
                        endY = h
                    )
                )
            }
        }
    }
}
