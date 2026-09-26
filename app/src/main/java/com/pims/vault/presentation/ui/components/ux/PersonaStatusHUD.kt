package com.pims.vault.presentation.ui.components.ux

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.pimsApplePress
import com.pims.vault.presentation.ui.theme.pimsShake
import com.pims.vault.presentation.ui.util.PimsFeedback
import com.pims.vault.presentation.ui.util.rememberPimsFeedback
import kotlinx.coroutines.delay

enum class ToastType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

data class PersonaToastMessage(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val type: ToastType = ToastType.INFO,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val durationMs: Long = 3200L
)

@Stable
class PersonaToastController {
    var currentToast by mutableStateOf<PersonaToastMessage?>(null)
        private set

    fun show(toast: PersonaToastMessage) {
        currentToast = toast
    }

    fun showSuccess(message: String, durationMs: Long = 3000L) {
        show(PersonaToastMessage(message = message, type = ToastType.SUCCESS, durationMs = durationMs))
    }

    fun showError(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null, durationMs: Long = 4200L) {
        show(PersonaToastMessage(
            message = message,
            type = ToastType.ERROR,
            actionLabel = actionLabel,
            onAction = onAction,
            durationMs = durationMs
        ))
    }

    fun showWarning(message: String, durationMs: Long = 3500L) {
        show(PersonaToastMessage(message = message, type = ToastType.WARNING, durationMs = durationMs))
    }

    fun showInfo(message: String, durationMs: Long = 2800L) {
        show(PersonaToastMessage(message = message, type = ToastType.INFO, durationMs = durationMs))
    }

    fun dismiss() {
        currentToast = null
    }
}

@Composable
fun rememberPersonaToastController(): PersonaToastController {
    return remember { PersonaToastController() }
}

/**
 * Apple-grade Dynamic Floating Status HUD:
 * Positioned cleanly near the top/center of the screen like Dynamic Island.
 * Accompanied by tactile haptic feedback, sound cues, and bounce/shake micro-animations.
 */
@Composable
fun PersonaToastHost(
    controller: PersonaToastController,
    modifier: Modifier = Modifier
) {
    val toast = controller.currentToast
    val feedback = rememberPimsFeedback()
    val isDark = isSystemInDarkTheme()

    // Sound and haptic cues triggered on each new toast
    LaunchedEffect(toast?.id) {
        if (toast != null) {
            when (toast.type) {
                ToastType.SUCCESS -> feedback.success()
                ToastType.ERROR -> feedback.error()
                ToastType.WARNING -> feedback.warning()
                ToastType.INFO -> feedback.tap()
            }
            delay(toast.durationMs)
            if (controller.currentToast?.id == toast.id) {
                controller.dismiss()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 48.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = toast != null,
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                initialOffsetY = { -it }
            ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
            exit = slideOutVertically(
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                targetOffsetY = { -it }
            ) + fadeOut()
        ) {
            if (toast != null) {
                PersonaToastPill(
                    toast = toast,
                    isDark = isDark,
                    onDismiss = { controller.dismiss() }
                )
            }
        }
    }
}

@Composable
private fun PersonaToastPill(
    toast: PersonaToastMessage,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val haptics = rememberPimsHaptics()
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    val (accentColor, icon: ImageVector) = when (toast.type) {
        ToastType.SUCCESS -> colorScheme.primary to Icons.Default.CheckCircle
        ToastType.ERROR -> colorScheme.error to Icons.Default.Error
        ToastType.WARNING -> colorScheme.tertiary to Icons.Default.Warning
        ToastType.INFO -> colorScheme.primary to Icons.Default.Info
    }

    val iconBounce = remember { Animatable(0.7f) }
    LaunchedEffect(toast.id) {
        iconBounce.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
        )
    }

    // Horizontal swipe-to-dismiss offset with spring physics
    val offsetX = remember { Animatable(0f) }

    val pillBg = colorScheme.surface.copy(alpha = 0.97f)
    val pillBorder = colorScheme.outlineVariant.copy(alpha = 0.8f)
    val textColor = colorScheme.onSurface

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = pillBg,
        border = BorderStroke(1.dp, pillBorder),
        shadowElevation = 10.dp,
        modifier = Modifier
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .widthIn(min = 180.dp, max = 380.dp)
            .heightIn(min = 44.dp)
            .pimsShake(isShaking = toast.type == ToastType.ERROR)
            .pointerInput(toast.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (abs(offsetX.value) > 130f) {
                                haptics.selection()
                                offsetX.animateTo(
                                    targetValue = if (offsetX.value > 0) 1000f else -1000f,
                                    animationSpec = tween(150)
                                )
                                onDismiss()
                            } else {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
                                )
                            }
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount)
                        }
                    }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Accent icon with spring bounce
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier
                        .size(16.dp)
                        .scale(iconBounce.value)
                )
            }

            // Message text
            Text(
                text = toast.message,
                color = textColor,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            // Optional inline action button
            if (toast.actionLabel != null && toast.onAction != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.18f),
                    modifier = Modifier.pimsApplePress {
                        toast.onAction.invoke()
                        onDismiss()
                    }
                ) {
                    Text(
                        text = toast.actionLabel,
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
