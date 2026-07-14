package com.midnight.assistant.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.midnight.assistant.ui.theme.MidnightColors
import com.midnight.assistant.viewmodel.OrbState
import kotlin.math.min

/**
 * "AI Orbs & Visualizers": a fluid, animated gradient sphere that reacts to voice
 * frequency, with a soft ambient glow bleeding into the Midnight background.
 */
@Composable
fun AiOrb(
    state: OrbState,
    micLevel: Float,
    modifier: Modifier = Modifier,
    baseSize: androidx.compose.ui.unit.Dp = 220.dp
) {
    val infinite = rememberInfiniteTransition(label = "orb")

    val breathing by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val (coreA, coreB, glow) = when (state) {
        OrbState.IDLE -> Triple(MidnightColors.primary, MidnightColors.brandElectricViolet, MidnightColors.primary)
        OrbState.LISTENING -> Triple(MidnightColors.tertiary, MidnightColors.brandAzure, MidnightColors.tertiary)
        OrbState.THINKING -> Triple(MidnightColors.secondary, MidnightColors.secondaryContainer, MidnightColors.secondary)
        OrbState.SPEAKING -> Triple(MidnightColors.tertiary, MidnightColors.secondary, MidnightColors.tertiary)
        OrbState.ERROR -> Triple(MidnightColors.error, MidnightColors.errorContainer, MidnightColors.error)
    }

    val reactiveScale = breathing + (micLevel.coerceIn(0f, 1f) * 0.12f)

    Canvas(modifier = modifier.size(baseSize)) {
        val diameter = min(size.width, size.height)
        val radius = (diameter / 2f) * reactiveScale
        val center = Offset(size.width / 2f, size.height / 2f)

        // Ambient glow — soft colored blur bleeding outward (drop shadow substitute)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glow.copy(alpha = 0.35f), Color.Transparent),
                center = center,
                radius = radius * 1.9f
            ),
            radius = radius * 1.9f,
            center = center
        )

        // Fluid gradient core, subtly rotated for a "living" feel
        rotate(rotation, pivot = center) {
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(coreA, coreB, coreA),
                ),
                radius = radius,
                center = center
            )
        }

        // Inner highlight to sell the glass/orb material
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                center = Offset(center.x - radius * 0.3f, center.y - radius * 0.35f),
                radius = radius * 0.9f
            ),
            radius = radius * 0.9f,
            center = Offset(center.x - radius * 0.3f, center.y - radius * 0.35f)
        )
    }
}
