package io.github.genesjc.climatefinder.data.local

import android.content.Context
import android.content.SharedPreferences
import io.github.genesjc.climatefinder.domain.BandAngle
import io.github.genesjc.climatefinder.domain.BandAxis
import io.github.genesjc.climatefinder.config.StateUsaDefinition

/**
 * The three kinds of settings ([RegionSettings], [AppearanceSettings], [ElevationSettings]) kept in one SharedPreferences
 * file, with one key per value. Each consumer asks the container for the narrow interface it needs.
 */
class AppSettingsPreferences(
    private val prefs: SharedPreferences,
    private val stateUsa: StateUsaDefinition
) : RegionSettings, AppearanceSettings, ElevationSettings {

    constructor(context: Context, stateUsa: StateUsaDefinition) :
        this(context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE), stateUsa)

    // One instance lives for the whole app run, so this is true once this launch has been counted
    private var countedThisLaunch = false

    override fun selectedRegionId(): String {
        val saved = prefs.getString(KEY_REGION, null)
        return if (stateUsa.regions.any { it.id == saved }) saved!! else stateUsa.defaultRegionId
    }

    override fun saveSelectedRegionId(regionId: String) {
        prefs.edit().putString(KEY_REGION, regionId).apply()
    }

    override fun selectedStationId(regionId: String): String? =
        prefs.getString(KEY_STATION_ID, null)?.takeIf { prefs.getString(KEY_STATION_REGION, null) == regionId }

    override fun saveSelectedStationId(regionId: String, stationId: String?) {
        prefs.edit().putString(KEY_STATION_REGION, regionId).putString(KEY_STATION_ID, stationId).apply()
    }

    override fun mapZoom(regionId: String): Float =
        if (prefs.getString(KEY_ZOOM_REGION, null) == regionId) prefs.getFloat(KEY_ZOOM, 1f) else 1f

    override fun saveMapZoom(regionId: String, zoom: Float) {
        prefs.edit().putString(KEY_ZOOM_REGION, regionId).putFloat(KEY_ZOOM, zoom).apply()
    }

    override fun darkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)

    override fun saveDarkMode(dark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, dark).apply()
    }

    override fun countElevationHintLaunch() {
        if (countedThisLaunch) return
        countedThisLaunch = true
        prefs.edit().putInt(KEY_HINT_LAUNCHES, prefs.getInt(KEY_HINT_LAUNCHES, 0) + 1).apply()
    }

    override fun shouldShowElevationHint(): Boolean =
        !prefs.getBoolean(KEY_HINT_TAPPED, false) && prefs.getInt(KEY_HINT_LAUNCHES, 0) <= MAX_HINT_LAUNCHES

    override fun markElevationLineTapped() {
        if (!prefs.getBoolean(KEY_HINT_TAPPED, false)) prefs.edit().putBoolean(KEY_HINT_TAPPED, true).apply()
    }

    override fun elevationLayers(): Boolean = prefs.getBoolean(KEY_ELEVATION_LAYERS, true)

    override fun saveElevationLayers(on: Boolean) {
        prefs.edit().putBoolean(KEY_ELEVATION_LAYERS, on).apply()
    }

    // Stored in degrees. Before the angle there was an axis stored by name (builds 113.x): west to east reads as 90, anything
    // else, or nothing, as north to south
    override fun elevationBandAngle(): Int =
        if (prefs.getInt(KEY_ELEVATION_BAND_ANGLE, NO_ANGLE) != NO_ANGLE) {
            BandAngle.clamp(prefs.getInt(KEY_ELEVATION_BAND_ANGLE, BandAngle.NORTH).toFloat())
        } else if (prefs.getString(KEY_ELEVATION_BAND_AXIS, null) == BandAxis.WEST_EAST.name) {
            BandAngle.WEST
        } else {
            BandAngle.NORTH
        }

    override fun saveElevationBandAngle(degrees: Int) {
        prefs.edit().putInt(KEY_ELEVATION_BAND_ANGLE, BandAngle.clamp(degrees.toFloat())).apply()
    }

    override fun homeTab(): Int = prefs.getInt(KEY_HOME_TAB, 0).coerceIn(0, 1)

    override fun saveHomeTab(tab: Int) {
        prefs.edit().putInt(KEY_HOME_TAB, tab.coerceIn(0, 1)).apply()
    }

    companion object {
        const val FILE_NAME = "app_settings"
        const val MAX_HINT_LAUNCHES = 3

        private const val KEY_REGION = "selected_region"
        private const val KEY_STATION_REGION = "station_region"
        private const val KEY_STATION_ID = "station_id"
        private const val KEY_ZOOM_REGION = "zoom_region"
        private const val KEY_ZOOM = "zoom"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_HINT_LAUNCHES = "hint_launches"
        private const val KEY_HINT_TAPPED = "hint_line_tapped"
        private const val KEY_ELEVATION_LAYERS = "elevation_layers"
        private const val KEY_HOME_TAB = "home_tab"
        private const val KEY_ELEVATION_BAND_AXIS = "elevation_band_axis"
        private const val KEY_ELEVATION_BAND_ANGLE = "elevation_band_angle"
        private const val NO_ANGLE = -1
    }
}
