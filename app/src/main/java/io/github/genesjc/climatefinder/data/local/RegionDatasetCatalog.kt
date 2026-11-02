package io.github.genesjc.climatefinder.data.local

import io.github.genesjc.climatefinder.domain.model.MicroclimateStation

/** What the app asks of its station data: a dataset per region, the fallback one, and how many regions have data. */
interface RegionDatasetCatalog {
    /** The region's dataset, or null when it has none. Reads the station pack the first time, off the main thread. */
    suspend fun datasetFor(regionId: String): StationDatasetInfo?

    /** The region's dataset if it has already been read; never reads anything, so it is safe to call from the screen. */
    fun cachedDatasetFor(regionId: String): StationDatasetInfo?

    /** The default region's data, or [emptyDefaultDataset] if it cannot be loaded. Reads off the main thread. */
    suspend fun defaultDataset(): StationDatasetInfo

    /** An empty stand-in for the default region's data, for while [defaultDataset] has not been read (no reading). */
    fun emptyDefaultDataset(): StationDatasetInfo

    /** Number of regions with full station data (a CSV override or a pack), not just the sample. */
    fun fullDataRegionCount(): Int

    fun totalRegionCount(): Int

    /** The region's stations (empty when it has none), read off the main thread. */
    suspend fun stationsFor(regionId: String): List<MicroclimateStation>
}
