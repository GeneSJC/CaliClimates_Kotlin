package io.github.genesjc.microclimatefinder.data.local

import android.content.Context
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import java.util.concurrent.ConcurrentHashMap

/** Where a dataset's stations are read from (the app's assets; a fake in tests). */
interface StationSource {
    fun loadStations(dataset: StationDatasetInfo): List<MicroclimateStation>
}

/** Reads a dataset's stations from the station packs (or a CSV override) in the app's assets. */
class AssetStationSource(context: Context) : StationSource {
    private val parser = StationCsvParser(context.applicationContext)

    override fun loadStations(dataset: StationDatasetInfo): List<MicroclimateStation> = parser.loadStations(dataset)
}

/**
 * The stations of each dataset, read once and kept for the life of the app. A region's pack is unzipped and parsed the
 * first time anyone asks (the dataset's own summary, Home, the maps, the Station List) and every later ask is a lookup,
 * so changing region does not repeat that work on the main thread. Only a successful (non-empty) read is kept, so a
 * missing or damaged file is tried again next time.
 */
class StationRepository(private val source: StationSource) {
    private val cache = ConcurrentHashMap<String, List<MicroclimateStation>>()

    private fun key(dataset: StationDatasetInfo) = dataset.packFileName ?: dataset.fileName

    /** The dataset's stations if they were read already; null otherwise. Never reads a file. */
    fun peek(dataset: StationDatasetInfo): List<MicroclimateStation>? = cache[key(dataset)]

    /** The dataset's stations: from the cache, else read and parse them now (and keep them). */
    fun stations(dataset: StationDatasetInfo): List<MicroclimateStation> =
        cache[key(dataset)] ?: source.loadStations(dataset).also {
            if (it.isNotEmpty()) cache[key(dataset)] = it
        }
}
