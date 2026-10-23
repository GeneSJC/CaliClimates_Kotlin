package io.github.genesjc.microclimatefinder

import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.ui.navigation.AppScreen

/**
 * The build's edition. The app comes in two: **core** is the app alone, from the start screen on; **demo**
 * adds the Demos & Previews hub and the legacy and prototype screens behind it. Each edition's source set
 * (`src/core`, `src/demo`) declares [EditionConfig], `EditionScreens` and `editionBackTarget`.
 *
 * What an edition's extra screens need from the app's navigation:
 */
class EditionNavigation(
    val screen: AppScreen,
    val navigate: (AppScreen) -> Unit,
    /** Sets where Back goes from the Regional Sections screen. */
    val setRegionsReturn: (AppScreen) -> Unit,
    val regionDataset: StationDatasetInfo?,
    val defaultDataset: StationDatasetInfo,
    val selectedRegionId: String,
    val pickRegion: (String) -> Unit
)
