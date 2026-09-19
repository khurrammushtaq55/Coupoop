package com.mmushtaq04.coupoop.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Typography scale aligned with coupoop-ui-spec.md
// Note: Using default FontFamily as custom assets aren't bundled yet.
val SyncTypography = Typography(
    // Brand Wordmark / Login Title
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    // Feed Header / Screen Titles
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    // Screen title (e.g. "Find your poop buddy")
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 23.sp,
        lineHeight = 28.sp
    ),
    // Log card name
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold, // 800
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Button label
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold, // 800
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Section labels
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold, // 800
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    // Body / Tagline
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Log card meta
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)
