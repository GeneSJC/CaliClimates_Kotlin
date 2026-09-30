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
import io.github.genesjc.microclimatefinder.ui.components.RegionPickerButton
import io.github.genesjc.microclimatefinder.ui.components.StationCard
import io.github.genesjc.microclimatefinder.ui.components.StationSummaryTile
import io.github.genesjc.microclimatefinder.ui.components.MapViewSwitch
import io.github.genesjc.microclimatefinder.ui.components.HeroTile
import io.github.genesjc.microclimatefinder.ui.components.TerrainLegend
import io.github.genesjc.microclimatefinder.ui.components.RegionPickerOverlay
import io.github.genesjc.microclimatefinder.ui.components.rememberRegionPickerState
import io.github.genesjc.microclimatefinder.domain.model.TerrainType
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.data.local.StationCsvParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.Popup
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

private fun landingEntries(dataset: StationDatasetInfo?): List<LandingEntry> {
    val entries = mutableListOf<LandingEntry>()
    if (dataset != null) {
        entries += LandingEntry(
            title = "Topography",
            description = "Stations on a map of the region. Zoom, pan and tap a station.",
            iconChar = "▲",
            target = AppScreen.STATION_MAP
        )
        entries += LandingEntry(
            title = "Elevation",
            description = "Stations sorted by elevation, from lowest to highest, with live sky/weather theming.",
            iconChar = "⛰️",
            target = AppScreen.ELEVATION_PROFILE
        )
    }
    return entries
}

/** One row in the quick region picker. */
data class RegionOption(val id: String, val name: String, val stationCount: Int?)

