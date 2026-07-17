package com.midnight.assistant.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * A slightly more restrained radius scale than a typical "friendly bubbly AI app" — softer
 * than sharp corners, but tighter and more tailored, in keeping with Solace's boutique
 * rather than playful feel.
 */
object MidnightRadius {
    val sm = 6.dp
    val md1 = 14.dp
    val md = 18.dp     // standard card radius
    val lg = 26.dp     // pill-leaning interactive buttons
    val xl = 40.dp
    val full = 9999.dp
}

val MidnightShapes = Shapes(
    extraSmall = RoundedCornerShape(MidnightRadius.sm),
    small = RoundedCornerShape(MidnightRadius.md1),
    medium = RoundedCornerShape(MidnightRadius.md),
    large = RoundedCornerShape(MidnightRadius.lg),
    extraLarge = RoundedCornerShape(MidnightRadius.xl)
)

/** Spacing scale — unchanged rhythm, still 4px-based. */
object MidnightSpacing {
    val unit = 4.dp
    val marginMobile = 20.dp
    val gutter = 16.dp
    val stackSm = 8.dp
    val stackMd = 24.dp
    val stackLg = 48.dp
}
