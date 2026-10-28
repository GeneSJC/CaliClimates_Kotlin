package io.github.genesjc.climatefinder.ui.root

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import io.github.genesjc.climatefinder.data.local.AppSettings
import io.github.genesjc.climatefinder.data.local.TerrainRepository
import io.github.genesjc.climatefinder.viewmodel.AppBaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject

/**
 * What sits above every screen: the dark or light look (remembered between launches) and the backdrop's relief
 * texture. The backdrop's region tint follows the region HomeViewModel owns, so it is not kept here.
 * Its helpers come from the dependency container.
 */
class AppRootViewModel : AppBaseViewModel() {

    private val settings: AppSettings by inject()
    private val terrains: TerrainRepository by inject()

    private val _darkMode = MutableStateFlow(settings.darkMode())
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _backdropTexture = MutableStateFlow<ImageBitmap?>(null)

    /** The relief picture for the light backdrop: null until it has loaded, or when there is none. */
    val backdropTexture: StateFlow<ImageBitmap?> = _backdropTexture.asStateFlow()

    init {
        viewModelScope.launch { _backdropTexture.value = terrains.backdropRelief() }
    }

    /** Switches the look and remembers the choice. */
    fun setDarkMode(dark: Boolean) {
        settings.saveDarkMode(dark)
        _darkMode.value = dark
    }
}
