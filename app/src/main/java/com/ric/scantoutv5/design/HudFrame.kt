package com.ric.scantoutv5.design

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ric.scantoutv5.theme.LocalHudTheme
import com.ric.scantoutv5.theme.glowIntensity
import com.ric.scantoutv5.theme.showGrid

/**
 * Cadre HUD futuriste : coins en crochets + grille de fond + lueur néon.
 * Utilisé comme conteneur de chaque écran de l'app.
 *
 * @param title titre affiché dans le bandeau supérieur du cadre.
 * @param content contenu de l'écran.
 */
@Composable
fun HudFrame(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val theme = LocalHudTheme.current
    val corner = 18.dp
    // V2c (t_26c14be2) — STYLE réellement consommé : l'intensité de la lueur
    // néon suit glowIntensity (MINIMAL=faible, RETRO=moyen, MMO=plein).
    val glow = if (theme.animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "hudFrameGlow")
        val alpha by transition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
            label = "hudFrameGlowAlpha",
        )
        alpha * theme.glowIntensity
    } else {
        0.8f * theme.glowIntensity
    }

    Box(modifier = modifier.fillMaxSize()) {
        // V2c — grille de fond masquée en style MINIMAL (showGrid=false).
        if (theme.showGrid) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val grid = 32.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(theme.gridLine, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += grid
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(theme.gridLine, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += grid
                }
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = corner.toPx()
            val stroke = 2.dp.toPx()
            val glowColor = theme.primary.copy(alpha = glow)
            // Coin haut-gauche
            drawLine(glowColor, Offset(0f, c), Offset(0f, 0f), stroke, StrokeCap.Round)
            drawLine(glowColor, Offset(0f, 0f), Offset(c, 0f), stroke, StrokeCap.Round)
            // Coin haut-droit
            drawLine(glowColor, Offset(size.width - c, 0f), Offset(size.width, 0f), stroke, StrokeCap.Round)
            drawLine(glowColor, Offset(size.width, 0f), Offset(size.width, c), stroke, StrokeCap.Round)
            // Coin bas-droit
            drawLine(glowColor, Offset(size.width, size.height - c), Offset(size.width, size.height), stroke, StrokeCap.Round)
            drawLine(glowColor, Offset(size.width, size.height), Offset(size.width - c, size.height), stroke, StrokeCap.Round)
            // Coin bas-gauche
            drawLine(glowColor, Offset(c, size.height), Offset(0f, size.height), stroke, StrokeCap.Round)
            drawLine(glowColor, Offset(0f, size.height), Offset(0f, size.height - c), stroke, StrokeCap.Round)
        }
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Column(modifier = Modifier.fillMaxSize()) {
                HudHudText(
                    text = title,
                    color = theme.primary,
                    size = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
        }
    }
}
