package com.pims.vault.presentation.ui.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PimsSoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val prefs = context.getSharedPreferences("pims_sound_prefs", Context.MODE_PRIVATE)

    private val _isSoundEnabled = MutableStateFlow(prefs.getBoolean(KEY_SOUND_ENABLED, true))
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 35) // Gentle volume
    } catch (_: Exception) {
        null
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
        _isSoundEnabled.value = enabled
    }

    /**
     * Subtle, tactile navigation tick (used sparingly for major tab switches).
     */
    fun navigation() {
        if (!_isSoundEnabled.value) return
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.25f)
        } catch (_: Exception) {}
    }

    /**
     * Gentle confirmation for saves, additions, or successful actions.
     */
    fun success() {
        if (!_isSoundEnabled.value) return
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.4f)
        } catch (_: Exception) {}
    }

    /**
     * Distinct sound for saving/committing records.
     */
    fun save() {
        if (!_isSoundEnabled.value) return
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.35f)
        } catch (_: Exception) {}
    }

    /**
     * Sound cue for deletion confirmation.
     */
    fun delete() {
        if (!_isSoundEnabled.value) return
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE, 0.4f)
        } catch (_: Exception) {}
    }

    /**
     * Sound cue for share link/QR generation.
     */
    fun share() {
        if (!_isSoundEnabled.value) return
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.35f)
        } catch (_: Exception) {}
    }

    /**
     * Sound cue for receiving/viewing a notification.
     */
    fun notification() {
        if (!_isSoundEnabled.value) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
        } catch (_: Exception) {}
    }

    /**
     * Sound cue for authentication error or blocked action.
     */
    fun error() {
        if (!_isSoundEnabled.value) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 90)
        } catch (_: Exception) {}
    }

    /**
     * Sound cue for successful biometric or vault unlock.
     */
    fun unlock() {
        if (!_isSoundEnabled.value) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 80)
        } catch (_: Exception) {}
    }

    companion object {
        private const val KEY_SOUND_ENABLED = "in_app_sound_effects_enabled"
    }
}

@Composable
fun rememberPimsSoundManager(): PimsSoundManager {
    val context = LocalContext.current
    return remember(context) { PimsSoundManager(context.applicationContext) }
}
