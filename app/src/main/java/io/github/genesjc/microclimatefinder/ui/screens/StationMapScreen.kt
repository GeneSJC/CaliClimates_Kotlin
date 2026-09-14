package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
import io.github.genesjc.microclimatefinder.data.local.StationCsvParser
import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.data.local.StationDatasets
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.domain.model.TerrainType
import io.github.genesjc.microclimatefinder.ui.components.StationCard

enum class MapVisualizationMode(val label: String) {
    SPATIAL_GRID("Spatial Map"),
    ELEVATION_PROFILE("Elevation Profile")
}

data class LandmarkCity(
    val id: String,
    val displayName: String,
    val yOffsetDp: Int = -26,
    val xOffsetDp: Int = 0
)

val ELEVATION_LANDMARKS = listOf(
    LandmarkCity("KSAC", "Sacramento 5m", yOffsetDp = -24, xOffsetDp = 4),
    LandmarkCity("KLHM", "Lincoln 37m", yOffsetDp = -24, xOffsetDp = 0),
    LandmarkCity("KAUN", "Auburn 453m", yOffsetDp = -26, xOffsetDp = -10),
    LandmarkCity("KGOO", "Grass Valley 961m", yOffsetDp = -26, xOffsetDp = -20),
    LandmarkCity("BDMC1", "Bald Mtn 1,406m", yOffsetDp = -28, xOffsetDp = -26)
)

