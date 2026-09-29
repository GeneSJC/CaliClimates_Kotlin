package io.github.genesjc.microclimatefinder.ui.screens

import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.microclimatefinder.data.local.TransectConfig
import io.github.genesjc.microclimatefinder.data.local.LocalNewsRepository
import io.github.genesjc.microclimatefinder.data.local.StationCsvParser
import io.github.genesjc.microclimatefinder.domain.model.LocalNewsStory
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.ui.components.StationCard
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Unified Draggable Topo Map (v0.20.0 - Macro Central California Transect)
 *
 * Demonstrates continuous 4-way pan and multi-touch zoom spanning from Northern California
 * (Sacramento River Basin & Sierra Crest, 39.4°N) south to Central Coast California
 * (Monterey Bay, Salinas Valley thermal trough & Santa Lucia Range, 35.6°N) across 36 regional NOAA stations.
 *
 * Features:
 *  - Continuous edge-to-edge topographical rendering without bounding border clipping.
 *  - Macro geography: Pacific Coastline & Monterey Bay indentation, Coast Ranges,
 *    Sacramento & San Joaquin Valleys, Delta breeze gate, and Sierra Nevada elevation bands.
 *  - Quick-jump region anchor chips: [📍 Sacramento], [🌊 Monterey Bay], [🗺️ View All].
 *  - Real-time pan & zoom HUD readout with live offset display.
 *  - Floating +/- zoom controls and tap-to-inspect StationCard integration.
 *  - Expandable Local News panel with direct story links and in-app viewer.
 */
