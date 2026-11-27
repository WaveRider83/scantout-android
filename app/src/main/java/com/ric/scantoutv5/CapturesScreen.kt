package com.ric.scantoutv5

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.RotateRight
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext as LCtx
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private fun ocrTextOf(context: android.content.Context, f: File): String? =
    OcrSelectionStore.loadText(context, f.name)?.takeIf { it.isNotBlank() }
        ?: OcrSelectionStore.load(context, f.name).joinToString("\n").takeIf { it.isNotBlank() }

private fun sharePlainText(context: android.content.Context, files: List<File>): String =
    files.mapNotNull { f -> ocrTextOf(context, f)?.let { "— ${f.name} —\n$it" } }.joinToString("\n\n")

// PDF : une page par capture, image au-dessus du texte OCR.
private fun sanitizeFileName(raw: String): String {
    val cleaned = raw.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .replace(Regex("\\s+"), "_").trim('_').take(60)
    return cleaned.ifBlank { "captures_ocr" }
}

private fun shareSuggestions(context: android.content.Context, files: List<File>): List<String> =
    files.asSequence()
        .flatMap { ocrTextOf(context, it)?.lines()?.asSequence() ?: emptySequence() }
        .map { it.trim() }
        .filter { it.length >= 4 }
        .distinct()
        .take(4)
        .toList()

private fun buildSharePdf(context: android.content.Context, files: List<File>, baseName: String): File? = runCatching {
    val doc = android.graphics.pdf.PdfDocument()
    val pageW = 595; val pageH = 842; val margin = 36f
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = android.graphics.Color.BLACK }
    val headPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; color = android.graphics.Color.GRAY }
    for (f in files) {
        var pageNum = 1
        var page = doc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
        var canvas = page.canvas
        var y = margin + headPaint.textSize
        canvas.drawText(f.name, margin, y, headPaint)
        y += 8f
        loadRotatedBitmap(context, f)?.let { bmp ->
            val maxW = pageW - 2 * margin
            val maxH = pageH * 0.55f
            val scale = minOf(maxW / bmp.width, maxH / bmp.height, 1f)
            val w = bmp.width * scale; val h = bmp.height * scale
            canvas.drawBitmap(bmp, null, android.graphics.RectF(margin, y, margin + w, y + h), paint)
            y += h + 12f
        }
        fun newPageIfNeeded() {
            if (y > pageH - margin) {
                doc.finishPage(page)
                pageNum += 1
                page = doc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
                canvas = page.canvas
                y = margin + textPaint.textSize
            }
        }
        ocrTextOf(context, f)?.let { t ->
            for (rawLine in t.lines()) {
                var line = rawLine
                while (line.isNotEmpty()) {
                    var count = textPaint.breakText(line, true, pageW - 2 * margin, null)
                    if (count <= 0) count = line.length
                    newPageIfNeeded()
                    canvas.drawText(line.substring(0, count), margin, y, textPaint)
                    y += textPaint.textSize * 1.35f
                    line = line.substring(count)
                }
            }
        }
        doc.finishPage(page)
    }
    val dir = java.io.File(context.cacheDir, "update").apply { mkdirs() }
    val out = java.io.File(dir, "$baseName.pdf")
    out.outputStream().use { doc.writeTo(it) }
    doc.close()
    out
}.getOrNull()

