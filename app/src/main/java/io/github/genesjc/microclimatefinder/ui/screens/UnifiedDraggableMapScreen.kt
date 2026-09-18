package io.github.genesjc.microclimatefinder.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
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
import io.github.genesjc.microclimatefinder.data.local.StationCsvParser
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.ui.components.StationCard
import kotlin.math.roundToInt

/**
 * Unified Draggable Topo Map (v0.15.0 - Macro Central California Transect)
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
 */
@Composable
fun UnifiedDraggableMapScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val parser = remember { StationCsvParser(context) }
    val stations = remember {
        try {
            val unified = parser.loadStationsFromCsv("california_unified_stations.csv")
            if (unified.isNotEmpty()) unified else parser.loadStationsFromCsv("sacramento_stations_60mi.csv")
        } catch (e: Exception) {
            parser.loadStationsFromCsv("sacramento_stations_60mi.csv")
        }
    }

    var selectedStation by remember {
        mutableStateOf(stations.firstOrNull { it.id == "KMRY" } ?: stations.firstOrNull { it.id == "KSAC" } ?: stations.firstOrNull())
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
                        text = "California Transect Map",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Sacramento to Monterey Bay • 36 Stations",
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
                    text = "v0.15.0",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D4ED8),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Interactive Navigation HUD: Pan Offset, Zoom Level, Recenter + Quick Region Anchors
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // Top HUD row: Coordinates & Reset button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PAN: ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "ΔX=${panX.roundToInt()}px, ΔY=${panY.roundToInt()}px • ${(zoomScale * 10).roundToInt() / 10f}x",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0284C7)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0284C7),
                        modifier = Modifier.clickable {
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
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick-Jump Region Anchor Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "JUMP:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Sacramento Basin Anchor
                    RegionAnchorChip(
                        label = "📍 Sacramento",
                        isActive = panY < -150f,
                        onClick = {
                            panX = 10f
                            panY = -340f
                            zoomScale = 1.35f
                        }
                    )

                    // Delta / Bay Gateway Anchor
                    RegionAnchorChip(
                        label = "🌉 Bay & Delta",
                        isActive = panY in -150f..100f && zoomScale > 1.1f,
                        onClick = {
                            panX = -20f
                            panY = 0f
                            zoomScale = 1.3f
                        }
                    )

                    // Monterey Bay Anchor
                    RegionAnchorChip(
                        label = "🌊 Monterey Bay",
                        isActive = panY > 100f,
                        onClick = {
                            panX = -35f
                            panY = 320f
                            zoomScale = 1.4f
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Main Draggable & Zoomable Canvas Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
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

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
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

                                        panX += panDelta.x
                                        panY += panDelta.y
                                        zoomScale = (zoomScale * zoomDelta).coerceIn(0.5f, 4.0f)

                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
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
                        // 1. Base Continental Valley Landmass (Light fertile sage)
                        drawRect(color = Color(0xFFF1F6EC))

                        // 2. Pacific Ocean (Western Coastal Waters)
                        val oceanPath = Path().apply {
                            moveTo(-200f, -200f)
                            lineTo(toScreenX(-122.55), -200f)
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
                            lineTo(-200f, toScreenY(35.50))
                            close()
                        }
                        drawPath(path = oceanPath, color = Color(0xFFE0F2FE))
                        drawPath(path = oceanPath, color = Color(0xFF0284C7).copy(alpha = 0.45f), style = Stroke(width = 2.2f))

                        // 3. Santa Lucia Range & Big Sur Mountains (South-West Coast)
                        val santaLuciaPath = Path().apply {
                            moveTo(toScreenX(-121.90), toScreenY(36.65))
                            lineTo(toScreenX(-121.50), toScreenY(36.25)) // Chews Ridge (1,538m)
                            lineTo(toScreenX(-121.30), toScreenY(35.70))
                            lineTo(toScreenX(-121.05), toScreenY(35.50))
                            lineTo(toScreenX(-121.35), toScreenY(35.50))
                            lineTo(toScreenX(-121.75), toScreenY(36.20))
                            close()
                        }
                        drawPath(path = santaLuciaPath, color = Color(0xFFBED7A6))

                        // 4. Salinas Valley Thermal Trough (Between Santa Lucia & Gabilan Ranges)
                        val salinasValleyPath = Path().apply {
                            moveTo(toScreenX(-121.80), toScreenY(36.80)) // Moss Landing Gate
                            lineTo(toScreenX(-121.65), toScreenY(36.68)) // Salinas
                            lineTo(toScreenX(-121.40), toScreenY(36.35)) // Soledad / King City
                            lineTo(toScreenX(-120.70), toScreenY(35.65)) // Paso Robles
                            lineTo(toScreenX(-120.85), toScreenY(35.65))
                            lineTo(toScreenX(-121.50), toScreenY(36.30))
                            close()
                        }
                        drawPath(path = salinasValleyPath, color = Color(0xFFEDF5E7))

                        // 5. Diablo Range & Mount Hamilton (1,280m) in Central Transect
                        val diabloRangePath = Path().apply {
                            moveTo(toScreenX(-121.80), toScreenY(37.90))
                            lineTo(toScreenX(-121.64), toScreenY(37.34)) // Mount Hamilton
                            lineTo(toScreenX(-121.30), toScreenY(36.80)) // Pinnacles & Gabilan
                            lineTo(toScreenX(-121.15), toScreenY(36.45))
                            lineTo(toScreenX(-121.40), toScreenY(36.50))
                            lineTo(toScreenX(-121.85), toScreenY(37.40))
                            close()
                        }
                        drawPath(path = diabloRangePath, color = Color(0xFFD7E5C8))

                        // 6. Sierra Nevada Elevation Relief Bands (Rising eastward in Northern/Central CA)
                        val sierraFoothills = Path().apply {
                            moveTo(toScreenX(-121.36), toScreenY(39.50))
                            lineTo(toScreenX(-121.30), toScreenY(39.10))
                            lineTo(toScreenX(-121.15), toScreenY(38.80))
                            lineTo(toScreenX(-120.95), toScreenY(38.40))
                            lineTo(toScreenX(-120.75), toScreenY(38.00))
                            lineTo(toScreenX(-120.40), toScreenY(37.50))
                            lineTo(toScreenX(-120.10), toScreenY(37.50))
                            lineTo(toScreenX(-120.10), toScreenY(39.50))
                            close()
                        }
                        drawPath(path = sierraFoothills, color = Color(0xFFDFE9D0))

                        val sierraHighRidge = Path().apply {
                            moveTo(toScreenX(-121.00), toScreenY(39.50))
                            lineTo(toScreenX(-120.85), toScreenY(39.15))
                            lineTo(toScreenX(-120.70), toScreenY(38.85))
                            lineTo(toScreenX(-120.50), toScreenY(38.40))
                            lineTo(toScreenX(-120.30), toScreenY(38.00))
                            lineTo(toScreenX(-120.00), toScreenY(38.00))
                            lineTo(toScreenX(-120.00), toScreenY(39.50))
                            close()
                        }
                        drawPath(path = sierraHighRidge, color = Color(0xFFAECFA0))

                        // 7. Waterways: Sacramento River, American River, Delta, Salinas River
                        val waterColor = Color(0xFF0284C7)
                        val waterGlow = Color(0xFF38BDF8).copy(alpha = 0.35f)

                        // Sacramento River
                        val sacRiver = Path().apply {
                            moveTo(toScreenX(-121.90), toScreenY(39.40))
                            lineTo(toScreenX(-121.75), toScreenY(39.00))
                            lineTo(toScreenX(-121.61), toScreenY(38.70))
                            lineTo(toScreenX(-121.51), toScreenY(38.59))
                            lineTo(toScreenX(-121.52), toScreenY(38.50))
                            lineTo(toScreenX(-121.75), toScreenY(38.15)) // Into Delta
                            lineTo(toScreenX(-122.10), toScreenY(38.05)) // Carquinez Strait
                        }
                        drawPath(sacRiver, color = waterGlow, style = Stroke(width = 4.2f, cap = StrokeCap.Round))
                        drawPath(sacRiver, color = waterColor, style = Stroke(width = 2.4f, cap = StrokeCap.Round))

                        // American River & Folsom Lake
                        val americanRiver = Path().apply {
                            moveTo(toScreenX(-120.95), toScreenY(38.85))
                            lineTo(toScreenX(-121.15), toScreenY(38.70))
                            lineTo(toScreenX(-121.51), toScreenY(38.59))
                        }
                        drawPath(americanRiver, color = waterColor, style = Stroke(width = 2.2f, cap = StrokeCap.Round))

                        // San Joaquin River & Stockton Channel
                        val sanJoaquin = Path().apply {
                            moveTo(toScreenX(-121.20), toScreenY(37.75))
                            lineTo(toScreenX(-121.32), toScreenY(37.95)) // Stockton
                            lineTo(toScreenX(-121.70), toScreenY(38.10)) // Confluence
                        }
                        drawPath(sanJoaquin, color = waterColor, style = Stroke(width = 2.2f, cap = StrokeCap.Round))

                        // Salinas River (South into Monterey Bay)
                        val salinasRiver = Path().apply {
                            moveTo(toScreenX(-120.70), toScreenY(35.65)) // Paso Robles
                            lineTo(toScreenX(-121.15), toScreenY(36.15))
                            lineTo(toScreenX(-121.45), toScreenY(36.45))
                            lineTo(toScreenX(-121.65), toScreenY(36.68)) // Salinas
                            lineTo(toScreenX(-121.80), toScreenY(36.78)) // Monterey Bay estuary
                        }
                        drawPath(salinasRiver, color = waterGlow, style = Stroke(width = 3.6f, cap = StrokeCap.Round))
                        drawPath(salinasRiver, color = waterColor, style = Stroke(width = 2.0f, cap = StrokeCap.Round))

                        // 8. Geographic Reference Grid (36°N, 37°N, 38°N, 39°N)
                        val gridColor = Color(0xFF64748B).copy(alpha = 0.12f)
                        val gridPaint = Paint().apply {
                            color = android.graphics.Color.parseColor("#94A3B8")
                            textSize = 24f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            isAntiAlias = true
                        }

                        for (lat in listOf(36.0, 37.0, 38.0, 39.0)) {
                            val y = toScreenY(lat)
                            drawLine(color = gridColor, start = Offset(0f, y), end = Offset(canvasWidth, y), strokeWidth = 1.2f)
                            drawContext.canvas.nativeCanvas.drawText("${lat.toInt()}°N", 16f, y - 6f, gridPaint)
                        }

                        // 9. Station Pins & Labels
                        val textPaint = Paint().apply {
                            color = android.graphics.Color.parseColor("#0F172A")
                            textSize = 28f * zoomScale.coerceIn(0.7f, 1.4f)
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            isAntiAlias = true
                        }

                        val subTextPaint = Paint().apply {
                            color = android.graphics.Color.parseColor("#475569")
                            textSize = 22f * zoomScale.coerceIn(0.7f, 1.3f)
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                            isAntiAlias = true
                        }

                        stations.forEach { station ->
                            val pinX = toScreenX(station.longitude)
                            val pinY = toScreenY(station.latitude)

                            // Render if visible within reasonable screen bounds
                            if (pinX >= -60f && pinX <= canvasWidth + 60f && pinY >= -60f && pinY <= canvasHeight + 60f) {
                                val isSelected = station.id == selectedStation?.id

                                val pinColor = when {
                                    station.elevationMeters < 30 -> Color(0xFF0284C7)   // Coastal/Sea level (Sky Blue)
                                    station.elevationMeters < 100 -> Color(0xFFF59E0B)  // Valley Floor (Amber)
                                    station.elevationMeters < 400 -> Color(0xFF10B981)  // Foothills (Emerald)
                                    station.elevationMeters < 900 -> Color(0xFF3B82F6)  // Low Montane (Blue)
                                    else -> Color(0xFF7C3AED)                           // Alpine/Ridge (Purple)
                                }

                                val baseRadius = if (isSelected) 10f else 6.5f
                                val scaledRadius = baseRadius * zoomScale.coerceIn(0.7f, 1.5f)

                                // Selection Pulse Halo
                                if (isSelected) {
                                    drawCircle(
                                        color = pinColor.copy(alpha = 0.25f),
                                        radius = scaledRadius * 2.8f,
                                        center = Offset(pinX, pinY)
                                    )
                                    drawCircle(
                                        color = pinColor.copy(alpha = 0.45f),
                                        radius = scaledRadius * 1.8f,
                                        center = Offset(pinX, pinY)
                                    )
                                }

                                // Pin Core
                                drawCircle(
                                    color = pinColor,
                                    radius = scaledRadius,
                                    center = Offset(pinX, pinY)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = scaledRadius * 0.45f,
                                    center = Offset(pinX, pinY)
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.9f),
                                    radius = scaledRadius,
                                    center = Offset(pinX, pinY),
                                    style = Stroke(width = 1.8f)
                                )

                                // Station Label & Details
                                if (zoomScale >= 0.85f || isSelected) {
                                    drawContext.canvas.nativeCanvas.drawText(
                                        station.name,
                                        pinX + scaledRadius + 8f,
                                        pinY - 2f,
                                        textPaint
                                    )

                                    drawContext.canvas.nativeCanvas.drawText(
                                        "${station.elevationMeters}m • ${station.currentReading.temperatureCelsius.roundToInt()}°C",
                                        pinX + scaledRadius + 8f,
                                        pinY + textPaint.textSize,
                                        subTextPaint
                                    )
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
                                    zoomScale = (zoomScale * 1.25f).coerceAtMost(4.0f)
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

                        // Current Zoom Multiplier Display
                        Text(
                            text = "${(zoomScale * 10).roundToInt() / 10f}x",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Zoom Out Button (−)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .size(38.dp)
                                .clickable {
                                    zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.5f)
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

                // 5. Visual Gestural Hint Pill in Bottom-Left Corner
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 2.dp,
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
                    }
                }
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
                StationCard(
                    station = st,
                    isSelected = true
                )
            }
        }
    }
}

@Composable
private fun RegionAnchorChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) Color(0xFFE0F2FE) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(
            1.dp,
            if (isActive) Color(0xFF0284C7) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) Color(0xFF0369A1) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