@Composable
fun StationMapScreen(
    dataset: StationDatasetInfo = StationDatasets.DEFAULT_DATASET,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val parser = remember(dataset.assetFileName) { StationCsvParser(context) }
    val stations = remember(dataset.assetFileName) { parser.loadStationsFromCsv(dataset.assetFileName) }

    var selectedStationId by remember {
        mutableStateOf(stations.firstOrNull()?.id)
    }
    var activeMode by remember { mutableStateOf(MapVisualizationMode.SPATIAL_GRID) }
    var pinScale by remember { mutableStateOf(1.0f) }

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
                    text = "Topographical Map",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${dataset.title} • 60-Mile Grid",
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

        // Mode Switcher: Spatial Map vs. Elevation Profile
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapVisualizationMode.values().forEach { mode ->
                val isSelected = activeMode == mode
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeMode = mode }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = mode.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Map / Elevation Canvas Container
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            if (activeMode == MapVisualizationMode.SPATIAL_GRID) {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Left 85%: Map Canvas
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        SpatialMapCanvas(
                            stations = stations,
                            selectedStationId = selectedStationId,
                            pinScale = pinScale,
                            onSelectStation = { selectedStationId = it.id }
                        )
                    }

                    // Right 15%: Vertical Dot Size Slider
                    VerticalPinSizeSlider(
                        value = pinScale,
                        onValueChange = { pinScale = it },
                        modifier = Modifier
                            .width(50.dp)
                            .fillMaxHeight()
                    )
                }
            } else {
                ElevationProfileCanvas(
                    stations = stations,
                    selectedStationId = selectedStationId,
                    onSelectStation = { selectedStationId = it.id }
                )
            }
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
                        text = "Tap any station pin on the map to inspect microclimate data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalPinSizeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    // Widget handle: Rounded-square with width increased by 80% (32.dp width x 18.dp height)
    val thumbShape = RoundedCornerShape(4.dp)
    val thumbWidth = 32.dp
    val thumbHeight = 18.dp

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Indicator: Large Dot icon (tap to set max 2.5x)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(13.dp)
                    .clickable { currentOnValueChange(2.5f) }
            ) {}

            Text(
                text = "SIZE",
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Draggable Track Area
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .width(36.dp)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                val trackHeight = maxHeight
                val normalizedRatio = ((value - 1.0f) / 1.5f).coerceIn(0f, 1f)

                val thumbHeightPx = with(density) { thumbHeight.toPx() }
                val trackHeightPx = with(density) { trackHeight.toPx() }
                val usableHeightPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)

                // Track Background
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(5.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(3.dp)
                        )
                )

                // Active Filled Track (from bottom up)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .height(trackHeight * normalizedRatio)
                        .width(5.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(3.dp)
                        )
                )

                // Draggable Slider Thumb: Square with rounded corners, width increased by 80% (32.dp x 18.dp)
                val thumbOffset = (trackHeight - thumbHeight) * (1f - normalizedRatio)

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = thumbOffset)
                        .size(width = thumbWidth, height = thumbHeight)
                        .shadow(3.dp, thumbShape)
                        .background(MaterialTheme.colorScheme.primary, thumbShape)
                        .border(2.dp, Color.White, thumbShape)
                )

                // Full-height Touch & Drag Area overlay covering the entire rail
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(trackHeightPx) {
                            detectTapGestures { tapOffset ->
                                val relativeY = (tapOffset.y - thumbHeightPx / 2f).coerceIn(0f, usableHeightPx)
                                val ratio = 1f - (relativeY / usableHeightPx)
                                val newScale = (1.0f + ratio * 1.5f).coerceIn(1.0f, 2.5f)
                                currentOnValueChange((newScale * 10f).roundToInt() / 10f)
                            }
                        }
                        .pointerInput(trackHeightPx) {
                            detectVerticalDragGestures(
                                onDragStart = { startOffset ->
                                    val relativeY = (startOffset.y - thumbHeightPx / 2f).coerceIn(0f, usableHeightPx)
                                    val ratio = 1f - (relativeY / usableHeightPx)
                                    val newScale = (1.0f + ratio * 1.5f).coerceIn(1.0f, 2.5f)
                                    currentOnValueChange((newScale * 10f).roundToInt() / 10f)
                                },
                                onVerticalDrag = { change, _ ->
                                    change.consume()
                                    val touchY = change.position.y
                                    val relativeY = (touchY - thumbHeightPx / 2f).coerceIn(0f, usableHeightPx)
                                    val ratio = 1f - (relativeY / usableHeightPx)
                                    val newScale = (1.0f + ratio * 1.5f).coerceIn(1.0f, 2.5f)
                                    currentOnValueChange((newScale * 10f).roundToInt() / 10f)
                                }
                            )
                        }
                )
            }

            // Bottom Indicator: Small Dot icon (tap to set min 1.0x)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(5.dp)
                    .clickable { currentOnValueChange(1.0f) }
            ) {}

            Text(
                text = "${(value * 10).toInt() / 10f}x",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(8.dp)
        ) {}
        Text(
            text = " $label",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SpatialMapCanvas(
    stations: List<MicroclimateStation>,
    selectedStationId: String?,
    pinScale: Float = 1.0f,
    onSelectStation: (MicroclimateStation) -> Unit
) {
    if (stations.isEmpty()) return

    // Calculate dynamic geographic bounding box
    val minLat = remember(stations) { stations.minOf { it.latitude } }
    val maxLat = remember(stations) { stations.maxOf { it.latitude } }
    val minLon = remember(stations) { stations.minOf { it.longitude } }
    val maxLon = remember(stations) { stations.maxOf { it.longitude } }

    val latSpan = (maxLat - minLat).coerceAtLeast(0.01)
    val lonSpan = (maxLon - minLon).coerceAtLeast(0.01)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(stations, pinScale) {
                detectTapGestures { tapOffset ->
                    val padding = 28f
                    val w = size.width - (padding * 2)
                    val h = size.height - (padding * 2)

                    var closestStation: MicroclimateStation? = null
                    var closestDist = Float.MAX_VALUE

                    // Scaling the touch threshold dynamically based on slider
                    val maxTouchDist = 1800f * (pinScale * pinScale).coerceAtLeast(1f)

                    stations.forEach { station ->
                        // Invert latitude: higher lat = near top (y=0)
                        val x = padding + ((station.longitude - minLon) / lonSpan).toFloat() * w
                        val y = padding + (1f - ((station.latitude - minLat) / latSpan).toFloat()) * h

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
        val padding = 28f
        val w = size.width - (padding * 2)
        val h = size.height - (padding * 2)

        // Draw subtle topographic terrain gradient (SW delta lowlands -> NE Sierra foothills)
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF0284C7).copy(alpha = 0.08f), // Delta basin SW
                    Color(0xFF10B981).copy(alpha = 0.06f), // Mid valley
                    Color(0xFF7C3AED).copy(alpha = 0.12f)  // Sierra ridge NE
                ),
                start = Offset(0f, size.height),
                end = Offset(size.width, 0f)
            )
        )

        // Subtle grid lines
        for (i in 1..3) {
            val gridY = padding + (h * i / 4f)
            drawLine(
                color = Color.Gray.copy(alpha = 0.15f),
                start = Offset(padding, gridY),
                end = Offset(size.width - padding, gridY),
                strokeWidth = 1f
            )
            val gridX = padding + (w * i / 4f)
            drawLine(
                color = Color.Gray.copy(alpha = 0.15f),
                start = Offset(gridX, padding),
                end = Offset(gridX, size.height - padding),
                strokeWidth = 1f
            )
        }

        // Draw stations with dynamic pinScale
        stations.forEach { station ->
            val x = padding + ((station.longitude - minLon) / lonSpan).toFloat() * w
            val y = padding + (1f - ((station.latitude - minLat) / latSpan).toFloat()) * h
            val isSelected = station.id == selectedStationId

            val pinColor = when (station.terrain) {
                TerrainType.COASTAL -> Color(0xFF0284C7)
                TerrainType.RIDGE -> Color(0xFF7C3AED)
                TerrainType.VALLEY_BASIN -> Color(0xFFEA580C)
                TerrainType.URBAN_CANYON -> Color(0xFF475569)
            }

            if (isSelected) {
                // Outer glowing selection ring scaled with pinScale
                drawCircle(
                    color = pinColor.copy(alpha = 0.35f),
                    radius = 20f * pinScale,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = pinColor,
                    radius = 16f * pinScale,
                    center = Offset(x, y),
                    style = Stroke(width = 3f * pinScale)
                )
            }

            // Station Pin
            drawCircle(
                color = Color.White,
                radius = if (isSelected) 9f * pinScale else 7f * pinScale,
                center = Offset(x, y)
            )
            drawCircle(
                color = pinColor,
                radius = if (isSelected) 7f * pinScale else 5.5f * pinScale,
                center = Offset(x, y)
            )
        }
    }
}

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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthDp = maxWidth
        val heightDp = maxHeight
        val density = LocalDensity.current

        val paddingPx = with(density) { 32.dp.toPx() }
        val widthPx = with(density) { widthDp.toPx() } - (paddingPx * 2)
        val heightPx = with(density) { heightDp.toPx() } - (paddingPx * 2)

        // Canvas drawing the slope, shaded terrain, leader lines, and nodes
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

            // Draw elevation terrain fill
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

                val isLandmark = ELEVATION_LANDMARKS.any { it.id == station.id }

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
                        color = pinColor.copy(alpha = 0.35f),
                        radius = 18f,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = pinColor,
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
                    color = pinColor,
                    radius = if (isSelected) 6f else (if (isLandmark) 4.8f else 3.2f),
                    center = Offset(x, y)
                )
            }
        }

        // Overlay Interactive City Landmark Labels along the curve
        sorted.forEachIndexed { index, station ->
            val landmark = ELEVATION_LANDMARKS.find { it.id == station.id }
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
                    border = androidx.compose.foundation.BorderStroke(
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
