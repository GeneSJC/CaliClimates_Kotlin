package io.github.genesjc.microclimatefinder.data.local

import android.content.Context
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegionalDefinitions

/** A station dataset for one CA region: a full bundled file, or the small built-in sample. */
data class StationDatasetInfo(
    val regionId: String,
    val fileName: String,
    val isSample: Boolean,
    val title: String,
    val subtitle: String,
    val stationCount: Int,
    val minElevationMeters: Int,
    val maxElevationMeters: Int
)

/**
 * Finds station data per region, first match wins:
 *  1. a full file bundled at build time (stations_<regionId>.csv in assets, dropped into the
 *     gitignored app/src/debug/assets folder),
 *  2. the small built-in sample (stations_<regionId>_sample.csv), Bay Area only.
 */
class StationDataStore(private val context: Context) {

    private val parser = StationCsvParser(context)

    private val assetNames: Set<String> by lazy {
        try { context.assets.list("")?.toSet() ?: emptySet() } catch (e: Exception) { emptySet() }
    }

    private fun fileNameFor(regionId: String) = "stations_$regionId.csv"

    private fun sampleFileNameFor(regionId: String) = "stations_${regionId}_sample.csv"

    private fun hasFullFile(regionId: String) = fileNameFor(regionId) in assetNames

    /** Number of regions with a full data file, not just the sample. */
    fun fullDataRegionCount(): Int =
        CaliforniaRegionalDefinitions.ALL_REGIONS.count { hasFullFile(it.id) }

    fun totalRegionCount(): Int = CaliforniaRegionalDefinitions.ALL_REGIONS.size

    fun datasetFor(regionId: String): StationDatasetInfo? {
        val region = CaliforniaRegionalDefinitions.ALL_REGIONS.firstOrNull { it.id == regionId }
            ?: return null
        if (hasFullFile(regionId)) {
            build(region.id, region.name, fileNameFor(regionId), sample = false)?.let { return it }
        }
        if (regionId in SAMPLE_REGION_IDS) {
            return build(region.id, region.name, sampleFileNameFor(regionId), sample = true)
        }
        return null
    }

    /** Bay Area / Sacramento data: the full file when bundled, else the built-in sample. */
    fun defaultDataset(): StationDatasetInfo =
        datasetFor(CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.id)
            ?: StationDatasetInfo(
                regionId = CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.id,
                fileName = sampleFileNameFor(CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.id),
                isSample = true,
                title = CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.name,
                subtitle = "No stations found",
                stationCount = 0,
                minElevationMeters = 0,
                maxElevationMeters = 0
            )

    private fun build(regionId: String, regionName: String, fileName: String, sample: Boolean): StationDatasetInfo? {
        val base = StationDatasetInfo(
            regionId = regionId,
            fileName = fileName,
            isSample = sample,
            title = regionName,
            subtitle = "",
            stationCount = 0,
            minElevationMeters = 0,
            maxElevationMeters = 0
        )
        val stations = parser.loadStations(base)
        if (stations.isEmpty()) return null
        val kind = if (sample) "Built-in sample" else "Bundled dataset"
        return base.copy(
            subtitle = "$kind • ${stations.size} stations",
            stationCount = stations.size,
            minElevationMeters = stations.minOf { it.elevationMeters },
            maxElevationMeters = stations.maxOf { it.elevationMeters }
        )
    }

    companion object {
        // Regions whose small sample file ships in app/src/main/assets
        private val SAMPLE_REGION_IDS = setOf(CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.id)
    }
}
