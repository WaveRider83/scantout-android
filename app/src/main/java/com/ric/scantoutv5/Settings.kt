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
    private const val KEY_VIEW_MODE = "view_mode"
    private const val KEY_SORT_ASC = "sort_asc"
    private const val KEY_SCROLL = "scroll_"

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

    fun viewMode(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_VIEW_MODE, "GRID") ?: "GRID"

    fun setViewMode(context: Context, value: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_VIEW_MODE, value).apply()

    fun sortAscending(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SORT_ASC, true)

    fun setSortAscending(context: Context, value: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_SORT_ASC, value).apply()

    fun scrollPos(context: Context, mode: String): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_SCROLL + mode, 0)

    fun setScrollPos(context: Context, mode: String, value: Int) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_SCROLL + mode, value).apply()

    fun barcodesEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_BARCODES, true)

    fun setBarcodesEnabled(context: Context, value: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_BARCODES, value).apply()
}
