package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import io.github.genesjc.microclimatefinder.data.local.LocalNewsRepository
import io.github.genesjc.microclimatefinder.data.local.StationCsvParser
import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.domain.model.TerrainType
import io.github.genesjc.microclimatefinder.ui.components.LegendItem
import io.github.genesjc.microclimatefinder.ui.components.StationCard

private data class LandmarkCity(
    val id: String,
    val displayName: String,
    val yOffsetDp: Int = -26,
    val xOffsetDp: Int = 0
)

private const val LANDMARK_COUNT = 6

// Rough chart width on a phone, used only to keep labels inside the chart
private const val CHART_WIDTH_DP_ESTIMATE = 300f

/**
 * Picks stations spread evenly by elevation and labels each with its place name and elevation.
 * Airport stations are preferred near each target because they are recognizable cities. Labels are
 * placed left to right on up to three heights, and a label is skipped when it would overlap one
 * already placed at the same height.
 */
private fun buildLandmarks(sorted: List<MicroclimateStation>): List<LandmarkCity> {
    if (sorted.isEmpty()) return emptyList()
    val candidateCount = minOf(LANDMARK_COUNT + 3, sorted.size)
    val maxElev = sorted.last().elevationMeters.coerceAtLeast(1)
    val chosen = mutableListOf<Int>()
    for (i in 0 until candidateCount) {
        val target = if (candidateCount == 1) 0 else ((i * (sorted.size - 1).toFloat() / (candidateCount - 1)) + 0.5f).toInt()
        val window = (target - 2..target + 2).filter { it in sorted.indices && it !in chosen }
        val pick = window
            .filter { sorted[it].id.startsWith("K") }
            .minByOrNull { kotlin.math.abs(it - target) }
            ?: window.minByOrNull { kotlin.math.abs(it - target) }
            ?: target
        chosen.add(pick)
    }

    val placed = mutableListOf<Triple<Float, Float, Int>>() // left, right, height level
    val result = mutableListOf<LandmarkCity>()
    for (idx in chosen.distinct().sorted()) {
        if (result.size >= LANDMARK_COUNT) break
        val station = sorted[idx]
        val ratioX = idx.toFloat() / (sorted.size - 1).coerceAtLeast(1)
        val elevRatio = station.elevationMeters.toFloat() / maxElev
        val city = LocalNewsRepository.getCleanCityName(station.id, station.name)
        val label = "$city ${"%,d".format(station.elevationMeters)}m"
        // Estimated label width at 9sp; labels that would pass the chart's right edge end at the pin instead
        val labelWidthDp = label.length * 5.4f + 10f
        val xOffset = when {
            ratioX * CHART_WIDTH_DP_ESTIMATE + labelWidthDp - 24f > CHART_WIDTH_DP_ESTIMATE * 0.95f ->
                (24f - labelWidthDp).toInt()
            ratioX < 0.12f -> 4
            else -> 0
        }
        val left = ratioX * CHART_WIDTH_DP_ESTIMATE + xOffset - 24f
        val right = left + labelWidthDp
        // High pins stay on the lowest height so they remain inside the chart
        val maxLevel = if (elevRatio > 0.8f) 0 else 2
        val level = (0..maxLevel).firstOrNull { lv ->
            placed.none { it.third == lv && left < it.second && right > it.first }
        } ?: continue
        placed.add(Triple(left, right, level))
        result.add(
            LandmarkCity(
                id = station.id,
                displayName = label,
                yOffsetDp = -26 - 16 * level,
                xOffsetDp = xOffset
            )
        )
    }
    return result
}

