package com.mmushtaq04.coupoop

import android.content.Context
import com.mmushtaq04.coupoop.data.repository.ThemeRepositoryImpl
import com.mmushtaq04.coupoop.domain.model.AppThemeMode

object PrefsManager {
    // Theme modes: 0 = System, 1 = Light, 2 = Dark
    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    private fun repository(context: Context) = ThemeRepositoryImpl(context.applicationContext)

    fun getThemeMode(context: Context): Int {
        return repository(context).getThemeMode().value
    }

    fun setThemeMode(context: Context, mode: Int) {
        repository(context).setThemeMode(AppThemeMode.fromValue(mode))
    }
}