/** The mix of terrain types in the region, as a bar with counts. */
@Composable
private fun TerrainMixCard(stations: List<MicroclimateStation>) {
    val mix = TerrainType.values()
        .map { type -> type to stations.count { it.terrain == type } }
        .filter { it.second > 0 }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = sheetCardColor(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = "Terrain mix",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
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
            Spacer(modifier = Modifier.height(6.dp))
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

/** The Station List link, with the elevation range below it. */
@Composable
private fun RegionStatsCard(stations: List<MicroclimateStation>, onOpenList: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = sheetCardColor(),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Station List \u203A",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClickLabel = "Open the Station List", role = Role.Button) { onOpenList() }
                    .padding(vertical = 4.dp)
            )
            if (stations.isNotEmpty()) {
                val minElev = stations.minOf { it.elevationMeters }
                val maxElev = stations.maxOf { it.elevationMeters }
                Column {
                    Text(
                        text = "${"%,d".format(minElev)}\u2013${"%,d".format(maxElev)} m",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "elevation range",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

/**
 * About the current region, as a half-width tile. "More" grows it, in width and height, into a
 * full-width panel floating over the page, so nothing on the page moves.
 */
@Composable
private fun RegionDetailsCard(region: CaliforniaRegion, fullWidthPx: Int) {
    val cardColor = sheetCardColor()
    HeroTile(
        fullWidthPx = fullWidthPx,
        tileColor = cardColor,
        panelColor = cardColor.compositeOver(MaterialTheme.colorScheme.background),
        openLabel = "Show more about ${region.name}",
        closeLabel = "Show less about ${region.name}",
        resetKey = region.id,
        tile = { RegionDetailsTile(region) },
        panel = { RegionDetailsBody(region, expanded = true) }
    )
}

/** The selected station as a half-width tile that grows into the full compact station card. */
@Composable
private fun StationHeroTile(station: MicroclimateStation, fullWidthPx: Int) {
    HeroTile(
        fullWidthPx = fullWidthPx,
        tileColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        tileBorder = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        // The card draws its own tint and border; this backs it so nothing shows through
        panelColor = MaterialTheme.colorScheme.background,
        openLabel = "Show the full station card",
        closeLabel = "Hide the full station card",
        resetKey = station.id,
        tile = { StationSummaryTile(station) },
        panel = { StationCard(station = station, isSelected = true, onSelect = {}, compact = true) }
    )
}

/** The half-width tile: title, a short summary, and the More link. */
@Composable
private fun RegionDetailsTile(region: CaliforniaRegion) {
    Column(modifier = Modifier.padding(12.dp)) {
        Text(
            text = region.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = region.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "More \u25BC",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** The expanded region panel: title, description, traits and key places. */
@Composable
private fun RegionDetailsBody(region: CaliforniaRegion, expanded: Boolean = true) {
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

/** How long the info sheet takes to slide in and out. */
private const val SHEET_OPEN_MS = 420
private const val SHEET_CLOSE_MS = 320
/** How much the page behind the sheet is dimmed (0 = not at all; the layer still catches taps). */
private const val SHEET_DIM = 0f

/** The sheet is solid at its top and fades toward its bottom, where the start screen shows through. */
private const val SHEET_OPACITY_TOP = 1f
private const val SHEET_OPACITY_BOTTOM = 0.2f

/** The sheet's fill: a pastel green in light mode, a deep green in dark mode, fading downward. */
@Composable
private fun sheetBrush(): Brush {
    val tint = if (isSystemInDarkTheme()) Color(0xFF1F3A2A) else Color(0xFFA8D5B0)
    return Brush.verticalGradient(
        listOf(tint.copy(alpha = SHEET_OPACITY_TOP), tint.copy(alpha = SHEET_OPACITY_BOTTOM))
    )
}

/** The cards on the sheet: fully opaque, in a lighter tone than the tint, so they stand out from it. */
@Composable
private fun sheetCardColor(): Color = MaterialTheme.colorScheme.surface


/**
 * A panel that slides in from the right edge, over the page. The page behind it stays sharp, and
 * the panel itself fades from solid at the top to mostly see-through at the bottom. Tap outside,
 * swipe it to the right, or press Back to close.
 */
@Composable
private fun BoxScope.InfoSideSheet(
    reveal: Animatable<Float, AnimationVector1D>,
    topPx: Float,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    val visible by remember { derivedStateOf { reveal.value > 0f } }
    // Wait until the anchor is measured (it is not on the first frame after coming back to Home)
    if (!visible || topPx <= 0f) return
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Invisible layer over the page; a tap on it closes the sheet
    Box(
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer { alpha = reveal.value }
            .background(Color.Black.copy(alpha = SHEET_DIM))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Close the region info"
            ) { onClose() }
    )
    var originY by remember { mutableStateOf(0f) }
    BoxWithConstraints(
        modifier = Modifier
            .matchParentSize()
            .onGloballyPositioned { originY = it.positionInRoot().y }
    ) {
        // The sheet's top edge lines up with the top of the cards in the start screen's first row
        val topPx = (topPx - originY).coerceAtLeast(0f)
        val panelWidth = maxWidth * 0.9f
        val panelWidthPx = with(density) { panelWidth.toPx() }
        val dragState = rememberDraggableState { delta ->
            scope.launch { reveal.snapTo((reveal.value - delta / panelWidthPx).coerceIn(0f, 1f)) }
        }
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                // Level with the first row of cards, and only as tall as its content
                .offset { IntOffset(0, topPx.roundToInt()) }
                .width(panelWidth)
                .heightIn(max = maxHeight - with(density) { topPx.toDp() } - 20.dp)
                .graphicsLayer { translationX = (1f - reveal.value) * panelWidthPx }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        if (velocity > 800f || reveal.value < 0.6f) onClose() else onOpen()
                    }
                )
        ) {
            Box(modifier = Modifier.background(sheetBrush())) {
                Box(modifier = Modifier.verticalScroll(rememberScrollState())) { content() }
            }
        }
    }
}

/** What the info sheet holds: the region tile and stats, then the terrain mix. */
@Composable
private fun RegionInfoContent(
    region: CaliforniaRegion,
    hasStationData: Boolean,
    stations: List<MicroclimateStation>,
    onOpenStationList: () -> Unit
) {
    var topRowWidthPx by remember { mutableStateOf(0) }
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Region Info",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        // Top row: the region tile on the left; Station List and elevation on the right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .onSizeChanged { topRowWidthPx = it.width },
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                RegionDetailsCard(region, fullWidthPx = topRowWidthPx)
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (hasStationData) RegionStatsCard(stations, onOpenStationList)
            }
        }
        if (stations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            TerrainMixCard(stations)
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
    infoOpen: Boolean,
    onInfoOpenChange: (Boolean) -> Unit,
    homeTab: Int,
    onHomeTabChange: (Int) -> Unit,
    selectedStationId: String?,
    onStationSelected: (String) -> Unit,
    pinScale: Float,
    onPinScaleChange: (Float) -> Unit,
    mapExpanded: Boolean,
    onMapExpandedChange: (Boolean) -> Unit,
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
        landingEntries(dataset)
    }
    val appName = stringResource(R.string.app_name)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLimitedDataDialog by remember { mutableStateOf(false) }
    var showRegionMenu by remember { mutableStateOf(false) }

    val pickerState = rememberRegionPickerState()

    // Region info sheet: slides in from the right over the start screen
    // The open state lives with the caller, so the sheet is still open when Back returns from the Station List
    val infoReveal = remember { Animatable(if (infoOpen) 1f else 0f) }
    // Where the sheet starts: the top of the map (or, with no map, just under the link)
    var mapTopPx by remember { mutableStateOf(0f) }
    var rowWidthPx by remember { mutableStateOf(0) }
    var rowLeftPx by remember { mutableStateOf(0f) }
    var infoHandleBottomPx by remember { mutableStateOf(0f) }
    val sheetTopPx = if (mapTopPx > 0f) mapTopPx else infoHandleBottomPx
    val openInfo: () -> Unit = {
        onInfoOpenChange(true)
        scope.launch { infoReveal.animateTo(1f, tween(SHEET_OPEN_MS, easing = FastOutSlowInEasing)) }
    }
    val closeInfo: () -> Unit = {
        onInfoOpenChange(false)
        scope.launch { infoReveal.animateTo(0f, tween(SHEET_CLOSE_MS, easing = FastOutSlowInEasing)) }
    }
    BackHandler(enabled = infoOpen) { closeInfo() }
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
                    // The same views as the tabs on the start screen
                    entries.forEach { entry ->
                        NavigationDrawerItem(
                            label = { Text(entry.title) },
                            icon = { Text(text = entry.iconChar, fontSize = 18.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onHomeTabChange(if (entry.target == AppScreen.ELEVATION_PROFILE) 1 else 0)
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                    if (entries.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
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
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            run {
                // App Header Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        RegionPickerButton(
                            state = pickerState,
                            selectedRegionId = selectedRegionId,
                            background = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                            content = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    }
                }
            }

            // Row 1: the selected station as a half-width tile (it grows into the full card), and
            // the Terrain Legend and Region Info, stacked at the bottom beside it
            val shown = if (dataset != null) {
                snapshotStations.firstOrNull { it.id == selectedStationId } ?: snapshotStations.firstOrNull()
            } else null
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .onSizeChanged { rowWidthPx = it.width }
                    .onGloballyPositioned { rowLeftPx = it.positionInRoot().x },
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1.4f).fillMaxHeight()) {
                    if (shown != null) StationHeroTile(shown, fullWidthPx = rowWidthPx)
                }
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // The Terrain Legend at the top of the column, at the right; it opens as a full-width panel over the map
                    if (dataset != null && snapshotStations.isNotEmpty()) {
                        TerrainLegend(
                            stations = snapshotStations,
                            modifier = Modifier.padding(end = 8.dp),
                            // The dot-size slider belongs to the map, so only the Topography view has it
                            extra = { if (homeTab == 0) PinSizeSlider(value = pinScale, onValueChange = onPinScaleChange) },
                            headerFontSize = 15.84.sp,
                            expandedWidthPx = rowWidthPx,
                            panelLeftPx = rowLeftPx,
                            headerAlignment = Alignment.TopEnd,
                            headerEndInset = 8.dp
                        )
                    }
                    // Region Info at the very bottom of the column, level with the tile's last line. The sheet slides in from the right. A fixed
                    // height, so the row does not change when the link swaps with the X
                    Row(
                        modifier = Modifier.fillMaxWidth().height(26.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val handleModifier = Modifier
                            .onGloballyPositioned {
                                infoHandleBottomPx = it.positionInRoot().y + it.size.height
                            }
                            .clickable(
                                onClickLabel = if (infoOpen) "Close the region info" else "Open the region info",
                                role = Role.Button
                            ) { if (infoOpen) closeInfo() else openInfo() }
                        val handleColor = MaterialTheme.colorScheme.primary
                        if (infoOpen) {
                            // A bold X, drawn so its weight and size are exact
                            Canvas(modifier = handleModifier.padding(horizontal = 8.dp, vertical = 6.5.dp).size(13.dp)) {
                                val stroke = 2.2.dp.toPx()
                                val inset = stroke / 2f
                                drawLine(
                                    color = handleColor,
                                    start = Offset(inset, inset),
                                    end = Offset(size.width - inset, size.height - inset),
                                    strokeWidth = stroke,
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = handleColor,
                                    start = Offset(size.width - inset, inset),
                                    end = Offset(inset, size.height - inset),
                                    strokeWidth = stroke,
                                    cap = StrokeCap.Round
                                )
                            }
                        } else {
                            Text(
                                text = "\u00AB Region Info",
                                style = MaterialTheme.typography.titleLarge,
                                fontSize = 15.84.sp,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = handleColor,
                                modifier = handleModifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            run {
                Column {
                    // This region has no station data in this build
                    if (dataset == null) {
                        Spacer(modifier = Modifier.height(6.dp))
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

            // The map, with small buttons on it to switch between its Topography and Elevation views.
            // The standalone screens are unchanged; these are the same screens without their own app bar.
            if (dataset != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onGloballyPositioned { mapTopPx = it.positionInRoot().y }
                ) {
                    if (homeTab == 0) {
                        StationMapScreen(
                            dataset = dataset,
                            selectedRegionId = selectedRegionId,
                            onSelectRegion = onSelectRegion,
                            onNavigateBack = {},
                            embedded = true,
                            sharedSelectedStationId = selectedStationId,
                            onStationSelected = onStationSelected,
                            sharedPinScale = pinScale,
                            sharedExpanded = mapExpanded,
                            onExpandedChange = onMapExpandedChange
                        )
                    } else {
                        ElevationProfileScreen(
                            dataset = dataset,
                            onNavigateBack = {},
                            embedded = true,
                            sharedSelectedStationId = selectedStationId,
                            onStationSelected = onStationSelected,
                            sharedExpanded = mapExpanded,
                            onExpandedChange = onMapExpandedChange
                        )
                    }
                    MapViewSwitch(
                        selected = homeTab,
                        onSelect = onHomeTabChange,
                        modifier = Modifier.align(Alignment.TopStart).padding(start = 54.dp, top = 12.dp)
                    )
                }
            }
        }

        InfoSideSheet(reveal = infoReveal, topPx = sheetTopPx, onOpen = openInfo, onClose = closeInfo) {
            RegionInfoContent(
                region = region,
                hasStationData = hasStationData,
                stations = snapshotStations,
                onOpenStationList = { onNavigateTo(AppScreen.STATION_LIST) }
            )
        }

        RegionPickerOverlay(
            state = pickerState,
            selectedRegionId = selectedRegionId,
            onSelectRegion = onSelectRegion
        )
        }
    }

    if (showLimitedDataDialog) {
        AlertDialog(
            onDismissRequest = { showLimitedDataDialog = false },
            title = { Text("Limited station data") },
            text = {
                Text(
                    "$fullDataRegions of $totalRegions regions have station data in this build. " +
                        "A region without it shows no stations. Readings for the stations that are " +
                        "shown are live from the National Weather Service.\n\n" +
                        "Each region's stations ship in a small data file in the app's assets " +
                        "(a.txt to f.txt). A region with none has a missing or damaged file: check " +
                        "Logcat (tag StationPack). To try other data, add stations_<regionId>.csv to " +
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
                        "taken. Readings are for general interest, not for safety or aviation decisions.\n\n" +
                        "Terrain: the shading on the Topography map is drawn from public elevation data " +
                        "(USGS 3DEP, NASA SRTM and other public sources), via AWS Open Data Terrain Tiles."
                )
            }
        )
    }
}
