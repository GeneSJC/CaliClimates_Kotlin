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
    val maxElevationMeters: Int,
    /** Set when the stations come from a station pack (a.txt to f.txt) instead of a CSV file. */
    val packFileName: String? = null
)

/**
 * Finds station data per region, first match wins:
 *  1. a raw CSV in assets (stations_<regionId>.csv, dropped into the gitignored app/src/debug/assets
 *     folder): the developer override, so fresh data can be tried without rebuilding the packs,
 *  2. the region's station pack (a.txt to f.txt in assets),
 *  3. the small built-in sample (stations_<regionId>_sample.csv), Bay Area only.
 * A region none of these can supply has no stations; the app shows its limited-data banner.
 */
class StationDataStore(private val context: Context) {

    private val parser = StationCsvParser(context)

    private val assetNames: Set<String> by lazy {
        try { context.assets.list("")?.toSet() ?: emptySet() } catch (e: Exception) { emptySet() }
    }

    private fun fileNameFor(regionId: String) = "stations_$regionId.csv"

    private fun sampleFileNameFor(regionId: String) = "stations_${regionId}_sample.csv"

    private fun hasCsvOverride(regionId: String) = fileNameFor(regionId) in assetNames

    private fun packFileFor(regionId: String): String? =
        PACK_FILES[regionId]?.takeIf { it in assetNames }

    /** Number of regions with full station data (a CSV override or a pack), not just the sample. */
    fun fullDataRegionCount(): Int =
        CaliforniaRegionalDefinitions.ALL_REGIONS.count { hasCsvOverride(it.id) || packFileFor(it.id) != null }

    fun totalRegionCount(): Int = CaliforniaRegionalDefinitions.ALL_REGIONS.size

    fun datasetFor(regionId: String): StationDatasetInfo? {
        val region = CaliforniaRegionalDefinitions.ALL_REGIONS.firstOrNull { it.id == regionId }
            ?: return null
        if (hasCsvOverride(regionId)) {
            build(region.id, region.name, fileNameFor(regionId), sample = false, kind = "CSV override")
                ?.let { return it }
        }
        packFileFor(regionId)?.let { pack ->
            build(region.id, region.name, pack, sample = false, kind = "Bundled dataset", packFile = pack)
                ?.let { return it }
        }
        if (regionId in SAMPLE_REGION_IDS) {
            return build(region.id, region.name, sampleFileNameFor(regionId), sample = true, kind = "Built-in sample")
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

    private fun build(
        regionId: String,
        regionName: String,
        fileName: String,
        sample: Boolean,
        kind: String,
        packFile: String? = null
    ): StationDatasetInfo? {
        val base = StationDatasetInfo(
            regionId = regionId,
            fileName = fileName,
            isSample = sample,
            title = regionName,
            subtitle = "",
            stationCount = 0,
            minElevationMeters = 0,
            maxElevationMeters = 0,
            packFileName = packFile
        )
        val stations = parser.loadStations(base)
        if (stations.isEmpty()) return null
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

        // Station pack per region, named by the app's region order: north to south, then west to east
        private val PACK_FILES = mapOf(
            CaliforniaRegionalDefinitions.NORTHERN_CALIFORNIA.id to "a.txt",
            CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.id to "b.txt",
            CaliforniaRegionalDefinitions.CENTRAL_COAST.id to "c.txt",
            CaliforniaRegionalDefinitions.CENTRAL_VALLEY.id to "d.txt",
            CaliforniaRegionalDefinitions.LOS_ANGELES.id to "e.txt",
            CaliforniaRegionalDefinitions.SAN_DIEGO.id to "f.txt"
        )
    }
}
