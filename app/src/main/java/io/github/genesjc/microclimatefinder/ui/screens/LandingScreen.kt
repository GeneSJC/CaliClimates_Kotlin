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
            badge = "v0.8.2",
            iconChar = "▦",
            onClick = { onNavigateTo(AppScreen.STATION_CARD_PREVIEW) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Demo 2: Generic Station List Feed (Passes Sacramento Dataset)
        DemoNavigationCard(
            title = "Sacramento Station Feed (CSV)",
            description = "21 verified NOAA stations (5m to 1406m elev) loaded from assets via StationListScreen.",
            badge = "v0.9.2",
            iconChar = "▤",
            onClick = { onNavigateTo(AppScreen.STATION_LIST) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Demo 3: Sacramento Topographical Map (v0.11.0)
        // Demo 3: Sacramento Topographical Map (v0.12.0) - Reverted to clean view without top region/biome buttons
        DemoNavigationCard(
            title = "Sacramento Topographical Map",
            description = "21 verified NOAA stations (5m to 1406m elev) across Sacramento 60-mile grid. Dual spatial map and elevation profile.",
            badge = "v0.12.0",
            iconChar = "▲",
            onClick = { onNavigateTo(AppScreen.STATION_MAP) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Demo 4: Draggable Topo Map (v0.14.0 - Step 1: Pan Gesture Canvas)
        DemoNavigationCard(
            title = "Draggable Topo Map",
            description = "Interactive 4-way pan (N/S/E/W) with gesture dragging across Sacramento 60-mile stations, coordinate readout & recenter.",
            badge = "v0.14.0",
            iconChar = "✥",
            onClick = { onNavigateTo(AppScreen.DRAGGABLE_MAP) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Demo 5: California Transect Map (v0.17.0 - Macro Draggable & Zoomable Canvas)
        DemoNavigationCard(
            title = "California Transect Map",
            description = "Continuous 4-way pan & zoom from Sacramento & Sierra (39.4°N) to Monterey Bay & Salinas (35.6°N) across 36 regional stations with zoom limits & snackbar feedback.",
            badge = "v0.17.0",
            iconChar = "🗺️",
            onClick = { onNavigateTo(AppScreen.UNIFIED_DRAGGABLE_MAP) }
        )
    }
}

@Composable
private fun DemoNavigationCard(
    title: String,
    description: String,
    badge: String,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = iconChar,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Text(
                text = "→",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
