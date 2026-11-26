package com.ric.scantoutv5

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

data class TextZone(val id: Int, val text: String, val box: Rect)

object OcrEngine {
    /**
     * Reconstruit le texte d'une ligne à partir des éléments ML Kit :
     * - espace simple entre éléments (comportement normal d'une ligne),
     * - double espace quand l'écart dépasse max(1.8× l'écart médian, 0.5× la hauteur de ligne),
     *   signature typique d'une icône/image insérée dans la chaîne.
     */
    private fun reconstructWithGaps(line: com.google.mlkit.vision.text.Text.Line): String {
        val elems = line.elements.mapNotNull { e ->
            val b = e.boundingBox ?: return@mapNotNull null
            if (e.text.isBlank()) null else e to b
        }.sortedBy { it.second.left }
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
    fun recognize(bitmap: Bitmap, withBarcodes: Boolean = true): List<TextZone> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val zones = mutableListOf<TextZone>()
        var id = 0
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val result = Tasks.await(recognizer.process(image))
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
                val codes = Tasks.await(scanner.process(image))
                for (code in codes) {
                    val box = code.boundingBox ?: continue
                    val value = code.rawValue ?: code.displayValue ?: continue
                    zones += TextZone(id++, value, box)
                }
            } finally {
                scanner.close()
            }
        }
        return zones
    }
}
