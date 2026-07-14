package com.midnight.assistant.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Every value below is copied verbatim from design.md ("Midnight Intelligence").
 * Do not hand-tune these — if the palette needs to change, change design.md and
 * mirror the edit here so the two stay in sync.
 */
object MidnightColors {
    val surface = Color(0xFF051424)
    val surfaceDim = Color(0xFF051424)
    val surfaceBright = Color(0xFF2C3A4C)
    val surfaceContainerLowest = Color(0xFF010F1F)
    val surfaceContainerLow = Color(0xFF0D1C2D)
    val surfaceContainer = Color(0xFF122131)
    val surfaceContainerHigh = Color(0xFF1C2B3C)
    val surfaceContainerHighest = Color(0xFF273647)

    val onSurface = Color(0xFFD4E4FA)
    val onSurfaceVariant = Color(0xFFC6C6CD)
    val inverseSurface = Color(0xFFD4E4FA)
    val inverseOnSurface = Color(0xFF233143)

    val outline = Color(0xFF909097)
    val outlineVariant = Color(0xFF45464D)

    val surfaceTint = Color(0xFFBEC6E0)
    val primary = Color(0xFFBEC6E0)
    val onPrimary = Color(0xFF283044)
    val primaryContainer = Color(0xFF0F172A)
    val onPrimaryContainer = Color(0xFF798098)
    val inversePrimary = Color(0xFF565E74)

    val secondary = Color(0xFFD2BBFF)          // Electric Violet family — "AI Thinking"
    val onSecondary = Color(0xFF3F008E)
    val secondaryContainer = Color(0xFF6001D1)
    val onSecondaryContainer = Color(0xFFC9AEFF)

    val tertiary = Color(0xFF89CEFF)           // Azure — "Listening" state
    val onTertiary = Color(0xFF00344D)
    val tertiaryContainer = Color(0xFF001A29)
    val onTertiaryContainer = Color(0xFF0089C3)

    val error = Color(0xFFFFB4AB)
    val onError = Color(0xFF690005)
    val errorContainer = Color(0xFF93000A)
    val onErrorContainer = Color(0xFFFFDAD6)

    val primaryFixed = Color(0xFFDAE2FD)
    val primaryFixedDim = Color(0xFFBEC6E0)
    val onPrimaryFixed = Color(0xFF131B2E)
    val onPrimaryFixedVariant = Color(0xFF3F465C)

    val secondaryFixed = Color(0xFFEADDFF)
    val secondaryFixedDim = Color(0xFFD2BBFF)
    val onSecondaryFixed = Color(0xFF25005A)
    val onSecondaryFixedVariant = Color(0xFF5A00C6)

    val tertiaryFixed = Color(0xFFC9E6FF)
    val tertiaryFixedDim = Color(0xFF89CEFF)
    val onTertiaryFixed = Color(0xFF001E2F)
    val onTertiaryFixedVariant = Color(0xFF004C6E)

    val background = Color(0xFF051424)
    val onBackground = Color(0xFFD4E4FA)
    val surfaceVariant = Color(0xFF273647)

    // Reference brand swatches called out in the prose section of design.md
    // (kept for anywhere the "brand" hues, rather than the M3 token hues, are wanted)
    val brandMidnight = Color(0xFF0F172A)
    val brandElectricViolet = Color(0xFF7C3AED)
    val brandAzure = Color(0xFF0EA5E9)
    val brandSlateGray = Color(0xFF94A3B8)

    // Ghost border — 1px stroke, 10-15% white opacity
    val ghostBorder = Color(0x1AFFFFFF)
    val ghostBorderStrong = Color(0x26FFFFFF)
    val glassFillTop = Color(0x26FFFFFF)
    val glassFillBottom = Color(0x0DFFFFFF)
}
