package io.github.genesjc.microclimatefinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.genesjc.microclimatefinder.data.local.StationDatasets
import io.github.genesjc.microclimatefinder.domain.model.LocalNewsStory
import io.github.genesjc.microclimatefinder.ui.navigation.AppScreen
import io.github.genesjc.microclimatefinder.ui.screens.AnimationPreviewsScreen
import io.github.genesjc.microclimatefinder.ui.screens.CaliforniaRegionsMapScreen
import io.github.genesjc.microclimatefinder.ui.screens.DemosAndPreviewsScreen
import io.github.genesjc.microclimatefinder.ui.screens.DraggableMapScreen
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

    // Intercept hardware/gesture back press when not on Landing
    if (currentScreen != AppScreen.LANDING) {
        BackHandler {
            if (currentScreen == AppScreen.NEWS_STORY_VIEWER) {
                currentScreen = previousScreen
            } else if (currentScreen == AppScreen.ANIMATION_FIXED_CLOUD_V1 || currentScreen == AppScreen.ANIMATION_MOVING_CLOUDS_V2) {
                currentScreen = AppScreen.ANIMATION_PREVIEWS
            } else if (currentScreen == AppScreen.STATION_CARD_PREVIEW ||
                currentScreen == AppScreen.STATION_LIST ||
                currentScreen == AppScreen.STATION_MAP ||
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
                onNavigateTo = { targetScreen ->
                    currentScreen = targetScreen
                }
            )
        }
        AppScreen.DEMOS_AND_PREVIEWS -> {
            DemosAndPreviewsScreen(
                onNavigateTo = { targetScreen ->
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
                dataset = StationDatasets.DEFAULT_DATASET,
                onNavigateBack = {
                    currentScreen = AppScreen.DEMOS_AND_PREVIEWS
                }
            )
        }
        AppScreen.STATION_MAP -> {
            StationMapScreen(
                dataset = StationDatasets.DEFAULT_DATASET,
                onNavigateBack = {
                    currentScreen = AppScreen.DEMOS_AND_PREVIEWS
                }
            )
        }
        AppScreen.DRAGGABLE_MAP -> {
            DraggableMapScreen(
                dataset = StationDatasets.DEFAULT_DATASET,
                onNavigateBack = {
                    currentScreen = AppScreen.DEMOS_AND_PREVIEWS
                }
            )
        }
        AppScreen.UNIFIED_DRAGGABLE_MAP -> {
            UnifiedDraggableMapScreen(
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
            CaliforniaRegionsMapScreen(
                onNavigateBack = {
                    currentScreen = AppScreen.LANDING
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
