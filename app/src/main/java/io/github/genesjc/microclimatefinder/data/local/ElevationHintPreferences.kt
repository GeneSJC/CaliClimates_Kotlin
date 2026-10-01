package io.github.genesjc.microclimatefinder.data.local

import android.content.Context

/**
 * Remembers how many app launches have opened the Elevation view, and whether the user has already tapped
 * the line. The "tap the line" hint shows on each of the first few launches, and stops at the first tap.
 */
class ElevationHintPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("elevation_hint_prefs", Context.MODE_PRIVATE)

    /** Counts this launch once, however many times the view is opened during it. */
    fun countLaunch() {
        if (countedThisLaunch) return
        countedThisLaunch = true
        prefs.edit().putInt(KEY_LAUNCHES, prefs.getInt(KEY_LAUNCHES, 0) + 1).apply()
    }

    fun shouldShow(): Boolean = !prefs.getBoolean(KEY_TAPPED, false) && prefs.getInt(KEY_LAUNCHES, 0) <= MAX_LAUNCHES

    fun markLineTapped() {
        if (!prefs.getBoolean(KEY_TAPPED, false)) prefs.edit().putBoolean(KEY_TAPPED, true).apply()
    }

    private companion object {
        const val KEY_LAUNCHES = "launches"
        const val KEY_TAPPED = "line_tapped"
        const val MAX_LAUNCHES = 3
        @Volatile var countedThisLaunch = false
    }
}
