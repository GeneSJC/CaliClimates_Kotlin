package io.github.genesjc.microclimatefinder.ui.map

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.genesjc.microclimatefinder.data.local.LocalNewsRepository
import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.data.local.StationRepository
import io.github.genesjc.microclimatefinder.data.local.TerrainImage
import io.github.genesjc.microclimatefinder.data.local.TerrainSource
import io.github.genesjc.microclimatefinder.data.remote.LiveReadings
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.context.GlobalContext

/** The Topography map's ViewModel. Today it only needs what [MapViewModel] provides. */
class StationMapViewModel(
    stationsOf: (StationDatasetInfo) -> List<MicroclimateStation>,
    loadTerrain: (String) -> TerrainImage?,
    cityNameOf: (MicroclimateStation) -> String,
    readings: LiveReadings,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MapViewModel(stationsOf, loadTerrain, cityNameOf, readings, ioDispatcher) {

    companion object {
        /** Builds the real [StationMapViewModel] from the application context. */
        fun factory(context: Context): ViewModelProvider.Factory {
            // Until the ViewModels take their helpers with `by inject()`, the factory asks the container for them
            return viewModelFactory {
                initializer {
                    val koin = GlobalContext.get()
                    val stations = koin.get<StationRepository>()
                    val terrain = koin.get<TerrainSource>()
                    StationMapViewModel(
                        stationsOf = { stations.stations(it) },
                        loadTerrain = { terrain.load(it) },
                        cityNameOf = { LocalNewsRepository.getCleanCityName(it.id, it.name) },
                        readings = koin.get()
                    )
                }
            }
        }
    }
}
