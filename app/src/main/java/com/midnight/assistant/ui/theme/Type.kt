package com.midnight.assistant.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Solace pairs a serif display face with a clean sans body — an editorial, boutique-hotel
 * feel rather than the all-sans-everything look most AI apps default to. This ships on
 * system serif/sans-serif so the project builds with zero bundled font assets; drop real
 * licensed files (e.g. "Fraunces" or "Freight Display" for headlines, "Inter" or "Söhne"
 * for body) into res/font/ and swap the FontFamily.* references below for the exact look.
 */
private val DisplaySerif = FontFamily.Serif
private val BodySans = FontFamily.SansSerif
private val LabelSans = FontFamily.SansSerif

val MidnightTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.Normal,
        fontSize = 50.sp,
        lineHeight = 58.sp,
        letterSpacing = (-0.01).em
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.em
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.Normal,
        fontSize = 27.sp,
        lineHeight = 34.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    labelMedium = TextStyle(
        fontFamily = LabelSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.08.em
    )
)
