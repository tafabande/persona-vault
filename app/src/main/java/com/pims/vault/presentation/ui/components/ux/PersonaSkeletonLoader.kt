package com.pims.vault.presentation.ui.components.ux

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Returns an animated fluid shimmer brush for skeleton loading states.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val isDark = isSystemInDarkTheme()
    val baseColor = if (isDark) Color(0xFF252528) else Color(0xFFE2E8F0)
    val highlightColor = if (isDark) Color(0xFF38383D) else Color(0xFFF1F5F9)

    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val shimmerBrush = rememberShimmerBrush()
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush)
    )
}

@Composable
fun SkeletonLine(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    widthFraction: Float = 1.0f,
    shape: Shape = RoundedCornerShape(6.dp)
) {
    SkeletonBox(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height),
        shape = shape
    )
}

@Composable
fun SkeletonCircle(
    size: Dp,
    modifier: Modifier = Modifier
) {
    SkeletonBox(
        modifier = modifier.size(size),
        shape = CircleShape
    )
}

/**
 * Pre-composed Vault list skeleton (passwords, credentials, cards).
 */
@Composable
fun VaultListSkeleton(
    itemCount: Int = 4,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(itemCount) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SkeletonCircle(size = 40.dp)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SkeletonLine(height = 14.dp, widthFraction = 0.55f)
                    SkeletonLine(height = 11.dp, widthFraction = 0.35f)
                }
                SkeletonBox(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            }
        }
    }
}

/**
 * Pre-composed Hero payment / identity card skeleton.
 */
@Composable
fun CardSkeleton(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .clip(RoundedCornerShape(22.dp))
    ) {
        val shimmerBrush = rememberShimmerBrush()
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(shimmerBrush)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SkeletonLine(height = 16.dp, widthFraction = 0.4f)
                SkeletonLine(height = 11.dp, widthFraction = 0.25f)
            }
            SkeletonBox(
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.BottomEnd),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

/**
 * Pre-composed 2-column masonry notes grid skeleton.
 */
@Composable
fun NotesGridSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(16.dp)
            )
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(16.dp)
            )
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}
