package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import io.github.genesjc.microclimatefinder.ui.components.LegendItem
import io.github.genesjc.microclimatefinder.domain.model.TerrainType
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.data.local.StationCsvParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import io.github.genesjc.microclimatefinder.BuildInfo
import io.github.genesjc.microclimatefinder.R
import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.data.local.TransectConfig
import io.github.genesjc.microclimatefinder.data.local.TransectConfigs
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegion
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
        val kind = dataset.subtitle.substringBefore(" • ")
        entries += LandingEntry(
            title = "Station Feed (CSV)",
            description = "Every station in the region, as a list. $kind.",
            iconChar = "▤",
            target = AppScreen.STATION_LIST
        )
        entries += LandingEntry(
            title = "Topographical Map",
            description = "Stations on a map of the region. Zoom, pan and tap a station.",
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
                title = "${transect.displayName} Transect Map",
                description = "Continuous 4-way pan & zoom from ${transect.routeLabel} across $transectStationCount regional stations with expandable Local News panel & story viewer.",
                iconChar = "🗺️",
                target = AppScreen.UNIFIED_DRAGGABLE_MAP
            )
        }
    }
    return entries
}

/** One row in the quick region picker. */
data class RegionOption(val id: String, val name: String, val stationCount: Int?)

/** At a glance: how many stations, their elevation range, and the mix of terrain types. */
@Composable
private fun RegionSnapshotCard(stations: List<MicroclimateStation>) {
    val minElev = stations.minOf { it.elevationMeters }
    val maxElev = stations.maxOf { it.elevationMeters }
    val mix = TerrainType.values()
        .map { type -> type to stations.count { it.terrain == type } }
        .filter { it.second > 0 }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Snapshot",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${stations.size}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "stations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${"%,d".format(minElev)}\u2013${"%,d".format(maxElev)} m",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "elevation range",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Terrain mix",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
            ) {
                mix.forEach { (type, count) ->
                    Box(
                        modifier = Modifier
                            .weight(count.toFloat())
                            .fillMaxHeight()
                            .background(terrainColor(type))
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                mix.forEach { (type, count) ->
                    LegendItem(color = terrainColor(type), label = "${terrainShortName(type)} $count")
                }
            }
        }
    }
}

// Same colors as the map legends
private fun terrainColor(type: TerrainType): Color = when (type) {
    TerrainType.RIDGE -> Color(0xFF7C3AED)
    TerrainType.VALLEY_BASIN -> Color(0xFFEA580C)
    TerrainType.COASTAL -> Color(0xFF0284C7)
    TerrainType.URBAN_CANYON -> Color(0xFF475569)
}

private fun terrainShortName(type: TerrainType): String = when (type) {
    TerrainType.RIDGE -> "Ridge"
    TerrainType.VALLEY_BASIN -> "Valley"
    TerrainType.COASTAL -> "Coastal"
    TerrainType.URBAN_CANYON -> "Urban"
}

