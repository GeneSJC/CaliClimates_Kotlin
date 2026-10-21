package io.github.genesjc.microclimatefinder.ui.regionpicker

import androidx.lifecycle.viewModelScope
import io.github.genesjc.microclimatefinder.data.local.StateTerrain
import io.github.genesjc.microclimatefinder.data.local.TerrainRepository
import io.github.genesjc.microclimatefinder.viewmodel.AppBaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject

/**
 * The California region picker's data: the statewide terrain picture its map draws. It starts loading as soon as
 * the picker is first composed (the Home screen), so the first time it is opened the picture is already there.
 */
class RegionPickerViewModel : AppBaseViewModel() {

    private val terrains: TerrainRepository by inject()

    private val _stateTerrain = MutableStateFlow<StateTerrain?>(null)

    /** The statewide picture, null until it has loaded (the map draws a plain base meanwhile) or when there is none. */
    val stateTerrain: StateFlow<StateTerrain?> = _stateTerrain

    init {
        viewModelScope.launch { _stateTerrain.value = terrains.stateTerrain() }
    }
}
