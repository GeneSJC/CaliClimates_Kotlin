package io.github.genesjc.microclimatefinder.ui.screens

import io.github.genesjc.microclimatefinder.domain.Elevation
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.FilterQuality
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.produceState
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.microclimatefinder.data.local.LocalNewsRepository
import io.github.genesjc.microclimatefinder.data.local.StationRepository
import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaGeography
import io.github.genesjc.microclimatefinder.data.local.TerrainImage
import io.github.genesjc.microclimatefinder.data.local.TerrainMaps
import io.github.genesjc.microclimatefinder.ui.components.MapProjection
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.domain.model.TerrainType
import io.github.genesjc.microclimatefinder.ui.components.LegendItem
import io.github.genesjc.microclimatefinder.ui.components.MapExpandToggle
import io.github.genesjc.microclimatefinder.ui.components.TerrainLegend
import io.github.genesjc.microclimatefinder.ui.components.RegionPickerButton
import io.github.genesjc.microclimatefinder.ui.components.RegionPickerOverlay
import io.github.genesjc.microclimatefinder.ui.components.rememberRegionPickerState
import io.github.genesjc.microclimatefinder.ui.components.MAX_MAP_ZOOM
import io.github.genesjc.microclimatefinder.ui.components.ZoomSlider
import io.github.genesjc.microclimatefinder.ui.components.MapText
import io.github.genesjc.microclimatefinder.ui.components.HOME_TILE_BAND
import io.github.genesjc.microclimatefinder.ui.components.ms
import io.github.genesjc.microclimatefinder.ui.components.animateZoom
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import io.github.genesjc.microclimatefinder.ui.components.clampPan
import io.github.genesjc.microclimatefinder.ui.components.labelBudget
import io.github.genesjc.microclimatefinder.ui.components.panForZoom
import io.github.genesjc.microclimatefinder.ui.components.rememberLabelFades
import io.github.genesjc.microclimatefinder.ui.components.zoomDotGrowth
import io.github.genesjc.microclimatefinder.ui.components.StationCard

