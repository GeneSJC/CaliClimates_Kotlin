package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.ui.res.stringResource
import io.github.genesjc.climatefinder.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import io.github.genesjc.climatefinder.ui.text.elevationFeetText
import io.github.genesjc.climatefinder.ui.text.rememberFeetFormatter
import io.github.genesjc.climatefinder.ui.text.rememberMilesFormatter
import io.github.genesjc.climatefinder.domain.BandAngle
import io.github.genesjc.climatefinder.domain.BandAxis
import io.github.genesjc.climatefinder.domain.DepthBands
import io.github.genesjc.climatefinder.domain.DepthCue
import io.github.genesjc.climatefinder.domain.StationDistance
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.BlendMode
import io.github.genesjc.climatefinder.data.local.TerrainImage
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.climatefinder.data.remote.ObservationResult
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import io.github.genesjc.climatefinder.domain.model.TerrainType
import io.github.genesjc.climatefinder.ui.theme.LocalStateUsa
import io.github.genesjc.climatefinder.ui.components.MAX_MAP_ZOOM
import io.github.genesjc.climatefinder.ui.components.ZoomSlider
import io.github.genesjc.climatefinder.ui.components.MapText
import io.github.genesjc.climatefinder.ui.components.HOME_TILE_BAND
import io.github.genesjc.climatefinder.ui.components.BRING_INTO_VIEW_MS
import io.github.genesjc.climatefinder.ui.components.LayerColors
import io.github.genesjc.climatefinder.ui.components.hazeColor
import io.github.genesjc.climatefinder.ui.components.layerColor
import io.github.genesjc.climatefinder.ui.components.BAND_ANGLE_SLIDER_GAP_DP
import io.github.genesjc.climatefinder.ui.components.BAND_ANGLE_SLIDER_HEIGHT_DP
import io.github.genesjc.climatefinder.ui.components.BAND_ARC_SIZE_DP
import io.github.genesjc.climatefinder.ui.components.BandAngleArc
import io.github.genesjc.climatefinder.ui.components.BAND_COMPASS_SIZE_DP
import io.github.genesjc.climatefinder.ui.components.BandCompass
import io.github.genesjc.climatefinder.ui.components.BandAngleSlider
import io.github.genesjc.climatefinder.ui.components.BandAngleToast
import io.github.genesjc.climatefinder.ui.components.BAND_HEADING_GAP_DP
import io.github.genesjc.climatefinder.ui.components.BAND_HEADING_HEIGHT_DP
import io.github.genesjc.climatefinder.ui.components.BandHeadingStrip
import io.github.genesjc.climatefinder.ui.components.LAYER_LEGEND_HEIGHT_DP
import io.github.genesjc.climatefinder.ui.components.layerLegendWidthDp
import io.github.genesjc.climatefinder.ui.components.LayerLegend
import io.github.genesjc.climatefinder.ui.components.LayersToggleButton
import io.github.genesjc.climatefinder.ui.components.SliderToggleButton
import io.github.genesjc.climatefinder.ui.components.ms
import io.github.genesjc.climatefinder.ui.components.animateZoom
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import io.github.genesjc.climatefinder.ui.components.clampPan
import io.github.genesjc.climatefinder.ui.components.labelBudget
import io.github.genesjc.climatefinder.ui.components.rememberLabelFades
import io.github.genesjc.climatefinder.ui.components.panForZoom
import io.github.genesjc.climatefinder.ui.components.zoomDotGrowth

