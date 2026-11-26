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
                    val text = line.text.trim()
                    if (text.isEmpty()) continue
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
