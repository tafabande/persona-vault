package com.pims.vault.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import kotlinx.coroutines.launch

data class WalkthroughStep(
    val title: String,
    val description: String,
    val targetIndex: Int,
    val visualType: Int
)

/**
 * Interactive Onboarding Walkthrough
 *
 * Teaches:
 * 1. Digital Identity: "This is your personal identity space."
 * 2. Share Pass: "Share selected information securely."
 * 3. Documents: "Keep your important records organised."
 * 4. Activity: "See important sharing and account activity here."
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingWalkthroughScreen(
    onFinish: () -> Unit,
    onSkip: () -> Unit = onFinish,
    modifier: Modifier = Modifier
) {
    val haptics = rememberPimsHaptics()
    val scope = rememberCoroutineScope()

    val steps = listOf(
        WalkthroughStep(
            title = "Digital Identity",
            description = "This is your personal identity space.",
            targetIndex = 0,
            visualType = 0
        ),
        WalkthroughStep(
            title = "Share Pass",
            description = "Share selected information securely.",
            targetIndex = 1,
            visualType = 3
        ),
        WalkthroughStep(
            title = "Documents",
            description = "Keep your important records organised.",
            targetIndex = 2,
            visualType = 1
        ),
        WalkthroughStep(
            title = "Activity",
            description = "See important sharing and account activity here.",
            targetIndex = 3,
            visualType = 2
        )
    )

    val pagerState = rememberPagerState(pageCount = { steps.size })

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Skip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        haptics.light()
                        onSkip()
                    }
                ) {
                    Text(
                        text = "Skip",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }

            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIndex ->
                val step = steps[pageIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Minimalist Abstract Visual Artwork
                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (step.visualType) {
                            0 -> ConcentricOrbitalVisual()
                            1 -> IdentityLayersVisual()
                            2 -> ConnectedNodesVisual()
                            3 -> ShieldEmblemVisual()
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Interactive 2x2 Feature Selector (teaches Home -> Profile -> People -> Documents -> Share)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InteractiveWalkthroughCard(
                                icon = Icons.Default.Badge,
                                title = "Digital Identity",
                                isHighlighted = pageIndex == 0,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptics.selection()
                                    scope.launch { pagerState.animateScrollToPage(0) }
                                }
                            )
                            InteractiveWalkthroughCard(
                                icon = Icons.Default.QrCode,
                                title = "Share Pass",
                                isHighlighted = pageIndex == 1,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptics.selection()
                                    scope.launch { pagerState.animateScrollToPage(1) }
                                }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InteractiveWalkthroughCard(
                                icon = Icons.Default.Description,
                                title = "Documents",
                                isHighlighted = pageIndex == 2,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptics.selection()
                                    scope.launch { pagerState.animateScrollToPage(2) }
                                }
                            )
                            InteractiveWalkthroughCard(
                                icon = Icons.Default.History,
                                title = "Activity",
                                isHighlighted = pageIndex == 3,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptics.selection()
                                    scope.launch { pagerState.animateScrollToPage(3) }
                                }
                            )
                        }
                    }
                }
            }

            // Bottom Navigation & Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(steps.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .height(5.dp)
                                .width(if (isSelected) 22.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                                )
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(
                        visible = pagerState.currentPage > 0,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        TextButton(
                            onClick = {
                                haptics.light()
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        ) {
                            Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (pagerState.currentPage == 0) {
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    val isLastPage = pagerState.currentPage == steps.size - 1
                    Button(
                        onClick = {
                            if (isLastPage) {
                                haptics.success()
                                onFinish()
                            } else {
                                haptics.light()
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Text(
                            text = if (isLastPage) "Get started" else "Next",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveWalkthroughCard(
    icon: ImageVector,
    title: String,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptics = rememberPimsHaptics()
    val borderColor = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val containerColor = if (isHighlighted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(if (isHighlighted) 1.5.dp else 1.dp, borderColor),
        modifier = modifier.tactilePress {
            haptics.selection()
            onClick()
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// =============================================================================
// MINIMALIST VISUAL ARTWORKS
// =============================================================================

@Composable
private fun ConcentricOrbitalVisual(color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier = Modifier.size(140.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = color.copy(alpha = 0.12f),
            radius = 56.dp.toPx(),
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = color.copy(alpha = 0.25f),
            radius = 38.dp.toPx(),
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = color,
            radius = 12.dp.toPx(),
            center = center
        )
    }
}

@Composable
private fun IdentityLayersVisual(color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier = Modifier.size(140.dp)) {
        val w = size.width
        val h = size.height

        drawRoundRect(
            color = color.copy(alpha = 0.15f),
            topLeft = Offset(w * 0.2f, h * 0.15f),
            size = androidx.compose.ui.geometry.Size(w * 0.65f, h * 0.7f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawRoundRect(
            color = color.copy(alpha = 0.35f),
            topLeft = Offset(w * 0.15f, h * 0.22f),
            size = androidx.compose.ui.geometry.Size(w * 0.65f, h * 0.7f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = color,
            radius = 12.dp.toPx(),
            center = Offset(w * 0.32f, h * 0.42f)
        )
    }
}

@Composable
private fun ConnectedNodesVisual(color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier = Modifier.size(140.dp)) {
        val w = size.width
        val h = size.height
        val c1 = Offset(w * 0.3f, h * 0.35f)
        val c2 = Offset(w * 0.7f, h * 0.35f)
        val c3 = Offset(w * 0.5f, h * 0.68f)

        drawLine(color = color.copy(alpha = 0.25f), start = c1, end = c2, strokeWidth = 1.5.dp.toPx())
        drawLine(color = color.copy(alpha = 0.25f), start = c2, end = c3, strokeWidth = 1.5.dp.toPx())
        drawLine(color = color.copy(alpha = 0.25f), start = c3, end = c1, strokeWidth = 1.5.dp.toPx())

        drawCircle(color = color, radius = 8.dp.toPx(), center = c1)
        drawCircle(color = color, radius = 8.dp.toPx(), center = c2)
        drawCircle(color = color, radius = 11.dp.toPx(), center = c3)
    }
}

@Composable
private fun ShieldEmblemVisual(color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier = Modifier.size(140.dp)) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.2f)
            lineTo(w * 0.78f, h * 0.32f)
            lineTo(w * 0.78f, h * 0.58f)
            cubicTo(w * 0.78f, h * 0.75f, w * 0.5f, h * 0.85f, w * 0.5f, h * 0.85f)
            cubicTo(w * 0.5f, h * 0.85f, w * 0.22f, h * 0.75f, w * 0.22f, h * 0.58f)
            lineTo(w * 0.22f, h * 0.32f)
            close()
        }

        drawPath(path = path, color = color.copy(alpha = 0.25f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(color = color, radius = 7.dp.toPx(), center = Offset(w * 0.5f, h * 0.5f))
    }
}
