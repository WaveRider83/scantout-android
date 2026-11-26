package com.ric.scantoutv5

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

data class TextZone(val id: Int, val text: String, val box: Rect)

object OcrEngine {
    /** OCR synchrone (à appeler hors thread principal). Retourne les lignes détectées. */
    fun recognize(bitmap: Bitmap): List<TextZone> {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val result = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)))
            val zones = mutableListOf<TextZone>()
            var id = 0
            for (block in result.textBlocks) {
                for (line in block.lines) {
                    val box = line.boundingBox ?: continue
                    val text = line.text.trim()
                    if (text.isEmpty()) continue
                    zones += TextZone(id++, text, box)
                }
            }
            zones
        } finally {
            recognizer.close()
        }
    }
}
