package io.github.genesjc.microclimatefinder.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.microclimatefinder.data.remote.LiveObservationCache
import io.github.genesjc.microclimatefinder.data.remote.ObservationResult
import io.github.genesjc.microclimatefinder.data.local.LocalNewsRepository
import io.github.genesjc.microclimatefinder.data.local.StationCsvParser
import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.domain.model.TerrainType
import io.github.genesjc.microclimatefinder.ui.components.LegendItem
import io.github.genesjc.microclimatefinder.ui.components.TerrainLegend
import io.github.genesjc.microclimatefinder.ui.components.MAX_MAP_ZOOM
import io.github.genesjc.microclimatefinder.ui.components.ZoomSlider
import io.github.genesjc.microclimatefinder.ui.components.animateZoom
import kotlinx.coroutines.Job
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

// Labels are about this tall (dp) when checking for overlaps
private const val LABEL_HEIGHT_DP = 18f

/** A label's spot (top-left corner in dp) and whether it has room at the current zoom. */
private data class PlacedLabel(
    val station: MicroclimateStation,
    val text: String,
    val xDp: Float,
    val yDp: Float,
    val hasRoom: Boolean
)

/**
 * Ranks stations for labels, most useful first: each next pick is the station farthest along the
 * profile from those already picked, with airports (K-prefixed, recognizable cities) preferred.
 */
private fun rankLandmarkIndexes(sorted: List<MicroclimateStation>): List<Int> {
    if (sorted.isEmpty()) return emptyList()
    fun bonus(i: Int) = if (sorted[i].id.startsWith("K")) 2f else 0f
    val chosen = mutableListOf(0)
    while (chosen.size < minOf(sorted.size, 60)) {
        val next = sorted.indices
            .filter { it !in chosen }
            .maxByOrNull { c -> chosen.minOf { kotlin.math.abs(c - it) }.toFloat() + bonus(c) }
            ?: break
        chosen.add(next)
    }
    return chosen
}

