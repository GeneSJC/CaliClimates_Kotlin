package io.github.genesjc.climatefinder.ui.root

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import io.github.genesjc.climatefinder.data.local.AppearanceSettings
import io.github.genesjc.climatefinder.data.local.TerrainRepository
import io.github.genesjc.climatefinder.ui.common.AppBaseViewModel
import io.github.genesjc.climatefinder.ui.theme.SHOW_RELIEF_TEXTURE
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

    private val settings: AppearanceSettings by inject()
    private val terrains: TerrainRepository by inject()

    private val _darkMode = MutableStateFlow(settings.darkMode())
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _backdropTexture = MutableStateFlow<Bitmap?>(null)

    /** The relief picture for the light backdrop: null until it has loaded, or when there is none. */
    val backdropTexture: StateFlow<Bitmap?> = _backdropTexture.asStateFlow()

    init {
        if (SHOW_RELIEF_TEXTURE) viewModelScope.launch { _backdropTexture.value = terrains.backdropRelief() }
    }

    /** Switches the look and remembers the choice. */
    fun setDarkMode(dark: Boolean) {
        settings.saveDarkMode(dark)
        _darkMode.value = dark
    }
}
