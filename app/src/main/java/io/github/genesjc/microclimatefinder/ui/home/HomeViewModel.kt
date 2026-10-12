package io.github.genesjc.microclimatefinder.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.genesjc.microclimatefinder.data.local.RegionPreferences
import io.github.genesjc.microclimatefinder.data.local.SelectedRegionStore
import io.github.genesjc.microclimatefinder.data.local.StationDataStore
import io.github.genesjc.microclimatefinder.data.local.StationRepository
import io.github.genesjc.microclimatefinder.data.remote.LiveObservationCache
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.ui.screens.DEFAULT_PIN_SCALE
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Everything Home shows that can change, in one immutable value. The screens read it and send events to
 * [HomeViewModel]; they do not hold or change any of it themselves.
 */
data class HomeUiState(
    /** The app-wide region, remembered between launches. */
    val selectedRegionId: String,
    /** Which map view Home shows: 0 = Topography, 1 = Elevation. */
    val tab: Int = 0,
    /** The station selected on Home, shared by both map views and kept across a trip to the Station List. */
    val selectedStationId: String? = null,
    /** The Topography map's dot size. */
    val pinScale: Float = DEFAULT_PIN_SCALE,
    /** Whether the maps' zoom slider is showing: one setting for both map views. */
    val sliderShown: Boolean = true,
    /** Whether the Region Info sheet is open; kept here so it survives a trip to the Station List. */
    val infoOpen: Boolean = false
)

/**
 * Owns Home's state and the work that follows from it.
 *
 * - State: [uiState] is a [StateFlow]; the UI collects it and calls the functions below for every change.
 * - Region: choosing a region saves it and clears the selected station.
 * - Live readings: whenever the region changes, the new region's featured stations are fetched. A newer region
 *   cancels the previous region's fetch ([collectLatest]).
 *
 * Its collaborators are passed in as plain values, so tests can supply fakes. [factory] builds the real ones.
 */
class HomeViewModel(
    private val regionStore: SelectedRegionStore,
    /** The stations of a region (empty when it has none). Called on [ioDispatcher]. */
    private val stationsForRegion: (String) -> List<MicroclimateStation>,
    /** Fetches live readings for a region's stations; cancelled when the region changes. */
    private val prefetch: suspend (List<MicroclimateStation>) -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(selectedRegionId = regionStore.load()))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            uiState.map { it.selectedRegionId }.distinctUntilChanged().collectLatest { regionId ->
                val stations = withContext(ioDispatcher) { stationsForRegion(regionId) }
                prefetch(stations)
            }
        }
    }

    /** Changes the app-wide region, remembers it, and clears the station chosen in the old one. */
    fun selectRegion(regionId: String) {
        regionStore.save(regionId)
        _uiState.update { it.copy(selectedRegionId = regionId, selectedStationId = null) }
    }

    fun selectStation(stationId: String?) = _uiState.update { it.copy(selectedStationId = stationId) }

    fun setTab(tab: Int) = _uiState.update { it.copy(tab = tab) }

    fun setPinScale(scale: Float) = _uiState.update { it.copy(pinScale = scale) }

    fun setSliderShown(shown: Boolean) = _uiState.update { it.copy(sliderShown = shown) }

    fun setInfoOpen(open: Boolean) = _uiState.update { it.copy(infoOpen = open) }

    companion object {
        /** Builds the real [HomeViewModel] from the application context. */
        fun factory(context: Context): ViewModelProvider.Factory {
            val app = context.applicationContext
            return viewModelFactory {
                initializer {
                    val store = StationDataStore(app)
                    HomeViewModel(
                        regionStore = RegionPreferences(app),
                        stationsForRegion = { regionId ->
                            store.datasetFor(regionId)?.let { StationRepository.stations(app, it) } ?: emptyList()
                        },
                        prefetch = { LiveObservationCache.prefetch(it) }
                    )
                }
            }
        }
    }
}
