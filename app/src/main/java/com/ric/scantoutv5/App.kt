package com.ric.scantoutv5

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

private enum class Screen { Camera, Captures }

@Composable
fun App() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        var screen by rememberSaveable { mutableStateOf(Screen.Camera) }
        Box(Modifier.fillMaxSize()) {
            when (screen) {
                Screen.Camera -> CameraScreen(onOpenCaptures = { screen = Screen.Captures })
                Screen.Captures -> CapturesScreen(onBack = { screen = Screen.Camera })
            }
        }
    }
}
