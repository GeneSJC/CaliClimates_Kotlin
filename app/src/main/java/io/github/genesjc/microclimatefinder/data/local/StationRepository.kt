package io.github.genesjc.microclimatefinder.data.local

import android.content.Context
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import java.util.concurrent.ConcurrentHashMap

/**
 * The stations of each dataset, read from assets once and kept for the life of the app. A region's pack is
 * unzipped and parsed the first time anyone asks (the dataset's own summary, Home, the maps, the Station List)
 * and every later ask is a lookup, so changing region no longer repeats that work several times on the main thread.
 * Only a successful (non-empty) read is kept, so a missing or damaged file is tried again next time.
 */
object StationRepository {
    private val cache = ConcurrentHashMap<String, List<MicroclimateStation>>()

    private fun key(dataset: StationDatasetInfo) = dataset.packFileName ?: dataset.fileName

    /** The dataset's stations if they were read already; null otherwise. Never reads a file. */
    fun peek(dataset: StationDatasetInfo): List<MicroclimateStation>? = cache[key(dataset)]

    /** The dataset's stations: from the cache, else read and parse them now (and keep them). */
    fun stations(context: Context, dataset: StationDatasetInfo): List<MicroclimateStation> =
        cache[key(dataset)] ?: StationCsvParser(context.applicationContext).loadStations(dataset).also {
            if (it.isNotEmpty()) cache[key(dataset)] = it
        }
}
