package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
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
import io.github.genesjc.microclimatefinder.ui.components.GridDemoCard
import io.github.genesjc.microclimatefinder.ui.navigation.AppScreen

private data class LandingGridEntry(
    val title: String,
    val description: String,
    val badge: String,
    val iconChar: String,
    val isNew: Boolean,
    val target: AppScreen
)

private val LANDING_GRID_ENTRIES = listOf(
    LandingGridEntry(
        title = "Demos & Previews",
        description = "Explore component demos, station feeds (CSV), topographical maps, splash animations, and 2-column grid previews.",
        badge = "v0.24.0",
        iconChar = "▦",
        isNew = true,
        target = AppScreen.DEMOS_AND_PREVIEWS
    ),
    LandingGridEntry(
        title = "California Transect Map",
        description = "Continuous 4-way pan & zoom from Sacramento to Monterey Bay across 36 regional stations with expandable Local News panel & story viewer.",
        badge = "v0.20.0",
        iconChar = "🗺️",
        isNew = false,
        target = AppScreen.UNIFIED_DRAGGABLE_MAP
    ),
    LandingGridEntry(
        title = "California Regional Sections",
        description = "Regional boundary overlays and multi-transect microclimate analysis across California climate divisions.",
        badge = "v0.21.0",
        iconChar = "🧭",
        isNew = false,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
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

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(LANDING_GRID_ENTRIES) { entry ->
                GridDemoCard(
                    title = entry.title,
                    description = entry.description,
                    badge = entry.badge,
                    iconChar = entry.iconChar,
                    isNew = entry.isNew,
                    onClick = { onNavigateTo(entry.target) }
                )
            }
        }
    }
}