@Composable
fun ElevationProfileScreen(
    dataset: StationDatasetInfo,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** True when shown inside another screen (the Home tabs): no app bar of its own. */
    embedded: Boolean = false,
    /** A station selected elsewhere (the other Home tab), and a way to report selections back. */
    sharedSelectedStationId: String? = null,
    onStationSelected: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val parser = remember(dataset.fileName) { StationCsvParser(context) }
    val stations = remember(dataset) { parser.loadStations(dataset) }

    // The selected station can be shared with another screen (Home keeps it across its two tabs)
    var localSelectedId by remember { mutableStateOf(stations.firstOrNull()?.id) }
    val selectedStationId = stations.firstOrNull { it.id == sharedSelectedStationId }?.id ?: localSelectedId

    val selectedStation = stations.find { it.id == selectedStationId } ?: stations.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(if (embedded) PaddingValues(top = 0.dp, bottom = 16.dp) else PaddingValues(16.dp))
            .verticalScroll(rememberScrollState())
    ) {
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
                    text = "Elevation Profile",
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

        // Elevation Canvas Container
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            ElevationProfileCanvas(
                stations = stations,
                selectedStationId = selectedStationId,
                onSelectStation = {
                    localSelectedId = it.id
                    onStationSelected?.invoke(it.id)
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Terrain legend: one line until opened, then this region's terrain types with counts.
        // Embedded, Home shows it beside the Region Info link.
        if (!embedded) TerrainLegend(stations = stations)

        // The selected station card; embedded, Home shows it above the tabs
        if (!embedded) {
        Spacer(modifier = Modifier.height(16.dp))

        // Selected station card (the Station Inspector)

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

// Data class for atmospheric sky gradient & weather condition above the elevation profile
private data class SkyAtmosphereTheme(
    val colors: List<Color>,
    val condition: String,
    val icon: String,
    val accent: Color,
    val badgeBg: Color
)

@Composable
private fun ElevationProfileCanvas(
    stations: List<MicroclimateStation>,
    selectedStationId: String?,
    onSelectStation: (MicroclimateStation) -> Unit
) {
    if (stations.isEmpty()) return

    // Sort stations by elevation ascending (valley 5m -> ridge 1406m)
    val sorted = remember(stations) { stations.sortedBy { it.elevationMeters } }
    val maxElevation = remember(sorted) { (sorted.maxOfOrNull { it.elevationMeters } ?: 1500).toFloat() }
    // Stations ranked for labels; the first few are the ones the sky badge cycles through
    val rankedLandmarks = remember(sorted) { rankLandmarkIndexes(sorted) }
    val cycleIds = remember(sorted) {
        rankedLandmarks.take(LANDMARK_COUNT).sorted().map { sorted[it].id }
    }

    // View transform: an even zoom (like a magnifying glass) about the centre plus a pan in pixels
    var zoom by remember(sorted) { mutableStateOf(1f) }
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
        ?: sorted.firstOrNull { LiveObservationCache.peek(it.id) is ObservationResult.Reading }
        ?: sorted.firstOrNull()
    // Real NWS temperature for the selected station; null while loading or when it has no reading
    var live by remember(currentStation?.id) {
        mutableStateOf(currentStation?.let { LiveObservationCache.peek(it.id) })
    }
    LaunchedEffect(currentStation?.id) {
        currentStation?.let {
            live = if (it.canHaveLiveReading) LiveObservationCache.get(it.id) else ObservationResult.NoReading
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
            .pointerInput(Unit) {
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
            .pointerInput(Unit) {
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

        // Which labels have room at this zoom: more as the profile spreads out. Only the target (room
        // or no room) is decided here; each label's opacity eases toward it on its own.
        val placedLabels = run {
            val budget = labelBudget(LANDMARK_COUNT, zoom, 5f).toInt()
            val taken = mutableListOf(
                listOf(0f, 0f, 46f, heightDp.value),      // zoom slider
                listOf(45f, 45f, 215f, 92f)               // weather badge
            )
            // Null when the station is out of view; otherwise its label spot, marked with or without room
            fun plan(idx: Int, allow: Boolean, force: Boolean): PlacedLabel? {
                val station = sorted[idx]
                val ratioX = idx.toFloat() / (sorted.size - 1).coerceAtLeast(1)
                val elevRatio = station.elevationMeters / maxElevation
                val pinX = with(density) { viewX(paddingPx + ratioX * widthPx).toDp().value }
                val pinY = with(density) { viewY(paddingPx + (1f - elevRatio) * heightPx).toDp().value }
                if (pinX < 0f || pinX > widthDp.value || pinY < 0f || pinY > heightDp.value) return null
                val text = LocalNewsRepository.getCleanCityName(station.id, station.name) +
                    " ${"%,d".format(station.elevationMeters)}m"
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
                            val y = viewY(padding + (1f - elevRatio) * h)

                            val dx = tapOffset.x - x
                            val dy = tapOffset.y - y
                            val dist = (dx * dx + dy * dy)
                            if (dist < closestDist && dist < 1800f) {
                                closestDist = dist
                                closestStation = station
                            }
                        }
                        closestStation?.let { onSelectStation(it) }
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
                lineTo(viewX(size.width - padding), viewY(padding + (1f - lastElevRatio) * h))
                for (i in (sorted.size - 1) downTo 0) {
                    val st = sorted[i]
                    val x = viewX(padding + (i.toFloat() / (sorted.size - 1).coerceAtLeast(1)) * w)
                    val elevRatio = st.elevationMeters / maxElevation
                    val y = viewY(padding + (1f - elevRatio) * h)
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
                val y = viewY(padding + (1f - elevRatio) * h)

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

            // Gradient under the elevation curve (Warm valley 28°C -> Cool Ridge 18°C)
            drawPath(
                path = terrainPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF7C3AED).copy(alpha = 0.35f), // High ridge cool air
                        Color(0xFF0284C7).copy(alpha = 0.20f), // Mid
                        Color(0xFFEA580C).copy(alpha = 0.12f)  // Valley basin heat
                    )
                )
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
                val y = viewY(padding + (1f - elevRatio) * h)
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
            }
        }

        // 4. Atmospheric Sky Micro-Display in the blank space to the left of the graph (15px margin from top & left)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = skyTheme.badgeBg.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, skyTheme.accent.copy(alpha = 0.45f)),
            modifier = Modifier
                .offset(x = 45.dp, y = 45.dp)
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
                Text(text = skyTheme.icon, fontSize = 14.sp)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tempF?.let { "${it.toInt()}°F" } ?: "—°F",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentStation?.name?.take(16) ?: "Sacramento",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1
                        )
                    }
                    Text(
                        text = "${skyTheme.condition} • ${currentStation?.elevationMeters}m elev",
                        fontSize = 8.sp,
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
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        // Floating zoom slider on the left edge (the steep ridge is on the right)
        fun nearestLinePoint(): Offset {
            val middle = Offset(canvasW / 2f, canvasH / 2f)
            return sorted.indices
                .map { i ->
                    val elevRatio = sorted[i].elevationMeters / maxElevation
                    Offset(
                        viewX(paddingPx + i.toFloat() / (sorted.size - 1).coerceAtLeast(1) * widthPx),
                        viewY(paddingPx + (1f - elevRatio) * heightPx)
                    )
                }
                .minByOrNull { (it - middle).getDistance() } ?: middle
        }
        ZoomSlider(
            value = zoom,
            onValueChange = { newZoom ->
                // Zoom about the point on the line nearest the middle of the view, not the (often empty) middle itself
                zoomJob?.cancel()
                pan = panForZoom(pan, zoom, newZoom, nearestLinePoint(), canvasW, canvasH, CHART_PADDING_PX, CHART_PADDING_PX)
                zoom = newZoom
            },
            onStep = { newZoom -> animateZoomTo(newZoom, nearestLinePoint(), canvasW, canvasH) },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .width(40.dp)
                .fillMaxHeight(0.9f)
        )
    }
}
