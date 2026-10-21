package io.github.genesjc.microclimatefinder.ui.regionpicker

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.genesjc.microclimatefinder.data.local.StateTerrain
import io.github.genesjc.microclimatefinder.data.local.TerrainSource
import io.github.genesjc.microclimatefinder.viewmodel.AppBaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext

/**
 * The California region picker's data: the statewide terrain picture its map draws. It starts loading as soon as
 * the picker is first composed (the Home screen), so the first time it is opened the picture is already there.
 * [loadStateTerrain] runs off the main thread.
 */
class RegionPickerViewModel(
    private val loadStateTerrain: () -> StateTerrain?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AppBaseViewModel() {

    private val _stateTerrain = MutableStateFlow<StateTerrain?>(null)

    /** The statewide picture, null until it has loaded (the map draws a plain base meanwhile) or when there is none. */
    val stateTerrain: StateFlow<StateTerrain?> = _stateTerrain

    init {
        viewModelScope.launch { _stateTerrain.value = withContext(ioDispatcher) { loadStateTerrain() } }
    }

    companion object {
        /** Builds the real [RegionPickerViewModel] from the application context. */
        fun factory(context: Context): ViewModelProvider.Factory {
            // Until the ViewModels take their helpers with `by inject()`, the factory asks the container for them
            return viewModelFactory {
                initializer {
                    val terrain = GlobalContext.get().get<TerrainSource>()
                    RegionPickerViewModel(loadStateTerrain = { terrain.loadState() })
                }
            }
        }
    }
}
