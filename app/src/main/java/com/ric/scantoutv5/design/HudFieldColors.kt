package com.ric.scantoutv5.design

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import com.ric.scantoutv5.theme.LocalHudTheme

/**
 * T15 (retour terrain Ric 11/10) — palette des champs outlined du thème HUD.
 *
 * Les OutlinedTextField sans `colors` explicites tombent sur les couleurs
 * Material par défaut (thème clair, texte onSurface sombre) → texte quasi
 * invisible sur la surface sombre du dialogue MODIFIER DETAIL. Les écrans
 * (LibraryScreen, ManualEntryScreen) définissaient chacun une copie privée
 * identique : ce helper centralise la config pour les sites restants.
 *
 * Contenu éditable en textPrimary (≥ 4.5:1 requis), labels discrets en
 * textSecondary, bordure/cureur en primary.
 */
@Composable
fun HudFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = LocalHudTheme.current.textPrimary,
    unfocusedTextColor = LocalHudTheme.current.textPrimary,
    focusedContainerColor = LocalHudTheme.current.surfaceHigh,
    unfocusedContainerColor = LocalHudTheme.current.surfaceHigh,
    focusedBorderColor = LocalHudTheme.current.primary,
    unfocusedBorderColor = LocalHudTheme.current.primary.copy(alpha = 0.4f),
    focusedLabelColor = LocalHudTheme.current.primary,
    unfocusedLabelColor = LocalHudTheme.current.textSecondary,
    cursorColor = LocalHudTheme.current.primary,
)