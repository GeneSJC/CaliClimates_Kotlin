package io.github.genesjc.climatefinder.domain

/** A landmark a height is compared with: [isNear] when within about 20% of its height, else the height is above it. */
data class LandmarkComparison(val name: String, val feet: Int, val isNear: Boolean)
