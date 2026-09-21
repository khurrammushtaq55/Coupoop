package com.mmushtaq04.coupoop.domain.repository

import com.mmushtaq04.coupoop.domain.model.AppThemeMode

interface ThemeRepository {
    fun getThemeMode(): AppThemeMode
    fun setThemeMode(mode: AppThemeMode)
}
