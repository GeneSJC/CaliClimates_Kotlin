package io.github.genesjc.microclimatefinder.data.local

import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegionalDefinitions

data class StationDatasetInfo(
    val regionId: String,
    val assetFileName: String,
    val title: String,
    val subtitle: String,
    val stationCount: Int,
    val minElevationMeters: Int,
    val maxElevationMeters: Int
)

object StationDatasets {
    // Array of available datasets; currently contains the single verified Sacramento dataset
    val AVAILABLE_DATASETS = arrayOf(
        StationDatasetInfo(
            regionId = CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.id,
            assetFileName = "sacramento_stations_60mi.csv",
            title = "Sacramento NOAA Stations",
            subtitle = "60-Mile Radius • Offline CSV Dataset",
            stationCount = 21,
            minElevationMeters = 5,
            maxElevationMeters = 1406
        )
    )

    // Direct reference to the zero index of the array
    val DEFAULT_DATASET: StationDatasetInfo
        get() = AVAILABLE_DATASETS[0]

    // Dataset for a region, or null when that region has no station data yet
    fun forRegion(regionId: String): StationDatasetInfo? =
        AVAILABLE_DATASETS.firstOrNull { it.regionId == regionId }
}
