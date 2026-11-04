package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.foundation.layout.height
import io.github.genesjc.climatefinder.domain.DepthCue
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import io.github.genesjc.climatefinder.ui.components.MapText
import io.github.genesjc.climatefinder.ui.components.HOME_TILE_BAND
import io.github.genesjc.climatefinder.ui.components.BAND_ARC_CENTRE_INSET_DP
import io.github.genesjc.climatefinder.ui.components.BAND_ARC_GAP_DP
import io.github.genesjc.climatefinder.ui.components.LAYERS_BUTTON_SIZE_DP
import io.github.genesjc.climatefinder.ui.components.BAND_ARC_SIZE_DP
import io.github.genesjc.climatefinder.ui.components.BAND_COMPASS_SIZE_DP
import io.github.genesjc.climatefinder.ui.components.LAYER_LEGEND_HEIGHT_DP
import io.github.genesjc.climatefinder.ui.components.layerLegendWidthDp
import io.github.genesjc.climatefinder.ui.components.labelBudget
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

/**
 * Which stations get a label, and where. More labels have room as the chart is zoomed in (the budget grows with [zoom]); the
 * landmark stations come first ([rankedLandmarks]), then the rest, and each label is put in the first free spot above its pin,
 * clear of the controls at the chart's edges. Only the target (room or no room) is decided here; each label's opacity eases
 * toward it on its own. With a station selected, the others show their distance and no height, except [feetIds]: a few
 * reference points for the heights in between (see [referenceStationIds]).
 *
 * The positions come from the chart's own geometry: [viewX] and [viewY] turn a chart position into a screen position at the
 * current zoom and pan, [pinBaseX] and [pinBaseY] give a station's chart position. [held], when not null, are the stations whose
 * labels are kept as they were (none else get one). The selected station always keeps its label.
 */
internal fun planElevationLabels(
    feetIds: Set<String>,
    zoom: Float,
    embedded: Boolean,
    layersOn: Boolean,
    layersButtonTop: Dp,
    widthDp: Dp,
    heightDp: Dp,
    sliderShown: Boolean,
    sliderTop: Dp,
    density: Density,
    sorted: List<MicroclimateStation>,
    maxElevation: Float,
    viewX: (Float) -> Float,
    viewY: (Float) -> Float,
    pinBaseX: (Int) -> Float,
    pinBaseY: (Int) -> Float,
    milesFromSelected: Map<String, Int>,
    labelTexts: List<String>,
    labelNames: List<String>,
    milesText: (Int) -> String,
    depthOf: (String) -> Float?,
    held: Set<String>?,
    rankedLandmarks: List<Int>,
    selectedStationId: String?
): List<PlacedLabel> {
    val budget = labelBudget(LANDMARK_COUNT, zoom, 5f).toInt()
    val taken = mutableListOf(
        if (embedded) listOf(0f, 0f, widthDp.value, HOME_TILE_BAND.value) else listOf(0f, 0f, 0f, 0f),  // the floating station tile
        if (embedded) listOf(0f, HOME_TILE_BAND.value, if (VIEW_CHIP_IN_CONTROLS) 200f else 140f, HOME_TILE_BAND.value + (if (VIEW_CHIP_IN_CONTROLS) CONTROLS_GAP.value + 12f + 32f else 50f)) else listOf(0f, 0f, 0f, 0f),  // Home's controls: the arrow button and the Stations row
        if (embedded && !VIEW_CHIP_IN_CONTROLS) listOf(0f, HOME_TILE_BAND.value + 50f, 46f, HOME_TILE_BAND.value + 82f) else listOf(0f, 0f, 0f, 0f),  // the slider's arrow button (on the control row with the chip)
        if (embedded && !sliderShown) listOf(0f, 0f, 0f, 0f) else listOf(0f, sliderTop.value, 46f, sliderTop.value + (heightDp.value - sliderTop.value - 4f) * (if (embedded) ELEVATION_SLIDER_FRACTION else 1f)), // zoom slider (at the left)
        if (embedded) listOf(widthDp.value - 170f, HOME_TILE_BAND.value + (if (VIEW_CHIP_IN_CONTROLS) CONTROLS_GAP.value + 12f else 0f), widthDp.value, HOME_TILE_BAND.value + (if (VIEW_CHIP_IN_CONTROLS) CONTROLS_GAP.value + 12f + VIEW_CHIP_HEIGHT.value else 56f)) else listOf(0f, 0f, 0f, 0f),  // Home's view chip (or buttons)
        listOf(widthDp.value - 126f, 52f, widthDp.value - 46f, 84f),  // expand toggle
        if (layersOn && VIEW_CHIP_IN_CONTROLS) {
            // The compass sits left of the dial, level with it, above the key
            val right = widthDp.value - LEGEND_MARGIN_DP - BAND_ARC_SIZE_DP - 10f
            val bottom = heightDp.value - LEGEND_MARGIN_DP - LAYER_LEGEND_HEIGHT_DP - BAND_ARC_GAP_DP
            listOf(right - BAND_COMPASS_SIZE_DP, bottom - BAND_COMPASS_SIZE_DP, right, bottom)
        } else if (layersOn) listOf(widthDp.value - 6f - BAND_COMPASS_SIZE_DP, layersButtonTop.value, widthDp.value, layersButtonTop.value + BAND_COMPASS_SIZE_DP) else listOf(0f, 0f, 0f, 0f),  // the compass
        listOf(widthDp.value - LEGEND_MARGIN_DP - layerLegendWidthDp(layersOn), heightDp.value - LEGEND_MARGIN_DP - LAYER_LEGEND_HEIGHT_DP, widthDp.value, heightDp.value),  // the bands key
        if (layersOn) {
            val top = heightDp.value - LEGEND_MARGIN_DP - LAYER_LEGEND_HEIGHT_DP - BAND_ARC_GAP_DP - BAND_ARC_SIZE_DP
            listOf(widthDp.value - LEGEND_MARGIN_DP - BAND_ARC_SIZE_DP, top, widthDp.value, top + BAND_ARC_SIZE_DP)  // the quarter dial
        } else {
            // The bands button on its own where the dial's hole is
            val centreX = widthDp.value - LEGEND_MARGIN_DP - BAND_ARC_CENTRE_INSET_DP
            val centreY = heightDp.value - LEGEND_MARGIN_DP - LAYER_LEGEND_HEIGHT_DP - BAND_ARC_GAP_DP - BAND_ARC_CENTRE_INSET_DP
            listOf(centreX - LAYERS_BUTTON_SIZE_DP / 2f, centreY - LAYERS_BUTTON_SIZE_DP / 2f, centreX + LAYERS_BUTTON_SIZE_DP / 2f, centreY + LAYERS_BUTTON_SIZE_DP / 2f)
        },
        listOf(52f, 50f + (if (embedded) HOME_TILE_BAND.value else 0f), 222f, 97f + (if (embedded) HOME_TILE_BAND.value else 0f))  // weather badge
    )
    // The controls, badge and key: a label never goes over these unless it is the selected station's and has nowhere else
    val controls = taken.toList()
    // Null when the station is out of view; otherwise its label spot, marked with or without room. [force] lets the label
    // overlap other labels (never the controls); [overControls] also lets it overlap those, as a last resort.
    fun plan(idx: Int, allow: Boolean, force: Boolean, overControls: Boolean = false): PlacedLabel? {
        val station = sorted[idx]
        val elevRatio = station.elevationMeters / maxElevation
        val pinX = with(density) { viewX(pinBaseX(idx)).toDp().value }
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
