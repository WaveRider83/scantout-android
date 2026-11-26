package com.ric.scantoutv5.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ric.scantoutv5.theme.LocalHudTheme
import kotlin.random.Random

/**
 * Particules HUD : points néon dérivant dans le fond.
 * Effet "vidéo futuriste" du design system.
 *
 * @param count nombre de particules.
 * @param active true = animation active.
 */
@Composable
fun HudParticles(
    modifier: Modifier = Modifier,
    count: Int = 24,
    active: Boolean = true,
) {
    val theme = LocalHudTheme.current
    val transition = rememberInfiniteTransition(label = "hudParticles")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "hudParticlesDrift",
    )

    val particles = remember(count) {
        List(count) {
            Triple(
                Random.nextFloat(),
                Random.nextFloat(),
                Random.nextFloat() * 0.5f + 0.2f,
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { (x, y, speed) ->
            val py = (y + drift * speed) % 1f
            val alpha = if (active) 0.15f + 0.25f * speed else 0.2f
            drawCircle(
                color = if (x > 0.5f) theme.primary.copy(alpha = alpha) else theme.accent.copy(alpha = alpha),
                radius = 1.5.dp.toPx(),
                center = Offset(x * size.width, py * size.height),
            )
        }
    }
}