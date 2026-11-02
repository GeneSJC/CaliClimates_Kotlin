package io.github.genesjc.climatefinder.domain

/**
 * The direction the Elevation view looks across the region, as an angle from north toward west: 0 degrees looks north
 * (the north is at the back, the south in front), 90 degrees looks west (the west is at the back, the east in front).
 * The two values are the two ends of a continuous range, so a later control can offer the angles between them.
 */
enum class BandAxis(val degrees: Double) {
    NORTH_SOUTH(0.0),
    WEST_EAST(90.0);

    /** The other axis, for a switch between the two. */
    fun other(): BandAxis = if (this == NORTH_SOUTH) WEST_EAST else NORTH_SOUTH

    /** How far back a station is when looking along this axis: bigger is farther back (see [DepthBands.depthKey]). */
    fun depthKey(latitude: Double, longitude: Double): Double = DepthBands.depthKey(latitude, longitude, degrees)
}
