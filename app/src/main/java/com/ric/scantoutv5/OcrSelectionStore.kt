package com.ric.scantoutv5

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Mémorise, par nom de fichier, les textes des zones OCR sélectionnées,
 * pour restaurer la sélection à la réouverture d'une capture.
 */
object OcrSelectionStore {
    private fun store(context: Context) = File(context.filesDir, "ocr_selection.json")

    fun load(context: Context, fileName: String): List<String> {
        val f = store(context)
        if (!f.exists()) return emptyList()
        return runCatching {
            val obj = JSONObject(f.readText())
            obj.optJSONArray(fileName)?.let { arr -> List(arr.length()) { arr.getString(it) } } ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, fileName: String, texts: List<String>) {
        val f = store(context)
        val obj = runCatching { if (f.exists()) JSONObject(f.readText()) else JSONObject() }.getOrDefault(JSONObject())
        if (texts.isEmpty()) obj.remove(fileName) else obj.put(fileName, org.json.JSONArray(texts))
        f.writeText(obj.toString())
    }
}
