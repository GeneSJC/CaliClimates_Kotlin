package io.github.genesjc.microclimatefinder.ui.home

import androidx.lifecycle.viewModelScope
import io.github.genesjc.microclimatefinder.data.local.MapZoomStore
import io.github.genesjc.microclimatefinder.data.local.RegionDatasets
import io.github.genesjc.microclimatefinder.data.local.SelectedRegionStore
import io.github.genesjc.microclimatefinder.data.local.SelectedStationStore
import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.data.local.StationRepository
import io.github.genesjc.microclimatefinder.data.remote.LiveReadings
import io.github.genesjc.microclimatefinder.data.remote.ObservationResult
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegionalDefinitions
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.ui.components.MAX_MAP_ZOOM
import io.github.genesjc.microclimatefinder.ui.screens.DEFAULT_PIN_SCALE
import io.github.genesjc.microclimatefinder.viewmodel.AppBaseViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.inject

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

/** Something Home's screen does once, in answer to a change. */
sealed interface HomeEvent {
    /** The app-wide region changed: the screen gives a click so the change is felt as well as seen. */
    data object RegionChanged : HomeEvent
}

/**
 * Owns Home's state and the work that follows from it.
 *
 * - State: [uiState] is a [StateFlow]; the UI collects it and calls the functions below for every change.
 * - Region: choosing a region saves it and clears the selected station. The selected station and the map zoom
 *   are saved too, and come back on the next launch as long as they were set in the saved region.
 * - Live readings: whenever the region changes, it opens a visit to the new region in [LiveReadings], which fetches
 *   the region's featured stations and cancels the previous visit's fetch.
 *
 * Its helpers come from the dependency container with `by inject()`, so it has no constructor arguments.
 */
@OptIn(FlowPreview::class)
class HomeViewModel : AppBaseViewModel() {

    private val regionStore: SelectedRegionStore by inject()
    /** Where the last selected station is remembered, so the app reopens on it. */
    private val stationStore: SelectedStationStore by inject()
    /** Where the map zoom is remembered, so the app reopens at the same zoom. */
    private val zoomStore: MapZoomStore by inject()
    /** The station dataset of each region, and how many regions have data. */
    private val datasets: RegionDatasets by inject()
    private val stationRepository: StationRepository by inject()
    /** The live readings the station tile and card show; also fetches a region's featured stations when it is opened. */
    private val readings: LiveReadings by inject()

    private val _uiState = MutableStateFlow(
        regionStore.load().let { regionId ->
            HomeUiState(selectedRegionId = regionId, selectedStationId = stationStore.load(regionId))
        }
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Just the region, for what sits above Home (the backdrop's tint), so it does not follow every other change. */
    val selectedRegionId: StateFlow<String> = uiState
        .map { it.selectedRegionId }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.selectedRegionId)

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)

    /** One-off things the screen should do once (like the click on a region change); read by the one collector. */
    val events = _events.receiveAsFlow()

    /**
     * The map zoom shared by both map views. A plain value, not part of [uiState]: it changes on every frame of a
     * pinch, so only the maps follow it and nothing recomposes. Saved a moment after the last change.
     */
    var mapZoom: Float = zoomStore.load(_uiState.value.selectedRegionId).coerceIn(1f, MAX_MAP_ZOOM)
        private set

    private val zoomChanges = MutableSharedFlow<Float>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        viewModelScope.launch {
            zoomChanges.debounce(ZOOM_SAVE_DELAY_MS).collect { zoomStore.save(uiState.value.selectedRegionId, it) }
        }
        viewModelScope.launch {
            uiState.map { it.selectedRegionId }.distinctUntilChanged().collect { regionId ->
                readings.open(datasets.stationsFor(regionId))
            }
        }
        // Once the first region is on its way, read the other regions' stations in the background, so changing
        // region later finds them already parsed instead of unzipping a pack on the main thread mid-transition
        viewModelScope.launch {
            CaliforniaRegionalDefinitions.ALL_REGIONS.forEach { datasets.stationsFor(it.id) }
        }
    }

    /** Changes the app-wide region, remembers it, and clears (and forgets) the station chosen in the old one. */
    fun selectRegion(regionId: String) {
        val changed = regionId != _uiState.value.selectedRegionId
        regionStore.save(regionId)
        stationStore.save(regionId, null)
        mapZoom = 1f
        zoomStore.save(regionId, 1f)
        _uiState.update { it.copy(selectedRegionId = regionId, selectedStationId = null) }
        if (changed) _events.trySend(HomeEvent.RegionChanged)
    }

    /**
     * The region's stations, for Home's dropdown, Terrain Legend and station card. Usually already read (the region's
     * fetch and the warm-up of the others do it in the background), so it is a lookup; call it from remember, not per frame.
     */
    fun stations(regionId: String): List<MicroclimateStation> =
        datasets.datasetFor(regionId)?.let { stationRepository.peek(it) } ?: emptyList()

    /** The region's station dataset, or null when it has no data (Home then shows its limited-data banner). */
    fun datasetFor(regionId: String): StationDatasetInfo? = datasets.datasetFor(regionId)

    /** The Bay Area dataset, for the Station List when a region has none; read only when asked. */
    fun defaultDataset(): StationDatasetInfo = datasets.defaultDataset()

    fun fullDataRegionCount(): Int = datasets.fullDataRegionCount()

    fun totalRegionCount(): Int = datasets.totalRegionCount()

    /** The station's live reading for its tile and card, null while it is being checked. Ask once per station shown. */
    fun reading(station: MicroclimateStation): StateFlow<ObservationResult?> = readings.flowFor(station)

    fun selectStation(stationId: String?) {
        stationStore.save(_uiState.value.selectedRegionId, stationId)
        _uiState.update { it.copy(selectedStationId = stationId) }
    }

    fun setMapZoom(zoom: Float) {
        mapZoom = zoom
        zoomChanges.tryEmit(zoom)
    }

    fun setTab(tab: Int) = _uiState.update { it.copy(tab = tab) }

    fun setPinScale(scale: Float) = _uiState.update { it.copy(pinScale = scale) }

    fun setSliderShown(shown: Boolean) = _uiState.update { it.copy(sliderShown = shown) }

    fun setInfoOpen(open: Boolean) = _uiState.update { it.copy(infoOpen = open) }

    private companion object {
        const val ZOOM_SAVE_DELAY_MS = 500L
    }
}
