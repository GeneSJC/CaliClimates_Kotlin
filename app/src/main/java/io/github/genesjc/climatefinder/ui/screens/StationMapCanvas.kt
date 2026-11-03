package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.ui.res.stringResource
import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.ui.theme.LocalStateUsa
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import io.github.genesjc.climatefinder.ui.text.rememberFeetFormatter
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.genesjc.climatefinder.data.local.TerrainImage
import io.github.genesjc.climatefinder.ui.components.MapProjection
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import io.github.genesjc.climatefinder.ui.components.MAX_MAP_ZOOM
import io.github.genesjc.climatefinder.ui.components.ZoomSlider
import io.github.genesjc.climatefinder.ui.components.MapText
import io.github.genesjc.climatefinder.ui.components.HOME_TILE_BAND
import io.github.genesjc.climatefinder.ui.components.BRING_INTO_VIEW_MS
import io.github.genesjc.climatefinder.ui.components.SliderToggleButton
import io.github.genesjc.climatefinder.ui.components.ms
import io.github.genesjc.climatefinder.ui.components.animateZoom
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import io.github.genesjc.climatefinder.ui.components.clampPan
import io.github.genesjc.climatefinder.ui.components.labelBudget
import io.github.genesjc.climatefinder.ui.components.panForZoom
import io.github.genesjc.climatefinder.ui.components.rememberLabelFades
import io.github.genesjc.climatefinder.ui.components.zoomDotGrowth
import androidx.compose.ui.graphics.asImageBitmap

/** A landmark's label text and its estimated width: fixed per station, so made once, not on every zoom or pan frame. */
internal class LandmarkLabel(val station: MicroclimateStation, val city: String, val elevText: String, val widthDp: Dp)

internal data class StationLabelPlan(
    val station: MicroclimateStation,
    val city: String,
    val elevText: String,
    val xDp: Dp,
    val yDp: Dp
)

