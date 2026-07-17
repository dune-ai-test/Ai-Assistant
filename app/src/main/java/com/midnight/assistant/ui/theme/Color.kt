package com.midnight.assistant.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * "Solace" — a warm, low-lit, jewel-toned dark palette. Built deliberately away from the
 * cool slate/violet/azure "generic AI app" look: a near-black warm charcoal base, a
 * champagne-gold signature accent instead of electric blue/purple, and jewel-tone states
 * (emerald for listening, dusty wine for thinking, warm ember for errors) so each voice
 * state reads as a distinct, considered color rather than a palette swap of the same hue.
 */
object MidnightColors {
    val surface = Color(0xFF0D0B08)
    val surfaceDim = Color(0xFF0D0B08)
    val surfaceBright = Color(0xFF2E271C)
    val surfaceContainerLowest = Color(0xFF060504)
    val surfaceContainerLow = Color(0xFF17130D)
    val surfaceContainer = Color(0xFF1E1811)
    val surfaceContainerHigh = Color(0xFF272016)
    val surfaceContainerHighest = Color(0xFF31281B)

    val onSurface = Color(0xFFF4ECDD)
    val onSurfaceVariant = Color(0xFFC9BBA3)
    val inverseSurface = Color(0xFFF4ECDD)
    val inverseOnSurface = Color(0xFF272016)

    val outline = Color(0xFF8F8266)
    val outlineVariant = Color(0xFF463C2C)

    // Signature brand accent — warm champagne gold. Used for the idle orb state, primary
    // buttons, and anywhere the app needs to say "this is Solace" rather than "this is AI".
    val surfaceTint = Color(0xFFE7C793)
    val primary = Color(0xFFE7C793)
    val onPrimary = Color(0xFF2C1C08)
    val primaryContainer = Color(0xFF261D0E)
    val onPrimaryContainer = Color(0xFFDBC08D)
    val inversePrimary = Color(0xFF7A5C2E)

    // Dusty wine/rose — "Thinking" state.
    val secondary = Color(0xFFDCA6BC)
    val onSecondary = Color(0xFF44142B)
    val secondaryContainer = Color(0xFF56213F)
    val onSecondaryContainer = Color(0xFFEBC4D6)

    // Deep jade/emerald — "Listening" state and interactive accents.
    val tertiary = Color(0xFF8FCBAE)
    val onTertiary = Color(0xFF07301F)
    val tertiaryContainer = Color(0xFF123B28)
    val onTertiaryContainer = Color(0xFFAEE2C6)

    // Warm ember — errors. A soft coal-fire red rather than a clinical stop-sign red.
    val error = Color(0xFFE8917F)
    val onError = Color(0xFF3B0900)
    val errorContainer = Color(0xFF5E1B08)
    val onErrorContainer = Color(0xFFFBD7CC)

    val primaryFixed = Color(0xFFF4E1BE)
    val primaryFixedDim = Color(0xFFE7C793)
    val onPrimaryFixed = Color(0xFF2C1C08)
    val onPrimaryFixedVariant = Color(0xFF5C471F)

    val secondaryFixed = Color(0xFFF3D3E1)
    val secondaryFixedDim = Color(0xFFDCA6BC)
    val onSecondaryFixed = Color(0xFF44142B)
    val onSecondaryFixedVariant = Color(0xFF7A3A56)

    val tertiaryFixed = Color(0xFFC9EBDA)
    val tertiaryFixedDim = Color(0xFF8FCBAE)
    val onTertiaryFixed = Color(0xFF07301F)
    val onTertiaryFixedVariant = Color(0xFF2F5D46)

    val background = Color(0xFF0D0B08)
    val onBackground = Color(0xFFF4ECDD)
    val surfaceVariant = Color(0xFF31281B)

    // Named brand swatches for the orb's state gradients — distinct, jewel-toned hues.
    val brandCopper = Color(0xFFC97A3D)
    val brandGold = Color(0xFFE7C793)
    val brandEmerald = Color(0xFF3FA37B)
    val brandJade = Color(0xFF8FCBAE)
    val brandWine = Color(0xFF9C4A6C)
    val brandRose = Color(0xFFDCA6BC)
    val brandEmber = Color(0xFFE8917F)

    // Retained for anywhere legacy naming is referenced.
    val brandMidnight = Color(0xFF0D0B08)
    val brandElectricViolet = Color(0xFF9C4A6C)
    val brandAzure = Color(0xFF3FA37B)
    val brandSlateGray = Color(0xFFC9BBA3)

    // "Ghost border" — ultra-thin strokes at low opacity, warm-tinted rather than pure white
    // so they read as candlelit glass rather than cold frosted plastic.
    val ghostBorder = Color(0x1FE7C793)
    val ghostBorderStrong = Color(0x33E7C793)
    val glassFillTop = Color(0x21E7C793)
    val glassFillBottom = Color(0x0DE7C793)
}
