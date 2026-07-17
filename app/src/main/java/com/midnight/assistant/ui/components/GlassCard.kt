package com.midnight.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.midnight.assistant.ui.theme.MidnightColors
import com.midnight.assistant.ui.theme.MidnightRadius

/**
 * Solace's "candlelit glass" surface: a warm-tinted translucent panel with a fine gold-tinted
 * hairline border, standing in for backdrop blur (not available in classic Compose without
 * extra plugins) so the surface still reads as glass catching warm light rather than a flat
 * card.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = MidnightRadius.md,
    contentPadding: androidx.compose.ui.unit.Dp = 24.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MidnightColors.surfaceContainer.copy(alpha = 0.55f))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(MidnightColors.glassFillTop, MidnightColors.glassFillBottom)
                )
            )
            .border(width = 1.dp, color = MidnightColors.ghostBorder, shape = shape)
            .padding(contentPadding)
    ) {
        content()
    }
}
