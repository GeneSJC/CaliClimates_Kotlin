package io.github.genesjc.climatefinder.config

import androidx.annotation.StringRes

/** A name drawn on the hand-drawn picker map: its string resource and where it sits. */
data class StateUsaMapLabel(
    @StringRes val textRes: Int,
    val lon: Float,
    val lat: Float,
    /** A bigger, heavier name (for the neighbour that matters most). */
    val emphasis: Boolean = false,
    /** For a region's name: the chip starts just inside the region's left border at this latitude (the longitude is then not used), instead of being centred on the point. */
    val startInside: Boolean = false
)
