package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.microclimatefinder.BuildConfig
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

        // Demo 1: Station Card Preview Screen
        DemoNavigationCard(
            title = "Station Card Preview",
            description = "Interactive single station card with elevation badge, dual C/F temps, wind, and terrain styling.",
            iconChar = "▦",
            onClick = { onNavigateTo(AppScreen.STATION_CARD_PREVIEW) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Demo 2: Generic Station List Feed (Passes Sacramento Dataset)
        DemoNavigationCard(
            title = "Sacramento Station Feed (CSV)",
            description = "21 verified NOAA stations (5m to 1406m elev) loaded from assets via StationListScreen.",
            iconChar = "▤",
            onClick = { onNavigateTo(AppScreen.STATION_LIST) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Demo 3: Sacramento Topographical Map (v0.11.0)
        // Demo 3: Sacramento Topographical Map (v0.12.0) - Reverted to clean view without top region/biome buttons
        DemoNavigationCard(
            title = "Sacramento Topographical Map",
            description = "21 verified NOAA stations (5m to 1406m elev) across Sacramento 60-mile grid. Dual spatial map and elevation profile.",
            iconChar = "▲",
            onClick = { onNavigateTo(AppScreen.STATION_MAP) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Demo 4: Draggable Topo Map (v0.14.0 - Step 1: Pan Gesture Canvas)
        DemoNavigationCard(
            title = "Draggable Topo Map",
            description = "Interactive 4-way pan (N/S/E/W) with gesture dragging across Sacramento 60-mile stations, coordinate readout & recenter.",
            iconChar = "✥",
            onClick = { onNavigateTo(AppScreen.DRAGGABLE_MAP) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Demo 5: California Transect Map (v0.20.0 - Macro Draggable & Zoomable Canvas with Launcher Icon)
        DemoNavigationCard(
            title = "California Transect Map",
            description = "Continuous 4-way pan & zoom from Sacramento to Monterey Bay across 36 regional stations with expandable Local News panel & story viewer.",
            iconChar = "🗺️",
            onClick = { onNavigateTo(AppScreen.UNIFIED_DRAGGABLE_MAP) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Demo 6: California Regional Sections (v0.21.0 - Multi-Region Constraints & Map)
        DemoNavigationCard(
            title = "California Regional Sections",
            description = "Interactive map of CA's 6 macro climatic sections: San Diego, LA, Central Valley, Central Coast, Bay Area/Sacramento, and Northern CA with coordinate constraints.",
            iconChar = "🧭",
            onClick = { onNavigateTo(AppScreen.CALIFORNIA_REGIONS) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Demo 7: Animation Previews (v0.25.0)
        DemoNavigationCard(
            title = "Animation Previews",
            description = "Interactive atmospheric and weather animation prototypes: fixed hero cloud v1.0, moving clouds v2.0, and offline web vector preview.",
            iconChar = "✨",
            onClick = { onNavigateTo(AppScreen.ANIMATION_PREVIEWS) }
        )
    }
}

@Composable
private fun DemoNavigationCard(
    title: String,
    description: String,
    iconChar: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = iconChar,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "→",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
