package io.github.genesjc.climatefinder.domain

/** What is worth saying about a height: a landmark comparison, if any, and the rule-of-thumb cooling in degrees F. */
data class ElevationNote(val landmark: LandmarkComparison?, val coolerFahrenheit: Int)
