package com.ric.scantoutv5

import android.content.Context

/** Préférences simples (SharedPreferences). */
object Settings {
    private const val PREFS = "scantout_v5"
    private const val KEY_BARCODES = "barcodes_enabled"
    private const val KEY_DARK = "dark_theme"

    fun darkTheme(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DARK, true)

    fun setDarkTheme(context: Context, value: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DARK, value).apply()

    fun barcodesEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_BARCODES, true)

    fun setBarcodesEnabled(context: Context, value: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_BARCODES, value).apply()
}
