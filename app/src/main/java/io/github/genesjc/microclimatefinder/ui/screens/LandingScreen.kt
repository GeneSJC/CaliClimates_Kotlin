package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import io.github.genesjc.microclimatefinder.BuildInfo
import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.data.local.TransectConfig
import io.github.genesjc.microclimatefinder.data.local.TransectConfigs
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegionalDefinitions
import io.github.genesjc.microclimatefinder.ui.components.CaliforniaOutlineIcon
import io.github.genesjc.microclimatefinder.ui.components.DemoNavigationCard
import io.github.genesjc.microclimatefinder.ui.navigation.AppScreen
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class LandingEntry(
    val title: String,
    val description: String,
    val iconChar: String,
    val target: AppScreen
)

private fun landingEntries(
    dataset: StationDatasetInfo?,
    transect: TransectConfig?,
    transectStationCount: Int
): List<LandingEntry> {
    val entries = mutableListOf<LandingEntry>()
    if (dataset != null) {
        val stats = "${dataset.stationCount} stations " +
            "(${dataset.minElevationMeters}m to ${dataset.maxElevationMeters}m elev)"
        val kind = dataset.subtitle.substringBefore(" • ")
        entries += LandingEntry(
            title = "Station Feed (CSV)",
            description = "$stats. $kind.",
            iconChar = "▤",
            target = AppScreen.STATION_LIST
        )
        entries += LandingEntry(
            title = "Topographical Map",
            description = "$stats. Interactive spatial map.",
            iconChar = "▲",
            target = AppScreen.STATION_MAP
        )
        entries += LandingEntry(
            title = "Elevation Profile",
            description = "Stations sorted by elevation, from lowest to highest, with live sky/weather theming.",
            iconChar = "⛰️",
            target = AppScreen.ELEVATION_PROFILE
        )
        if (transect != null) {
            entries += LandingEntry(
                title = "California Transect Map",
                description = "Continuous 4-way pan & zoom from ${transect.routeLabel} across $transectStationCount regional stations with expandable Local News panel & story viewer.",
                iconChar = "🗺️",
                target = AppScreen.UNIFIED_DRAGGABLE_MAP
            )
        }
    }
    return entries
}

@Composable
fun LandingScreen(
    selectedRegionId: String,
    dataset: StationDatasetInfo?,
    onNavigateTo: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val region = remember(selectedRegionId) {
        CaliforniaRegionalDefinitions.ALL_REGIONS.firstOrNull { it.id == selectedRegionId }
            ?: CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO
    }
    val hasStationData = dataset != null
    val entries = remember(region.id, dataset) {
        landingEntries(
            dataset,
            TransectConfigs.forRegion(region.id),
            region.activeStationCount
        )
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showAboutDialog by remember { mutableStateOf(false) }

    // Hero expand: the CA icon grows from the header corner to fill the screen, then
    // opens the Regional Sections screen.
    val expand = remember { Animatable(0f) }
    var expanding by remember { mutableStateOf(false) }
    var iconBounds by remember { mutableStateOf(Rect.Zero) }
    var containerOrigin by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var containerWidth by remember { mutableStateOf(0f) }
    var containerHeight by remember { mutableStateOf(0f) }

    LaunchedEffect(expanding) {
        if (expanding) {
            expand.animateTo(1f, tween(durationMillis = 700, easing = FastOutSlowInEasing))
            onNavigateTo(AppScreen.CALIFORNIA_REGIONS)
        }
    }

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
                    Text(
                        text = "Build ${BuildInfo.LABEL}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 2.dp)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    containerOrigin = it.positionInRoot()
                    containerWidth = it.size.width.toFloat()
                    containerHeight = it.size.height.toFloat()
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
                        modifier = Modifier.padding(16.dp),
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
                            text = region.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Change-region button: the California outline on a bordered chip like the menu button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .clickable(
                                    enabled = !expanding,
                                    onClickLabel = "Change region",
                                    role = Role.Button
                                ) { expanding = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CaliforniaOutlineIcon(
                                    fill = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f),
                                    stroke = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .onGloballyPositioned { iconBounds = Rect(it.positionInRoot(), it.size.toSize()) }
                                        .alpha(if (expanding) 0f else 1f)
                                )
                            }
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        text = "Micro Climate Finder",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    if (!hasStationData) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "No station data loaded",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = region.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Station data isn't included in this build for this region. " +
                                        "Tap the California button at the top right to switch regions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            items(entries) { entry ->
                DemoNavigationCard(
                    title = entry.title,
                    description = entry.description,
                    iconChar = entry.iconChar,
                    onClick = { onNavigateTo(entry.target) }
                )
            }
        }

        if (expanding) {
            val t = expand.value
            val density = LocalDensity.current
            val startX = iconBounds.left - containerOrigin.x
            val startY = iconBounds.top - containerOrigin.y
            val widthPx = lerp(iconBounds.width, containerWidth, t)
            val heightPx = lerp(iconBounds.height, containerHeight, t)
            Box(
                modifier = Modifier
                    .offset { IntOffset(lerp(startX, 0f, t).roundToInt(), lerp(startY, 0f, t).roundToInt()) }
                    .size(
                        width = with(density) { widthPx.toDp() },
                        height = with(density) { heightPx.toDp() }
                    )
                    .clip(RoundedCornerShape(lerp(10f, 0f, t).dp))
                    .background(Color(0xFFF4EBD0).copy(alpha = t))
            ) {
                CaliforniaOutlineIcon(
                    fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    stroke = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding((lerp(0f, 32f, t)).dp)
                )
            }
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
