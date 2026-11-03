package io.github.genesjc.climatefinder.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One text scale for everything on the Home map and the station card, so the type size can be tuned in one
 * place. Sizes are written as the base size in sp passed through [ms]; the scale multiplies them all.
 */
object MapText {
    /**
     * TEMPORARY, while the right type size is being chosen on a real device: true makes the vertical slider on
     * the map set [scale] instead of the zoom (zoom is by pinch and double-tap only). Set it back to false to
     * return the slider to zoom.
     */
    const val SLIDER_SETS_FONT = false

    const val MIN_SCALE = 0.8f
    const val MAX_SCALE = 2.5f
    const val DEFAULT_SCALE = 1.5f

    var scale by mutableFloatStateOf(DEFAULT_SCALE)
}

/**
 * On Home the station tile floats over the top of the map, so the map's own controls (maximize, compass, view
 * buttons, slider) and its drawing start this far below the map's top edge.
 */
val HOME_TILE_BAND: Dp = 89.dp

/** A base text size in sp, multiplied by the shared [MapText.scale]. */
fun ms(base: Float): TextUnit = (base * MapText.scale).sp