@Composable
fun StationMapScreen(
    dataset: StationDatasetInfo,
    selectedRegionId: String,
    onSelectRegion: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** True when shown inside another screen (the Home tabs): no app bar and no region picker of its own. */
    embedded: Boolean = false,
    /** A station selected elsewhere (the other Home tab), and a way to report selections back. */
    sharedSelectedStationId: String? = null,
    onStationSelected: ((String) -> Unit)? = null,
    /** The dot size, when Home owns it (its Terrain Legend panel holds the slider). */
    sharedPinScale: Float? = null,
    /** The zoom to start at, and a report of each change (Home keeps the zoom when it switches views). */
    initialZoom: Float = 1f,
    onZoomChange: ((Float) -> Unit)? = null,
) {
    val context = LocalContext.current
    val stations = remember(dataset) { StationRepository.stations(context, dataset) }

    // The selected station can be shared with another screen (Home keeps it across its two tabs)
    // The region's terrain picture, loaded off the main thread; the map shows a plain base until it arrives
    val terrain by produceState<TerrainImage?>(null, dataset.regionId) {
        value = withContext(Dispatchers.IO) { TerrainMaps.load(context, dataset.regionId) }
    }
    var localSelectedId by remember { mutableStateOf(stations.firstOrNull()?.id) }
    val selectedStationId = stations.firstOrNull { it.id == sharedSelectedStationId }?.id ?: localSelectedId
    var localPinScale by remember { mutableStateOf(DEFAULT_PIN_SCALE) }
    val pinScale = sharedPinScale ?: localPinScale

    val selectedStation = stations.find { it.id == selectedStationId } ?: stations.firstOrNull()
    val pickerState = rememberRegionPickerState()
    // The map starts at its regular height; the toggle on the map stretches it, slowly, to fill the screen
    var localExpanded by remember { mutableStateOf(false) }
    // On Home the map always fills the space; the standalone screen keeps the Expand / Shrink toggle
    val mapExpanded = embedded || localExpanded
    val pageScroll = rememberScrollState()
    val density = LocalDensity.current
    var aboveMapPx by remember { mutableStateOf(0) }

    // Top and bottom padding of the page: the embedded map has no app bar, so less above it
    val mapVerticalPadding = if (embedded) 16.dp else 32.dp
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    // Expanded height: the screen less the padding and everything above the map
    val expandedMapHeight = with(density) {
        ((maxHeight - mapVerticalPadding).roundToPx() - aboveMapPx).toDp()
    }.coerceAtLeast(MAP_REGULAR_HEIGHT)
    val mapHeight by animateDpAsState(
        targetValue = if (mapExpanded) expandedMapHeight else MAP_REGULAR_HEIGHT,
        animationSpec = tween(MAP_RESIZE_MS, easing = FastOutSlowInEasing),
        label = "mapHeight"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (embedded) PaddingValues(top = 0.dp, bottom = 16.dp) else PaddingValues(16.dp))
            .verticalScroll(pageScroll)
    ) {
      Column(modifier = Modifier.onSizeChanged { aboveMapPx = it.height }) {
        if (!embedded) {
        // App Bar
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

            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = "Topography",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${dataset.title} • ${dataset.stationCount} stations",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RegionPickerButton(state = pickerState, selectedRegionId = selectedRegionId)
        }

        Spacer(modifier = Modifier.height(14.dp))
        }

        // The selected station card; embedded, Home shows it above the tabs
        if (!embedded) {
        if (selectedStation != null) {
            StationCard(
                station = selectedStation,
                isSelected = true,
                onSelect = { /* Already selected */ },
                compact = true
            )
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Tap any station pin on the map to inspect microclimate data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        }

        // 2. Terrain legend: one line until opened, then this region's terrain types with counts.
        // Embedded, Home shows it beside the Region Info link.
        if (!embedded) {
        TerrainLegend(
            stations = stations,
            // Dot size lives here, out of the way, instead of in a row under the map
            extra = { PinSizeSlider(value = pinScale, onValueChange = { localPinScale = it }) }
        )

        Spacer(modifier = Modifier.height(6.dp))
        }
      }

        // 3. The map with its dot-size slider: regular height, or the rest of the screen when expanded
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(mapHeight)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                // A 1dp outline, for a crisper edge
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxSize()
            ) {
                SpatialMapCanvas(
                    stations = stations,
                    terrain = terrain,
                    embedded = embedded,
                    selectedStationId = selectedStationId,
                    pinScale = pinScale,
                    initialZoom = initialZoom,
                    onZoomChange = onZoomChange,
                    onSelectStation = {
                    localSelectedId = it.id
                    onStationSelected?.invoke(it.id)
                }
                )
            }
            // Expand / shrink toggle, just left of the top of the zoom slider (not on Home, where the map always fills the space)
            if (!embedded) {
            MapExpandToggle(
                expanded = mapExpanded,
                onToggle = {
                    localExpanded = !mapExpanded
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    // Just left of the top of the zoom slider
                    .padding(end = 50.dp, top = 56.dp)
            )
            }
        }

    }
    if (!embedded) {
        RegionPickerOverlay(
            state = pickerState,
            selectedRegionId = selectedRegionId,
            onSelectRegion = onSelectRegion
        )
    }
    }
}

/** How much room the map leaves around the stations, as a fraction of their spread. */
private const val FRAME_MARGIN = 0.08

/** The map's regular height, before it is expanded to fill the screen. */
private val MAP_REGULAR_HEIGHT = 300.dp

/** How long the map takes to grow to full height and back. */
private const val MAP_RESIZE_MS = 700

/** How far the map can be dragged past its edge, as a fraction of the map's size, at any zoom. */
private const val MAP_OVERSCROLL = 0.12f

/** Dots start at the largest size the Dot size slider allows, so they are easy to see and tap. */
internal const val DEFAULT_PIN_SCALE = 2.5f

/** Horizontal dot-size control under the map: small dot to large dot. */
@OptIn(ExperimentalMaterial3Api::class) // custom slider thumb
@Composable
internal fun PinSizeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Dot size",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(5.dp)
        ) {}
        Slider(
            value = value,
            onValueChange = { onValueChange((it * 10f).roundToInt() / 10f) },
            valueRange = 1.0f..2.5f,
            thumb = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(20.dp)
                ) {}
            },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
        )
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp)
        ) {}
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "${(value * 10).toInt() / 10f}x",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** A landmark's label text and its estimated width: fixed per station, so made once, not on every zoom or pan frame. */
private class LandmarkLabel(val station: MicroclimateStation, val city: String, val elevText: String, val widthDp: Dp)

