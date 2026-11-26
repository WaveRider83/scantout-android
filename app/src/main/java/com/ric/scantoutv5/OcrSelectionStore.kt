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

    fun loadText(context: Context, fileName: String): String? {
        val f = store(context)
        if (!f.exists()) return null
        return runCatching { JSONObject(f.readText()).optString(fileName + "#text", null) }.getOrNull()
    }

    fun saveText(context: Context, fileName: String, text: String?) {
        val f = store(context)
        val obj = runCatching { if (f.exists()) JSONObject(f.readText()) else JSONObject() }.getOrDefault(JSONObject())
        if (text == null) obj.remove(fileName + "#text") else obj.put(fileName + "#text", text)
        f.writeText(obj.toString())
    }

    fun loadRotation(context: Context, fileName: String): Int {
        val f = store(context)
        if (!f.exists()) return 0
        return runCatching { JSONObject(f.readText()).optInt(fileName + "#rot", 0) }.getOrDefault(0)
    }

    fun saveRotation(context: Context, fileName: String, degrees: Int) {
        val f = store(context)
        val obj = runCatching { if (f.exists()) JSONObject(f.readText()) else JSONObject() }.getOrDefault(JSONObject())
        obj.put(fileName + "#rot", ((degrees % 360) + 360) % 360)
        f.writeText(obj.toString())
    }

    fun save(context: Context, fileName: String, texts: List<String>) {
        val f = store(context)
        val obj = runCatching { if (f.exists()) JSONObject(f.readText()) else JSONObject() }.getOrDefault(JSONObject())
        if (texts.isEmpty()) obj.remove(fileName) else obj.put(fileName, org.json.JSONArray(texts))
        f.writeText(obj.toString())
    }
}
