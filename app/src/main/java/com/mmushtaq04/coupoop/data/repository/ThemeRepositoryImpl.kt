package com.mmushtaq04.coupoop.data.repository

import android.content.Context
import com.mmushtaq04.coupoop.data.preferences.ThemePreferencesDataSource
import com.mmushtaq04.coupoop.domain.model.AppThemeMode
import com.mmushtaq04.coupoop.domain.repository.ThemeRepository

class ThemeRepositoryImpl(context: Context) : ThemeRepository {
    private val preferences = ThemePreferencesDataSource(context.applicationContext)

    override fun getThemeMode(): AppThemeMode = preferences.getThemeMode()

    override fun setThemeMode(mode: AppThemeMode) {
        preferences.setThemeMode(mode)
    }
}
