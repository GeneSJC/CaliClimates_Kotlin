package io.github.genesjc.microclimatefinder.ui.stationlist

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.data.local.StationRepository
import io.github.genesjc.microclimatefinder.data.remote.LiveReadings
import io.github.genesjc.microclimatefinder.data.remote.ObservationResult
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.viewmodel.AppBaseViewModel
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.context.GlobalContext

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
    val emptyMessage: String?
)

/**
 * Maps a region's station dataset and the station selected on Home to what the Station List shows.
 * The selection itself is owned by HomeViewModel; this class only reads it.
 * [stationsFor] is a plain collaborator so tests can supply fakes; [factory] builds the real one.
 */
class StationListViewModel(
    private val stationsFor: (StationDatasetInfo) -> List<MicroclimateStation>,
    private val readings: LiveReadings
) : AppBaseViewModel() {

    /** The station's live reading for its card, null while it is being checked. Ask once per card shown. */
    fun reading(station: MicroclimateStation): StateFlow<ObservationResult?> = readings.flowFor(station)

    fun stateFor(dataset: StationDatasetInfo, selectedStationId: String?): StationListUiState {
        val stations = stationsFor(dataset)
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

        /** Builds the real [StationListViewModel] from the application context. */
        fun factory(context: Context): ViewModelProvider.Factory {
            // Until the ViewModels take their helpers with `by inject()`, the factory asks the container for them
            return viewModelFactory {
                initializer {
                    val koin = GlobalContext.get()
                    val stations = koin.get<StationRepository>()
                    StationListViewModel(
                        stationsFor = { stations.stations(it) },
                        readings = koin.get()
                    )
                }
            }
        }
    }
}
