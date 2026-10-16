package com.pims.vault.presentation.ui.components.carousel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pims.vault.presentation.ui.theme.LocalReducedMotion
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Reusable, native-feeling carousel component designed for the Persona ecosystem.
 *
 * Implements the Persona Android Carousel Behaviour Specification:
 * - Configurable motion timings (280ms slide, 220ms swipe settling, 160ms indicator).
 * - Continuous infinite looping or stop-at-boundaries navigation with zero unexpected jumps.
 * - Comprehensive autoplay lifecycle: pauses during drag, screen backgrounding, inactivity.
 * - Zero edge leakage: strict bounds clipping.
 * - Accessible: full screen-reader announcements, 48dp touch targets, reduced-motion adherence.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> PersonaCarousel(
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    initialIndex: Int = 0,
    config: PersonaCarouselConfig = PersonaCarouselConfig(),
    onItemSelected: ((item: T, index: Int) -> Unit)? = null,
    onItemClick: ((item: T, index: Int) -> Unit)? = null,
    onItemLongClick: ((item: T, index: Int) -> Unit)? = null,
    onCompletionAction: (() -> Unit)? = null,
    emptyContent: (@Composable () -> Unit)? = null,
    itemContent: @Composable (item: T, index: Int) -> Unit
) {
    if (items.isEmpty()) {
        if (emptyContent != null) {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                emptyContent()
            }
        }
        return
    }

    val scope = rememberCoroutineScope()
    val haptics = rememberPimsHaptics()
    val isReducedMotion = LocalReducedMotion.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Lifecycle tracking for autoplay pause
    var isLifecycleActive by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isLifecycleActive = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Determine loop mode and virtual page configuration
    val isLooping = config.endBehaviour == CarouselEndBehaviour.CONTINUOUS_LOOP && items.size > 1
    val infiniteMultiplier = if (isLooping) INFINITE_VIRTUAL_MULTIPLIER else 1

    val resolvedInitialPage = remember(items.size, isLooping) {
        val clampedIndex = initialIndex.coerceIn(0, items.size - 1)
        if (isLooping) {
            (infiniteMultiplier / 2) * items.size + clampedIndex
        } else {
            clampedIndex
        }
    }

    // Key pager state on collection size to avoid stale index bounds
    val pagerState = key(items.size, isLooping) {
        rememberPagerState(
            initialPage = resolvedInitialPage,
            pageCount = { (items.size * infiniteMultiplier).coerceAtLeast(1) }
        )
    }

    // Derived active index in [0, items.size - 1]
    val actualIndex by remember(pagerState, items.size) {
        derivedStateOf {
            if (items.isEmpty()) 0 else Math.floorMod(pagerState.currentPage, items.size)
        }
    }

    // Expose selection changes
    LaunchedEffect(actualIndex) {
        val item = items.getOrNull(actualIndex)
        if (item != null) {
            onItemSelected?.invoke(item, actualIndex)
            // Trigger completion action if reached terminal item in bounded mode
            if (config.endBehaviour == CarouselEndBehaviour.STOP_AT_BOUNDARIES && actualIndex == items.size - 1) {
                onCompletionAction?.invoke()
            }
        }
    }

    // Drag tracking for autoplay pausing
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    var isInteractingWithControls by remember { mutableStateOf(false) }

    // Autoplay Engine
    val allowAutoplay = remember(items.size, config, isReducedMotion) {
        PersonaCarouselLogic.shouldEnableAutoplay(
            totalItems = items.size,
            isSensitive = config.isSensitive,
            isReducedMotion = isReducedMotion,
            config = config
        )
    }

    LaunchedEffect(allowAutoplay, isDragged, isInteractingWithControls, isLifecycleActive, pagerState) {
        if (allowAutoplay) {
            while (true) {
                delay(config.autoplayIntervalMs)
                val isPaused = (config.pausePolicy.pauseOnInteraction && (isDragged || isInteractingWithControls)) ||
                        (config.pausePolicy.pauseWhenBackgrounded && !isLifecycleActive)

                if (!isPaused && items.size > 1) {
                    val nextPage = if (isLooping) {
                        pagerState.currentPage + 1
                    } else {
                        (pagerState.currentPage + 1).takeIf { it < items.size }
                    }

                    if (nextPage != null) {
                        pagerState.animateScrollToPage(
                            page = nextPage,
                            animationSpec = tween(
                                durationMillis = if (isReducedMotion) 0 else config.slideDurationMs,
                                easing = config.easing
                            )
                        )
                    }
                }
            }
        }
    }

    // Accessible semantics
    val accessibilityStateDescription = "Item ${actualIndex + 1} of ${items.size}"

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .semantics {
                contentDescription = "Carousel container"
                stateDescription = accessibilityStateDescription
            }
    ) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            contentPadding = config.contentPadding,
            pageSpacing = config.itemSpacing,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                pagerSnapDistance = PagerSnapDistance.atMost(1),
                snapAnimationSpec = tween(
                    durationMillis = if (isReducedMotion) 0 else config.swipeSettlingDurationMs,
                    easing = config.easing
                )
            ),
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageActualIndex = Math.floorMod(page, items.size)
            val currentItem = items.getOrNull(pageActualIndex)
            if (currentItem != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .combinedClickable(
                            onClick = {
                                if (config.enableHaptics) haptics.light()
                                onItemClick?.invoke(currentItem, pageActualIndex)
                            },
                            onLongClick = {
                                if (config.enableHaptics) haptics.warning()
                                onItemLongClick?.invoke(currentItem, pageActualIndex)
                            }
                        )
                ) {
                    itemContent(currentItem, pageActualIndex)
                }
            }
        }

        // Optional Navigation Controls (Arrows)
        if (config.showNavigationArrows && items.size > 1) {
            val canGoBack = PersonaCarouselLogic.canScrollBackward(actualIndex, items.size, config.endBehaviour)
            val canGoForward = PersonaCarouselLogic.canScrollForward(actualIndex, items.size, config.endBehaviour)

            // Previous Button
            if (canGoBack || isLooping) {
                IconButton(
                    onClick = {
                        isInteractingWithControls = true
                        if (config.enableHaptics) haptics.light()
                        scope.launch {
                            val prevPage = if (isLooping) pagerState.currentPage - 1 else pagerState.currentPage - 1
                            if (prevPage >= 0) {
                                pagerState.animateScrollToPage(
                                    page = prevPage,
                                    animationSpec = tween(durationMillis = config.slideDurationMs, easing = config.easing)
                                )
                            }
                            delay(config.pausePolicy.resumeDelayMs)
                            isInteractingWithControls = false
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                        .size(48.dp) // Accessibility min touch target
                        .semantics { contentDescription = "Previous carousel item" },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Next Button
            if (canGoForward || isLooping) {
                IconButton(
                    onClick = {
                        isInteractingWithControls = true
                        if (config.enableHaptics) haptics.light()
                        scope.launch {
                            val nextPage = pagerState.currentPage + 1
                            if (isLooping || nextPage < items.size) {
                                pagerState.animateScrollToPage(
                                    page = nextPage,
                                    animationSpec = tween(durationMillis = config.slideDurationMs, easing = config.easing)
                                )
                            }
                            delay(config.pausePolicy.resumeDelayMs)
                            isInteractingWithControls = false
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .size(48.dp) // Accessibility min touch target
                        .semantics { contentDescription = "Next carousel item" },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Visual Pagination Indicators
        if (items.size > 1 && config.indicatorStyle != CarouselIndicatorStyle.NONE) {
            when (config.indicatorStyle) {
                CarouselIndicatorStyle.DOTS -> {
                    CarouselDotsIndicator(
                        totalItems = items.size,
                        activeIndex = actualIndex,
                        transitionDurationMs = config.indicatorTransitionDurationMs,
                        onDotClicked = { targetIndex ->
                            if (config.enableHaptics) haptics.light()
                            val targetVirtualPage = if (isLooping) {
                                PersonaCarouselLogic.resolveNearestTargetPage(
                                    currentPage = pagerState.currentPage,
                                    targetActual = targetIndex,
                                    totalItems = items.size
                                )
                            } else {
                                targetIndex
                            }
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    page = targetVirtualPage,
                                    animationSpec = tween(
                                        durationMillis = config.slideDurationMs,
                                        easing = config.easing
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    )
                }
                CarouselIndicatorStyle.SEGMENTED_LINE -> {
                    CarouselSegmentedIndicator(
                        totalItems = items.size,
                        activeIndex = actualIndex,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
                CarouselIndicatorStyle.NONE -> Unit
            }
        }
    }
}

/**
 * Micro-dot pagination indicator with smooth width transitions (160ms).
 */
@Composable
private fun CarouselDotsIndicator(
    totalItems: Int,
    activeIndex: Int,
    transitionDurationMs: Int,
    onDotClicked: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.28f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalItems) { index ->
            val isSelected = activeIndex == index
            val dotWidth by animateDpAsState(
                targetValue = if (isSelected) 16.dp else 6.dp,
                animationSpec = tween(durationMillis = transitionDurationMs),
                label = "dotWidth"
            )

            Box(
                modifier = Modifier
                    .size(width = dotWidth, height = 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (isSelected) Color.White else Color.White.copy(alpha = 0.40f)
                    )
                    .clickable { onDotClicked(index) }
            )
        }
    }
}

/**
 * Clean segmented line indicator across items.
 */
@Composable
private fun CarouselSegmentedIndicator(
    totalItems: Int,
    activeIndex: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalItems) { index ->
            val isSelected = activeIndex >= index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .size(height = 3.dp, width = Dp.Unspecified)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f)
                    )
            )
        }
    }
}
