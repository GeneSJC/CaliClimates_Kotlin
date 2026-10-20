package io.github.genesjc.microclimatefinder.data.local

import android.content.Context

/** Where the dark-look choice is kept between launches (a seam so tests can use a fake). */
interface DarkModeStore {
    fun darkMode(): Boolean

    fun saveDarkMode(dark: Boolean)
}

/** Remembers whether the user wants the dark look. It starts off (the light look). */
class ThemePreferences(context: Context) : DarkModeStore {

    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    override fun darkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)

    override fun saveDarkMode(dark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, dark).apply()
    }

    private companion object {
        const val KEY_DARK_MODE = "dark_mode"
    }
}
