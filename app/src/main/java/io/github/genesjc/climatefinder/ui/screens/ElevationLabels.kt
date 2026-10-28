package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlin.math.abs
import androidx.compose.ui.unit.dp
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation

// Labels are about this tall (dp) when checking for overlaps
internal const val LABEL_HEIGHT_DP = 18f

/** A label's spot (top-left corner in dp) and whether it has room at the current zoom. */
internal data class PlacedLabel(
    val station: MicroclimateStation,
    val text: String,
    val xDp: Float,
    val yDp: Float,
    val hasRoom: Boolean
)

/**
 * Ranks stations for labels, most useful first: each next pick is the station farthest along the
 * profile from those already picked, with airports (K-prefixed, recognizable cities) preferred.
 */
internal fun rankLandmarkIndexes(sorted: List<MicroclimateStation>): List<Int> {
    if (sorted.isEmpty()) return emptyList()
    fun bonus(i: Int) = if (sorted[i].id.startsWith("K")) 2f else 0f
    val chosen = mutableListOf(0)
    while (chosen.size < minOf(sorted.size, 60)) {
        val next = sorted.indices
            .filter { it !in chosen }
            .maxByOrNull { c -> chosen.minOf { kotlin.math.abs(c - it) }.toFloat() + bonus(c) }
            ?: break
        chosen.add(next)
    }
    return chosen
}
