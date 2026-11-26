package com.ric.scantoutv5

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private sealed class UpdateState {
    data object Checking : UpdateState()
    data object UpToDate : UpdateState()
    data class Available(val info: UpdateInfo) : UpdateState()
    data class Failed(val message: String) : UpdateState()
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
        }.getOrDefault("?")
    }
    var state by remember { mutableStateOf<UpdateState>(UpdateState.Checking) }
    var progress by remember { mutableStateOf<Float?>(null) }
    var installError by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    suspend fun check() {
        state = UpdateState.Checking
        state = try {
            val info = checkForUpdate(context)
            if (info != null) UpdateState.Available(info) else UpdateState.UpToDate
        } catch (e: Exception) {
            UpdateState.Failed(e.message ?: "erreur réseau")
        }
    }
    LaunchedEffect(Unit) { check() }

    AlertDialog(
        onDismissRequest = { if (progress == null) onDismiss() },
        title = { androidx.compose.material3.Text("Scantout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Version $versionName\n\n© 2026 Cédric Burnotte. Tous droits réservés.\n\nApplication de scan et copie de zones de texte (OCR).\nUsage personnel. Toute reproduction ou distribution sans autorisation écrite de l'auteur est interdite.")
                Spacer(Modifier.height(4.dp))
                when (val s = state) {
                    UpdateState.Checking -> {
                        Text("Recherche de mise à jour…")
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(24.dp))
                    }
                    UpdateState.UpToDate -> Text("✓ Vous avez la dernière version.")
                    is UpdateState.Available -> {
                        Text("Mise à jour disponible : v${s.info.version}")
                        if (progress != null) {
                            LinearProgressIndicator(progress = { progress ?: 0f })
                            Text("Téléchargement… ${((progress ?: 0f) * 100).toInt()} %")
                        }
                        installError?.let { Text(it, color = androidx.compose.ui.graphics.Color(0xFFCF6679)) }
                    }
                    is UpdateState.Failed -> Text("Vérification impossible : ${s.message}")
                }
            }
        },
        confirmButton = {
            val s = state
            when {
                s is UpdateState.Available && progress == null ->
                    Button(onClick = {
                        installError = null
                        progress = 0f
                        scope.launch {
                            val err = downloadAndInstall(context, s.info) { p -> progress = p }
                            progress = null
                            if (err != null) installError = err
                        }
                    }) { Text("Mettre à jour") }
                s is UpdateState.Checking || s is UpdateState.UpToDate || s is UpdateState.Failed ->
                    TextButton(onClick = onDismiss) { Text("Fermer") }
                else -> {}
            }
        },
        dismissButton = {
            if (state is UpdateState.Available && progress == null) {
                TextButton(onClick = onDismiss) { Text("Plus tard") }
            }
        }
    )
}
