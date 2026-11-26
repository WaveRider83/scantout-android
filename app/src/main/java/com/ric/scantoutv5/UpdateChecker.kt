package com.ric.scantoutv5

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class UpdateInfo(val version: String, val apkUrl: String, val sha256: String?, val notes: String)

private const val LATEST_RELEASE_API =
    "https://api.github.com/repos/WaveRider83/scantout-android/releases/latest"

private fun currentVersionName(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"

private fun parseVersion(v: String): List<Int> =
    v.trimStart('v', 'V').split('.').map { it.substringBefore('-').toIntOrNull() ?: 0 }

fun isNewer(remote: String, local: String): Boolean {
    val r = parseVersion(remote); val l = parseVersion(local)
    for (i in 0 until maxOf(r.size, l.size)) {
        val a = r.getOrElse(i) { 0 }; val b = l.getOrElse(i) { 0 }
        if (a != b) return a > b
    }
    return false
}

suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
    try {
        val conn = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000; readTimeout = 8000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "scantout-android")
        }
        val body = conn.inputStream.bufferedReader().readText()
        conn.disconnect()
        val json = JSONObject(body)
        val tag = json.optString("tag_name").ifEmpty { return@withContext null }
        if (!isNewer(tag, currentVersionName(context))) return@withContext null
        val assets = json.optJSONArray("assets") ?: return@withContext null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.optString("name") == "scantout.apk") {
                val digest = a.optString("digest").ifEmpty { null }?.removePrefix("sha256:")
                return@withContext UpdateInfo(tag.trimStart('v', 'V'), a.getString("browser_download_url"), digest, json.optString("body"))
            }
        }
        null
    } catch (e: Exception) {
        null // pas de réseau / API down : on n'embête pas l'utilisateur
    }
}

// Télécharge l'APK dans le cache, vérifie le sha256 (si fourni par l'API GitHub),
// et lance l'installateur. Retourne null si OK, sinon message d'erreur.
suspend fun downloadAndInstall(
    context: Context,
    info: UpdateInfo,
    onProgress: (Float) -> Unit
): String? = withContext(Dispatchers.IO) {
    try {
        val dir = File(context.cacheDir, "update").apply { mkdirs() }
        val apk = File(dir, "scantout-update.apk")
        if (apk.exists()) apk.delete()
        val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000; readTimeout = 30000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "scantout-android")
        }
        val total = conn.contentLengthLong
        var read = 0L
        val digest = MessageDigest.getInstance("SHA-256")
        conn.inputStream.use { input ->
            FileOutputStream(apk).use { out ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    digest.update(buf, 0, n)
                    out.write(buf, 0, n)
                    read += n
                    if (total > 0) onProgress(read.toFloat() / total)
                }
            }
        }
        conn.disconnect()
        if (info.sha256 != null) {
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (!actual.equals(info.sha256, ignoreCase = true)) {
                apk.delete()
                return@withContext "Échec de vérification SHA-256 — mise à jour annulée."
            }
        }
        val uri: Uri = FileProvider.getUriForFile(context, "com.ric.scantoutv5.fileprovider", apk)
        val install = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(install)
        null
    } catch (e: Exception) {
        "Téléchargement impossible : ${e.message}"
    }
}
