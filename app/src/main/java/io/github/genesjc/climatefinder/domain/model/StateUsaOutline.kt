package io.github.genesjc.climatefinder.domain.model

/** A state's own outline as (longitude, latitude) pairs, for the maps that have no region artwork. */
data class StateUsaOutline(
    /** The sea coast, from the northern border to the southern one. */
    val coast: List<Pair<Double, Double>>,
    /** The land border, from the southern coast end round to the northern coast end. */
    val landBorder: List<Pair<Double, Double>>
)
