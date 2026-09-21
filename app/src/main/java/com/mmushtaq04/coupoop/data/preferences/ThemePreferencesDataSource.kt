package com.mmushtaq04.coupoop.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.mmushtaq04.coupoop.domain.model.AppThemeMode

class ThemePreferencesDataSource(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getThemeMode(): AppThemeMode {
        val storedValue = prefs.getInt(KEY_THEME, AppThemeMode.SYSTEM.value)
        return AppThemeMode.fromValue(storedValue)
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putInt(KEY_THEME, mode.value).apply()
    }

    companion object {
        private const val PREFS_NAME = "coupoop_prefs"
        private const val KEY_THEME = "theme_mode"
    }
}
