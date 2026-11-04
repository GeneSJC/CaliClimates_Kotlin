package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import io.github.genesjc.climatefinder.ui.text.rememberFeetFormatter
import io.github.genesjc.climatefinder.ui.text.rememberMilesFormatter
import io.github.genesjc.climatefinder.domain.BandAngle
import io.github.genesjc.climatefinder.domain.BandAxis
import io.github.genesjc.climatefinder.domain.DepthBands
import io.github.genesjc.climatefinder.domain.StationDistance
import kotlin.math.abs
import io.github.genesjc.climatefinder.data.local.TerrainImage
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp
import io.github.genesjc.climatefinder.domain.model.ObservationResult
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import io.github.genesjc.climatefinder.ui.theme.LocalStateUsa
import io.github.genesjc.climatefinder.ui.components.MAX_MAP_ZOOM
import io.github.genesjc.climatefinder.ui.components.HOME_TILE_BAND
import io.github.genesjc.climatefinder.ui.components.BRING_INTO_VIEW_MS
import io.github.genesjc.climatefinder.ui.components.layerColor
import io.github.genesjc.climatefinder.ui.components.animateZoom
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import io.github.genesjc.climatefinder.ui.components.clampPan
import io.github.genesjc.climatefinder.ui.components.rememberLabelFades
import io.github.genesjc.climatefinder.ui.components.panForZoom
import io.github.genesjc.climatefinder.ui.components.zoomDotGrowth
import androidx.compose.ui.graphics.asImageBitmap

