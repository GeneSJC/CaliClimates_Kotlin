package io.github.genesjc.climatefinder.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.genesjc.climatefinder.ui.theme.LocalStateUsa
import io.github.genesjc.climatefinder.ui.screens.StateUsaRegionsMapScreen
import io.github.genesjc.climatefinder.ui.components.LoadingPane
import io.github.genesjc.climatefinder.ui.home.HomeEvent
import io.github.genesjc.climatefinder.ui.home.HomeViewModel
import io.github.genesjc.climatefinder.ui.screens.LandingScreen
import io.github.genesjc.climatefinder.ui.screens.RegionOption
import io.github.genesjc.climatefinder.ui.screens.StationListScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.climatefinder.editionBackTarget
import io.github.genesjc.climatefinder.EditionScreens
import io.github.genesjc.climatefinder.EditionNavigation
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

@Composable
fun AppNavigation(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    homeViewModel: HomeViewModel,
    navigation: NavigationViewModel = viewModel()
) {
    // Which screen is showing, and where Back returns to from Regional Sections (Home icon or Demos & Previews): both live in the ViewModel
    val currentScreen by navigation.screen.collectAsStateWithLifecycle()
    val regionsReturnScreen by navigation.regionsReturn.collectAsStateWithLifecycle()
    // Home's state (region, map view, selected station, dot size, slider, info sheet) lives in the ViewModel
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val noReadingIds by homeViewModel.noReadingIds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val selectedRegionId = homeState.selectedRegionId
    // A click when the region changes (not when the app first opens), so the change is felt as well as seen
    LaunchedEffect(homeViewModel) {
        homeViewModel.events.collect { event ->
            when (event) {
                HomeEvent.RegionChanged -> io.github.genesjc.climatefinder.ui.components.RegionHaptics.tick(context)
            }
        }
    }

    // Station data per region: a CSV override in assets, else the region's bundled pack. HomeViewModel reads the
    // region's pack off the main thread (homeState.loading is true until it is in) and keeps its dataset in the state
    val fullDataRegions = homeState.fullDataRegions
    val regionDataset = homeState.regionDataset
    // The Bay Area fallback dataset is only needed by the Station List when a region has none, and by the demos; the view
    // model reads it (off the main thread) once a region turns out to have no data, and keeps an empty stand-in until then
    val defaultDataset = homeState.defaultDataset
    // Changes the app-wide region and remembers it (Home, Topo Map and Station Feed pickers use this)
    fun pickRegion(regionId: String) = homeViewModel.selectRegion(regionId)
    // Rows for the quick region list on Home: names only (reading every region's stations just for a count slowed the
    // start-up by seconds, and the list no longer shows counts)
    val stateUsa = LocalStateUsa.current
    val regionOptions = remember(stateUsa) {
        stateUsa.regions.map { r -> RegionOption(r.id, r.nameRes, null) }
    }

    // (Live readings for the region's featured stations are fetched by HomeViewModel whenever the region changes.)

    // Intercept hardware/gesture back press when not on Landing
    if (currentScreen != AppScreen.LANDING) {
        BackHandler { navigation.back(::editionBackTarget) }
    }

    when (currentScreen) {
        AppScreen.LANDING -> if (homeState.loading) {
            // The first region's stations are being read off the main thread
            LoadingPane()
        } else {
            LandingScreen(
                selectedRegionId = selectedRegionId,
                dataset = regionDataset,
                fullDataRegions = fullDataRegions,
                totalRegions = homeState.totalRegions,
                buildLabel = homeViewModel.buildLabel,
                regionOptions = regionOptions,
                onSelectRegion = { pickRegion(it) },
                onNavigateTo = { targetScreen ->
                    if (targetScreen == AppScreen.STATE_USA_REGIONS) navigation.setRegionsReturn(AppScreen.LANDING)
                    navigation.navigate(targetScreen)
                },
                infoOpen = homeState.infoOpen,
                onInfoOpenChange = homeViewModel::setInfoOpen,
                regionDetailsOpen = homeState.regionDetailsOpen,
                onRegionDetailsOpenChange = homeViewModel::setRegionDetailsOpen,
                legendOpen = homeState.legendOpen,
                onLegendOpenChange = homeViewModel::setLegendOpen,
                homeTab = homeState.tab,
                onHomeTabChange = homeViewModel::setTab,
                selectedStationId = homeState.selectedStationId,
                onStationSelected = homeViewModel::selectStation,
                pinScale = homeState.pinScale,
                onPinScaleChange = homeViewModel::setPinScale,
                sliderShown = homeState.sliderShown,
                onSliderShownChange = homeViewModel::setSliderShown,
                mapZoom = { homeViewModel.mapZoom },
                onMapZoomChange = homeViewModel::setMapZoom,
                stationsForRegion = homeViewModel::stations,
                readingFor = homeViewModel::reading,
                noReadingIds = noReadingIds,
                darkMode = darkMode,
                onDarkModeChange = onDarkModeChange
            )
        }
        AppScreen.STATION_LIST -> {
            StationListScreen(
                dataset = regionDataset ?: defaultDataset,
                selectedRegionId = selectedRegionId,
                onSelectRegion = { pickRegion(it) },
                onNavigateBack = { navigation.navigate(AppScreen.LANDING) },
                selectedStationId = homeState.selectedStationId,
                onStationSelected = homeViewModel::selectStation
            )
        }
        AppScreen.STATE_USA_REGIONS -> {
            // Opened from Demos & Previews the screen is a standalone demo with its own region
            // state; opened from the Home button it still sets the app-wide region.
            val isDemo = regionsReturnScreen == AppScreen.DEMOS_AND_PREVIEWS
            var demoRegionId by remember {
                mutableStateOf(stateUsa.defaultRegionId)
            }
            StateUsaRegionsMapScreen(
                selectedRegionId = if (isDemo) demoRegionId else selectedRegionId,
                onSelectRegion = {
                    if (isDemo) {
                        demoRegionId = it
                    } else {
                        pickRegion(it)
                    }
                },
                onNavigateBack = { navigation.navigate(regionsReturnScreen) },
                onNavigateToActiveTransect = { navigation.navigate(AppScreen.UNIFIED_DRAGGABLE_MAP) }
            )
        }
        // The screens an edition adds (the demo edition's Demos & Previews and what is behind it)
        else -> EditionScreens(
            EditionNavigation(
                screen = currentScreen,
                navigate = navigation::navigate,
                setRegionsReturn = navigation::setRegionsReturn,
                regionDataset = regionDataset,
                defaultDataset = defaultDataset,
                selectedRegionId = selectedRegionId,
                pickRegion = { pickRegion(it) }
            )
        )
    }
}
