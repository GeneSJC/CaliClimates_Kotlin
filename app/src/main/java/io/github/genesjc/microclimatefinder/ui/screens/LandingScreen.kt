package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.genesjc.microclimatefinder.BuildConfig
import io.github.genesjc.microclimatefinder.ui.components.DemoNavigationCard
import io.github.genesjc.microclimatefinder.ui.navigation.AppScreen

private data class LandingEntry(
    val title: String,
    val description: String,
    val iconChar: String,
    val target: AppScreen
)

private val LANDING_ENTRIES = listOf(
    LandingEntry(
        title = "Demos & Previews",
        description = "Component/feature demos: Station Card Preview, Draggable Topo Map, and Animation Previews.",
        iconChar = "▦",
        target = AppScreen.DEMOS_AND_PREVIEWS
    ),
    LandingEntry(
        title = "Sacramento Station Feed (CSV)",
        description = "21 verified NOAA stations (5m to 1406m elev) loaded from assets via StationListScreen.",
        iconChar = "▤",
        target = AppScreen.STATION_LIST
    ),
    LandingEntry(
        title = "Sacramento Topographical Map",
        description = "21 verified NOAA stations (5m to 1406m elev) across Sacramento 60-mile grid. Dual spatial map and elevation profile.",
        iconChar = "▲",
        target = AppScreen.STATION_MAP
    ),
    LandingEntry(
        title = "California Transect Map",
        description = "Continuous 4-way pan & zoom from Sacramento to Monterey Bay across 36 regional stations with expandable Local News panel & story viewer.",
        iconChar = "🗺️",
        target = AppScreen.UNIFIED_DRAGGABLE_MAP
    ),
    LandingEntry(
        title = "California Regional Sections",
        description = "Interactive map of CA's 6 macro climatic sections: San Diego, LA, Central Valley, Central Coast, Bay Area/Sacramento, and Northern CA with coordinate constraints.",
        iconChar = "🧭",
        target = AppScreen.CALIFORNIA_REGIONS
    )
)

@Composable
fun LandingScreen(
    onNavigateTo: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val flavorTitle = if (BuildConfig.IS_COMMERCIAL) {
        "Micro Climate Finder • Commercial Edition"
    } else {
        "Micro Climate Finder • FOSS Edition"
    }

    val mapProvider = if (BuildConfig.IS_COMMERCIAL) {
        "Google Maps SDK & Premium Radar"
    } else {
        "OpenStreetMap / OsmDroid"
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            // App Header Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Micro Climate Finder",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = flavorTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    Text(
                        text = "Active Engine: $mapProvider",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Component & Feature Demos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(LANDING_ENTRIES) { entry ->
            DemoNavigationCard(
                title = entry.title,
                description = entry.description,
                iconChar = entry.iconChar,
                onClick = { onNavigateTo(entry.target) }
            )
        }
    }
}
