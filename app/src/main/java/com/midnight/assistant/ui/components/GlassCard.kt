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
 * "Frosted Glass" pane per design.md: no solid fill, a soft top-to-bottom gradient
 * standing in for a blur, and a 1px "Ghost Border" at 10-15% white opacity.
 *
 * True backdrop blur isn't available in classic Compose without extra plugins, so the
 * translucent gradient + border combination is used to approximate the glass surface
 * while staying dependency-free.
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
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(MidnightColors.glassFillTop, MidnightColors.glassFillBottom)
                )
            )
            .background(MidnightColors.surfaceContainer.copy(alpha = 0.35f))
            .border(width = 1.dp, color = MidnightColors.ghostBorder, shape = shape)
            .padding(contentPadding)
    ) {
        content()
    }
}
