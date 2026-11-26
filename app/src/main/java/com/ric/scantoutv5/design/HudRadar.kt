package com.ric.scantoutv5.design

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ric.scantoutv5.theme.LocalHudTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * Radar HUD : anneaux concentriques + balayage rotatif + blips.
 * Utilisé comme indicateur de scan (caméra, Wi-Fi, NFC).
 *
 * @param size taille du radar.
 * @param blips positions normalisées (0..1) des cibles détectées.
 * @param scanning true = balayage animé actif.
 */
@Composable
fun HudRadar(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    blips: List<Offset> = emptyList(),
    scanning: Boolean = true,
) {
    val theme = LocalHudTheme.current
    val transition = rememberInfiniteTransition(label = "hudRadar")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "hudRadarSweep",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "hudRadarPulse",
    )

    Canvas(modifier = modifier.size(size)) {
        val canvasSize = this.size
        val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
        val radius = canvasSize.minDimension / 2f
        val ringColor = theme.primary.copy(alpha = 0.5f)
        // Anneaux
        for (i in 1..3) {
            drawCircle(
                color = ringColor,
                radius = radius * i / 3f,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        // Croix
        drawLine(ringColor, Offset(center.x, 0f), Offset(center.x, canvasSize.height), 1.dp.toPx())
        drawLine(ringColor, Offset(0f, center.y), Offset(canvasSize.width, center.y), 1.dp.toPx())
        // Balayage
        if (scanning) {
            val angle = Math.toRadians(sweep.toDouble())
            val sweepEnd = Offset(
                center.x + radius * cos(angle).toFloat(),
                center.y + radius * sin(angle).toFloat(),
            )
            drawLine(
                color = theme.primary.copy(alpha = pulse),
                start = center,
                end = sweepEnd,
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        // Blips
        blips.forEach { blip ->
            val pos = Offset(
                center.x + (blip.x - 0.5f) * 2f * radius,
                center.y + (blip.y - 0.5f) * 2f * radius,
            )
            drawCircle(
                color = theme.accent,
                radius = 3.dp.toPx(),
                center = pos,
            )
        }
    }
}