@Composable
internal fun ElevationProfileCanvas(
    /** A short note to show beside the bands controls, such as which view the bands button switched to. */
    notice: String? = null,
    stations: List<MicroclimateStation>,
    regionId: String,
    terrain: TerrainImage?,
    skyline: List<FloatArray>?,
    cityName: (MicroclimateStation) -> String,
    knownReading: (String) -> ObservationResult?,
    fetchReading: suspend (MicroclimateStation) -> ObservationResult,
    /** Counts a visit to this view; true when the hint in words should show. */
    startVisit: () -> Boolean,
    onLineTapped: () -> Unit,
    embedded: Boolean,
    expandT: Float = 0f,
    /** The layers view (stations in bands), and the button's toggle. */
    layersOn: Boolean = false,
    onToggleLayers: () -> Unit = {},
    /** The direction the bands look across the region. */
    bandAxis: BandAxis = BandAxis.NORTH_SOUTH,
    bandAngle: Int = bandAxis.degrees.toInt(),
    onAngleChange: (Int) -> Unit = {},
    selectedStationId: String?,
    initialZoom: Float = 1f,
    onZoomChange: ((Float) -> Unit)? = null,
    sliderShownShared: Boolean? = null,
    onSliderShownChange: ((Boolean) -> Unit)? = null,
    onSelectStation: (MicroclimateStation) -> Unit
) {
    if (stations.isEmpty()) return
    // The terrain picture as a Compose image, made once per picture (the data layer hands over a plain bitmap)
    val terrainPicture = remember(terrain) { terrain?.bitmap?.asImageBitmap() }

    val treeMix = LocalStateUsa.current.treeMix[regionId] ?: DEFAULT_TREE_MIX
    // Sort stations by elevation ascending (valley 5m -> ridge 1406m)
    val sorted = remember(stations) { stations.sortedBy { it.elevationMeters } }
    val maxElevation = remember(sorted) { (sorted.maxOfOrNull { it.elevationMeters } ?: 1500).toFloat() }
    // Stations ranked for labels; the first few are the ones the sky badge cycles through
    val landmarkIds = LocalStateUsa.current.landmarkStations[regionId] ?: emptyList()
    val rankedLandmarks = remember(sorted, landmarkIds) { rankLandmarkIndexes(sorted, landmarkIds) }
    // Each station's label text, made once (label placement runs on every zoom and pan frame)
    val feetText = rememberFeetFormatter()
    val labelNames = remember(sorted) { sorted.map { cityName(it) } }
    val labelTexts = remember(sorted) {
        sorted.mapIndexed { i, station -> labelNames[i] + " ${feetText(station.elevationMeters)}" }
    }
    // How far each station is from the selected one (nothing selected: no distances), added to the other labels
    val milesText = rememberMilesFormatter()
    val milesFromSelected = remember(sorted, selectedStationId) {
        sorted.find { it.id == selectedStationId }?.let { StationDistance.milesFrom(it, sorted) }.orEmpty()
    }
    // Each station's depth from the selected one (0 here, 1 across the region); null for all when nothing is selected
    val spanMiles = remember(sorted) { StationDistance.spanMiles(sorted) }
    fun depthOf(id: String): Float? = milesFromSelected[id]?.let { StationDistance.depth(it.toDouble(), spanMiles) }
    val ridgeCache = remember { RidgeHeights() }
    val cycleIds = remember(sorted) {
        rankedLandmarks.take(LANDMARK_COUNT).sorted().map { sorted[it].id }
    }

    // On Home the slider can be tucked away with the small arrow button under the menu button (pinch still zooms)
    var localSliderShown by remember { mutableStateOf(true) }
    val sliderShown = sliderShownShared ?: localSliderShown
    val setSliderShown: (Boolean) -> Unit = { localSliderShown = it; onSliderShownChange?.invoke(it) }
    val sliderCollapsible = embedded
    // The slider starts lower on Home, to leave room for that button
    val sliderTop = 56.dp + (if (embedded) HOME_TILE_BAND + (if (VIEW_CHIP_IN_CONTROLS) VIEW_CHIP_HEIGHT + CONTROLS_GAP else 32.dp) else 0.dp)
    // The layers button sits at the right, under Home's view buttons (or level with the expand toggle)
    val layersButtonTop = if (embedded) HOME_TILE_BAND + 62.dp else 56.dp

    // View transform: an even zoom (like a magnifying glass) about the centre plus a pan in pixels
    var zoom by remember(sorted) { mutableStateOf(initialZoom.coerceIn(ELEVATION_MIN_ZOOM, MAX_MAP_ZOOM)) }
    androidx.compose.runtime.LaunchedEffect(zoom) { onZoomChange?.invoke(zoom) }

    // Every visit: a pulse runs up the line once. First launches only: a hint in words says the line can be tapped
    var showHint by remember { mutableStateOf(false) }
    val pulse = remember { androidx.compose.animation.core.Animatable(0f) }
    // The hint eases in to a translucent look, then fades slowly from the start; a tap clears it quickly
    val hintAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(showHint) {
        if (showHint) {
            hintAlpha.animateTo(0.75f, tween(600))
            hintAlpha.animateTo(0f, tween(5500, easing = androidx.compose.animation.core.LinearEasing))
        } else {
            hintAlpha.animateTo(0f, tween(300))
        }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        // Counts this visit; the words show only while the hint is still wanted
        // The ripple up the line plays on every visit to this view (it is the cue that the line can be tapped); the
        // words of the hint show only on the first few launches, and stop at the first tap
        val showWords = startVisit()
        if (showWords) showHint = true
        kotlinx.coroutines.delay(700)
        pulse.snapTo(0f)
        pulse.animateTo(1f, tween(2000, easing = androidx.compose.animation.core.LinearEasing))
        if (showWords) {
            // The hint fades the whole time (see hintAlpha), so it is gone as the timeline ends
            kotlinx.coroutines.delay(3400)
            showHint = false
        }
    }
    var pan by remember(sorted) { mutableStateOf(Offset.Zero) }
    // When the person last pinched or dragged: an automatic move of the chart (bringing a station into view, recentring)
    // stops applying as soon as they touch it, instead of fighting the finger
    val touchedAt = remember { LongArray(1) }
    // Button, rail-tap and double-tap zooms animate (so labels fade smoothly); pinch and drag stay direct
    val zoomScope = rememberCoroutineScope()
    var zoomJob by remember { mutableStateOf<Job?>(null) }
    fun animateZoomTo(newZoom: Float, anchor: Offset, w: Float, h: Float) {
        zoomJob?.cancel()
        zoomJob = zoomScope.launch {
            animateZoom(zoom, pan, newZoom, anchor, w, h, CHART_PADDING_PX, CHART_PADDING_PX, ELEVATION_OVERSCROLL, { z -> elevationLowestPanY(z, h) }) { z, p -> zoom = z; pan = p }
        }
    }

    // Nothing selected: start on the lowest station that has a live reading, so the first view is not blank
    val currentStation = sorted.find { it.id == selectedStationId }
        ?: sorted.firstOrNull { knownReading(it.id) is ObservationResult.Reading }
        ?: sorted.firstOrNull()
    // Real NWS temperature for the selected station; null while loading or when it has no reading
    var live by remember(currentStation?.id) {
        mutableStateOf(currentStation?.let { knownReading(it.id) })
    }
    LaunchedEffect(currentStation?.id) {
        currentStation?.let {
            live = fetchReading(it)
        }
    }
    val tempF: Double? = (live as? ObservationResult.Reading)?.observation?.temperatureFahrenheit

    val skyTheme = remember(tempF) { skyThemeFor(tempF) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
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
        // The chart can only be dragged up so far: past this the sea would take more than MAX_WATER_SHARE of the height
        val waterLimit = { z: Float -> elevationLowestPanY(z, canvasH) }

        // A jump of the viewing angle while the bands are on (the axis switch) pivots: the chart folds down to the single line,
        // regroups the stations at the bottom of the fold, and unfolds again, so no frame shows the old grouping under the
        // new names. A small move (the angle slider) regroups at once, and with the single line showing there is nothing to
        // fold. [angleShown] is the angle the chart is drawn with; [bandAngle] is the one asked for.
        var shownAngle by remember { mutableStateOf(bandAngle) }
        var folding by remember { mutableStateOf(false) }
        // True from the bottom of a fold until the unfold has finished: the chart only re-centres on the station then if it
        // would otherwise be out of view, so a pivot does not slide the whole chart about
        var unfolding by remember { mutableStateOf(false) }
        LaunchedEffect(bandAngle) {
            if (bandAngle == shownAngle) {
                folding = false // asked to go back before the fold reached the bottom: unfold, nothing regrouped
                unfolding = false
                return@LaunchedEffect
            }
            if (layersOn && abs(bandAngle - shownAngle) >= AXIS_PIVOT_JUMP_DEGREES) {
                folding = true
                kotlinx.coroutines.delay((LAYERS_FADE_MS + AXIS_PIVOT_MARGIN_MS).toLong())
                unfolding = true
            }
            shownAngle = bandAngle
            folding = false
            if (unfolding) {
                kotlinx.coroutines.delay(LAYERS_FADE_MS.toLong())
                unfolding = false
            }
        }
        val angleShown = if (!layersOn && !folding) bandAngle else shownAngle
        // The bands as the chart draws them: on while the layers are on, except at the bottom of a fold
        val layersShown = layersOn && !folding

        // Layers: each station's band (all band 0 with the layers off, which is the plain profile), and
        // its pin's place on the page with the band's squash and lift
        // The switch fades in and out: the squash, lift, haze and colours follow layerMix, 0 (single line) to 1 (layers)
        val layerMix by animateFloatAsState(
            targetValue = if (layersShown) 1f else 0f,
            animationSpec = tween(LAYERS_FADE_MS, easing = FastOutSlowInEasing),
            label = "layerMix"
        )
        val bandsActive = layersShown || layerMix > 0.001f
        // Looking west (the west-east axis) the ocean is straight ahead, not in the foreground, so as the bands unfold it moves
        // from the bottom of the chart to the horizon. With the single line showing, or looking north, it stays at the bottom.
        val westShare = angleShown / BandAngle.WEST.toFloat()
        val seaMix = layerMix * westShare
        // Looking north, the far ranges are wooded, so the land seems to run to the horizon
        val landMix = layerMix * (1f - westShare)
        val landShare = 1f - westShare
        // Every station's band, always: the tags and the key use it in both views; the layout only when the
        // Elevation Bands are on (otherwise everything is in band 0, the plain profile)
        val stationBands = remember(sorted, angleShown) {
            DepthBands.byRank(sorted.map { DepthBands.depthKey(it.latitude, it.longitude, angleShown.toDouble()) })
        }
        val bandIndex = remember(sorted, bandsActive, stationBands) {
            if (bandsActive) stationBands else IntArray(sorted.size)
        }
        val layered = bandIndex.any { it > 0 }
        // The layer that holds the selected station is the active one; the others step back (null: no layers)
        val activeIdx = sorted.indexOfFirst { it.id == currentStation?.id }
        val activeBand: Int? = if (activeIdx >= 0) bandIndex[activeIdx] else null
        // The band of the selected station, for the key (in both views)
        val keyBand: Int? = if (activeIdx >= 0) stationBands[activeIdx] else null
        // A ripple runs along the line when the active layer changes (or the layers come on), not on the first look:
        // the visit's own ripple covers that
        var rippleArmed by remember { mutableStateOf(false) }
        var rippleAngle by remember { mutableStateOf(bandAngle) }
        LaunchedEffect(layersShown, activeBand) {
            // Sliding the angle regroups the stations and so changes the active band, over and over; only a jump (the axis
            // switch) or a new selection ripples
            val slid = bandAngle != rippleAngle && abs(bandAngle - rippleAngle) < AXIS_PIVOT_JUMP_DEGREES
            rippleAngle = bandAngle
            if (slid) return@LaunchedEffect
            if (!rippleArmed) {
                rippleArmed = true
                return@LaunchedEffect
            }
            if (!layersShown) return@LaunchedEffect
            kotlinx.coroutines.delay(LAYERS_FADE_MS / 2L)
            pulse.snapTo(0f)
            pulse.animateTo(1f, tween(1400, easing = androidx.compose.animation.core.LinearEasing))
        }
        // With the bands on, a station stands in its order across the line of sight over the chart's width (see [lateralPositions]);
        // with the single line it stands where it ranks by height
        val lateralFrac = remember(sorted, angleShown) { lateralPositions(sorted, angleShown) }
        // A station's base x and y (before the zoom and pan), with the layers [mix] of the way on (0 the single line, 1 the bands)
        fun pinXAt(idx: Int, mix: Float): Float = elevationPinX(idx, sorted.size, mix, lateralFrac, paddingPx, widthPx)
        fun pinBaseX(idx: Int): Float = pinXAt(idx, layerMix)
        fun pinYAt(idx: Int, mix: Float): Float = elevationPinY(
            sorted[idx].elevationMeters, maxElevation, bandIndex[idx], lateralFrac[idx], mix, angleShown, paddingPx, widthPx, heightPx
        )
        fun pinBaseY(idx: Int): Float = pinYAt(idx, layerMix)

        // Whether a pin (at base position [bx], [by]) is well inside the chart at the current zoom and pan: clear of the
        // edges, the tile row and the bottom
        fun inComfortableView(bx: Float, by: Float): Boolean {
            val vx = viewX(bx)
            val vy = viewY(by)
            val sideMargin = with(density) { 48.dp.toPx() }
            val topMargin = with(density) { (if (embedded) HOME_TILE_BAND + 24.dp else 40.dp).toPx() }
            val bottomMargin = with(density) { 40.dp.toPx() }
            return vx in sideMargin..(canvasW - sideMargin) && vy in topMargin..(canvasH - bottomMargin)
        }
        // The pan that puts a pin in the middle, within the same limit on the water: a low station in the middle would
        // otherwise leave half the screen as sea
        fun centringPan(bx: Float, by: Float): Offset = clampPan(
            Offset(-(bx - canvasW / 2f) * zoom, -(by - canvasH / 2f) * zoom),
            zoom, canvasW, canvasH, CHART_PADDING_PX, CHART_PADDING_PX, ELEVATION_OVERSCROLL, waterLimit
        )

        // Switching between the single line and the Elevation Bands moves every pin, so the chart eases over to put the
        // selected station back in the middle (not on the first look). An axis pivot is gentler: nothing moves on the
        // fold, and the unfold only re-centres when the station would end up out of view.
        var recenterArmed by remember { mutableStateOf(false) }
        androidx.compose.runtime.LaunchedEffect(layersShown) {
            if (!recenterArmed) {
                recenterArmed = true
                return@LaunchedEffect
            }
            val idx = sorted.indexOfFirst { it.id == currentStation?.id }
            if (idx < 0) return@LaunchedEffect
            val bx = pinXAt(idx, if (layersShown) 1f else 0f)
            val by = pinYAt(idx, if (layersShown) 1f else 0f)
            if (folding) return@LaunchedEffect
            if (unfolding && inComfortableView(bx, by)) return@LaunchedEffect
            zoomJob?.cancel()
            easePanTo(pan, centringPan(bx, by), LAYERS_FADE_MS, touchedAt) { pan = it }
        }

        // When the selected station changes (from the Stations list, the tile or the other view) and it is not in
        // view at this zoom and pan, ease the chart over so it is
        androidx.compose.runtime.LaunchedEffect(selectedStationId, sorted) {
            val idx = sorted.indexOfFirst { it.id == selectedStationId }
            if (idx < 0) return@LaunchedEffect
            val bx = pinBaseX(idx)
            val by = pinBaseY(idx)
            if (inComfortableView(bx, by)) return@LaunchedEffect
            zoomJob?.cancel()
            easePanTo(pan, centringPan(bx, by), BRING_INTO_VIEW_MS, touchedAt) { pan = it }
        }

        // When the viewing angle changes, the labels that were showing can stay showing, now at their stations' new places,
        // until the chart is zoomed or touched; then the labels are chosen afresh and fade in and out as usual (see [LabelHold])
        val labelHold = remember(sorted) { LabelHold(bandAngle) }
        var tagsFollowView by remember { mutableStateOf(TAGS_FOLLOW_VIEW) }  // the switch in the key bar changes it
        if (labelHold.angle != bandAngle) labelHold.angleChanged(bandAngle, zoom, touchedAt[0], followView = tagsFollowView)
        // Ended by a pinch, a drag or any change of zoom, but not by the chart easing itself over (the pivot re-centres it)
        if (labelHold.ids != null && (zoom != labelHold.zoom || touchedAt[0] != labelHold.touch)) labelHold.ids = null
        val held = labelHold.ids
        // Which labels have room at this zoom, and where (see [planElevationLabels])
        fun layOut(feetIds: Set<String>): List<PlacedLabel> = planElevationLabels(
            feetIds = feetIds,
            zoom = zoom,
            embedded = embedded,
            layersOn = layersOn,
            layersButtonTop = layersButtonTop,
            widthDp = widthDp,
            heightDp = heightDp,
            sliderShown = sliderShown,
            sliderTop = sliderTop,
            density = density,
            sorted = sorted,
            maxElevation = maxElevation,
            viewX = { viewX(it) },
            viewY = { viewY(it) },
            pinBaseX = { pinBaseX(it) },
            pinBaseY = { pinBaseY(it) },
            milesFromSelected = milesFromSelected,
            labelTexts = labelTexts,
            labelNames = labelNames,
            milesText = milesText,
            depthOf = { depthOf(it) },
            held = held,
            rankedLandmarks = rankedLandmarks,
            selectedStationId = selectedStationId
        )
        val firstPass = layOut(emptySet())
        val feetIds = if (milesFromSelected.isEmpty()) emptySet() else {
            val selected = sorted.find { it.id == selectedStationId }
            val others = firstPass.filter { it.hasRoom && it.station.id != selectedStationId }.map { it.station }
            if (selected == null) emptySet() else referenceStationIds(others, selected.elevationMeters)
        }
        val placedLabels = if (feetIds.isEmpty()) firstPass else layOut(feetIds)
        val placedIds = placedLabels.filter { it.hasRoom }.map { it.station.id }.toSet()
        labelHold.lastPlaced = placedIds
        // While the angle is moving the labels change as the stations regroup, so they fade slowly, and keep fading for a moment
        // after the slider stops, instead of popping in and out
        val turning = rememberTurning(bandAngle)
        val labelFades = rememberLabelFades(
            placedLabels.associate { it.station.id to if (it.hasRoom) 1f else 0f },
            fadeSeconds = if (turning) TURNING_LABEL_FADE_SECONDS else 0.18f
        )

        // Canvas drawing the dynamic sky, slope, shaded terrain, leader lines, and nodes
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // The pan, pinch and double-tap handlers live on this layer, not on the container around it: the slider, the dial and
                // the buttons are siblings above this layer, so a touch on them never reaches these handlers (as children of the
                // container they shared its touches, and a drag from a control could be taken by the chart)
                // Keyed to the data: zoom and pan are remembered per region, so when the region changes they are new state objects
                // and the handlers must restart to use them (with a fixed key they kept writing to the old ones and touch stopped working)
                .pointerInput(sorted) {
                    // Pinch to zoom in on the profile, drag to pan
                    detectTransformGestures { centroid, panChange, zoomChange, _ ->
                        touchedAt[0] = System.nanoTime()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val newZoom = (zoom * zoomChange).coerceIn(ELEVATION_MIN_ZOOM, MAX_MAP_ZOOM)
                        val waterLimit = { z: Float -> elevationLowestPanY(z, h) }
                        val anchored = panForZoom(pan, zoom, newZoom, centroid, w, h, CHART_PADDING_PX, CHART_PADDING_PX, ELEVATION_OVERSCROLL, waterLimit)
                        pan = clampPan(anchored + panChange, newZoom, w, h, CHART_PADDING_PX, CHART_PADDING_PX, ELEVATION_OVERSCROLL, waterLimit)
                        zoom = newZoom
                    }
                }
                .pointerInput(sorted) {
                    // Double-tap: zoom in on the spot, and back out once fully zoomed
                    detectTapGestures(onDoubleTap = { spot ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        if (zoom >= MAX_MAP_ZOOM * 0.99f) {
                            animateZoomTo(ELEVATION_MIN_ZOOM.coerceAtLeast(1f), Offset(w / 2f, h / 2f), w, h)
                        } else {
                            animateZoomTo((zoom * 2f).coerceAtMost(MAX_MAP_ZOOM), spot, w, h)
                        }
                    })
                }
                .elevationStationTaps(
                sorted = sorted,
                canvasW = canvasW,
                stationPosition = { Offset(viewX(pinBaseX(it)), viewY(pinBaseY(it))) },
                onSelectStation = onSelectStation,
                onLineTapped = onLineTapped,
                onHit = { showHint = false }
            )
        ) {
            val padding = CHART_PADDING_PX
            val w = size.width - (padding * 2)
            val h = size.height - (padding * 2)

            // 1. DYNAMIC SKY GRADIENT FILL: fills the open space above & to the left of the slope
            // The sky and the ground run well past the chart's edges, so the buffer beyond the first and last
            // stations (see ELEVATION_OVERSCROLL) shows the same scenery instead of the app backdrop
            val bleedLeft = -size.width
            val bleedRight = size.width * 2f
            val bleedTop = -size.height
            val skyPath = drawElevationSky(
                bleedLeft = bleedLeft, bleedRight = bleedRight, bleedTop = bleedTop, h = h, layered = layered, padding = padding,
                pinBaseX = { pinBaseX(it) }, pinBaseY = { pinBaseY(it) }, skyTheme = skyTheme, sorted = sorted,
                viewX = { viewX(it) }, viewY = { viewY(it) }
            )

            // 1b. The far scenery behind the profile (ranges, the sea at the horizon with its boats, the shore, trees)
            drawElevationFarScenery(
                bandIndex = bandIndex, embedded = embedded, h = h, landMix = landMix, padding = padding, pan = pan,
                pinBaseX = { pinBaseX(it) }, pinBaseY = { pinBaseY(it) }, regionId = regionId, ridgeCache = ridgeCache, seaMix = seaMix,
                skyPath = skyPath, skyTheme = skyTheme, skyline = skyline, sorted = sorted, treeMix = treeMix,
                viewX = { viewX(it) }, viewY = { viewY(it) }, zoom = zoom
            )

            // 2. The weather pictures in the sky
            drawWeatherVisuals(tempF, padding)

            // 3. Draw elevation terrain fill
            // Back to front: the far layer first, the nearest last
            for (band in DepthBands.COUNT - 1 downTo 0) {
                val members = sorted.indices.filter { bandIndex[it] == band }.sortedBy { pinBaseX(it) }
                if (members.isNotEmpty()) {
                    drawBandLayer(
                        members = members, band = band, activeBand = activeBand, angleShown = angleShown, bleedLeft = bleedLeft,
                        bleedRight = bleedRight, h = h, landShare = landShare, layerMix = layerMix, layered = layered,
                        maxElevation = maxElevation, padding = padding, pan = pan, pinBaseX = { pinBaseX(it) }, pinBaseY = { pinBaseY(it) },
                        stations = stations, terrain = terrain, terrainPicture = terrainPicture, viewX = { viewX(it) }, viewY = { viewY(it) },
                        zoom = zoom
                    )
                }
            }

            // The ocean: the bottom of the chart, below sea level (decoration; the heights above it are data)
            drawElevationForegroundSea(h = h, padding = padding, seaMix = seaMix, viewY = { viewY(it) })

            // Station points along the slope & leader lines for landmarks
            drawElevationStationPins(
                activeBand = activeBand, bandIndex = bandIndex, depthOf = { depthOf(it) }, dotGrowth = dotGrowth, layerMix = layerMix,
                pinBaseX = { pinBaseX(it) }, pinBaseY = { pinBaseY(it) }, placedIds = placedIds, pulse = pulse,
                selectedStationId = selectedStationId, skyTheme = skyTheme, sorted = sorted, viewX = { viewX(it) }, viewY = { viewY(it) }
            )
        }

        ElevationFishLayer(zoom = zoom, pan = pan, seaMix = seaMix, viewY = { viewY(it) }, paddingPx = paddingPx, heightPx = heightPx)

        // The hint, in the ocean: fades away after a few seconds or at the first tap on the line
        ElevationHint(hintAlpha = { hintAlpha.value }, heightDp = heightDp)

        if (SHOW_SKY_BADGE) ElevationSkyBadge(skyTheme, tempF, currentStation, cycleIds, sorted, expandT, embedded, onSelectStation)

        // Overlay Interactive City Landmark Labels along the curve. They are drawn back to front: the selected station's
        // label on top, then the labels of the active band, then the other bands from the front backwards, so the
        // nearer layer's tags sit over the farther one's, and the active band's over all of them.
        val labelsBackToFront = placedLabels.sortedByDescending { label ->
            val band = bandIndex[sorted.indexOfFirst { it.id == label.station.id }.coerceAtLeast(0)]
            when {
                label.station.id == selectedStationId -> 0
                band == activeBand -> 1
                else -> 2 + band
            }
        }
        // While the viewing angle is moving, the tags in the top half of the chart step back so the bands changing behind them can be
        // seen, and come back when the control is still
        val upperMute by animateFloatAsState(
            targetValue = if (turning && layersOn) 1f else 0f,
            animationSpec = tween(if (turning) 450 else 700),
            label = "upperLabelMute"
        )
        labelsBackToFront.forEach { label ->
            val station = label.station
            val isSelected = station.id == selectedStationId
            val muted = if (label.yDp < heightDp.value / 2f) 1f - (if (isSelected) UPPER_MUTE_SELECTED else UPPER_MUTE) * upperMute else 1f
            val fade = (labelFades[station.id] ?: 0f) * muted
            if (fade <= 0.01f) return@forEach
            val depth = depthOf(station.id)
            val stripe = layerColor(stationBands[sorted.indexOfFirst { it.id == station.id }.coerceAtLeast(0)], angleShown)

            ElevationStationTag(
                label = label,
                isSelected = isSelected,
                fade = fade,
                depth = depth,
                stripe = stripe,
                onSelect = { onSelectStation(station) }
            )
        }

        // The key to the bands, and a way to jump between them: in the bottom right corner, whatever the pan and zoom.
        // A tap picks the band's station nearest the middle of the chart; the chart eases over to it as for any selection.
        fun pickInBand(band: Int) {
            val middle = Offset(canvasW / 2f, canvasH / 2f)
            sorted.indices
                .filter { stationBands[it] == band }
                .minByOrNull { i ->
                    val spot = Offset(
                        viewX(pinBaseX(i)),
                        viewY(pinBaseY(i))
                    )
                    (spot - middle).getDistance()
                }
                ?.let { onSelectStation(sorted[it]) }
        }
        ElevationBandControls(
            angleShown = angleShown,
            bandAngle = bandAngle,
            layersOn = layersOn,
            keyBand = keyBand,
            hasStations = List(DepthBands.COUNT) { band -> stationBands.any { it == band } },
            onPick = ::pickInBand,
            onAngleChange = onAngleChange,
            onToggleLayers = onToggleLayers,
            notice = notice,
            widthDp = widthDp,
            layersButtonTop = layersButtonTop,
            tagsFollowView = tagsFollowView,
            onToggleTags = { tagsFollowView = !tagsFollowView; labelHold.ids = null }
        )

        // Floating zoom slider on the right edge, where the Topography map has it
        fun nearestLinePoint(): Offset {
            val middle = Offset(canvasW / 2f, canvasH / 2f)
            return sorted.indices
                .map { i ->
                    Offset(
                        viewX(pinBaseX(i)),
                        viewY(pinBaseY(i))
                    )
                }
                .minByOrNull { (it - middle).getDistance() } ?: middle
        }
        ElevationZoomControls(
            sliderCollapsible = sliderCollapsible,
            sliderShown = sliderShown,
            onSliderShownChange = setSliderShown,
            expandT = expandT,
            sliderTop = sliderTop,
            embedded = embedded,
            zoom = zoom,
            onZoomChange = { newZoom ->
                // Zoom about the point on the line nearest the middle of the view, not the (often empty) middle itself
                zoomJob?.cancel()
                pan = panForZoom(pan, zoom, newZoom, nearestLinePoint(), canvasW, canvasH, CHART_PADDING_PX, CHART_PADDING_PX, ELEVATION_OVERSCROLL, waterLimit)
                zoom = newZoom
            },
            onZoomStep = { newZoom -> animateZoomTo(newZoom, nearestLinePoint(), canvasW, canvasH) },
            minValue = ELEVATION_MIN_ZOOM
        )
    }
}
