package io.github.genesjc.climatefinder.data.local

import io.github.genesjc.climatefinder.config.StateUsaCA

/** Per-region settings for the Transect Map: which stations to load and how to label them. */
data class TransectConfig(
    val regionId: String,
    val displayName: String,
    val assetFileName: String,
    val fallbackAssetFileName: String,
    val routeLabel: String,
    val defaultStationIds: List<String>
)

object TransectConfigs {
    val ALL = listOf(
        TransectConfig(
            regionId = StateUsaCA.BAY_AREA_SACRAMENTO.id,
            displayName = "Bay Area",
            assetFileName = "california_unified_stations.csv",
            fallbackAssetFileName = "sacramento_stations_60mi.csv",
            routeLabel = "Sacramento to Monterey Bay",
            defaultStationIds = listOf("KMRY", "KSAC")
        )
    )

    val DEFAULT: TransectConfig
        get() = ALL[0]

    // Transect config for a region, or null when that region has no transect yet
    fun forRegion(regionId: String): TransectConfig? =
        ALL.firstOrNull { it.regionId == regionId }
}
