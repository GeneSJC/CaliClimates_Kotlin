package io.github.genesjc.microclimatefinder.ui.map

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.genesjc.microclimatefinder.data.local.ElevationHintPreferences
import io.github.genesjc.microclimatefinder.data.local.ElevationHintStore
import io.github.genesjc.microclimatefinder.data.local.LocalNewsRepository
import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.data.local.StationRepository
import io.github.genesjc.microclimatefinder.data.local.TerrainImage
import io.github.genesjc.microclimatefinder.data.local.TerrainMaps
import io.github.genesjc.microclimatefinder.data.remote.LiveObservationCache
import io.github.genesjc.microclimatefinder.data.remote.ObservationResult
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The Elevation view's ViewModel: what [MapViewModel] provides, plus the region's skylines, the selected
 * station's live reading and the "tap the line" hint.
 */
class ElevationViewModel(
    stationsOf: (StationDatasetInfo) -> List<MicroclimateStation>,
    loadTerrain: (String) -> TerrainImage?,
    cityNameOf: (MicroclimateStation) -> String,
    /** Loads a region's skylines (null when it has none). Called off the main thread. */
    private val loadSkyline: (String) -> List<FloatArray>?,
    /** The latest known reading for a station without fetching; null if never fetched. */
    private val peekReading: (String) -> ObservationResult?,
    private val fetchReading: suspend (String) -> ObservationResult,
    private val hintStore: ElevationHintStore,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : MapViewModel(stationsOf, loadTerrain, cityNameOf, io) {

    private val skylines = HashMap<String, MutableStateFlow<List<FloatArray>?>>()

    /** The region's real skylines for the distant ranges: null until loaded, or when it has none. */
    fun skyline(regionId: String): StateFlow<List<FloatArray>?> {
        skylines[regionId]?.let { return it }
        skylines.clear()
        return MutableStateFlow<List<FloatArray>?>(null).also { flow ->
            skylines[regionId] = flow
            viewModelScope.launch { flow.value = withContext(io) { loadSkyline(regionId) } }
        }
    }

    /** The station's latest known reading, without fetching. */
    fun knownReading(stationId: String): ObservationResult? = peekReading(stationId)

    /** The station's live reading (fetched when not fresh); [ObservationResult.NoReading] when it cannot report one. */
    suspend fun reading(station: MicroclimateStation): ObservationResult =
        if (station.canHaveLiveReading) fetchReading(station.id) else ObservationResult.NoReading

    /** Counts this visit to the view and says whether the hint in words should show. */
    fun startVisit(): Boolean {
        hintStore.countLaunch()
        return hintStore.shouldShow()
    }

    /** The user tapped the line, so the hint in words stops showing. */
    fun lineTapped() = hintStore.markLineTapped()

    companion object {
        /** Builds the real [ElevationViewModel] from the application context. */
        fun factory(context: Context): ViewModelProvider.Factory {
            val app = context.applicationContext
            return viewModelFactory {
                initializer {
                    ElevationViewModel(
                        stationsOf = { StationRepository.stations(app, it) },
                        loadTerrain = { TerrainMaps.load(app, it) },
                        cityNameOf = { LocalNewsRepository.getCleanCityName(it.id, it.name) },
                        loadSkyline = { TerrainMaps.loadSkyline(app, it) },
                        peekReading = { LiveObservationCache.peek(it) },
                        fetchReading = { LiveObservationCache.get(it) },
                        hintStore = ElevationHintPreferences(app)
                    )
                }
            }
        }
    }
}
