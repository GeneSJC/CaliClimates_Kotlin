package io.github.genesjc.microclimatefinder.ui.screens

import io.github.genesjc.microclimatefinder.domain.Elevation
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.BlendMode
import io.github.genesjc.microclimatefinder.data.local.TerrainImage
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.microclimatefinder.ui.map.ElevationViewModel
import androidx.compose.ui.platform.LocalDensity
import io.github.genesjc.microclimatefinder.ui.components.MapExpandToggle
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.microclimatefinder.data.remote.ObservationResult
import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.domain.model.TerrainType
import io.github.genesjc.microclimatefinder.ui.components.TerrainLegend
import io.github.genesjc.microclimatefinder.ui.components.MAX_MAP_ZOOM
import io.github.genesjc.microclimatefinder.ui.components.ZoomSlider
import io.github.genesjc.microclimatefinder.ui.components.MapText
import io.github.genesjc.microclimatefinder.ui.components.HOME_TILE_BAND
import io.github.genesjc.microclimatefinder.ui.components.BRING_INTO_VIEW_MS
import io.github.genesjc.microclimatefinder.ui.components.SliderToggleButton
import io.github.genesjc.microclimatefinder.ui.components.ms
import io.github.genesjc.microclimatefinder.ui.components.animateZoom
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import io.github.genesjc.microclimatefinder.ui.components.clampPan
import io.github.genesjc.microclimatefinder.ui.components.labelBudget
import io.github.genesjc.microclimatefinder.ui.components.rememberLabelFades
import io.github.genesjc.microclimatefinder.ui.components.panForZoom
import io.github.genesjc.microclimatefinder.ui.components.zoomDotGrowth
import io.github.genesjc.microclimatefinder.ui.components.StationCard

private const val LANDMARK_COUNT = 6

// Chart margin in pixels; the Canvas and the label overlay must use the same value
private const val CHART_PADDING_PX = 32f

// The profile sits between a little headroom and sea level; the bottom of the chart is ocean, so sea level
// is in view and the cities spread over more of the height
private const val PROFILE_HEAD = 0.15f
// The slider's height on Home, as a share of the room below its arrow button (Topography uses 0.8)
private const val ELEVATION_SLIDER_FRACTION = 0.64f

private const val SEA_FRACTION = 0.126f

/** Where (0 at the chart's top, 1 at its bottom) a height, as a share of the highest station, is drawn. */
private fun profileFrac(elevRatio: Float): Float = PROFILE_HEAD + (1f - elevRatio) * (1f - PROFILE_HEAD - SEA_FRACTION)



@Composable
fun ElevationProfileScreen(
    dataset: StationDatasetInfo,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** True when shown inside another screen (the Home tabs): no app bar of its own. */
    embedded: Boolean = false,
    /** A station selected elsewhere (the other Home tab), and a way to report selections back. */
    sharedSelectedStationId: String? = null,
    onStationSelected: ((String) -> Unit)? = null,
    /** The zoom to start at, and a report of each change (Home keeps the zoom when it switches views). */
    initialZoom: Float = 1f,
    onZoomChange: ((Float) -> Unit)? = null,
    /** Whether the slider shows, when Home owns it (one setting for both map views), and a report of changes. */
    sliderShownShared: Boolean? = null,
    onSliderShownChange: ((Boolean) -> Unit)? = null,
    /** Home: 0 normally, 1 when the chart has grown out into the page margins (this view); eased between. */
    expandT: Float = 0f,
    viewModel: ElevationViewModel = viewModel(),
) {
    val stations by remember(dataset) { viewModel.stations(dataset) }.collectAsStateWithLifecycle()

    // The region's terrain picture (the one the Topography map draws), used as a faint texture in the ground
    val terrain by remember(dataset.regionId) { viewModel.terrain(dataset.regionId) }.collectAsStateWithLifecycle()

    // The region's real skylines, for the distant ranges
    val skyline by remember(dataset.regionId) { viewModel.skyline(dataset.regionId) }.collectAsStateWithLifecycle()

    // The selected station can be shared with another screen (Home keeps it across its two tabs)
    var localSelectedId by remember(stations) { mutableStateOf(stations.firstOrNull()?.id) }
    val selectedStationId = stations.firstOrNull { it.id == sharedSelectedStationId }?.id ?: localSelectedId

    val selectedStation = stations.find { it.id == selectedStationId } ?: stations.firstOrNull()

    // The chart starts at its regular height; the toggle on it stretches it, slowly, to fill the screen
    var localExpanded by remember { mutableStateOf(false) }
    // On Home the chart always fills the space; the standalone screen keeps the Expand / Shrink toggle
    val chartExpanded = embedded || localExpanded
    var aboveChartPx by remember { mutableStateOf(0) }
    var belowChartPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    // Embedded: just the 5 dp left under the chart, which goes to 0 as it grows out to the screen bottom
    val verticalPadding = if (embedded) 5.dp * (1f - expandT) else 32.dp
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    // Expanded height: the screen less the padding and everything above and below the chart
    val expandedHeight = with(density) {
        ((maxHeight - verticalPadding).roundToPx() - aboveChartPx - belowChartPx).toDp()
    }.coerceAtLeast(CHART_REGULAR_HEIGHT)
    val chartHeight by animateDpAsState(
        targetValue = if (chartExpanded) expandedHeight else CHART_REGULAR_HEIGHT,
        animationSpec = tween(CHART_RESIZE_MS, easing = FastOutSlowInEasing),
        label = "chartHeight"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (embedded) PaddingValues(top = 0.dp, bottom = 5.dp * (1f - expandT)) else PaddingValues(16.dp))
            .verticalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.onSizeChanged { aboveChartPx = it.height }) {
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
                    text = "Elevation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${dataset.title} • ${dataset.stationCount} stations",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "${stations.size} Plotted",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        }
        }

        // Elevation chart, with its Expand / Shrink toggle
        Box(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
            Surface(
                // The bottom corners square off as the chart grows out to the screen edges
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp * (1f - expandT), bottomEnd = 16.dp * (1f - expandT)),
                // An opaque base (the old tint, laid over the page colour) so the backdrop's relief does not show through the chart
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f).compositeOver(MaterialTheme.colorScheme.background),
                // A 1dp outline, for a crisper edge
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxSize()
            ) {
                ElevationProfileCanvas(
                    stations = stations,
                    regionId = dataset.regionId,
                    terrain = terrain,
                    skyline = skyline,
                    cityName = viewModel::cityName,
                    knownReading = viewModel::knownReading,
                    fetchReading = viewModel::latestReading,
                    startVisit = viewModel::startVisit,
                    onLineTapped = viewModel::lineTapped,
                    embedded = embedded,
                    expandT = expandT,
                    selectedStationId = selectedStationId,
                    initialZoom = initialZoom,
                    onZoomChange = onZoomChange,
                    sliderShownShared = sliderShownShared,
                    onSliderShownChange = onSliderShownChange,
                    onSelectStation = {
                        localSelectedId = it.id
                        onStationSelected?.invoke(it.id)
                    }
                )
            }
            if (!embedded) {
            MapExpandToggle(
                expanded = chartExpanded,
                onToggle = {
                    localExpanded = !chartExpanded
                },
                // Just left of the top of the zoom slider, as on the Topography map
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 50.dp, top = 56.dp)
            )
            }
        }

        Column(modifier = Modifier.onSizeChanged { belowChartPx = it.height }) {
        if (!embedded) Spacer(modifier = Modifier.height(12.dp))

        // Terrain legend: one line until opened, then this region's terrain types with counts.
        // Embedded, Home shows it beside the Region Info link.
        if (!embedded) TerrainLegend(stations = stations)

        // The selected station card; embedded, Home shows it above the tabs
        if (!embedded) {
        Spacer(modifier = Modifier.height(16.dp))

        // Selected station card (the Station Inspector)

        if (selectedStation != null) {
            val reading by remember(selectedStation.id) { viewModel.reading(selectedStation) }.collectAsStateWithLifecycle()
            StationCard(
                station = selectedStation,
                isSelected = true,
                onSelect = { /* Already selected */ },
                compact = true,
                reading = reading
            )
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Tap any station along the slope to inspect microclimate data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        }
        }
    }
    }
}

