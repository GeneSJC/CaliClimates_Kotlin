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
import io.github.genesjc.microclimatefinder.data.local.ThemePreferences
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
import io.github.genesjc.microclimatefinder.data.local.StationDataStore
import io.github.genesjc.microclimatefinder.data.local.RegionPreferences
import io.github.genesjc.microclimatefinder.data.local.TerrainMaps
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegionalDefinitions
import io.github.genesjc.microclimatefinder.ui.navigation.AppScreen
import io.github.genesjc.microclimatefinder.ui.screens.CaliforniaRegionsMapScreen
import io.github.genesjc.microclimatefinder.ui.home.HomeViewModel
import io.github.genesjc.microclimatefinder.ui.screens.LandingScreen
import io.github.genesjc.microclimatefinder.ui.screens.RegionOption
import io.github.genesjc.microclimatefinder.ui.screens.StationListScreen
import io.github.genesjc.microclimatefinder.ui.theme.MicroClimateFinderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Dark mode is the default; the menu switches it, and the choice is remembered
            val themePrefs = remember { ThemePreferences(applicationContext) }
            var darkMode by remember { mutableStateOf(themePrefs.darkMode()) }
            // The backdrop takes a soft tint of the selected region's colour (AppNavigation reports each change)
            var backdropRegionId by remember { mutableStateOf(RegionPreferences(applicationContext).load()) }
            // The opening view shows once per launch (and not again after a rotation)
            var showIntro by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
            MicroClimateFinderTheme(darkTheme = darkMode) {
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                AppBackground(darkTheme = darkMode, regionId = backdropRegionId) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent,
                        // The default text colour for everything below, so text without its own colour is
                        // dark on light and light on dark
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ) {
                        AppNavigation(
                            darkMode = darkMode,
                            onRegionChanged = { backdropRegionId = it },
                            onDarkModeChange = {
                                darkMode = it
                                themePrefs.saveDarkMode(it)
                            }
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
    onRegionChanged: (String) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(LocalContext.current))
) {
    var currentScreen by remember { mutableStateOf(AppScreen.LANDING) }
    // Where Back returns to from Regional Sections (Home icon or Demos & Previews)
    var regionsReturnScreen by remember { mutableStateOf(AppScreen.LANDING) }
    // Home's state (region, map view, selected station, dot size, slider, info sheet) lives in the ViewModel
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val selectedRegionId = homeState.selectedRegionId
    // Tell the backdrop which region is selected, so it can take on the region's tint
    LaunchedEffect(selectedRegionId) { onRegionChanged(selectedRegionId) }
    // A click when the region changes (not when the app first opens), so the change is felt as well as seen
    var firstRegion by remember { mutableStateOf(true) }
    LaunchedEffect(selectedRegionId) {
        if (firstRegion) firstRegion = false
        else io.github.genesjc.microclimatefinder.ui.components.RegionHaptics.tick(context)
    }

    // Station data per region: a CSV override in assets, else the region's bundled pack
    val store = remember { StationDataStore(context) }
    val fullDataRegions = remember { store.fullDataRegionCount() }
    val regionDataset = remember(selectedRegionId) { store.datasetFor(selectedRegionId) }
    // The Bay Area fallback dataset is only needed by the Station List when a region has none, and by the demos, so it
    // is read the first time it is wanted and not while the app is starting
    val defaultDatasetLazy = remember { lazy { store.defaultDataset() } }
    // Changes the app-wide region and remembers it (Home, Topo Map and Station Feed pickers use this)
    fun pickRegion(regionId: String) = homeViewModel.selectRegion(regionId)
    // Rows for the quick region list on Home: names only (reading every region's stations just for a count slowed the
    // start-up by seconds, and the list no longer shows counts)
    val regionOptions = remember {
        CaliforniaRegionalDefinitions.ALL_REGIONS.map { r -> RegionOption(r.id, r.name, null) }
    }

    // (Live readings for the region's featured stations are fetched by HomeViewModel whenever the region changes.)

    // Once the first screen is up, read the other regions' stations in the background, so changing region later finds
    // them already parsed instead of unzipping a pack on the main thread in the middle of the transition
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            CaliforniaRegionalDefinitions.ALL_REGIONS.forEach { store.datasetFor(it.id) }
            // ... and the statewide terrain pictures the region picker draws, so its first open shows them at once
            TerrainMaps.loadState(context)
        }
    }

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
                totalRegions = store.totalRegionCount(),
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
