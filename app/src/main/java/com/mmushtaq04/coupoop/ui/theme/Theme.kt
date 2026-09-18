package com.mmushtaq04.coupoop.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val SyncLightColors = lightColorScheme(
    primary = SyncLightPrimary,
    onPrimary = SyncLightOnPrimary,
    secondary = SyncLightSecondary,
    onSecondary = SyncLightOnSecondary,
    tertiary = SyncLightTertiary,
    onTertiary = SyncLightOnTertiary,
    background = SyncLightBackground,
    onBackground = SyncLightOnBackground,
    surface = SyncLightSurface,
    onSurface = SyncLightOnSurface
)

private val SyncDarkColors = darkColorScheme(
    primary = SyncDarkPrimary,
    onPrimary = SyncDarkOnPrimary,
    secondary = SyncDarkSecondary,
    onSecondary = SyncDarkOnSecondary,
    tertiary = SyncDarkTertiary,
    onTertiary = SyncDarkOnTertiary,
    background = SyncDarkBackground,
    onBackground = SyncDarkOnBackground,
    surface = SyncDarkSurface,
    onSurface = SyncDarkOnSurface
)

/**
 * App theme: uses Material You dynamic color on Android 12+ (matching the
 * user's wallpaper), and falls back to Sync's own warm coral/teal palette
 * on older devices. Replaces the bare MaterialTheme{} + stock purple/teal
 * that was here before.
 */
@Composable
fun CoupoopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> SyncDarkColors
        else -> SyncLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SyncTypography,
        shapes = SyncShapes,
        content = content
    )
}
