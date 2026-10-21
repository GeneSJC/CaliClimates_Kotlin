package io.github.genesjc.microclimatefinder.data.local

import android.content.Context
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegionalDefinitions

/** A station dataset for one CA region: its bundled station pack, or a CSV override. */
data class StationDatasetInfo(
    val regionId: String,
    val fileName: String,
    val title: String,
    val subtitle: String,
    val stationCount: Int,
    val minElevationMeters: Int,
    val maxElevationMeters: Int,
    /** Set when the stations come from a station pack (a.txt to f.txt) instead of a CSV file. */
    val packFileName: String? = null
)

/** What the app asks of its station data: a dataset per region, the fallback one, and how many regions have data. */
interface RegionDatasets {
    fun datasetFor(regionId: String): StationDatasetInfo?

    /** Bay Area / Sacramento data, or an empty placeholder if it cannot be loaded. */
    fun defaultDataset(): StationDatasetInfo

    /** Number of regions with full station data (a CSV override or a pack), not just the sample. */
    fun fullDataRegionCount(): Int

    fun totalRegionCount(): Int
}

/**
 * Finds station data per region, first match wins:
 *  1. a raw CSV in assets (stations_<regionId>.csv, dropped into the gitignored app/src/debug/assets
 *     folder): the developer override, so fresh data can be tried without rebuilding the packs,
 *  2. the region's station pack (a.txt to f.txt in assets).
 * A region neither can supply has no stations; the app shows its limited-data banner.
 */
class StationDataStore(
    private val context: Context,
    private val repository: StationRepository
) : RegionDatasets {

    private val assetNames: Set<String> by lazy {
        try { context.assets.list("")?.toSet() ?: emptySet() } catch (e: Exception) { emptySet() }
    }

    private fun fileNameFor(regionId: String) = "stations_$regionId.csv"

    private fun hasCsvOverride(regionId: String) = fileNameFor(regionId) in assetNames

    private fun packFileFor(regionId: String): String? =
        PACK_FILES[regionId]?.takeIf { it in assetNames }

    override fun fullDataRegionCount(): Int =
        CaliforniaRegionalDefinitions.ALL_REGIONS.count { hasCsvOverride(it.id) || packFileFor(it.id) != null }

    override fun totalRegionCount(): Int = CaliforniaRegionalDefinitions.ALL_REGIONS.size

    override fun datasetFor(regionId: String): StationDatasetInfo? {
        val region = CaliforniaRegionalDefinitions.ALL_REGIONS.firstOrNull { it.id == regionId }
            ?: return null
        if (hasCsvOverride(regionId)) {
            build(region.id, region.name, fileNameFor(regionId), kind = "CSV override")
                ?.let { return it }
        }
        packFileFor(regionId)?.let { pack ->
            build(region.id, region.name, pack, kind = "Bundled dataset", packFile = pack)
                ?.let { return it }
        }
        return null
    }

    override fun defaultDataset(): StationDatasetInfo {
        val bayArea = CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO
        return datasetFor(bayArea.id)
            ?: StationDatasetInfo(
                regionId = bayArea.id,
                fileName = PACK_FILES.getValue(bayArea.id),
                title = bayArea.name,
                subtitle = "No stations found",
                stationCount = 0,
                minElevationMeters = 0,
                maxElevationMeters = 0,
                packFileName = PACK_FILES.getValue(bayArea.id)
            )
    }

    private fun build(
        regionId: String,
        regionName: String,
        fileName: String,
        kind: String,
        packFile: String? = null
    ): StationDatasetInfo? {
        val base = StationDatasetInfo(
            regionId = regionId,
            fileName = fileName,
            title = regionName,
            subtitle = "",
            stationCount = 0,
            minElevationMeters = 0,
            maxElevationMeters = 0,
            packFileName = packFile
        )
        val stations = repository.stations(base)
        if (stations.isEmpty()) return null
        return base.copy(
            subtitle = "$kind • ${stations.size} stations",
            stationCount = stations.size,
            minElevationMeters = stations.minOf { it.elevationMeters },
            maxElevationMeters = stations.maxOf { it.elevationMeters }
        )
    }

    companion object {
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
