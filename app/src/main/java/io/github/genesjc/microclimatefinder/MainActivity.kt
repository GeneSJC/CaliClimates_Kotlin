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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.genesjc.microclimatefinder.data.local.StationDataStore
import io.github.genesjc.microclimatefinder.data.local.RegionPreferences
import io.github.genesjc.microclimatefinder.data.local.StationCsvParser
import io.github.genesjc.microclimatefinder.data.remote.LiveObservationCache
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegionalDefinitions
import io.github.genesjc.microclimatefinder.ui.navigation.AppScreen
import io.github.genesjc.microclimatefinder.ui.screens.CaliforniaRegionsMapScreen
import io.github.genesjc.microclimatefinder.ui.screens.DEFAULT_PIN_SCALE
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
fun AppNavigation(darkMode: Boolean, onRegionChanged: (String) -> Unit, onDarkModeChange: (Boolean) -> Unit) {
    var currentScreen by remember { mutableStateOf(AppScreen.LANDING) }
    // Where Back returns to from Regional Sections (Home icon or Demos & Previews)
    var regionsReturnScreen by remember { mutableStateOf(AppScreen.LANDING) }
    // Whether Home's region info sheet is open; kept here so it survives a trip to the Station List
    var homeInfoOpen by remember { mutableStateOf(false) }
    // Which map view Home shows: 0 = Topo Map, 1 = Elevation Profile
    var homeTab by remember { mutableStateOf(0) }
    // The station selected on Home, shared by both tabs and kept across a trip to the Station List
    var homeStationId by remember { mutableStateOf<String?>(null) }
    // The Topo Map's dot size, set in the Terrain Legend panel on Home
    var homePinScale by remember { mutableStateOf(DEFAULT_PIN_SCALE) }
    val context = LocalContext.current
    // App-wide CA region, remembered between launches; the Regional Sections screen is its only writer
    val regionPrefs = remember { RegionPreferences(context) }
    var selectedRegionId by remember { mutableStateOf(regionPrefs.load()) }
    // Tell the backdrop which region is selected, so it can take on the region's tint
    LaunchedEffect(selectedRegionId) { onRegionChanged(selectedRegionId) }

    // Station data per region: a CSV override in assets, else the region's bundled pack
    val store = remember { StationDataStore(context) }
    val fullDataRegions = remember { store.fullDataRegionCount() }
    val regionDataset = remember(selectedRegionId) { store.datasetFor(selectedRegionId) }
    val defaultDataset = remember { store.defaultDataset() }
    // Changes the app-wide region and remembers it (Home, Topo Map and Station Feed pickers use this)
    fun pickRegion(regionId: String) {
        selectedRegionId = regionId
        homeStationId = null
        regionPrefs.save(regionId)
    }
    // Rows for the quick region picker on Home: every region with its station count
    val regionOptions = remember {
        CaliforniaRegionalDefinitions.ALL_REGIONS.map { r ->
            RegionOption(r.id, r.name, store.datasetFor(r.id)?.stationCount)
        }
    }

    // Fetch live readings for the region's featured stations when a region loads; switching region
    // cancels this and starts the new region's.
    LaunchedEffect(regionDataset) {
        regionDataset?.let { dataset ->
            val stations = withContext(Dispatchers.IO) { StationCsvParser(context).loadStations(dataset) }
            LiveObservationCache.prefetch(stations)
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
                infoOpen = homeInfoOpen,
                onInfoOpenChange = { homeInfoOpen = it },
                homeTab = homeTab,
                onHomeTabChange = { homeTab = it },
                selectedStationId = homeStationId,
                onStationSelected = { homeStationId = it },
                pinScale = homePinScale,
                onPinScaleChange = { homePinScale = it },
                darkMode = darkMode,
                onDarkModeChange = onDarkModeChange
            )
        }
        AppScreen.STATION_LIST -> {
            StationListScreen(
                dataset = regionDataset ?: defaultDataset,
                selectedRegionId = selectedRegionId,
                onSelectRegion = { pickRegion(it) },
                onNavigateBack = {
                    currentScreen = AppScreen.LANDING
                }
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
                        selectedRegionId = it
                        regionPrefs.save(it)
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
                defaultDataset = defaultDataset,
                selectedRegionId = selectedRegionId,
                pickRegion = { pickRegion(it) }
            )
        )
    }
}
