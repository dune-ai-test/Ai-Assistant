package com.midnight.assistant.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** rounded.* from design.md */
object MidnightRadius {
    val sm = 8.dp     // 0.5rem
    val md1 = 16.dp   // DEFAULT 1rem
    val md = 24.dp    // 1.5rem — standard card minimum radius
    val lg = 32.dp    // 2rem — interactive buttons
    val xl = 48.dp    // 3rem
    val full = 9999.dp
}

val MidnightShapes = Shapes(
    extraSmall = RoundedCornerShape(MidnightRadius.sm),
    small = RoundedCornerShape(MidnightRadius.md1),
    medium = RoundedCornerShape(MidnightRadius.md),
    large = RoundedCornerShape(MidnightRadius.lg),
    extraLarge = RoundedCornerShape(MidnightRadius.xl)
)

/** spacing.* from design.md, in Dp */
object MidnightSpacing {
    val unit = 4.dp
    val marginMobile = 20.dp
    val gutter = 16.dp
    val stackSm = 8.dp
    val stackMd = 24.dp
    val stackLg = 48.dp
}
