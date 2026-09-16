package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.zIndex
import java.util.Locale
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

private data class LandmarkCity(
    val id: String,
    val displayName: String,
    val yOffsetDp: Int = -26,
    val xOffsetDp: Int = 0
)

private val ELEVATION_LANDMARKS = listOf(
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
            LegendItem(color = Color(0xFF166534), label = "Sierra Relief")
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

private data class RegionalMapCity(
    val name: String,
    val lat: Double,
    val lon: Double,
    val isCapital: Boolean = false,
    val elevationText: String? = null,
    val targetStationId: String? = null,
    val xOffsetDp: Int = 0,
    val yOffsetDp: Int = -16
)

private val REGIONAL_MAP_CITIES = listOf(
    RegionalMapCity("Sacramento", 38.5816, -121.4944, isCapital = true, elevationText = "5m", targetStationId = "KSAC", xOffsetDp = -20, yOffsetDp = -18),
    RegionalMapCity("Davis", 38.5449, -121.7405, elevationText = "16m", targetStationId = "KEDU", xOffsetDp = -14, yOffsetDp = -16),
    RegionalMapCity("Roseville", 38.7521, -121.2880, elevationText = "50m", targetStationId = "KMHR", xOffsetDp = -10, yOffsetDp = -16),
    RegionalMapCity("Auburn", 38.8966, -121.0763, elevationText = "453m", targetStationId = "KAUN", xOffsetDp = 4, yOffsetDp = -16),
    RegionalMapCity("Grass Valley", 39.2191, -121.0611, elevationText = "961m", targetStationId = "KGOO", xOffsetDp = -4, yOffsetDp = -16),
    RegionalMapCity("Placerville", 38.7296, -120.7985, elevationText = "568m", targetStationId = "BDMC1", xOffsetDp = -8, yOffsetDp = -16),
    RegionalMapCity("Vacaville", 38.3566, -121.9877, elevationText = "32m", targetStationId = "KVCB", xOffsetDp = -8, yOffsetDp = 8)
)

@Composable
private fun SpatialMapCanvas(
    stations: List<MicroclimateStation>,
    selectedStationId: String?,
    pinScale: Float = 1.0f,
    onSelectStation: (MicroclimateStation) -> Unit
) {
    if (stations.isEmpty()) return

    // Geographic bounding box with slight 4% safety margin
    val rawMinLat = remember(stations) { stations.minOf { it.latitude } }
    val rawMaxLat = remember(stations) { stations.maxOf { it.latitude } }
    val rawMinLon = remember(stations) { stations.minOf { it.longitude } }
    val rawMaxLon = remember(stations) { stations.maxOf { it.longitude } }

    val rawLatSpan = (rawMaxLat - rawMinLat).coerceAtLeast(0.01)
    val rawLonSpan = (rawMaxLon - rawMinLon).coerceAtLeast(0.01)

    val minLat = rawMinLat - (rawLatSpan * 0.04)
    val maxLat = rawMaxLat + (rawLatSpan * 0.04)
    val minLon = rawMinLon - (rawLonSpan * 0.04)
    val maxLon = rawMaxLon + (rawLonSpan * 0.04)

    val latSpan = (maxLat - minLat)
    val lonSpan = (maxLon - minLon)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthDp = maxWidth
        val heightDp = maxHeight
        val density = LocalDensity.current

        val paddingPx = with(density) { 20.dp.toPx() }
        val widthPx = with(density) { widthDp.toPx() } - (paddingPx * 2)
        val heightPx = with(density) { heightDp.toPx() } - (paddingPx * 2)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(stations, pinScale) {
                    detectTapGestures { tapOffset ->
                        val padding = 20f
                        val w = size.width - (padding * 2)
                        val h = size.height - (padding * 2)

                        var closestStation: MicroclimateStation? = null
                        var closestDist = Float.MAX_VALUE

                        val maxTouchDist = 2000f * (pinScale * pinScale).coerceAtLeast(1f)

                        stations.forEach { station ->
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
            val padding = 20f
            val w = size.width - (padding * 2)
            val h = size.height - (padding * 2)

            fun mapX(lon: Double): Float = padding + ((lon - minLon) / lonSpan).toFloat() * w
            fun mapY(lat: Double): Float = padding + (1f - ((lat - minLat) / latSpan).toFloat()) * h

            // 1. Base Alluvial Sacramento Valley Floor
            drawRect(color = Color(0xFFF1F6EC))

            // 2. Coast Range Foothills & Vaca Mountains (Western boundary strip)
            val coastRangePath = Path().apply {
                moveTo(mapX(minLon), mapY(maxLat))
                lineTo(mapX(-122.02), mapY(maxLat))
                lineTo(mapX(-121.98), mapY(39.10))
                lineTo(mapX(-121.92), mapY(38.65))
                lineTo(mapX(-121.88), mapY(38.30))
                lineTo(mapX(-121.84), mapY(minLat))
                lineTo(mapX(minLon), mapY(minLat))
                close()
            }
            drawPath(path = coastRangePath, color = Color(0xFFE3ECD7))

            // 3. Sierra Nevada Elevation Relief Bands (Rising eastward)
            // Band A: Low Foothills & Oak Belt (100m - 350m)
            val foothillsPath = Path().apply {
                moveTo(mapX(-121.36), mapY(maxLat))
                lineTo(mapX(-121.30), mapY(39.10))
                lineTo(mapX(-121.20), mapY(38.80))
                lineTo(mapX(-121.05), mapY(38.50))
                lineTo(mapX(-120.85), mapY(38.15))
                lineTo(mapX(-120.75), mapY(minLat))
                lineTo(mapX(maxLon), mapY(minLat))
                lineTo(mapX(maxLon), mapY(maxLat))
                close()
            }
            drawPath(path = foothillsPath, color = Color(0xFFDFE9D0))

            // Band B: Montane Pine & Conifer Forest (350m - 850m)
            val montanePath = Path().apply {
                moveTo(mapX(-121.18), mapY(maxLat))
                lineTo(mapX(-121.10), mapY(39.15))
                lineTo(mapX(-120.95), mapY(38.85))
                lineTo(mapX(-120.78), mapY(38.55))
                lineTo(mapX(-120.65), mapY(38.20))
                lineTo(mapX(-120.55), mapY(minLat))
                lineTo(mapX(maxLon), mapY(minLat))
                lineTo(mapX(maxLon), mapY(maxLat))
                close()
            }
            drawPath(path = montanePath, color = Color(0xFFC7DEB1))

            // Band C: Sierra High Ridge & Subalpine Crest (>850m - 1500m+)
            val highRidgePath = Path().apply {
                moveTo(mapX(-120.95), mapY(maxLat))
                lineTo(mapX(-120.88), mapY(39.15))
                lineTo(mapX(-120.72), mapY(38.90))
                lineTo(mapX(-120.58), mapY(38.60))
                lineTo(mapX(-120.48), mapY(38.25))
                lineTo(mapX(maxLon), mapY(38.25))
                lineTo(mapX(maxLon), mapY(maxLat))
                close()
            }
            drawPath(path = highRidgePath, color = Color(0xFFAECFA0))

            // 4. Subtle Topographic Elevation Contour Lines
            val contourColor = Color(0xFF6B8B5B).copy(alpha = 0.35f)
            val contourStroke = Stroke(width = 1.2f, cap = StrokeCap.Round)

            // 150m Contour
            val contour150 = Path().apply {
                moveTo(mapX(-121.36), mapY(maxLat))
                lineTo(mapX(-121.30), mapY(39.10))
                lineTo(mapX(-121.20), mapY(38.80))
                lineTo(mapX(-121.05), mapY(38.50))
                lineTo(mapX(-120.85), mapY(38.15))
                lineTo(mapX(-120.75), mapY(minLat))
            }
            drawPath(contour150, color = contourColor, style = contourStroke)

            // 400m Contour
            val contour400 = Path().apply {
                moveTo(mapX(-121.18), mapY(maxLat))
                lineTo(mapX(-121.10), mapY(39.15))
                lineTo(mapX(-120.95), mapY(38.85))
                lineTo(mapX(-120.78), mapY(38.55))
                lineTo(mapX(-120.65), mapY(38.20))
                lineTo(mapX(-120.55), mapY(minLat))
            }
            drawPath(contour400, color = contourColor, style = contourStroke)

            // 900m Contour
            val contour900 = Path().apply {
                moveTo(mapX(-120.95), mapY(maxLat))
                lineTo(mapX(-120.88), mapY(39.15))
                lineTo(mapX(-120.72), mapY(38.90))
                lineTo(mapX(-120.58), mapY(38.60))
                lineTo(mapX(-120.48), mapY(38.25))
            }
            drawPath(contour900, color = contourColor, style = contourStroke)

            // Coast Range Contour
            val contourCoast = Path().apply {
                moveTo(mapX(-122.02), mapY(maxLat))
                lineTo(mapX(-121.98), mapY(39.10))
                lineTo(mapX(-121.92), mapY(38.65))
                lineTo(mapX(-121.88), mapY(38.30))
                lineTo(mapX(-121.84), mapY(minLat))
            }
            drawPath(contourCoast, color = contourColor.copy(alpha = 0.25f), style = contourStroke)

            // 5. Sutter Buttes (Distinct circular volcanic peak cluster in northern valley)
            val sutterX = mapX(-121.82)
            val sutterY = mapY(39.21)
            drawCircle(color = Color(0xFFD2E3C0), radius = 13f, center = Offset(sutterX, sutterY))
            drawCircle(color = Color(0xFFBED7A6), radius = 8f, center = Offset(sutterX, sutterY))
            drawCircle(color = Color(0xFF8BA674), radius = 3.5f, center = Offset(sutterX, sutterY))

            // 6. Hydrography & Waterways (Vibrant River Blue)
            val waterColor = Color(0xFF0284C7)
            val waterGlow = Color(0xFF38BDF8).copy(alpha = 0.35f)

            // Sacramento-San Joaquin Delta / Suisun Estuary Channels (Southwest)
            val deltaChannel1 = Path().apply {
                moveTo(mapX(-121.85), mapY(38.10))
                lineTo(mapX(-121.72), mapY(38.18))
                lineTo(mapX(-121.65), mapY(38.28))
            }
            val deltaChannel2 = Path().apply {
                moveTo(mapX(-121.90), mapY(38.15))
                lineTo(mapX(-121.78), mapY(38.22))
                lineTo(mapX(-121.68), mapY(38.35))
            }
            drawPath(deltaChannel1, color = waterColor.copy(alpha = 0.6f), style = Stroke(width = 2.5f))
            drawPath(deltaChannel2, color = waterColor.copy(alpha = 0.5f), style = Stroke(width = 2.0f))

            // Folsom Lake Reservoir (distinct multi-arm polygon)
            val folsomLakePath = Path().apply {
                moveTo(mapX(-121.17), mapY(38.70))
                lineTo(mapX(-121.14), mapY(38.74))
                lineTo(mapX(-121.10), mapY(38.78))
                lineTo(mapX(-121.07), mapY(38.76))
                lineTo(mapX(-121.10), mapY(38.73))
                lineTo(mapX(-121.05), mapY(38.70))
                lineTo(mapX(-121.11), mapY(38.67))
                lineTo(mapX(-121.16), mapY(38.69))
                close()
            }
            drawPath(path = folsomLakePath, color = waterColor)
            drawPath(path = folsomLakePath, color = Color(0xFF0369A1), style = Stroke(width = 1.2f))

            // Feather River
            val featherRiverPath = Path().apply {
                moveTo(mapX(-121.58), mapY(39.36))
                lineTo(mapX(-121.60), mapY(39.08))
                lineTo(mapX(-121.62), mapY(38.80))
            }
            drawPath(featherRiverPath, color = waterColor.copy(alpha = 0.8f), style = Stroke(width = 1.8f, cap = StrokeCap.Round))

            // American River (Flows from Sierra -> Folsom Lake -> Confluence with Sacramento River)
            val americanRiverPath = Path().apply {
                moveTo(mapX(-120.98), mapY(38.85))
                lineTo(mapX(-121.15), mapY(38.71)) // Folsom Lake
                lineTo(mapX(-121.28), mapY(38.65)) // Fair Oaks
                lineTo(mapX(-121.40), mapY(38.60)) // Rancho Cordova
                lineTo(mapX(-121.51), mapY(38.59)) // Confluence at Sacramento
            }
            drawPath(americanRiverPath, color = waterGlow, style = Stroke(width = 3.6f, cap = StrokeCap.Round))
            drawPath(americanRiverPath, color = waterColor, style = Stroke(width = 2.2f, cap = StrokeCap.Round))

            // Sacramento River (Main valley arterial waterway)
            val sacRiverPath = Path().apply {
                moveTo(mapX(-121.92), mapY(39.38))
                lineTo(mapX(-121.82), mapY(39.20))
                lineTo(mapX(-121.75), mapY(38.98))
                lineTo(mapX(-121.68), mapY(38.80))
                lineTo(mapX(-121.61), mapY(38.70)) // Adjacent to KSMF
                lineTo(mapX(-121.51), mapY(38.59)) // Sacramento confluence
                lineTo(mapX(-121.52), mapY(38.50)) // Passing KSAC
                lineTo(mapX(-121.57), mapY(38.38))
                lineTo(mapX(-121.68), mapY(38.25))
                lineTo(mapX(-121.78), mapY(38.00)) // Into Delta
            }
            drawPath(sacRiverPath, color = waterGlow, style = Stroke(width = 4.2f, cap = StrokeCap.Round))
            drawPath(sacRiverPath, color = waterColor, style = Stroke(width = 2.6f, cap = StrokeCap.Round))

            // 7. Subtle Geographic Reference Grid
            for (i in 1..3) {
                val gridY = padding + (h * i / 4f)
                drawLine(
                    color = Color.DarkGray.copy(alpha = 0.08f),
                    start = Offset(padding, gridY),
                    end = Offset(size.width - padding, gridY),
                    strokeWidth = 1f
                )
                val gridX = padding + (w * i / 4f)
                drawLine(
                    color = Color.DarkGray.copy(alpha = 0.08f),
                    start = Offset(gridX, padding),
                    end = Offset(gridX, size.height - padding),
                    strokeWidth = 1f
                )
            }

            // 8. Station Pins with dynamic pinScale
            stations.forEach { station ->
                val x = mapX(station.longitude)
                val y = mapY(station.latitude)
                val isSelected = station.id == selectedStationId

                val pinColor = when (station.terrain) {
                    TerrainType.COASTAL -> Color(0xFF0284C7)
                    TerrainType.RIDGE -> Color(0xFF7C3AED)
                    TerrainType.VALLEY_BASIN -> Color(0xFFEA580C)
                    TerrainType.URBAN_CANYON -> Color(0xFF475569)
                }

                if (isSelected) {
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

        // 9. Overlay Cartographic Typography & Landmark Badges
        // Regional Watermark Labels
        Box(
            modifier = Modifier
                .offset(x = 36.dp, y = 140.dp)
        ) {
            Text(
                text = "SACRAMENTO VALLEY",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                color = Color(0xFF4B6349).copy(alpha = 0.38f)
            )
        }

        Box(
            modifier = Modifier
                .offset(x = (widthDp * 0.58f), y = 60.dp)
        ) {
            Text(
                text = "SIERRA NEVADA",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                color = Color(0xFF3F5938).copy(alpha = 0.48f)
            )
        }

        Box(
            modifier = Modifier
                .offset(x = 10.dp, y = (heightDp * 0.76f))
        ) {
            Text(
                text = "THE DELTA",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = Color(0xFF0284C7).copy(alpha = 0.55f)
            )
        }

        // Folsom Lake label
        Box(
            modifier = Modifier
                .offset(x = (widthDp * 0.52f), y = (heightDp * 0.44f))
        ) {
            Text(
                text = "Folsom Lake",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0369A1)
            )
        }

        // Sutter Buttes label
        Box(
            modifier = Modifier
                .offset(x = (widthDp * 0.14f), y = (heightDp * 0.12f))
        ) {
            Text(
                text = "Sutter Buttes",
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF4B6349).copy(alpha = 0.70f)
            )
        }

        // Interactive Regional Cities & Landmarks
        REGIONAL_MAP_CITIES.forEach { city ->
            val ratioX = ((city.lon - minLon) / lonSpan).toFloat().coerceIn(0f, 1f)
            val ratioY = (1f - ((city.lat - minLat) / latSpan).toFloat()).coerceIn(0f, 1f)

            val xDp = with(density) { (paddingPx + ratioX * widthPx).toDp() } + city.xOffsetDp.dp
            val yDp = with(density) { (paddingPx + ratioY * heightPx).toDp() } + city.yOffsetDp.dp

            val isSelected = city.targetStationId == selectedStationId

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (city.isCapital) {
                    Color(0xFFFEF3C7)
                } else {
                    Color.White.copy(alpha = 0.88f)
                },
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (city.isCapital) Color(0xFFD97706) else Color.DarkGray.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .offset(x = xDp, y = yDp)
                    .clickable {
                        city.targetStationId?.let { targetId ->
                            stations.find { it.id == targetId }?.let { onSelectStation(it) }
                        }
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    if (city.isCapital) {
                        Text(
                            text = "★ ",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    }
                    Text(
                        text = city.name,
                        fontSize = 8.sp,
                        fontWeight = if (city.isCapital || isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (city.isCapital) Color(0xFF92400E) else Color(0xFF1E293B)
                    )
                    if (city.elevationText != null) {
                        Text(
                            text = " ${city.elevationText}",
                            fontSize = 7.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Compass Rose (Top-Left)
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color.White.copy(alpha = 0.90f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "▲ N",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0284C7)
                )
            }
        }

        // Map Scale Bar (Bottom-Left)
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color.White.copy(alpha = 0.90f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 6.dp, bottom = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "0 ── 15 mi (24 km)",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
            }
        }

        // Topo Relief Badge (Top-Right)
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFF0FDF4),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 6.dp, top = 6.dp)
        ) {
            Text(
                text = "🗺️ Topo Relief",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF166534),
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
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
            border = androidx.compose.foundation.BorderStroke(1.dp, skyTheme.accent.copy(alpha = 0.45f)),
            modifier = Modifier
                .offset(x = 45.dp, y = 45.dp)
                .clickable {
                    // Tap to cycle landmarks
                    val currentIndex = ELEVATION_LANDMARKS.indexOfFirst { it.id == currentStation?.id }
                    val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % ELEVATION_LANDMARKS.size else 0
                    val nextId = ELEVATION_LANDMARKS[nextIndex].id
                    sorted.find { it.id == nextId }?.let { onSelectStation(it) }
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
