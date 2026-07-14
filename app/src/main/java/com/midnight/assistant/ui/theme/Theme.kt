package com.midnight.assistant.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MidnightDarkScheme = darkColorScheme(
    primary = MidnightColors.primary,
    onPrimary = MidnightColors.onPrimary,
    primaryContainer = MidnightColors.primaryContainer,
    onPrimaryContainer = MidnightColors.onPrimaryContainer,
    inversePrimary = MidnightColors.inversePrimary,

    secondary = MidnightColors.secondary,
    onSecondary = MidnightColors.onSecondary,
    secondaryContainer = MidnightColors.secondaryContainer,
    onSecondaryContainer = MidnightColors.onSecondaryContainer,

    tertiary = MidnightColors.tertiary,
    onTertiary = MidnightColors.onTertiary,
    tertiaryContainer = MidnightColors.tertiaryContainer,
    onTertiaryContainer = MidnightColors.onTertiaryContainer,

    background = MidnightColors.background,
    onBackground = MidnightColors.onBackground,

    surface = MidnightColors.surface,
    onSurface = MidnightColors.onSurface,
    surfaceVariant = MidnightColors.surfaceVariant,
    onSurfaceVariant = MidnightColors.onSurfaceVariant,
    surfaceTint = MidnightColors.surfaceTint,

    inverseSurface = MidnightColors.inverseSurface,
    inverseOnSurface = MidnightColors.inverseOnSurface,

    outline = MidnightColors.outline,
    outlineVariant = MidnightColors.outlineVariant,

    error = MidnightColors.error,
    onError = MidnightColors.onError,
    errorContainer = MidnightColors.errorContainer,
    onErrorContainer = MidnightColors.onErrorContainer,
)

/**
 * "Quiet Power" is always dark — the whole design system assumes the Midnight
 * background, so this theme intentionally ignores system light/dark toggles.
 */
@Composable
fun MidnightAssistantTheme(content: @Composable () -> Unit) {
    val colorScheme = MidnightDarkScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = MidnightColors.surface.toArgb()
            window.navigationBarColor = MidnightColors.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MidnightTypography,
        shapes = MidnightShapes,
        content = content
    )
}
