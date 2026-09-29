package io.github.genesjc.microclimatefinder.data.remote

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory live readings, shared by the whole app. A reading is reused for 15 minutes (NWS stations
 * report about hourly); a station with no usable reading or a failed request is not retried for 5.
 * Compose screens can read [entries] and recompose as readings arrive.
 */
object LiveObservationCache {
    private const val TAG = "NwsCache"
    private const val READING_TTL_MS = 15 * 60_000L
    private const val NEGATIVE_TTL_MS = 5 * 60_000L

    /** Featured stations fetched when a region loads. The single ceiling; raise it if limits allow. */
    const val PREFETCH_LIMIT = 40
    private const val CONCURRENT_REQUESTS = 4

    private class Entry(val result: ObservationResult, val fetchedAtMs: Long) {
        fun isFresh(now: Long) = now - fetchedAtMs <
            if (result is ObservationResult.Reading) READING_TTL_MS else NEGATIVE_TTL_MS
    }

    private val entries = mutableStateMapOf<String, ObservationResult>()
    private val stamps = ConcurrentHashMap<String, Entry>()
    private val gate = Semaphore(CONCURRENT_REQUESTS)

    /** Latest known result for a station without fetching; null if never fetched. */
    fun peek(stationId: String): ObservationResult? = entries[stationId]

    /** Fresh cached result, else fetch (at most [CONCURRENT_REQUESTS] requests at a time). */
    suspend fun get(stationId: String): ObservationResult {
        stamps[stationId]?.takeIf { it.isFresh(System.currentTimeMillis()) }?.let { return it.result }
        return gate.withPermit {
            val result = NwsObservationClient.fetchLatest(stationId)
            stamps[stationId] = Entry(result, System.currentTimeMillis())
            entries[stationId] = result
            result
        }
    }

    /**
     * Fetches the region's featured stations, up to [PREFETCH_LIMIT], that can report temperature.
     * Cancelling the calling coroutine (leaving the region) stops the remaining requests.
     */
    suspend fun prefetch(stations: List<MicroclimateStation>) {
        val targets = stations.filter { it.isFeatured && it.canHaveLiveReading }.take(PREFETCH_LIMIT)
        if (targets.isEmpty()) return
        val started = System.currentTimeMillis()
        coroutineScope {
            targets.map { station -> async { get(station.id) } }.forEach { it.await() }
        }
        val got = targets.count { entries[it.id] is ObservationResult.Reading }
        Log.d(TAG, "prefetched ${targets.size} featured stations, $got readings, ${System.currentTimeMillis() - started} ms")
    }
}
