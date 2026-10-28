package io.github.genesjc.climatefinder.data.local

import android.content.Context
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import io.github.genesjc.climatefinder.domain.model.StateUsaDefinition
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Where a dataset's stations come from. The screens turn this into words; the data layer only reports it. */
enum class DatasetKind { BUNDLED_PACK, CSV_OVERRIDE, NONE }

/** A station dataset for one region: its bundled station pack, or a CSV override. */
data class StationDatasetInfo(
    val regionId: String,
    val fileName: String,
    val kind: DatasetKind,
    val stationCount: Int,
    val minElevationMeters: Int,
    val maxElevationMeters: Int,
    /** Set when the stations come from a station pack (a.txt to f.txt) instead of a CSV file. */
    val packFileName: String? = null
)

/** What the app asks of its station data: a dataset per region, the fallback one, and how many regions have data. */
interface RegionDatasets {
    fun datasetFor(regionId: String): StationDatasetInfo?

    /** The default region's data, or an empty placeholder if it cannot be loaded. */
    fun defaultDataset(): StationDatasetInfo

    /** Number of regions with full station data (a CSV override or a pack), not just the sample. */
    fun fullDataRegionCount(): Int

    fun totalRegionCount(): Int

    /** The region's stations (empty when it has none), read off the main thread. */
    suspend fun stationsFor(regionId: String): List<MicroclimateStation>
}

/**
 * Finds station data per region, first match wins:
 *  1. a raw CSV in assets (stations_<regionId>.csv, dropped into the gitignored app/src/debug/assets
 *     folder): the developer override, so fresh data can be tried without rebuilding the packs,
 *  2. the region's station pack (a.txt to f.txt in assets).
 * A region neither can supply has no stations; the app shows its limited-data banner.
 */
class StationDataStore(
    private val listAssets: () -> Set<String>,
    private val repository: StationRepository,
    private val stateUsa: StateUsaDefinition,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : RegionDatasets {

    private val assetNames: Set<String> by lazy { listAssets() }

    private fun fileNameFor(regionId: String) = "stations_$regionId.csv"

    private fun hasCsvOverride(regionId: String) = fileNameFor(regionId) in assetNames

    private fun packFileFor(regionId: String): String? =
        stateUsa.stationPacks[regionId]?.takeIf { it in assetNames }

    override fun fullDataRegionCount(): Int =
        stateUsa.regions.count { hasCsvOverride(it.id) || packFileFor(it.id) != null }

    override suspend fun stationsFor(regionId: String): List<MicroclimateStation> = withContext(ioDispatcher) {
        datasetFor(regionId)?.let { repository.stations(it) } ?: emptyList()
    }

    override fun totalRegionCount(): Int = stateUsa.regions.size

    override fun datasetFor(regionId: String): StationDatasetInfo? {
        val region = stateUsa.region(regionId) ?: return null
        if (hasCsvOverride(regionId)) {
            build(region.id, fileNameFor(regionId), kind = DatasetKind.CSV_OVERRIDE)
                ?.let { return it }
        }
        packFileFor(regionId)?.let { pack ->
            build(region.id, pack, kind = DatasetKind.BUNDLED_PACK, packFile = pack)
                ?.let { return it }
        }
        return null
    }

    override fun defaultDataset(): StationDatasetInfo {
        val default = stateUsa.defaultRegion()
        return datasetFor(default.id)
            ?: StationDatasetInfo(
                regionId = default.id,
                fileName = stateUsa.stationPacks.getValue(default.id),
                kind = DatasetKind.NONE,
                stationCount = 0,
                minElevationMeters = 0,
                maxElevationMeters = 0,
                packFileName = stateUsa.stationPacks.getValue(default.id)
            )
    }

    private fun build(
        regionId: String,
        fileName: String,
        kind: DatasetKind,
        packFile: String? = null
    ): StationDatasetInfo? {
        val base = StationDatasetInfo(
            regionId = regionId,
            fileName = fileName,
            kind = kind,
            stationCount = 0,
            minElevationMeters = 0,
            maxElevationMeters = 0,
            packFileName = packFile
        )
        val stations = repository.stations(base)
        if (stations.isEmpty()) return null
        return base.copy(
            stationCount = stations.size,
            minElevationMeters = stations.minOf { it.elevationMeters },
            maxElevationMeters = stations.maxOf { it.elevationMeters }
        )
    }

    companion object {
        /** The store over the app's real assets; a missing or unreadable asset folder reads as no data. */
        fun fromAssets(context: Context, repository: StationRepository, stateUsa: StateUsaDefinition) = StationDataStore(
            listAssets = { try { context.assets.list("")?.toSet() ?: emptySet() } catch (e: Exception) { emptySet() } },
            repository = repository,
            stateUsa = stateUsa
        )
    }
}
