package com.mmushtaq04.coupoop.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.mmushtaq04.coupoop.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val FredokaFont = GoogleFont("Fredoka")

val FredokaFontFamily = FontFamily(
    Font(googleFont = FredokaFont, fontProvider = provider),
    Font(googleFont = FredokaFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = FredokaFont, fontProvider = provider, weight = FontWeight.Bold)
)

// Typography scale aligned with coupoop-ui-spec.md
val SyncTypography = Typography(
    // Brand Wordmark / Login Title
    headlineLarge = TextStyle(
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    // Feed Header / Screen Titles
    headlineMedium = TextStyle(
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    // Screen title (e.g. "Find your poop buddy")
    titleLarge = TextStyle(
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 23.sp,
        lineHeight = 28.sp
    ),
    // Log card name
    titleMedium = TextStyle(
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.ExtraBold, // 800
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Button label
    labelLarge = TextStyle(
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.ExtraBold, // 800
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Section labels
    labelMedium = TextStyle(
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.ExtraBold, // 800
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    // Body / Tagline
    bodyMedium = TextStyle(
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Log card meta
    bodySmall = TextStyle(
        fontFamily = FredokaFontFamily,
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)
