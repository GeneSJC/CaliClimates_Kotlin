package io.github.genesjc.climatefinder

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.genesjc.climatefinder.data.local.TransectConfigs
import io.github.genesjc.climatefinder.ui.navigation.AppScreen
import io.github.genesjc.climatefinder.ui.screens.AnimationPreviewsScreen
import io.github.genesjc.climatefinder.ui.screens.DemosAndPreviewsScreen
import io.github.genesjc.climatefinder.ui.screens.DraggableMapScreen
import io.github.genesjc.climatefinder.ui.screens.ElevationProfileScreen
import io.github.genesjc.climatefinder.ui.screens.FixedCloudSplashScreen
import io.github.genesjc.climatefinder.ui.screens.MovingCloudsSplashScreen
import io.github.genesjc.climatefinder.ui.screens.StationCardPreviewScreen
import io.github.genesjc.climatefinder.ui.screens.StationMapScreen
import io.github.genesjc.climatefinder.ui.screens.UnifiedDraggableMapScreen
import io.github.genesjc.climatefinder.ui.screens.WebSplashAnimationScreen

/** The demo edition: the app plus the Demos & Previews hub and the legacy and prototype screens behind it. */
object EditionConfig {
    const val HAS_DEMOS = true
}

/** The screens the demo edition adds to the app's navigation. */
@Composable
fun EditionScreens(nav: EditionNavigation) {
    when (nav.screen) {
        AppScreen.DEMOS_AND_PREVIEWS -> DemosAndPreviewsScreen(
            onNavigateTo = { target ->
                if (target == AppScreen.STATE_USA_REGIONS) nav.setRegionsReturn(AppScreen.DEMOS_AND_PREVIEWS)
                nav.navigate(target)
            },
            onNavigateBack = { nav.navigate(AppScreen.LANDING) }
        )
        AppScreen.STATION_CARD_PREVIEW -> StationCardPreviewScreen(
            onNavigateBack = { nav.navigate(AppScreen.DEMOS_AND_PREVIEWS) }
        )
        AppScreen.STATION_MAP -> StationMapScreen(
            dataset = nav.regionDataset ?: nav.defaultDataset,
            selectedRegionId = nav.selectedRegionId,
            onSelectRegion = nav.pickRegion,
            onNavigateBack = { nav.navigate(AppScreen.DEMOS_AND_PREVIEWS) }
        )
        AppScreen.ELEVATION_PROFILE -> ElevationProfileScreen(
            dataset = nav.regionDataset ?: nav.defaultDataset,
            onNavigateBack = { nav.navigate(AppScreen.DEMOS_AND_PREVIEWS) }
        )
        AppScreen.DRAGGABLE_MAP -> DraggableMapScreen(
            dataset = nav.defaultDataset,
            onNavigateBack = { nav.navigate(AppScreen.DEMOS_AND_PREVIEWS) }
        )
        AppScreen.UNIFIED_DRAGGABLE_MAP -> UnifiedDraggableMapScreen(
            // A demo: always the Bay Area transect, whichever region the app is on
            config = TransectConfigs.DEFAULT,
            onNavigateBack = { nav.navigate(AppScreen.DEMOS_AND_PREVIEWS) }
        )
        AppScreen.ANIMATION_PREVIEWS -> AnimationPreviewsScreen(
            onNavigateBack = { nav.navigate(AppScreen.DEMOS_AND_PREVIEWS) },
            onSelectAnimation = { nav.navigate(it) }
        )
        AppScreen.ANIMATION_FIXED_CLOUD_V1 -> FixedCloudSplashScreen(
            onNavigateBack = { nav.navigate(AppScreen.ANIMATION_PREVIEWS) }
        )
        AppScreen.ANIMATION_MOVING_CLOUDS_V2 -> MovingCloudsSplashScreen(
            onNavigateBack = { nav.navigate(AppScreen.ANIMATION_PREVIEWS) }
        )
        AppScreen.ANIMATION_WEB_VIEW -> WebSplashAnimationScreen(
            onNavigateBack = { nav.navigate(AppScreen.ANIMATION_PREVIEWS) }
        )
        else -> Unit
    }
}

/** Where Back goes from a screen the demo edition adds. */
fun editionBackTarget(screen: AppScreen): AppScreen? = when (screen) {
    AppScreen.ANIMATION_FIXED_CLOUD_V1,
    AppScreen.ANIMATION_MOVING_CLOUDS_V2,
    AppScreen.ANIMATION_WEB_VIEW -> AppScreen.ANIMATION_PREVIEWS
    AppScreen.STATION_CARD_PREVIEW,
    AppScreen.STATION_MAP,
    AppScreen.ELEVATION_PROFILE,
    AppScreen.DRAGGABLE_MAP,
    AppScreen.UNIFIED_DRAGGABLE_MAP,
    AppScreen.ANIMATION_PREVIEWS -> AppScreen.DEMOS_AND_PREVIEWS
    else -> null
}
