package io.github.genesjc.climatefinder.data.remote

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import io.github.genesjc.climatefinder.domain.model.ObservationResult

/**
 * Holds each station's latest result with the time it was received. It stores and reports; whether a result is still
 * usable is [ReadingFreshness]'s rule, applied here against [now] (tests pass their own clock).
 *
 * The map changes only through [put], which adds to the current map, so overlapping stores never overwrite each other.
 */
class ReadingCache(private val now: () -> Long = { System.currentTimeMillis() }) {

    private class Entry(val result: ObservationResult, val receivedAtMs: Long)

    private val entries = MutableStateFlow<Map<String, Entry>>(emptyMap())

    /** The latest result for a station whatever its age; null if none was stored. */
    fun peek(stationId: String): ObservationResult? = entries.value[stationId]?.result

    /** The station's result if it is still fresh; null when none was stored or it has expired. */
    fun fresh(stationId: String): ObservationResult? =
        entries.value[stationId]
            ?.takeIf { ReadingFreshness.isFresh(it.result, now() - it.receivedAtMs) }
            ?.result

    /** Stores a result, stamped with the time it was received. */
    fun put(stationId: String, result: ObservationResult) {
        entries.update { it + (stationId to Entry(result, now())) }
    }

    /** A station's result as an observable value: null until one is stored, then each change. */
    fun changes(stationId: String): Flow<ObservationResult?> =
        entries.map { it[stationId]?.result }.distinctUntilChanged()

    /**
     * The ids of the stations whose latest result is [ObservationResult.NoReading] (it answered, with nothing usable), as an
     * observable set. A failed request is not one: it is temporary, and the station may well have a reading next time.
     */
    fun idsWithNoReading(): Flow<Set<String>> =
        entries.map { all -> all.filterValues { it.result is ObservationResult.NoReading }.keys.toSet() }.distinctUntilChanged()
}
