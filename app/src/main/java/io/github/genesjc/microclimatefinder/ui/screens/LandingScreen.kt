package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
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

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Component & Feature Demos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Demos & Previews Hub
        DemoNavigationCard(
            title = "Demos & Previews",
            description = "Component/feature demos: Station Card Preview, Draggable Topo Map, and Animation Previews.",
            iconChar = "▦",
            onClick = { onNavigateTo(AppScreen.DEMOS_AND_PREVIEWS) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sacramento Station Feed (CSV)
        DemoNavigationCard(
            title = "Sacramento Station Feed (CSV)",
            description = "21 verified NOAA stations (5m to 1406m elev) loaded from assets via StationListScreen.",
            iconChar = "▤",
            onClick = { onNavigateTo(AppScreen.STATION_LIST) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sacramento Topographical Map
        DemoNavigationCard(
            title = "Sacramento Topographical Map",
            description = "21 verified NOAA stations (5m to 1406m elev) across Sacramento 60-mile grid. Dual spatial map and elevation profile.",
            iconChar = "▲",
            onClick = { onNavigateTo(AppScreen.STATION_MAP) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // California Transect Map (v0.20.0 - Macro Draggable & Zoomable Canvas with Launcher Icon)
        DemoNavigationCard(
            title = "California Transect Map",
            description = "Continuous 4-way pan & zoom from Sacramento to Monterey Bay across 36 regional stations with expandable Local News panel & story viewer.",
            iconChar = "🗺️",
            onClick = { onNavigateTo(AppScreen.UNIFIED_DRAGGABLE_MAP) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // California Regional Sections (v0.21.0 - Multi-Region Constraints & Map)
        DemoNavigationCard(
            title = "California Regional Sections",
            description = "Interactive map of CA's 6 macro climatic sections: San Diego, LA, Central Valley, Central Coast, Bay Area/Sacramento, and Northern CA with coordinate constraints.",
            iconChar = "🧭",
            onClick = { onNavigateTo(AppScreen.CALIFORNIA_REGIONS) }
        )
    }
}
