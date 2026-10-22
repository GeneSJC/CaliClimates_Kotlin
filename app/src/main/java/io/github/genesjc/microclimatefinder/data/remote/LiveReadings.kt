package io.github.genesjc.microclimatefinder.data.remote

import android.util.Log
import io.github.genesjc.microclimatefinder.data.AppScope
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

/**
 * The app's live readings: the one state holder for them (one instance, built by the dependency container, living in
 * the app-wide [scope]). It owns the readings and decides when one is stale, so a load already under way finishes
 * even if the screen that asked for it has gone, and the station cards, the Elevation chart and the region prefetch
 * all see the same value.
 *
 * - **One source of state:** [entries] holds every station's result with the time it was fetched, changed only through
 *   [update], so overlapping fetches add to each other's result and never overwrite it.
 * - **Freshness:** a reading is reused for 15 minutes (NWS stations report about hourly); a station with no usable
 *   reading or a failed request is not retried for 5. [now] is the clock; tests pass their own.
 * - **Visits:** [open] starts a visit to a region. It cancels the previous visit's prefetch, so leaving a region stops
 *   the requests still waiting. Readings themselves are kept across visits on purpose: they cost a request each.
 *
 * A station's reading is null until its first answer arrives ("Checking..."); a station that cannot report goes
 * straight to [ObservationResult.NoReading]. [source] is where readings come from; tests pass a fake.
 */
class LiveReadings(
    private val source: ObservationSource,
    private val scope: CoroutineScope = AppScope.scope,
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    private class Entry(val result: ObservationResult, val fetchedAtMs: Long) {
        fun isFresh(now: Long) = now - fetchedAtMs <
            if (result is ObservationResult.Reading) READING_TTL_MS else NEGATIVE_TTL_MS
    }

    private val entries = MutableStateFlow<Map<String, Entry>>(emptyMap())
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val gate = Semaphore(CONCURRENT_REQUESTS)
    private val flows = ConcurrentHashMap<String, StateFlow<ObservationResult?>>()
    private var visit: Job? = null

    /** Latest known result for a station without fetching; null if never fetched. */
    fun peek(stationId: String): ObservationResult? = entries.value[stationId]?.result

    /**
     * Fresh result, else fetch (at most [CONCURRENT_REQUESTS] requests at a time, and one at a time per station: a
     * second caller for the same station waits for the first and reuses its result).
     */
    suspend fun get(stationId: String): ObservationResult {
        fresh(stationId)?.let { return it }
        return locks.getOrPut(stationId) { Mutex() }.withLock {
            fresh(stationId) ?: gate.withPermit {
                val result = source.fetchLatest(stationId)
                entries.update { it + (stationId to Entry(result, now())) }
                result
            }
        }
    }

    private fun fresh(stationId: String): ObservationResult? =
        entries.value[stationId]?.takeIf { it.isFresh(now()) }?.result

    /**
     * The station's reading as one observable value, starting a refresh (which the freshness rule usually answers from
     * what is held) each time it is asked for, so call it once per card shown (inside a `remember` keyed to the
     * station) and not on every recomposition.
     */
    fun flowFor(station: MicroclimateStation): StateFlow<ObservationResult?> {
        if (!station.canHaveLiveReading) return CANNOT_REPORT
        val flow = flows.getOrPut(station.id) {
            entries.map { it[station.id]?.result }.distinctUntilChanged()
                .stateIn(scope, SharingStarted.Eagerly, peek(station.id))
        }
        scope.launch { get(station.id) }
        return flow
    }

    /**
     * Starts a visit to a region: fetches its featured stations in the background and cancels whatever the previous
     * visit was still fetching. Call from the main thread.
     */
    fun open(stations: List<MicroclimateStation>) {
        visit?.cancel()
        visit = scope.launch { prefetch(stations) }
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
        val got = targets.count { peek(it.id) is ObservationResult.Reading }
        Log.d(TAG, "prefetched ${targets.size} featured stations, $got readings, ${System.currentTimeMillis() - started} ms")
    }

    companion object {
        private const val TAG = "NwsCache"
        private const val READING_TTL_MS = 15 * 60_000L
        private const val NEGATIVE_TTL_MS = 5 * 60_000L

        /** Featured stations fetched when a region loads. The single ceiling; raise it if limits allow. */
        const val PREFETCH_LIMIT = 40
        private const val CONCURRENT_REQUESTS = 4

        private val CANNOT_REPORT: StateFlow<ObservationResult?> = MutableStateFlow(ObservationResult.NoReading)
    }
}
