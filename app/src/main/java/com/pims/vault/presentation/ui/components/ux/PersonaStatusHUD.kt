package com.pims.vault.presentation.ui.components.ux

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pims.vault.presentation.ui.theme.LocalPimsTonalSurfaces
import com.pims.vault.presentation.ui.theme.LocalReducedMotion
import com.pims.vault.presentation.ui.theme.PersonaIcons
import com.pims.vault.presentation.ui.util.rememberPimsFeedback
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val toastIds = AtomicLong()

enum class ToastType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

fun personaFeedbackType(message: String): ToastType {
    val text = message.lowercase()
    return when {
        listOf("failed", "failure", "error", "could not", "unable", "unavailable", "denied", "locked")
            .any(text::contains) -> ToastType.ERROR
        listOf("warning", "pending", "waiting", "declined", "wiped").any(text::contains) -> ToastType.WARNING
        message.trimStart().startsWith("✓") ||
            listOf("success", "copied", "saved", "updated", "added").any(text::contains) -> ToastType.SUCCESS
        else -> ToastType.INFO
    }
}

data class PersonaToastMessage(
    val id: Long = toastIds.incrementAndGet(),
    val message: String,
    val type: ToastType = ToastType.INFO,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val durationMs: Long = 3200L,
    val onDismiss: (() -> Unit)? = null
)

@Stable
class PersonaToastController {
    var currentToast by mutableStateOf<PersonaToastMessage?>(null)
        private set

    fun show(toast: PersonaToastMessage) {
        val previous = currentToast
        currentToast = toast
        if (previous?.id != toast.id) previous?.onDismiss?.invoke()
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
        val previous = currentToast
        currentToast = null
        previous?.onDismiss?.invoke()
    }
}

@Composable
fun rememberPersonaToastController(): PersonaToastController {
    return remember { PersonaToastController() }
}

/** Shared app feedback with theme roles, accessible actions and bounded motion. */
@Composable
fun PersonaToastHost(controller: PersonaToastController, modifier: Modifier = Modifier) {
    val toast = controller.currentToast
    val reduced = LocalReducedMotion.current
    val accessibility = LocalAccessibilityManager.current
    val feedback = rememberPimsFeedback()
    LaunchedEffect(toast?.id) {
        if (toast != null) {
            when (toast.type) {
                ToastType.SUCCESS -> feedback.success()
                ToastType.ERROR -> feedback.error()
                ToastType.WARNING -> feedback.warning()
                ToastType.INFO -> feedback.tap()
            }
            val duration = accessibility?.calculateRecommendedTimeoutMillis(toast.durationMs,
                containsIcons = true, containsText = true, containsControls = true) ?: toast.durationMs
            delay(duration)
            if (controller.currentToast?.id == toast.id) controller.dismiss()
        }
    }
    Box(modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(visible = toast != null,
            enter = if (reduced) EnterTransition.None else fadeIn(tween(160)) + slideInVertically(tween(180)) { -it / 3 },
            exit = if (reduced) ExitTransition.None else fadeOut(tween(120))) {
            if (toast != null) PersonaToastCard(toast, controller::dismiss)
        }
    }
}

@Composable
private fun PersonaToastCard(toast: PersonaToastMessage, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val icon = when (toast.type) {
        ToastType.SUCCESS -> PersonaIcons.Success
        ToastType.ERROR -> PersonaIcons.Danger
        ToastType.WARNING -> PersonaIcons.Warning
        ToastType.INFO -> PersonaIcons.Info
    }
    val iconBackground = when (toast.type) {
        ToastType.ERROR -> colors.errorContainer
        ToastType.WARNING -> colors.tertiaryContainer
        else -> colors.primaryContainer
    }
    val iconContent = when (toast.type) {
        ToastType.ERROR -> colors.onErrorContainer
        ToastType.WARNING -> colors.onTertiaryContainer
        else -> colors.onPrimaryContainer
    }
    val offset = remember(toast.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val threshold = with(LocalDensity.current) { 96.dp.toPx() }
    Surface(color = LocalPimsTonalSurfaces.current.elevated, contentColor = colors.onSurface,
        shape = MaterialTheme.shapes.large, tonalElevation = 2.dp, shadowElevation = 2.dp,
        modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth()
            .offset { IntOffset(offset.value.roundToInt(), 0) }
            .semantics { liveRegion = LiveRegionMode.Polite }
            .pointerInput(toast.id) {
                detectHorizontalDragGestures(onDragEnd = {
                    if (abs(offset.value) > threshold) onDismiss()
                    else scope.launch { offset.animateTo(0f, tween(160)) }
                }, onDragCancel = { scope.launch { offset.animateTo(0f, tween(160)) } },
                    onHorizontalDrag = { change, amount -> change.consume(); scope.launch { offset.snapTo(offset.value + amount) } })
            }) {
        Row(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(32.dp).clip(MaterialTheme.shapes.small).background(iconBackground),
                contentAlignment = Alignment.Center) { Icon(icon, null, tint = iconContent, modifier = Modifier.size(20.dp)) }
            Text(toast.message.trimStart().removePrefix("✓").removePrefix("⚠️").trim(),
                style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp))
            if (toast.actionLabel != null && toast.onAction != null) {
                TextButton(onClick = { toast.onAction.invoke(); onDismiss() }) { Text(toast.actionLabel) }
            }
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Dismiss message", Modifier.size(18.dp)) }
        }
    }
}
