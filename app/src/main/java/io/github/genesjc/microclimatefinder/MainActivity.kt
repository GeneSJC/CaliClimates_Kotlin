package io.github.genesjc.microclimatefinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import io.github.genesjc.microclimatefinder.data.local.TransectConfigs
import io.github.genesjc.microclimatefinder.data.remote.LiveObservationCache
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegionalDefinitions
import io.github.genesjc.microclimatefinder.domain.model.LocalNewsStory
import io.github.genesjc.microclimatefinder.ui.navigation.AppScreen
import io.github.genesjc.microclimatefinder.ui.screens.AnimationPreviewsScreen
import io.github.genesjc.microclimatefinder.ui.screens.CaliforniaRegionsMapScreen
import io.github.genesjc.microclimatefinder.ui.screens.DemosAndPreviewsScreen
import io.github.genesjc.microclimatefinder.ui.screens.DraggableMapScreen
import io.github.genesjc.microclimatefinder.ui.screens.ElevationProfileScreen
import io.github.genesjc.microclimatefinder.ui.screens.FixedCloudSplashScreen
import io.github.genesjc.microclimatefinder.ui.screens.LandingScreen
import io.github.genesjc.microclimatefinder.ui.screens.MovingCloudsSplashScreen
import io.github.genesjc.microclimatefinder.ui.screens.NewsStoryViewerScreen
import io.github.genesjc.microclimatefinder.ui.screens.StationCardPreviewScreen
import io.github.genesjc.microclimatefinder.ui.screens.StationListScreen
import io.github.genesjc.microclimatefinder.ui.screens.StationMapScreen
import io.github.genesjc.microclimatefinder.ui.screens.UnifiedDraggableMapScreen
import io.github.genesjc.microclimatefinder.ui.screens.WebSplashAnimationScreen
import io.github.genesjc.microclimatefinder.ui.theme.MicroClimateFinderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MicroClimateFinderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf(AppScreen.LANDING) }
    var previousScreen by remember { mutableStateOf(AppScreen.LANDING) }
    var selectedNewsStory by remember { mutableStateOf<LocalNewsStory?>(null) }
    var selectedNewsCity by remember { mutableStateOf("") }
    // Where Back returns to from Regional Sections (Home icon or Demos & Previews)
    var regionsReturnScreen by remember { mutableStateOf(AppScreen.LANDING) }
    val context = LocalContext.current
    // App-wide CA region, remembered between launches; the Regional Sections screen is its only writer
    val regionPrefs = remember { RegionPreferences(context) }
    var selectedRegionId by remember { mutableStateOf(regionPrefs.load()) }

    // Station data: the full bundled files per region, or the built-in Bay Area sample
    val store = remember { StationDataStore(context) }
    val fullDataRegions = remember { store.fullDataRegionCount() }
    val regionDataset = remember(selectedRegionId) { store.datasetFor(selectedRegionId) }
    val defaultDataset = remember { store.defaultDataset() }

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
            if (currentScreen == AppScreen.NEWS_STORY_VIEWER) {
                currentScreen = previousScreen
            } else if (currentScreen == AppScreen.ANIMATION_FIXED_CLOUD_V1 || currentScreen == AppScreen.ANIMATION_MOVING_CLOUDS_V2) {
                currentScreen = AppScreen.ANIMATION_PREVIEWS
            } else if (currentScreen == AppScreen.CALIFORNIA_REGIONS) {
                currentScreen = regionsReturnScreen
            } else if (currentScreen == AppScreen.STATION_CARD_PREVIEW ||
                currentScreen == AppScreen.DRAGGABLE_MAP ||
                currentScreen == AppScreen.ANIMATION_PREVIEWS
            ) {
                currentScreen = AppScreen.DEMOS_AND_PREVIEWS
            } else {
                currentScreen = AppScreen.LANDING
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
                onNavigateTo = { targetScreen ->
                    if (targetScreen == AppScreen.CALIFORNIA_REGIONS) regionsReturnScreen = AppScreen.LANDING
                    currentScreen = targetScreen
                }
            )
        }
        AppScreen.DEMOS_AND_PREVIEWS -> {
            DemosAndPreviewsScreen(
                onNavigateTo = { targetScreen ->
                    if (targetScreen == AppScreen.CALIFORNIA_REGIONS) regionsReturnScreen = AppScreen.DEMOS_AND_PREVIEWS
                    currentScreen = targetScreen
                },
                onNavigateBack = {
                    currentScreen = AppScreen.LANDING
                }
            )
        }
        AppScreen.STATION_CARD_PREVIEW -> {
            StationCardPreviewScreen(
                onNavigateBack = {
                    currentScreen = AppScreen.DEMOS_AND_PREVIEWS
                }
            )
        }
        AppScreen.STATION_LIST -> {
            StationListScreen(
                dataset = regionDataset ?: defaultDataset,
                onNavigateBack = {
                    currentScreen = AppScreen.LANDING
                }
            )
        }
        AppScreen.STATION_MAP -> {
            StationMapScreen(
                dataset = regionDataset ?: defaultDataset,
                onNavigateBack = {
                    currentScreen = AppScreen.LANDING
                }
            )
        }
        AppScreen.ELEVATION_PROFILE -> {
            ElevationProfileScreen(
                dataset = regionDataset ?: defaultDataset,
                onNavigateBack = {
                    currentScreen = AppScreen.LANDING
                }
            )
        }
        AppScreen.DRAGGABLE_MAP -> {
            DraggableMapScreen(
                dataset = defaultDataset,
                onNavigateBack = {
                    currentScreen = AppScreen.DEMOS_AND_PREVIEWS
                }
            )
        }
        AppScreen.UNIFIED_DRAGGABLE_MAP -> {
            UnifiedDraggableMapScreen(
                config = TransectConfigs.forRegion(selectedRegionId) ?: TransectConfigs.DEFAULT,
                onNavigateBack = {
                    currentScreen = AppScreen.LANDING
                },
                onOpenNewsStory = { story, city ->
                    selectedNewsStory = story
                    selectedNewsCity = city
                    previousScreen = AppScreen.UNIFIED_DRAGGABLE_MAP
                    currentScreen = AppScreen.NEWS_STORY_VIEWER
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
        AppScreen.ANIMATION_PREVIEWS -> {
            AnimationPreviewsScreen(
                onNavigateBack = {
                    currentScreen = AppScreen.DEMOS_AND_PREVIEWS
                },
                onSelectAnimation = { animScreen ->
                    currentScreen = animScreen
                }
            )
        }
        AppScreen.ANIMATION_FIXED_CLOUD_V1 -> {
            FixedCloudSplashScreen(
                onNavigateBack = {
                    currentScreen = AppScreen.ANIMATION_PREVIEWS
                }
            )
        }
        AppScreen.ANIMATION_MOVING_CLOUDS_V2 -> {
            MovingCloudsSplashScreen(
                onNavigateBack = {
                    currentScreen = AppScreen.ANIMATION_PREVIEWS
                }
            )
        }
        AppScreen.ANIMATION_WEB_VIEW -> {
            WebSplashAnimationScreen(
                onNavigateBack = {
                    currentScreen = AppScreen.ANIMATION_PREVIEWS
                }
            )
        }
        AppScreen.NEWS_STORY_VIEWER -> {
            selectedNewsStory?.let { story ->
                NewsStoryViewerScreen(
                    storyTitle = story.title,
                    storyUrl = story.url,
                    cityName = selectedNewsCity,
                    source = story.source,
                    onBackToApp = {
                        currentScreen = previousScreen
                    }
                )
            } ?: run {
                currentScreen = AppScreen.LANDING
            }
        }
    }
}
