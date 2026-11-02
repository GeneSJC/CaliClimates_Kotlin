package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlin.math.abs
import androidx.compose.ui.unit.dp
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation

// Labels are about this tall (dp) when checking for overlaps
internal const val LABEL_HEIGHT_DP = 18f

// The bands key sits this far (dp) from the chart's right and bottom edges
internal const val LEGEND_MARGIN_DP = 10f

// A label wider than this (dp) wraps to a second line
internal const val LABEL_MAX_WIDTH_DP = 124f

/** A label's spot (top-left corner in dp) and whether it has room at the current zoom. */
internal data class PlacedLabel(
    val station: MicroclimateStation,
    val text: String,
    val xDp: Float,
    val yDp: Float,
    val hasRoom: Boolean
)

/**
 * Stations that are shown first whenever they are in view, ahead of the rest: the region's featured stations and the airports
 * (K-prefixed, which stand for the recognizable cities, such as Sacramento, Oakland and San Jose in the Bay Area).
 */
internal fun isMajorStation(station: MicroclimateStation): Boolean = station.isFeatured || station.id.startsWith("K")

/**
 * Ranks stations for labels, most useful first: the region's landmark stations ([landmarkIds]) first, then the major stations (see
 * [isMajorStation]), then the rest, each next pick
 * being the station farthest along the profile from those already picked, with airports preferred among the rest.
 */
internal fun rankLandmarkIndexes(sorted: List<MicroclimateStation>, landmarkIds: List<String> = emptyList()): List<Int> {
    if (sorted.isEmpty()) return emptyList()
    // The region's own list of landmark stations goes before everything, in its order
    fun bonus(i: Int): Float {
        val listed = landmarkIds.indexOf(sorted[i].id)
        val first = if (listed >= 0) 100_000f - listed else 0f
        return first + (if (isMajorStation(sorted[i])) 1000f else 0f) + (if (sorted[i].id.startsWith("K")) 2f else 0f)
    }
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

/**
 * Which of the visible labels also show their height, as reference points for the ones without. [visible] are the
 * labels that fit (the selected station left out). On each side of the selected height: the second from the far end
 * (or the only one, when there is just one), and the middle one when the side has [MIDDLE_REFERENCE_MIN] or more.
 */
internal fun referenceStationIds(visible: List<MicroclimateStation>, selectedMeters: Int): Set<String> {
    fun side(stations: List<MicroclimateStation>): List<String> {
        // Nearest the selected height first, the far end last
        val far = stations.size
        if (far == 0) return emptyList()
        val ids = mutableListOf(stations[(far - 2).coerceAtLeast(0)].id)
        if (far >= MIDDLE_REFERENCE_MIN) ids.add(stations[far / 2].id)
        return ids
    }
    val below = visible.filter { it.elevationMeters < selectedMeters }.sortedByDescending { it.elevationMeters }
    val above = visible.filter { it.elevationMeters > selectedMeters }.sortedBy { it.elevationMeters }
    return (side(below) + side(above)).toSet()
}

/** A side of the selected height with this many labels or more gets a middle reference as well. */
internal const val MIDDLE_REFERENCE_MIN = 6

/** Which stations' labels to keep showing after the band axis switches, until the chart is zoomed or touched. */
internal class LabelHold(var angle: Int) {
    var ids: Set<String>? = null
    var zoom = 1f
    var touch = 0L
    var lastPlaced: Set<String> = emptySet()
}
