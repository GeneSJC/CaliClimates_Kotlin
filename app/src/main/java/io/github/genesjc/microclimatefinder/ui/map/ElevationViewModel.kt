package io.github.genesjc.microclimatefinder.ui.map

import androidx.lifecycle.viewModelScope
import io.github.genesjc.microclimatefinder.data.local.AppSettings
import io.github.genesjc.microclimatefinder.data.remote.ObservationResult
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject

/**
 * The Elevation view's ViewModel: what [MapViewModel] provides, plus the region's skylines, the selected
 * station's live reading and the "tap the line" hint.
 */
class ElevationViewModel : MapViewModel() {

    private val settings: AppSettings by inject()

    private val skylines = HashMap<String, MutableStateFlow<List<FloatArray>?>>()

    /** The region's real skylines for the distant ranges: null until loaded, or when it has none. */
    fun skyline(regionId: String): StateFlow<List<FloatArray>?> {
        skylines[regionId]?.let { return it }
        skylines.clear()
        return MutableStateFlow<List<FloatArray>?>(null).also { flow ->
            skylines[regionId] = flow
            viewModelScope.launch { flow.value = terrains.skyline(regionId) }
        }
    }

    /** The station's latest known reading, without fetching. */
    fun knownReading(stationId: String): ObservationResult? = readings.peek(stationId)

    /** The station's live reading (fetched when not fresh); [ObservationResult.NoReading] when it cannot report one. */
    suspend fun latestReading(station: MicroclimateStation): ObservationResult =
        if (station.canHaveLiveReading) readings.get(station.id) else ObservationResult.NoReading

    /** Counts this visit to the view and says whether the hint in words should show. */
    fun startVisit(): Boolean {
        settings.countElevationHintLaunch()
        return settings.shouldShowElevationHint()
    }

    /** The user tapped the line, so the hint in words stops showing. */
    fun lineTapped() = settings.markElevationLineTapped()
}
