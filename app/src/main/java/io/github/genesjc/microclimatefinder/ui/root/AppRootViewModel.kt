package io.github.genesjc.microclimatefinder.ui.root

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.genesjc.microclimatefinder.data.local.DarkModeStore
import io.github.genesjc.microclimatefinder.data.local.TerrainSource
import io.github.genesjc.microclimatefinder.viewmodel.AppBaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext

/**
 * What sits above every screen: the dark or light look (remembered between launches) and the backdrop's relief
 * texture. The backdrop's region tint follows the region HomeViewModel owns, so it is not kept here.
 * Collaborators are plain values so tests can supply fakes; [factory] builds the real ones.
 */
class AppRootViewModel(
    private val darkModeStore: DarkModeStore,
    /** Loads the backdrop's relief picture (null when it is missing). Called off the main thread. */
    private val loadBackdropTexture: () -> ImageBitmap?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AppBaseViewModel() {

    private val _darkMode = MutableStateFlow(darkModeStore.darkMode())
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _backdropTexture = MutableStateFlow<ImageBitmap?>(null)

    /** The relief picture for the light backdrop: null until it has loaded, or when there is none. */
    val backdropTexture: StateFlow<ImageBitmap?> = _backdropTexture.asStateFlow()

    init {
        viewModelScope.launch { _backdropTexture.value = withContext(ioDispatcher) { loadBackdropTexture() } }
    }

    /** Switches the look and remembers the choice. */
    fun setDarkMode(dark: Boolean) {
        darkModeStore.saveDarkMode(dark)
        _darkMode.value = dark
    }

    companion object {
        /** Builds the real [AppRootViewModel] from the application context. */
        fun factory(context: Context): ViewModelProvider.Factory {
            // Until the ViewModels take their helpers with `by inject()`, the factory asks the container for them
            return viewModelFactory {
                initializer {
                    val koin = GlobalContext.get()
                    val terrain = koin.get<TerrainSource>()
                    AppRootViewModel(
                        darkModeStore = koin.get(),
                        loadBackdropTexture = { terrain.loadBackdropRelief() }
                    )
                }
            }
        }
    }
}