/** The chart's regular height, before it is expanded to fill the screen. */
private val CHART_REGULAR_HEIGHT = 300.dp

/** How long the chart takes to grow to full height and back. */
private const val CHART_RESIZE_MS = 700


@Composable
private fun ElevationProfileCanvas(
    stations: List<MicroclimateStation>,
    regionId: String,
    terrain: TerrainImage?,
    skyline: List<FloatArray>?,
    cityName: (MicroclimateStation) -> String,
    knownReading: (String) -> ObservationResult?,
    fetchReading: suspend (MicroclimateStation) -> ObservationResult,
    /** Counts a visit to this view; true when the hint in words should show. */
    startVisit: () -> Boolean,
    onLineTapped: () -> Unit,
    embedded: Boolean,
    expandT: Float = 0f,
    selectedStationId: String?,
    initialZoom: Float = 1f,
    onZoomChange: ((Float) -> Unit)? = null,
    sliderShownShared: Boolean? = null,
    onSliderShownChange: ((Boolean) -> Unit)? = null,
    onSelectStation: (MicroclimateStation) -> Unit
) {
    if (stations.isEmpty()) return

    // Sort stations by elevation ascending (valley 5m -> ridge 1406m)
    val sorted = remember(stations) { stations.sortedBy { it.elevationMeters } }
    val maxElevation = remember(sorted) { (sorted.maxOfOrNull { it.elevationMeters } ?: 1500).toFloat() }
    // Stations ranked for labels; the first few are the ones the sky badge cycles through
    val rankedLandmarks = remember(sorted) { rankLandmarkIndexes(sorted) }
    // Each station's label text, made once (label placement runs on every zoom and pan frame)
    val labelTexts = remember(sorted) {
        sorted.map { cityName(it) + " ${Elevation.formatFeet(it.elevationMeters)}" }
    }
    val ridgeCache = remember { RidgeHeights() }
    val cycleIds = remember(sorted) {
        rankedLandmarks.take(LANDMARK_COUNT).sorted().map { sorted[it].id }
    }

    // On Home the slider can be tucked away with the small arrow button under the menu button (pinch still zooms)
    var localSliderShown by remember { mutableStateOf(true) }
    val sliderShown = sliderShownShared ?: localSliderShown
    val setSliderShown: (Boolean) -> Unit = { localSliderShown = it; onSliderShownChange?.invoke(it) }
    val sliderCollapsible = embedded
    // The slider starts lower on Home, to leave room for that button
    val sliderTop = 56.dp + (if (embedded) HOME_TILE_BAND + 32.dp else 0.dp)

    // View transform: an even zoom (like a magnifying glass) about the centre plus a pan in pixels
    var zoom by remember(sorted) { mutableStateOf(initialZoom.coerceIn(1f, MAX_MAP_ZOOM)) }
    androidx.compose.runtime.LaunchedEffect(zoom) { onZoomChange?.invoke(zoom) }

    // Every visit: a pulse runs up the line once. First launches only: a hint in words says the line can be tapped
    var showHint by remember { mutableStateOf(false) }
    val pulse = remember { androidx.compose.animation.core.Animatable(0f) }
    // The hint eases in to a translucent look, then fades slowly from the start; a tap clears it quickly
    val hintAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(showHint) {
        if (showHint) {
            hintAlpha.animateTo(0.75f, tween(600))
            hintAlpha.animateTo(0f, tween(5500, easing = androidx.compose.animation.core.LinearEasing))
        } else {
            hintAlpha.animateTo(0f, tween(300))
        }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        // Counts this visit; the words show only while the hint is still wanted
        // The ripple up the line plays on every visit to this view (it is the cue that the line can be tapped); the
        // words of the hint show only on the first few launches, and stop at the first tap
        val showWords = startVisit()
        if (showWords) showHint = true
        kotlinx.coroutines.delay(700)
        pulse.snapTo(0f)
        pulse.animateTo(1f, tween(2000, easing = androidx.compose.animation.core.LinearEasing))
        if (showWords) {
            // The hint fades the whole time (see hintAlpha), so it is gone as the timeline ends
            kotlinx.coroutines.delay(3400)
            showHint = false
        }
    }
    var pan by remember(sorted) { mutableStateOf(Offset.Zero) }
    // Button, rail-tap and double-tap zooms animate (so labels fade smoothly); pinch and drag stay direct
    val zoomScope = rememberCoroutineScope()
    var zoomJob by remember { mutableStateOf<Job?>(null) }
    fun animateZoomTo(newZoom: Float, anchor: Offset, w: Float, h: Float) {
        zoomJob?.cancel()
        zoomJob = zoomScope.launch {
            animateZoom(zoom, pan, newZoom, anchor, w, h, CHART_PADDING_PX, CHART_PADDING_PX) { z, p -> zoom = z; pan = p }
        }
    }

    // Nothing selected: start on the lowest station that has a live reading, so the first view is not blank
    val currentStation = sorted.find { it.id == selectedStationId }
        ?: sorted.firstOrNull { knownReading(it.id) is ObservationResult.Reading }
        ?: sorted.firstOrNull()
    // Real NWS temperature for the selected station; null while loading or when it has no reading
    var live by remember(currentStation?.id) {
        mutableStateOf(currentStation?.let { knownReading(it.id) })
    }
    LaunchedEffect(currentStation?.id) {
        currentStation?.let {
            live = fetchReading(it)
        }
    }
    val tempF: Double? = (live as? ObservationResult.Reading)?.observation?.temperatureFahrenheit

    val skyTheme = remember(tempF) {
        when {
            tempF == null -> SkyAtmosphereTheme(
                colors = listOf(
                    Color(0xFF475569).copy(alpha = 0.40f),
                    Color(0xFF94A3B8).copy(alpha = 0.20f),
                    Color(0xFFE2E8F0).copy(alpha = 0.05f)
                ),
                condition = "No current reading",
                icon = "⛰️",
                accent = Color(0xFFCBD5E1),
                badgeBg = Color(0xFF1E293B)
            )
            tempF >= 84.0 -> SkyAtmosphereTheme(
                colors = listOf(
                    Color(0xFFC2410C).copy(alpha = 0.45f),
                    Color(0xFFF59E0B).copy(alpha = 0.22f),
                    Color(0xFFFEF08A).copy(alpha = 0.05f)
                ),
                condition = "Valley Solar Heat",
                icon = "☀️",
                accent = Color(0xFFF59E0B),
                badgeBg = Color(0xFF451A03)
            )
            tempF >= 74.0 -> SkyAtmosphereTheme(
                colors = listOf(
                    Color(0xFF0284C7).copy(alpha = 0.45f),
                    Color(0xFF38BDF8).copy(alpha = 0.22f),
                    Color(0xFFFEF3C7).copy(alpha = 0.05f)
                ),
                condition = "Fair Foothills",
                icon = "🌤️",
                accent = Color(0xFF38BDF8),
                badgeBg = Color(0xFF082F49)
            )
            tempF >= 65.0 -> SkyAtmosphereTheme(
                colors = listOf(
                    Color(0xFF0369A1).copy(alpha = 0.45f),
                    Color(0xFF06B6D4).copy(alpha = 0.22f),
                    Color(0xFF94A3B8).copy(alpha = 0.05f)
                ),
                condition = "Temperate Ridge",
                icon = "⛅",
                accent = Color(0xFF06B6D4),
                badgeBg = Color(0xFF164E63)
            )
            else -> SkyAtmosphereTheme(
                colors = listOf(
                    Color(0xFF312E81).copy(alpha = 0.50f),
                    Color(0xFF6366F1).copy(alpha = 0.25f),
                    Color(0xFF38BDF8).copy(alpha = 0.06f)
                ),
                condition = "Crisp Sub-Alpine",
                icon = "🏔️",
                accent = Color(0xFFA78BFA),
                badgeBg = Color(0xFF1E1B4B)
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            // Keyed to the data: zoom and pan are remembered per region, so when the region changes they are new state objects
            // and the handlers must restart to use them (with a fixed key they kept writing to the old ones and touch stopped working)
            .pointerInput(sorted) {
                // Pinch to zoom in on the profile, drag to pan
                detectTransformGestures { centroid, panChange, zoomChange, _ ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val newZoom = (zoom * zoomChange).coerceIn(1f, MAX_MAP_ZOOM)
                    val anchored = panForZoom(pan, zoom, newZoom, centroid, w, h, CHART_PADDING_PX, CHART_PADDING_PX)
                    pan = clampPan(anchored + panChange, newZoom, w, h, CHART_PADDING_PX, CHART_PADDING_PX)
                    zoom = newZoom
                }
            }
            .pointerInput(sorted) {
                // Double-tap: zoom in on the spot, and back out once fully zoomed
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

        val paddingPx = CHART_PADDING_PX
        val canvasW = with(density) { widthDp.toPx() }
        val canvasH = with(density) { heightDp.toPx() }
        val widthPx = canvasW - (paddingPx * 2)
        val heightPx = canvasH - (paddingPx * 2)
        fun viewX(baseX: Float) = canvasW / 2f + (baseX - canvasW / 2f) * zoom + pan.x
        fun viewY(baseY: Float) = canvasH / 2f + (baseY - canvasH / 2f) * zoom + pan.y
        val dotGrowth = zoomDotGrowth(zoom)

        // When the selected station changes (from the Stations list, the tile or the other view) and it is not in
        // view at this zoom and pan, ease the chart over so it is
        androidx.compose.runtime.LaunchedEffect(selectedStationId, sorted) {
            val idx = sorted.indexOfFirst { it.id == selectedStationId }
            if (idx < 0) return@LaunchedEffect
            val bx = paddingPx + idx.toFloat() / (sorted.size - 1).coerceAtLeast(1) * widthPx
            val by = paddingPx + profileFrac(sorted[idx].elevationMeters / maxElevation) * heightPx
            val vx = viewX(bx)
            val vy = viewY(by)
            val sideMargin = with(density) { 48.dp.toPx() }
            val topMargin = with(density) { (if (embedded) HOME_TILE_BAND + 24.dp else 40.dp).toPx() }
            val bottomMargin = with(density) { 40.dp.toPx() }
            val inView = vx in sideMargin..(canvasW - sideMargin) && vy in topMargin..(canvasH - bottomMargin)
            if (inView) return@LaunchedEffect
            zoomJob?.cancel()
            val from = pan
            val target = clampPan(
                Offset(-(bx - canvasW / 2f) * zoom, -(by - canvasH / 2f) * zoom),
                zoom, canvasW, canvasH, CHART_PADDING_PX, CHART_PADDING_PX
            )
            androidx.compose.animation.core.animate(0f, 1f, animationSpec = tween(BRING_INTO_VIEW_MS, easing = FastOutSlowInEasing)) { v, _ ->
                pan = Offset(from.x + (target.x - from.x) * v, from.y + (target.y - from.y) * v)
            }
        }

        // Which labels have room at this zoom: more as the profile spreads out. Only the target (room
        // or no room) is decided here; each label's opacity eases toward it on its own.
        val placedLabels = run {
            val budget = labelBudget(LANDMARK_COUNT, zoom, 5f).toInt()
            val taken = mutableListOf(
                if (embedded) listOf(0f, 0f, widthDp.value, HOME_TILE_BAND.value) else listOf(0f, 0f, 0f, 0f),  // the floating station tile
                if (embedded) listOf(0f, HOME_TILE_BAND.value, 140f, HOME_TILE_BAND.value + 50f) else listOf(0f, 0f, 0f, 0f),  // Home's control row
                if (embedded) listOf(0f, HOME_TILE_BAND.value + 50f, 46f, HOME_TILE_BAND.value + 82f) else listOf(0f, 0f, 0f, 0f),  // the slider's arrow button
                if (embedded && !sliderShown) listOf(0f, 0f, 0f, 0f) else listOf(0f, sliderTop.value, 46f, sliderTop.value + (heightDp.value - sliderTop.value - 4f) * (if (embedded) ELEVATION_SLIDER_FRACTION else 1f)), // zoom slider (at the left)
                if (embedded) listOf(widthDp.value - 170f, HOME_TILE_BAND.value, widthDp.value, HOME_TILE_BAND.value + 56f) else listOf(0f, 0f, 0f, 0f),  // Home's view buttons
                listOf(widthDp.value - 126f, 52f, widthDp.value - 46f, 84f),  // expand toggle
                listOf(52f, 50f + (if (embedded) HOME_TILE_BAND.value else 0f), 222f, 97f + (if (embedded) HOME_TILE_BAND.value else 0f))  // weather badge
            )
            // Null when the station is out of view; otherwise its label spot, marked with or without room
            fun plan(idx: Int, allow: Boolean, force: Boolean): PlacedLabel? {
                val station = sorted[idx]
                val ratioX = idx.toFloat() / (sorted.size - 1).coerceAtLeast(1)
                val elevRatio = station.elevationMeters / maxElevation
                val pinX = with(density) { viewX(paddingPx + ratioX * widthPx).toDp().value }
                val pinY = with(density) { viewY(paddingPx + profileFrac(elevRatio) * heightPx).toDp().value }
                if (pinX < 0f || pinX > widthDp.value || pinY < 0f || pinY > heightDp.value) return null
                val text = labelTexts[idx]
                // Estimated label width at 9sp; labels that would pass the right edge end at the pin instead
                val labelW = text.length * 5.4f + 10f
                val left = (if (pinX - 24f + labelW > widthDp.value * 0.97f) pinX - labelW else pinX - 24f).coerceAtLeast(2f)
                // High pins stay on the lowest height so the label stays inside the chart
                val maxLevel = if (elevRatio > 0.8f) 0 else 2
                if (allow) {
                    for (level in 0..maxLevel) {
                        val top = pinY - 26f - 16f * level
                        if (top < 0f) continue
                        val right = left + labelW
                        val bottom = top + LABEL_HEIGHT_DP
                        val free = taken.none { r -> left < r[2] && right > r[0] && top < r[3] && bottom > r[1] }
                        if (free || force) {
                            taken.add(listOf(left, top, right, bottom))
                            return PlacedLabel(station, text, left, top, hasRoom = true)
                        }
                    }
                }
                return PlacedLabel(station, text, left, pinY - 26f, hasRoom = false)
            }
            val plans = mutableListOf<PlacedLabel>()
            var shown = 0
            for (idx in rankedLandmarks) {
                val p = plan(idx, allow = shown < budget, force = false) ?: continue
                if (p.hasRoom) shown++
                plans.add(p)
            }
            // The selected station always keeps its label
            val selectedIdx = sorted.indexOfFirst { it.id == selectedStationId }
            if (selectedIdx >= 0) {
                val existing = plans.indexOfFirst { it.station.id == selectedStationId }
                if (existing < 0 || !plans[existing].hasRoom) {
                    plan(selectedIdx, allow = true, force = true)?.let {
                        if (existing >= 0) plans[existing] = it else plans.add(it)
                    }
                }
            }
            plans
        }
        val placedIds = placedLabels.filter { it.hasRoom }.map { it.station.id }.toSet()
        val labelFades = rememberLabelFades(placedLabels.associate { it.station.id to if (it.hasRoom) 1f else 0f })

        // Canvas drawing the dynamic sky, slope, shaded terrain, leader lines, and nodes
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(sorted, canvasW) {
                    detectTapGestures { tapOffset ->
                        val padding = CHART_PADDING_PX
                        val w = size.width - (padding * 2)
                        val h = size.height - (padding * 2)

                        var closestStation: MicroclimateStation? = null
                        var closestDist = Float.MAX_VALUE

                        sorted.forEachIndexed { index, station ->
                            val x = viewX(padding + (index.toFloat() / (sorted.size - 1).coerceAtLeast(1)) * w)
                            val elevRatio = station.elevationMeters / maxElevation
                            val y = viewY(padding + profileFrac(elevRatio) * h)

                            val dx = tapOffset.x - x
                            val dy = tapOffset.y - y
                            val dist = (dx * dx + dy * dy)
                            if (dist < closestDist && dist < 1800f) {
                                closestDist = dist
                                closestStation = station
                            }
                        }
                        closestStation?.let {
                            onSelectStation(it)
                            onLineTapped()
                            showHint = false
                        }
                    }
                }
        ) {
            val padding = CHART_PADDING_PX
            val w = size.width - (padding * 2)
            val h = size.height - (padding * 2)

            // 1. DYNAMIC SKY GRADIENT FILL: fills the open space above & to the left of the slope
            val skyPath = Path().apply {
                moveTo(viewX(padding), viewY(padding))
                lineTo(viewX(size.width - padding), viewY(padding))
                val lastElevRatio = (sorted.lastOrNull()?.elevationMeters ?: 0) / maxElevation
                lineTo(viewX(size.width - padding), viewY(padding + profileFrac(lastElevRatio) * h))
                for (i in (sorted.size - 1) downTo 0) {
                    val st = sorted[i]
                    val x = viewX(padding + (i.toFloat() / (sorted.size - 1).coerceAtLeast(1)) * w)
                    val elevRatio = st.elevationMeters / maxElevation
                    val y = viewY(padding + profileFrac(elevRatio) * h)
                    lineTo(x, y)
                }
                close()
            }

            drawPath(
                path = skyPath,
                brush = Brush.verticalGradient(
                    colors = skyTheme.colors,
                    startY = viewY(padding),
                    endY = viewY(padding + h * 0.85f)
                )
            )

            // 1b. Distant ranges, hills and trees behind the profile, for depth. Decoration: the three
            // farthest ranges take their outline from the region's real skyline (the highest ground,
            // looking north across the region); the nearer hills and trees are made up, the same every
            // time for a region. Each layer drifts slower than the profile when you pan and zoom, and
            // hazes toward the sky colour the farther away it is.
            val horizon = skyTheme.colors.last()
            val seed = regionId.hashCode()
            // The layers keep the size they have in the regular-height chart. When the chart is taller,
            // the extra height is shared out between them (the far ranges stay near the top, the near
            // hills sink toward the ground), so nothing stretches and the gaps fill with more layers.
            val ridgeHeights = ridgeCache.get(size.width, seed, skyline)
            val ridgeH = CHART_REGULAR_HEIGHT.toPx() - padding * 2
            val extra = (h - ridgeH).coerceAtLeast(0f)
            clipPath(skyPath) {
                RIDGE_LAYERS.forEachIndexed { layer, spec ->
                    val steps = RIDGE_STEPS
                    val startX = -size.width
                    val endX = size.width * 2f
                    val points = ArrayList<Offset>(steps + 1)
                    val layerHeights = ridgeHeights[layer]
                    for (i in 0..steps) {
                        val bx = startX + (endX - startX) * i / steps
                        val height = layerHeights[i]
                        val by = padding + ridgeH * spec.base - ridgeH * spec.amplitude * height + spec.anchor * extra
                        val lx = size.width / 2f + (bx - size.width / 2f) * (1f + (zoom - 1f) * spec.follow) + pan.x * spec.follow
                        val ly = size.height / 2f + (by - size.height / 2f) * (1f + (zoom - 1f) * spec.follow) + pan.y * spec.follow
                        points.add(Offset(lx, ly))
                    }
                    val ridge = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (k in 1 until points.size) lineTo(points[k].x, points[k].y)
                        lineTo(size.width * 4f, size.height * 3f)
                        lineTo(-size.width * 3f, size.height * 3f)
                        close()
                    }
                    // Darker at the ridge, hazing lighter below it, so the gap to the next layer reads as mist
                    val top = lerp(horizon, Color(0xFF334155), spec.depthShade)
                    val topY = points.minOf { it.y }
                    drawPath(
                        ridge,
                        brush = Brush.verticalGradient(
                            colors = listOf(top.copy(alpha = spec.alpha), lerp(top, horizon, 0.5f).copy(alpha = spec.alpha * 0.7f)),
                            startY = topY,
                            endY = topY + ridgeH * 0.45f * (1f + (zoom - 1f) * spec.follow)
                        )
                    )
                    if (spec.treeSizeDp > 0f) {
                        val treeScale = 1f + (zoom - 1f) * spec.follow
                        drawTrees(
                            points = points,
                            regionId = regionId,
                            sizePx = spec.treeSizeDp.dp.toPx() * treeScale,
                            color = lerp(top, Color(0xFF0F2F2A), 0.55f).copy(alpha = (spec.alpha + 0.15f).coerceAtMost(0.95f)),
                            seed = seed + layer * 101,
                            xMin = -40f,
                            xMax = size.width + 40f
                        )
                    }
                }
            }

            // 2. DYNAMIC WEATHER VISUALS (Clouds / Sun corona in the upper-left quadrant)
            if (tempF == null) {
                // No live reading: plain sky, no weather visuals
            } else if (tempF >= 84.0) {
                // Valley solar heat corona
                drawCircle(
                    color = Color(0xFFFDE047).copy(alpha = 0.25f),
                    radius = 34f,
                    center = Offset(padding + 48f, padding + 48f)
                )
                drawCircle(
                    color = Color(0xFFFEF08A).copy(alpha = 0.85f),
                    radius = 16f,
                    center = Offset(padding + 48f, padding + 48f)
                )
            } else if (tempF >= 74.0) {
                // Foothill cumulus cloud puffs
                drawCircle(color = Color.White.copy(alpha = 0.55f), radius = 18f, center = Offset(padding + 45f, padding + 48f))
                drawCircle(color = Color.White.copy(alpha = 0.65f), radius = 24f, center = Offset(padding + 65f, padding + 44f))
                drawCircle(color = Color.White.copy(alpha = 0.50f), radius = 16f, center = Offset(padding + 84f, padding + 48f))
            } else if (tempF >= 65.0) {
                // Stratified mountain ridge streaks
                drawLine(
                    color = Color(0xFFBAE6FD).copy(alpha = 0.5f),
                    start = Offset(padding + 28f, padding + 42f),
                    end = Offset(padding + 110f, padding + 42f),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFE0F2FE).copy(alpha = 0.4f),
                    start = Offset(padding + 48f, padding + 54f),
                    end = Offset(padding + 130f, padding + 54f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            } else {
                // Alpine cirrus streaks
                drawLine(
                    color = Color(0xFFC7D2FE).copy(alpha = 0.6f),
                    start = Offset(padding + 25f, padding + 36f),
                    end = Offset(padding + 95f, padding + 36f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFA5B4FC).copy(alpha = 0.5f),
                    start = Offset(padding + 45f, padding + 48f),
                    end = Offset(padding + 115f, padding + 48f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
            }

            // 3. Draw elevation terrain fill
            val terrainPath = Path()
            val linePath = Path()

            sorted.forEachIndexed { index, station ->
                val x = viewX(padding + (index.toFloat() / (sorted.size - 1).coerceAtLeast(1)) * w)
                val elevRatio = station.elevationMeters / maxElevation
                val y = viewY(padding + profileFrac(elevRatio) * h)

                if (index == 0) {
                    terrainPath.moveTo(x, viewY(size.height - padding))
                    terrainPath.lineTo(x, y)
                    linePath.moveTo(x, y)
                } else {
                    terrainPath.lineTo(x, y)
                    linePath.lineTo(x, y)
                }
            }
            terrainPath.lineTo(viewX(size.width - padding), viewY(size.height - padding))
            terrainPath.close()

            // The ground: tinted by height with the same ramp as the Topography map (data), so a given
            // height has the same colour in both views
            val groundTop = viewY(padding)
            val groundBottom = viewY(padding + h)
            drawPath(
                path = terrainPath,
                brush = Brush.verticalGradient(
                    colorStops = (0..10).map { step ->
                        val fraction = step / 10f
                        // The height drawn at this share of the chart (clamped: above the top and below sea level)
                        val ratio = (1f - (fraction - PROFILE_HEAD) / (1f - PROFILE_HEAD - SEA_FRACTION)).coerceIn(0f, 1f)
                        fraction to heightTint(ratio * maxElevation)
                    }.toTypedArray(),
                    startY = groundTop,
                    endY = groundBottom
                )
            )

            // A faint relief texture from the region's own terrain picture (decoration, not data). It
            // drifts slower than the profile when you pan and zoom, which gives the ground some depth.
            if (terrain != null) {
                val b = terrain.bounds
                val lonSpan = b.maxLon - b.minLon
                val latSpan = b.maxLat - b.minLat
                // The part of the picture that holds the stations
                val sLeft = ((stations.minOf { it.longitude } - b.minLon) / lonSpan).coerceIn(0.0, 1.0)
                val sRight = ((stations.maxOf { it.longitude } - b.minLon) / lonSpan).coerceIn(0.0, 1.0)
                val sTop = ((b.maxLat - stations.maxOf { it.latitude }) / latSpan).coerceIn(0.0, 1.0)
                val sBottom = ((b.maxLat - stations.minOf { it.latitude }) / latSpan).coerceIn(0.0, 1.0)
                val img = terrain.image
                val srcX = (sLeft * img.width).toInt()
                val srcY = (sTop * img.height).toInt()
                val srcW = ((sRight - sLeft) * img.width).toInt().coerceAtLeast(1)
                val srcH = ((sBottom - sTop) * img.height).toInt().coerceAtLeast(1)
                val follow = 0.6f
                val scale = 1f + (zoom - 1f) * follow
                // Big enough to cover the whole chart, whatever its shape
                val cover = maxOf(size.width * 1.6f / srcW, size.height * 1.4f / srcH)
                val dstW = srcW * cover * scale
                val dstH = srcH * cover * scale
                val dstX = size.width / 2f - dstW / 2f + pan.x * follow
                val dstY = size.height / 2f - dstH / 2f + pan.y * follow
                clipPath(terrainPath) {
                    drawImage(
                        image = img,
                        srcOffset = IntOffset(srcX, srcY),
                        srcSize = IntSize(srcW, srcH),
                        dstOffset = IntOffset(dstX.roundToInt(), dstY.roundToInt()),
                        dstSize = IntSize(dstW.roundToInt(), dstH.roundToInt()),
                        alpha = 0.5f,
                        colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                        blendMode = BlendMode.Multiply
                    )
                }
            }

            // The ocean: the bottom of the chart, below sea level (decoration; the heights above it are data)
            val seaY = viewY(padding + (1f - SEA_FRACTION) * h)
            val oceanBottom = maxOf(size.height, viewY(size.height - padding) + padding)
            if (seaY < oceanBottom) {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF7CC3E6), Color(0xFF3B8EC4), Color(0xFF1F5F99)),
                        startY = seaY,
                        endY = oceanBottom
                    ),
                    topLeft = Offset(0f, seaY),
                    size = androidx.compose.ui.geometry.Size(size.width, oceanBottom - seaY)
                )
                // A light line at the waterline and a few soft wave crests
                drawLine(Color.White.copy(alpha = 0.55f), Offset(0f, seaY), Offset(size.width, seaY), strokeWidth = 2.5f)
                val crest = Color.White.copy(alpha = 0.22f)
                for (row in 1..3) {
                    val wy = seaY + (oceanBottom - seaY) * row / 4.5f
                    var wx = -(row * 37f) % 90f
                    while (wx < size.width) {
                        drawLine(crest, Offset(wx, wy), Offset(wx + 26f, wy), strokeWidth = 2f, cap = StrokeCap.Round)
                        wx += 90f
                    }
                }
            }

            // A lit edge along the profile, like the sun catching the ridge
            drawPath(
                path = linePath,
                color = Color.White.copy(alpha = 0.30f),
                style = Stroke(width = 10f, cap = StrokeCap.Round)
            )

            // Top ridge line
            drawPath(
                path = linePath,
                color = Color(0xFF7C3AED),
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )

            // Station points along the slope & leader lines for landmarks
            sorted.forEachIndexed { index, station ->
                val x = viewX(padding + (index.toFloat() / (sorted.size - 1).coerceAtLeast(1)) * w)
                val elevRatio = station.elevationMeters / maxElevation
                val y = viewY(padding + profileFrac(elevRatio) * h)
                val isSelected = station.id == selectedStationId

                val isLandmark = station.id in placedIds

                val pinColor = when (station.terrain) {
                    TerrainType.RIDGE -> Color(0xFF7C3AED)
                    TerrainType.COASTAL -> Color(0xFF0284C7)
                    TerrainType.VALLEY_BASIN -> Color(0xFFEA580C)
                    TerrainType.URBAN_CANYON -> Color(0xFF475569)
                }

                // Vertical leader line for landmark cities
                if (isLandmark || isSelected) {
                    drawLine(
                        color = (if (isSelected) pinColor else Color.Gray).copy(alpha = 0.5f),
                        start = Offset(x, y - 4f),
                        end = Offset(x, y - 18f),
                        strokeWidth = if (isSelected) 2f else 1.2f
                    )
                }

                if (isSelected) {
                    drawCircle(
                        color = skyTheme.accent.copy(alpha = 0.35f),
                        radius = 18f * dotGrowth,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = skyTheme.accent,
                        radius = 14f * dotGrowth,
                        center = Offset(x, y),
                        style = Stroke(width = 2.5f)
                    )
                }

                drawCircle(
                    color = Color.White,
                    radius = (if (isSelected) 8f else (if (isLandmark) 6.5f else 4.5f)) * dotGrowth,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = if (isSelected) skyTheme.accent else pinColor,
                    radius = (if (isSelected) 6f else (if (isLandmark) 4.8f else 3.2f)) * dotGrowth,
                    center = Offset(x, y)
                )

                // The one-off pulse: a ring that runs up the line, low dots first
                val pulseValue = pulse.value
                if (pulseValue > 0f && pulseValue < 1f) {
                    val ring = (pulseValue * (sorted.size + 8f) - index) / 8f
                    if (ring in 0f..1f) {
                        drawCircle(
                            color = Color.White.copy(alpha = (1f - ring) * 0.85f),
                            radius = (9f + 26f * ring) * dotGrowth,
                            center = Offset(x, y),
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }
        }

        // Three fish swimming in the ocean (decoration). They are drawn in their own layer, so only it
        // redraws as they move, and they follow the ocean when the chart is zoomed or panned.
        // The swim is stepped about 30 times a second (not every frame), stands still while the chart is being
        // panned or zoomed and for a moment after, and stays still when the system's animations are turned off.
        var fishTime by remember { mutableStateOf(0f) }
        var chartMoving by remember { mutableStateOf(true) }
        val fishContext = androidx.compose.ui.platform.LocalContext.current
        val animationsOn = remember {
            android.provider.Settings.Global.getFloat(
                fishContext.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            ) > 0f
        }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            // Each change of zoom or pan marks the chart as moving until it has been still for a little while
            androidx.compose.runtime.snapshotFlow { zoom to pan }.collectLatest {
                chartMoving = true
                kotlinx.coroutines.delay(600)
                chartMoving = false
            }
        }
        androidx.compose.runtime.LaunchedEffect(animationsOn) {
            if (!animationsOn) return@LaunchedEffect
            androidx.compose.runtime.snapshotFlow { chartMoving }.collectLatest { moving ->
                if (!moving) {
                    var last = System.nanoTime()
                    while (true) {
                        kotlinx.coroutines.delay(FISH_STEP_MS)
                        val now = System.nanoTime()
                        fishTime = (fishTime + (now - last) / 1e9f / FISH_LOOP_SECONDS) % 1f
                        last = now
                    }
                }
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val seaTop = viewY(paddingPx + (1f - SEA_FRACTION) * heightPx)
            val seaBottom = maxOf(size.height, viewY(size.height - paddingPx) + paddingPx)
            val band = seaBottom - seaTop
            if (seaTop < size.height && band > 40.dp.toPx()) {
                clipRect(top = maxOf(0f, seaTop), bottom = size.height) {
                    for (fish in FISH) {
                        val length = fish.lengthDp.dp.toPx()
                        val travel = size.width + 2f * length
                        val phase = (fishTime * fish.cycles + fish.offset) % 1f
                        val cx = if (fish.rightward) phase * travel - length else size.width + length - phase * travel
                        val wobble = kotlin.math.sin((fishTime * 60f * fish.cycles + fish.offset) * 2f * Math.PI.toFloat())
                        val cy = seaTop + band * fish.depth + wobble * 4.dp.toPx()
                        drawFish(cx, cy, length, fish.color, fish.rightward, wobble)
                    }
                }
            }
        }

        // The hint, in the ocean: fades away after a few seconds or at the first tap on the line
        if (hintAlpha.value > 0.01f) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xCC0F172A),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = heightDp * 0.045f + 6.dp)
                    .graphicsLayer { alpha = hintAlpha.value }
            ) {
                Text(
                    text = "Tap anywhere on the line to pick a station",
                    color = Color.White,
                    // A third smaller than before; fixed, not tied to the font slider
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }

        // 4. Atmospheric Sky Micro-Display in the blank space to the left of the graph, below the expand toggle
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = skyTheme.badgeBg.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, skyTheme.accent.copy(alpha = 0.45f)),
            modifier = Modifier
                .offset(x = 52.dp + 5.dp * expandT, y = 50.dp + (if (embedded) HOME_TILE_BAND else 0.dp))
                .clickable {
                    // Tap to cycle landmarks
                    if (cycleIds.isNotEmpty()) {
                        val currentIndex = cycleIds.indexOfFirst { it == currentStation?.id }
                        val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % cycleIds.size else 0
                        val nextId = cycleIds[nextIndex]
                        sorted.find { it.id == nextId }?.let { onSelectStation(it) }
                    }
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = skyTheme.icon, fontSize = ms(14f))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tempF?.let { "${it.toInt()}°F" } ?: "—°F",
                            fontSize = ms(11f),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentStation?.name?.let { if (it.length > 26) it.take(25) + "\u2026" else it } ?: "Sacramento",
                            fontSize = ms(9f),
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1
                        )
                    }
                    Text(
                        text = "${skyTheme.condition} • ${currentStation?.let { Elevation.formatFeet(it.elevationMeters) }} elev",
                        fontSize = ms(8f),
                        color = skyTheme.accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Overlay Interactive City Landmark Labels along the curve
        placedLabels.forEach { label ->
            val station = label.station
            val isSelected = station.id == selectedStationId
            val fade = labelFades[station.id] ?: 0f
            if (fade <= 0.01f) return@forEach

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .offset(x = label.xDp.dp, y = label.yDp.dp)
                    .alpha(fade)
                    .clickable { onSelectStation(station) }
            ) {
                Text(
                    text = label.text,
                    fontSize = ms(9f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        // Floating zoom slider on the right edge, where the Topography map has it
        fun nearestLinePoint(): Offset {
            val middle = Offset(canvasW / 2f, canvasH / 2f)
            return sorted.indices
                .map { i ->
                    val elevRatio = sorted[i].elevationMeters / maxElevation
                    Offset(
                        viewX(paddingPx + i.toFloat() / (sorted.size - 1).coerceAtLeast(1) * widthPx),
                        viewY(paddingPx + profileFrac(elevRatio) * heightPx)
                    )
                }
                .minByOrNull { (it - middle).getDistance() } ?: middle
        }
        // The arrow button under the menu button: up while the slider shows (tap to tuck it away), down when hidden
        if (sliderCollapsible) {
            SliderToggleButton(
                shown = sliderShown,
                onToggle = { setSliderShown(!sliderShown) },
                modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp + 5.dp * expandT, top = 50.dp + HOME_TILE_BAND)
            )
        }
        // For now the slider sets the type size instead of the zoom (see MapText)
        if (sliderCollapsible && !sliderShown) {
            // hidden: pinch and double-tap still zoom
        } else if (MapText.SLIDER_SETS_FONT) {
            ZoomSlider(
                value = MapText.scale,
                onValueChange = { MapText.scale = it },
                minValue = MapText.MIN_SCALE,
                maxValue = MapText.MAX_SCALE,
                caption = "FONT",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = sliderTop, start = 4.dp + 5.dp * expandT, bottom = 4.dp)
                    .width(40.dp)
                    // Another 20% shorter than Topography's on Home (so the chart's bottom-left corner stays clear); the top stays under the arrow button
                .fillMaxHeight(if (embedded) ELEVATION_SLIDER_FRACTION else 1f)
            )
        } else ZoomSlider(
            value = zoom,
            onValueChange = { newZoom ->
                // Zoom about the point on the line nearest the middle of the view, not the (often empty) middle itself
                zoomJob?.cancel()
                pan = panForZoom(pan, zoom, newZoom, nearestLinePoint(), canvasW, canvasH, CHART_PADDING_PX, CHART_PADDING_PX)
                zoom = newZoom
            },
            onStep = { newZoom -> animateZoomTo(newZoom, nearestLinePoint(), canvasW, canvasH) },
            // At the left here (the chart has no use for that side); its top stays put, the bottom follows the chart
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = sliderTop, start = 4.dp + 5.dp * expandT, bottom = 4.dp)
                .width(40.dp)
                // Another 20% shorter than Topography's on Home (so the chart's bottom-left corner stays clear); the top stays under the arrow button
                .fillMaxHeight(if (embedded) ELEVATION_SLIDER_FRACTION else 1f)
        )
    }
}
