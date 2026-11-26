package com.ric.scantoutv5

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun CapturesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var captures by remember { mutableStateOf(CapturesStore.list(context)) }
    var selected by remember { mutableStateOf<File?>(null) }

    val current = selected
    if (current != null) {
        CaptureDetail(file = current, onBack = {
            selected = null
            captures = CapturesStore.list(context)
        })
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← Retour") }
            Spacer(Modifier.width(8.dp))
            Text("Historique (${captures.size})", style = MaterialTheme.typography.titleMedium)
        }
        if (captures.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucune capture")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(captures, key = { it.absolutePath }) { file ->
                    val bmp = remember(file.absolutePath) {
                        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = file.name,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable { selected = file }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureDetail(file: File, onBack: () -> Unit) {
    val bmp = remember(file.absolutePath) {
        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Retour") }
            Spacer(Modifier.width(8.dp))
            Text(file.name, style = MaterialTheme.typography.bodyMedium)
        }
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = file.name,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            "OCR à venir (lot V5-2)",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
