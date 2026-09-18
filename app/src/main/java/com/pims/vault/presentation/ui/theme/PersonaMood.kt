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
    val dividerColor: Color,
    val darkAccentColor: Color,
    val darkSoftAccentColor: Color,
    val darkBackgroundColor: Color,
    val darkSurfaceColor: Color,
    val darkSurfaceVariantColor: Color,
    val darkAvatarBackdropColor: Color,
    val darkDividerColor: Color
) {
    CALM(
        title = "Calm",
        subtitle = "Sage + cream",
        accentColor = Color(0xFF53745C),
        softAccentColor = Color(0xFFDDE6DF),
        backgroundColor = Color(0xFFEFF3EE),
        surfaceColor = Color(0xFFF6F9F5),
        surfaceVariantColor = Color(0xFFE2E8E1),
        avatarBackdropColor = Color(0xFFE2EBE4),
        dividerColor = Color(0xFFD8DFD7),
        darkAccentColor = Color(0xFF7A9E84),
        darkSoftAccentColor = Color(0xFF2A332C),
        darkBackgroundColor = Color(0xFF181C19),
        darkSurfaceColor = Color(0xFF212623),
        darkSurfaceVariantColor = Color(0xFF2B312D),
        darkAvatarBackdropColor = Color(0xFF263029),
        darkDividerColor = Color(0xFF353B37)
    ),
    WARM(
        title = "Warm",
        subtitle = "Terracotta + peach",
        accentColor = Color(0xFFB65F3A),
        softAccentColor = Color(0xFFF3DDD0),
        backgroundColor = Color(0xFFF3EFE6),
        surfaceColor = Color(0xFFFAF7F0),
        surfaceVariantColor = Color(0xFFE9E4D8),
        avatarBackdropColor = Color(0xFFEAD8CD),
        dividerColor = Color(0xFFE0DACB),
        darkAccentColor = Color(0xFFD4845E),
        darkSoftAccentColor = Color(0xFF3D2E26),
        darkBackgroundColor = Color(0xFF1A1917),
        darkSurfaceColor = Color(0xFF232220),
        darkSurfaceVariantColor = Color(0xFF2E2C29),
        darkAvatarBackdropColor = Color(0xFF332A24),
        darkDividerColor = Color(0xFF3A3733)
    ),
    SUNNY(
        title = "Sunny",
        subtitle = "Soft yellow + cream",
        accentColor = Color(0xFF9E7C38),
        softAccentColor = Color(0xFFF5ECCB),
        backgroundColor = Color(0xFFF4F2E9),
        surfaceColor = Color(0xFFFAF8F0),
        surfaceVariantColor = Color(0xFFEAE6DA),
        avatarBackdropColor = Color(0xFFECE3CA),
        dividerColor = Color(0xFFDFDACD),
        darkAccentColor = Color(0xFFC9A44E),
        darkSoftAccentColor = Color(0xFF3A3422),
        darkBackgroundColor = Color(0xFF1A1916),
        darkSurfaceColor = Color(0xFF23221E),
        darkSurfaceVariantColor = Color(0xFF2E2C26),
        darkAvatarBackdropColor = Color(0xFF332E22),
        darkDividerColor = Color(0xFF3A3730)
    ),
    BLUSH(
        title = "Blush",
        subtitle = "Muted rose + warm cream",
        accentColor = Color(0xFFA45C68),
        softAccentColor = Color(0xFFF5E0E4),
        backgroundColor = Color(0xFFF6EFF1),
        surfaceColor = Color(0xFFFAF4F5),
        surfaceVariantColor = Color(0xFFECE1E4),
        avatarBackdropColor = Color(0xFFECD5DA),
        dividerColor = Color(0xFFE3D6DA),
        darkAccentColor = Color(0xFFCC7E8C),
        darkSoftAccentColor = Color(0xFF3A282C),
        darkBackgroundColor = Color(0xFF1A1819),
        darkSurfaceColor = Color(0xFF232122),
        darkSurfaceVariantColor = Color(0xFF2E2B2C),
        darkAvatarBackdropColor = Color(0xFF33282B),
        darkDividerColor = Color(0xFF3A3637)
    ),
    QUIET(
        title = "Quiet",
        subtitle = "Muted teal + sand",
        accentColor = Color(0xFF3F6E74),
        softAccentColor = Color(0xFFD6E7EA),
        backgroundColor = Color(0xFFEDF2F2),
        surfaceColor = Color(0xFFF4F8F8),
        surfaceVariantColor = Color(0xFFE0E7E8),
        avatarBackdropColor = Color(0xFFD9E6E9),
        dividerColor = Color(0xFFD5DDDE),
        darkAccentColor = Color(0xFF6A9EA5),
        darkSoftAccentColor = Color(0xFF243234),
        darkBackgroundColor = Color(0xFF171A1A),
        darkSurfaceColor = Color(0xFF1F2323),
        darkSurfaceVariantColor = Color(0xFF292E2E),
        darkAvatarBackdropColor = Color(0xFF243030),
        darkDividerColor = Color(0xFF343939)
    ),
    EARTH(
        title = "Earth",
        subtitle = "Olive + clay",
        accentColor = Color(0xFF64724D),
        softAccentColor = Color(0xFFE2E9D8),
        backgroundColor = Color(0xFFEFF1EB),
        surfaceColor = Color(0xFFF6F8F3),
        surfaceVariantColor = Color(0xFFE2E5DC),
        avatarBackdropColor = Color(0xFFDEE3D5),
        dividerColor = Color(0xFFD7DBD0),
        darkAccentColor = Color(0xFF8E9E72),
        darkSoftAccentColor = Color(0xFF2E3326),
        darkBackgroundColor = Color(0xFF191A17),
        darkSurfaceColor = Color(0xFF222320),
        darkSurfaceVariantColor = Color(0xFF2C2E29),
        darkAvatarBackdropColor = Color(0xFF2A2E24),
        darkDividerColor = Color(0xFF373933)
    ),
    OCEAN(
        title = "Blue Gradient",
        subtitle = "Deep azure + sky",
        accentColor = Color(0xFF0284C7),
        softAccentColor = Color(0xFFBAE6FD),
        backgroundColor = Color(0xFFEAF3F9),
        surfaceColor = Color(0xFFF3F8FC),
        surfaceVariantColor = Color(0xFFDBE7F0),
        avatarBackdropColor = Color(0xFFBAE6FD),
        dividerColor = Color(0xFFCEDCE7),
        darkAccentColor = Color(0xFF38A3E0),
        darkSoftAccentColor = Color(0xFF1A2E3A),
        darkBackgroundColor = Color(0xFF161A1D),
        darkSurfaceColor = Color(0xFF1E2327),
        darkSurfaceVariantColor = Color(0xFF282E33),
        darkAvatarBackdropColor = Color(0xFF1E2C35),
        darkDividerColor = Color(0xFF333A40)
    ),
    PINK(
        title = "Rose Pink",
        subtitle = "Blush rose + champagne",
        accentColor = Color(0xFFD94680),
        softAccentColor = Color(0xFFFCE7F0),
        backgroundColor = Color(0xFFFAF2F5),
        surfaceColor = Color(0xFFFFF8FA),
        surfaceVariantColor = Color(0xFFF3E4EB),
        avatarBackdropColor = Color(0xFFF7D8E4),
        dividerColor = Color(0xFFEBD7E0),
        darkAccentColor = Color(0xFFF472B6),
        darkSoftAccentColor = Color(0xFF3F1D2C),
        darkBackgroundColor = Color(0xFF191417),
        darkSurfaceColor = Color(0xFF231B20),
        darkSurfaceVariantColor = Color(0xFF2E242B),
        darkAvatarBackdropColor = Color(0xFF3B2531),
        darkDividerColor = Color(0xFF3A2E35)
    ),
    MONOCHROME(
        title = "Monochrome",
        subtitle = "Obsidian + pure silver",
        accentColor = Color(0xFF18181B),
        softAccentColor = Color(0xFFE4E4E7),
        backgroundColor = Color(0xFFF4F4F5),
        surfaceColor = Color(0xFFFAFAFA),
        surfaceVariantColor = Color(0xFFE4E4E7),
        avatarBackdropColor = Color(0xFFD4D4D8),
        dividerColor = Color(0xFFD4D4D8),
        darkAccentColor = Color(0xFFF4F4F5),
        darkSoftAccentColor = Color(0xFF27272A),
        darkBackgroundColor = Color(0xFF09090B),
        darkSurfaceColor = Color(0xFF141417),
        darkSurfaceVariantColor = Color(0xFF202024),
        darkAvatarBackdropColor = Color(0xFF27272A),
        darkDividerColor = Color(0xFF2E2E33)
    ),
    METALLIC(
        title = "Metallic",
        subtitle = "Titanium + platinum silver",
        accentColor = Color(0xFF64748B),
        softAccentColor = Color(0xFFE2E8F0),
        backgroundColor = Color(0xFFF1F5F9),
        surfaceColor = Color(0xFFF8FAFC),
        surfaceVariantColor = Color(0xFFE2E8F0),
        avatarBackdropColor = Color(0xFFCBD5E1),
        dividerColor = Color(0xFFCBD5E1),
        darkAccentColor = Color(0xFF94A3B8),
        darkSoftAccentColor = Color(0xFF1E293B),
        darkBackgroundColor = Color(0xFF0F172A),
        darkSurfaceColor = Color(0xFF1E293B),
        darkSurfaceVariantColor = Color(0xFF334155),
        darkAvatarBackdropColor = Color(0xFF1E293B),
        darkDividerColor = Color(0xFF334155)
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
