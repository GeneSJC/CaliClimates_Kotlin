package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import io.github.genesjc.climatefinder.ui.components.BAND_ARC_CENTRE_INSET_DP
import io.github.genesjc.climatefinder.ui.components.BAND_ARC_GAP_DP
import io.github.genesjc.climatefinder.ui.components.LAYERS_BUTTON_SIZE_DP
import io.github.genesjc.climatefinder.ui.components.BAND_ARC_SIZE_DP
import io.github.genesjc.climatefinder.ui.components.BandAngleArc
import io.github.genesjc.climatefinder.ui.components.BandsNotice
import io.github.genesjc.climatefinder.ui.components.BandCompass
import io.github.genesjc.climatefinder.ui.components.LAYER_LEGEND_HEIGHT_DP
import io.github.genesjc.climatefinder.ui.components.LayerLegend
import io.github.genesjc.climatefinder.ui.components.LayersToggleButton
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.BoxScope

/**
 * The Elevation Bands controls, at the chart's right and bottom edges whatever the pan and zoom: the key to the bands
 * (a tap on a band's segment calls [onPick] with that band), the compass and the dial that turn the viewing angle
 * ([bandAngle], shown as [angleShown] while it turns), the note about the bands button ([notice]) and the bands button.
 * [hasStations] says which bands have a station, [keyBand] which band holds the selected one.
 */
@Composable
internal fun BoxScope.ElevationBandControls(
    angleShown: Int,
    bandAngle: Int,
    layersOn: Boolean,
    keyBand: Int?,
    hasStations: List<Boolean>,
    onPick: (Int) -> Unit,
    onAngleChange: (Int) -> Unit,
    onToggleLayers: () -> Unit,
    notice: String?,
    widthDp: Dp,
    layersButtonTop: Dp,
    /** How the tags behave while the dial turns (see [TAGS_FOLLOW_VIEW]), and how to switch it; the switch is the key bar's first segment. */
    tagsFollowView: Boolean,
    onToggleTags: () -> Unit
) {
    val tagsToast = rememberTagsToast()
    TagsToast(tagsToast)
    LayerLegend(
        angle = angleShown,
        showReading = layersOn,
        activeBand = keyBand,
        hasStations = hasStations,
        alpha = 1f,
        onPick = onPick,
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = LEGEND_MARGIN_DP.dp, bottom = LEGEND_MARGIN_DP.dp),
        leading = { TagsSwitch(tagsFollowView, tagsToast, onToggleTags) }
    )
    if (layersOn) {
        BandCompass(
            angle = bandAngle,
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp, top = layersButtonTop)
        )
        BandAngleArc(
            angle = bandAngle,
            onAngleChange = onAngleChange,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = LEGEND_MARGIN_DP.dp, bottom = (LEGEND_MARGIN_DP + LAYER_LEGEND_HEIGHT_DP + BAND_ARC_GAP_DP).dp)
        )
    }
    // The note about the bands button sits above the key, to the left of the dial and the button, beside what it describes
    BandsNotice(
        message = notice,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(
                end = (LEGEND_MARGIN_DP + BAND_ARC_SIZE_DP + 8f).dp,
                bottom = (LEGEND_MARGIN_DP + LAYER_LEGEND_HEIGHT_DP + BAND_ARC_GAP_DP).dp
            )
            .widthIn(max = (widthDp.value - LEGEND_MARGIN_DP - BAND_ARC_SIZE_DP - 8f - 10f).dp)
    )
    // The bands button sits in the hole of the dial, above the key; with the bands off it stays there on its own
    LayersToggleButton(
        on = layersOn,
        onToggle = onToggleLayers,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(
                end = (LEGEND_MARGIN_DP + BAND_ARC_CENTRE_INSET_DP - LAYERS_BUTTON_SIZE_DP / 2f).dp,
                bottom = (LEGEND_MARGIN_DP + LAYER_LEGEND_HEIGHT_DP + BAND_ARC_GAP_DP + BAND_ARC_CENTRE_INSET_DP - LAYERS_BUTTON_SIZE_DP / 2f).dp
            )
    )
}
