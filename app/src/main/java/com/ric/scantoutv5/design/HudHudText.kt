package com.ric.scantoutv5.design

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.ric.scantoutv5.theme.LocalHudTheme
import com.ric.scantoutv5.theme.densityScale

/**
 * Texte HUD : monospace, capitalisé, couleur néon.
 * Composant de base de tout le design system.
 *
 * @param text contenu.
 * @param color couleur (défaut : primaire du thème).
 * @param size taille de police.
 * @param weight graisse.
 * @param modifier modificateur.
 * @param maxLines nombre max de lignes (1 = troncature ellipsis, défaut illimité).
 */
@Composable
fun HudHudText(
    text: String,
    color: Color? = null,
    size: TextUnit = 14.sp,
    weight: FontWeight = FontWeight.SemiBold,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    val theme = LocalHudTheme.current
    Text(
        text = text.uppercase(),
        color = color ?: theme.primary,
        // V2c (t_26c14be2) — DENSITÉ réellement consommée : la taille de police
        // suit densityScale (COMPACT=0.85, NORMAL=1, COMFORTABLE=1.15).
        fontSize = size * theme.densityScale,
        fontWeight = weight,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.5.sp,
        modifier = modifier,
        maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
    )
}