package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.ui.res.stringResource
import io.github.genesjc.climatefinder.R
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.genesjc.climatefinder.ui.components.ZoomSlider
import io.github.genesjc.climatefinder.ui.components.MapText
import io.github.genesjc.climatefinder.ui.components.HOME_TILE_BAND
import io.github.genesjc.climatefinder.ui.components.SliderToggleButton
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.BoxScope

/**
 * The chart's zoom controls at its left edge: the small arrow button that tucks the slider away on Home, and the zoom
 * slider. [onZoomChange] is a drag of the slider, [onZoomStep] a step of its buttons (animated). While the slider is
 * hidden, pinch and double-tap still zoom.
 */
@Composable
internal fun BoxScope.ElevationZoomControls(
    sliderCollapsible: Boolean,
    sliderShown: Boolean,
    onSliderShownChange: (Boolean) -> Unit,
    expandT: Float,
    sliderTop: Dp,
    embedded: Boolean,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    onZoomStep: (Float) -> Unit,
    /** How far the slider (and so the zoom) can go out: the chart can be zoomed out below 1. */
    minValue: Float = 1f
) {
    // The arrow button under the menu button: up while the slider shows (tap to tuck it away), down when hidden
    if (sliderCollapsible) {
        SliderToggleButton(
            shown = sliderShown,
            onToggle = { onSliderShownChange(!sliderShown) },
            modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp + 5.dp * expandT, top = (if (embedded && VIEW_CHIP_IN_CONTROLS) 12.dp + CONTROLS_GAP + 30.dp else 50.dp) + HOME_TILE_BAND)
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
        onValueChange = onZoomChange,
        onStep = onZoomStep,
        minValue = minValue,
        // At the left here (the chart has no use for that side); its top stays put, the bottom follows the chart
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(top = sliderTop, start = 4.dp + 5.dp * expandT, bottom = 4.dp)
            .width(40.dp)
            // Another 20% shorter than Topography's on Home (so the chart's bottom-left corner stays clear); the top stays under the arrow button
            .fillMaxHeight(if (embedded) ELEVATION_SLIDER_FRACTION else 1f)
    )
}
