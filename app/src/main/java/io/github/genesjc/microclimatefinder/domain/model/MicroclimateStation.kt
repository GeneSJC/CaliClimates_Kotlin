package io.github.genesjc.microclimatefinder.domain.model

import androidx.annotation.StringRes
import io.github.genesjc.microclimatefinder.R

enum class TerrainType(@StringRes val labelRes: Int) {
    COASTAL(R.string.terrain_type_coastal),
    RIDGE(R.string.terrain_type_ridge),
    VALLEY_BASIN(R.string.terrain_type_valley_basin),
    URBAN_CANYON(R.string.terrain_type_urban_canyon)
}

data class WeatherReading(
    val temperatureCelsius: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val windDirectionDegrees: Int,
    val solarIrradianceWm2: Double,
    val pressureHpa: Double
) {
    val temperatureFahrenheit: Double
        get() = (temperatureCelsius * 9.0 / 5.0) + 32.0
}

data class MicroclimateStation(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Int,
    val terrain: TerrainType,
    val currentReading: WeatherReading,
    // The station's own note from the data files; null when the files have none (the screens then say something
    // general about its terrain type and height)
    val microclimateNote: String?,
    // From the station data files: whether the station is known to report each live measurement.
    // null means the files do not say (not checked, or the column is missing).
    val reportsTemp: Boolean? = null,
    val reportsHumidity: Boolean? = null,
    val reportsWind: Boolean? = null,
    // Marked "featured" in the data files: the region's top stations, fetched first for live readings.
    val isFeatured: Boolean = false
) {
    /** False only when the data files say the station does not report temperature. */
    val canHaveLiveReading: Boolean
        get() = reportsTemp != false
}
