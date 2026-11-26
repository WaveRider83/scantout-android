package com.ric.scantoutv5

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.nio.FloatBuffer

/**
 * PP-OCRv4 English mobile recognition (CTC), onnxruntime.
 * blank = classe 0, caractères = dict (95 entrées) des classes 1..95.
 */
object PpOcrRec {
    private const val MODEL = "pp_ocr_rec_en.onnx"
    private const val DICT = "en_dict.txt"
    private const val TARGET_H = 48

    data class RecResult(val text: String, val confidence: Float, val lowConfidenceChars: List<Int>)

    private var session: OrtSession? = null
    private var dict: List<String> = emptyList()

    @Synchronized
    fun load(context: Context) {
        if (session != null) return
        val env = OrtEnvironment.getEnvironment()
        val modelFile = File(context.cacheDir, MODEL)
        if (!modelFile.exists()) {
            context.assets.open(MODEL).use { input -> modelFile.outputStream().use { input.copyTo(it) } }
        }
        session = env.createSession(modelFile.absolutePath, OrtSession.SessionOptions())
        dict = context.assets.open(DICT).bufferedReader().readLines()
    }

    /** Reconnaît une zone (crop) de la photo. Retourne texte + confiance moyenne. */
    @Synchronized
    fun recognize(context: Context, full: Bitmap, box: android.graphics.Rect): RecResult? {
        return runCatching {
            load(context)
            val sess = session ?: return null
            val left = box.left.coerceIn(0, full.width - 1)
            val top = box.top.coerceIn(0, full.height - 1)
            val right = box.right.coerceIn(left + 1, full.width)
            val bottom = box.bottom.coerceIn(top + 1, full.height)
            val crop = Bitmap.createBitmap(full, left, top, right - left, bottom - top)
            val h = TARGET_H
            val w = (crop.width.toFloat() * h / crop.height).toInt().coerceIn(1, 2000)
            val scaled = Bitmap.createScaledBitmap(crop, w, h, true)
            val pixels = IntArray(w * h)
            scaled.getPixels(pixels, 0, w, 0, 0, w, h)
            val input = FloatArray(3 * h * w)
            var i = 0
            for (c in 0 until 3) {
                for (p in pixels) {
                    val v = when (c) {
                        0 -> (p shr 16) and 0xFF; 1 -> (p shr 8) and 0xFF; else -> p and 0xFF
                    }
                    input[i++] = (v / 255f - 0.5f) / 0.5f
                }
            }
            val env = OrtEnvironment.getEnvironment()
            val shape = longArrayOf(1, 3, h.toLong(), w.toLong())
            OnnxTensor.createTensor(env, FloatBuffer.wrap(input), shape).use { tensor ->
                sess.run(mapOf("x" to tensor)).use { result ->
                    @Suppress("UNCHECKED_CAST")
                    val out = result[0].value as Array<Array<FloatArray>>
                    val seq = out[0] // [T, 97]
                    decode(seq)
                }
            }
        }.getOrNull()
    }

    private fun decode(seq: Array<FloatArray>): RecResult {
        val chars = StringBuilder()
        val confs = mutableListOf<Float>()
        var prevIdx = -1
        for (t in seq) {
            var best = 0
            var bestV = Float.NEGATIVE_INFINITY
            for (i in t.indices) if (t[i] > bestV) { bestV = t[i]; best = i }
            if (best == 0) { prevIdx = 0; continue } // CTC blank
            if (best == prevIdx) continue           // repeated class
            prevIdx = best
            // softmax approx: scores are logits? model ends with softmax per output name "softmax_2.tmp_0"
            val ch = dict.getOrNull(best - 1) ?: continue
            chars.append(ch)
            confs.add(bestV)
        }
        val meanConf = if (confs.isEmpty()) 0f else confs.average().toFloat()
        val low = mutableListOf<Int>()
        var idx = 0
        for (c in confs) { if (c < 0.75f) low.add(idx); idx++ }
        return RecResult(chars.toString().trim(), meanConf, low)
    }
}
