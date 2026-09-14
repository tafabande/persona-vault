package com.pims.vault.presentation.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

class PimsHaptics(
    private val context: Context,
    private val view: View? = null
) {
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val prefs = context.getSharedPreferences("pims_haptics_prefs", Context.MODE_PRIVATE)

    fun isUserHapticsEnabled(): Boolean = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)

    fun setUserHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, enabled).apply()
    }

    private fun isHapticsEnabled(): Boolean {
        if (!isUserHapticsEnabled()) return false
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1
            ) != 0
        } catch (_: Exception) {
            true
        }
    }

    /**
     * Subtle, tactile tick for navigation tabs, toggles, copy actions.
     */
    fun light() {
        selection()
    }

    fun selection() {
        if (!isHapticsEnabled()) return
        try {
            if (view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) == true) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(10L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Clean confirmation for successful saves, unlock, QR generation.
     */
    fun success() {
        if (!isHapticsEnabled()) return
        try {
            if (view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM) == true) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(20L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Distinct feedback for destructive confirmation or authentication failure.
     */
    fun warning() {
        if (!isHapticsEnabled()) return
        try {
            if (view?.performHapticFeedback(HapticFeedbackConstants.REJECT) == true) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Light tactile response for button press.
     */
    fun buttonPress() {
        light()
    }

    /**
     * Playful, tiny tactile tick for avatar interactions and micro-reactions.
     */
    fun avatarInteraction() {
        selection()
    }

    /**
     * Crisp, microscopic tick when snapping between carousel items or option chips.
     */
    fun snap() {
        if (!isHapticsEnabled()) return
        try {
            if (view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) == true) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(6L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Subtle pulse when holding or reaching long-press threshold.
     */
    fun longPressPulse() {
        if (!isHapticsEnabled()) return
        try {
            if (view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) == true) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(25L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Rejection feedback for errors or blocked actions.
     */
    fun error() {
        warning()
    }

    companion object {
        const val KEY_HAPTICS_ENABLED = "in_app_haptics_enabled"
    }
}

@Composable
fun rememberPimsHaptics(): PimsHaptics {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) { PimsHaptics(context, view) }
}
