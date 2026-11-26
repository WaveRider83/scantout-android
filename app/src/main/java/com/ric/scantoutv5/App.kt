package com.ric.scantoutv5

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ric.scantoutv5.theme.NeonPalette
import com.ric.scantoutv5.theme.ScantoutTheme
import com.ric.scantoutv5.theme.HudThemeConfig

private enum class Screen { Camera, Captures }

@Composable
fun App() {
    ScantoutTheme(HudThemeConfig()) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = NeonPalette.Cyan,
                secondary = NeonPalette.Magenta,
                background = NeonPalette.Background,
                surface = NeonPalette.Surface,
                onBackground = NeonPalette.TextPrimary,
                onSurface = NeonPalette.TextPrimary,
            )
        ) {
        var screen by rememberSaveable { mutableStateOf(Screen.Camera) }
        Box(Modifier.fillMaxSize()) {
            when (screen) {
                Screen.Camera -> CameraScreen(onOpenCaptures = { screen = Screen.Captures })
                Screen.Captures -> CapturesScreen(onBack = { screen = Screen.Camera })
            }
        }
        }
    }
}
