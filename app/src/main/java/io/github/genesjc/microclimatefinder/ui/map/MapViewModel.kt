package io.github.genesjc.microclimatefinder.ui.map

import androidx.lifecycle.viewModelScope
import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.data.local.TerrainImage
import io.github.genesjc.microclimatefinder.data.remote.LiveReadings
import io.github.genesjc.microclimatefinder.data.remote.ObservationResult
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.viewmodel.AppBaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * What both map views (Topography and Elevation) need from the data: a region's stations, its terrain picture
 * and the name each station goes by on a label. A specialised base: it extends [AppBaseViewModel] and holds
 * data access for the map screens only.
 *
 * Collaborators are plain values so tests can supply fakes; the subclasses' factories build the real ones.
 */
abstract class MapViewModel(
    private val stationsOf: (StationDatasetInfo) -> List<MicroclimateStation>,
    /** Loads a region's terrain picture (null when it has none). Called off the main thread. */
    private val loadTerrain: (String) -> TerrainImage?,
    private val cityNameOf: (MicroclimateStation) -> String,
    private val readings: LiveReadings,
    private val ioDispatcher: CoroutineDispatcher
) : AppBaseViewModel() {

    private val terrains = HashMap<String, MutableStateFlow<TerrainImage?>>()

    fun stationsFor(dataset: StationDatasetInfo): List<MicroclimateStation> = stationsOf(dataset)

    /**
     * The region's terrain picture: null until it has loaded (the map shows a plain base meanwhile). Loaded once
     * per region and kept while the region stays the same; asking for another region drops the old picture, as
     * the decoded images are large.
     */
    fun terrain(regionId: String): StateFlow<TerrainImage?> {
        terrains[regionId]?.let { return it }
        terrains.clear()
        return MutableStateFlow<TerrainImage?>(null).also { flow ->
            terrains[regionId] = flow
            viewModelScope.launch { flow.value = withContext(ioDispatcher) { loadTerrain(regionId) } }
        }
    }

    /** The station's live reading for its card, null while it is being checked. Ask once per card shown. */
    fun reading(station: MicroclimateStation): StateFlow<ObservationResult?> = readings.flowFor(station)

    /** The name a station goes by on the maps' labels. */
    fun cityName(station: MicroclimateStation): String = cityNameOf(station)
}
