package io.github.genesjc.microclimatefinder.data.local

import android.content.Context

/** Remembers whether the user wants the dark look. It starts off (the light look). */
class ThemePreferences(context: Context) {

    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    fun darkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)

    fun saveDarkMode(dark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, dark).apply()
    }

    private companion object {
        const val KEY_DARK_MODE = "dark_mode"
    }
}
