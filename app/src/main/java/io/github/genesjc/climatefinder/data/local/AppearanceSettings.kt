package io.github.genesjc.climatefinder.data.local

/** What the app remembers about its look, between launches. */
interface AppearanceSettings {
    /** Whether the user wants the dark look. It starts off (the light look). */
    fun darkMode(): Boolean

    fun saveDarkMode(dark: Boolean)
}
