package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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

        // Interactive Map / Elevation Canvas
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
        ) {
            if (activeMode == MapVisualizationMode.SPATIAL_GRID) {
                SpatialMapCanvas(
                    stations = stations,
                    selectedStationId = selectedStationId,
                    onSelectStation = { selectedStationId = it.id }
                )
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
            .pointerInput(stations) {
                detectTapGestures { tapOffset ->
                    val padding = 34f
                    val w = size.width - (padding * 2)
                    val h = size.height - (padding * 2)

                    var closestStation: MicroclimateStation? = null
                    var closestDist = Float.MAX_VALUE

                    stations.forEach { station ->
                        // Invert latitude: higher lat = near top (y=0)
                        val x = padding + ((station.longitude - minLon) / lonSpan).toFloat() * w
                        val y = padding + (1f - ((station.latitude - minLat) / latSpan).toFloat()) * h

                        val dx = tapOffset.x - x
                        val dy = tapOffset.y - y
                        val dist = (dx * dx + dy * dy)
                        // Hit radius of 28dp
                        if (dist < closestDist && dist < 1600f) {
                            closestDist = dist
                            closestStation = station
                        }
                    }
                    closestStation?.let { onSelectStation(it) }
                }
            }
    ) {
        val padding = 34f
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

        // Draw compass / cardinal labels
        // N at top right, SW Delta at bottom left
        // Draw stations
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
                // Outer glowing selection ring
                drawCircle(
                    color = pinColor.copy(alpha = 0.35f),
                    radius = 20f,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = pinColor,
                    radius = 16f,
                    center = Offset(x, y),
                    style = Stroke(width = 3f)
                )
            }

            // Station Pin
            drawCircle(
                color = Color.White,
                radius = if (isSelected) 9f else 7f,
                center = Offset(x, y)
            )
            drawCircle(
                color = pinColor,
                radius = if (isSelected) 7f else 5.5f,
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

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(sorted) {
                detectTapGestures { tapOffset ->
                    val padding = 34f
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
                        if (dist < closestDist && dist < 1600f) {
                            closestDist = dist
                            closestStation = station
                        }
                    }
                    closestStation?.let { onSelectStation(it) }
                }
            }
    ) {
        val padding = 34f
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

        // Station points along the slope
        sorted.forEachIndexed { index, station ->
            val x = padding + (index.toFloat() / (sorted.size - 1).coerceAtLeast(1)) * w
            val elevRatio = station.elevationMeters / maxElevation
            val y = padding + (1f - elevRatio) * h
            val isSelected = station.id == selectedStationId

            val pinColor = when (station.terrain) {
                TerrainType.RIDGE -> Color(0xFF7C3AED)
                TerrainType.COASTAL -> Color(0xFF0284C7)
                TerrainType.VALLEY_BASIN -> Color(0xFFEA580C)
                TerrainType.URBAN_CANYON -> Color(0xFF475569)
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
                radius = if (isSelected) 8f else 5.5f,
                center = Offset(x, y)
            )
            drawCircle(
                color = pinColor,
                radius = if (isSelected) 6f else 4f,
                center = Offset(x, y)
            )
        }
    }
}
