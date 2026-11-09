package io.github.genesjc.climatefinder.ui.home

import androidx.lifecycle.viewModelScope
import io.github.genesjc.climatefinder.data.local.RegionSettings
import io.github.genesjc.climatefinder.data.local.RegionDatasetCatalog
import io.github.genesjc.climatefinder.domain.model.StationDatasetInfo
import io.github.genesjc.climatefinder.data.local.StationRepository
import io.github.genesjc.climatefinder.data.remote.LiveReadingsRepository
import io.github.genesjc.climatefinder.domain.model.ObservationResult
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import io.github.genesjc.climatefinder.ui.components.MAX_MAP_ZOOM
import io.github.genesjc.climatefinder.ui.screens.DEFAULT_PIN_SCALE
import io.github.genesjc.climatefinder.ui.common.AppBaseViewModel
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
import kotlinx.coroutines.Job
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
    /** The stations selected before this one, oldest first, for the card's Back button; kept for the whole run and across both map views, cleared when the region changes. */
    val stationHistory: List<String> = emptyList(),
    /** The stations gone back from, most recent last, for the card's Forward button; cleared by a new pick and by a region change. */
    val stationForward: List<String> = emptyList(),
    /** The Topography map's dot size. */
    val pinScale: Float = DEFAULT_PIN_SCALE,
    /** Whether the maps' zoom slider is showing: one setting for both map views. */
    val sliderShown: Boolean = true,
    /** Whether the Region Info sheet is open; kept here so it survives a trip to the Station List. */
    val infoOpen: Boolean = false,
    /** Whether the Region Info sheet's "More" card (the region's description) is open: kept for the app's run, so the sheet opens as it was left. */
    val regionDetailsOpen: Boolean = false,
    /** Whether the Region Info sheet's Terrain Legend is open: kept for the app's run, like [regionDetailsOpen]. */
    val legendOpen: Boolean = false,
    /** The station dataset of [selectedRegionId]; null when the region has no data (Home then shows its limited-data banner). */
    val regionDataset: StationDatasetInfo? = null,
    /** True while the first region's stations are being read at start-up; Home shows a loading view until they are in. */
    val loading: Boolean = true,
    /** How many of the state's regions have station data in this build, and how many regions there are: facts that do not change in a run. */
    val fullDataRegions: Int = 0,
    val totalRegions: Int = 0,
    /**
     * The default region's dataset, for the Station List when a region has none. An empty stand-in until a region turns out to
     * have no data; then the view model reads the real one (off the main thread) and puts it here.
     */
    val defaultDataset: StationDatasetInfo
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
 * - Region: the saved region's stations are read off the main thread when Home starts ([HomeUiState.loading] is true until
 *   they are in). Choosing a region reads its stations off the main thread too, then saves it and clears the selected
 *   station. The selected station and the map zoom are saved too, and come back on the next launch as long as they were
 *   set in the saved region.
 * - Live readings: whenever the region changes, it opens a visit to the new region in [LiveReadingsRepository], which fetches
 *   the region's featured stations and cancels the previous visit's fetch.
 *
 * Its helpers come from the dependency container with `by inject()`, so it has no constructor arguments.
 */
@OptIn(FlowPreview::class)
class HomeViewModel : AppBaseViewModel() {

    /** What is remembered between launches: the region, and the station and map zoom set in it. */
    private val settings: RegionSettings by inject()
    /** The station dataset of each region, and how many regions have data. */
    private val datasets: RegionDatasetCatalog by inject()
    private val stationRepository: StationRepository by inject()
    /** The live readings the station tile and card show; also fetches a region's featured stations when it is opened. */
    private val readings: LiveReadingsRepository by inject()

