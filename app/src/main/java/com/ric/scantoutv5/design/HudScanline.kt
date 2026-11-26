package com.ric.scantoutv5.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ric.scantoutv5.theme.LocalHudTheme

/**
 * Ligne de balayage HUD : défilement vertical d'un dégradé néon.
 * Utilisée comme indicateur d'analyse (ex: OCR, détection d'objets).
 *
 * @param active true = animation active, false = ligne statique.
 */
@Composable
fun HudScanline(
    modifier: Modifier = Modifier,
    active: Boolean = true,
) {
    val theme = LocalHudTheme.current
    val transition = rememberInfiniteTransition(label = "hudScanline")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "hudScanlineProgress",
    )

    Canvas(modifier = modifier.fillMaxWidth().height(3.dp)) {
        val y = if (active) progress * size.height else size.height / 2f
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    theme.primary.copy(alpha = 0.9f),
                    Color.Transparent,
                ),
            ),
            topLeft = Offset(0f, y),
            size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx()),
        )
    }
}