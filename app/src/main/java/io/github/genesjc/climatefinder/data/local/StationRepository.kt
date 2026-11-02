package io.github.genesjc.climatefinder.data.local

import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.genesjc.climatefinder.domain.model.StationDatasetInfo

/**
 * The stations of each dataset, read once and kept for the life of the app. A region's pack is unzipped and parsed the
 * first time anyone asks (the dataset's own summary, Home, the maps, the Station List) and every later ask is a lookup,
 * so changing region does not repeat that work on the main thread. Only a successful (non-empty) read is kept, so a
 * missing or damaged file is tried again next time.
 */
class StationRepository(
    private val source: StationSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val cache = ConcurrentHashMap<String, List<MicroclimateStation>>()

    private fun key(dataset: StationDatasetInfo) = dataset.packFileName ?: dataset.fileName

    /** The dataset's stations if they were read already; null otherwise. Never reads a file. */
    fun peek(dataset: StationDatasetInfo): List<MicroclimateStation>? = cache[key(dataset)]

    /**
     * The dataset's stations without blocking the caller: a lookup if they were read already, else read and parsed on
     * the IO dispatcher (and kept). The ViewModels use this one, so none of them can parse a pack on the main thread.
     */
    suspend fun load(dataset: StationDatasetInfo): List<MicroclimateStation> =
        peek(dataset) ?: withContext(ioDispatcher) { stations(dataset) }

    /** The dataset's stations: from the cache, else read and parse them now (and keep them). Blocks while it reads. */
    fun stations(dataset: StationDatasetInfo): List<MicroclimateStation> =
        cache[key(dataset)] ?: source.loadStations(dataset).also {
            if (it.isNotEmpty()) cache[key(dataset)] = it
        }
}