@Composable
internal fun SpatialMapCanvas(
    stations: List<MicroclimateStation>,
    terrain: TerrainImage?,
    cityName: (MicroclimateStation) -> String,
    embedded: Boolean,
    selectedStationId: String?,
    pinScale: Float = 1.0f,
    initialZoom: Float = 1f,
    onZoomChange: ((Float) -> Unit)? = null,
    sliderToggleTop: Dp = 52.dp,
    expandT: Float = 0f,
    onSelectedOffscreenChange: ((Boolean) -> Unit)? = null,
    recenterTick: Int = 0,
    sliderShownShared: Boolean? = null,
    onSliderShownChange: ((Boolean) -> Unit)? = null,
    onSelectStation: (MicroclimateStation) -> Unit
) {
    val outline = LocalStateUsa.current.outline
    if (stations.isEmpty()) return
    // The terrain picture as a Compose image, made once per picture (the data layer hands over a plain bitmap)
    val terrainPicture = remember(terrain) { terrain?.bitmap?.asImageBitmap() }

    // The selected station; when none is selected yet (or the id is not in this region) Home shows the first one, so this does too
    val selectedStation = stations.firstOrNull { it.id == selectedStationId } ?: stations.firstOrNull()

    // Geographic bounding box with a margin of 8%
    val rawMinLat = remember(stations) { stations.minOf { it.latitude } }
    val rawMaxLat = remember(stations) { stations.maxOf { it.latitude } }
    val rawMinLon = remember(stations) { stations.minOf { it.longitude } }
    val rawMaxLon = remember(stations) { stations.maxOf { it.longitude } }

    val rawLatSpan = (rawMaxLat - rawMinLat).coerceAtLeast(0.01)
    val rawLonSpan = (rawMaxLon - rawMinLon).coerceAtLeast(0.01)

    val minLat = rawMinLat - (rawLatSpan * FRAME_MARGIN)
    val maxLat = rawMaxLat + (rawLatSpan * FRAME_MARGIN)
    val minLon = rawMinLon - (rawLonSpan * FRAME_MARGIN)
    val maxLon = rawMaxLon + (rawLonSpan * FRAME_MARGIN)

    val textMeasurer = rememberTextMeasurer()
    // Ranked by spread, so the first few are the most useful; more fade in as the map zooms
    val landmarks = remember(stations) { pickMapLandmarks(stations, minOf(stations.size, MAP_LABEL_COUNT * 8)) }

    // View transform: zoom about the map centre plus a pan in pixels
    // The labels' text and widths, made once per station list (the placement below runs on every zoom and pan frame)
    val textScale = MapText.scale
    val feetText = rememberFeetFormatter()
    val landmarkLabels = remember(landmarks, textScale) {
        landmarks.map { station ->
            val city = cityName(station)
                .let { if (it.length > 18) it.take(17) + "\u2026" else it }
            val elevText = feetText(station.elevationMeters)
            // Estimated label width at the current text scale; labels that would pass the right edge end at the pin instead
            LandmarkLabel(station, city, elevText, ((city.length + elevText.length + 1) * 4.9f * textScale + 12f).dp)
        }
    }

    var zoom by remember(stations) { mutableStateOf(initialZoom.coerceIn(1f, MAX_MAP_ZOOM)) }
    // On Home the slider can be tucked away with the small arrow button above it (pinch still zooms)
    var localSliderShown by remember { mutableStateOf(true) }
    val sliderShown = sliderShownShared ?: localSliderShown
    val setSliderShown: (Boolean) -> Unit = { localSliderShown = it; onSliderShownChange?.invoke(it) }
    // Its top sits lower on Home, below that button and the view buttons
    val sliderTop = if (embedded) sliderToggleTop + 32.dp + 6.dp else 56.dp
    androidx.compose.runtime.LaunchedEffect(zoom) { onZoomChange?.invoke(zoom) }
    var pan by remember(stations) { mutableStateOf(Offset.Zero) }
    // Button, rail-tap and double-tap zooms animate (so labels fade smoothly); pinch and drag stay direct
    val zoomScope = rememberCoroutineScope()
    var zoomJob by remember { mutableStateOf<Job?>(null) }
    fun animateZoomTo(newZoom: Float, anchor: Offset, w: Float, h: Float) {
        zoomJob?.cancel()
        zoomJob = zoomScope.launch {
            animateZoom(zoom, pan, newZoom, anchor, w, h, overscroll = MAP_OVERSCROLL) { z, p -> zoom = z; pan = p }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val widthDp = maxWidth
        val heightDp = maxHeight
        val density = LocalDensity.current

        val paddingPx = with(density) { 20.dp.toPx() }
        val canvasW = with(density) { widthDp.toPx() }
        val canvasH = with(density) { heightDp.toPx() }
        // One scale in both directions, so the region keeps its true shape
        // On Home the station tile floats over the top of the map: the zoomed-out map fits below it
        val topInset = if (embedded) with(density) { HOME_TILE_BAND.toPx() } else 0f
        val projection = remember(minLon, maxLon, minLat, maxLat, canvasW, canvasH, topInset) {
            MapProjection(minLon, maxLon, minLat, maxLat, canvasW, canvasH, paddingPx, topInset)
        }
        fun viewX(baseX: Float) = canvasW / 2f + (baseX - canvasW / 2f) * zoom + pan.x
        fun viewY(baseY: Float) = canvasH / 2f + (baseY - canvasH / 2f) * zoom + pan.y
        // Longitude and latitude to canvas pixels, with the current zoom and pan
        fun mapX(lon: Double): Float = viewX(projection.baseX(lon))
        fun mapY(lat: Double): Float = viewY(projection.baseY(lat))

        // Eases the map over so the selected station is in the middle of the view. Without [always] it does nothing when
        // the station is already comfortably in view.
        suspend fun bringSelectedIntoView(always: Boolean) {
            val st = selectedStation ?: return
            val bx = projection.baseX(st.longitude)
            val by = projection.baseY(st.latitude)
            val vx = viewX(bx)
            val vy = viewY(by)
            val sideMargin = with(density) { 48.dp.toPx() }
            val topMargin = with(density) { (if (embedded) HOME_TILE_BAND + 24.dp else 40.dp).toPx() }
            val bottomMargin = with(density) { 40.dp.toPx() }
            val inView = vx in sideMargin..(canvasW - sideMargin) && vy in topMargin..(canvasH - bottomMargin)
            if (inView && !always) return
            zoomJob?.cancel()
            val from = pan
            val target = clampPan(
                Offset(-(bx - canvasW / 2f) * zoom, -(by - canvasH / 2f) * zoom),
                zoom, canvasW, canvasH, overscroll = MAP_OVERSCROLL
            )
            androidx.compose.animation.core.animate(0f, 1f, animationSpec = tween(BRING_INTO_VIEW_MS, easing = FastOutSlowInEasing)) { v, _ ->
                pan = Offset(from.x + (target.x - from.x) * v, from.y + (target.y - from.y) * v)
            }
        }
        // When the selected station changes (from the Stations list, the tile or the other view) and it is not in view
        // at this zoom and pan, ease the map over so it is
        androidx.compose.runtime.LaunchedEffect(selectedStationId, stations) { bringSelectedIntoView(always = false) }
        // The locate button on Home asks for it explicitly
        androidx.compose.runtime.LaunchedEffect(recenterTick) { if (recenterTick > 0) bringSelectedIntoView(always = true) }

        // Whether the selected station is out of the visible map (panned away, zoomed past it, or under the station tile),
        // reported to Home, which shows its locate button only then
        val topHidden = if (embedded) with(density) { HOME_TILE_BAND.toPx() } else 0f
        // Worked out directly from the current zoom and pan on every change (the label placement below already runs
        // that often), so it can never lag behind or hold on to old values
        val selectedOffscreen = selectedStation?.let { st ->
            val x = viewX(projection.baseX(st.longitude))
            val y = viewY(projection.baseY(st.latitude))
            !(x in 0f..canvasW && y in topHidden..canvasH)
        } ?: false
        androidx.compose.runtime.LaunchedEffect(selectedOffscreen) { onSelectedOffscreenChange?.invoke(selectedOffscreen) }
        androidx.compose.runtime.DisposableEffect(Unit) { onDispose { onSelectedOffscreenChange?.invoke(false) } }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // The pan, pinch and double-tap handlers live on this layer, not on the container around it: the slider and the
                // buttons are siblings above this layer, so a touch on them never reaches these handlers (as children of the container
                // they shared its touches, and a drag from the slider's thumb could be taken by the map)
                // Keyed to the data: zoom and pan are remembered per region, so when the region changes they are new state objects
                // and the handlers must restart to use them (with a fixed key they kept writing to the old ones and touch stopped working)
                .pointerInput(stations) {
                    // Pinch to zoom, drag to pan
                    detectTransformGestures { centroid, panChange, zoomChange, _ ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val newZoom = (zoom * zoomChange).coerceIn(1f, MAX_MAP_ZOOM)
                        pan = clampPan(
                            panForZoom(pan, zoom, newZoom, centroid, w, h, overscroll = MAP_OVERSCROLL) + panChange,
                            newZoom, w, h, overscroll = MAP_OVERSCROLL
                        )
                        zoom = newZoom
                    }
                }
                .pointerInput(stations) {
                    // Double-tap: zoom in on the spot, and back out to the full map once fully zoomed
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
                .pointerInput(stations, pinScale, projection) {
                    detectTapGestures { tapOffset ->
                        var closestStation: MicroclimateStation? = null
                        var closestDist = Float.MAX_VALUE

                        val dotScale = pinScale * zoomDotGrowth(zoom)
                        val maxTouchDist = 2000f * (dotScale * dotScale).coerceAtLeast(1f)

                        stations.forEach { station ->
                            val x = mapX(station.longitude)
                            val y = mapY(station.latitude)

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
            if (terrain != null && terrainPicture != null) {
                // Land colour first (the picture may not reach every edge), then the terrain, then a light grid
                drawRect(color = Color(0xFFE4EBDA))
                val b = terrain.bounds
                val left = mapX(b.minLon)
                val top = mapY(b.maxLat)
                drawImage(
                    image = terrainPicture,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(terrainPicture.width, terrainPicture.height),
                    dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                    dstSize = IntSize((mapX(b.maxLon) - left).roundToInt(), (mapY(b.minLat) - top).roundToInt()),
                    filterQuality = FilterQuality.Medium
                )
                drawGraticule(
                    minLon = minLon, maxLon = maxLon, minLat = minLat, maxLat = maxLat,
                    padding = paddingPx, mapX = ::mapX, mapY = ::mapY,
                    zoom = zoom, textMeasurer = textMeasurer
                )
            } else {
                drawNeutralBase(
                    outline = outline,
                    minLon = minLon, maxLon = maxLon, minLat = minLat, maxLat = maxLat,
                    padding = paddingPx, mapX = ::mapX, mapY = ::mapY,
                    zoom = zoom, textMeasurer = textMeasurer
                )
            }

            // 8. Station Pins: dot-size setting times the growth from zoom
            val dotScale = pinScale * zoomDotGrowth(zoom)
            stations.forEach { station ->
                val x = mapX(station.longitude)
                val y = mapY(station.latitude)
                // The selected station is drawn last, above the labels, by the overlay below
                if (station.id == selectedStationId) return@forEach
                drawStationPin(x, y, pinColor(station.terrain), dotScale, selected = false)
            }
        }

        // Areas taken by the fixed controls (compass, expand toggle, topo badge, scale bar, zoom slider) so labels stay clear of them
        val placedRects = mutableListOf(
            if (embedded) listOf(0f, 0f, widthDp.value, HOME_TILE_BAND.value) else listOf(0f, 0f, 0f, 0f),  // the floating station tile
            if (embedded) listOf(0f, HOME_TILE_BAND.value, 140f, HOME_TILE_BAND.value + 50f) else listOf(0f, 0f, 58f, 40f),  // Home's control row, or the compass
            listOf(0f, heightDp.value - 46f, 130f, heightDp.value),     // scale bar
            if (embedded) listOf(widthDp.value - 46f, sliderToggleTop.value, widthDp.value, sliderToggleTop.value + 32f) else listOf(0f, 0f, 0f, 0f),  // the slider's arrow button
            if (embedded && !sliderShown) listOf(0f, 0f, 0f, 0f) else listOf(widthDp.value - 46f, sliderTop.value, widthDp.value, sliderTop.value + (heightDp.value - sliderTop.value - 4f) * (if (embedded) 0.8f else 1f)),  // zoom slider
            if (embedded) listOf(widthDp.value - 170f, HOME_TILE_BAND.value, widthDp.value, HOME_TILE_BAND.value + 56f) else listOf(0f, 0f, 0f, 0f),  // Home's view buttons
            listOf(widthDp.value - 126f, 52f, widthDp.value - 46f, 84f)  // expand / shrink toggle
        )

        // Which labels have room right now. Only the target (shown or hidden) is decided here; the
        // opacity eases toward it on its own, so labels fade in and out however the zoom changed.
        val labelTargets = mutableMapOf<String, Float>()
        val stationPlans = mutableListOf<StationLabelPlan>()

        run {
            // Labels for stations picked from the loaded stations: more as the map zooms
            val stationBudget = labelBudget(MAP_LABEL_COUNT, zoom, 3f).toInt()
            var shownLabels = 0
            for (label in landmarkLabels) {
                val station = label.station
                val city = label.city
                val elevText = label.elevText
                val labelWidthDp = label.widthDp
                val pinXDp = with(density) { mapX(station.longitude).toDp() }
                val pinYDp = with(density) { mapY(station.latitude).toDp() }
                // Stations panned out of view get no label
                if (pinXDp < 0.dp || pinXDp > widthDp || pinYDp < 0.dp || pinYDp > heightDp) continue
                val xDp = if (pinXDp + labelWidthDp > widthDp - 6.dp) pinXDp - labelWidthDp else pinXDp
                val yDp = pinYDp + (if (pinYDp < 30.dp) 8.dp else (-16f * MapText.scale).dp)
                val left = xDp.value
                val top = yDp.value
                val right = left + labelWidthDp.value
                val bottom = top + 20f * MapText.scale
                val hasRoom = shownLabels < stationBudget &&
                    placedRects.none { r -> left < r[2] && right > r[0] && top < r[3] && bottom > r[1] }
                if (hasRoom) {
                    placedRects.add(listOf(left, top, right, bottom))
                    shownLabels++
                }
                labelTargets[station.id] = if (hasRoom) 1f else 0f
                stationPlans.add(StationLabelPlan(station, city, elevText, xDp, yDp))
            }
        }
        val labelFades = rememberLabelFades(labelTargets)

        // Draw every label that is at least partly visible, at its current opacity
        stationPlans.forEach { plan ->
            val fade = labelFades[plan.station.id] ?: 0f
            if (fade > 0.01f) {
                val isSelected = plan.station.id == selectedStationId
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White.copy(alpha = 0.88f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .offset(x = plan.xDp, y = plan.yDp)
                        .alpha(fade)
                        .clickable { onSelectStation(plan.station) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.8.dp)
                    ) {
                        Text(
                            text = plan.city,
                            fontSize = ms(8f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = " ${plan.elevText}",
                            fontSize = ms(7f),
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        // The selected station's marker, above every label and city name, so nothing hides it. The
        // canvas takes no touches; taps fall through to the map below.
        stations.firstOrNull { it.id == selectedStationId }?.let { station ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawStationPin(mapX(station.longitude), mapY(station.latitude), pinColor(station.terrain), pinScale * zoomDotGrowth(zoom), selected = true)
            }
        }

        // Compass Rose (Top-Left). On Home the north indicator is part of the control row Home draws, so none here.
        if (!embedded) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color.White.copy(alpha = 0.90f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = if (embedded) 50.dp + HOME_TILE_BAND else 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "▲ N",
                    fontSize = ms(8f),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0284C7)
                )
            }
        }

        }

        // Map Scale Bar (Bottom-Left): a bar drawn to the map's real scale
        val mapWidthKm = canvasW / (projection.pixelsPerKm * zoom)
        val scaleKm = listOf(1, 2, 5, 10, 20, 25, 50, 100, 200).lastOrNull { it <= mapWidthKm * 0.4 } ?: 5
        val scaleMiles = (scaleKm * 0.621371).roundToInt()
        val scaleBarDp = with(density) { (scaleKm * projection.pixelsPerKm * zoom).toFloat().toDp() }
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
                    text = stringResource(R.string.map_scale_bar, scaleMiles, scaleKm),
                    fontSize = ms(7.5f),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
            }
        }

        // The arrow button above the slider: up while it shows (tap to tuck it away), down when it is hidden
        if (embedded) {
            SliderToggleButton(
                shown = sliderShown,
                onToggle = { setSliderShown(!sliderShown) },
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 8.dp + 5.dp * expandT, top = sliderToggleTop)
            )
        }
        // Floating zoom slider on the right edge (for now it sets the type size instead; see MapText)
        if (embedded && !sliderShown) {
            // hidden: pinch and double-tap still zoom
        } else if (MapText.SLIDER_SETS_FONT) {
            ZoomSlider(
                value = MapText.scale,
                onValueChange = { MapText.scale = it },
                minValue = MapText.MIN_SCALE,
                maxValue = MapText.MAX_SCALE,
                caption = stringResource(R.string.zoom_font_caption),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = sliderTop, end = 4.dp + 5.dp * expandT, bottom = 4.dp)
                    .width(40.dp)
                    // 20% shorter from the bottom on Home; the top stays under the arrow button
                .fillMaxHeight(if (embedded) 0.8f else 1f)
            )
        } else ZoomSlider(
            value = zoom,
            onValueChange = { newZoom ->
                zoomJob?.cancel()
                pan = panForZoom(pan, zoom, newZoom, Offset(canvasW / 2f, canvasH / 2f), canvasW, canvasH, overscroll = MAP_OVERSCROLL)
                zoom = newZoom
            },
            onStep = { newZoom -> animateZoomTo(newZoom, Offset(canvasW / 2f, canvasH / 2f), canvasW, canvasH) },
            // Its top stays under the data credit whatever the map's height; the bottom follows the map
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = sliderTop, end = 4.dp + 5.dp * expandT, bottom = 4.dp)
                .width(40.dp)
                // 20% shorter from the bottom on Home; the top stays under the arrow button
                .fillMaxHeight(if (embedded) 0.8f else 1f)
        )
    }
}