@Composable
fun ElevationProfileScreen(
    dataset: StationDatasetInfo,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val parser = remember(dataset.fileName) { StationCsvParser(context) }
    val stations = remember(dataset) { parser.loadStations(dataset) }

    var selectedStationId by remember {
        mutableStateOf(stations.firstOrNull()?.id)
    }

    val selectedStation = stations.find { it.id == selectedStationId } ?: stations.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
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
                onSelectStation = { selectedStationId = it.id }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = Color(0xFF7C3AED), label = "Ridge (>500m)")
            LegendItem(color = Color(0xFFEA580C), label = "Valley Basin")
            LegendItem(color = Color(0xFF0284C7), label = "Delta / Coast")
            LegendItem(color = Color(0xFF166534), label = "Mountain Relief")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Station Inspector Card
        Text(
            text = "Station Inspector",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedStation != null) {
            StationCard(
                station = selectedStation,
                isSelected = true,
                onSelect = { /* Already selected */ }
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
    val landmarks = remember(sorted) { buildLandmarks(sorted) }

    val currentStation = sorted.find { it.id == selectedStationId } ?: sorted.firstOrNull()
    val tempF = currentStation?.currentReading?.temperatureFahrenheit ?: 85.0

    val skyTheme = remember(tempF) {
        when {
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthDp = maxWidth
        val heightDp = maxHeight
        val density = LocalDensity.current

        val paddingPx = with(density) { 32.dp.toPx() }
        val widthPx = with(density) { widthDp.toPx() } - (paddingPx * 2)
        val heightPx = with(density) { heightDp.toPx() } - (paddingPx * 2)

        // Canvas drawing the dynamic sky, slope, shaded terrain, leader lines, and nodes
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(sorted) {
                    detectTapGestures { tapOffset ->
                        val padding = 32f
                        val w = size.width - (padding * 2)
                        val h = size.height - (padding * 2)

                        var closestStation: MicroclimateStation? = null
                        var closestDist = Float.MAX_VALUE

                        sorted.forEachIndexed { index, station ->
                            val x = padding + (index.toFloat() / (sorted.size - 1).coerceAtLeast(1)) * w
                            val elevRatio = station.elevationMeters / maxElevation
                            val y = padding + (1f - elevRatio) * h

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
            val padding = 32f
            val w = size.width - (padding * 2)
            val h = size.height - (padding * 2)

            // 1. DYNAMIC SKY GRADIENT FILL: fills the open space above & to the left of the slope
            val skyPath = Path().apply {
                moveTo(padding, padding)
                lineTo(size.width - padding, padding)
                val lastElevRatio = (sorted.lastOrNull()?.elevationMeters ?: 0) / maxElevation
                lineTo(size.width - padding, padding + (1f - lastElevRatio) * h)
                for (i in (sorted.size - 1) downTo 0) {
                    val st = sorted[i]
                    val x = padding + (i.toFloat() / (sorted.size - 1).coerceAtLeast(1)) * w
                    val elevRatio = st.elevationMeters / maxElevation
                    val y = padding + (1f - elevRatio) * h
                    lineTo(x, y)
                }
                close()
            }

            drawPath(
                path = skyPath,
                brush = Brush.verticalGradient(
                    colors = skyTheme.colors,
                    startY = padding,
                    endY = padding + h * 0.85f
                )
            )

            // 2. DYNAMIC WEATHER VISUALS (Clouds / Sun corona in the upper-left quadrant)
            if (tempF >= 84.0) {
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
                val x = padding + (index.toFloat() / (sorted.size - 1).coerceAtLeast(1)) * w
                val elevRatio = station.elevationMeters / maxElevation
                val y = padding + (1f - elevRatio) * h

                if (index == 0) {
                    terrainPath.moveTo(x, size.height - padding)
                    terrainPath.lineTo(x, y)
                    linePath.moveTo(x, y)
                } else {
                    terrainPath.lineTo(x, y)
                    linePath.lineTo(x, y)
                }
            }
            terrainPath.lineTo(size.width - padding, size.height - padding)
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
                val x = padding + (index.toFloat() / (sorted.size - 1).coerceAtLeast(1)) * w
                val elevRatio = station.elevationMeters / maxElevation
                val y = padding + (1f - elevRatio) * h
                val isSelected = station.id == selectedStationId

                val isLandmark = landmarks.any { it.id == station.id }

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
                        radius = 18f,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = skyTheme.accent,
                        radius = 14f,
                        center = Offset(x, y),
                        style = Stroke(width = 2.5f)
                    )
                }

                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 8f else (if (isLandmark) 6.5f else 4.5f),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = if (isSelected) skyTheme.accent else pinColor,
                    radius = if (isSelected) 6f else (if (isLandmark) 4.8f else 3.2f),
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
                    if (landmarks.isNotEmpty()) {
                        val currentIndex = landmarks.indexOfFirst { it.id == currentStation?.id }
                        val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % landmarks.size else 0
                        val nextId = landmarks[nextIndex].id
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
                            text = "${tempF.toInt()}°F",
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
        sorted.forEachIndexed { index, station ->
            val landmark = landmarks.find { it.id == station.id }
            if (landmark != null) {
                val ratioX = index.toFloat() / (sorted.size - 1).coerceAtLeast(1)
                val elevRatio = station.elevationMeters / maxElevation

                val xDp = with(density) {
                    (paddingPx + ratioX * widthPx).toDp()
                } + landmark.xOffsetDp.dp

                val yDp = with(density) {
                    (paddingPx + (1f - elevRatio) * heightPx).toDp()
                } + landmark.yOffsetDp.dp

                val isSelected = station.id == selectedStationId

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
                        .offset(x = xDp - 24.dp, y = yDp)
                        .clickable { onSelectStation(station) }
                ) {
                    Text(
                        text = landmark.displayName,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
