package io.github.genesjc.climatefinder.data.local

import android.content.Context
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import io.github.genesjc.climatefinder.config.StateUsaDefinition
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import io.github.genesjc.climatefinder.domain.model.DatasetKind
import io.github.genesjc.climatefinder.domain.model.StationDatasetInfo

/**
 * Finds station data per region, first match wins:
 *  1. a raw CSV in assets (stations_<regionId>.csv, dropped into the gitignored app/src/debug/assets
 *     folder): the developer override, so fresh data can be tried without rebuilding the packs,
 *  2. the region's station pack (a.txt to f.txt in assets).
 * A region neither can supply has no stations; the app shows its limited-data banner.
 */
class AssetDatasetCatalog(
    private val listAssets: () -> Set<String>,
    private val repository: StationRepository,
    private val stateUsa: StateUsaDefinition,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : RegionDatasetCatalog {

    private val assetNames: Set<String> by lazy { listAssets() }

    /** The datasets read so far, by region id; a region with no data is not kept and is looked for again. */
    private val datasetCache = ConcurrentHashMap<String, StationDatasetInfo>()

    private fun fileNameFor(regionId: String) = "stations_$regionId.csv"

    private fun hasCsvOverride(regionId: String) = fileNameFor(regionId) in assetNames

    private fun packFileFor(regionId: String): String? =
        stateUsa.stationPacks[regionId]?.takeIf { it in assetNames }

    override fun fullDataRegionCount(): Int =
        stateUsa.regions.count { hasCsvOverride(it.id) || packFileFor(it.id) != null }

    override suspend fun stationsFor(regionId: String): List<MicroclimateStation> = withContext(ioDispatcher) {
        readDataset(regionId)?.let { repository.stations(it) } ?: emptyList()
    }

    override fun totalRegionCount(): Int = stateUsa.regions.size

    override suspend fun datasetFor(regionId: String): StationDatasetInfo? = withContext(ioDispatcher) { readDataset(regionId) }

    override fun cachedDatasetFor(regionId: String): StationDatasetInfo? = datasetCache[regionId]

    override suspend fun defaultDataset(): StationDatasetInfo = withContext(ioDispatcher) {
        readDataset(stateUsa.defaultRegion().id) ?: emptyDefaultDataset()
    }

    /** Blocks while the pack is read and parsed the first time: call it only from the IO dispatcher. */
    private fun readDataset(regionId: String): StationDatasetInfo? {
        datasetCache[regionId]?.let { return it }
        return findDataset(regionId)?.also { datasetCache[regionId] = it }
    }

    private fun findDataset(regionId: String): StationDatasetInfo? {
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

    override fun emptyDefaultDataset(): StationDatasetInfo {
        val default = stateUsa.defaultRegion()
        return StationDatasetInfo(
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
        /** The catalog over the app's real assets; a missing or unreadable asset folder reads as no data. */
        fun fromAssets(context: Context, repository: StationRepository, stateUsa: StateUsaDefinition) = AssetDatasetCatalog(
            listAssets = { try { context.assets.list("")?.toSet() ?: emptySet() } catch (e: Exception) { emptySet() } },
            repository = repository,
            stateUsa = stateUsa
        )
    }
}
