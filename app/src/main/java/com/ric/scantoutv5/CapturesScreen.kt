package com.ric.scantoutv5

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext as LCtx
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class ViewMode { GRID, LIST, DETAIL }

private fun fmtDate(f: File): String =
    java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(f.lastModified()))

private fun fmtSize(f: File): String {
    val kb = f.length() / 1024.0
    return if (kb < 1024) "%.0f Ko".format(kb) else "%.1f Mo".format(kb / 1024.0)
}

@Composable
fun CapturesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var captures by remember { mutableStateOf(CapturesStore.list(context)) }
    var selected by remember { mutableStateOf<File?>(null) }
    var viewMode by remember { mutableStateOf(ViewMode.GRID) }
    var selectionMode by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf(setOf<String>()) }
    var confirmDelete by remember { mutableStateOf(false) }

    BackHandler(enabled = selected != null || selectionMode) {
        if (selected != null) {
            selected = null
            captures = CapturesStore.list(context)
        } else {
            selectionMode = false
            checked = emptySet()
        }
    }

    fun toggleCheck(f: File) {
        checked = if (f.absolutePath in checked) checked - f.absolutePath else checked + f.absolutePath
    }

    fun shareFiles(files: List<File>) {
        if (files.isEmpty()) return
        val uris = ArrayList(files.map {
            FileProvider.getUriForFile(context, "com.ric.scantoutv5.fileprovider", it)
        })
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/jpeg"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Partager ${files.size} image(s)"))
    }

    val current = selected
    if (current != null) {
        CaptureDetail(file = current, onBack = {
            selected = null
            captures = CapturesStore.list(context)
        })
        return
    }

    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        // Barre haute : retour + titre + modes d'affichage + sélection
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← Retour") }
            Text(
                if (selectionMode) "${checked.size} sélectionnée(s)" else "Historique (${captures.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (!selectionMode) {
                IconButton(onClick = { viewMode = ViewMode.GRID }) {
                    Icon(Icons.Default.GridView, contentDescription = "Vignettes",
                        tint = if (viewMode == ViewMode.GRID) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { viewMode = ViewMode.LIST }) {
                    Icon(Icons.Default.ViewList, contentDescription = "Liste",
                        tint = if (viewMode == ViewMode.LIST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { viewMode = ViewMode.DETAIL }) {
                    Icon(Icons.Default.ViewAgenda, contentDescription = "Détail",
                        tint = if (viewMode == ViewMode.DETAIL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                TextButton(onClick = { selectionMode = true }) { Text("Sélect.") }
            } else {
                TextButton(onClick = { selectionMode = false; checked = emptySet() }) { Text("Annuler") }
            }
        }

        if (captures.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucune capture")
            }
        } else {
            when (viewMode) {
                ViewMode.GRID -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(captures, key = { it.absolutePath }) { file ->
                        val bmp = remember(file.absolutePath) { CapturesStore.decodeBitmap(file)?.asImageBitmap() }
                        val isChecked = file.absolutePath in checked
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box {
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = file.name,
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clickable {
                                                if (selectionMode) toggleCheck(file) else selected = file
                                            }
                                    )
                                }
                                if (selectionMode) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { toggleCheck(file) },
                                        modifier = Modifier.align(Alignment.TopStart)
                                    )
                                }
                            }
                            Text(fmtDate(file), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
                ViewMode.LIST -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(captures, key = { it.absolutePath }) { file ->
                        val bmp = remember(file.absolutePath) { CapturesStore.decodeBitmap(file)?.asImageBitmap() }
                        val isChecked = file.absolutePath in checked
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { if (selectionMode) toggleCheck(file) else selected = file }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectionMode) {
                                Checkbox(checked = isChecked, onCheckedChange = { toggleCheck(file) })
                            }
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp,
                                    contentDescription = file.name,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(fmtDate(file), style = MaterialTheme.typography.bodyMedium)
                                Text(fmtSize(file), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()
                    }
                }
                ViewMode.DETAIL -> LazyColumn(
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(captures, key = { it.absolutePath }) { file ->
                        val bmp = remember(file.absolutePath) { CapturesStore.decodeBitmap(file)?.asImageBitmap() }
                        val isChecked = file.absolutePath in checked
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (selectionMode) toggleCheck(file) else selected = file }
                        ) {
                            Column {
                                Box {
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp,
                                            contentDescription = file.name,
                                            modifier = Modifier.fillMaxWidth().height(180.dp)
                                        )
                                    }
                                    if (selectionMode) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { toggleCheck(file) },
                                            modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                                        )
                                    }
                                }
                                Row(
                                    Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(fmtDate(file), style = MaterialTheme.typography.bodyMedium)
                                        Text("${fmtSize(file)} · ${file.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Barre d'actions en sélection multiple (au-dessus de la nav bar)
            if (selectionMode) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        checked = if (checked.size == captures.size) emptySet() else captures.map { it.absolutePath }.toSet()
                    }) { Text(if (checked.size == captures.size) "Aucun" else "Tout") }
                    Button(onClick = { shareFiles(captures.filter { it.absolutePath in checked }) },
                        enabled = checked.isNotEmpty()) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(6.dp)); Text("Partager (${checked.size})")
                    }
                    OutlinedButton(onClick = { confirmDelete = true }, enabled = checked.isNotEmpty()) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(6.dp)); Text("Supprimer")
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer ${checked.size} capture(s) ?") },
            text = { Text("Cette action est définitive.") },
            confirmButton = {
                TextButton(onClick = {
                    captures.filter { it.absolutePath in checked }.forEach { it.delete() }
                    captures = CapturesStore.list(context)
                    checked = emptySet()
                    selectionMode = false
                    confirmDelete = false
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun CaptureDetail(file: File, onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val bitmap = remember(file.absolutePath) { CapturesStore.decodeBitmap(file) }
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }

    var zones by remember { mutableStateOf<List<TextZone>>(emptyList()) }
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    var loading by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf("") }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    fun runOcr() {
        val bmp = bitmap ?: return
        loading = true
        scope.launch {
            val result = withContext(Dispatchers.Default) { OcrEngine.recognize(bmp, Settings.barcodesEnabled(context)) }
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
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Retour") }
            Spacer(Modifier.width(8.dp))
            Text(file.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                val uri = FileProvider.getUriForFile(context, "com.ric.scantoutv5.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    if (editedText.isNotBlank()) putExtra(Intent.EXTRA_TEXT, editedText)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Partager"))
            }) {
                Icon(Icons.Default.Share, contentDescription = "Partager")
            }
        }

        if (bitmap != null && imageBitmap != null) {
            val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspect)
                    .clipToBounds()
                    .onGloballyPositioned { viewSize = it.size }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, panChange, zoomChange, _ ->
                            zoom = (zoom * zoomChange).coerceIn(1f, 6f)
                            pan += panChange * zoom
                        }
                    }
                    .pointerInput(zones, viewSize) {
                        detectTapGestures { offset ->
                            if (viewSize.width == 0) return@detectTapGestures
                            val cx = viewSize.width / 2f
                            val cy = viewSize.height / 2f
                            // position dans le repere de l'image non transformee
                            val lx = (offset.x - cx - pan.x) / zoom + cx
                            val ly = (offset.y - cy - pan.y) / zoom + cy
                            val sx = bitmap.width.toFloat() / viewSize.width
                            val sy = bitmap.height.toFloat() / viewSize.height
                            val ix = lx * sx
                            val iy = ly * sy
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
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = zoom,
                            scaleY = zoom,
                            translationX = pan.x,
                            translationY = pan.y
                        )
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
