package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.microclimatefinder.ui.components.DemoNavigationCard
import io.github.genesjc.microclimatefinder.ui.navigation.AppScreen
import kotlinx.coroutines.launch

private data class LandingEntry(
    val title: String,
    val description: String,
    val iconChar: String,
    val target: AppScreen
)

private val LANDING_ENTRIES = listOf(
    LandingEntry(
        title = "Sacramento Station Feed (CSV)",
        description = "21 verified NOAA stations (5m to 1406m elev) loaded from assets via StationListScreen.",
        iconChar = "▤",
        target = AppScreen.STATION_LIST
    ),
    LandingEntry(
        title = "Sacramento Topographical Map",
        description = "21 verified NOAA stations (5m to 1406m elev) across Sacramento 60-mile grid. Interactive spatial map.",
        iconChar = "▲",
        target = AppScreen.STATION_MAP
    ),
    LandingEntry(
        title = "Elevation Profile",
        description = "Sacramento stations sorted by elevation, from valley floor to Sierra ridgeline, with live sky/weather theming.",
        iconChar = "⛰️",
        target = AppScreen.ELEVATION_PROFILE
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showAboutDialog by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(vertical = 24.dp)) {
                    Text(
                        text = "Micro Climate Finder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    NavigationDrawerItem(
                        label = { Text("Demos & Previews") },
                        icon = { Text(text = "▦", fontSize = 18.sp) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateTo(AppScreen.DEMOS_AND_PREVIEWS)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("About") },
                        icon = { Text(text = "ℹ", fontSize = 18.sp) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showAboutDialog = true
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
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
                // App Header Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { scope.launch { drawerState.open() } }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "☰",
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = "Micro Climate Finder",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
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

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            },
            title = {
                Text(
                    text = "About Micro Climate Finder",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Micro Climate Finder is an interactive explorer for California's " +
                        "microclimates — combining live NOAA station data, topographic maps, and " +
                        "regional climate visualizations across the state, from coastal fog belts to " +
                        "alpine Sierra ridgelines.\n\nCurrent scope: CA Climate Finder. The app is " +
                        "focused on California for now."
                )
            }
        )
    }
}