private data class StationLabelPlan(
    val station: MicroclimateStation,
    val city: String,
    val elevText: String,
    val xDp: Dp,
    val yDp: Dp
)

@Composable
private fun SpatialMapCanvas(
    stations: List<MicroclimateStation>,
    terrain: TerrainImage?,
    embedded: Boolean,
    selectedStationId: String?,
    pinScale: Float = 1.0f,
    initialZoom: Float = 1f,
    onZoomChange: ((Float) -> Unit)? = null,
    onSelectStation: (MicroclimateStation) -> Unit
) {
    if (stations.isEmpty()) return

    // Geographic bounding box with a margin of 8%
    val rawMinLat = remember(stations) { stations.minOf { it.latitude } }
    val rawMaxLat = remember(stations) { stations.maxOf { it.latitude } }
    val rawMinLon = remember(stations) { stations.minOf { it.longitude } }
    val rawMaxLon = remember(stations) { stations.maxOf { it.longitude } }

    val rawLatSpan = (rawMaxLat - rawMinLat).coerceAtLeast(0.01)
    val rawLonSpan = (rawMaxLon - rawMinLon).coerceAtLeast(0.01)

    val minLat = rawMinLat - (rawLatSpan * FRAME_MARGIN)
    val maxLat = rawMaxLat + (rawLatSpan * FRAME_MARGIN)
    val minLon = rawMinLon - (rawLonSpan * FRAME_MARGIN)
    val maxLon = rawMaxLon + (rawLonSpan * FRAME_MARGIN)

    val textMeasurer = rememberTextMeasurer()
    // Ranked by spread, so the first few are the most useful; more fade in as the map zooms
    val landmarks = remember(stations) { pickMapLandmarks(stations, minOf(stations.size, MAP_LABEL_COUNT * 8)) }

    // View transform: zoom about the map centre plus a pan in pixels
    // The labels' text and widths, made once per station list (the placement below runs on every zoom and pan frame)
    val textScale = MapText.scale
    val landmarkLabels = remember(landmarks, textScale) {
        landmarks.map { station ->
            val city = LocalNewsRepository.getCleanCityName(station.id, station.name)
                .let { if (it.length > 18) it.take(17) + "\u2026" else it }
            val elevText = Elevation.formatFeet(station.elevationMeters)
            // Estimated label width at the current text scale; labels that would pass the right edge end at the pin instead
            LandmarkLabel(station, city, elevText, ((city.length + elevText.length + 1) * 4.9f * textScale + 12f).dp)
        }
    }

    var zoom by remember(stations) { mutableStateOf(initialZoom.coerceIn(1f, MAX_MAP_ZOOM)) }
    androidx.compose.runtime.LaunchedEffect(zoom) { onZoomChange?.invoke(zoom) }
    var pan by remember(stations) { mutableStateOf(Offset.Zero) }
    // Button, rail-tap and double-tap zooms animate (so labels fade smoothly); pinch and drag stay direct
    val zoomScope = rememberCoroutineScope()
    var zoomJob by remember { mutableStateOf<Job?>(null) }
    fun animateZoomTo(newZoom: Float, anchor: Offset, w: Float, h: Float) {
        zoomJob?.cancel()
        zoomJob = zoomScope.launch {
            animateZoom(zoom, pan, newZoom, anchor, w, h, overscroll = MAP_OVERSCROLL) { z, p -> zoom = z; pan = p }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                // Pinch to zoom, drag to pan
                detectTransformGestures { centroid, panChange, zoomChange, _ ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val newZoom = (zoom * zoomChange).coerceIn(1f, MAX_MAP_ZOOM)
                    pan = clampPan(
                        panForZoom(pan, zoom, newZoom, centroid, w, h, overscroll = MAP_OVERSCROLL) + panChange,
                        newZoom, w, h, overscroll = MAP_OVERSCROLL
                    )
                    zoom = newZoom
                }
            }
            .pointerInput(Unit) {
                // Double-tap: zoom in on the spot, and back out to the full map once fully zoomed
                detectTapGestures(onDoubleTap = { spot ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    if (zoom >= MAX_MAP_ZOOM * 0.99f) {
                        animateZoomTo(1f, Offset(w / 2f, h / 2f), w, h)
                    } else {
                        animateZoomTo((zoom * 2f).coerceAtMost(MAX_MAP_ZOOM), spot, w, h)
                    }
                })
            }
    ) {
        val widthDp = maxWidth
        val heightDp = maxHeight
        val density = LocalDensity.current

        val paddingPx = with(density) { 20.dp.toPx() }
        val canvasW = with(density) { widthDp.toPx() }
        val canvasH = with(density) { heightDp.toPx() }
        // One scale in both directions, so the region keeps its true shape
        // On Home the station tile floats over the top of the map: the zoomed-out map fits below it
        val topInset = if (embedded) with(density) { HOME_TILE_BAND.toPx() } else 0f
        val projection = remember(minLon, maxLon, minLat, maxLat, canvasW, canvasH, topInset) {
            MapProjection(minLon, maxLon, minLat, maxLat, canvasW, canvasH, paddingPx, topInset)
        }
        fun viewX(baseX: Float) = canvasW / 2f + (baseX - canvasW / 2f) * zoom + pan.x
        fun viewY(baseY: Float) = canvasH / 2f + (baseY - canvasH / 2f) * zoom + pan.y
        // Longitude and latitude to canvas pixels, with the current zoom and pan
        fun mapX(lon: Double): Float = viewX(projection.baseX(lon))
        fun mapY(lat: Double): Float = viewY(projection.baseY(lat))

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(stations, pinScale, projection) {
                    detectTapGestures { tapOffset ->
                        var closestStation: MicroclimateStation? = null
                        var closestDist = Float.MAX_VALUE

                        val dotScale = pinScale * zoomDotGrowth(zoom)
                        val maxTouchDist = 2000f * (dotScale * dotScale).coerceAtLeast(1f)

                        stations.forEach { station ->
                            val x = mapX(station.longitude)
                            val y = mapY(station.latitude)

                            val dx = tapOffset.x - x
                            val dy = tapOffset.y - y
                            val dist = (dx * dx + dy * dy)
                            if (dist < closestDist && dist < maxTouchDist) {
                                closestDist = dist
                                closestStation = station
                            }
                        }
                        closestStation?.let { onSelectStation(it) }
                    }
                }
        ) {
            if (terrain != null) {
                // Land colour first (the picture may not reach every edge), then the terrain, then a light grid
                drawRect(color = Color(0xFFE4EBDA))
                val b = terrain.bounds
                val left = mapX(b.minLon)
                val top = mapY(b.maxLat)
                drawImage(
                    image = terrain.image,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(terrain.image.width, terrain.image.height),
                    dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                    dstSize = IntSize((mapX(b.maxLon) - left).roundToInt(), (mapY(b.minLat) - top).roundToInt()),
                    filterQuality = FilterQuality.Medium
                )
                drawGraticule(
                    minLon = minLon, maxLon = maxLon, minLat = minLat, maxLat = maxLat,
                    padding = paddingPx, mapX = ::mapX, mapY = ::mapY,
                    zoom = zoom, textMeasurer = textMeasurer
                )
            } else {
                drawNeutralBase(
                    minLon = minLon, maxLon = maxLon, minLat = minLat, maxLat = maxLat,
                    padding = paddingPx, mapX = ::mapX, mapY = ::mapY,
                    zoom = zoom, textMeasurer = textMeasurer
                )
            }

            // 8. Station Pins: dot-size setting times the growth from zoom
            val dotScale = pinScale * zoomDotGrowth(zoom)
            stations.forEach { station ->
                val x = mapX(station.longitude)
                val y = mapY(station.latitude)
                // The selected station is drawn last, above the labels, by the overlay below
                if (station.id == selectedStationId) return@forEach
                drawStationPin(x, y, pinColor(station.terrain), dotScale, selected = false)
            }
        }

        // Areas taken by the fixed controls (compass, expand toggle, topo badge, scale bar, zoom slider) so labels stay clear of them
        val placedRects = mutableListOf(
            if (embedded) listOf(0f, 0f, widthDp.value, HOME_TILE_BAND.value) else listOf(0f, 0f, 0f, 0f),  // the floating station tile
            if (embedded) listOf(0f, HOME_TILE_BAND.value, 140f, HOME_TILE_BAND.value + 50f) else listOf(0f, 0f, 58f, 40f),  // Home's control row, or the compass
            listOf(0f, heightDp.value - 46f, 130f, heightDp.value),     // scale bar
            listOf(widthDp.value - 46f, 56f + (if (embedded) HOME_TILE_BAND.value else 0f), widthDp.value, heightDp.value),  // zoom slider
            if (embedded) listOf(widthDp.value - 170f, HOME_TILE_BAND.value, widthDp.value, HOME_TILE_BAND.value + 56f) else listOf(0f, 0f, 0f, 0f),  // Home's view buttons
            listOf(widthDp.value - 126f, 52f, widthDp.value - 46f, 84f)  // expand / shrink toggle
        )

        // Which labels have room right now. Only the target (shown or hidden) is decided here; the
        // opacity eases toward it on its own, so labels fade in and out however the zoom changed.
        val labelTargets = mutableMapOf<String, Float>()
        val stationPlans = mutableListOf<StationLabelPlan>()

        run {
            // Labels for stations picked from the loaded stations: more as the map zooms
            val stationBudget = labelBudget(MAP_LABEL_COUNT, zoom, 3f).toInt()
            var shownLabels = 0
            for (label in landmarkLabels) {
                val station = label.station
                val city = label.city
                val elevText = label.elevText
                val labelWidthDp = label.widthDp
                val pinXDp = with(density) { mapX(station.longitude).toDp() }
                val pinYDp = with(density) { mapY(station.latitude).toDp() }
                // Stations panned out of view get no label
                if (pinXDp < 0.dp || pinXDp > widthDp || pinYDp < 0.dp || pinYDp > heightDp) continue
                val xDp = if (pinXDp + labelWidthDp > widthDp - 6.dp) pinXDp - labelWidthDp else pinXDp
                val yDp = pinYDp + (if (pinYDp < 30.dp) 8.dp else (-16f * MapText.scale).dp)
                val left = xDp.value
                val top = yDp.value
                val right = left + labelWidthDp.value
                val bottom = top + 20f * MapText.scale
                val hasRoom = shownLabels < stationBudget &&
                    placedRects.none { r -> left < r[2] && right > r[0] && top < r[3] && bottom > r[1] }
                if (hasRoom) {
                    placedRects.add(listOf(left, top, right, bottom))
                    shownLabels++
                }
                labelTargets[station.id] = if (hasRoom) 1f else 0f
                stationPlans.add(StationLabelPlan(station, city, elevText, xDp, yDp))
            }
        }
        val labelFades = rememberLabelFades(labelTargets)

        // Draw every label that is at least partly visible, at its current opacity
        stationPlans.forEach { plan ->
            val fade = labelFades[plan.station.id] ?: 0f
            if (fade > 0.01f) {
                val isSelected = plan.station.id == selectedStationId
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White.copy(alpha = 0.88f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .offset(x = plan.xDp, y = plan.yDp)
                        .alpha(fade)
                        .clickable { onSelectStation(plan.station) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.8.dp)
                    ) {
                        Text(
                            text = plan.city,
                            fontSize = ms(8f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = " ${plan.elevText}",
                            fontSize = ms(7f),
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        // The selected station's marker, above every label and city name, so nothing hides it. The
        // canvas takes no touches; taps fall through to the map below.
        stations.firstOrNull { it.id == selectedStationId }?.let { station ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawStationPin(mapX(station.longitude), mapY(station.latitude), pinColor(station.terrain), pinScale * zoomDotGrowth(zoom), selected = true)
            }
        }

        // Compass Rose (Top-Left). On Home the north indicator is part of the control row Home draws, so none here.
        if (!embedded) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color.White.copy(alpha = 0.90f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = if (embedded) 50.dp + HOME_TILE_BAND else 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "▲ N",
                    fontSize = ms(8f),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0284C7)
                )
            }
        }

        }

        // Map Scale Bar (Bottom-Left): a bar drawn to the map's real scale
        val mapWidthKm = canvasW / (projection.pixelsPerKm * zoom)
        val scaleKm = listOf(1, 2, 5, 10, 20, 25, 50, 100, 200).lastOrNull { it <= mapWidthKm * 0.4 } ?: 5
        val scaleMiles = (scaleKm * 0.621371).roundToInt()
        val scaleBarDp = with(density) { (scaleKm * projection.pixelsPerKm * zoom).toFloat().toDp() }
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color.White.copy(alpha = 0.90f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 6.dp, bottom = 6.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)) {
                Box(
                    modifier = Modifier
                        .width(scaleBarDp)
                        .height(3.dp)
                        .background(Color(0xFF334155))
                )
                Text(
                    text = "$scaleMiles mi ($scaleKm km)",
                    fontSize = ms(7.5f),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
            }
        }

        // Floating zoom slider on the right edge (for now it sets the type size instead; see MapText)
        if (MapText.SLIDER_SETS_FONT) {
            ZoomSlider(
                value = MapText.scale,
                onValueChange = { MapText.scale = it },
                minValue = MapText.MIN_SCALE,
                maxValue = MapText.MAX_SCALE,
                caption = "FONT",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 56.dp + (if (embedded) HOME_TILE_BAND else 0.dp), end = 4.dp, bottom = 4.dp)
                    .width(40.dp)
                    .fillMaxHeight()
            )
        } else ZoomSlider(
            value = zoom,
            onValueChange = { newZoom ->
                zoomJob?.cancel()
                pan = panForZoom(pan, zoom, newZoom, Offset(canvasW / 2f, canvasH / 2f), canvasW, canvasH, overscroll = MAP_OVERSCROLL)
                zoom = newZoom
            },
            onStep = { newZoom -> animateZoomTo(newZoom, Offset(canvasW / 2f, canvasH / 2f), canvasW, canvasH) },
            // Its top stays under the data credit whatever the map's height; the bottom follows the map
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp + (if (embedded) HOME_TILE_BAND else 0.dp), end = 4.dp, bottom = 4.dp)
                .width(40.dp)
                .fillMaxHeight()
        )
    }
}


