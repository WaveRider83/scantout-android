package com.ric.scantoutv5

import android.content.Context

/** Préférences simples (SharedPreferences). */
object Settings {
    private const val PREFS = "scantout_v5"
    private const val KEY_BARCODES = "barcodes_enabled"
    private const val KEY_DARK = "dark_theme"
    private const val KEY_SN_PAIR = "sn_barcode_pair"
    private const val KEY_HL_DOUBT = "highlight_doubts"
    private const val KEY_SHOW_QR = "show_qr_zone"

    fun darkTheme(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DARK, true)

    fun setDarkTheme(context: Context, value: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DARK, value).apply()

    fun snBarcodePair(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SN_PAIR, true)

    fun setSnBarcodePair(context: Context, value: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_SN_PAIR, value).apply()

    fun highlightDoubts(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_HL_DOUBT, true)

    fun setHighlightDoubts(context: Context, value: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_HL_DOUBT, value).apply()

    fun showBarcodeZone(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SHOW_QR, true)

    fun setShowBarcodeZone(context: Context, value: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_SHOW_QR, value).apply()

    fun barcodesEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_BARCODES, true)

    fun setBarcodesEnabled(context: Context, value: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_BARCODES, value).apply()
}
