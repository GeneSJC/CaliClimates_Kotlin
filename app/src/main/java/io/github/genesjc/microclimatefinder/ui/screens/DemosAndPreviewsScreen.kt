package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.microclimatefinder.ui.components.DemoNavigationCard
import io.github.genesjc.microclimatefinder.ui.navigation.AppScreen

/**
 * Demos & Previews Hub Screen
 * Houses the component/feature demo cards moved off the top-level Landing grid:
 * station card preview, station feed, topographical maps, and animation previews.
 */
@Composable
fun DemosAndPreviewsScreen(
    onNavigateTo: (AppScreen) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onNavigateBack)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "←",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Demos & Previews",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Component & Feature Demos",
            style = MaterialTheme.typography.titleSmall,
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

        // Demo 3: Sacramento Topographical Map
        DemoNavigationCard(
            title = "Sacramento Topographical Map",
            description = "21 verified NOAA stations (5m to 1406m elev) across Sacramento 60-mile grid. Dual spatial map and elevation profile.",
            badge = "v0.12.0",
            iconChar = "▲",
            onClick = { onNavigateTo(AppScreen.STATION_MAP) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Demo 4: Draggable Topo Map
        DemoNavigationCard(
            title = "Draggable Topo Map",
            description = "Interactive 4-way pan (N/S/E/W) with gesture dragging across Sacramento 60-mile stations, coordinate readout & recenter.",
            badge = "v0.14.0",
            iconChar = "✥",
            onClick = { onNavigateTo(AppScreen.DRAGGABLE_MAP) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Demo 5: Animation Previews
        DemoNavigationCard(
            title = "Animation Previews",
            description = "Interactive atmospheric and weather animation prototypes: fixed hero cloud v1.0, moving clouds v2.0, and offline web vector preview.",
            badge = "v0.25.0",
            iconChar = "✨",
            onClick = { onNavigateTo(AppScreen.ANIMATION_PREVIEWS) }
        )
    }
}