    private val _uiState = MutableStateFlow(
        settings.selectedRegionId().let { regionId ->
            HomeUiState(
                selectedRegionId = regionId,
                tab = settings.homeTab(),
                selectedStationId = settings.selectedStationId(regionId),
                fullDataRegions = datasets.fullDataRegionCount(),
                totalRegions = datasets.totalRegionCount(),
                defaultDataset = datasets.emptyDefaultDataset()
            )
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
    var mapZoom: Float = settings.mapZoom(_uiState.value.selectedRegionId).coerceIn(1f, MAX_MAP_ZOOM)
        private set

    private val zoomChanges = MutableSharedFlow<Float>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** The region being read (at start-up, or after a choice); a newer choice replaces it. */
    private var regionLoad: Job? = null

    /** The read of the default region's dataset, started once the first region with no data shows up. */
    private var defaultDatasetLoad: Job? = null

    init {
        viewModelScope.launch {
            zoomChanges.debounce(ZOOM_SAVE_DELAY_MS).collect { settings.saveMapZoom(uiState.value.selectedRegionId, it) }
        }
        viewModelScope.launch {
            uiState.map { it.selectedRegionId }.distinctUntilChanged().collect { regionId ->
                readings.open(datasets.stationsFor(regionId))
            }
        }
        // Read the saved region's stations off the main thread; Home shows its loading view until they are in
        regionLoad = viewModelScope.launch {
            val regionId = _uiState.value.selectedRegionId
            val dataset = readRegion(regionId)
            _uiState.update { it.copy(regionDataset = dataset, loading = false) }
            readDefaultDatasetIfNeeded(dataset)
        }
        // Once the first region is on its way, read the other regions' stations in the background, so changing
        // region later finds them already parsed instead of unzipping a pack on the main thread mid-transition
        viewModelScope.launch {
            stateUsa.regions.forEach { datasets.stationsFor(it.id) }
        }
    }

    /** Reads the region's stations and its dataset summary off the main thread, so the screen only looks them up. */
    private suspend fun readRegion(regionId: String): StationDatasetInfo? {
        datasets.stationsFor(regionId)
        return datasets.datasetFor(regionId)
    }

    /**
     * Changes the app-wide region, remembers it, and clears (and forgets) the station chosen in the old one. A region
     * whose stations have not been read yet is read first, off the main thread, and the screen switches when they are
     * ready: switching first would make the screen read the region's pack on the main thread, which on a cold start (the
     * first run after an install, before the background warm-up has reached that region) freezes the app. Choosing
     * another region while one is still loading replaces it.
     */
    fun selectRegion(regionId: String) {
        regionLoad?.cancel()
        val current = _uiState.value
        if (regionId == current.selectedRegionId && !current.loading) {
            applyRegion(regionId, current.regionDataset, changed = false)
            return
        }
        regionLoad = viewModelScope.launch {
            val dataset = readRegion(regionId)
            applyRegion(regionId, dataset, changed = regionId != _uiState.value.selectedRegionId)
        }
    }

    private fun applyRegion(regionId: String, dataset: StationDatasetInfo?, changed: Boolean) {
        settings.saveSelectedRegionId(regionId)
        settings.saveSelectedStationId(regionId, null)
        mapZoom = 1f
        settings.saveMapZoom(regionId, 1f)
        _uiState.update { it.copy(selectedRegionId = regionId, selectedStationId = null, stationHistory = emptyList(), stationForward = emptyList(), regionDataset = dataset, loading = false) }
        readDefaultDatasetIfNeeded(dataset)
        if (changed) _events.trySend(HomeEvent.RegionChanged)
    }

    /**
     * The region's stations, for Home's dropdown, Terrain Legend and station card. Already read once Home has loaded (the
     * region's own read and the warm-up of the others do it in the background), so it is a lookup that never reads a
     * pack; call it from remember, not per frame.
     */
    fun stations(regionId: String): List<MicroclimateStation> =
        datasets.cachedDatasetFor(regionId)?.let { stationRepository.peek(it) } ?: emptyList()

    /**
     * A region with no data needs the default region's dataset (the Station List then shows it): it is read once, the first time
     * a region turns out to have none, off the main thread, and put in [HomeUiState.defaultDataset].
     */
    private fun readDefaultDatasetIfNeeded(regionDataset: StationDatasetInfo?) {
        if (regionDataset != null || defaultDatasetLoad != null) return
        defaultDatasetLoad = viewModelScope.launch {
            val dataset = datasets.defaultDataset()
            _uiState.update { it.copy(defaultDataset = dataset) }
        }
    }

    /**
     * The stations known to have no reading right now (they answered with nothing usable), for the Region Info legend's
     * ring count. It is kept from the readings the app already fetches, so watching it asks for nothing more.
     */
    val noReadingIds: StateFlow<Set<String>> by lazy {
        readings.noReadingIds().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    }

    /** The station's live reading for its tile and card, null while it is being checked. Ask once per station shown. */
    fun reading(station: MicroclimateStation): StateFlow<ObservationResult?> = readings.flowFor(station)

    /**
     * The station the screens are showing: the selected one, or, when none is selected (nothing saved, or a saved station
     * the region no longer has), the region's first station, which is what the map, the tile and the card fall back to. The
     * history counts this one too, or the station Home opened on would be missing from Back.
     */
    private fun shownStationId(state: HomeUiState): String? {
        val stations = stations(state.selectedRegionId)
        val selected = state.selectedStationId
        // Stations not read yet: trust the selection
        return if (selected != null && (stations.isEmpty() || stations.any { it.id == selected })) selected else stations.firstOrNull()?.id
    }

    fun selectStation(stationId: String?) {
        settings.saveSelectedStationId(_uiState.value.selectedRegionId, stationId)
        _uiState.update { state ->
            val current = shownStationId(state)
            // The station being left goes on the history, so a wrong tap can be undone (see goBackStation). The history is
            // Home's, so it is the same whichever map view the pick was made on, and it is kept for the whole run
            val history = if (current != null && stationId != null && stationId != current) {
                state.stationHistory + current
            } else {
                state.stationHistory
            }
            // A new pick starts a new path, as in a browser: what could have been gone forward to is dropped
            val forward = if (stationId != current) emptyList() else state.stationForward
            state.copy(selectedStationId = stationId, stationHistory = history, stationForward = forward)
        }
    }

    /** Goes back to the station selected before this one, if there is one; the one left is kept to go forward to (it is a step, not a new pick). */
    fun goBackStation() {
        val state = _uiState.value
        val previous = state.stationHistory.lastOrNull() ?: return
        settings.saveSelectedStationId(state.selectedRegionId, previous)
        _uiState.update {
            it.copy(
                selectedStationId = previous,
                stationHistory = it.stationHistory.dropLast(1),
                stationForward = shownStationId(it)?.let { left -> it.stationForward + left } ?: it.stationForward
            )
        }
    }

    /** Goes forward again to the station gone back from, if there is one. */
    fun goForwardStation() {
        val state = _uiState.value
        val next = state.stationForward.lastOrNull() ?: return
        settings.saveSelectedStationId(state.selectedRegionId, next)
        _uiState.update {
            it.copy(
                selectedStationId = next,
                stationForward = it.stationForward.dropLast(1),
                stationHistory = shownStationId(it)?.let { left -> it.stationHistory + left } ?: it.stationHistory
            )
        }
    }

    fun setMapZoom(zoom: Float) {
        mapZoom = zoom
        zoomChanges.tryEmit(zoom)
    }

    fun setTab(tab: Int) {
        _uiState.update { it.copy(tab = tab) }
        settings.saveHomeTab(tab)
    }

    fun setPinScale(scale: Float) = _uiState.update { it.copy(pinScale = scale) }

    fun setSliderShown(shown: Boolean) = _uiState.update { it.copy(sliderShown = shown) }

    fun setInfoOpen(open: Boolean) = _uiState.update { it.copy(infoOpen = open) }

    fun setRegionDetailsOpen(open: Boolean) = _uiState.update { it.copy(regionDetailsOpen = open) }

    fun setLegendOpen(open: Boolean) = _uiState.update { it.copy(legendOpen = open) }

    private companion object {
        const val ZOOM_SAVE_DELAY_MS = 500L
    }
}
