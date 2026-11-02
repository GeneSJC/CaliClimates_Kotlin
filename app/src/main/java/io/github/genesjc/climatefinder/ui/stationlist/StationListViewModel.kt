package io.github.genesjc.climatefinder.ui.stationlist

import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.domain.model.DatasetKind
import io.github.genesjc.climatefinder.domain.model.StationDatasetInfo
import io.github.genesjc.climatefinder.data.local.StationRepository
import io.github.genesjc.climatefinder.data.remote.LiveReadingsRepository
import io.github.genesjc.climatefinder.domain.model.ObservationResult
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import io.github.genesjc.climatefinder.viewmodel.AppBaseViewModel
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
    private val readings: LiveReadingsRepository by inject()

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
            return StationListUiState(titleFor(dataset), subtitleFor(dataset), emptyList(), null, 0, string(R.string.station_list_showing_on_map), null, isLoading = true)
        }
        val activeId = stations.firstOrNull { it.id == selectedStationId }?.id ?: stations.firstOrNull()?.id
        return StationListUiState(
            title = titleFor(dataset),
            subtitle = subtitleFor(dataset),
            stations = stations,
            activeStationId = activeId,
            initialIndex = stations.indexOfFirst { it.id == activeId }.coerceAtLeast(0),
            selectedLabel = string(R.string.station_list_showing_on_map),
            emptyMessage = if (stations.isEmpty()) string(R.string.station_list_empty, dataset.fileName) else null
        )
    }

    /** The dataset's region name, or its id when it is not one of the regions. */
    private fun titleFor(dataset: StationDatasetInfo): String =
        stateUsa.region(dataset.regionId)?.nameRes?.let { string(it) } ?: dataset.regionId

    /** Where the stations come from and how many there are. */
    private fun subtitleFor(dataset: StationDatasetInfo): String {
        val count = string.plural(R.plurals.station_count, dataset.stationCount, dataset.stationCount)
        return when (dataset.kind) {
            DatasetKind.BUNDLED_PACK -> string(R.string.dataset_subtitle, string(R.string.dataset_kind_bundled), count)
            DatasetKind.CSV_OVERRIDE -> string(R.string.dataset_subtitle, string(R.string.dataset_kind_csv), count)
            DatasetKind.NONE -> string(R.string.dataset_kind_none)
        }
    }
}
