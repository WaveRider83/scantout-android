package com.ric.scantoutv5

import android.content.Context
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

    fun list(context: Context): List<File> =
        scansDir(context).listFiles { f -> f.isFile && f.extension == "jpg" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
}
