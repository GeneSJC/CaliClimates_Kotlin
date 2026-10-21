package io.github.genesjc.microclimatefinder.ui.map

import androidx.lifecycle.viewModelScope
import io.github.genesjc.microclimatefinder.data.local.LocalNewsRepository
import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.data.local.StationRepository
import io.github.genesjc.microclimatefinder.data.local.TerrainImage
import io.github.genesjc.microclimatefinder.data.local.TerrainRepository
import io.github.genesjc.microclimatefinder.data.remote.LiveReadings
import io.github.genesjc.microclimatefinder.data.remote.ObservationResult
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.viewmodel.AppBaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject

/**
 * What both map views (Topography and Elevation) need from the data: a region's stations, its terrain picture,
 * the station's live reading for its card, and the name each station goes by on a label. A specialised base: it
 * extends [AppBaseViewModel] and holds data access for the map screens only. Its helpers come from the container.
 */
abstract class MapViewModel : AppBaseViewModel() {

    private val repository: StationRepository by inject()
    protected val terrains: TerrainRepository by inject()
    private val readings: LiveReadings by inject()

    private val terrainFlows = HashMap<String, MutableStateFlow<TerrainImage?>>()

    private val stationFlows = HashMap<String, MutableStateFlow<List<MicroclimateStation>>>()

    /**
     * The dataset's stations: empty until they are read, which is rare because Home reads the region's stations first
     * (a lookup then). The reading happens off the main thread. Ask once per dataset; the same flow comes back.
     */
    fun stations(dataset: StationDatasetInfo): StateFlow<List<MicroclimateStation>> {
        val key = dataset.packFileName ?: dataset.fileName
        stationFlows[key]?.let { return it }
        val known = repository.peek(dataset)
        return MutableStateFlow(known ?: emptyList()).also { flow ->
            stationFlows[key] = flow
            if (known == null) viewModelScope.launch { flow.value = repository.load(dataset) }
        }
    }

    /**
     * The region's terrain picture: null until it has loaded (the map shows a plain base meanwhile). Loaded once
     * per region and kept while the region stays the same; asking for another region drops the old picture, as
     * the decoded images are large.
     */
    fun terrain(regionId: String): StateFlow<TerrainImage?> {
        terrainFlows[regionId]?.let { return it }
        terrainFlows.clear()
        return MutableStateFlow<TerrainImage?>(null).also { flow ->
            terrainFlows[regionId] = flow
            viewModelScope.launch { flow.value = terrains.terrain(regionId) }
        }
    }

    /** The station's live reading for its card, null while it is being checked. Ask once per card shown. */
    fun reading(station: MicroclimateStation): StateFlow<ObservationResult?> = readings.flowFor(station)

    /** The name a station goes by on the maps' labels. */
    fun cityName(station: MicroclimateStation): String = LocalNewsRepository.getCleanCityName(station.id, station.name)
}
