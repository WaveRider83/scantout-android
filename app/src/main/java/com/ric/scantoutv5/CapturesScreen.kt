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
import androidx.compose.foundation.Canvas
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                        CapturesStore.decodeBitmap(file)?.asImageBitmap()
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
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val bitmap = remember(file.absolutePath) { CapturesStore.decodeBitmap(file) }
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }

    var zones by remember { mutableStateOf<List<TextZone>>(emptyList()) }
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    var loading by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf("") }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    fun runOcr() {
        val bmp = bitmap ?: return
        loading = true
        scope.launch {
            val result = withContext(Dispatchers.Default) { OcrEngine.recognize(bmp) }
            zones = result
            selectedIds = result.map { it.id }.toSet()
            editedText = result.joinToString("\n") { it.text }
            loading = false
        }
    }

    LaunchedEffect(file.absolutePath) { runOcr() }

    fun syncEditedFromSelection() {
        editedText = zones.filter { it.id in selectedIds }.joinToString("\n") { it.text }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Retour") }
            Spacer(Modifier.width(8.dp))
            Text(file.name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }

        if (bitmap != null && imageBitmap != null) {
            val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspect)
                    .clipToBounds()
                    .onGloballyPositioned { viewSize = it.size }
                    .pointerInput(zones, viewSize) {
                        detectTapGestures { offset ->
                            if (viewSize.width == 0) return@detectTapGestures
                            val sx = bitmap.width.toFloat() / viewSize.width
                            val sy = bitmap.height.toFloat() / viewSize.height
                            val ix = offset.x * sx
                            val iy = offset.y * sy
                            val hit = zones.lastOrNull { z ->
                                ix >= z.box.left && ix <= z.box.right && iy >= z.box.top && iy <= z.box.bottom
                            }
                            if (hit != null) {
                                selectedIds = if (hit.id in selectedIds) selectedIds - hit.id else selectedIds + hit.id
                                syncEditedFromSelection()
                            }
                        }
                    }
            ) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = file.name,
                    modifier = Modifier.fillMaxSize()
                )
                if (viewSize != IntSize.Zero) {
                    Canvas(Modifier.fillMaxSize()) {
                        val sx = size.width / bitmap.width
                        val sy = size.height / bitmap.height
                        zones.forEach { z ->
                            val sel = z.id in selectedIds
                            drawRect(
                                color = if (sel) Color(0x66FFEB3B) else Color(0x33000000),
                                topLeft = Offset(z.box.left * sx, z.box.top * sy),
                                size = Size(z.box.width() * sx, z.box.height() * sy)
                            )
                            drawRect(
                                color = if (sel) Color(0xFFFFEB3B) else Color(0xFF80DEEA),
                                topLeft = Offset(z.box.left * sx, z.box.top * sy),
                                size = Size(z.box.width() * sx, z.box.height() * sy),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = if (sel) 5f else 2f)
                            )
                        }
                    }
                }
            }
        }

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { runOcr() }, enabled = !loading) { Text("Re-analyser") }
            OutlinedButton(onClick = {
                selectedIds = zones.map { it.id }.toSet()
                syncEditedFromSelection()
            }) { Text("Tout") }
            OutlinedButton(onClick = {
                selectedIds = emptySet()
                editedText = ""
            }) { Text("Aucun") }
        }

        OutlinedTextField(
            value = editedText,
            onValueChange = { editedText = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            minLines = 3,
            label = { Text("Texte (éditable)") }
        )

        Button(
            onClick = { clipboard.setText(AnnotatedString(editedText)) },
            enabled = editedText.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) { Text("Copier") }
    }
}
