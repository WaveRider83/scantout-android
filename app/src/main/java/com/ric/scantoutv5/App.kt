package com.ric.scantoutv5

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.mutableStateOf as ms
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.activity.compose.BackHandler
import com.ric.scantoutv5.theme.NeonPalette
import com.ric.scantoutv5.theme.ScantoutTheme
import com.ric.scantoutv5.theme.HudThemeConfig

private enum class Screen { Camera, Captures }

@Composable
fun App() {
    val context = LocalContext.current
    val dark = Settings.darkTheme(context)
    val scheme = if (dark) darkColorScheme(
        primary = NeonPalette.Cyan,
        secondary = NeonPalette.Magenta,
        background = NeonPalette.Background,
        surface = NeonPalette.Surface,
        onBackground = NeonPalette.TextPrimary,
        onSurface = NeonPalette.TextPrimary,
    ) else lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF00838F),
        secondary = androidx.compose.ui.graphics.Color(0xFFC000D0),
        background = androidx.compose.ui.graphics.Color(0xFFF5F7FA),
        surface = androidx.compose.ui.graphics.Color.White,
        onBackground = androidx.compose.ui.graphics.Color(0xFF1A2430),
        onSurface = androidx.compose.ui.graphics.Color(0xFF1A2430),
    )
    ScantoutTheme(HudThemeConfig()) {
        MaterialTheme(colorScheme = scheme) {
        androidx.compose.material3.Surface(color = scheme.background, contentColor = scheme.onBackground) {
        var screen by rememberSaveable { mutableStateOf(Screen.Camera) }
        BackHandler(enabled = screen != Screen.Camera) { screen = Screen.Camera }
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            when (screen) {
                Screen.Camera -> CameraScreen(onOpenCaptures = { screen = Screen.Captures })
                Screen.Captures -> CapturesScreen(onBack = { screen = Screen.Camera })
            }
        }
        }
        }
    }
}
