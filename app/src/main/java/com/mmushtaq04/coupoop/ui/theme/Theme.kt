package com.mmushtaq04.coupoop.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = LightCoral,
    onPrimary = Color.White,
    secondary = LightTeal,
    onSecondary = Color.White,
    tertiary = LightAmber,
    onTertiary = LightCharcoal,
    background = LightCream,
    onBackground = LightCharcoal,
    surface = LightSurface,
    onSurface = LightCharcoal,
    surfaceVariant = LightChipBg,
    onSurfaceVariant = LightMuted,
    outline = LightLine,
    error = Danger,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkCoral,
    onPrimary = DarkCoralDark,
    secondary = DarkTeal,
    onSecondary = Color(0xFF00382F),
    tertiary = DarkAmber,
    onTertiary = Color(0xFF3F2E00),
    background = DarkCream,
    onBackground = DarkCharcoal,
    surface = DarkSurface,
    onSurface = DarkCharcoal,
    surfaceVariant = DarkChipBg,
    onSurfaceVariant = DarkMuted,
    outline = DarkLine,
    error = Danger,
    onError = Color.White
)

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
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SyncTypography,
        shapes = SyncShapes,
        content = content
    )
}