/** About the current region: its subtitle and description, with traits and key cities on expand. */
@Composable
private fun RegionDetailsCard(region: CaliforniaRegion) {
    var expanded by remember(region.id) { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = if (expanded) "Show less about ${region.name}" else "Show more about ${region.name}",
                role = Role.Button
            ) { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = region.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = region.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (expanded) "Less ▲" else "More ▼",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = region.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "What shapes the weather here",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                region.microclimateTraits.forEach { trait ->
                    Text(
                        text = "\u2022 $trait",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Key places",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = region.keyCities.joinToString(" \u2022 "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun LandingScreen(
    selectedRegionId: String,
    dataset: StationDatasetInfo?,
    fullDataRegions: Int,
    totalRegions: Int,
    regionOptions: List<RegionOption>,
    onSelectRegion: (String) -> Unit,
    onNavigateTo: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val region = remember(selectedRegionId) {
        CaliforniaRegionalDefinitions.ALL_REGIONS.firstOrNull { it.id == selectedRegionId }
            ?: CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO
    }
    val hasStationData = dataset != null
    val context = LocalContext.current
    val snapshotStations = remember(dataset) {
        dataset?.let { StationCsvParser(context).loadStations(it) } ?: emptyList()
    }
    val entries = remember(region.id, dataset) {
        landingEntries(
            dataset,
            TransectConfigs.forRegion(region.id),
            region.activeStationCount
        )
    }
    val appName = stringResource(R.string.app_name)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLimitedDataDialog by remember { mutableStateOf(false) }
    var showRegionMenu by remember { mutableStateOf(false) }

    // Region picker overlay: the CA icon grows from the header corner into a panel over Home
    // (about 75% of the screen, with a translucent scrim). Picking a region on its map only marks
    // a pending choice; Home changes when Confirm is tapped.
    val expand = remember { Animatable(0f) }
    var overlayVisible by remember { mutableStateOf(false) }
    var pendingRegionId by remember { mutableStateOf(selectedRegionId) }
    var iconBounds by remember { mutableStateOf(Rect.Zero) }
    var containerOrigin by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var containerWidth by remember { mutableStateOf(0f) }
    var containerHeight by remember { mutableStateOf(0f) }

    fun openOverlay() {
        pendingRegionId = selectedRegionId
        overlayVisible = true
        scope.launch { expand.animateTo(1f, tween(durationMillis = 450, easing = FastOutSlowInEasing)) }
    }
    fun closeOverlay() {
        scope.launch {
            expand.animateTo(0f, tween(durationMillis = 300, easing = FastOutSlowInEasing))
            overlayVisible = false
        }
    }
    BackHandler(enabled = overlayVisible) { closeOverlay() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(vertical = 24.dp)) {
                    Text(
                        text = appName,
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

                        // Region name with a chevron: tap for the quick region picker
                        Box(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClickLabel = "Choose region", role = Role.Button) {
                                        showRegionMenu = true
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = region.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Text(
                                    text = "\u25BE",
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(start = 6.dp, end = 4.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showRegionMenu,
                                onDismissRequest = { showRegionMenu = false }
                            ) {
                                regionOptions.forEach { option ->
                                    val isCurrent = option.id == selectedRegionId
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.widthIn(min = 200.dp)
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = option.name,
                                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = option.stationCount?.let { "$it stations" } ?: "No station list",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (isCurrent) {
                                                    Text(
                                                        text = "\u2713",
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(start = 12.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            showRegionMenu = false
                                            if (!isCurrent) onSelectRegion(option.id)
                                        }
                                    )
                                }
                            }
                        }

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
                                    enabled = !overlayVisible,
                                    onClickLabel = "Change region",
                                    role = Role.Button
                                ) { openOverlay() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CaliforniaOutlineIcon(
                                    fill = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f),
                                    stroke = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .onGloballyPositioned { iconBounds = Rect(it.positionInRoot(), it.size.toSize()) }
                                        .alpha(if (overlayVisible) 0f else 1f)
                                )
                            }
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    // This region has no full station list in this build (built-in sample or none)
                    if (dataset == null || dataset.isSample) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    onClickLabel = "About limited station data",
                                    role = Role.Button
                                ) { showLimitedDataDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Limited station data",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.15f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "i",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    RegionDetailsCard(region)
                    if (snapshotStations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        RegionSnapshotCard(snapshotStations)
                    }
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

        if (overlayVisible) {
            val t = expand.value
            val density = LocalDensity.current
            val noRipple = remember { MutableInteractionSource() }
            val targetW = containerWidth * 0.92f
            val targetH = containerHeight * 0.75f
            val targetX = (containerWidth - targetW) / 2f
            val targetY = (containerHeight - targetH) / 2f
            val startX = iconBounds.left - containerOrigin.x
            val startY = iconBounds.top - containerOrigin.y
            // Scrim: tapping it closes the overlay without changing the region
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f * t))
                    .clickable(interactionSource = noRipple, indication = null) { closeOverlay() }
            )
            Box(
                modifier = Modifier
                    .offset { IntOffset(lerp(startX, targetX, t).roundToInt(), lerp(startY, targetY, t).roundToInt()) }
                    .size(
                        width = with(density) { lerp(iconBounds.width, targetW, t).toDp() },
                        height = with(density) { lerp(iconBounds.height, targetH, t).toDp() }
                    )
                    .clip(RoundedCornerShape(lerp(10f, 24f, t).dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = t))
                    // Taps inside the panel must not reach the scrim
                    .clickable(interactionSource = noRipple, indication = null) { },
                contentAlignment = Alignment.Center
            ) {
                CaliforniaOutlineIcon(
                    fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    stroke = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(lerp(0f, 32f, t).dp)
                        .alpha((1f - t * 2f).coerceAtLeast(0f))
                )
                if (t > 0.5f) {
                    CaliforniaRegionPickerPanel(
                        pendingRegionId = pendingRegionId,
                        onPickRegion = { pendingRegionId = it },
                        onConfirm = {
                            if (pendingRegionId != selectedRegionId) onSelectRegion(pendingRegionId)
                            closeOverlay()
                        },
                        onClose = { closeOverlay() },
                        modifier = Modifier
                            .requiredSize(
                                width = with(density) { targetW.toDp() },
                                height = with(density) { targetH.toDp() }
                            )
                            .alpha(((t - 0.5f) / 0.5f).coerceIn(0f, 1f))
                    )
                }
            }
        }
        }
    }

    if (showLimitedDataDialog) {
        AlertDialog(
            onDismissRequest = { showLimitedDataDialog = false },
            title = { Text("Limited station data") },
            text = {
                Text(
                    "$fullDataRegions of $totalRegions regions have full station lists in " +
                        "this build. The rest show no stations, and Bay Area / Sacramento shows a " +
                        "small built-in sample. Readings for the stations that are shown are live " +
                        "from the National Weather Service.\n\n" +
                        "To load all station lists, add the station files (stations_<regionId>.csv) to " +
                        "app/src/debug/assets and rebuild."
                )
            },
            confirmButton = {
                TextButton(onClick = { showLimitedDataDialog = false }) { Text("OK") }
            }
        )
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
                    text = "About $appName",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "$appName is an interactive explorer for California's " +
                        "microclimates — combining live NOAA station data, topographic maps, and " +
                        "regional climate visualizations across the state, from coastal fog belts to " +
                        "alpine Sierra ridgelines.\n\nCurrent scope: California.\n\n" +
                        "Live readings: temperature, humidity, wind and pressure come from the " +
                        "National Weather Service (weather.gov). Stations report about once an hour, " +
                        "so a reading can be up to an hour or more old; each card shows when it was " +
                        "taken. Readings are for general interest, not for safety or aviation decisions."
                )
            }
        )
    }
}
