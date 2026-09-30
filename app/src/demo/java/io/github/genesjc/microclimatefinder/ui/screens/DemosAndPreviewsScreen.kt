package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

private data class DemoEntry(
    val title: String,
    val description: String,
    val iconChar: String,
    val target: AppScreen
)

private val DEMOS_AND_PREVIEWS_ENTRIES = listOf(
    DemoEntry(
        title = "Topography (legacy)",
        description = "The Topography map as its own screen, with its own header. Kept for reference: the same map is the Topography view on Home.",
        iconChar = "▲",
        target = AppScreen.STATION_MAP
    ),
    DemoEntry(
        title = "Elevation (legacy)",
        description = "The Elevation profile as its own screen, with its own header. Kept for reference: the same view is the Elevation view on Home.",
        iconChar = "⛰️",
        target = AppScreen.ELEVATION_PROFILE
    ),
    DemoEntry(
        title = "Station Card Preview",
        description = "Interactive single station card with elevation badge, dual C/F temps, wind, and terrain styling.",
        iconChar = "▦",
        target = AppScreen.STATION_CARD_PREVIEW
    ),
    DemoEntry(
        title = "Draggable Topo Map",
        description = "Interactive 4-way pan (N/S/E/W) with gesture dragging across Sacramento 60-mile stations, coordinate readout & recenter.",
        iconChar = "✥",
        target = AppScreen.DRAGGABLE_MAP
    ),
    DemoEntry(
        title = "Animation Previews",
        description = "Interactive atmospheric and weather animation prototypes: fixed hero cloud v1.0, moving clouds v2.0, and offline web vector preview.",
        iconChar = "✨",
        target = AppScreen.ANIMATION_PREVIEWS
    ),
    DemoEntry(
        title = "Bay Area Transect Map",
        description = "The original Bay Area transect: continuous 4-way pan and zoom from Sacramento to Monterey Bay, with a Local News panel and story viewer. A reference demo, kept as it was.",
        iconChar = "🗺️",
        target = AppScreen.UNIFIED_DRAGGABLE_MAP
    ),
    DemoEntry(
        title = "California Regional Sections",
        description = "Interactive map of CA's 6 macro climatic sections: San Diego, Los Angeles Area, Central Valley, Central Coast, Bay Area/Sacramento, and Northern CA with coordinate constraints.",
        iconChar = "🧭",
        target = AppScreen.CALIFORNIA_REGIONS
    )
)

/**
 * Demos & Previews Hub Screen
 * Two-column grid housing the component/feature demo cards moved off the Landing list:
 * Station Card Preview, Draggable Topo Map, Animation Previews, the Bay Area Transect Map, and
 * California Regional Sections.
 */
@Composable
fun DemosAndPreviewsScreen(
    onNavigateTo: (AppScreen) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Component & Feature Demos",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(DEMOS_AND_PREVIEWS_ENTRIES) { entry ->
            DemoNavigationCard(
                title = entry.title,
                description = entry.description,
                iconChar = entry.iconChar,
                onClick = { onNavigateTo(entry.target) }
            )
        }
    }
}
