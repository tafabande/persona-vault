package com.pims.vault.presentation.ui.components.ux

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * 1. SPRING CHECKBOX
 * Apple / Reactbits-inspired checkbox featuring physics-spring pop and animated checkmark path.
 */
@Composable
fun SpringCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    enabled: Boolean = true
) {
    val haptics = rememberPimsHaptics()
    val scaleAnim by animateFloatAsState(
        targetValue = if (checked) 1.0f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "springCheckboxScale"
    )

    val checkAnim by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "springCheckProgress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val uncheckedColor = MaterialTheme.colorScheme.outlineVariant
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .size(size)
            .scale(scaleAnim)
            .clip(RoundedCornerShape(size * 0.3f))
            .clickable(enabled = enabled) {
                haptics.selection()
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cornerRadius = w * 0.3f

            if (checked) {
                drawRoundRect(
                    color = primaryColor,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                )

                // Animated checkmark stroke
                val path = Path().apply {
                    moveTo(w * 0.26f, h * 0.52f)
                    lineTo(w * 0.44f, h * 0.70f)
                    lineTo(w * 0.76f, h * 0.32f)
                }

                drawPath(
                    path = path,
                    color = onPrimaryColor,
                    style = Stroke(
                        width = w * 0.12f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            } else {
                drawRoundRect(
                    color = uncheckedColor,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
                    style = Stroke(width = w * 0.08f)
                )
            }
        }
    }
}

/**
 * 2. SLIDING SEGMENTED CONTROL
 * Fluid sliding pill indicator with spring physics and tactile snapping.
 */
@Composable
fun SlidingSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberPimsHaptics()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                val pillScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.0f else 0.96f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "pillScale_$index"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .scale(pillScale)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
                        )
                        .clickable {
                            if (!isSelected) {
                                haptics.snap()
                                onOptionSelected(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.5.sp
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 3. PULSE HEART (Cardiac / Biometric)
 * Continuous double-bump cardiac micro-animation for medical ICE and health metrics.
 */
@Composable
fun PulseHeartIcon(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    tint: Color = MaterialTheme.colorScheme.error
) {
    val infiniteTransition = rememberInfiniteTransition(label = "heartPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                1.0f at 0
                1.22f at 160 using FastOutSlowInEasing // first heartbeat lub
                1.04f at 300
                1.16f at 450 using FastOutSlowInEasing // second heartbeat dub
                1.0f at 650
                1.0f at 1400 // rest interval
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "heartScale"
    )

    Icon(
        imageVector = Icons.Default.Favorite,
        contentDescription = "Pulse Heart",
        tint = tint,
        modifier = modifier
            .size(size)
            .scale(scale)
    )
}

/**
 * 4. WARM TOOLTIP
 * Interactive floating warm pill balloon that springs open on tap/press.
 */
@Composable
fun WarmTooltip(
    text: String,
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    LaunchedEffect(visible) {
        if (visible) {
            delay(2400)
            onDismissRequest()
        }
    }

    Box(modifier = modifier) {
        content()

        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.85f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-38).dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                shadowElevation = 6.dp,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

/**
 * 5. PERSONA OTP INPUT SLOTS
 * Reactbits OTP slots with individual spring-bounce on input and focused glow.
 */
@Composable
fun PersonaOtpInput(
    code: String,
    onCodeChange: (String) -> Unit,
    slotCount: Int = 6,
    modifier: Modifier = Modifier
) {
    val haptics = rememberPimsHaptics()

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Invisible input field catching keyboard input
        BasicTextField(
            value = code,
            onValueChange = {
                val clean = it.filter { ch -> ch.isDigit() }.take(slotCount)
                if (clean.length > code.length) {
                    haptics.selection()
                }
                onCodeChange(clean)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .pointerInput(Unit) {}
        )

        // Visual animated slots
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until slotCount) {
                val char = code.getOrNull(i)?.toString() ?: ""
                val isCurrent = i == code.length
                val isFilled = char.isNotEmpty()

                val slotScale by animateFloatAsState(
                    targetValue = if (isFilled) 1.05f else if (isCurrent) 1.02f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "otpSlot_$i"
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isCurrent) 2.dp else 1.dp,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary
                                else if (isFilled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    shadowElevation = if (isCurrent) 4.dp else 1.dp,
                    modifier = Modifier
                        .size(48.dp, 56.dp)
                        .scale(slotScale)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * 6. SCRUB FIELD
 * Tactile numeric scrubber: slide horizontally to adjust values with micro-haptic ticks.
 */
@Composable
fun ScrubField(
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int = 0,
    max: Int = 100,
    unit: String = "",
    modifier: Modifier = Modifier
) {
    val haptics = rememberPimsHaptics()
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
            .pointerInput(value) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    dragAccumulator += dragAmount.x
                    val stepPx = 18f
                    if (dragAccumulator > stepPx) {
                        val steps = (dragAccumulator / stepPx).toInt()
                        val next = (value + steps).coerceIn(min, max)
                        if (next != value) {
                            haptics.snap()
                            onValueChange(next)
                        }
                        dragAccumulator %= stepPx
                    } else if (dragAccumulator < -stepPx) {
                        val steps = (-dragAccumulator / stepPx).toInt()
                        val next = (value - steps).coerceIn(min, max)
                        if (next != value) {
                            haptics.snap()
                            onValueChange(next)
                        }
                        dragAccumulator %= stepPx
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "◄",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "$value$unit",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "►",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * 7. SPRING DELETE BUTTON
 * 2-step confirmation with smooth slide and red reveal.
 */
@Composable
fun SpringDeleteButton(
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Delete"
) {
    val haptics = rememberPimsHaptics()
    var isConfirming by remember { mutableStateOf(false) }

    LaunchedEffect(isConfirming) {
        if (isConfirming) {
            delay(3200)
            isConfirming = false
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isConfirming) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (isConfirming) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier.tactilePress {
            if (isConfirming) {
                haptics.warning()
                onConfirmDelete()
                isConfirming = false
            } else {
                haptics.selection()
                isConfirming = true
            }
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = if (isConfirming) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isConfirming) "Confirm Delete?" else label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isConfirming) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
