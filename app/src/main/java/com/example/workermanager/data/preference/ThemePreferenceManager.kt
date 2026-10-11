package com.example.workermanager.data.preference

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode(val title: String, val subtitle: String) {
    SYSTEM("System Default", "Follow phone's theme settings"),
    LIGHT("Light", "Always use Light mode"),
    DARK("Dark", "Always use Dark mode")
}

class ThemePreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("workcraft_preferences", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(getSavedThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private fun getSavedThemeMode(): ThemeMode {
        if (prefs.contains(KEY_THEME_MODE)) {
            val savedName = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
            return try {
                ThemeMode.valueOf(savedName ?: ThemeMode.SYSTEM.name)
            } catch (e: Exception) {
                ThemeMode.SYSTEM
            }
        }
        // Migration from legacy boolean if present
        if (prefs.contains(KEY_DARK_MODE_LEGACY)) {
            val wasDark = prefs.getBoolean(KEY_DARK_MODE_LEGACY, false)
            val migrated = if (wasDark) ThemeMode.DARK else ThemeMode.SYSTEM
            prefs.edit().putString(KEY_THEME_MODE, migrated.name).apply()
            return migrated
        }
        return ThemeMode.SYSTEM
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_DARK_MODE_LEGACY = "key_dark_mode"
    }
}
