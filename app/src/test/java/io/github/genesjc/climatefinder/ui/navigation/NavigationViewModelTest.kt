package io.github.genesjc.climatefinder.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationViewModelTest {

    private val none: (AppScreen) -> AppScreen? = { null }

    @Test
    fun startsOnTheStartScreen() {
        val navigation = NavigationViewModel()
        assertEquals(AppScreen.LANDING, navigation.screen.value)
        assertEquals(AppScreen.LANDING, navigation.regionsReturn.value)
    }

    @Test
    fun navigateChangesTheScreen() {
        val navigation = NavigationViewModel()
        navigation.navigate(AppScreen.STATION_LIST)
        assertEquals(AppScreen.STATION_LIST, navigation.screen.value)
    }

    @Test
    fun backFromTheRegionalSectionsGoesWhereItWasOpenedFrom() {
        val navigation = NavigationViewModel()
        navigation.setRegionsReturn(AppScreen.DEMOS_AND_PREVIEWS)
        navigation.navigate(AppScreen.STATE_USA_REGIONS)
        navigation.back(none)
        assertEquals(AppScreen.DEMOS_AND_PREVIEWS, navigation.screen.value)
    }

    @Test
    fun backFromAnEditionScreenGoesToTheEditionsTarget() {
        val navigation = NavigationViewModel()
        navigation.navigate(AppScreen.UNIFIED_DRAGGABLE_MAP)
        navigation.back { screen -> if (screen == AppScreen.UNIFIED_DRAGGABLE_MAP) AppScreen.DEMOS_AND_PREVIEWS else null }
        assertEquals(AppScreen.DEMOS_AND_PREVIEWS, navigation.screen.value)
    }

    @Test
    fun backFromAnyOtherScreenGoesToTheStartScreen() {
        val navigation = NavigationViewModel()
        navigation.navigate(AppScreen.ELEVATION_PROFILE)
        navigation.back(none)
        assertEquals(AppScreen.LANDING, navigation.screen.value)
    }
}