private fun shareCaptures(context: android.content.Context, files: List<File>, asPdf: Boolean, baseName: String) {
    if (files.isEmpty()) return
    val plainText = sharePlainText(context, files)
    val safe = sanitizeFileName(baseName)
    if (asPdf) {
        val pdfFile = buildSharePdf(context, files, safe)
        if (pdfFile != null) {
            val pdfUri = FileProvider.getUriForFile(context, "com.ric.scantoutv5.fileprovider", pdfFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                if (plainText.isNotBlank()) putExtra(Intent.EXTRA_TEXT, plainText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Partager ${files.size} capture(s) en PDF"))
            return
        }
    }
    val cacheDir = java.io.File(context.cacheDir, "update").apply { mkdirs() }
    val uris = ArrayList(files.mapIndexed { i, f ->
        val name = if (files.size == 1) "$safe.jpg" else "${safe}_${i + 1}.jpg"
        val dest = java.io.File(cacheDir, name)
        val ok = runCatching { f.copyTo(dest, overwrite = true) }.isSuccess
        FileProvider.getUriForFile(context, "com.ric.scantoutv5.fileprovider", if (ok) dest else f)
    })
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "image/jpeg"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        if (plainText.isNotBlank()) putExtra(Intent.EXTRA_TEXT, plainText)
        clipData = android.content.ClipData.newUri(context.contentResolver, "captures", uris.first()).apply {
            uris.drop(1).forEach { addItem(android.content.ClipData.Item(it)) }
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Partager ${files.size} image(s)"))
}

@Composable
private fun RenameCaptureDialog(file: File, suggestions: List<String>, onDismiss: () -> Unit, onRenamed: (File) -> Unit) {
    val context = LocalContext.current
    var name by remember(file.absolutePath) { mutableStateOf(file.nameWithoutExtension) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renommer la capture") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Nom") })
                if (suggestions.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Suggestions (détections OCR) :", style = MaterialTheme.typography.labelMedium)
                    suggestions.forEach { s ->
                        TextButton(onClick = { name = s }) { Text(s, maxLines = 1) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val safe = sanitizeFileName(name)
                val dir = file.parentFile
                var target = File(dir, "$safe.jpg")
                var i = 1
                while (target.exists() && target.absolutePath != file.absolutePath) {
                    target = File(dir, "${safe}_$i.jpg"); i++
                }
                if (file.renameTo(target)) {
                    OcrSelectionStore.renameKeys(context, file.name, target.name)
                    onDismiss()
                    onRenamed(target)
                }
            }) { Text("Renommer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
private fun ShareFormatDialog(files: List<File>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val suggestions = remember(files) { shareSuggestions(context, files) }
    var asPdf by remember { mutableStateOf<Boolean?>(null) }
    var name by remember(files) { mutableStateOf(suggestions.firstOrNull() ?: "captures_ocr") }
    val step = asPdf
    if (step == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Partager ${files.size} capture(s)") },
            text = { Text("Format du partage — le texte OCR est toujours inclus dans le corps du mail.") },
            confirmButton = {
                TextButton(onClick = { asPdf = true }) { Text("PDF") }
            },
            dismissButton = {
                TextButton(onClick = { asPdf = false }) { Text("Images") }
            }
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nom du fichier") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Nom") })
                if (suggestions.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Suggestions (détections OCR) :", style = MaterialTheme.typography.labelMedium)
                    suggestions.forEach { s ->
                        TextButton(onClick = { name = s }) { Text(s, maxLines = 1) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); shareCaptures(context, files, step, name) }) { Text("Partager") }
        },
        dismissButton = {
            TextButton(onClick = { asPdf = null }) { Text("Retour") }
        }
    )
}

private enum class ViewMode { GRID, LIST, DETAIL }

private fun fmtDate(f: File): String =
    java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(f.lastModified()))

private fun fmtSize(f: File): String {
    val kb = f.length() / 1024.0
    return if (kb < 1024) "%.0f Ko".format(kb) else "%.1f Mo".format(kb / 1024.0)
}

fun loadRotatedBitmap(context: android.content.Context, file: java.io.File): android.graphics.Bitmap? {
    val b = CapturesStore.decodeBitmap(file) ?: return null
    val rot = OcrSelectionStore.loadRotation(context, file.name)
    return if (rot % 360 == 0) b else rotateBitmapTop(b, rot.toFloat())
}

