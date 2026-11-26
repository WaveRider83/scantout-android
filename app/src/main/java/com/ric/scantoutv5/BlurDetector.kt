package com.ric.scantoutv5

import android.graphics.Bitmap
import android.graphics.Color

/** Détection simple de flou : variance du Laplacien sur image réduite en niveaux de gris. */
object BlurDetector {
    private const val MAX_DIM = 256
    const val DEFAULT_THRESHOLD = 100.0

    fun variance(src: Bitmap): Double {
        val scale = MAX_DIM.toFloat() / maxOf(src.width, src.height).coerceAtLeast(1)
        val w = maxOf(1, (src.width * scale).toInt())
        val h = maxOf(1, (src.height * scale).toInt())
        val small = Bitmap.createScaledBitmap(src, w, h, true)
        val px = IntArray(w * h)
        small.getPixels(px, 0, w, 0, 0, w, h)
        val gray = DoubleArray(w * h) { Color.red(px[it]) * 0.299 + Color.green(px[it]) * 0.587 + Color.blue(px[it]) * 0.114 }
        var sum = 0.0
        var sumSq = 0.0
        var n = 0
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val i = y * w + x
                val lap = 4 * gray[i] - gray[i - 1] - gray[i + 1] - gray[i - w] - gray[i + w]
                sum += lap
                sumSq += lap * lap
                n++
            }
        }
        if (n == 0) return 0.0
        val mean = sum / n
        return sumSq / n - mean * mean
    }

    fun isBlurry(src: Bitmap, threshold: Double = DEFAULT_THRESHOLD): Boolean =
        variance(src) < threshold
}
