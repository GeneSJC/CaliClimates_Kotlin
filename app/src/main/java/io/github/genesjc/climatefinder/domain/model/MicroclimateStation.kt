package io.github.genesjc.climatefinder.domain.model

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
