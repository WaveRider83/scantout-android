package com.ric.scantoutv5.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Configuration du thème HUD, fournie par CompositionLocal. */
@Immutable
data class HudThemeConfig(
    val primary: Color = HudTheme.Default.primary,
    val accent: Color = HudTheme.Default.accent,
    val style: HudStyle = HudStyle.HUD_STYLE_MMO,
    val density: HudDensity = HudDensity.HUD_DENSITY_NORMAL,
    val animationsEnabled: Boolean = true,
    val background: Color = HudTheme.Default.background,
    val surface: Color = HudTheme.Default.surface,
    val surfaceHigh: Color = HudTheme.Default.surfaceHigh,
    val textPrimary: Color = HudTheme.Default.textPrimary,
    val textSecondary: Color = HudTheme.Default.textSecondary,
    val gridLine: Color = HudTheme.Default.gridLine,
)

val LocalHudTheme = staticCompositionLocalOf { HudThemeConfig() }

// V2c (t_26c14be2) — STYLE/DENSITÉ réellement consommés au rendu.
// Avant, style/density étaient stockés dans la config mais jamais lus par les
// composants → "STYLE/DENSITÉ ne fonctionne pas" (t_6eb7f9ce). Ces helpers
// exposent des valeurs dérivées que les composants design consomment.
val HudThemeConfig.densityScale: Float
    get() = when (density) {
        HudDensity.HUD_DENSITY_COMPACT -> 0.85f
        HudDensity.HUD_DENSITY_COMFORTABLE -> 1.15f
        else -> 1f
    }

/** Style MINIMAL : grille de fond masquée (rendu épuré). */
val HudThemeConfig.showGrid: Boolean
    get() = style != HudStyle.HUD_STYLE_MINIMAL

/** Intensité de la lueur néon selon le style (MMO=plein, MINIMAL=faible, RETRO=moyen). */
val HudThemeConfig.glowIntensity: Float
    get() = when (style) {
        HudStyle.HUD_STYLE_MINIMAL -> 0.3f
        HudStyle.HUD_STYLE_RETRO -> 0.7f
        else -> 1f
    }

/** Fournit le thème HUD à toute la composition. */
@Composable
fun ScantoutTheme(config: HudThemeConfig, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalHudTheme provides config, content = content)
}