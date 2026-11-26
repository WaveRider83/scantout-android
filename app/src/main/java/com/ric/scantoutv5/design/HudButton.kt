package com.ric.scantoutv5.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ric.scantoutv5.theme.LocalHudTheme
import com.ric.scantoutv5.theme.densityScale

/**
 * Bouton HUD : fond sombre, bordure néon, texte monospace.
 *
 * Implémentation custom (Surface + clickable) : le Button Material3 impose
 * un minHeight interne de 40dp qui empêchait toute réduction réelle.
 * Tailles effectives : normal 36dp, compact 30dp (t_ac090f0b §3).
 *
 * @param text libellé du bouton.
 * @param onClick action au clic.
 * @param modifier modificateur.
 * @param enabled état actif.
 * @param selected état sélectionné (bordure pleine + fond surfaceHigh + glow léger).
 * @param compact mode compact : minHeight 30.dp + padding (10,4).
 * @param contentPadding padding interne (ignoré si compact=true).
 * @param textSize taille du texte (via HudHudText).
 */
@Composable
fun HudButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    compact: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    textSize: TextUnit = 13.sp,
) {
    val theme = LocalHudTheme.current
    val borderColor = when {
        !enabled -> theme.textSecondary.copy(alpha = 0.4f)
        selected -> theme.primary
        else -> theme.primary.copy(alpha = 0.9f)
    }
    val containerColor = when {
        !enabled -> theme.surface.copy(alpha = 0.5f)
        else -> theme.surfaceHigh
    }
    val contentColor = when {
        !enabled -> theme.textSecondary
        else -> theme.primary
    }
    val glowModifier = if (selected && theme.animationsEnabled) {
        modifier.shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(4.dp),
            ambientColor = theme.primary.copy(alpha = 0.35f),
            spotColor = theme.primary.copy(alpha = 0.35f),
        )
    } else {
        modifier
    }
    val effectivePadding = if (compact) {
        PaddingValues(horizontal = 10.dp, vertical = 4.dp)
    } else {
        contentPadding
    }
    // V2c (t_26c14be2) — DENSITÉ réellement consommée : la hauteur minimale du
    // bouton est mise à l'échelle (COMPACT=0.85, NORMAL=1, COMFORTABLE=1.15).
    val minHeight = (if (compact) 30.dp else 36.dp) * theme.densityScale
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = glowModifier
            .defaultMinSize(minHeight = minHeight)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(effectivePadding),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            HudHudText(text = text, color = contentColor, size = textSize * theme.densityScale)
        }
    }
}