@Composable
fun UnifiedDraggableMapScreen(
    config: TransectConfig,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenNewsStory: ((LocalNewsStory, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val parser = remember { StationCsvParser(context) }
    val stations = remember(config) {
        try {
            val unified = parser.loadStationsFromCsv(config.assetFileName)
            if (unified.isNotEmpty()) unified else parser.loadStationsFromCsv(config.fallbackAssetFileName)
        } catch (e: Exception) {
            parser.loadStationsFromCsv(config.fallbackAssetFileName)
        }
    }

    fun defaultStation() =
        config.defaultStationIds.firstNotNullOfOrNull { id -> stations.firstOrNull { it.id == id } }
            ?: stations.firstOrNull()

    var selectedStation by remember(config) { mutableStateOf(defaultStation()) }

    LaunchedEffect(stations) {
        if (selectedStation == null && stations.isNotEmpty()) {
            selectedStation = defaultStation()
        }
    }

    // Interactive Pan & Zoom State
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var lastDragSnackbarTimeMs by remember { mutableLongStateOf(0L) }

    fun showSnackbar(message: String) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    // Smooth programmatic panning & zooming for anchor chips and zoom buttons
    fun animateTo(targetPanX: Float, targetPanY: Float, targetZoom: Float) {
        val clampedZoom = targetZoom.coerceIn(1.0f, 4.0f)
        coroutineScope.launch {
            val startX = panX
            val startY = panY
            val startZ = zoomScale
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            ) { progress, _ ->
                panX = startX + (targetPanX - startX) * progress
                panY = startY + (targetPanY - startY) * progress
                zoomScale = startZ + (clampedZoom - startZ) * progress
            }
        }
    }

    // Geographic bounding box for the entire Central California transect
    val rawMinLat = remember(stations) { if (stations.isNotEmpty()) stations.minOf { it.latitude } else 35.6 }
    val rawMaxLat = remember(stations) { if (stations.isNotEmpty()) stations.maxOf { it.latitude } else 39.4 }
    val rawMinLon = remember(stations) { if (stations.isNotEmpty()) stations.minOf { it.longitude } else -122.5 }
    val rawMaxLon = remember(stations) { if (stations.isNotEmpty()) stations.maxOf { it.longitude } else -120.4 }

    val latSpan = (rawMaxLat - rawMinLat).coerceAtLeast(0.01)
    val lonSpan = (rawMaxLon - rawMinLon).coerceAtLeast(0.01)

    val minLat = rawMinLat - (latSpan * 0.08)
    val maxLat = rawMaxLat + (latSpan * 0.08)
    val minLon = rawMinLon - (lonSpan * 0.08)
    val maxLon = rawMaxLon + (lonSpan * 0.08)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp)
    ) {
        // 1. Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
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

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "${config.displayName} Transect Map",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${config.routeLabel} • ${stations.size} Stations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier.padding(start = 6.dp)
            ) {
                Text(
                    text = "v0.20.0",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D4ED8),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Main Draggable & Zoomable Canvas Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
            ) {
                val canvasWidth = constraints.maxWidth.toFloat()
                val canvasHeight = constraints.maxHeight.toFloat()
                val cX = canvasWidth / 2f
                val cY = canvasHeight / 2f

                // Geographic coordinate to screen pixel transform taking into account center zoom and pan
                fun toScreenX(lon: Double): Float {
                    val normX = (lon - minLon) / (maxLon - minLon)
                    val baseX = (normX * canvasWidth).toFloat()
                    return cX + (baseX - cX) * zoomScale + panX
                }

                fun toScreenY(lat: Double): Float {
                    val normY = (maxLat - lat) / (maxLat - minLat)
                    val baseY = (normY * canvasHeight).toFloat()
                    return cY + (baseY - cY) * zoomScale + panY
                }

                // Performance: Reusable, remembered paint instances to eliminate per-frame GC allocations
                val textPaint = remember {
                    Paint().apply {
                        color = android.graphics.Color.parseColor("#0F172A")
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        isAntiAlias = true
                    }
                }
                val selectedTextPaint = remember {
                    Paint().apply {
                        color = android.graphics.Color.WHITE
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        isAntiAlias = true
                    }
                }
                val subTextPaint = remember {
                    Paint().apply {
                        color = android.graphics.Color.parseColor("#475569")
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        isAntiAlias = true
                    }
                }
                val selectedSubTextPaint = remember {
                    Paint().apply {
                        color = android.graphics.Color.parseColor("#BAE6FD")
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        isAntiAlias = true
                    }
                }
                val pillBgPaint = remember {
                    Paint().apply {
                        color = android.graphics.Color.WHITE
                        alpha = 230
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                }
                val selectedPillBgPaint = remember {
                    Paint().apply {
                        color = android.graphics.Color.parseColor("#0F172A")
                        alpha = 245
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                }
                val pillBorderPaint = remember {
                    Paint().apply {
                        color = android.graphics.Color.parseColor("#CBD5E1")
                        strokeWidth = 1.5f
                        style = Paint.Style.STROKE
                        isAntiAlias = true
                    }
                }
                val selectedPillBorderPaint = remember {
                    Paint().apply {
                        color = android.graphics.Color.parseColor("#38BDF8")
                        strokeWidth = 2.5f
                        style = Paint.Style.STROKE
                        isAntiAlias = true
                    }
                }
                val gridPaint = remember {
                    Paint().apply {
                        color = android.graphics.Color.parseColor("#94A3B8")
                        textSize = 24f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        isAntiAlias = true
                    }
                }

                // Performance: Reusable remembered paths (cleared via rewind() rather than instantiated every frame)
                val reusableOceanPath = remember { Path() }
                val reusableSantaLuciaPath = remember { Path() }
                val reusableSalinasValleyPath = remember { Path() }
                val reusableDiabloRangePath = remember { Path() }
                val reusableSierraFoothillsPath = remember { Path() }
                val reusableSierraHighRidgePath = remember { Path() }
                val reusableSacRiverPath = remember { Path() }
                val reusableAmericanRiverPath = remember { Path() }
                val reusableSanJoaquinPath = remember { Path() }
                val reusableSalinasRiverPath = remember { Path() }
                val reusablePlacedBounds = remember { ArrayList<RectF>(40) }

                // Performance: Stable LOD ordering remembered once, not sorted on each frame
                val sortedStations = remember(stations) {
                    stations.sortedBy { getStationLODTier(it.id) }
                }

                // Smooth label fade-in and fade-out animation engine
                val stationLabelAlphas = remember {
                    val initialMap = HashMap<String, Float>(48)
                    stations.forEach { st ->
                        val tier = getStationLODTier(st.id)
                        val isInitialMajor = tier == 1 || st.id == "KMRY" || st.id == "KSAC"
                        initialMap[st.id] = if (isInitialMajor) 1f else 0f
                    }
                    initialMap
                }
                val targetLabelAlphas = remember { HashMap<String, Float>(48) }
                var isAnimatingLabels by remember { mutableStateOf(false) }
                var animFrameTick by remember { mutableLongStateOf(0L) }

                // Smooth 60-120fps alpha interpolation loop when label visibility transitions
                LaunchedEffect(isAnimatingLabels) {
                    if (isAnimatingLabels) {
                        var lastTimeNanos = withFrameNanos { it }
                        while (isActive && isAnimatingLabels) {
                            withFrameNanos { nowNanos ->
                                val dt = ((nowNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                                lastTimeNanos = nowNanos

                                var stillActive = false
                                val fadeSpeed = 4.5f // Completes 0.0 -> 1.0 fade in ~220ms

                                for (station in stations) {
                                    val current = stationLabelAlphas[station.id] ?: 0f
                                    val target = targetLabelAlphas[station.id] ?: 0f
                                    if (abs(current - target) > 0.005f) {
                                        val step = dt * fadeSpeed
                                        val next = if (current < target) {
                                            (current + step).coerceAtMost(target)
                                        } else {
                                            (current - step).coerceAtLeast(target)
                                        }
                                        stationLabelAlphas[station.id] = next
                                        stillActive = true
                                    } else if (current != target) {
                                        stationLabelAlphas[station.id] = target
                                    }
                                }

                                animFrameTick = nowNanos
                                if (!stillActive) {
                                    isAnimatingLabels = false
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .pointerInput(canvasWidth, canvasHeight) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var isInteracting = true
                                while (isInteracting) {
                                    val event = awaitPointerEvent()
                                    val canceled = event.changes.any { it.isConsumed }
                                    if (canceled || event.changes.all { !it.pressed }) {
                                        isInteracting = false
                                    } else {
                                        val panDelta = event.calculatePan()
                                        val zoomDelta = event.calculateZoom()

                                        val newZoom = (zoomScale * zoomDelta).coerceIn(1.0f, 4.0f)
                                        if (zoomDelta != 1.0f && newZoom == zoomScale) {
                                            val now = System.currentTimeMillis()
                                            if (now - lastDragSnackbarTimeMs > 2500L) {
                                                lastDragSnackbarTimeMs = now
                                                if (zoomDelta < 1.0f && zoomScale <= 1.05f) {
                                                    showSnackbar("Max zoom-out reached")
                                                } else if (zoomDelta > 1.0f && zoomScale >= 3.95f) {
                                                    showSnackbar("Max zoom-in reached")
                                                }
                                            }
                                        }
                                        zoomScale = newZoom

                                        // Restrict panning to keep Bay Area & Sacramento transect stations firmly in view.
                                        // Prevents dragging off into empty void to the West (Pacific), East (Nevada), North, or South.
                                        val maxPanX = (canvasWidth * 0.20f + canvasWidth * 0.22f * (zoomScale - 1f)).coerceAtLeast(canvasWidth * 0.18f)
                                        val maxPanY = (canvasHeight * 0.22f + canvasHeight * 0.24f * (zoomScale - 1f)).coerceAtLeast(canvasHeight * 0.20f)

                                        val tentativePanX = panX + panDelta.x
                                        val tentativePanY = panY + panDelta.y

                                        val clampedPanX = tentativePanX.coerceIn(-maxPanX, maxPanX)
                                        val clampedPanY = tentativePanY.coerceIn(-maxPanY, maxPanY)

                                        // If user attempts to drag beyond allowed boundary, alert them with snackbar
                                        if ((panDelta.x != 0f && clampedPanX == panX && tentativePanX != panX) ||
                                            (panDelta.y != 0f && clampedPanY == panY && tentativePanY != panY)) {
                                            val now = System.currentTimeMillis()
                                            if (now - lastDragSnackbarTimeMs > 2200L) {
                                                lastDragSnackbarTimeMs = now
                                                showSnackbar("Max dragging area reached")
                                            }
                                        }

                                        panX = clampedPanX
                                        panY = clampedPanY

                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .pointerInput(stations, zoomScale, panX, panY) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val tapPos = down.position
                                    var closestStation: MicroclimateStation? = null
                                    var closestDist = Float.MAX_VALUE
                                    val hitThreshold = 75f * zoomScale.coerceIn(0.8f, 2.0f)

                                    stations.forEach { st ->
                                        val px = toScreenX(st.longitude)
                                        val py = toScreenY(st.latitude)
                                        val dx = tapPos.x - px
                                        val dy = tapPos.y - py
                                        val d = kotlin.math.sqrt(dx * dx + dy * dy)
                                        if (d < closestDist && d <= hitThreshold) {
                                            closestDist = d
                                            closestStation = st
                                        }
                                    }

                                    closestStation?.let { selectedStation = it }
                                }
                            }
                    ) {
                        // Subscribe drawing pass to frame tick when label opacity is animating
                        @Suppress("UNUSED_VARIABLE")
                        val subscribedFrameTick = animFrameTick

                        // Strict Skia hardware clipping: Nothing drawn inside can ever escape the map viewport bounds!
                        clipRect(0f, 0f, size.width, size.height) {
                            // 1. Base Continental Valley Landmass (Light fertile sage)
                            drawRect(color = Color(0xFFF1F6EC))

                            // Far geographic boundaries to guarantee terrain seamlessly fills space during pan/zoom
                            val farWest = toScreenX(-135.0).coerceAtMost(-1000f)
                            val farEast = toScreenX(-115.0).coerceAtLeast(canvasWidth + 1000f)
                            val farNorth = toScreenY(42.0).coerceAtMost(-1000f)
                            val farSouth = toScreenY(33.0).coerceAtLeast(canvasHeight + 1000f)

                            // 2. Pacific Ocean (Western Coastal Waters - Seamlessly extends far to the west and north/south)
                            reusableOceanPath.rewind()
                            reusableOceanPath.apply {
                                moveTo(farWest, farNorth)
                                lineTo(toScreenX(-122.55), farNorth)
                                lineTo(toScreenX(-122.52), toScreenY(39.40))
                                lineTo(toScreenX(-122.48), toScreenY(38.80))
                                lineTo(toScreenX(-122.45), toScreenY(38.30)) // Bodega Bay
                                lineTo(toScreenX(-122.52), toScreenY(37.75)) // Point Reyes / Golden Gate
                                lineTo(toScreenX(-122.50), toScreenY(37.50)) // Half Moon Bay
                                lineTo(toScreenX(-122.25), toScreenY(37.15)) // Ano Nuevo
                                lineTo(toScreenX(-122.03), toScreenY(36.97)) // Santa Cruz
                                lineTo(toScreenX(-121.80), toScreenY(36.80)) // Moss Landing (Deep Submarine Canyon)
                                lineTo(toScreenX(-121.89), toScreenY(36.62)) // Monterey Harbor
                                lineTo(toScreenX(-121.95), toScreenY(36.56)) // Carmel Bay
                                lineTo(toScreenX(-121.80), toScreenY(36.25)) // Big Sur Coast
                                lineTo(toScreenX(-121.40), toScreenY(35.50))
                                lineTo(toScreenX(-121.00), farSouth)
                                lineTo(farWest, farSouth)
                                close()
                            }
                            drawPath(path = reusableOceanPath, color = Color(0xFFE0F2FE))
                            drawPath(path = reusableOceanPath, color = Color(0xFF0284C7).copy(alpha = 0.45f), style = Stroke(width = 2.2f))

                            // 3. Santa Lucia Range & Big Sur Mountains (South-West Coast)
                            reusableSantaLuciaPath.rewind()
                            reusableSantaLuciaPath.apply {
                                moveTo(toScreenX(-121.90), toScreenY(36.65))
                                lineTo(toScreenX(-121.50), toScreenY(36.25)) // Chews Ridge (1,538m)
                                lineTo(toScreenX(-121.30), toScreenY(35.70))
                                lineTo(toScreenX(-121.05), toScreenY(35.50))
                                lineTo(toScreenX(-121.35), toScreenY(35.50))
                                lineTo(toScreenX(-121.75), toScreenY(36.20))
                                close()
                            }
                            drawPath(path = reusableSantaLuciaPath, color = Color(0xFFBED7A6))

                            // 4. Salinas Valley Thermal Trough (Between Santa Lucia & Gabilan Ranges)
                            reusableSalinasValleyPath.rewind()
                            reusableSalinasValleyPath.apply {
                                moveTo(toScreenX(-121.80), toScreenY(36.80)) // Moss Landing Gate
                                lineTo(toScreenX(-121.65), toScreenY(36.68)) // Salinas
                                lineTo(toScreenX(-121.40), toScreenY(36.35)) // Soledad / King City
                                lineTo(toScreenX(-120.70), toScreenY(35.65)) // Paso Robles
                                lineTo(toScreenX(-120.85), toScreenY(35.65))
                                lineTo(toScreenX(-121.50), toScreenY(36.30))
                                close()
                            }
                            drawPath(path = reusableSalinasValleyPath, color = Color(0xFFEDF5E7))

                            // 5. Diablo Range & Mount Hamilton (1,280m) in Central Transect
                            reusableDiabloRangePath.rewind()
                            reusableDiabloRangePath.apply {
                                moveTo(toScreenX(-121.80), toScreenY(37.90))
                                lineTo(toScreenX(-121.64), toScreenY(37.34)) // Mount Hamilton
                                lineTo(toScreenX(-121.30), toScreenY(36.80)) // Pinnacles & Gabilan
                                lineTo(toScreenX(-121.15), toScreenY(36.45))
                                lineTo(toScreenX(-121.40), toScreenY(36.50))
                                lineTo(toScreenX(-121.85), toScreenY(37.40))
                                close()
                            }
                            drawPath(path = reusableDiabloRangePath, color = Color(0xFFD7E5C8))

                            // 6. Sierra Nevada Elevation Relief Bands (Rising eastward, extending far east seamlessly)
                            reusableSierraFoothillsPath.rewind()
                            reusableSierraFoothillsPath.apply {
                                moveTo(toScreenX(-121.36), farNorth)
                                lineTo(toScreenX(-121.36), toScreenY(39.50))
                                lineTo(toScreenX(-121.30), toScreenY(39.10))
                                lineTo(toScreenX(-121.15), toScreenY(38.80))
                                lineTo(toScreenX(-120.95), toScreenY(38.40))
                                lineTo(toScreenX(-120.75), toScreenY(38.00))
                                lineTo(toScreenX(-120.40), toScreenY(37.50))
                                lineTo(toScreenX(-120.10), toScreenY(37.50))
                                lineTo(farEast, toScreenY(37.50))
                                lineTo(farEast, farNorth)
                                close()
                            }
                            drawPath(path = reusableSierraFoothillsPath, color = Color(0xFFDFE9D0))

                            reusableSierraHighRidgePath.rewind()
                            reusableSierraHighRidgePath.apply {
                                moveTo(toScreenX(-121.00), farNorth)
                                lineTo(toScreenX(-121.00), toScreenY(39.50))
                                lineTo(toScreenX(-120.85), toScreenY(39.15))
                                lineTo(toScreenX(-120.70), toScreenY(38.85))
                                lineTo(toScreenX(-120.50), toScreenY(38.40))
                                lineTo(toScreenX(-120.30), toScreenY(38.00))
                                lineTo(toScreenX(-120.00), toScreenY(38.00))
                                lineTo(farEast, toScreenY(38.00))
                                lineTo(farEast, farNorth)
                                close()
                            }
                            drawPath(path = reusableSierraHighRidgePath, color = Color(0xFFAECFA0))

                            // 7. Waterways: Sacramento River, American River, Delta, Salinas River
                            val waterColor = Color(0xFF0284C7)
                            val waterGlow = Color(0xFF38BDF8).copy(alpha = 0.35f)

                            // Sacramento River
                            reusableSacRiverPath.rewind()
                            reusableSacRiverPath.apply {
                                moveTo(toScreenX(-121.90), toScreenY(39.40))
                                lineTo(toScreenX(-121.75), toScreenY(39.00))
                                lineTo(toScreenX(-121.61), toScreenY(38.70))
                                lineTo(toScreenX(-121.51), toScreenY(38.59))
                                lineTo(toScreenX(-121.52), toScreenY(38.50))
                                lineTo(toScreenX(-121.75), toScreenY(38.15)) // Into Delta
                                lineTo(toScreenX(-122.10), toScreenY(38.05)) // Carquinez Strait
                            }
                            drawPath(reusableSacRiverPath, color = waterGlow, style = Stroke(width = 4.2f, cap = StrokeCap.Round))
                            drawPath(reusableSacRiverPath, color = waterColor, style = Stroke(width = 2.4f, cap = StrokeCap.Round))

                            // American River & Folsom Lake
                            reusableAmericanRiverPath.rewind()
                            reusableAmericanRiverPath.apply {
                                moveTo(toScreenX(-120.95), toScreenY(38.85))
                                lineTo(toScreenX(-121.15), toScreenY(38.70))
                                lineTo(toScreenX(-121.51), toScreenY(38.59))
                            }
                            drawPath(reusableAmericanRiverPath, color = waterColor, style = Stroke(width = 2.2f, cap = StrokeCap.Round))

                            // San Joaquin River & Stockton Channel
                            reusableSanJoaquinPath.rewind()
                            reusableSanJoaquinPath.apply {
                                moveTo(toScreenX(-121.20), toScreenY(37.75))
                                lineTo(toScreenX(-121.32), toScreenY(37.95)) // Stockton
                                lineTo(toScreenX(-121.70), toScreenY(38.10)) // Confluence
                            }
                            drawPath(reusableSanJoaquinPath, color = waterColor, style = Stroke(width = 2.2f, cap = StrokeCap.Round))

                            // Salinas River (South into Monterey Bay)
                            reusableSalinasRiverPath.rewind()
                            reusableSalinasRiverPath.apply {
                                moveTo(toScreenX(-120.70), toScreenY(35.65)) // Paso Robles
                                lineTo(toScreenX(-121.15), toScreenY(36.15))
                                lineTo(toScreenX(-121.45), toScreenY(36.45))
                                lineTo(toScreenX(-121.65), toScreenY(36.68)) // Salinas
                                lineTo(toScreenX(-121.80), toScreenY(36.78)) // Monterey Bay estuary
                            }
                            drawPath(reusableSalinasRiverPath, color = waterGlow, style = Stroke(width = 3.6f, cap = StrokeCap.Round))
                            drawPath(reusableSalinasRiverPath, color = waterColor, style = Stroke(width = 2.0f, cap = StrokeCap.Round))

                            // 8. Geographic Reference Grid (36°N, 37°N, 38°N, 39°N)
                            val gridColor = Color(0xFF64748B).copy(alpha = 0.12f)
                            for (lat in listOf(36.0, 37.0, 38.0, 39.0)) {
                                val y = toScreenY(lat)
                                drawLine(color = gridColor, start = Offset(0f, y), end = Offset(canvasWidth, y), strokeWidth = 1.2f)
                                drawContext.canvas.nativeCanvas.drawText("${lat.toInt()}°N", 16f, y - 6f, gridPaint)
                            }

                            // 9. Station Pins (Base Core & Selection Pulse)
                            stations.forEach { station ->
                                val pinX = toScreenX(station.longitude)
                                val pinY = toScreenY(station.latitude)

                                if (pinX >= -60f && pinX <= canvasWidth + 60f && pinY >= -60f && pinY <= canvasHeight + 60f) {
                                    val isSelected = station.id == selectedStation?.id

                                    val pinColor = when {
                                        station.elevationMeters < 30 -> Color(0xFF0284C7)   // Coastal/Sea level (Sky Blue)
                                        station.elevationMeters < 100 -> Color(0xFFF59E0B)  // Valley Floor (Amber)
                                        station.elevationMeters < 400 -> Color(0xFF10B981)  // Foothills (Emerald)
                                        station.elevationMeters < 900 -> Color(0xFF3B82F6)  // Low Montane (Blue)
                                        else -> Color(0xFFA855F7)                           // Alpine/High Ridge (Purple)
                                    }

                                    val baseRadius = if (isSelected) 9.dp.toPx() else 6.5.dp.toPx()
                                    val scaledRadius = (baseRadius * (1f + (zoomScale - 1f) * 0.25f)).coerceIn(5.5.dp.toPx(), 16.dp.toPx())

                                    // Selection Pulse Halo
                                    if (isSelected) {
                                        drawCircle(
                                            color = pinColor.copy(alpha = 0.25f),
                                            radius = scaledRadius * 2.5f,
                                            center = Offset(pinX, pinY)
                                        )
                                        drawCircle(
                                            color = pinColor.copy(alpha = 0.45f),
                                            radius = scaledRadius * 1.6f,
                                            center = Offset(pinX, pinY)
                                        )
                                    }

                                    // White Pin Border for crisp contrast over any topo relief
                                    drawCircle(
                                        color = Color.White,
                                        radius = scaledRadius + (2.dp.toPx() * (1f + (zoomScale - 1f) * 0.2f)).coerceIn(1.8f, 3.8f),
                                        center = Offset(pinX, pinY)
                                    )

                                    // Solid Vibrant Pin Core
                                    drawCircle(
                                        color = pinColor,
                                        radius = scaledRadius,
                                        center = Offset(pinX, pinY)
                                    )
                                }
                            }

                            // 10. Level-of-Detail (LOD) & Anti-Collision Label Badges with Smooth Fade In/Out
                            val baseFontSize = (20f * zoomScale.coerceIn(0.85f, 1.25f))
                            textPaint.textSize = baseFontSize
                            selectedTextPaint.textSize = baseFontSize + 2f
                            subTextPaint.textSize = baseFontSize * 0.82f
                            selectedSubTextPaint.textSize = baseFontSize * 0.82f

                            reusablePlacedBounds.clear()

                            // Render selected station first so it always takes label priority without colliding
                            val stationsToRender = if (selectedStation != null) {
                                listOf(selectedStation!!) + sortedStations.filter { it.id != selectedStation?.id }
                            } else {
                                sortedStations
                            }

                            // Pass 1: Compute target alpha and resolve spatial collision for each station
                            var needsAnim = false
                            for (station in stationsToRender) {
                                val pinX = toScreenX(station.longitude)
                                val pinY = toScreenY(station.latitude)

                                if (pinX < -60f || pinX > canvasWidth + 60f || pinY < -60f || pinY > canvasHeight + 60f) {
                                    targetLabelAlphas[station.id] = 0f
                                    if ((stationLabelAlphas[station.id] ?: 0f) > 0.005f) {
                                        needsAnim = true
                                    }
                                    continue
                                }

                                val isSelected = station.id == selectedStation?.id
                                val tier = getStationLODTier(station.id)

                                // Smooth progressive zoom target LOD:
                                // Tier 1: Always target 1.0f (Sacramento, Monterey, Stockton, Salinas, Palo Alto, etc.)
                                // Tier 2: Smooth ramp between zoom 1.15x and 1.30x (Auburn, Santa Cruz, Travis AFB, etc.)
                                // Tier 3: Smooth ramp between zoom 1.70x and 1.90x (Local foothill stations, ranches)
                                val zoomTarget = if (isSelected) {
                                    1.0f
                                } else when (tier) {
                                    1 -> 1.0f
                                    2 -> ((zoomScale - 1.05f) / 0.15f).coerceIn(0f, 1f)
                                    else -> ((zoomScale - 1.25f) / 0.20f).coerceIn(0f, 1f)
                                }

                                if (zoomTarget <= 0f) {
                                    targetLabelAlphas[station.id] = 0f
                                    if ((stationLabelAlphas[station.id] ?: 0f) > 0.005f) {
                                        needsAnim = true
                                    }
                                    continue
                                }

                                val labelName = getCleanStationName(station.id, station.name)
                                val currentTextPaint = if (isSelected) selectedTextPaint else textPaint
                                val currentSubPaint = if (isSelected) selectedSubTextPaint else subTextPaint

                                val nameWidth = currentTextPaint.measureText(labelName)
                                val nameHeight = currentTextPaint.textSize

                                val showSubtitle = isSelected || zoomScale >= 1.65f
                                val subText = if (showSubtitle) {
                                    "${station.elevationMeters}m • ${station.currentReading.temperatureCelsius.roundToInt()}°C"
                                } else null
                                val subWidth = subText?.let { currentSubPaint.measureText(it) } ?: 0f
                                val subHeight = if (subText != null) currentSubPaint.textSize + 2f else 0f

                                val padH = 8f
                                val padV = 5f
                                val badgeWidth = maxOf(nameWidth, subWidth) + (padH * 2f)
                                val badgeHeight = nameHeight + subHeight + (padV * 2f)

                                val baseRadius = if (isSelected) 10f else 6.5f
                                val scaledRadius = baseRadius * zoomScale.coerceIn(0.7f, 1.4f)
                                val badgeLeft = pinX + scaledRadius + 5f
                                val badgeTop = pinY - (badgeHeight / 2f)
                                val candidateRect = RectF(badgeLeft, badgeTop, badgeLeft + badgeWidth, badgeTop + badgeHeight)

                                val collisionMarginRect = RectF(
                                    candidateRect.left - 3f,
                                    candidateRect.top - 2f,
                                    candidateRect.right + 3f,
                                    candidateRect.bottom + 2f
                                )

                                var hasCollision = false
                                if (!isSelected) {
                                    for (placed in reusablePlacedBounds) {
                                        if (RectF.intersects(placed, collisionMarginRect)) {
                                            hasCollision = true
                                            break
                                        }
                                    }
                                }

                                if (hasCollision) {
                                    targetLabelAlphas[station.id] = 0f
                                } else {
                                    targetLabelAlphas[station.id] = zoomTarget
                                    reusablePlacedBounds.add(candidateRect)
                                }

                                val cur = stationLabelAlphas[station.id] ?: 0f
                                if (kotlin.math.abs(cur - (targetLabelAlphas[station.id] ?: 0f)) > 0.005f) {
                                    needsAnim = true
                                }
                            }

                            if (needsAnim && !isAnimatingLabels) {
                                isAnimatingLabels = true
                            }

                            // Pass 2: Draw all labels with smooth animated alpha and zero per-frame object allocation
                            for (station in stationsToRender) {
                                val currentAlpha = stationLabelAlphas[station.id] ?: 0f
                                if (currentAlpha <= 0.01f) {
                                    continue // Faded out - zero draw cost
                                }

                                val pinX = toScreenX(station.longitude)
                                val pinY = toScreenY(station.latitude)
                                if (pinX < -60f || pinX > canvasWidth + 60f || pinY < -60f || pinY > canvasHeight + 60f) {
                                    continue
                                }

                                val isSelected = station.id == selectedStation?.id
                                val labelName = getCleanStationName(station.id, station.name)

                                val currentTextPaint = if (isSelected) selectedTextPaint else textPaint
                                val currentSubPaint = if (isSelected) selectedSubTextPaint else subTextPaint

                                val nameWidth = currentTextPaint.measureText(labelName)
                                val nameHeight = currentTextPaint.textSize

                                val subZoomAlpha = if (isSelected) 1f else ((zoomScale - 1.65f) / 0.15f).coerceIn(0f, 1f)
                                val subAlpha = currentAlpha * subZoomAlpha
                                val showSubtitle = isSelected || subAlpha > 0.02f

                                val subText = if (showSubtitle) {
                                    "${station.elevationMeters}m • ${station.currentReading.temperatureCelsius.roundToInt()}°C"
                                } else null
                                val subWidth = subText?.let { currentSubPaint.measureText(it) } ?: 0f
                                val subHeight = if (subText != null) currentSubPaint.textSize + 2f else 0f

                                val padH = 8f
                                val padV = 5f
                                val badgeWidth = maxOf(nameWidth, subWidth) + (padH * 2f)
                                val badgeHeight = nameHeight + subHeight + (padV * 2f)

                                val baseRadius = if (isSelected) 10f else 6.5f
                                val scaledRadius = baseRadius * zoomScale.coerceIn(0.7f, 1.4f)
                                val badgeLeft = pinX + scaledRadius + 5f
                                val badgeTop = pinY - (badgeHeight / 2f)
                                val candidateRect = RectF(badgeLeft, badgeTop, badgeLeft + badgeWidth, badgeTop + badgeHeight)

                                val bgPaint = if (isSelected) selectedPillBgPaint else pillBgPaint
                                val borderPaint = if (isSelected) selectedPillBorderPaint else pillBorderPaint

                                val baseBgAlpha = if (isSelected) 245f else 230f
                                bgPaint.alpha = (baseBgAlpha * currentAlpha).roundToInt().coerceIn(0, 255)
                                borderPaint.alpha = (255f * currentAlpha).roundToInt().coerceIn(0, 255)
                                currentTextPaint.alpha = (255f * currentAlpha).roundToInt().coerceIn(0, 255)
                                currentSubPaint.alpha = (210f * subAlpha).roundToInt().coerceIn(0, 255)

                                // Draw rounded pill background & border with animated alpha
                                drawContext.canvas.nativeCanvas.drawRoundRect(candidateRect, 10f, 10f, bgPaint)
                                drawContext.canvas.nativeCanvas.drawRoundRect(candidateRect, 10f, 10f, borderPaint)

                                // Draw title text
                                val textX = badgeLeft + padH
                                val textY = badgeTop + padV + nameHeight - 2f
                                drawContext.canvas.nativeCanvas.drawText(labelName, textX, textY, currentTextPaint)

                                // Draw elevation and temperature subtitle
                                if (subText != null && subAlpha > 0.02f) {
                                    drawContext.canvas.nativeCanvas.drawText(subText, textX, textY + subHeight, currentSubPaint)
                                }
                            }
                        }
                    }
                }

                // 4. Floating Zoom Widget in Bottom-Right Corner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        // Zoom In Button (+)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .size(38.dp)
                                .clickable {
                                    if (zoomScale >= 3.95f) {
                                        coroutineScope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar("Max zoom-in reached")
                                        }
                                    } else {
                                        val nextZoom = (zoomScale * 1.25f).coerceAtMost(4.0f)
                                        animateTo(panX, panY, nextZoom)
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "+",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }

                        // Current Zoom Multiplier Display (Tap to Recenter)
                        Text(
                            text = "${(zoomScale * 10).roundToInt() / 10f}x",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7),
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .clickable {
                                    animateTo(0f, 0f, 1.0f)
                                }
                        )

                        // Zoom Out Button (−)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .size(38.dp)
                                .clickable {
                                    if (zoomScale <= 1.05f) {
                                        coroutineScope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar("Max zoom-out reached")
                                        }
                                    } else {
                                        val nextZoom = (zoomScale / 1.25f).coerceAtLeast(1.0f)
                                        animateTo(panX, panY, nextZoom)
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "−",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }

                // 5. Visual Gestural Hint Pill in Bottom-Left Corner with Quick Recenter
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = "✥", fontSize = 12.sp, color = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Pan • Pinch to zoom",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )
                        if (panX != 0f || panY != 0f || zoomScale != 1.0f) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFE0F2FE),
                                modifier = Modifier.clickable {
                                    animateTo(0f, 0f, 1.0f)
                                }
                            ) {
                                Text(
                                    text = "Recenter",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0369A1),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // 6. Snackbar Host for Zoom Limit Alerts
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 6. Selected Station Inspection Card
        AnimatedVisibility(
            visible = selectedStation != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            selectedStation?.let { st ->
                val cleanCity = remember(st.id, st.name) {
                    LocalNewsRepository.getCleanCityName(st.id, st.name)
                }
                StationCard(
                    station = st,
                    isSelected = true,
                    onOpenStory = { story ->
                        onOpenNewsStory?.invoke(story, cleanCity)
                    }
                )
            }
        }
    }
}

/**
 * Cartographic Level-Of-Detail (LOD) tiering:
 * - Tier 1: Major Primary Anchors (Visible at all zoom levels >= 0.5x)
 * - Tier 2: Secondary Regional Hubs (Revealed at zoomScale >= 1.25x)
 * - Tier 3: Local stations, ranches, minor airfields (Revealed at zoomScale >= 1.85x)
 * Note: The currently selected/inspected station always has its label forced visible.
 */
private fun getStationLODTier(id: String): Int {
    return when (id) {
        "KSMF",  // Sacramento Intl
        "KMRY",  // Monterey Regional
        "KSCK",  // Stockton Metro
        "KSNS",  // Salinas
        "KPAO",  // Palo Alto
        "CHWC1", // Chews Ridge (1538m apex)
        "MTHC1", // Mount Hamilton (1280m apex)
        "KPRB",  // Paso Robles (Southern terminus)
        "KMYV"   // Marysville (Northern valley terminus)
        -> 1

        "KSAC",  // Sacramento Executive
        "KAUN",  // Auburn Municipal
        "SCZC1", // Santa Cruz
        "KSUU",  // Travis AFB / Fairfield
        "KHAF",  // Half Moon Bay
        "PINC1", // Pinnacles
        "BIGC1", // Big Sur Coast
        "KVCB",  // Vacaville
        "KCVH",  // Hollister
        "BKSC1", // Brooks
        "MTZC1"  // Mount Zion
        -> 2

        else -> 3
    }
}

/**
 * Clean, compact display names optimized for mobile cartography to avoid clutter.
 */
private fun getCleanStationName(id: String, rawName: String): String {
    return when (id) {
        "KSMF" -> "Sacramento Intl"
        "KSAC" -> "Sacramento Exec"
        "KMHR" -> "Mather"
        "KEDU" -> "Davis Arpt"
        "KLHM" -> "Lincoln Reg"
        "KAUN" -> "Auburn"
        "KMYV" -> "Marysville"
        "KVCB" -> "Vacaville"
        "KSUU" -> "Travis AFB"
        "KSCK" -> "Stockton"
        "KPAO" -> "Palo Alto"
        "KHAF" -> "Half Moon Bay"
        "MTHC1" -> "Mt Hamilton"
        "SCZC1" -> "Santa Cruz"
        "KWVI" -> "Watsonville"
        "MSLC1" -> "Moss Landing"
        "KSNS" -> "Salinas"
        "KMRY" -> "Monterey Reg"
        "PMSC1" -> "Pt Pinos"
        "CARC1" -> "Carmel Valley"
        "CHWC1" -> "Chews Ridge"
        "BIGC1" -> "Big Sur"
        "PINC1" -> "Pinnacles"
        "KCVH" -> "Hollister"
        "KPRB" -> "Paso Robles"
        "PLTC1" -> "Pilot Hill"
        "BENC1" -> "Ben Bolt"
        "BDMC1" -> "Bald Mtn"
        "KGOO" -> "Nevada Co"
        "SETC1" -> "Secret Town"
        "RRRC1" -> "Reader Ranch"
        "FSHC1" -> "Seedorchard"
        "BNGC1" -> "Bangor"
        "BKSC1" -> "Brooks"
        "MTZC1" -> "Mt Zion"
        "ESPC1" -> "Banner Rd"
        else -> rawName
            .replace(" Airport", "")
            .replace(" Municipal", "")
            .replace(", Stockton", "")
            .take(16)
    }
}
