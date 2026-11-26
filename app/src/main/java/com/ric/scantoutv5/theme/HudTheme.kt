package com.ric.scantoutv5.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Thème HUD Scantout — toutes les couleurs et métriques dérivent des préférences
 * utilisateur (DataStore Proto). Construit par [ScantoutTheme] à partir de [HudThemeConfig].
 */
@Immutable
data class HudTheme(
    val primary: Color,
    val accent: Color,
    val style: HudStyle,
    val density: HudDensity,
    val animationsEnabled: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val gridLine: Color,
) {
    val isDark: Boolean get() = true

    companion object {
        /** Thème par défaut : sombre, cyan/magenta, style MMO. */
        val Default = HudTheme(
            primary = NeonPalette.Cyan,
            accent = NeonPalette.Magenta,
            style = HudStyle.HUD_STYLE_MMO,
            density = HudDensity.HUD_DENSITY_NORMAL,
            animationsEnabled = true,
            background = NeonPalette.Background,
            surface = NeonPalette.Surface,
            surfaceHigh = NeonPalette.SurfaceHigh,
            textPrimary = NeonPalette.TextPrimary,
            textSecondary = NeonPalette.TextSecondary,
            gridLine = NeonPalette.GridLine,
        )
    }
}