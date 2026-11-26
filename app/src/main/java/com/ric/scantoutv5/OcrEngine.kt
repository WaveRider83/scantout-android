package com.ric.scantoutv5

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

data class TextZone(val id: Int, val text: String, val box: Rect, val isBarcode: Boolean = false, val rawValue: String? = null)

object OcrEngine {
    /**
     * Isole le numéro de série réel d'une charge utile code-barres GS1 / Dahua.
     * Ex. "010880988002002121K111M3GL800027T" -> "K111M3GL800027T" (GTIN-14 puis AI(21)).
     * Retourne l'entrée nettoyée si aucun motif GS1 connu n'est détecté.
     */
    fun sanitizeSn(raw: String): String {
        val s = raw.trim()
        Regex("""^01\d{14}21(.+)$""").find(s)?.let { return it.groupValues[1].trim() }
        Regex("""\(01\)\d{14}\(21\)([^()]+)""").find(s)?.let { return it.groupValues[1].trim() }
        Regex("""\(21\)([^()]+)""").find(s)?.let { return it.groupValues[1].trim() }
        return s.removePrefix("SN:").removePrefix("SN ").removePrefix("S/N:").trim()
    }
    /**
     * Reconstruit le texte d'une ligne à partir des éléments ML Kit :
     * - espace simple entre éléments (comportement normal d'une ligne),
     * - double espace quand l'écart dépasse max(1.8× l'écart médian, 0.5× la hauteur de ligne),
     *   signature typique d'une icône/image insérée dans la chaîne.
     */
    private fun reconstructWithGaps(line: com.google.mlkit.vision.text.Text.Line): String {
        val allElems = line.elements.mapNotNull { e ->
            val b = e.boundingBox ?: return@mapNotNull null
            if (e.text.isBlank()) null else e to b
        }.sortedBy { it.second.left }
        val lineH0 = (line.boundingBox?.height() ?: allElems.firstOrNull()?.second?.height() ?: 1).toFloat()
        // Filtre icônes/logos lus comme un glyphe unique : élément mono-caractère « carré » (aspect ~1)
        // et quasi aussi haut que la ligne -> probablement un logo, pas du texte.
        val iconFiltered = allElems.filterNot { (e, b) ->
            val t = e.text.trim()
            t.length == 1 && t[0] in "Il1|'\"`" &&
                b.width().toFloat() / maxOf(1, b.height()).toFloat() >= 0.55f &&
                b.height().toFloat() >= 0.6f * lineH0
        }
        val elems = if (iconFiltered.isEmpty()) allElems else iconFiltered
        if (elems.isEmpty()) return line.text.trim()
        if (elems.size == 1) return elems[0].first.text.trim()
        val lineH = (line.boundingBox?.height() ?: elems[0].second.height()).toFloat()
        val gaps = elems.zipWithNext { a, b -> (b.second.left - a.second.right).toFloat() }
        val median = gaps.sorted()[gaps.size / 2].coerceAtLeast(1f)
        val bigGap = maxOf(1.8f * median, 0.5f * lineH)
        val sb = StringBuilder(elems[0].first.text.trim())
        for (i in 1 until elems.size) {
            sb.append(if (gaps[i - 1] > bigGap) "  " else " ")
            sb.append(elems[i].first.text.trim())
        }
        return sb.toString().trim()
    }

    /** OCR synchrone (à appeler hors thread principal). Retourne les lignes détectées.
     *  Si [withBarcodes], ajoute aussi les QR/codes-barres détectés. */
    private suspend fun <T> Task<T>.awaitMain(): T =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { cont.resume(it) }
            addOnFailureListener { cont.resumeWithException(it) }
        }

    /** À appeler depuis le thread principal (ML Kit l'exige). L'inférence reste asynchrone. */
    suspend fun recognize(bitmap: Bitmap, withBarcodes: Boolean = true): List<TextZone> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val zones = mutableListOf<TextZone>()
        var id = 0
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val result = recognizer.process(image).awaitMain()
            for (block in result.textBlocks) {
                for (line in block.lines) {
                    val box = line.boundingBox ?: continue
                    val text = reconstructWithGaps(line)
                    if (text.isBlank()) continue
                    zones += TextZone(id++, text, box)
                }
            }
        } finally {
            recognizer.close()
        }
        if (withBarcodes) {
            val scanner = BarcodeScanning.getClient()
            try {
                val codes = scanner.process(image).awaitMain()
                for (code in codes) {
                    val box = code.boundingBox ?: continue
                    val value = code.rawValue ?: code.displayValue ?: continue
                    zones += TextZone(id++, sanitizeSn(value), box, isBarcode = true, rawValue = value)
                }
            } finally {
                scanner.close()
            }
        }
        return zones
    }
}
