package com.mmushtaq04.coupoop.domain.model

enum class AppThemeMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2);

    companion object {
        fun fromValue(value: Int): AppThemeMode = entries.firstOrNull { it.value == value } ?: SYSTEM
    }
}
