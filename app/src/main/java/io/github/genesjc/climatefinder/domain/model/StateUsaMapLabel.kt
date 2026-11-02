package io.github.genesjc.climatefinder.domain.model

import androidx.annotation.StringRes

/** A name drawn on the hand-drawn picker map: its string resource and where it sits. */
data class StateUsaMapLabel(
    @StringRes val textRes: Int,
    val lon: Float,
    val lat: Float,
    /** A bigger, heavier name (for the neighbour that matters most). */
    val emphasis: Boolean = false
)