/** The dot colour for a terrain type; the same colours as the legend. */
private fun pinColor(terrain: TerrainType): Color = when (terrain) {
    TerrainType.COASTAL -> Color(0xFF0284C7)
    TerrainType.RIDGE -> Color(0xFF7C3AED)
    TerrainType.VALLEY_BASIN -> Color(0xFFEA580C)
    TerrainType.URBAN_CANYON -> Color(0xFF475569)
}

/** One station dot; the selected one gets a halo and a ring, and is slightly larger. */
private fun DrawScope.drawStationPin(x: Float, y: Float, color: Color, dotScale: Float, selected: Boolean) {
    if (selected) {
        drawCircle(color = color.copy(alpha = 0.35f), radius = 20f * dotScale, center = Offset(x, y))
        drawCircle(
            color = color,
            radius = 16f * dotScale,
            center = Offset(x, y),
            style = Stroke(width = 3f * dotScale)
        )
    }
    drawCircle(color = Color.White, radius = if (selected) 9f * dotScale else 7f * dotScale, center = Offset(x, y))
    drawCircle(color = color, radius = if (selected) 7f * dotScale else 5.5f * dotScale, center = Offset(x, y))
}

// Maximum number of place labels drawn on the neutral base map
private const val MAP_LABEL_COUNT = 6

