package io.github.genesjc.climatefinder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.github.genesjc.climatefinder.ui.navigation.AppScreen

/** The core edition: the app alone, with no Demos & Previews and none of the screens behind them. */
object EditionConfig {
    const val HAS_DEMOS = false
}

/** Nothing extra to show; if a screen only the demo edition has is ever asked for, go home. */
@Composable
fun EditionScreens(nav: EditionNavigation) {
    LaunchedEffect(nav.screen) { nav.navigate(AppScreen.LANDING) }
}

/** Where Back goes from a screen this edition adds (none here). */
fun editionBackTarget(screen: AppScreen): AppScreen? = null
