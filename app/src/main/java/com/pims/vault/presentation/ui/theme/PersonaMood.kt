package com.pims.vault.presentation.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persona Mood: Curated, calm aesthetic moods.
 * Muted, sophisticated tones — never neon, never generic blue/purple SaaS gradients.
 * Influences: accent color, avatar background, selected wallpaper, small illustration accents, and interaction details.
 */
enum class PersonaMood(
    val title: String,
    val subtitle: String,
    val accentColor: Color,
    val softAccentColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val surfaceVariantColor: Color,
    val avatarBackdropColor: Color,
    val dividerColor: Color
) {
    CALM(
        title = "Calm",
        subtitle = "Sage + cream",
        accentColor = Color(0xFF53745C),
        softAccentColor = Color(0xFFDDE6DF),
        backgroundColor = Color(0xFFF6F8F5),
        surfaceColor = Color(0xFFFCFEFB),
        surfaceVariantColor = Color(0xFFE9EFEA),
        avatarBackdropColor = Color(0xFFE2EBE4),
        dividerColor = Color(0xFFE0E6E1)
    ),
    WARM(
        title = "Warm",
        subtitle = "Terracotta + peach",
        accentColor = Color(0xFFB65F3A),
        softAccentColor = Color(0xFFF3DDD0),
        backgroundColor = Color(0xFFF7F5F0),
        surfaceColor = Color(0xFFFFFDF8),
        surfaceVariantColor = Color(0xFFEFECE5),
        avatarBackdropColor = Color(0xFFEAD8CD),
        dividerColor = Color(0xFFE5E0D6)
    ),
    SUNNY(
        title = "Sunny",
        subtitle = "Soft yellow + cream",
        accentColor = Color(0xFF9E7C38),
        softAccentColor = Color(0xFFF5ECCB),
        backgroundColor = Color(0xFFF8F7F2),
        surfaceColor = Color(0xFFFFFDF9),
        surfaceVariantColor = Color(0xFFEFECE4),
        avatarBackdropColor = Color(0xFFECE3CA),
        dividerColor = Color(0xFFE7E3D8)
    ),
    BLUSH(
        title = "Blush",
        subtitle = "Muted rose + warm cream",
        accentColor = Color(0xFFA45C68),
        softAccentColor = Color(0xFFF5E0E4),
        backgroundColor = Color(0xFFFAF6F6),
        surfaceColor = Color(0xFFFFFDFC),
        surfaceVariantColor = Color(0xFFF2E9EC),
        avatarBackdropColor = Color(0xFFECD5DA),
        dividerColor = Color(0xFFE8DFE2)
    ),
    QUIET(
        title = "Quiet",
        subtitle = "Muted teal + sand",
        accentColor = Color(0xFF3F6E74),
        softAccentColor = Color(0xFFD6E7EA),
        backgroundColor = Color(0xFFF4F7F7),
        surfaceColor = Color(0xFFFBFEFE),
        surfaceVariantColor = Color(0xFFE6EEF0),
        avatarBackdropColor = Color(0xFFD9E6E9),
        dividerColor = Color(0xFFDCE4E6)
    ),
    EARTH(
        title = "Earth",
        subtitle = "Olive + clay",
        accentColor = Color(0xFF64724D),
        softAccentColor = Color(0xFFE2E9D8),
        backgroundColor = Color(0xFFF5F6F2),
        surfaceColor = Color(0xFFFDFEFB),
        surfaceVariantColor = Color(0xFFEBECE5),
        avatarBackdropColor = Color(0xFFDEE3D5),
        dividerColor = Color(0xFFDFE1D8)
    ),
    OCEAN(
        title = "Blue Gradient",
        subtitle = "Deep azure + sky",
        accentColor = Color(0xFF0284C7),
        softAccentColor = Color(0xFFBAE6FD),
        backgroundColor = Color(0xFFF0F9FF),
        surfaceColor = Color(0xFFF8FAFC),
        surfaceVariantColor = Color(0xFFE0F2FE),
        avatarBackdropColor = Color(0xFFBAE6FD),
        dividerColor = Color(0xFFE2E8F0)
    )
}

@Singleton
class PersonaMoodManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("persona_mood_prefs", Context.MODE_PRIVATE)

    init {
        synchronized(lock) {
            if (sharedMoodFlow == null) {
                sharedMoodFlow = MutableStateFlow(loadInitialMood())
            }
        }
    }

    val currentMood: StateFlow<PersonaMood>
        get() = synchronized(lock) {
            sharedMoodFlow ?: MutableStateFlow(loadInitialMood()).also { sharedMoodFlow = it }
        }.asStateFlow()

    private fun loadInitialMood(): PersonaMood {
        val saved = prefs.getString(KEY_MOOD, PersonaMood.WARM.name)
        return try {
            PersonaMood.valueOf(saved ?: PersonaMood.WARM.name)
        } catch (_: Exception) {
            PersonaMood.WARM
        }
    }

    fun setMood(mood: PersonaMood) {
        prefs.edit().putString(KEY_MOOD, mood.name).apply()
        synchronized(lock) {
            sharedMoodFlow?.value = mood
        }
    }

    companion object {
        private const val KEY_MOOD = "persona_active_mood"
        private val lock = Any()
        @Volatile
        private var sharedMoodFlow: MutableStateFlow<PersonaMood>? = null
    }
}