/** Plain base map with no terrain artwork: land, ocean, state border, coast and a lat/lon grid. */
private fun DrawScope.drawNeutralBase(
    minLon: Double,
    maxLon: Double,
    minLat: Double,
    maxLat: Double,
    padding: Float,
    mapX: (Double) -> Float,
    mapY: (Double) -> Float,
    zoom: Float,
    textMeasurer: TextMeasurer
) {
    val far = 5000f
    val coast = CaliforniaGeography.COAST
    val border = CaliforniaGeography.LAND_BORDER

    // Land
    drawRect(color = Color(0xFFF1F6EC))

    // Outside California (Nevada, Arizona, Oregon): everything past the land border
    val outside = Path().apply {
        moveTo(mapX(border.first().first), mapY(border.first().second))
        for (i in 1 until border.size) lineTo(mapX(border[i].first), mapY(border[i].second))
        lineTo(mapX(border.last().first), -far)
        lineTo(far, -far)
        lineTo(far, mapY(border.first().second))
        close()
    }
    drawPath(outside, color = Color(0xFFE8E8E1))

    // Pacific Ocean: everything west of the coast
    val ocean = Path().apply {
        moveTo(mapX(coast.first().first), -far)
        for (point in coast) lineTo(mapX(point.first), mapY(point.second))
        lineTo(mapX(coast.last().first), far)
        lineTo(-far, far)
        lineTo(-far, -far)
        close()
    }
    drawPath(ocean, color = Color(0xFFDCEBF4))

    drawGraticule(minLon, maxLon, minLat, maxLat, padding, mapX, mapY, zoom, textMeasurer)

    // Border (dashed) and coast
    val borderPath = Path().apply {
        moveTo(mapX(border.first().first), mapY(border.first().second))
        for (i in 1 until border.size) lineTo(mapX(border[i].first), mapY(border[i].second))
    }
    drawPath(
        borderPath,
        color = Color(0xFF94A3B8),
        style = Stroke(width = 1.8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
    )
    val coastPath = Path().apply {
        moveTo(mapX(coast.first().first), mapY(coast.first().second))
        for (i in 1 until coast.size) lineTo(mapX(coast[i].first), mapY(coast[i].second))
    }
    drawPath(coastPath, color = Color(0xFF7FA7C4), style = Stroke(width = 2.6f, cap = StrokeCap.Round))
}

/** A light latitude / longitude grid with degree labels, drawn over the base. */
private fun DrawScope.drawGraticule(
    minLon: Double,
    maxLon: Double,
    minLat: Double,
    maxLat: Double,
    padding: Float,
    mapX: (Double) -> Float,
    mapY: (Double) -> Float,
    zoom: Float,
    textMeasurer: TextMeasurer
) {
    // Latitude / longitude grid with degree labels
    // Steps chosen so neighboring labels stay at least ~64dp (longitude) and ~44dp (latitude) apart
    fun niceStep(spanDegrees: Double, lengthPx: Float, minGapPx: Float): Double {
        val steps = listOf(0.05, 0.1, 0.25, 0.5, 1.0, 2.0, 5.0)
        return steps.firstOrNull { it / spanDegrees * lengthPx >= minGapPx } ?: steps.last()
    }
    val lonStep = niceStep((maxLon - minLon) / zoom, size.width - padding * 2, 64.dp.toPx())
    val latStep = niceStep((maxLat - minLat) / zoom, size.height - padding * 2, 44.dp.toPx())
    fun degrees(value: Double): String = "%.2f".format(value).trimEnd('0').trimEnd('.')
    val labelStyle = TextStyle(fontSize = ms(8f), color = Color(0xFF64748B))
    var lon = ceil(minLon / lonStep) * lonStep
    while (lon <= maxLon) {
        val x = mapX(lon)
        drawLine(Color.DarkGray.copy(alpha = 0.10f), Offset(x, padding), Offset(x, size.height - padding), 1f)
        // Skip labels under the scale bar (bottom-left)
        if (x > padding + 120.dp.toPx()) {
            val label = textMeasurer.measure("${degrees(abs(lon))}\u00B0W", labelStyle)
            drawText(label, topLeft = Offset(x + 2f, size.height - padding - label.size.height))
        }
        lon += lonStep
    }
    var lat = ceil(minLat / latStep) * latStep
    while (lat <= maxLat) {
        val y = mapY(lat)
        drawLine(Color.DarkGray.copy(alpha = 0.10f), Offset(padding, y), Offset(size.width - padding, y), 1f)
        // Skip labels under the compass (top-left)
        if (y > padding + 34.dp.toPx()) {
            val label = textMeasurer.measure("${degrees(lat)}\u00B0N", labelStyle)
            drawText(label, topLeft = Offset(padding + 3f, y - label.size.height - 1f))
        }
        lat += latStep
    }
}

/**
 * Picks about six stations spread across the map for place labels. Airport stations (K-prefixed)
 * are preferred because they are recognizable cities.
 */
private fun pickMapLandmarks(stations: List<MicroclimateStation>, count: Int = MAP_LABEL_COUNT * 2): List<MicroclimateStation> {
    if (stations.size <= count) return stations
    val midLat = stations.map { it.latitude }.average()
    val lonScale = cos(midLat * PI / 180)
    fun dist(a: MicroclimateStation, b: MicroclimateStation): Double {
        val dx = (a.longitude - b.longitude) * lonScale
        val dy = a.latitude - b.latitude
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    fun bonus(s: MicroclimateStation) = if (s.id.startsWith("K")) 0.4 else 0.0
    val centerLon = stations.map { it.longitude }.average()
    val seed = stations.minByOrNull { (abs(it.latitude - midLat) + abs(it.longitude - centerLon)) - bonus(it) }
        ?: return stations.take(count)
    val chosen = mutableListOf(seed)
    while (chosen.size < count) {
        val next = stations
            .filter { it !in chosen }
            .maxByOrNull { candidate -> chosen.minOf { dist(candidate, it) } + bonus(candidate) }
            ?: break
        chosen.add(next)
    }
    return chosen
}
