package com.midnight.assistant.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * design.md specifies three families: Hanken Grotesk (display/headline), Inter (body),
 * and Geist (labels/technical data). This project ships without bundled .ttf files so it
 * builds out of the box — drop the real font files into res/font/ and swap the
 * FontFamily.Default references below for FontFamily(Font(R.font.xxx, weight)) to get the
 * exact typeface. Sizes, weights, line-heights and tracking below are taken verbatim from
 * design.md so the rhythm is correct even before the real fonts are wired in.
 */
private val HankenGrotesk = FontFamily.SansSerif
private val Inter = FontFamily.SansSerif
private val Geist = FontFamily.Monospace

val MidnightTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.02).em
    ),
    headlineLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.01).em
    ),
    headlineMedium = TextStyle(
        // headline-lg-mobile from design.md
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    bodyLarge = TextStyle(
        // body-md
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        // body-sm
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.05.em
    )
)
