package com.ric.scantoutv5

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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
        var updateInfo by androidx.compose.runtime.remember { mutableStateOf<UpdateInfo?>(null) }
        var updateProgress by androidx.compose.runtime.remember { mutableStateOf<Float?>(null) }
        var updateError by androidx.compose.runtime.remember { mutableStateOf<String?>(null) }
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        LaunchedEffect(Unit) {
            updateInfo = checkForUpdate(context)
        }
        updateInfo?.let { info ->
            AlertDialog(
                onDismissRequest = { if (updateProgress == null) updateInfo = null },
                title = { Text("Mise à jour v${info.version} disponible") },
                text = {
                    Column {
                        Text("Une nouvelle version de Scantout est disponible.")
                        updateProgress?.let { p ->
                            androidx.compose.foundation.layout.Spacer(Modifier.padding(4.dp))
                            LinearProgressIndicator(progress = { p }, modifier = Modifier)
                        }
                        updateError?.let { Text(it, color = androidx.compose.ui.graphics.Color(0xFFCF6679)) }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = updateProgress == null,
                        onClick = {
                            updateError = null
                            updateProgress = 0f
                            scope.launch {
                                val err = downloadAndInstall(context, info) { p -> updateProgress = p }
                                updateProgress = null
                                if (err != null) updateError = err else updateInfo = null
                            }
                        }
                    ) { Text("Mettre à jour") }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { updateInfo = null }, enabled = updateProgress == null) { Text("Plus tard") }
                }
            )
        }
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
