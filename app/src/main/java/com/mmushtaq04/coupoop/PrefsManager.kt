package com.mmushtaq04.coupoop

import android.content.Context
import com.mmushtaq04.coupoop.data.repository.ThemeRepositoryImpl
import com.mmushtaq04.coupoop.domain.model.AppThemeMode

object PrefsManager {
    // Theme modes: 0 = System, 1 = Light, 2 = Dark
    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    private const val PREFS_NAME = "coupoop_prefs"
    private const val KEY_PREMIUM = "premium_ad_free"
    private const val KEY_PREMIUM_PROMPT_AT = "premium_prompt_at"
    private const val PREMIUM_PROMPT_DELAY_MS = 3L * 24L * 60L * 60L * 1000L

    private fun repository(context: Context) = ThemeRepositoryImpl(context.applicationContext)

    fun getThemeMode(context: Context): Int {
        return repository(context).getThemeMode().value
    }

    fun setThemeMode(context: Context, mode: Int) {
        repository(context).setThemeMode(AppThemeMode.fromValue(mode))
    }

    fun isPremium(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PREMIUM, false)
    }

    fun setPremium(context: Context, premium: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PREMIUM, premium).apply()
    }

    fun shouldShowPremiumPrompt(context: Context): Boolean {
        if (isPremium(context)) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val promptAt = prefs.getLong(KEY_PREMIUM_PROMPT_AT, 0L)
        val now = System.currentTimeMillis()
        if (promptAt == 0L) {
            prefs.edit().putLong(KEY_PREMIUM_PROMPT_AT, now).apply()
            return false
        }
        return now - promptAt >= PREMIUM_PROMPT_DELAY_MS
    }

    fun markPremiumPromptShown(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_PREMIUM_PROMPT_AT, System.currentTimeMillis()).apply()
    }
}
