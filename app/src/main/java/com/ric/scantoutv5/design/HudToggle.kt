package com.ric.scantoutv5.design

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ric.scantoutv5.theme.LocalHudTheme

/**
 * Toggle HUD custom — remplace le Switch Material3.
 *
 * Box avec bordure néon + curseur carré, même pattern que [HudButton].
 * Le glow du curseur suit [LocalHudTheme.animationsEnabled].
 *
 * @param checked état du toggle.
 * @param onCheckedChange action au clic.
 * @param modifier modificateur.
 */
@Composable
fun HudToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalHudTheme.current
    val trackSize = 52.dp
    val thumbSize = 20.dp
    val thumbOffset = 4.dp

    val glow = if (checked && theme.animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "hudToggleGlow")
        val alpha by transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
            label = "hudToggleGlowAlpha",
        )
        alpha
    } else {
        0.4f
    }

    Box(
        modifier = modifier
            .size(trackSize, 26.dp)
            .shadow(
                elevation = if (checked) 6.dp else 0.dp,
                shape = RoundedCornerShape(4.dp),
                ambientColor = theme.primary.copy(alpha = glow),
                spotColor = theme.primary.copy(alpha = glow),
            )
            .background(
                color = if (checked) theme.surfaceHigh else theme.surface,
                shape = RoundedCornerShape(4.dp),
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(start = if (checked) trackSize - thumbSize - thumbOffset else thumbOffset)
                .size(thumbSize)
                .background(
                    color = if (checked) theme.primary else theme.textSecondary,
                    shape = RoundedCornerShape(2.dp),
                ),
        )
    }
}
