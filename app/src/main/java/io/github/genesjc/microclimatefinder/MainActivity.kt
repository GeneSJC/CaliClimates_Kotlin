package io.github.genesjc.microclimatefinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import io.github.genesjc.microclimatefinder.ui.theme.AppBackground
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegionalDefinitions
import io.github.genesjc.microclimatefinder.ui.navigation.AppScreen
import io.github.genesjc.microclimatefinder.ui.screens.CaliforniaRegionsMapScreen
import io.github.genesjc.microclimatefinder.ui.home.HomeEvent
import io.github.genesjc.microclimatefinder.ui.home.HomeViewModel
import io.github.genesjc.microclimatefinder.ui.root.AppRootViewModel
import io.github.genesjc.microclimatefinder.ui.screens.LandingScreen
import io.github.genesjc.microclimatefinder.ui.screens.RegionOption
import io.github.genesjc.microclimatefinder.ui.screens.StationListScreen
import io.github.genesjc.microclimatefinder.ui.theme.CaliClimatesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // The look (dark or light, remembered between launches) and the backdrop's texture live in the ViewModel
            val appRoot: AppRootViewModel = viewModel(factory = AppRootViewModel.factory(applicationContext))
            val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(applicationContext))
            val darkMode by appRoot.darkMode.collectAsStateWithLifecycle()
            val backdropTexture by appRoot.backdropTexture.collectAsStateWithLifecycle()
            // The backdrop takes a soft tint of the selected region's colour
            val backdropRegionId by homeViewModel.selectedRegionId.collectAsStateWithLifecycle()
            // The opening view shows once per launch (and not again after a rotation)
            var showIntro by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
            CaliClimatesTheme(darkTheme = darkMode) {
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                AppBackground(darkTheme = darkMode, regionId = backdropRegionId, texture = backdropTexture) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent,
                        // The default text colour for everything below, so text without its own colour is
                        // dark on light and light on dark
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ) {
                        AppNavigation(
                            darkMode = darkMode,
                            onDarkModeChange = appRoot::setDarkMode,
                            homeViewModel = homeViewModel
                        )
                    }
                }
                if (showIntro) {
                    io.github.genesjc.microclimatefinder.ui.components.IntroOverlay(onDone = { showIntro = false })
                }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    homeViewModel: HomeViewModel
) {
    var currentScreen by remember { mutableStateOf(AppScreen.LANDING) }
    // Where Back returns to from Regional Sections (Home icon or Demos & Previews)
    var regionsReturnScreen by remember { mutableStateOf(AppScreen.LANDING) }
    // Home's state (region, map view, selected station, dot size, slider, info sheet) lives in the ViewModel
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val selectedRegionId = homeState.selectedRegionId
    // A click when the region changes (not when the app first opens), so the change is felt as well as seen
    LaunchedEffect(homeViewModel) {
        homeViewModel.events.collect { event ->
            when (event) {
                HomeEvent.RegionChanged -> io.github.genesjc.microclimatefinder.ui.components.RegionHaptics.tick(context)
            }
        }
    }

    // Station data per region: a CSV override in assets, else the region's bundled pack (HomeViewModel reads it)
    val fullDataRegions = remember { homeViewModel.fullDataRegionCount() }
    val regionDataset = remember(selectedRegionId) { homeViewModel.datasetFor(selectedRegionId) }
    // The Bay Area fallback dataset is only needed by the Station List when a region has none, and by the demos, so it
    // is read the first time it is wanted and not while the app is starting
    val defaultDatasetLazy = remember { lazy { homeViewModel.defaultDataset() } }
    // Changes the app-wide region and remembers it (Home, Topo Map and Station Feed pickers use this)
    fun pickRegion(regionId: String) = homeViewModel.selectRegion(regionId)
    // Rows for the quick region list on Home: names only (reading every region's stations just for a count slowed the
    // start-up by seconds, and the list no longer shows counts)
    val regionOptions = remember {
        CaliforniaRegionalDefinitions.ALL_REGIONS.map { r -> RegionOption(r.id, r.name, null) }
    }

    // (Live readings for the region's featured stations are fetched by HomeViewModel whenever the region changes.)

    // Intercept hardware/gesture back press when not on Landing
    if (currentScreen != AppScreen.LANDING) {
        BackHandler {
            currentScreen = when {
                currentScreen == AppScreen.CALIFORNIA_REGIONS -> regionsReturnScreen
                else -> editionBackTarget(currentScreen) ?: AppScreen.LANDING
            }
        }
    }

    when (currentScreen) {
        AppScreen.LANDING -> {
            LandingScreen(
                selectedRegionId = selectedRegionId,
                dataset = regionDataset,
                fullDataRegions = fullDataRegions,
                totalRegions = homeViewModel.totalRegionCount(),
                regionOptions = regionOptions,
                onSelectRegion = { pickRegion(it) },
                onNavigateTo = { targetScreen ->
                    if (targetScreen == AppScreen.CALIFORNIA_REGIONS) regionsReturnScreen = AppScreen.LANDING
                    currentScreen = targetScreen
                },
                infoOpen = homeState.infoOpen,
                onInfoOpenChange = homeViewModel::setInfoOpen,
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
                darkMode = darkMode,
                onDarkModeChange = onDarkModeChange
            )
        }
        AppScreen.STATION_LIST -> {
            StationListScreen(
                dataset = regionDataset ?: defaultDatasetLazy.value,
                selectedRegionId = selectedRegionId,
                onSelectRegion = { pickRegion(it) },
                onNavigateBack = {
                    currentScreen = AppScreen.LANDING
                },
                selectedStationId = homeState.selectedStationId,
                onStationSelected = homeViewModel::selectStation
            )
        }
        AppScreen.CALIFORNIA_REGIONS -> {
            // Opened from Demos & Previews the screen is a standalone demo with its own region
            // state; opened from the Home button it still sets the app-wide region.
            val isDemo = regionsReturnScreen == AppScreen.DEMOS_AND_PREVIEWS
            var demoRegionId by remember {
                mutableStateOf(CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.id)
            }
            CaliforniaRegionsMapScreen(
                selectedRegionId = if (isDemo) demoRegionId else selectedRegionId,
                onSelectRegion = {
                    if (isDemo) {
                        demoRegionId = it
                    } else {
                        pickRegion(it)
                    }
                },
                onNavigateBack = {
                    currentScreen = regionsReturnScreen
                },
                onNavigateToActiveTransect = {
                    currentScreen = AppScreen.UNIFIED_DRAGGABLE_MAP
                }
            )
        }
        // The screens an edition adds (the demo edition's Demos & Previews and what is behind it)
        else -> EditionScreens(
            EditionNavigation(
                screen = currentScreen,
                navigate = { currentScreen = it },
                setRegionsReturn = { regionsReturnScreen = it },
                regionDataset = regionDataset,
                defaultDataset = defaultDatasetLazy.value,
                selectedRegionId = selectedRegionId,
                pickRegion = { pickRegion(it) }
            )
        )
    }
}
