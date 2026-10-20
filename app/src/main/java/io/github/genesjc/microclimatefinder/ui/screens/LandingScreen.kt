package io.github.genesjc.microclimatefinder.ui.screens

import io.github.genesjc.microclimatefinder.ui.components.regionPickerColors
import io.github.genesjc.microclimatefinder.domain.Elevation
import androidx.compose.animation.AnimatedVisibility
import io.github.genesjc.microclimatefinder.ui.components.MapText
import io.github.genesjc.microclimatefinder.ui.components.HOME_TILE_BAND
import io.github.genesjc.microclimatefinder.ui.components.ms
import androidx.compose.ui.unit.Density
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
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
import io.github.genesjc.microclimatefinder.ui.components.MapViewButton
import io.github.genesjc.microclimatefinder.ui.components.HeroTile
import io.github.genesjc.microclimatefinder.ui.components.TerrainLegend
import io.github.genesjc.microclimatefinder.ui.components.RegionPickerOverlay
import io.github.genesjc.microclimatefinder.ui.components.rememberRegionPickerState
import io.github.genesjc.microclimatefinder.domain.model.TerrainType
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.data.local.StationRepository
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import io.github.genesjc.microclimatefinder.ui.components.TerrainLegendCard
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.genesjc.microclimatefinder.data.remote.ObservationResult
import kotlinx.coroutines.flow.StateFlow
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
import io.github.genesjc.microclimatefinder.EditionConfig
import io.github.genesjc.microclimatefinder.R
import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegion
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegionalDefinitions
import io.github.genesjc.microclimatefinder.ui.components.AboutDialog
import io.github.genesjc.microclimatefinder.ui.components.MenuDrawerContent
import io.github.genesjc.microclimatefinder.ui.components.CaliforniaOutlineIcon
import io.github.genesjc.microclimatefinder.ui.navigation.AppScreen
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** One small white button with a blue glyph, in the style of the map's other controls. */
@Composable
private fun MapControlButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.White.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier
            .height(32.dp)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
    ) {
        Box(modifier = Modifier.padding(horizontal = 9.dp), contentAlignment = Alignment.Center) { content() }
    }
}

/**
 * The row of small buttons at the map's top left. On Topography: the menu at the left, the north indicator in the
 * middle and the About (i) at the right. On Elevation: the menu, then a Stations drop-down (the north indicator and
 * the About button are not shown there). [stations] is only used by the drop-down.
 */
@Composable
private fun MapControlRow(
    onElevation: Boolean,
    onMenu: () -> Unit,
    /** Topography only: shows the locate button (the selected station is out of view) and what it does when tapped. */
    showLocate: Boolean = false,
    onLocate: () -> Unit = {},
    stations: List<MicroclimateStation> = emptyList(),
    selectedStationId: String? = null,
    onPickStation: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val blue = Color(0xFF0284C7)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        MapControlButton("Open the menu", onMenu) {
            Text(text = "\u2630", fontSize = 18.sp, color = blue)
        }
        if (onElevation) {
            StationsDropdown(stations, selectedStationId, onPickStation)
        } else {
            // The north indicator, as a button-sized tile (not a button: it does nothing)
            Box(modifier = Modifier.width(44.dp).height(32.dp), contentAlignment = Alignment.Center) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.90f),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "\u25B2 N", fontSize = ms(8f), fontWeight = FontWeight.ExtraBold, color = blue)
                    }
                }
            }
            // The empty box (four corner brackets): brings the selected station back into view. It is only there while
            // the selected station is out of the visible map (panned away, or zoomed in past it)
            androidx.compose.animation.AnimatedVisibility(
                visible = showLocate,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut()
            ) {
                MapControlButton("Show the selected station", onLocate) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        val stroke = 2.dp.toPx()
                        val arm = size.width * 0.38f
                        val w = size.width
                        val h = size.height
                        fun line(a: Offset, b: Offset) = drawLine(blue, a, b, strokeWidth = stroke, cap = StrokeCap.Round)
                        line(Offset(0f, 0f), Offset(arm, 0f)); line(Offset(0f, 0f), Offset(0f, arm))
                        line(Offset(w, 0f), Offset(w - arm, 0f)); line(Offset(w, 0f), Offset(w, arm))
                        line(Offset(0f, h), Offset(arm, h)); line(Offset(0f, h), Offset(0f, h - arm))
                        line(Offset(w, h), Offset(w - arm, h)); line(Offset(w, h), Offset(w, h - arm))
                    }
                }
            }
        }
    }
}

