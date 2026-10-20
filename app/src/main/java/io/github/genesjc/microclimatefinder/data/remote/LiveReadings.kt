package io.github.genesjc.microclimatefinder.data.remote

import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The live readings the station cards show, as one observable value per station. It lives for the whole app, so a
 * load already under way finishes even if the screen that asked for it has gone.
 *
 * A reading is null until the first answer arrives ("Checking..."); a station that cannot report goes straight to
 * [ObservationResult.NoReading]. The fetch itself is [fetch]; the underlying cache decides when a reading is stale.
 */
class LiveReadings(
    private val scope: CoroutineScope,
    private val peek: (String) -> ObservationResult?,
    private val fetch: suspend (String) -> ObservationResult
) {
    private val flows = HashMap<String, MutableStateFlow<ObservationResult?>>()

    /**
     * The station's reading, starting a refresh each time it is asked for, so call it once per card shown (inside
     * a `remember` keyed to the station) and not on every recomposition. Call from the main thread.
     */
    fun flowFor(station: MicroclimateStation): StateFlow<ObservationResult?> {
        val flow = flows.getOrPut(station.id) { MutableStateFlow(peek(station.id)) }
        scope.launch {
            flow.value = if (station.canHaveLiveReading) fetch(station.id) else ObservationResult.NoReading
        }
        return flow
    }

    companion object {
        /** The app's one instance, over the real cache. A stop-gap until the app has a dependency container. */
        val shared: LiveReadings by lazy {
            LiveReadings(
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
                peek = { LiveObservationCache.peek(it) },
                fetch = { LiveObservationCache.get(it) }
            )
        }
    }
}
