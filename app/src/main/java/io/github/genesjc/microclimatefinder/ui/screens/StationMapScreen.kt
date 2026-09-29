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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.microclimatefinder.data.local.LocalNewsRepository
import io.github.genesjc.microclimatefinder.data.local.StationCsvParser
import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaGeography
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegionalDefinitions
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.domain.model.TerrainType
import io.github.genesjc.microclimatefinder.ui.components.LegendItem
import io.github.genesjc.microclimatefinder.ui.components.StationCard

@Composable
fun StationMapScreen(
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

        // Interactive Spatial Map Canvas Container
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
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
                        hasArtwork = dataset.regionId == CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.id,
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
    hasArtwork: Boolean,
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

    val textMeasurer = rememberTextMeasurer()
    val landmarks = remember(stations) { pickMapLandmarks(stations) }

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

            if (hasArtwork) {
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
            } else {
                drawNeutralBase(
                    minLon = minLon, maxLon = maxLon, minLat = minLat, maxLat = maxLat,
                    padding = padding, mapX = ::mapX, mapY = ::mapY,
                    textMeasurer = textMeasurer
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

        if (hasArtwork) {
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
        } else {
            // Place labels picked from the loaded stations
            landmarks.forEach { station ->
                val ratioX = ((station.longitude - minLon) / lonSpan).toFloat().coerceIn(0f, 1f)
                val ratioY = (1f - ((station.latitude - minLat) / latSpan).toFloat()).coerceIn(0f, 1f)
                val xDp = with(density) { (paddingPx + ratioX * widthPx).toDp() } + (if (ratioX > 0.72f) (-56).dp else 0.dp)
                val yDp = with(density) { (paddingPx + ratioY * heightPx).toDp() } + (if (ratioY < 0.12f) 8.dp else (-16).dp)
                val isSelected = station.id == selectedStationId
                val city = LocalNewsRepository.getCleanCityName(station.id, station.name)

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White.copy(alpha = 0.88f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .offset(x = xDp, y = yDp)
                        .clickable { onSelectStation(station) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = city,
                            fontSize = 8.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = " ${"%,d".format(station.elevationMeters)}m",
                            fontSize = 7.sp,
                            color = Color(0xFF64748B)
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

        // Map Scale Bar (Bottom-Left): a bar drawn to the map's real scale
        val midLat = (minLat + maxLat) / 2
        val mapWidthKm = lonSpan * 111.32 * cos(midLat * PI / 180)
        val scaleKm = listOf(5, 10, 20, 25, 50, 100, 200).lastOrNull { it <= mapWidthKm * 0.4 } ?: 5
        val scaleMiles = (scaleKm * 0.621371).roundToInt()
        val scaleBarDp = with(density) { (scaleKm / mapWidthKm * widthPx).toFloat().toDp() }
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
                text = if (hasArtwork) "🗺️ Topo Relief" else "Base map: coast and border only",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF166534),
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
    }
}


/** Plain base map with no terrain artwork: land, ocean, state border, coast and a lat/lon grid. */
private fun DrawScope.drawNeutralBase(
    minLon: Double,
    maxLon: Double,
    minLat: Double,
    maxLat: Double,
    padding: Float,
    mapX: (Double) -> Float,
    mapY: (Double) -> Float,
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

    // Latitude / longitude grid with degree labels
    val span = maxOf(maxLon - minLon, maxLat - minLat)
    val step = when {
        span > 6.0 -> 1.0
        span > 2.5 -> 0.5
        else -> 0.25
    }
    val labelStyle = TextStyle(fontSize = 8.sp, color = Color(0xFF64748B))
    var lon = ceil(minLon / step) * step
    while (lon <= maxLon) {
        val x = mapX(lon)
        drawLine(Color.DarkGray.copy(alpha = 0.10f), Offset(x, padding), Offset(x, size.height - padding), 1f)
        val label = textMeasurer.measure("%.2f\u00B0W".format(abs(lon)).replace(".00", ".0"), labelStyle)
        drawText(label, topLeft = Offset(x + 2f, size.height - padding - label.size.height))
        lon += step
    }
    var lat = ceil(minLat / step) * step
    while (lat <= maxLat) {
        val y = mapY(lat)
        drawLine(Color.DarkGray.copy(alpha = 0.10f), Offset(padding, y), Offset(size.width - padding, y), 1f)
        val label = textMeasurer.measure("%.2f\u00B0N".format(lat).replace(".00", ".0"), labelStyle)
        drawText(label, topLeft = Offset(padding + 3f, y - label.size.height - 1f))
        lat += step
    }

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

/**
 * Picks about six stations spread across the map for place labels. Airport stations (K-prefixed)
 * are preferred because they are recognizable cities.
 */
private fun pickMapLandmarks(stations: List<MicroclimateStation>, count: Int = 6): List<MicroclimateStation> {
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
