package com.ric.scantoutv5

import android.content.Context
import android.graphics.BitmapFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Stockage simple des captures : fichiers JPEG dans filesDir/scans/. */
object CapturesStore {
    fun scansDir(context: Context): File =
        File(context.filesDir, "scans").apply { mkdirs() }

    fun newCaptureFile(context: Context): File {
        val stamp = SimpleDateFormat("yyMMdd_HHmmss_SSS", Locale.US).format(Date())
        return File(scansDir(context), "scan_$stamp.jpg")
    }

    /** Décode en appliquant l'orientation EXIF (photo affichée dans le sens de la capture). */
    fun decodeBitmap(file: File): android.graphics.Bitmap? {
        val raw = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        return try {
            val exif = androidx.exifinterface.media.ExifInterface(file.absolutePath)
            val matrix = android.graphics.Matrix()
            when (exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return raw
            }
            android.graphics.Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
        } catch (e: Exception) {
            raw
        }
    }

    fun list(context: Context): List<File> =
        scansDir(context).listFiles { f -> f.isFile && f.extension == "jpg" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
}
