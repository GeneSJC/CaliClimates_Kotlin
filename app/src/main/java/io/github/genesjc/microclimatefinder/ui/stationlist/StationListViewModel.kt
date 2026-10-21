package io.github.genesjc.microclimatefinder.ui.stationlist

import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.data.local.StationRepository
import io.github.genesjc.microclimatefinder.data.remote.LiveReadings
import io.github.genesjc.microclimatefinder.data.remote.ObservationResult
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.viewmodel.AppBaseViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject

/** What the Station List shows: the header text, the stations, which one is marked, and where the list opens. */
data class StationListUiState(
    val title: String,
    val subtitle: String,
    val stations: List<MicroclimateStation>,
    /** The station marked "Showing on map": the one selected on Home, else the first, as Home shows it. */
    val activeStationId: String?,
    /** The list opens with the active station in view. */
    val initialIndex: Int,
    /** The tag on the active card. */
    val selectedLabel: String,
    /** Set when the dataset has no stations; the screen shows it instead of the list. */
    val emptyMessage: String?,
    /** True while the stations are being read; the screen shows the header only. */
    val isLoading: Boolean = false
)

/**
 * Maps a region's station dataset and the station selected on Home to what the Station List shows.
 * The selection itself is owned by HomeViewModel; this class only reads it. Its helpers come from the container.
 */
class StationListViewModel : AppBaseViewModel() {

    private val repository: StationRepository by inject()
    private val readings: LiveReadings by inject()

    /** The station's live reading for its card, null while it is being checked. Ask once per card shown. */
    fun reading(station: MicroclimateStation): StateFlow<ObservationResult?> = readings.flowFor(station)

    private val stationFlows = HashMap<String, MutableStateFlow<List<MicroclimateStation>?>>()

    /** The dataset's stations, null while they are being read (off the main thread; usually already a lookup). */
    fun stations(dataset: StationDatasetInfo): StateFlow<List<MicroclimateStation>?> {
        val key = dataset.packFileName ?: dataset.fileName
        stationFlows[key]?.let { return it }
        val known = repository.peek(dataset)
        return MutableStateFlow(known).also { flow ->
            stationFlows[key] = flow
            if (known == null) viewModelScope.launch { flow.value = repository.load(dataset) }
        }
    }

    fun stateFor(dataset: StationDatasetInfo, stations: List<MicroclimateStation>?, selectedStationId: String?): StationListUiState {
        if (stations == null) {
            return StationListUiState(dataset.title, dataset.subtitle, emptyList(), null, 0, SELECTED_LABEL, null, isLoading = true)
        }
        val activeId = stations.firstOrNull { it.id == selectedStationId }?.id ?: stations.firstOrNull()?.id
        return StationListUiState(
            title = dataset.title,
            subtitle = dataset.subtitle,
            stations = stations,
            activeStationId = activeId,
            initialIndex = stations.indexOfFirst { it.id == activeId }.coerceAtLeast(0),
            selectedLabel = SELECTED_LABEL,
            emptyMessage = if (stations.isEmpty()) "No stations loaded from: ${dataset.fileName}" else null
        )
    }

    companion object {
        const val SELECTED_LABEL = "Showing on map"
    }
}