@Composable
internal fun ElevationProfileCanvas(
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
    /** The direction the bands look across the region, and the key's switch between the two. */
    bandAxis: BandAxis = BandAxis.NORTH_SOUTH,
    bandAngle: Int = bandAxis.degrees.toInt(),
    onAngleChange: (Int) -> Unit = {},
    onToggleAxis: () -> Unit = {},
    selectedStationId: String?,
    initialZoom: Float = 1f,
    onZoomChange: ((Float) -> Unit)? = null,
    sliderShownShared: Boolean? = null,
    onSliderShownChange: ((Boolean) -> Unit)? = null,
    onSelectStation: (MicroclimateStation) -> Unit
) {
    if (stations.isEmpty()) return

    val treeMix = LocalStateUsa.current.treeMix[regionId] ?: DEFAULT_TREE_MIX
    // Sort stations by elevation ascending (valley 5m -> ridge 1406m)
    val sorted = remember(stations) { stations.sortedBy { it.elevationMeters } }
    val maxElevation = remember(sorted) { (sorted.maxOfOrNull { it.elevationMeters } ?: 1500).toFloat() }
    // Stations ranked for labels; the first few are the ones the sky badge cycles through
    val rankedLandmarks = remember(sorted) { rankLandmarkIndexes(sorted) }
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
    val sliderTop = 56.dp + (if (embedded) HOME_TILE_BAND + 32.dp else 0.dp)
    // The layers button sits at the right, under Home's view buttons (or level with the expand toggle)
    val layersButtonTop = if (embedded) HOME_TILE_BAND + 62.dp else 56.dp

    // View transform: an even zoom (like a magnifying glass) about the centre plus a pan in pixels
    var zoom by remember(sorted) { mutableStateOf(initialZoom.coerceIn(1f, MAX_MAP_ZOOM)) }
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

    val skyTheme = remember(tempF) {
        when {
            tempF == null -> SkyAtmosphereTheme(
                colors = listOf(
                    Color(0xFF475569).copy(alpha = 0.40f),
                    Color(0xFF94A3B8).copy(alpha = 0.20f),
                    Color(0xFFE2E8F0).copy(alpha = 0.05f)
                ),
                conditionRes = R.string.reading_none,
                icon = null,
                accent = Color(0xFFCBD5E1),
                badgeBg = Color(0xFF1E293B)
            )
            tempF >= 84.0 -> SkyAtmosphereTheme(
                colors = listOf(
                    Color(0xFFC2410C).copy(alpha = 0.45f),
                    Color(0xFFF59E0B).copy(alpha = 0.22f),
                    Color(0xFFFEF08A).copy(alpha = 0.05f)
                ),
                conditionRes = R.string.sky_valley_heat,
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
                conditionRes = R.string.sky_fair_foothills,
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
                conditionRes = R.string.sky_temperate_ridge,
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
                conditionRes = R.string.sky_crisp_alpine,
                icon = "❄️",
                accent = Color(0xFFA78BFA),
                badgeBg = Color(0xFF1E1B4B)
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            // Keyed to the data: zoom and pan are remembered per region, so when the region changes they are new state objects
            // and the handlers must restart to use them (with a fixed key they kept writing to the old ones and touch stopped working)
            .pointerInput(sorted) {
                // Pinch to zoom in on the profile, drag to pan
                detectTransformGestures { centroid, panChange, zoomChange, _ ->
                    touchedAt[0] = System.nanoTime()
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val newZoom = (zoom * zoomChange).coerceIn(1f, MAX_MAP_ZOOM)
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
                        animateZoomTo(1f, Offset(w / 2f, h / 2f), w, h)
                    } else {
                        animateZoomTo((zoom * 2f).coerceAtMost(MAX_MAP_ZOOM), spot, w, h)
                    }
                })
            }
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
        val layerTopY = paddingPx + PROFILE_HEAD * heightPx
        val layerSeaY = paddingPx + (1f - SEA_FRACTION) * heightPx
        // The pin's place with the layers [mix] of the way on (0 the single line, 1 the Elevation Bands)
        fun pinYAt(idx: Int, mix: Float): Float {
            val plain = paddingPx + profileFrac(sorted[idx].elevationMeters / maxElevation) * heightPx
            // Turning the view toward west tilts the bands, their lines rising toward the left (the west) and the far bands tilting a
            // little less than the near ones, so the lines converge like lines in perspective: the bands seem to turn with the view
            val across = idx.toFloat() / (sorted.size - 1).coerceAtLeast(1) - 0.5f
            val tilt = BAND_TILT * (angleShown / BandAngle.WEST.toFloat()) * (1f - BAND_TILT_CONVERGE * bandIndex[idx]) * across * widthPx
            return plain + (DepthBands.layeredY(plain, bandIndex[idx], layerTopY, layerSeaY) - plain) * mix + tilt * mix
        }
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
            val bx = paddingPx + idx.toFloat() / (sorted.size - 1).coerceAtLeast(1) * widthPx
            val by = pinYAt(idx, if (layersShown) 1f else 0f)
            if (folding) return@LaunchedEffect
            if (unfolding && inComfortableView(bx, by)) return@LaunchedEffect
            zoomJob?.cancel()
            val from = pan
            val target = centringPan(bx, by)
            val startedAt = System.nanoTime()
            androidx.compose.animation.core.animate(0f, 1f, animationSpec = tween(LAYERS_FADE_MS, easing = FastOutSlowInEasing)) { v, _ ->
                if (touchedAt[0] < startedAt) pan = Offset(from.x + (target.x - from.x) * v, from.y + (target.y - from.y) * v)
            }
        }

        // When the selected station changes (from the Stations list, the tile or the other view) and it is not in
        // view at this zoom and pan, ease the chart over so it is
        androidx.compose.runtime.LaunchedEffect(selectedStationId, sorted) {
            val idx = sorted.indexOfFirst { it.id == selectedStationId }
            if (idx < 0) return@LaunchedEffect
            val bx = paddingPx + idx.toFloat() / (sorted.size - 1).coerceAtLeast(1) * widthPx
            val by = pinBaseY(idx)
            if (inComfortableView(bx, by)) return@LaunchedEffect
            zoomJob?.cancel()
            val from = pan
            val target = centringPan(bx, by)
            val startedAt = System.nanoTime()
            androidx.compose.animation.core.animate(0f, 1f, animationSpec = tween(BRING_INTO_VIEW_MS, easing = FastOutSlowInEasing)) { v, _ ->
                if (touchedAt[0] < startedAt) pan = Offset(from.x + (target.x - from.x) * v, from.y + (target.y - from.y) * v)
            }
        }

        // When the band axis switches, the labels that were showing stay showing, now at their stations' new places,
        // until the chart is zoomed or touched; then the labels are chosen afresh and fade in and out as usual
        val labelHold = remember(sorted) { LabelHold(bandAngle) }
        if (labelHold.angle != bandAngle) {
            // A jump (the axis switch) keeps the labels as they were through the pivot; a small move (the angle slider)
            // lays them out afresh at every angle, so they do not pile up as the stations regroup
            val jump = abs(bandAngle - labelHold.angle) >= AXIS_PIVOT_JUMP_DEGREES
            labelHold.angle = bandAngle
            labelHold.ids = if (jump) labelHold.lastPlaced else null
            labelHold.zoom = zoom
            labelHold.touch = touchedAt[0]
        }
        // Ended by a pinch, a drag or any change of zoom, but not by the chart easing itself over (the pivot re-centres it)
        if (labelHold.ids != null && (zoom != labelHold.zoom || touchedAt[0] != labelHold.touch)) labelHold.ids = null
        val held = labelHold.ids

        // Which labels have room at this zoom: more as the profile spreads out. Only the target (room
        // or no room) is decided here; each label's opacity eases toward it on its own. With a station selected, the
        // others show their distance and no height, except [feetIds]: a few reference points for the heights in between
        // (see [referenceStationIds]).
        fun layOut(feetIds: Set<String>): List<PlacedLabel> {
            val budget = labelBudget(LANDMARK_COUNT, zoom, 5f).toInt()
            val taken = mutableListOf(
                if (embedded) listOf(0f, 0f, widthDp.value, HOME_TILE_BAND.value) else listOf(0f, 0f, 0f, 0f),  // the floating station tile
                if (embedded) listOf(0f, HOME_TILE_BAND.value, 140f, HOME_TILE_BAND.value + 50f) else listOf(0f, 0f, 0f, 0f),  // Home's control row
                if (embedded) listOf(0f, HOME_TILE_BAND.value + 50f, 46f, HOME_TILE_BAND.value + 82f) else listOf(0f, 0f, 0f, 0f),  // the slider's arrow button
                if (embedded && !sliderShown) listOf(0f, 0f, 0f, 0f) else listOf(0f, sliderTop.value, 46f, sliderTop.value + (heightDp.value - sliderTop.value - 4f) * (if (embedded) ELEVATION_SLIDER_FRACTION else 1f)), // zoom slider (at the left)
                if (embedded) listOf(widthDp.value - 170f, HOME_TILE_BAND.value, widthDp.value, HOME_TILE_BAND.value + 56f) else listOf(0f, 0f, 0f, 0f),  // Home's view buttons
                listOf(widthDp.value - 126f, 52f, widthDp.value - 46f, 84f),  // expand toggle
                listOf(widthDp.value - 44f, layersButtonTop.value, widthDp.value, layersButtonTop.value + 36f),  // layers button
                if (layersOn && ANGLE_ARC) listOf(widthDp.value - 6f - BAND_COMPASS_SIZE_DP, layersButtonTop.value + 44f, widthDp.value, layersButtonTop.value + 44f + BAND_COMPASS_SIZE_DP) else listOf(0f, 0f, 0f, 0f),  // the compass
                listOf(widthDp.value - LEGEND_MARGIN_DP - layerLegendWidthDp(layersOn), heightDp.value - LEGEND_MARGIN_DP - LAYER_LEGEND_HEIGHT_DP, widthDp.value, heightDp.value),  // the bands key
                if (layersOn && ANGLE_ARC) {
                    val top = heightDp.value - LEGEND_MARGIN_DP - LAYER_LEGEND_HEIGHT_DP - BAND_ANGLE_SLIDER_GAP_DP - BAND_ARC_SIZE_DP
                    listOf(widthDp.value - LEGEND_MARGIN_DP - BAND_ARC_SIZE_DP, top, widthDp.value, top + BAND_ARC_SIZE_DP)  // the quarter dial
                } else listOf(0f, 0f, 0f, 0f),
                if (layersOn && !ANGLE_ARC) {
                    val top = heightDp.value - LEGEND_MARGIN_DP - LAYER_LEGEND_HEIGHT_DP - BAND_ANGLE_SLIDER_GAP_DP - BAND_ANGLE_SLIDER_HEIGHT_DP
                    listOf(widthDp.value - LEGEND_MARGIN_DP - layerLegendWidthDp(true), top, widthDp.value, top + BAND_ANGLE_SLIDER_HEIGHT_DP)  // the angle slider
                } else listOf(0f, 0f, 0f, 0f),
                if (layersOn && !ANGLE_ARC) {
                    val top = heightDp.value - LEGEND_MARGIN_DP - LAYER_LEGEND_HEIGHT_DP - BAND_ANGLE_SLIDER_GAP_DP - BAND_ANGLE_SLIDER_HEIGHT_DP - BAND_HEADING_GAP_DP - BAND_HEADING_HEIGHT_DP
                    listOf(widthDp.value - LEGEND_MARGIN_DP - layerLegendWidthDp(true), top, widthDp.value, top + BAND_HEADING_HEIGHT_DP)  // the heading strip
                } else listOf(0f, 0f, 0f, 0f),
                listOf(52f, 50f + (if (embedded) HOME_TILE_BAND.value else 0f), 222f, 97f + (if (embedded) HOME_TILE_BAND.value else 0f))  // weather badge
            )
            // The controls, badge and key: a label never goes over these unless it is the selected station's and has nowhere else
            val controls = taken.toList()
            // Null when the station is out of view; otherwise its label spot, marked with or without room. [force] lets the label
            // overlap other labels (never the controls); [overControls] also lets it overlap those, as a last resort.
            fun plan(idx: Int, allow: Boolean, force: Boolean, overControls: Boolean = false): PlacedLabel? {
                val station = sorted[idx]
                val ratioX = idx.toFloat() / (sorted.size - 1).coerceAtLeast(1)
                val elevRatio = station.elevationMeters / maxElevation
                val pinX = with(density) { viewX(paddingPx + ratioX * widthPx).toDp().value }
                val pinY = with(density) { viewY(pinBaseY(idx)).toDp().value }
                if (pinX < 0f || pinX > widthDp.value || pinY < 0f || pinY > heightDp.value) return null
                val miles = milesFromSelected[station.id]
                val fullText = when {
                    miles == null -> labelTexts[idx]
                    station.id in feetIds -> labelTexts[idx] + " " + milesText(miles)
                    else -> labelNames[idx] + " " + milesText(miles)
                }
                // High pins stay on the lowest height so the label stays inside the chart
                val maxLevel = if (elevRatio > 0.8f) 0 else 2
                val textScale = depthOf(station.id)?.let { DepthCue.textScale(it) } ?: 1f
                // The text's width at one line (the size setting included), and the label's real width: long ones wrap to two lines
                // (the size setting and the phone's font size both change how big the text is)
                val textSize = textScale * MapText.scale * density.fontScale
                fun lineWidthFor(text: String) = text.length * 5.4f * textSize + 17f
                fun widthFor(text: String) = minOf(lineWidthFor(text), LABEL_MAX_WIDTH_DP)
                fun heightFor(text: String) =
                    (if (lineWidthFor(text) > LABEL_MAX_WIDTH_DP) 2 else 1) * 9f * 1.15f * textSize + 8f
                // Estimated label width at 9sp; labels that would pass the right edge end at the pin instead
                fun leftFor(text: String) =
                    (if (pinX - 24f + widthFor(text) > widthDp.value * 0.97f) pinX - widthFor(text) else pinX - 24f).coerceAtLeast(2f)
                // A spot for this text, or null when there is no room
                fun tryPlace(text: String): PlacedLabel? {
                    val left = leftFor(text)
                    // The first spots that overlap another label but not the controls, and that overlap anything (for [force])
                    var overLabels: List<Float>? = null
                    var anywhere: List<Float>? = null
                    for (level in 0..maxLevel) {
                        // A two-line label grows upward, so its bottom edge stays where a one-line label's would be
                        val top = pinY - 26f - 16f * level - (heightFor(text) - LABEL_HEIGHT_DP)
                        if (top < 0f) continue
                        val right = left + widthFor(text)
                        val bottom = top + heightFor(text)
                        val spot = listOf(left, top, right, bottom)
                        if (taken.none { r -> left < r[2] && right > r[0] && top < r[3] && bottom > r[1] }) {
                            taken.add(spot)
                            return PlacedLabel(station, text, left, top, hasRoom = true)
                        }
                        if (force) {
                            if (overLabels == null && controls.none { r -> left < r[2] && right > r[0] && top < r[3] && bottom > r[1] }) overLabels = spot
                            if (anywhere == null) anywhere = spot
                        }
                    }
                    val forced = overLabels ?: if (overControls) anywhere else null
                    if (forced == null) return null
                    taken.add(forced)
                    return PlacedLabel(station, text, forced[0], forced[1], hasRoom = true)
                }
                if (allow) {
                    tryPlace(fullText)?.let { return it }
                    // No room for the longer text: the name alone (the position, size and fade still say the rest)
                    if (miles != null) tryPlace(labelNames[idx])?.let { return it }
                }
                val text = fullText
                val left = leftFor(text)
                return PlacedLabel(station, text, left, pinY - 26f, hasRoom = false)
            }
            val plans = mutableListOf<PlacedLabel>()
            var shown = 0
            for (idx in rankedLandmarks) {
                if (held != null) {
                    // Holding the labels from before the axis switched: those stations keep a label (in a free spot if
                    // there is one, else over others), and no other station gets one
                    val heldPlan: PlacedLabel? = if (sorted[idx].id in held) {
                        val free = plan(idx, allow = true, force = false)
                        if (free != null && free.hasRoom) free else plan(idx, allow = true, force = true)
                    } else {
                        plan(idx, allow = false, force = false)
                    }
                    if (heldPlan != null) plans.add(heldPlan)
                    continue
                }
                val p = plan(idx, allow = shown < budget, force = false) ?: continue
                if (p.hasRoom) shown++
                plans.add(p)
            }
            // The selected station always keeps its label
            val selectedIdx = sorted.indexOfFirst { it.id == selectedStationId }
            if (selectedIdx >= 0) {
                val existing = plans.indexOfFirst { it.station.id == selectedStationId }
                if (existing < 0 || !plans[existing].hasRoom) {
                    plan(selectedIdx, allow = true, force = true, overControls = true)?.let {
                        if (existing >= 0) plans[existing] = it else plans.add(it)
                    }
                }
            }
            return plans
        }
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
        var turning by remember { mutableStateOf(false) }
        var turningArmed by remember { mutableStateOf(false) }
        LaunchedEffect(bandAngle) {
            if (!turningArmed) {
                turningArmed = true
                return@LaunchedEffect
            }
            turning = true
            kotlinx.coroutines.delay(TURNING_LINGER_MS)
            turning = false
        }
        val labelFades = rememberLabelFades(
            placedLabels.associate { it.station.id to if (it.hasRoom) 1f else 0f },
            fadeSeconds = if (turning) TURNING_LABEL_FADE_SECONDS else 0.18f
        )

        // Canvas drawing the dynamic sky, slope, shaded terrain, leader lines, and nodes
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(sorted, canvasW) {
                    detectTapGestures { tapOffset ->
                        val padding = CHART_PADDING_PX
                        val w = size.width - (padding * 2)
                        val h = size.height - (padding * 2)

                        var closestStation: MicroclimateStation? = null
                        var closestDist = Float.MAX_VALUE

                        sorted.forEachIndexed { index, station ->
                            val x = viewX(padding + (index.toFloat() / (sorted.size - 1).coerceAtLeast(1)) * w)
                            val y = viewY(pinBaseY(index))

                            val dx = tapOffset.x - x
                            val dy = tapOffset.y - y
                            val dist = (dx * dx + dy * dy)
                            if (dist < closestDist && dist < 1800f) {
                                closestDist = dist
                                closestStation = station
                            }
                        }
                        closestStation?.let {
                            onSelectStation(it)
                            onLineTapped()
                            showHint = false
                        }
                    }
                }
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
            val skyPath = Path().apply {
                if (layered) {
                    // Layered: the ridges are drawn behind each band's ground, so the sky is the whole chart
                    moveTo(bleedLeft, bleedTop)
                    lineTo(bleedRight, bleedTop)
                    lineTo(bleedRight, size.height * 3f)
                    lineTo(bleedLeft, size.height * 3f)
                } else {
                    moveTo(bleedLeft, bleedTop)
                    lineTo(bleedRight, bleedTop)
                    lineTo(bleedRight, viewY(pinBaseY(sorted.lastIndex.coerceAtLeast(0))))
                    for (i in (sorted.size - 1) downTo 0) {
                        lineTo(viewX(padding + (i.toFloat() / (sorted.size - 1).coerceAtLeast(1)) * w), viewY(pinBaseY(i)))
                    }
                    lineTo(bleedLeft, viewY(pinBaseY(0)))
                }
                close()
            }

            drawPath(
                path = skyPath,
                brush = Brush.verticalGradient(
                    colors = skyTheme.colors,
                    startY = viewY(padding),
                    endY = viewY(padding + h * 0.85f)
                )
            )

            // 1b. Distant ranges, hills and trees behind the profile, for depth. Decoration: the three
            // farthest ranges take their outline from the region's real skyline (the highest ground,
            // looking north across the region); the nearer hills and trees are made up, the same every
            // time for a region. Each layer drifts slower than the profile when you pan and zoom, and
            // hazes toward the sky colour the farther away it is.
            val horizon = skyTheme.colors.last()
            val seed = regionId.hashCode()
            // The layers keep the size they have in the regular-height chart. When the chart is taller,
            // the extra height is shared out between them (the far ranges stay near the top, the near
            // hills sink toward the ground), so nothing stretches and the gaps fill with more layers.
            val ridgeHeights = ridgeCache.get(size.width, seed, skyline)
            val ridgeH = CHART_REGULAR_HEIGHT.toPx() - padding * 2
            val extra = (h - ridgeH).coerceAtLeast(0f)
            // The ocean at the horizon, behind the nearer hills; it drifts a little with the pan and zoom, like the far ranges
            val follow = 0.30f
            val horizonBase = padding + ridgeH * 0.55f
            val horizonY = size.height / 2f + (horizonBase - size.height / 2f) * (1f + (zoom - 1f) * follow) + pan.y * follow
            val depthPx = ridgeH * 0.5f * (1f + (zoom - 1f) * follow)
            // Looking between north and west the sea is on one side and the land on the other, with a slanted coast between
            // them: the coast sweeps in from the left as the angle turns west (seaMix 0: no sea, 1: the sea all across)
            val coastSlant = size.width * COAST_SLANT
            // How far the coast travels across the screen as the view turns (a little more than the width, so the shore's wander never
            // leaves land at the right edge when looking due west)
            val coastSweep = size.width * (1f + COAST_SLANT + 0.1f)
            val coastTopX = seaMix * coastSweep
            fun coastX(y: Float) = coastTopX - coastSlant * (y - horizonY) / size.height
            // The shore is not a straight line: it wanders in and out, headlands and bays, the same for a region every time
            val shorePhase = ((seed * 37) and 0xFF) / 40f
            fun shoreX(y: Float): Float {
                val t = (y - horizonY) / size.height
                return coastX(y) + size.width * 0.035f * (sin(t * 9f + shorePhase) + 0.6f * sin(t * 21f + shorePhase * 1.7f))
            }
            val shorePoints = List(SHORE_STEPS + 1) { i ->
                val y = horizonY + size.height * 1.6f * i / SHORE_STEPS
                Offset(shoreX(y), y)
            }
            val shorePath = Path().apply {
                moveTo(shorePoints.first().x, shorePoints.first().y)
                for (k in 1 until shorePoints.size) lineTo(shorePoints[k].x, shorePoints[k].y)
            }
            val seaPath = Path().apply {
                moveTo(-size.width, horizonY)
                for (p in shorePoints) lineTo(p.x, p.y)
                lineTo(-size.width, shorePoints.last().y)
                close()
            }
            // The hills slope down into the water over this much of the width, to the waterline at the shore
            val shoreSlopeW = size.width * 0.45f
            val shoreAtHorizon = shoreX(horizonY)
            // The far ranges are drawn on the land side only; the sea side is open water
            fun withLand(clipped: Boolean, block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) {
                if (clipped) clipPath(seaPath, clipOp = androidx.compose.ui.graphics.ClipOp.Difference, block = block) else block()
            }
            clipPath(skyPath) {
                if (seaMix > 0.01f) {
                    clipPath(seaPath) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFFA9D8EE), Color(0xFF5FA8D4), Color(0xFF2F7DB5)),
                                startY = horizonY,
                                endY = horizonY + depthPx
                            ),
                            topLeft = Offset(-size.width, horizonY),
                            size = androidx.compose.ui.geometry.Size(size.width * 4f, size.height * 3f)
                        )
                        drawLine(Color.White.copy(alpha = 0.6f), Offset(-size.width, horizonY), Offset(size.width * 3f, horizonY), strokeWidth = 2.5f)
                    }
                    // Sailboats on the water, so looking west is recognisable at a glance. Nobody tracks where they are, so they are
                    // placed on the screen, not in the chart: always in the water that is showing, between the top of the sea and the coast
                    // (the back band's ridge, the westernmost stations), above the labels, and fading out when too little water is showing,
                    // so they come and go the way a label does.
                    val backMembers = sorted.indices.filter { bandIndex[it] == DepthBands.COUNT - 1 }
                    if (backMembers.isNotEmpty()) {
                        val ridgeXs = backMembers.map { viewX(padding + it.toFloat() / (sorted.size - 1).coerceAtLeast(1) * w) }
                        val ridgeYs = backMembers.map { viewY(pinBaseY(it)) }
                        // The boats are part of the mural: each keeps its share of the water that is showing, so they slide across with the
                        // sea as the view turns, always on the water, and drift a little with the pan. The big one is the anchor; two small
                        // ones are further off, nearer the horizon.
                        fun coastYAt(x: Float): Float = when {
                            x <= ridgeXs.first() -> ridgeYs.first()
                            x >= ridgeXs.last() -> ridgeYs.last()
                            else -> {
                                val k = ridgeXs.indexOfLast { it <= x }
                                val t = (x - ridgeXs[k]) / (ridgeXs[k + 1] - ridgeXs[k])
                                ridgeYs[k] + (ridgeYs[k + 1] - ridgeYs[k]) * t
                            }
                        }
                        // Below the weather badge and the controls, above the key and the labels near the coast
                        val topSafe = ((if (embedded) HOME_TILE_BAND.value else 0f) + 110f).dp.toPx()
                        val waterTop = maxOf(horizonY + 14.dp.toPx(), topSafe)
                        val seed01 = ((seed * 31) and 0xFF) / 255f
                        // (share of the visible water across, share of the room down, size)
                        val boats = listOf(
                            Triple(0.40f + 0.15f * seed01, 0.6f, 1f),
                            Triple(0.16f + 0.08f * seed01, 0.10f, 0.5f),
                            Triple(0.74f - 0.08f * seed01, 0.26f, 0.62f)
                        )
                        for ((across, down, scale) in boats) {
                            val probeY = waterTop + 80.dp.toPx()
                            val sea = minOf(shoreX(probeY), size.width)
                            val coastY = coastYAt(across * sea)
                            val waterBottom = minOf(coastY - 110.dp.toPx(), size.height - 170.dp.toPx())
                            val room = waterBottom - waterTop
                            val boatY = waterTop + room * down
                            val boatX = across * minOf(shoreX(boatY), size.width) + pan.x * follow
                            // Fading in and out as the sea comes and goes at the left, and at the edges of the screen
                            val seaFade = ((minOf(shoreX(boatY), size.width) - 100.dp.toPx()) / 80.dp.toPx()).coerceIn(0f, 1f)
                            val edgeFade = (minOf(boatX, size.width - boatX) / 48.dp.toPx()).coerceIn(0f, 1f)
                            val boatAlpha = seaFade * edgeFade * (room / 60.dp.toPx()).coerceIn(0f, 1f)
                            if (boatAlpha > 0.01f) {
                                drawSailboat(
                                    x = boatX,
                                    y = boatY,
                                    unit = 3.2.dp.toPx() * scale * (1f + (zoom - 1f) * 0.3f),
                                    alpha = boatAlpha
                                )
                            }
                        }
                    }
                }
                RIDGE_LAYERS.forEachIndexed { layer, spec ->
                    // The three far ranges give way to the sea at the horizon: they are drawn on the land side of the coast
                    withLand(layer <= 2 && seaMix > 0.01f) {
                    val farFade = 1f
                    val steps = RIDGE_STEPS
                    val startX = -size.width
                    val endX = size.width * 2f
                    val points = ArrayList<Offset>(steps + 1)
                    val layerHeights = ridgeHeights[layer]
                    for (i in 0..steps) {
                        val bx = startX + (endX - startX) * i / steps
                        val height = layerHeights[i]
                        val by = padding + ridgeH * spec.base - ridgeH * spec.amplitude * height + spec.anchor * extra
                        // The three far ranges slide across with the coast as the view turns west, the farther the slower, so the
                        // scene pans as one picture with some depth
                        val slide = if (layer <= 2) seaMix * coastSweep * LAND_SLIDE[layer] else 0f
                        val lx = size.width / 2f + (bx - size.width / 2f) * (1f + (zoom - 1f) * spec.follow) + pan.x * spec.follow + slide
                        val ly = size.height / 2f + (by - size.height / 2f) * (1f + (zoom - 1f) * spec.follow) + pan.y * spec.follow
                        // Near the shore the far hills come down to the waterline, so they slope into the water
                        val slopedY = if (layer <= 2 && seaMix > 0.01f) {
                            val u = ((lx - shoreAtHorizon) / shoreSlopeW).coerceIn(0f, 1f)
                            horizonY + (ly - horizonY) * (u * u * (3f - 2f * u))
                        } else ly
                        points.add(Offset(lx, slopedY))
                    }
                    val ridge = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (k in 1 until points.size) lineTo(points[k].x, points[k].y)
                        lineTo(size.width * 4f, size.height * 3f)
                        lineTo(-size.width * 3f, size.height * 3f)
                        close()
                    }
                    // Darker at the ridge, hazing lighter below it, so the gap to the next layer reads as mist
                    // Looking north the land takes its own colours, green and golden, in place of the sky's haze
                    val skyTop = lerp(horizon, Color(0xFF334155), spec.depthShade)
                    val (landTop, landLow) = LAND_RIDGE_COLORS[layer.coerceAtMost(LAND_RIDGE_COLORS.lastIndex)]
                    val top = lerp(skyTop, landTop, landMix)
                    val low = lerp(lerp(skyTop, horizon, 0.5f), landLow, landMix)
                    val topY = points.minOf { it.y }
                    drawPath(
                        ridge,
                        brush = Brush.verticalGradient(
                            colors = listOf(top.copy(alpha = spec.alpha * farFade), low.copy(alpha = spec.alpha * 0.7f * farFade)),
                            startY = topY,
                            endY = topY + ridgeH * 0.45f * (1f + (zoom - 1f) * spec.follow)
                        )
                    )
                    // Looking north the middle and near far ranges carry small trees, fading in with the bands
                    if ((layer == 1 || layer == 2) && landMix > 0.01f) {
                        drawTrees(
                            points = points,
                            mix = treeMix,
                            sizePx = (4f + layer * 1.5f).dp.toPx() * (1f + (zoom - 1f) * spec.follow),
                            color = lerp(top, Color(0xFF0F2F2A), 0.55f).copy(alpha = (spec.alpha + 0.15f).coerceAtMost(0.95f) * landMix),
                            seed = seed + layer * 101,
                            xMin = -40f,
                            xMax = size.width + 40f
                        )
                    }
                    if (spec.treeSizeDp > 0f) {
                        val treeScale = 1f + (zoom - 1f) * spec.follow
                        drawTrees(
                            points = points,
                            mix = treeMix,
                            sizePx = spec.treeSizeDp.dp.toPx() * treeScale,
                            color = lerp(top, Color(0xFF0F2F2A), 0.55f).copy(alpha = (spec.alpha + 0.15f).coerceAtMost(0.95f)),
                            seed = seed + layer * 101,
                            xMin = -40f,
                            xMax = size.width + 40f
                        )
                    }
                    }
                    if (layer == 2 && seaMix > 0.01f) {
                        // The shore: shallow water and foam on the sea side, a strip of sand and a few trees on the land side
                        clipPath(seaPath) {
                            drawPath(shorePath, Color(0xFFBFE6EF).copy(alpha = 0.55f), style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                            drawPath(shorePath, Color.White.copy(alpha = 0.8f), style = Stroke(width = 3.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))
                        }
                        clipPath(seaPath, clipOp = androidx.compose.ui.graphics.ClipOp.Difference) {
                            drawPath(shorePath, Color(0xFFE6D8A6), style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                            for (row in 0..3) {
                                val ry = horizonY + (0.04f + 0.17f * row) * size.height
                                val rx = shoreX(ry)
                                drawTrees(
                                    points = listOf(Offset(rx, ry), Offset(rx + 120.dp.toPx(), ry)),
                                    mix = treeMix,
                                    sizePx = (4f + 1.8f * row).dp.toPx(),
                                    color = Color(0xFF3F6B3A).copy(alpha = 0.9f),
                                    seed = seed + 500 + row,
                                    xMin = rx + 9.dp.toPx(),
                                    xMax = rx + 95.dp.toPx()
                                )
                            }
                        }
                    }
                }
            }

            // 2. DYNAMIC WEATHER VISUALS (Clouds / Sun corona in the upper-left quadrant)
            if (tempF == null) {
                // No live reading: plain sky, no weather visuals
            } else if (tempF >= 84.0) {
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
            fun drawLayer(members: List<Int>, band: Int) {
                val terrainPath = Path()
                val linePath = Path()
                // The ground runs on below the chart's bottom edge: the ocean covers it there, except looking west
                val floor = viewY(size.height - padding) + size.height
                val leftEdge = bleedLeft
                val rightEdge = bleedRight

                members.forEachIndexed { k, index ->
                    val x = viewX(padding + (index.toFloat() / (sorted.size - 1).coerceAtLeast(1)) * w)
                    val y = viewY(pinBaseY(index))
                    if (k == 0) {
                        // A layer runs level out to the chart's edge beyond its first and last stations
                        terrainPath.moveTo(leftEdge, floor)
                        terrainPath.lineTo(leftEdge, y)
                        terrainPath.lineTo(x, y)
                        linePath.moveTo(leftEdge, y)
                        linePath.lineTo(x, y)
                    } else {
                        terrainPath.lineTo(x, y)
                        linePath.lineTo(x, y)
                    }
                }
                val lastY = viewY(pinBaseY(members.last()))
                terrainPath.lineTo(rightEdge, lastY)
                linePath.lineTo(rightEdge, lastY)
                terrainPath.lineTo(rightEdge, floor)
                terrainPath.close()

                // The ground: tinted by height with the same ramp as the Topography map (data), so a given
                // height has the same colour in both views
                val groundTop = viewY(padding)
                val groundBottom = viewY(padding + h)
                drawPath(
                    path = terrainPath,
                    brush = Brush.verticalGradient(
                        colorStops = (0..10).map { step ->
                            val fraction = step / 10f
                            // The height drawn at this share of the chart (clamped: above the top and below sea level)
                            val ratio = (1f - (fraction - PROFILE_HEAD) / (1f - PROFILE_HEAD - SEA_FRACTION)).coerceIn(0f, 1f)
                            fraction to heightTint(ratio * maxElevation)
                        }.toTypedArray(),
                        startY = groundTop,
                        endY = groundBottom
                    )
                )

                // A faint relief texture from the region's own terrain picture (decoration, not data). It
                // drifts slower than the profile when you pan and zoom, which gives the ground some depth.
                // (with the bands on, only the front layer and the active one: it is the costliest part of a frame)
                if (terrain != null && (!layered || band == 0 || band == activeBand)) {
                    val b = terrain.bounds
                    val lonSpan = b.maxLon - b.minLon
                    val latSpan = b.maxLat - b.minLat
                    // The part of the picture that holds the stations
                    val sLeft = ((stations.minOf { it.longitude } - b.minLon) / lonSpan).coerceIn(0.0, 1.0)
                    val sRight = ((stations.maxOf { it.longitude } - b.minLon) / lonSpan).coerceIn(0.0, 1.0)
                    val sTop = ((b.maxLat - stations.maxOf { it.latitude }) / latSpan).coerceIn(0.0, 1.0)
                    val sBottom = ((b.maxLat - stations.minOf { it.latitude }) / latSpan).coerceIn(0.0, 1.0)
                    val img = terrain.image
                    val srcX = (sLeft * img.width).toInt()
                    val srcY = (sTop * img.height).toInt()
                    val srcW = ((sRight - sLeft) * img.width).toInt().coerceAtLeast(1)
                    val srcH = ((sBottom - sTop) * img.height).toInt().coerceAtLeast(1)
                    val follow = 0.6f
                    val scale = 1f + (zoom - 1f) * follow
                    // Big enough to cover the whole chart, whatever its shape
                    val cover = maxOf(size.width * 1.6f / srcW, size.height * 1.4f / srcH)
                    val dstW = srcW * cover * scale
                    val dstH = srcH * cover * scale
                    val dstX = size.width / 2f - dstW / 2f + pan.x * follow
                    val dstY = size.height / 2f - dstH / 2f + pan.y * follow
                    clipPath(terrainPath) {
                        drawImage(
                            image = img,
                            srcOffset = IntOffset(srcX, srcY),
                            srcSize = IntSize(srcW, srcH),
                            dstOffset = IntOffset(dstX.roundToInt(), dstY.roundToInt()),
                            dstSize = IntSize(dstW.roundToInt(), dstH.roundToInt()),
                            alpha = 0.5f,
                            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                            blendMode = BlendMode.Multiply
                        )
                    }
                }

                // The farther layers are hazed toward the sky, and their ridge lines fainter and thinner
                // The ground's tint is the band's look on this axis: on west-east the front band wears the grey-blue haze
                // and the back band the plain ground
                val haze = hazeColor(band, angleShown)
                if (haze.alpha > 0f) drawPath(terrainPath, haze.copy(alpha = haze.alpha * layerMix))
                // The layers that do not hold the selected station step back: a wash over the ground, fainter lines
                val inactive = activeBand != null && band != activeBand
                if (inactive) drawPath(terrainPath, lerp(LayerWashColor, LayerWashColorNorth, landShare).copy(alpha = DepthBands.INACTIVE_WASH * layerMix))
                val lineFade = (1f + (DepthBands.lineFade(band) - 1f) * layerMix) *
                    (if (inactive) 1f - (1f - DepthBands.INACTIVE_FADE) * layerMix else 1f)
                // The single line is always the first colour; the bands take the axis's colours as they unfold
                val lineColor = lerp(LayerColors[0], layerColor(band, angleShown), layerMix)
                // A lit edge along the profile, like the sun catching the ridge
                drawPath(
                    path = linePath,
                    color = Color.White.copy(alpha = 0.30f * lineFade),
                    style = Stroke(width = 10f, cap = StrokeCap.Round)
                )
                // Top ridge line
                drawPath(
                    path = linePath,
                    color = lineColor.copy(alpha = lineFade),
                    // The active layer's line is a little heavier
                    style = Stroke(width = 3f * (1f - 0.15f * band) * (if (inactive) 1f else 1f + 0.25f * layerMix), cap = StrokeCap.Round)
                )
            }
            // Back to front: the far layer first, the nearest last
            for (band in DepthBands.COUNT - 1 downTo 0) {
                val members = sorted.indices.filter { bandIndex[it] == band }
                if (members.isNotEmpty()) drawLayer(members, band)
            }

            // The ocean: the bottom of the chart, below sea level (decoration; the heights above it are data)
            val seaY = viewY(padding + (1f - SEA_FRACTION) * h)
            val oceanBottom = maxOf(size.height, viewY(size.height - padding) + padding)
            val foregroundSea = 1f - seaMix  // fades out as the sea moves to the horizon
            if (seaY < oceanBottom && foregroundSea > 0.01f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF7CC3E6), Color(0xFF3B8EC4), Color(0xFF1F5F99)),
                        startY = seaY,
                        endY = oceanBottom
                    ),
                    topLeft = Offset(0f, seaY),
                    size = androidx.compose.ui.geometry.Size(size.width, oceanBottom - seaY),
                    alpha = foregroundSea
                )
                // A light line at the waterline and a few soft wave crests
                drawLine(Color.White.copy(alpha = 0.55f * foregroundSea), Offset(0f, seaY), Offset(size.width, seaY), strokeWidth = 2.5f)
                val crest = Color.White.copy(alpha = 0.22f * foregroundSea)
                for (row in 1..3) {
                    val wy = seaY + (oceanBottom - seaY) * row / 4.5f
                    var wx = -(row * 37f) % 90f
                    while (wx < size.width) {
                        drawLine(crest, Offset(wx, wy), Offset(wx + 26f, wy), strokeWidth = 2f, cap = StrokeCap.Round)
                        wx += 90f
                    }
                }
            }

            // Station points along the slope & leader lines for landmarks
            sorted.forEachIndexed { index, station ->
                val x = viewX(padding + (index.toFloat() / (sorted.size - 1).coerceAtLeast(1)) * w)
                val y = viewY(pinBaseY(index))
                val isSelected = station.id == selectedStationId

                val isLandmark = station.id in placedIds

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
                        radius = 18f * dotGrowth,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = skyTheme.accent,
                        radius = 14f * dotGrowth,
                        center = Offset(x, y),
                        style = Stroke(width = 2.5f)
                    )
                }

                val depth = depthOf(station.id)
                val dotCue = depth?.let { DepthCue.dotScale(it) } ?: 1f
                val bandMute = if (activeBand != null && bandIndex[index] != activeBand) 1f - (1f - DepthBands.INACTIVE_FADE) * layerMix else 1f
                val fadeCue = (depth?.let { DepthCue.alpha(it) } ?: 1f) * bandMute
                drawCircle(
                    color = Color.White.copy(alpha = fadeCue),
                    radius = (if (isSelected) 8f else (if (isLandmark) 6.5f else 4.5f)) * dotGrowth * dotCue,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = (if (isSelected) skyTheme.accent else pinColor).copy(alpha = fadeCue),
                    radius = (if (isSelected) 6f else (if (isLandmark) 4.8f else 3.2f)) * dotGrowth * dotCue,
                    center = Offset(x, y)
                )

                // The one-off pulse: a ring that runs up the line, low dots first
                val pulseValue = pulse.value
                if (pulseValue > 0f && pulseValue < 1f) {
                    val ring = (pulseValue * (sorted.size + 8f) - index) / 8f
                    // With layers on, the ripple runs along the active layer only
                    if (ring in 0f..1f && (activeBand == null || bandIndex[index] == activeBand)) {
                        drawCircle(
                            color = Color.White.copy(alpha = (1f - ring) * 0.85f),
                            radius = (9f + 26f * ring) * dotGrowth,
                            center = Offset(x, y),
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }
        }

        // Three fish swimming in the ocean (decoration). They are drawn in their own layer, so only it
        // redraws as they move, and they follow the ocean when the chart is zoomed or panned.
        // The swim is stepped about 30 times a second (not every frame), stands still while the chart is being
        // panned or zoomed and for a moment after, and stays still when the system's animations are turned off.
        var fishTime by remember { mutableStateOf(0f) }
        var chartMoving by remember { mutableStateOf(true) }
        val fishContext = androidx.compose.ui.platform.LocalContext.current
        val animationsOn = remember {
            android.provider.Settings.Global.getFloat(
                fishContext.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            ) > 0f
        }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            // Each change of zoom or pan marks the chart as moving until it has been still for a little while
            androidx.compose.runtime.snapshotFlow { zoom to pan }.collectLatest {
                chartMoving = true
                kotlinx.coroutines.delay(600)
                chartMoving = false
            }
        }
        androidx.compose.runtime.LaunchedEffect(animationsOn) {
            if (!animationsOn) return@LaunchedEffect
            androidx.compose.runtime.snapshotFlow { chartMoving }.collectLatest { moving ->
                if (!moving) {
                    var last = System.nanoTime()
                    while (true) {
                        kotlinx.coroutines.delay(FISH_STEP_MS)
                        val now = System.nanoTime()
                        fishTime = (fishTime + (now - last) / 1e9f / FISH_LOOP_SECONDS) % 1f
                        last = now
                    }
                }
            }
        }
        // The fish live in the foreground water, so they fade out with it when the sea moves to the horizon (drawn in
        // their own layer above the chart, they would swim over the land in front of a sea at the horizon)
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 1f - seaMix }) {
            val seaTop = viewY(paddingPx + (1f - SEA_FRACTION) * heightPx)
            val seaBottom = maxOf(size.height, viewY(size.height - paddingPx) + paddingPx)
            val band = seaBottom - seaTop
            if (seaTop < size.height && band > 40.dp.toPx()) {
                clipRect(top = maxOf(0f, seaTop), bottom = size.height) {
                    for (fish in FISH) {
                        val length = fish.lengthDp.dp.toPx()
                        val travel = size.width + 2f * length
                        val phase = (fishTime * fish.cycles + fish.offset) % 1f
                        val cx = if (fish.rightward) phase * travel - length else size.width + length - phase * travel
                        val wobble = kotlin.math.sin((fishTime * 60f * fish.cycles + fish.offset) * 2f * Math.PI.toFloat())
                        val cy = seaTop + band * fish.depth + wobble * 4.dp.toPx()
                        drawFish(cx, cy, length, fish.color, fish.rightward, wobble)
                    }
                }
            }
        }

        // The hint, in the ocean: fades away after a few seconds or at the first tap on the line
        if (hintAlpha.value > 0.01f) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xCC0F172A),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = heightDp * 0.045f + 6.dp)
                    .graphicsLayer { alpha = hintAlpha.value }
            ) {
                Text(
                    text = stringResource(R.string.elevation_hint_tap_line),
                    color = Color.White,
                    // A third smaller than before; fixed, not tied to the font slider
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }

        // 4. Atmospheric Sky Micro-Display in the blank space to the left of the graph, below the expand toggle
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = skyTheme.badgeBg.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, skyTheme.accent.copy(alpha = 0.45f)),
            modifier = Modifier
                .offset(x = 52.dp + 5.dp * expandT, y = 50.dp + (if (embedded) HOME_TILE_BAND else 0.dp))
                .clickable {
                    // Tap to cycle landmarks
                    if (cycleIds.isNotEmpty()) {
                        val currentIndex = cycleIds.indexOfFirst { it == currentStation?.id }
                        val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % cycleIds.size else 0
                        val nextId = cycleIds[nextIndex]
                        sorted.find { it.id == nextId }?.let { onSelectStation(it) }
                    }
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                skyTheme.icon?.let { Text(text = it, fontSize = ms(14f)) }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tempF?.let { stringResource(R.string.sky_temp_f, it.toInt()) } ?: stringResource(R.string.sky_temp_missing),
                            fontSize = ms(11f),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentStation?.name?.let { if (it.length > 26) it.take(25) + "\u2026" else it } ?: "",
                            fontSize = ms(9f),
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1
                        )
                    }
                    Text(
                        text = stringResource(R.string.sky_condition_line, stringResource(skyTheme.conditionRes), currentStation?.let { elevationFeetText(it.elevationMeters) }.orEmpty()),
                        fontSize = ms(8f),
                        color = skyTheme.accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

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
            // A far station's text is fainter, but its tag stays solid
            val depthFade = depth?.let { DepthCue.alpha(it) } ?: 1f
            val stripe = layerColor(stationBands[sorted.indexOfFirst { it.id == station.id }.coerceAtLeast(0)], angleShown)

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    // Nearly solid, so the ridges and ground behind never show through the tag
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f * depthFade)
                ),
                modifier = Modifier
                    .offset(x = label.xDp.dp, y = label.yDp.dp)
                    .widthIn(max = LABEL_MAX_WIDTH_DP.dp)
                    .alpha(fade)
                    .clickable { onSelectStation(station) }
            ) {
                Text(
                    text = label.text,
                    maxLines = 2,
                    fontSize = ms(9f * (depth?.let { DepthCue.textScale(it) } ?: 1f)),
                    // A tight line height (the theme's default is far taller than this small text), so a wrapped tag stays compact
                    lineHeight = ms(9f * (depth?.let { DepthCue.textScale(it) } ?: 1f) * 1.15f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = depthFade),
                    // A stripe down the tag's left edge in its band's colour (see the key at the bottom right)
                    modifier = Modifier
                        .drawBehind { drawRect(stripe, size = Size(6.dp.toPx(), size.height)) }
                        .padding(start = 11.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                )
            }
        }

        // The key to the bands, and a way to jump between them: in the bottom right corner, whatever the pan and zoom.
        // A tap picks the band's station nearest the middle of the chart; the chart eases over to it as for any selection.
        fun pickInBand(band: Int) {
            val middle = Offset(canvasW / 2f, canvasH / 2f)
            sorted.indices
                .filter { stationBands[it] == band }
                .minByOrNull { i ->
                    val spot = Offset(
                        viewX(paddingPx + i.toFloat() / (sorted.size - 1).coerceAtLeast(1) * widthPx),
                        viewY(pinBaseY(i))
                    )
                    (spot - middle).getDistance()
                }
                ?.let { onSelectStation(sorted[it]) }
        }
        LayerLegend(
            angle = angleShown,
            requestedAxis = bandAxis,
            onToggleAxis = onToggleAxis,
            showAxisSwitch = layersOn,
            activeBand = keyBand,
            hasStations = List(DepthBands.COUNT) { band -> stationBands.any { it == band } },
            alpha = 1f,
            onPick = ::pickInBand,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = LEGEND_MARGIN_DP.dp, bottom = LEGEND_MARGIN_DP.dp)
        )
        if (layersOn && ANGLE_ARC) {
            BandCompass(
                angle = bandAngle,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp, top = layersButtonTop + 44.dp)
            )
            BandAngleArc(
                angle = bandAngle,
                onAngleChange = onAngleChange,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = LEGEND_MARGIN_DP.dp, bottom = (LEGEND_MARGIN_DP + LAYER_LEGEND_HEIGHT_DP + BAND_ANGLE_SLIDER_GAP_DP).dp)
            )
        }
        if (layersOn && !ANGLE_ARC) {
            BandHeadingStrip(
                angle = bandAngle,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = LEGEND_MARGIN_DP.dp,
                        bottom = (LEGEND_MARGIN_DP + LAYER_LEGEND_HEIGHT_DP + BAND_ANGLE_SLIDER_GAP_DP + BAND_ANGLE_SLIDER_HEIGHT_DP + BAND_HEADING_GAP_DP).dp
                    )
                    .width(layerLegendWidthDp(true).dp)
            )
        }
        // The direction note sits just above the key, to the left of the dial, clear of the bands it describes
        BandAngleToast(
            angle = bandAngle,
            layersOn = layersOn,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = (LEGEND_MARGIN_DP + (if (ANGLE_ARC) BAND_ARC_SIZE_DP else 0f) + 8f).dp,
                    bottom = (LEGEND_MARGIN_DP + LAYER_LEGEND_HEIGHT_DP + BAND_ANGLE_SLIDER_GAP_DP +
                        (if (ANGLE_ARC) 0f else BAND_ANGLE_SLIDER_HEIGHT_DP + BAND_HEADING_GAP_DP + BAND_HEADING_HEIGHT_DP + BAND_ANGLE_SLIDER_GAP_DP)).dp
                )
                .widthIn(max = (widthDp.value - LEGEND_MARGIN_DP - (if (ANGLE_ARC) BAND_ARC_SIZE_DP else 0f) - 8f - 10f).dp)
        )
        if (layersOn && !ANGLE_ARC) {
            BandAngleSlider(
                angle = bandAngle,
                onAngleChange = onAngleChange,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = LEGEND_MARGIN_DP.dp, bottom = (LEGEND_MARGIN_DP + LAYER_LEGEND_HEIGHT_DP + BAND_ANGLE_SLIDER_GAP_DP).dp)
                    .width(layerLegendWidthDp(true).dp)
            )
        }
        LayersToggleButton(
            on = layersOn,
            onToggle = onToggleLayers,
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 8.dp, top = layersButtonTop)
        )

        // Floating zoom slider on the right edge, where the Topography map has it
        fun nearestLinePoint(): Offset {
            val middle = Offset(canvasW / 2f, canvasH / 2f)
            return sorted.indices
                .map { i ->
                    Offset(
                        viewX(paddingPx + i.toFloat() / (sorted.size - 1).coerceAtLeast(1) * widthPx),
                        viewY(pinBaseY(i))
                    )
                }
                .minByOrNull { (it - middle).getDistance() } ?: middle
        }
        // The arrow button under the menu button: up while the slider shows (tap to tuck it away), down when hidden
        if (sliderCollapsible) {
            SliderToggleButton(
                shown = sliderShown,
                onToggle = { setSliderShown(!sliderShown) },
                modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp + 5.dp * expandT, top = 50.dp + HOME_TILE_BAND)
            )
        }
        // For now the slider sets the type size instead of the zoom (see MapText)
        if (sliderCollapsible && !sliderShown) {
            // hidden: pinch and double-tap still zoom
        } else if (MapText.SLIDER_SETS_FONT) {
            ZoomSlider(
                value = MapText.scale,
                onValueChange = { MapText.scale = it },
                minValue = MapText.MIN_SCALE,
                maxValue = MapText.MAX_SCALE,
                caption = stringResource(R.string.zoom_font_caption),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = sliderTop, start = 4.dp + 5.dp * expandT, bottom = 4.dp)
                    .width(40.dp)
                    // Another 20% shorter than Topography's on Home (so the chart's bottom-left corner stays clear); the top stays under the arrow button
                .fillMaxHeight(if (embedded) ELEVATION_SLIDER_FRACTION else 1f)
            )
        } else ZoomSlider(
            value = zoom,
            onValueChange = { newZoom ->
                // Zoom about the point on the line nearest the middle of the view, not the (often empty) middle itself
                zoomJob?.cancel()
                pan = panForZoom(pan, zoom, newZoom, nearestLinePoint(), canvasW, canvasH, CHART_PADDING_PX, CHART_PADDING_PX, ELEVATION_OVERSCROLL, waterLimit)
                zoom = newZoom
            },
            onStep = { newZoom -> animateZoomTo(newZoom, nearestLinePoint(), canvasW, canvasH) },
            // At the left here (the chart has no use for that side); its top stays put, the bottom follows the chart
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = sliderTop, start = 4.dp + 5.dp * expandT, bottom = 4.dp)
                .width(40.dp)
                // Another 20% shorter than Topography's on Home (so the chart's bottom-left corner stays clear); the top stays under the arrow button
                .fillMaxHeight(if (embedded) ELEVATION_SLIDER_FRACTION else 1f)
        )
    }
}

/** Which stations' labels to keep showing after the band axis switches, until the chart is zoomed or touched. */
private class LabelHold(var angle: Int) {
    var ids: Set<String>? = null
    var zoom = 1f
    var touch = 0L
    var lastPlaced: Set<String> = emptySet()
}

/**
 * The lowest vertical pan the chart allows at [zoom] on a chart [canvasH] tall: any lower and the sea would take more
 * than [MAX_WATER_SHARE] of the height. Dragging the chart up stops here, which leaves a clear stopping point with the
 * water and the fish in view.
 */
internal fun elevationLowestPanY(zoom: Float, canvasH: Float): Float {
    val heightPx = canvasH - 2 * CHART_PADDING_PX
    val seaY = CHART_PADDING_PX + (1f - SEA_FRACTION) * heightPx
    return canvasH * (1f - MAX_WATER_SHARE) - canvasH / 2f - (seaY - canvasH / 2f) * zoom
}