fun rotateBitmapTop(src: android.graphics.Bitmap, degrees: Float): android.graphics.Bitmap {
    if (degrees % 360f == 0f) return src
    val m = android.graphics.Matrix().apply { postRotate(degrees, src.width / 2f, src.height / 2f) }
    val out = android.graphics.Bitmap.createBitmap(src.width, src.height, android.graphics.Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(out)
    c.drawColor(android.graphics.Color.WHITE)
    c.drawBitmap(src, m, null)
    return out
}

@Composable
fun CapturesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var captures by remember { mutableStateOf(CapturesStore.list(context)) }
    var ascending by remember { mutableStateOf(true) }
    val shown = remember(captures, ascending) {
        if (ascending) captures.sortedBy { it.lastModified() } else captures.sortedByDescending { it.lastModified() }
    }
    var selected by remember { mutableStateOf<File?>(null) }
    var viewMode by remember { mutableStateOf(ViewMode.GRID) }
    var selectionMode by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf(setOf<String>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            CapturesStore.importFromUri(context, uri)
            captures = CapturesStore.list(context)
        }
    }

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

    var shareDialog by remember { mutableStateOf<List<File>?>(null) }
    fun shareFiles(files: List<File>) {
        if (files.isEmpty()) return
        val order = shown.map { it.absolutePath }
        shareDialog = files.sortedBy { order.indexOf(it.absolutePath) }
    }

    val current = selected
    if (current != null) {
        CaptureDetail(file = current, onBack = {
            selected = null
            captures = CapturesStore.list(context)
        }, onRenamed = { newFile ->
            selected = newFile
            captures = CapturesStore.list(context)
        })
        return
    }

    Column(Modifier.fillMaxSize()) {
        // Barre haute : retour + titre + modes d'affichage + sélection
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← Retour") }
            if (selectionMode) {
                Text("${checked.size} sélectionnée(s)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            } else {
                Spacer(Modifier.weight(1f))
            }
            IconButton(onClick = {
                Settings.setDarkTheme(context, !Settings.darkTheme(context))
                (context as? android.app.Activity)?.recreate()
            }) {
                Icon(Icons.Default.Brightness6, contentDescription = "Basculer thème")
            }
            if (!selectionMode) {
                IconButton(onClick = { importLauncher.launch("image/*") }) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Importer une image",
                        tint = MaterialTheme.colorScheme.primary)
                }
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
                IconButton(onClick = { ascending = !ascending }) {
                    Icon(
                        if (ascending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (ascending) "Ordre chronologique (ancien → récent)" else "Ordre anti-chronologique (récent → ancien)",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                IconButton(onClick = {
                    shareFiles(shown.filter { it.absolutePath in checked })
                }, enabled = checked.isNotEmpty()) {
                    Icon(Icons.Default.Share, contentDescription = "Partager la sélection",
                        tint = if (checked.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { confirmDelete = true }, enabled = checked.isNotEmpty()) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer la sélection",
                        tint = if (checked.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { selectionMode = false; checked = emptySet() }) {
                    Icon(Icons.Default.Close, contentDescription = "Quitter la sélection")
                }
            }
        }

        if (shown.isEmpty()) {
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
                    items(shown, key = { it.absolutePath }) { file ->
                        val bmp = remember(file.absolutePath, OcrSelectionStore.loadRotation(context, file.name)) { loadRotatedBitmap(context, file)?.asImageBitmap() }
                        val isChecked = file.absolutePath in checked
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box {
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = file.name,
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .pointerInput(selectionMode) {
                                                detectTapGestures(
                                                    onTap = { if (selectionMode) toggleCheck(file) else selected = file },
                                                    onLongPress = { if (!selectionMode) { selectionMode = true; checked = setOf(file.absolutePath) } }
                                                )
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
                    items(shown, key = { it.absolutePath }) { file ->
                        val bmp = remember(file.absolutePath, OcrSelectionStore.loadRotation(context, file.name)) { loadRotatedBitmap(context, file)?.asImageBitmap() }
                        val isChecked = file.absolutePath in checked
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .pointerInput(selectionMode) {
                                    detectTapGestures(
                                        onTap = { if (selectionMode) toggleCheck(file) else selected = file },
                                        onLongPress = { if (!selectionMode) { selectionMode = true; checked = setOf(file.absolutePath) } }
                                    )
                                }
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
                    items(shown, key = { it.absolutePath }) { file ->
                        val bmp = remember(file.absolutePath, OcrSelectionStore.loadRotation(context, file.name)) { loadRotatedBitmap(context, file)?.asImageBitmap() }
                        val isChecked = file.absolutePath in checked
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(selectionMode) {
                                    detectTapGestures(
                                        onTap = { if (selectionMode) toggleCheck(file) else selected = file },
                                        onLongPress = { if (!selectionMode) { selectionMode = true; checked = setOf(file.absolutePath) } }
                                    )
                                }
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

        }
    }

    shareDialog?.let { files -> ShareFormatDialog(files) { shareDialog = null } }

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
private fun CaptureDetail(file: File, onBack: () -> Unit, onRenamed: (File) -> Unit = {}) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var bitmap by remember(file.absolutePath, OcrSelectionStore.loadRotation(context, file.name)) { mutableStateOf(loadRotatedBitmap(context, file)) }
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }


    var zones by remember { mutableStateOf<List<TextZone>>(emptyList()) }
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    var loading by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf("") }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var blurry by remember { mutableStateOf(false) }
    var ppRunning by remember { mutableStateOf(false) }
    var ppResults by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var snPair by remember { mutableStateOf(Settings.snBarcodePair(context)) }
    var hlDoubts by remember { mutableStateOf(Settings.highlightDoubts(context)) }
    var showQrZone by remember { mutableStateOf(Settings.showBarcodeZone(context)) }
    var restored by remember { mutableStateOf(false) }
    var shareDetailDialog by remember { mutableStateOf(false) }
    var renameDialog by remember { mutableStateOf(false) }

    fun normPp(s: String) = s.filter { !it.isWhitespace() }.uppercase()
    // Réinjecte les caractères PP-OCR dans la trame ML Kit (espaces/séparateurs conservés).
    // Retourne null si les longueurs (hors espaces) ne correspondent pas.
    fun mergeKeepLayout(ml: String, pp: String): String? {
        val ppChars = pp.filter { it.isLetterOrDigit() }
        val mlCore = ml.count { it.isLetterOrDigit() }
        if (ppChars.isEmpty() || ppChars.length != mlCore) return null
        val sb = StringBuilder()
        var i = 0
        for (c in ml) {
            if (c.isLetterOrDigit()) { sb.append(ppChars[i]); i++ } else sb.append(c)
        }
        return sb.toString()
    }
    fun doubtful(z: TextZone): Boolean = !z.isBarcode &&
        Regex("[A-Za-z0-9]{4,}").findAll(z.text).any { m ->
            val t = m.value; t.any(Char::isDigit) && t.any { it in "0O1lI5S8B" }
        }
    // Contrôle SN/QR : si une ligne OCR contient la valeur d'un code-barres (comparaison alphanum),
    // on corrige en place uniquement les caractères concernés. Sinon la ligne est laissée telle quelle.
    // Aligne la valeur du code-barres sur la ligne OCR par fenêtre glissante (tolère les substitutions OCR),
    // remplace la fenêtre par la valeur exacte, et nettoie un glyphe-piège résiduel (« I » de logo) collé après « SN ».
    fun correctLineWithBarcodes(line: String, barcodes: List<TextZone>): String {
        val idxMap = mutableListOf<Int>()
        val normSb = StringBuilder()
        for ((i, c) in line.withIndex()) {
            if (c.isLetterOrDigit()) { normSb.append(c.uppercaseChar()); idxMap.add(i) }
        }
        val nz = normSb.toString()
        if (nz.isEmpty()) return line
        for (b in barcodes) {
            val nb = b.text.uppercase().filter { it.isLetterOrDigit() }
            if (nb.length < 6 || nz.length < nb.length) continue
            var bestAt = -1; var bestMatch = -1
            for (i in 0..nz.length - nb.length) {
                var m = 0
                for (j in nb.indices) if (nz[i + j] == nb[j]) m++
                if (m > bestMatch) { bestMatch = m; bestAt = i }
            }
            if (bestMatch < (nb.length * 3 + 4) / 5) continue  // < 60 % de caractères identiques : pas la même chaîne
            val from = idxMap[bestAt]
            val to = idxMap[bestAt + nb.length - 1] + 1
            var prefix = line.substring(0, from)
            prefix = prefix.replace(Regex("""(?i)(S[/ ]?N)[Il1|]\s*$"""), "$1 ")
            val corrected = prefix + b.text.trim() + line.substring(to)
            return corrected
        }
        return line
    }

    fun applyBarcodeCorrections(zones: List<TextZone>, keepBarcodes: Boolean): List<TextZone> {
        val barcodes = zones.filter { it.isBarcode }
        return zones.mapNotNull { z ->
            if (z.isBarcode) { if (keepBarcodes) z else null }
            else {
                val corrected = correctLineWithBarcodes(z.text, barcodes)
                if (corrected != z.text) z.copy(text = corrected) else z
            }
        }
    }

    // Affichage : corrections SN + charge brute pour les zones code-barres (rawValue)
    fun displayZones(z: List<TextZone>): List<TextZone> =
        (if (snPair) applyBarcodeCorrections(z, showQrZone) else z)
            .map { it2 -> if (it2.isBarcode && !it2.rawValue.isNullOrEmpty()) it2.copy(text = it2.rawValue) else it2 }

    fun shownZones(): List<TextZone> {
        val base = displayZones(zones)
        return if (showQrZone) base else base.filter { !it.isBarcode }
    }

    fun renderWithPp(sel: List<TextZone>, pp: Map<Int, String>): String =
        sel.flatMap { z ->
            val lines = mutableListOf(z.text)
            if (snPair && z.isBarcode) {
                val raw = z.text.trim()
                val sn = OcrEngine.sanitizeSn(z.rawValue ?: z.text).trim()
                if (sn.isNotEmpty() && sn != raw) lines.add("SN " + sn)
            }
            pp[z.id]?.let { ppText ->
                val merged = mergeKeepLayout(z.text, ppText)
                if (merged != null && normPp(merged) != normPp(z.text)) lines[lines.size - 1] = merged
            }
            lines
        }.joinToString("\n")
    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    fun applySelection(ids: Set<Int>) {
        selectedIds = ids
        editedText = renderWithPp(shownZones().filter { it.id in ids }, ppResults)
        OcrSelectionStore.save(context, file.name, shownZones().filter { it.id in ids }.map { it.text })
    }

    fun restoreSelection(result: List<TextZone>): Pair<Set<Int>, Boolean> {
        val saved = OcrSelectionStore.load(context, file.name)
        val norm = { t: String -> t.lowercase().filter { !it.isWhitespace() } }
        val remaining = saved.map { norm(it) }.toMutableList()
        val ids = mutableSetOf<Int>()
        val shown = displayZones(result)
        fun candidates(z: TextZone): List<String> = buildList {
            add(norm(z.text))
            if (z.isBarcode) {
                add(norm(OcrEngine.sanitizeSn(z.text)))
                z.rawValue?.let { add(norm(it)) }
            }
        }
        for (z in shown) {
            val idx = candidates(z).firstNotNullOfOrNull { c -> remaining.indexOf(c).takeIf { it >= 0 } }
            if (idx != null && idx >= 0) { ids.add(z.id); remaining.removeAt(idx) }
        }
        selectedIds = ids
        val validated = OcrSelectionStore.loadText(context, file.name)
        val base = shown.filter { it.id in ids }
        editedText = validated ?: renderWithPp(base, emptyMap())
        return ids to (validated != null)
    }

    fun runOcr(resetSelection: Boolean = false) {
        val srcBmp = bitmap ?: return
        loading = true
        scope.launch {
            try {
                val withBarcodes = Settings.barcodesEnabled(context) || snPair
                var finalBmp: android.graphics.Bitmap = srcBmp
                var result: List<TextZone> = OcrEngine.recognize(srcBmp, withBarcodes)
                // 180° automatique uniquement si PP-OCR (confiance par ligne) préfère nettement l'image retournée.
                fun ppConfidence(bmp: android.graphics.Bitmap, res: List<TextZone>): Double {
                    val lines = res.filter { !it.isBarcode && it.text.length >= 4 }
                        .sortedByDescending { it.box.width().toLong() * it.box.height() }
                        .take(4)
                    var sum = 0.0; var wsum = 0.0
                    for (l in lines) {
                        val r = PpOcrRec.recognize(context, bmp, l.box) ?: continue
                        val wgt = l.text.length.toDouble()
                        sum += r.confidence * wgt; wsum += wgt
                    }
                    return if (wsum == 0.0) 0.0 else sum / wsum
                }
                run {
                    val flipped = rotateBitmapTop(srcBmp, 180f)
                    val flippedRes = OcrEngine.recognize(flipped, withBarcodes)
                    val c0 = ppConfidence(srcBmp, result)
                    val cF = ppConfidence(flipped, flippedRes)
                    if (c0 > 0.0 && cF > c0 * 1.2 && cF >= 0.55) {
                        finalBmp = flipped; result = flippedRes
                        ppResults = emptyMap()
                        bitmap = flipped
                        zoom = 1f; pan = androidx.compose.ui.geometry.Offset.Zero
                        val cur = OcrSelectionStore.loadRotation(context, file.name)
                        OcrSelectionStore.saveRotation(context, file.name, cur + 180)
                    }
                }

                blurry = withContext(Dispatchers.Default) { BlurDetector.isBlurry(finalBmp) }
                zones = result
                val (savedIds, _) = restoreSelection(result)
                restored = true
                val ids = savedIds
                val shownResult = displayZones(result)
                val targets = shownResult.filter { it.id in ids && !it.isBarcode && doubtful(it) }
                if (targets.isNotEmpty()) {
                    scope.launch {
                        ppRunning = true
                        val pp = mutableMapOf<Int, String>()
                        withContext(Dispatchers.Default) {
                            for (z in targets) PpOcrRec.recognize(context, finalBmp, z.box)?.let { pp[z.id] = it.text }
                        }
                        ppResults = ppResults + pp
                        val sel = displayZones(result).filter { it.id in ids }
                        editedText = renderWithPp(sel, ppResults)
                        ppRunning = false
                    }
                }
            } catch (e: Exception) {
                restored = true
                android.widget.Toast.makeText(context, "OCR échoué: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(file.absolutePath) { runOcr() }

    val ambiguousTokens = remember(editedText) {
        Regex("[A-Za-z0-9]{4,}").findAll(editedText).map { it.value }
            .filter { t -> t.any { it.isDigit() } && (t.contains('O') || t.contains('0') || t.contains('o')) }
            .distinct().take(8).toList()
    }


    fun refreshAfterSelectionChange() {
        val sel = shownZones().filter { it.id in selectedIds }
        editedText = renderWithPp(sel, ppResults)
        OcrSelectionStore.save(context, file.name, sel.map { it.text })
        val missing = sel.filter { doubtful(it) && it.id !in ppResults }
        val bmp = bitmap
        if (missing.isNotEmpty() && bmp != null) {
            scope.launch {
                ppRunning = true
                val texts = mutableMapOf<Int, String>()
                withContext(Dispatchers.Default) {
                    for (z in missing) PpOcrRec.recognize(context, bmp, z.box)?.takeIf { it.text.isNotBlank() }?.let { texts[z.id] = it.text }
                }
                ppResults = ppResults + texts
                editedText = renderWithPp(shownZones().filter { it.id in selectedIds }, ppResults)
                ppRunning = false
            }
        }
    }

    fun syncEditedFromSelection() {
        editedText = renderWithPp(shownZones().filter { it.id in selectedIds }, ppResults)
        OcrSelectionStore.save(context, file.name, shownZones().filter { it.id in selectedIds }.map { it.text })
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Retour") }
            Spacer(Modifier.width(8.dp))
            Text(file.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                OcrSelectionStore.saveText(context, file.name, editedText)
                shareDetailDialog = true
            }) {
                Icon(Icons.Default.Share, contentDescription = "Partager")
            }
            IconButton(onClick = { renameDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Renommer")
            }
        }

        val curBitmap = bitmap
        if (curBitmap != null && imageBitmap != null) {
            val bmpSafe = curBitmap
            val aspect = bmpSafe.width.toFloat() / bmpSafe.height.toFloat()
            val maxH = 340.dp
            BoxWithConstraints(
                Modifier.fillMaxWidth().height(maxH).clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
            // conteneur plein largeur x maxH : le zoom peut occuper tout l'espace disponible
            val w = minOf(maxWidth, maxH * aspect)
            val h = w / aspect
            Box(
                Modifier
                    .size(w, h)
                    .clipToBounds()
                    .onGloballyPositioned { viewSize = it.size }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, panChange, zoomChange, _ ->
                            zoom = (zoom * zoomChange).coerceIn(1f, 6f)
                            pan += panChange * zoom
                        }
                    }
                    .pointerInput(shownZones().size, viewSize) {
                        detectTapGestures { offset ->
                            if (viewSize.width == 0) return@detectTapGestures
                            val cx = viewSize.width / 2f
                            val cy = viewSize.height / 2f
                            // position dans le repere de l'image non transformee
                            val lx = (offset.x - cx - pan.x) / zoom + cx
                            val ly = (offset.y - cy - pan.y) / zoom + cy
                            val sx = bmpSafe.width.toFloat() / viewSize.width
                            val sy = bmpSafe.height.toFloat() / viewSize.height
                            val ix = lx * sx
                            val iy = ly * sy
                            val hit = shownZones().lastOrNull { z ->
                                ix >= z.box.left && ix <= z.box.right && iy >= z.box.top && iy <= z.box.bottom
                            }
                            if (hit != null) {
                                selectedIds = if (hit.id in selectedIds) selectedIds - hit.id else selectedIds + hit.id
                                refreshAfterSelectionChange()
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
                        val sx = size.width / bmpSafe.width
                        val sy = size.height / bmpSafe.height
                        shownZones().forEach { z ->
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
        }

        // Barre d'icônes compacte sous l'image
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            fun tint(on: Boolean) = if (on) Color(0xFF00E5FF) else Color(0xFFB8C9D9)
            IconButton(onClick = {
                selectedIds = shownZones().map { it.id }.toSet(); refreshAfterSelectionChange()
            }) { Icon(Icons.Default.SelectAll, contentDescription = "Tout sélectionner", tint = Color(0xFFE6F7FF)) }
            IconButton(onClick = {
                selectedIds = emptySet(); refreshAfterSelectionChange()
            }) { Icon(Icons.Default.ClearAll, contentDescription = "Aucune zone", tint = Color(0xFFE6F7FF)) }
            IconButton(onClick = { snPair = !snPair; Settings.setSnBarcodePair(context, snPair); refreshAfterSelectionChange() }) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "SN ↔ code-barres", tint = tint(snPair))
            }
            IconButton(onClick = { hlDoubts = !hlDoubts; Settings.setHighlightDoubts(context, hlDoubts) }) {
                Icon(Icons.Default.Highlight, contentDescription = "Doutes surlignés", tint = tint(hlDoubts))
            }
            IconButton(onClick = { showQrZone = !showQrZone; Settings.setShowBarcodeZone(context, showQrZone); refreshAfterSelectionChange() }) {
                Icon(Icons.Default.QrCode, contentDescription = "Zone QR visible", tint = tint(showQrZone))
            }
            IconButton(onClick = {
                val b = bitmap ?: return@IconButton
                val rot = (OcrSelectionStore.loadRotation(context, file.name) + 90) % 360
                OcrSelectionStore.saveRotation(context, file.name, rot)
                bitmap = rotateBitmapTop(b, 90f)
                ppResults = emptyMap()
                zoom = 1f; pan = androidx.compose.ui.geometry.Offset.Zero
                runOcr()
            }) {
                Icon(Icons.Default.RotateRight, contentDescription = "Pivoter de 90°", tint = Color(0xFFE6F7FF))
            }
        }

        if (blurry) {
            Text(
                "⚠ Photo floue — stabilisez l'appareil ou rapprochez-vous, puis reprenez la capture.",
                color = Color(0xFFFFB300),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        if (ambiguousTokens.isNotEmpty()) {
            Text(
                "À vérifier (0/O) : " + ambiguousTokens.joinToString(", "),
                color = Color(0xFFFFB300),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        }

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = {
                    OcrSelectionStore.saveText(context, file.name, null)
                    runOcr(resetSelection = true)
                }, enabled = !loading, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("Re-analyser", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        LaunchedEffect(editedText) {
            if (restored) OcrSelectionStore.saveText(context, file.name, editedText)
        }
        OutlinedTextField(
            value = editedText,
            onValueChange = { editedText = it },
            modifier = Modifier.fillMaxWidth().height(170.dp).padding(horizontal = 12.dp),
            label = { Text("Texte (éditable)") }
        )
        if (hlDoubts && editedText.isNotBlank()) {
            val doubt = "0O1lI5S8B2Z6"
            val ann = buildAnnotatedString {
                for (c in editedText) {
                    if (c in doubt) withStyle(SpanStyle(color = Color(0xFFFFB300), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) { append(c) } else append(c)
                }
            }
            Text(ann, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp), style = MaterialTheme.typography.bodySmall)
            Text("ambre = caractère ambigu", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8FA3B8), modifier = Modifier.padding(horizontal = 14.dp))
        }

        Button(
            onClick = { clipboard.setText(AnnotatedString(editedText)) },
            enabled = editedText.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) { Text("Copier") }
    }
    if (shareDetailDialog) {
        ShareFormatDialog(listOf(file)) { shareDetailDialog = false }
    }
    if (renameDialog) {
        RenameCaptureDialog(
            file = file,
            suggestions = shareSuggestions(context, listOf(file)),
            onDismiss = { renameDialog = false },
            onRenamed = { newFile -> renameDialog = false; onRenamed(newFile) }
        )
    }
}