/** The Elevation view's "Stations" drop-down: every station by name; picking one makes it the selected station. */
@Composable
private fun StationsDropdown(stations: List<MicroclimateStation>, selectedStationId: String?, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val ink = Color(0xFF334155)
    val sorted = remember(stations) { stations.sortedBy { it.name.lowercase() } }
    var query by remember { mutableStateOf("") }
    val matches = remember(sorted, query) {
        val q = query.trim()
        if (q.isEmpty()) sorted else sorted.filter { it.name.contains(q, ignoreCase = true) }
    }
    Box {
        // The same type as the Topography / Elevation buttons
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color.White.copy(alpha = 0.90f),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier
                .height(32.dp)
                .clickable(onClickLabel = "Choose a station", role = Role.Button) { open = true }
        ) {
            Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Stations", fontSize = ms(9f), fontWeight = FontWeight.Bold, color = ink)
                Text(text = " \u25BE", fontSize = ms(9f), fontWeight = FontWeight.Bold, color = ink)
            }
        }
        val lightLook = MaterialTheme.colorScheme.background.luminance() > 0.5f
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = if (lightLook) Color(0xFFF1E2C4) else MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, if (lightLook) Color(0xFF3A2A1E).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.25f)),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {
            // Search box at the top: type to filter the list as you go (the full list shows until you do)
            androidx.compose.runtime.LaunchedEffect(open) { if (open) query = "" }
            Row(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .widthIn(min = 230.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.75f))
                    .border(1.dp, Color(0xFF3A2A1E).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // A magnifying glass, drawn
                Canvas(modifier = Modifier.size(16.dp)) {
                    val stroke = 2.dp.toPx()
                    val r = size.width * 0.32f
                    drawCircle(ink, radius = r, center = Offset(r + stroke / 2f, r + stroke / 2f), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                    drawLine(ink, Offset(r * 1.75f + stroke, r * 1.75f + stroke), Offset(size.width - stroke / 2f, size.height - stroke / 2f), strokeWidth = stroke, cap = StrokeCap.Round)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search stations", fontSize = ms(9f), color = ink.copy(alpha = 0.55f))
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = ms(9f), color = Color(0xFF1E293B)),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(ink),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (query.isNotEmpty()) {
                    Text(
                        text = "\u2715",
                        fontSize = ms(9f),
                        color = ink,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClickLabel = "Clear the search") { query = "" }
                            .padding(start = 8.dp)
                    )
                }
            }
            // The list scrolls under the search box (which stays put)
            Column(
                modifier = Modifier
                    .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.6f).dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (matches.isEmpty()) {
                    Text(
                        text = "No stations match",
                        fontSize = ms(9f),
                        color = ink.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                matches.forEach { station ->
                    val current = station.id == selectedStationId
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = station.name,
                                fontSize = ms(9f),
                                fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            open = false
                            onPick(station.id)
                        }
                    )
                }
            }
        }
    }
}

/** One row in the quick region picker. */
/** The height of the three header widgets on Home. */
private val HEADER_HEIGHT = 42.dp

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
private fun RegionDetailsCard(region: CaliforniaRegion, fullWidthPx: Int, folded: Boolean) {
    val cardColor = sheetCardColor()
    HeroTile(
        fullWidthPx = fullWidthPx,
        tileColor = cardColor,
        panelColor = cardColor.compositeOver(MaterialTheme.colorScheme.background),
        openLabel = "Show more about ${region.name}",
        closeLabel = "Show less about ${region.name}",
        resetKey = region.id,
        forceClosed = folded,
        tile = { RegionDetailsTile(region) },
        panel = { RegionDetailsBody(region, expanded = true) }
    )
}

