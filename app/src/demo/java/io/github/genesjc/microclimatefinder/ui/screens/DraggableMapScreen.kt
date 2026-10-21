package io.github.genesjc.microclimatefinder.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.microclimatefinder.data.local.StationCsvParser
import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.ui.components.StationCard
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Draggable & Pinch-to-Zoom Topographical Map Screen (v0.14.0).
 *
 * Light-theme aligned with the core California Climate Finder design system:
 *  - Seamless edge-to-edge terrain relief matching StationMapScreen (Alluvial valley floor,
 *    Coast Range, Sierra elevation bands, Sutter Buttes, and vibrant blue waterways).
 *  - Infinite-span terrain projection: Panning navigates the geographic area without
 *    exposing artificial card borders or empty margins.
 *  - Smooth 1-finger continuous drag panning and 2-finger pinch/expand zoom.
 *  - Medium-sized floating +/- zoom control widget with live zoom readout in bottom-right corner.
 *  - Real-time HUD showing pan coordinates (ΔX, ΔY) and zoom level with instant Recenter button.
 *  - Tap-to-select station inspection card integration.
 */
@Composable
fun DraggableMapScreen(
    dataset: StationDatasetInfo,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val parser = remember(dataset.fileName) { StationCsvParser(context) }
    val stations = remember(dataset) { parser.loadStations(dataset) }

    var selectedStation by remember {
        mutableStateOf(stations.firstOrNull { it.id == "KSAC" } ?: stations.firstOrNull())
    }

    LaunchedEffect(stations) {
        if (selectedStation == null && stations.isNotEmpty()) {
            selectedStation = stations.firstOrNull { it.id == "KSAC" } ?: stations.first()
        }
    }

    // Interactive Pan & Zoom State
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    // Geographic bounding box with 10% breathing room for Sacramento 60-mile group
    val rawMinLat = remember(stations) { if (stations.isNotEmpty()) stations.minOf { it.latitude } else 38.2 }
    val rawMaxLat = remember(stations) { if (stations.isNotEmpty()) stations.maxOf { it.latitude } else 39.5 }
    val rawMinLon = remember(stations) { if (stations.isNotEmpty()) stations.minOf { it.longitude } else -122.2 }
    val rawMaxLon = remember(stations) { if (stations.isNotEmpty()) stations.maxOf { it.longitude } else -120.4 }

    val latSpan = (rawMaxLat - rawMinLat).coerceAtLeast(0.01)
    val lonSpan = (rawMaxLon - rawMinLon).coerceAtLeast(0.01)

    val minLat = rawMinLat - (latSpan * 0.10)
    val maxLat = rawMaxLat + (latSpan * 0.10)
    val minLon = rawMinLon - (lonSpan * 0.10)
    val maxLon = rawMaxLon + (lonSpan * 0.10)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp)
    ) {
        // 1. Top App Bar (Light Theme)
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
                        text = "Draggable Topo Map",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${dataset.title} • Pan & Zoom",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFECFDF5),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                modifier = Modifier.padding(start = 6.dp)
            ) {
                Text(
                    text = "Pan + Zoom",
                    color = Color(0xFF047857),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. HUD Control Bar (Light Theme)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Coordinate & Zoom Chip
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "OFFSET: ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "ΔX=${panX.roundToInt()}px, ΔY=${panY.roundToInt()}px",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF0284C7)
                    )
                    Text(
                        text = " • ",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${(zoomScale * 10).roundToInt() / 10f}x",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF059669)
                    )
                }

                // Recenter Button
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (panX != 0f || panY != 0f || zoomScale != 1.0f) Color(0xFF0284C7) else Color(0xFF94A3B8),
                    modifier = Modifier
                        .clickable {
                            panX = 0f
                            panY = 0f
                            zoomScale = 1.0f
                        }
                ) {
                    Text(
                        text = "Recenter",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Interactive Draggable & Zoomable Map Canvas (Light Theme & Edge-to-Edge Terrain)
        Card(
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6EC)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
            ) {
                val density = LocalDensity.current
                val canvasWidth = with(density) { maxWidth.toPx() }
                val canvasHeight = with(density) { maxHeight.toPx() }
                val cX = canvasWidth / 2f
                val cY = canvasHeight / 2f

                // Coordinate transforms taking into account center zoom and pan
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

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var totalPan = Offset.Zero
                                var hasTransform = false

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val anyPressed = event.changes.any { it.pressed }
                                    if (!anyPressed) break

                                    if (event.changes.size > 1) {
                                        // Multi-touch: 2-finger pinch/expand zoom + 2-finger pan
                                        hasTransform = true
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()

                                        zoomScale = (zoomScale * zoomChange).coerceIn(0.6f, 3.5f)
                                        val maxPan = canvasWidth * 2.5f * zoomScale
                                        panX = (panX + panChange.x).coerceIn(-maxPan, maxPan)
                                        panY = (panY + panChange.y).coerceIn(-maxPan, maxPan)
                                        event.changes.forEach { it.consume() }
                                    } else if (event.changes.isNotEmpty()) {
                                        // Single-touch: 1-finger drag pan
                                        val change = event.changes[0]
                                        if (change.pressed) {
                                            val dragDelta = change.position - change.previousPosition
                                            totalPan += dragDelta
                                            if (totalPan.getDistance() > 3f) {
                                                hasTransform = true
                                                val maxPan = canvasWidth * 2.5f * zoomScale
                                                panX = (panX + dragDelta.x).coerceIn(-maxPan, maxPan)
                                                panY = (panY + dragDelta.y).coerceIn(-maxPan, maxPan)
                                                change.consume()
                                            }
                                        }
                                    }
                                }

                                // If released without significant drag or zoom, process as tap selection
                                if (!hasTransform && totalPan.getDistance() <= 12f) {
                                    val tapOffset = down.position
                                    val hitRadius = 40.dp.toPx()

                                    val clicked = stations.minByOrNull { st ->
                                        val pinX = toScreenX(st.longitude)
                                        val pinY = toScreenY(st.latitude)
                                        hypot(tapOffset.x - pinX, tapOffset.y - pinY)
                                    }

                                    if (clicked != null) {
                                        val pinX = toScreenX(clicked.longitude)
                                        val pinY = toScreenY(clicked.latitude)
                                        if (hypot(tapOffset.x - pinX, tapOffset.y - pinY) <= hitRadius) {
                                            selectedStation = clicked
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Far bounds so terrain seamlessly covers the viewport during continuous pan
                        val farLeft = -5000f
                        val farRight = canvasWidth + 5000f
                        val farTop = -5000f
                        val farBottom = canvasHeight + 5000f

                        // 1. Base Alluvial Sacramento Valley Floor (Warm fertile light sage)
                        drawRect(color = Color(0xFFF1F6EC))

                        // 2. Coast Range Foothills & Vaca Mountains (Western strip extending off western horizon)
                        val coastRangePath = Path().apply {
                            moveTo(farLeft, farTop)
                            lineTo(toScreenX(-122.02), farTop)
                            lineTo(toScreenX(-121.98), toScreenY(39.10))
                            lineTo(toScreenX(-121.92), toScreenY(38.65))
                            lineTo(toScreenX(-121.88), toScreenY(38.30))
                            lineTo(toScreenX(-121.84), farBottom)
                            lineTo(farLeft, farBottom)
                            close()
                        }
                        drawPath(path = coastRangePath, color = Color(0xFFE3ECD7))

                        // 3. Sierra Nevada Elevation Relief Bands (Rising eastward, extending off eastern horizon)
                        // Band A: Low Foothills & Oak Belt (100m - 350m)
                        val foothillsPath = Path().apply {
                            moveTo(toScreenX(-121.36), farTop)
                            lineTo(toScreenX(-121.30), toScreenY(39.10))
                            lineTo(toScreenX(-121.20), toScreenY(38.80))
                            lineTo(toScreenX(-121.05), toScreenY(38.50))
                            lineTo(toScreenX(-120.85), toScreenY(38.15))
                            lineTo(toScreenX(-120.75), farBottom)
                            lineTo(farRight, farBottom)
                            lineTo(farRight, farTop)
                            close()
                        }
                        drawPath(path = foothillsPath, color = Color(0xFFDFE9D0))

                        // Band B: Montane Pine & Conifer Forest (350m - 850m)
                        val montanePath = Path().apply {
                            moveTo(toScreenX(-121.18), farTop)
                            lineTo(toScreenX(-121.10), toScreenY(39.15))
                            lineTo(toScreenX(-120.95), toScreenY(38.85))
                            lineTo(toScreenX(-120.78), toScreenY(38.55))
                            lineTo(toScreenX(-120.65), toScreenY(38.20))
                            lineTo(toScreenX(-120.55), farBottom)
                            lineTo(farRight, farBottom)
                            lineTo(farRight, farTop)
                            close()
                        }
                        drawPath(path = montanePath, color = Color(0xFFC7DEB1))

                        // Band C: Sierra High Alpine Ridge (>850m - 1400m+)
                        val highRidgePath = Path().apply {
                            moveTo(toScreenX(-120.95), farTop)
                            lineTo(toScreenX(-120.88), toScreenY(39.15))
                            lineTo(toScreenX(-120.72), toScreenY(38.90))
                            lineTo(toScreenX(-120.58), toScreenY(38.60))
                            lineTo(toScreenX(-120.48), toScreenY(38.25))
                            lineTo(farRight, toScreenY(38.25))
                            lineTo(farRight, farTop)
                            close()
                        }
                        drawPath(path = highRidgePath, color = Color(0xFFAECFA0))

                        // 4. Subtle Topographic Elevation Contour Lines
                        val contourColor = Color(0xFF6B8B5B).copy(alpha = 0.35f)
                        val contourStroke = Stroke(width = (1.4f * zoomScale).coerceIn(1.0f, 3.5f), cap = StrokeCap.Round)

                        val contour150 = Path().apply {
                            moveTo(toScreenX(-121.36), farTop)
                            lineTo(toScreenX(-121.30), toScreenY(39.10))
                            lineTo(toScreenX(-121.20), toScreenY(38.80))
                            lineTo(toScreenX(-121.05), toScreenY(38.50))
                            lineTo(toScreenX(-120.85), toScreenY(38.15))
                            lineTo(toScreenX(-120.75), farBottom)
                        }
                        drawPath(contour150, color = contourColor, style = contourStroke)

                        val contour400 = Path().apply {
                            moveTo(toScreenX(-121.18), farTop)
                            lineTo(toScreenX(-121.10), toScreenY(39.15))
                            lineTo(toScreenX(-120.95), toScreenY(38.85))
                            lineTo(toScreenX(-120.78), toScreenY(38.55))
                            lineTo(toScreenX(-120.65), toScreenY(38.20))
                            lineTo(toScreenX(-120.55), farBottom)
                        }
                        drawPath(contour400, color = contourColor, style = contourStroke)

                        val contourCoast = Path().apply {
                            moveTo(toScreenX(-122.02), farTop)
                            lineTo(toScreenX(-121.98), toScreenY(39.10))
                            lineTo(toScreenX(-121.92), toScreenY(38.65))
                            lineTo(toScreenX(-121.88), toScreenY(38.30))
                            lineTo(toScreenX(-121.84), farBottom)
                        }
                        drawPath(contourCoast, color = contourColor.copy(alpha = 0.25f), style = contourStroke)

                        // 5. Sutter Buttes (Distinct circular volcanic peak in northern valley)
                        val sutterX = toScreenX(-121.82)
                        val sutterY = toScreenY(39.21)
                        val sutterR = (16f * zoomScale).coerceIn(10f, 50f)
                        drawCircle(color = Color(0xFFD2E3C0), radius = sutterR, center = Offset(sutterX, sutterY))
                        drawCircle(color = Color(0xFFBED7A6), radius = sutterR * 0.62f, center = Offset(sutterX, sutterY))
                        drawCircle(color = Color(0xFF8BA674), radius = sutterR * 0.30f, center = Offset(sutterX, sutterY))

                        // 6. Hydrography & Waterways (Vibrant River Blue #0284C7)
                        val waterColor = Color(0xFF0284C7)
                        val waterGlow = Color(0xFF38BDF8).copy(alpha = 0.35f)
                        val waterStrokeWidth = (2.2f * zoomScale).coerceIn(1.8f, 5.0f)

                        // Delta Channels (Southwest)
                        val delta1 = Path().apply {
                            moveTo(toScreenX(-121.90), toScreenY(38.10))
                            lineTo(toScreenX(-121.72), toScreenY(38.18))
                            lineTo(toScreenX(-121.65), toScreenY(38.28))
                        }
                        val delta2 = Path().apply {
                            moveTo(toScreenX(-121.95), toScreenY(38.15))
                            lineTo(toScreenX(-121.78), toScreenY(38.22))
                            lineTo(toScreenX(-121.68), toScreenY(38.35))
                        }
                        drawPath(delta1, color = waterColor.copy(alpha = 0.7f), style = Stroke(width = waterStrokeWidth * 1.2f, cap = StrokeCap.Round))
                        drawPath(delta2, color = waterColor.copy(alpha = 0.6f), style = Stroke(width = waterStrokeWidth, cap = StrokeCap.Round))

                        // Folsom Lake Reservoir
                        val folsomLakePath = Path().apply {
                            moveTo(toScreenX(-121.17), toScreenY(38.70))
                            lineTo(toScreenX(-121.14), toScreenY(38.74))
                            lineTo(toScreenX(-121.10), toScreenY(38.78))
                            lineTo(toScreenX(-121.07), toScreenY(38.76))
                            lineTo(toScreenX(-121.10), toScreenY(38.73))
                            lineTo(toScreenX(-121.05), toScreenY(38.70))
                            lineTo(toScreenX(-121.11), toScreenY(38.67))
                            lineTo(toScreenX(-121.16), toScreenY(38.69))
                            close()
                        }
                        drawPath(folsomLakePath, color = waterColor)
                        drawPath(folsomLakePath, color = Color(0xFF0369A1), style = Stroke(width = 1.2f))

                        // Feather River
                        val featherRiverPath = Path().apply {
                            moveTo(toScreenX(-121.58), farTop)
                            lineTo(toScreenX(-121.60), toScreenY(39.08))
                            lineTo(toScreenX(-121.62), toScreenY(38.80))
                        }
                        drawPath(featherRiverPath, color = waterColor, style = Stroke(width = waterStrokeWidth * 0.9f, cap = StrokeCap.Round))

                        // American River (Flows from Sierra -> Folsom Lake -> Confluence at Sacramento)
                        val americanRiverPath = Path().apply {
                            moveTo(toScreenX(-120.90), toScreenY(38.90))
                            lineTo(toScreenX(-121.15), toScreenY(38.71))
                            lineTo(toScreenX(-121.28), toScreenY(38.65))
                            lineTo(toScreenX(-121.40), toScreenY(38.60))
                            lineTo(toScreenX(-121.51), toScreenY(38.59))
                        }
                        drawPath(americanRiverPath, color = waterGlow, style = Stroke(width = waterStrokeWidth * 1.6f, cap = StrokeCap.Round))
                        drawPath(americanRiverPath, color = waterColor, style = Stroke(width = waterStrokeWidth, cap = StrokeCap.Round))

                        // Sacramento River (Main valley arterial waterway)
                        val sacramentoRiverPath = Path().apply {
                            moveTo(toScreenX(-122.05), farTop)
                            lineTo(toScreenX(-121.90), toScreenY(39.25))
                            lineTo(toScreenX(-121.75), toScreenY(38.95))
                            lineTo(toScreenX(-121.62), toScreenY(38.80))
                            lineTo(toScreenX(-121.51), toScreenY(38.59))
                            lineTo(toScreenX(-121.55), toScreenY(38.45))
                            lineTo(toScreenX(-121.65), toScreenY(38.28))
                            lineTo(toScreenX(-121.75), farBottom)
                        }
                        drawPath(sacramentoRiverPath, color = waterGlow, style = Stroke(width = waterStrokeWidth * 1.8f, cap = StrokeCap.Round))
                        drawPath(sacramentoRiverPath, color = waterColor, style = Stroke(width = waterStrokeWidth * 1.2f, cap = StrokeCap.Round))

                        // 7. Subtle Geographic Coordinate Grid Lines
                        val gridColor = Color(0xFF64748B).copy(alpha = 0.12f)
                        val latsToDraw = listOf(38.2, 38.4, 38.6, 38.8, 39.0, 39.2, 39.4)
                        val lonsToDraw = listOf(-122.2, -122.0, -121.8, -121.6, -121.4, -121.2, -121.0, -120.8, -120.6, -120.4)

                        latsToDraw.forEach { latVal ->
                            val y = toScreenY(latVal)
                            if (y >= -100f && y <= canvasHeight + 100f) {
                                drawLine(color = gridColor, start = Offset(0f, y), end = Offset(canvasWidth, y), strokeWidth = 1f)
                            }
                        }
                        lonsToDraw.forEach { lonVal ->
                            val x = toScreenX(lonVal)
                            if (x >= -100f && x <= canvasWidth + 100f) {
                                drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, canvasHeight), strokeWidth = 1f)
                            }
                        }

                        // 8. Render Stations with Pan and Zoom Offsets
                        val textPaint = Paint().apply {
                            color = android.graphics.Color.parseColor("#0F172A")
                            textSize = (20f * zoomScale).coerceIn(16f, 26f)
                            isAntiAlias = true
                            isFakeBoldText = true
                        }

                        val subTextPaint = Paint().apply {
                            color = android.graphics.Color.parseColor("#475569")
                            textSize = (15f * zoomScale).coerceIn(12f, 20f)
                            isAntiAlias = true
                        }

                        stations.forEach { station ->
                            val pinX = toScreenX(station.longitude)
                            val pinY = toScreenY(station.latitude)

                            // Only render station pins currently inside or near the viewport
                            if (pinX >= -80f && pinX <= canvasWidth + 80f && pinY >= -80f && pinY <= canvasHeight + 80f) {
                                val isSelected = station.id == selectedStation?.id
                                val baseRadius = if (isSelected) 9.dp.toPx() else 6.5.dp.toPx()
                                val scaledRadius = (baseRadius * zoomScale).coerceIn(5.dp.toPx(), 18.dp.toPx())

                                val pinColor = when {
                                    station.elevationMeters < 30 -> Color(0xFF0284C7)   // Coastal/Sea level (Sky Blue)
                                    station.elevationMeters < 100 -> Color(0xFFF59E0B)  // Valley Floor (Amber)
                                    station.elevationMeters < 400 -> Color(0xFF10B981)  // Foothills (Emerald)
                                    station.elevationMeters < 900 -> Color(0xFF3B82F6)  // Low Montane (Blue)
                                    else -> Color(0xFFA855F7)                           // High Ridge (Purple)
                                }

                                // Selection Highlight Pulse Ring
                                if (isSelected) {
                                    drawCircle(
                                        color = pinColor.copy(alpha = 0.25f),
                                        radius = scaledRadius * 2.4f,
                                        center = Offset(pinX, pinY)
                                    )
                                    drawCircle(
                                        color = pinColor.copy(alpha = 0.5f),
                                        radius = scaledRadius * 1.6f,
                                        center = Offset(pinX, pinY),
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }

                                // White Pin Border for crisp contrast over any topo relief
                                drawCircle(
                                    color = Color.White,
                                    radius = scaledRadius + (2.dp.toPx() * zoomScale).coerceIn(1.5f, 3.5f),
                                    center = Offset(pinX, pinY)
                                )

                                // Solid Pin Core
                                drawCircle(
                                    color = pinColor,
                                    radius = scaledRadius,
                                    center = Offset(pinX, pinY)
                                )

                                // Station Label Text
                                drawContext.canvas.nativeCanvas.drawText(
                                    station.name,
                                    pinX + scaledRadius + 6f,
                                    pinY - 2f,
                                    textPaint
                                )

                                drawContext.canvas.nativeCanvas.drawText(
                                    "${station.elevationMeters}m • ${station.currentReading.temperatureCelsius.roundToInt()}°C",
                                    pinX + scaledRadius + 6f,
                                    pinY + textPaint.textSize,
                                    subTextPaint
                                )
                            }
                        }
                    }
                }

                // 4. Floating Zoom Widget in Bottom-Right Corner (Clean Light Theme)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(42.dp)
                    ) {
                        // Zoom In Button (+)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(width = 42.dp, height = 38.dp)
                                .clickable {
                                    zoomScale = (zoomScale * 1.25f).coerceAtMost(3.5f)
                                }
                        ) {
                            Text(
                                text = "+",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        // Divider with current zoom level readout
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFE2E8F0))
                        )

                        Text(
                            text = "${(zoomScale * 10).roundToInt() / 10f}x",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF0284C7),
                            modifier = Modifier.padding(vertical = 3.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFE2E8F0))
                        )

                        // Zoom Out Button (−)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(width = 42.dp, height = 38.dp)
                                .clickable {
                                    zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.6f)
                                }
                        ) {
                            Text(
                                text = "−",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }

                // 5. Floating Drag & Pinch Tip Pill in Bottom Left (Clean Light Theme)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.92f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "✥ Pan • Pinch to zoom",
                        color = Color(0xFF475569),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 6. Selected Station Inspector Card
        selectedStation?.let { st ->
            StationCard(
                station = st,
                isSelected = true,
                useMockReadings = true // demo screen: keeps the sample numbers
            )
        }
    }
}
