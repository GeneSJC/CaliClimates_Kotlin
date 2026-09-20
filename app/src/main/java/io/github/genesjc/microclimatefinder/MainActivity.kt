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
import io.github.genesjc.microclimatefinder.ui.screens.DraggableMapScreen
import io.github.genesjc.microclimatefinder.ui.screens.LandingScreen
import io.github.genesjc.microclimatefinder.ui.screens.NewsStoryViewerScreen
import io.github.genesjc.microclimatefinder.ui.screens.StationCardPreviewScreen
import io.github.genesjc.microclimatefinder.ui.screens.StationListScreen
import io.github.genesjc.microclimatefinder.ui.screens.StationMapScreen
import io.github.genesjc.microclimatefinder.ui.screens.UnifiedDraggableMapScreen
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
        AppScreen.STATION_CARD_PREVIEW -> {
            StationCardPreviewScreen(
                onNavigateBack = {
                    currentScreen = AppScreen.LANDING
                }
            )
        }
        AppScreen.STATION_LIST -> {
            StationListScreen(
                dataset = StationDatasets.DEFAULT_DATASET,
                onNavigateBack = {
                    currentScreen = AppScreen.LANDING
                }
            )
        }
        AppScreen.STATION_MAP -> {
            StationMapScreen(
                dataset = StationDatasets.DEFAULT_DATASET,
                onNavigateBack = {
                    currentScreen = AppScreen.LANDING
                }
            )
        }
        AppScreen.DRAGGABLE_MAP -> {
            DraggableMapScreen(
                dataset = StationDatasets.DEFAULT_DATASET,
                onNavigateBack = {
                    currentScreen = AppScreen.LANDING
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