/** The selected station as a half-width tile that grows into the full compact station card. */
@Composable
private fun StationHeroTile(
    station: MicroclimateStation,
    fullWidthPx: Int,
    regionStations: List<MicroclimateStation>,
    regionId: String,
    folded: Boolean,
    readingFor: (MicroclimateStation) -> StateFlow<ObservationResult?>
) {
    val reading by remember(station.id) { readingFor(station) }.collectAsStateWithLifecycle()
    HeroTile(
        fullWidthPx = fullWidthPx,
        // The same colour as the open card: its tint over the page colour, not over the backdrop
        tileColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            .compositeOver(MaterialTheme.colorScheme.background),
        tileBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        // The card draws its own tint and border; this backs it so nothing shows through
        panelColor = MaterialTheme.colorScheme.background,
        openLabel = "Show the full station card",
        closeLabel = "Hide the full station card",
        // Stays open when another station is picked from the map, and the map stays tappable behind it
        passThrough = true,
        // Half the rounding of the other cards
        tileCornerRadius = 8.dp,
        // The Region Info sheet sits above this card: opening it folds the card
        forceClosed = folded,
        tile = { StationSummaryTile(station, reading = reading) },
        panel = { close ->
            StationCard(
                station = station,
                isSelected = true,
                reading = reading,
                // Tapping the card folds it back, like Less
                onSelect = close,
                compact = true,
                elevationNote = Elevation.note(station.elevationMeters, regionId),
                // Inside the card's border: the region's elevation range, and the way to fold it back
                // The region's elevation range, at the right of the terrain chip's line
                compactFooter = {
                    if (regionStations.isNotEmpty()) {
                        Text(
                            text = "Region elevation ${Elevation.rangeFeet(regionStations.minOf { it.elevationMeters }, regionStations.maxOf { it.elevationMeters })}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                // Less at the right of the title row, where the other open cards have theirs
                compactHeaderAction = {
                    Text(
                        text = "Less \u25B2",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable(onClickLabel = "Hide the full station card", role = Role.Button) { close() }
                            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                    )
                }
            )
        }
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
            maxLines = 2,
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
private const val SHEET_OPACITY_BOTTOM = 0.4f

/** The sheet's fill: a pastel green in light mode, a deep green in dark mode, fading downward. */
@Composable
private fun sheetBrush(): Brush {
    val tint = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1F3A2A) else Color(0xFF8CC79A)
    // Fully solid through the title row, then fading down
    return Brush.verticalGradient(
        0f to tint.copy(alpha = SHEET_OPACITY_TOP),
        0.12f to tint.copy(alpha = SHEET_OPACITY_TOP),
        1f to tint.copy(alpha = SHEET_OPACITY_BOTTOM)
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
    onOpenStationList: () -> Unit,
    menuOpen: Boolean,
    dotScale: Float = 1f,
    legendExtra: (@Composable () -> Unit)? = null
) {
    var topRowWidthPx by remember { mutableStateOf(0) }
    Column {
        // The title band: a solid, deeper colour than the lavender header widgets above it and the pale body
        // below, so it reads as its own bar. A California icon with this region lit, the title, and the
        // Station List link as a pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CaliforniaOutlineIcon(
                    fill = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                    stroke = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    regionColors = regionPickerColors(),
                    selectedRegionId = region.id,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Region Info",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        // Off-white, not pure white, so it is easier on the eyes against the dark band
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f)
                    )
                }
                if (hasStationData) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClickLabel = "Open the Station List", role = Role.Button) { onOpenStationList() }
                    ) {
                        Text(
                            text = "Station List \u203A",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
        // The body's text follows the shared text scale (MapText), like the map labels and the station card
        val baseDensity = LocalDensity.current
        androidx.compose.runtime.CompositionLocalProvider(
            LocalDensity provides Density(baseDensity.density, baseDensity.fontScale * MapText.scale)
        ) {
        Column(modifier = Modifier.padding(16.dp)) {
        // The region tile, the full width of the sheet
        Box(modifier = Modifier.fillMaxWidth().onSizeChanged { topRowWidthPx = it.width }) {
            RegionDetailsCard(region, fullWidthPx = topRowWidthPx, folded = menuOpen)
        }
        if (stations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            // The Terrain mix card keeps its original text size for now (it does not follow the text scale)
            androidx.compose.runtime.CompositionLocalProvider(LocalDensity provides baseDensity) {
                TerrainMixCard(stations)
            }
            // The Terrain Legend, at the bottom, folded until opened
            Spacer(modifier = Modifier.height(12.dp))
            TerrainLegendCard(stations = stations, containerColor = sheetCardColor(), dotScale = dotScale, extra = legendExtra)
        }
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
    infoOpen: Boolean,
    onInfoOpenChange: (Boolean) -> Unit,
    homeTab: Int,
    onHomeTabChange: (Int) -> Unit,
    selectedStationId: String?,
    onStationSelected: (String) -> Unit,
    pinScale: Float,
    onPinScaleChange: (Float) -> Unit,
    sliderShown: Boolean,
    onSliderShownChange: (Boolean) -> Unit,
    /** The map zoom shared by both map views: read when a map is shown, reported as it changes. */
    mapZoom: () -> Float,
    onMapZoomChange: (Float) -> Unit,
    /** The live reading of a station, for its tile and card; null while it is being checked. */
    readingFor: (MicroclimateStation) -> StateFlow<ObservationResult?>,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val region = remember(selectedRegionId) {
        CaliforniaRegionalDefinitions.ALL_REGIONS.firstOrNull { it.id == selectedRegionId }
            ?: CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO
    }
    val hasStationData = dataset != null
    val context = LocalContext.current
    val snapshotStations = remember(dataset) {
        dataset?.let { StationRepository.stations(context, it) } ?: emptyList()
    }
    val appName = stringResource(R.string.app_name)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    // The menu drawer is the top layer: while it is opening or open, the floating cards fold
    val menuDrawerShowing = drawerState.targetValue != DrawerValue.Closed
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
    // The Topography / Elevation buttons' height (it follows the text size), measured
    val mapDensity = LocalDensity.current
    // On the Elevation view the map grows out into the page margins (west, east and south; never up), so the water
    // reaches the screen edges; eased both ways when switching views
    val expandT by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (homeTab == 1) 1f else 0f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "mapExpand"
    )
    var viewSwitchHeight by remember { mutableStateOf(34.dp) }
    // Reported by the Topography map: is the selected station out of view; and a counter the locate button bumps
    var selectedOffscreen by remember { mutableStateOf(false) }
    var recenterTick by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    var rowWidthPx by remember { mutableStateOf(0) }
    var stationTileTopPx by remember { mutableStateOf(0f) }
    val sheetTopPx = stationTileTopPx
    val openInfo: () -> Unit = {
        onInfoOpenChange(true)
        scope.launch { infoReveal.animateTo(1f, tween(SHEET_OPEN_MS, easing = FastOutSlowInEasing)) }
    }
    val closeInfo: () -> Unit = {
        onInfoOpenChange(false)
        scope.launch { infoReveal.animateTo(0f, tween(SHEET_CLOSE_MS, easing = FastOutSlowInEasing)) }
    }
    BackHandler(enabled = infoOpen) { closeInfo() }
    // Region Info is a Topography feature: switching to Elevation closes it
    LaunchedEffect(homeTab) { if (homeTab == 1 && infoOpen) closeInfo() }
    ModalNavigationDrawer(
        drawerState = drawerState,
        // The menu button opens it; a swipe must not compete with the map's drag to pan (it still closes by swipe)
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            // A narrow side panel only as tall as its content, starting at the header's top edge (the Region Info
            // sheet comes in lower, from the right edge, so the two stagger). The page shows behind it; a tap there,
            // a swipe to the left, or Back closes it.
            val menuWidth = (LocalConfiguration.current.screenWidthDp * 0.66f).dp
            Surface(
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 14.dp)
                    .width(menuWidth)
            ) {
              Box {
                Box(modifier = Modifier.background(sheetBrush())) {
                MenuDrawerContent(
                    appName = appName,
                    buildLabel = BuildInfo.LABEL,
                    // Only the demo edition has the Demos & Previews hub
                    showDemos = EditionConfig.HAS_DEMOS,
                    darkMode = darkMode,
                    onDarkModeChange = onDarkModeChange,
                    onOpenDemos = {
                        scope.launch { drawerState.close() }
                        onNavigateTo(AppScreen.DEMOS_AND_PREVIEWS)
                    },
                    onOpenAbout = {
                        scope.launch { drawerState.close() }
                        showAboutDialog = true
                    }
                )
                }
                // A small grip at the right edge: the cue that the panel can be swiped closed
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 5.dp)
                        .size(width = 4.dp, height = 36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                )
              }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(start = 5.dp, end = 5.dp, top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            run {
            Column {
            run {
                // App header: two separate widgets with the backdrop showing between them, each with the
                // About-dialog gradient: (1) the region (its name, and under it the menu and About buttons at the left
                // and the Region Info link at the right), (2) the region picker.
                val headerShape = RoundedCornerShape(8.dp)
                val headerFill = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    .compositeOver(MaterialTheme.colorScheme.background)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    // A wider gap between the region widget and the picker widget, so more terrain shows between them (8dp, then 16dp)
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Widget 2: the region name with a chevron (tap for the region list), and the Region Info link
                    // on the line below, in the same card. The Box is the list's anchor.
                    Box(modifier = Modifier.weight(1f).height(HEADER_HEIGHT)) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(headerShape)
                                // The tile's flat tint and 1 dp outline, so the name and Info link read clearly
                                .background(headerFill)
                                .border(1.dp, MaterialTheme.colorScheme.primary, headerShape)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClickLabel = "Choose region", role = Role.Button) {
                                        showRegionMenu = true
                                    }
                                    .padding(vertical = 0.dp)
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = region.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        // 25% smaller than the title style, so more of a long name shows
                                        fontSize = 20.6.sp,
                                        lineHeight = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Text(
                                        text = "\u25BE",
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(start = 6.dp, end = 4.dp)
                                    )
                                }
                                // The region picker, at the right of the name's row (it belongs with the drop-down)
                                Spacer(modifier = Modifier.width(8.dp))
                                // In a small button of its own, so it reads as tappable
                                Box(
                                    modifier = Modifier
                                        .size(width = 46.dp, height = 34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.6f))
                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    RegionPickerButton(
                                        state = pickerState,
                                        selectedRegionId = selectedRegionId,
                                        background = Color.Transparent,
                                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                                        border = Color.Transparent,
                                        size = 34.dp,
                                        iconSize = 28.dp
                                    )
                                }
                            }

                        }
                            // The region list, dressed like the rest of the app: a warm sand panel with a fine
                            // outline, each region marked with its own map colour, the current one tinted
                            val menuColors = regionPickerColors()
                            val lightLook = MaterialTheme.colorScheme.background.luminance() > 0.5f
                            DropdownMenu(
                                expanded = showRegionMenu,
                                onDismissRequest = { showRegionMenu = false },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = if (lightLook) Color(0xFFF1E2C4) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (lightLook) Color(0xFF3A2A1E).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.25f)
                                ),
                                tonalElevation = 0.dp,
                                shadowElevation = 8.dp
                            ) {
                                regionOptions.forEach { option ->
                                    val isCurrent = option.id == selectedRegionId
                                    val regionColor = menuColors[option.id] ?: MaterialTheme.colorScheme.primary
                                    DropdownMenuItem(
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isCurrent) regionColor.copy(alpha = 0.22f) else Color.Transparent),
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.widthIn(min = 200.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .background(regionColor)
                                                        .border(1.dp, Color(0xFF3A2A1E).copy(alpha = 0.35f), CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = option.name,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (isCurrent) {
                                                    Text(
                                                        text = "\u2713",
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
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

                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            }
            }
            val shown = if (dataset != null) {
                snapshotStations.firstOrNull { it.id == selectedStationId } ?: snapshotStations.firstOrNull()
            } else null
            run {
                Column {
                    // This region has no station data in this build
                    if (dataset == null) {
                        // No map to hold the buttons: the menu and About sit here
                        Spacer(modifier = Modifier.height(6.dp))
                        MapControlRow(onElevation = false, onMenu = { scope.launch { drawerState.open() } })
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
                        // Wider than the page by the margin on each side, in step with expandT (the page margin is 5 dp)
                        .layout { measurable, c ->
                            val e = (5.dp * expandT).roundToPx()
                            val w = c.maxWidth + 2 * e
                            val placeable = measurable.measure(c.copy(minWidth = w, maxWidth = w))
                            layout(c.maxWidth, placeable.height) { placeable.place(-e, 0) }
                        }
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
                            initialZoom = mapZoom(),
                            onZoomChange = onMapZoomChange,
                            sliderShownShared = sliderShown,
                            onSliderShownChange = onSliderShownChange,
                            // The slider's arrow button sits 4 dp under the view buttons, whatever their height
                            sliderToggleTop = 21.dp + HOME_TILE_BAND + viewSwitchHeight + 4.dp,
                            expandT = expandT,
                            onSelectedOffscreenChange = { selectedOffscreen = it },
                            recenterTick = recenterTick
                        )
                    } else {
                        ElevationProfileScreen(
                            dataset = dataset,
                            onNavigateBack = {},
                            embedded = true,
                            sharedSelectedStationId = selectedStationId,
                            onStationSelected = onStationSelected,
                            initialZoom = mapZoom(),
                            onZoomChange = onMapZoomChange,
                            sliderShownShared = sliderShown,
                            onSliderShownChange = onSliderShownChange,
                            expandT = expandT
                        )
                    }
                    // The tile and the buttons keep their place on the screen while the map grows out underneath them
                    Box(modifier = Modifier.matchParentSize().padding(horizontal = 5.dp * expandT)) {
                        // The selected station as a tile across the top of the map (it grows into the full card)
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth()
                                .padding(start = 6.dp, end = 6.dp, top = 5.dp)
                                .height(IntrinsicSize.Min)
                                .onSizeChanged { rowWidthPx = it.width },
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    // The Region Info sheet opens below this row (the tile and the Info link / X beside it), so it never covers them
                                    .onGloballyPositioned { stationTileTopPx = it.positionInRoot().y + it.size.height + with(mapDensity) { 6.dp.toPx() } }
                            ) {
                                if (shown != null) StationHeroTile(shown, fullWidthPx = rowWidthPx, regionStations = snapshotStations, regionId = region.id, folded = infoOpen || menuDrawerShowing || pickerState.visible, readingFor = readingFor)
                            }
                            // The info link, on its own to the right of the station tile; the sheet slides in from the right, the X takes
                            // its place. Topography only: the Elevation view has no Region Info, so the tile takes the whole width there
                            if (homeTab == 0) {
                                val handleColor = MaterialTheme.colorScheme.primary
                                // The link text always takes its space (hidden while the X shows), so the station tile beside it
                                // keeps one width and does not grow when the link becomes the X
                                Box(
                                    modifier = Modifier
                                        .clickable(
                                            onClickLabel = if (infoOpen) "Close the region info" else "Open the region info",
                                            role = Role.Button
                                        ) { if (infoOpen) closeInfo() else openInfo() }
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "\u00AB Info",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontSize = 15.84.sp,
                                        lineHeight = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = handleColor,
                                        modifier = Modifier.alpha(if (infoOpen) 0f else 1f)
                                    )
                                    if (infoOpen) {
                                        // A bold X, drawn so its weight and size are exact
                                        Canvas(modifier = Modifier.size(13.dp)) {
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
                                    }
                                }
                            }
                        }
                        // Below the tile, at the left: the menu, the north indicator and the About button
                        MapControlRow(
                            onElevation = homeTab == 1,
                            stations = snapshotStations,
                            selectedStationId = selectedStationId,
                            onPickStation = onStationSelected,
                            onMenu = { scope.launch { drawerState.open() } },
                            showLocate = selectedOffscreen && homeTab == 0,
                            onLocate = {
                                recenterTick++
                                // A short note at the bottom says what the button just did, so its meaning is learned in use
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(
                                        message = "Centering the map on ${shown?.name ?: "the selected station"}",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 12.dp + HOME_TILE_BAND)
                        )
                        // The two view buttons keep their places at the top right: Elevation, then Topography at the edge, as
                        // before. Elevation is raised to the row of controls; Topography stays 9 dp lower, a small offset
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 12.dp, top = 12.dp + HOME_TILE_BAND),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            MapViewButton(label = "Elevation", index = 1, selected = homeTab, onSelect = onHomeTabChange)
                            MapViewButton(
                                label = "Topography",
                                index = 0,
                                selected = homeTab,
                                onSelect = onHomeTabChange,
                                modifier = Modifier
                                    .padding(top = 9.dp)
                                    .onSizeChanged { viewSwitchHeight = with(mapDensity) { it.height.toDp() } }
                            )
                        }
                    }
                }
            }
        }

        InfoSideSheet(reveal = infoReveal, topPx = sheetTopPx, onOpen = openInfo, onClose = closeInfo) {
            RegionInfoContent(
                region = region,
                hasStationData = hasStationData,
                stations = snapshotStations,
                onOpenStationList = { onNavigateTo(AppScreen.STATION_LIST) },
                // The region picker is above the floating cards too: they fold while it is showing
                menuOpen = menuDrawerShowing || pickerState.visible,
                // The dot-size slider belongs to the map, so only the Topography view has it
                dotScale = if (homeTab == 0) pinScale else 1f,
                legendExtra = { if (homeTab == 0) PinSizeSlider(value = pinScale, onValueChange = onPinScaleChange) }
            )
        }

        // Short messages at the bottom (for example what the locate button just did)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 14.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(8.dp),
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White
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
        AboutDialog(appName = appName, buildLabel = BuildInfo.LABEL, onClose = { showAboutDialog = false })
    }
}
