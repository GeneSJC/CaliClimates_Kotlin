package io.github.genesjc.climatefinder.ui.screens

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
