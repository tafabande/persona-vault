package com.pims.vault.presentation.ui.theme

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("pims_theme_prefs", Context.MODE_PRIVATE)

    init {
        synchronized(lock) {
            if (sharedThemeFlow == null) {
                sharedThemeFlow = MutableStateFlow(loadInitialTheme())
            }
            if (sharedSakuraEffectsFlow == null) {
                sharedSakuraEffectsFlow = MutableStateFlow(prefs.getBoolean(KEY_SAKURA_EFFECTS, true))
            }
        }
    }

    val themeMode: StateFlow<ThemeMode>
        get() = synchronized(lock) {
            sharedThemeFlow ?: MutableStateFlow(loadInitialTheme()).also { sharedThemeFlow = it }
        }.asStateFlow()

    val sakuraEffectsEnabled: StateFlow<Boolean>
        get() = synchronized(lock) {
            sharedSakuraEffectsFlow ?: MutableStateFlow(prefs.getBoolean(KEY_SAKURA_EFFECTS, true)).also {
                sharedSakuraEffectsFlow = it
            }
        }.asStateFlow()

    fun setSakuraEffectsEnabled(enabled: Boolean) {
        synchronized(lock) {
            prefs.edit().putBoolean(KEY_SAKURA_EFFECTS, enabled).apply()
            sharedSakuraEffectsFlow?.value = enabled
        }
    }

    private fun loadInitialTheme(): ThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, ThemeMode.LIGHT.name)
        return try {
            ThemeMode.valueOf(saved ?: ThemeMode.LIGHT.name)
        } catch (_: Exception) {
            ThemeMode.LIGHT
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        synchronized(lock) {
            sharedThemeFlow?.value = mode
        }
        try {
            com.pims.vault.presentation.widget.PersonaQuickActionsWidgetProvider.updateAllWidgets(context)
        } catch (_: Exception) {}
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode_preference"
        private const val KEY_SAKURA_EFFECTS = "sakura_effects_enabled"
        private val lock = Any()
        @Volatile
        private var sharedThemeFlow: MutableStateFlow<ThemeMode>? = null
        @Volatile
        private var sharedSakuraEffectsFlow: MutableStateFlow<Boolean>? = null
    }
}
