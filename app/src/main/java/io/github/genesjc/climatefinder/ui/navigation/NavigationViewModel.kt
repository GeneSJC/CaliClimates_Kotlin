package io.github.genesjc.climatefinder.ui.navigation

import io.github.genesjc.climatefinder.ui.common.AppBaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Which screen the app is on, and where Back goes from the Regional Sections screen. It lives in a ViewModel, not in a
 * composable, so the choice survives a rotation and every screen reads it from one place.
 */
class NavigationViewModel : AppBaseViewModel() {

    private val _screen = MutableStateFlow(AppScreen.LANDING)

    /** The screen being shown. */
    val screen: StateFlow<AppScreen> = _screen.asStateFlow()

    private val _regionsReturn = MutableStateFlow(AppScreen.LANDING)

    /** Where Back returns to from Regional Sections (the Home icon or Demos & Previews). */
    val regionsReturn: StateFlow<AppScreen> = _regionsReturn.asStateFlow()

    fun navigate(to: AppScreen) {
        _screen.value = to
    }

    /** Sets where Back goes from Regional Sections. */
    fun setRegionsReturn(from: AppScreen) {
        _regionsReturn.value = from
    }

    /**
     * Goes back from the current screen: from Regional Sections to where it was opened from; from a screen the edition
     * adds to that edition's own target ([editionBackTarget]); from anywhere else to the start screen.
     */
    fun back(editionBackTarget: (AppScreen) -> AppScreen?) {
        val current = _screen.value
        _screen.value = when {
            current == AppScreen.STATE_USA_REGIONS -> _regionsReturn.value
            else -> editionBackTarget(current) ?: AppScreen.LANDING
        }
    }
}
