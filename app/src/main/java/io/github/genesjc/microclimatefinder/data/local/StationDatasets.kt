package io.github.genesjc.microclimatefinder.data.local

data class StationDatasetInfo(
    val assetFileName: String,
    val title: String,
    val subtitle: String
)

object StationDatasets {
    // Array of available datasets; currently contains the single verified Sacramento dataset
    val AVAILABLE_DATASETS = arrayOf(
        StationDatasetInfo(
            assetFileName = "sacramento_stations_60mi.csv",
            title = "Sacramento NOAA Stations",
            subtitle = "60-Mile Radius • Offline CSV Dataset"
        )
    )

    // Direct reference to the zero index of the array
    val DEFAULT_DATASET: StationDatasetInfo
        get() = AVAILABLE_DATASETS[0]
}
