package io.github.genesjc.microclimatefinder.domain.model

/**
 * Generalized California outline as (longitude, latitude) pairs, shared by map screens that have
 * no region-specific artwork. Coarse on purpose: a coastline of about 20 points.
 */
object CaliforniaGeography {

    /** Pacific coast from the Oregon border (north) to the Mexico border (south). */
    val COAST: List<Pair<Double, Double>> = listOf(
        -124.21 to 42.00, -124.41 to 40.44, -123.65 to 38.80, -123.00 to 38.00,
        -122.51 to 37.77, -122.20 to 37.00, -121.85 to 36.80, -121.90 to 36.30,
        -120.85 to 35.30, -120.47 to 34.45, -119.70 to 34.40, -119.10 to 34.10,
        -118.85 to 34.02, -118.40 to 33.74, -117.60 to 33.38, -117.25 to 32.72,
        -117.12 to 32.53
    )

    /** Land border from the Mexico coast, east and north around Nevada, to the Oregon coast. */
    val LAND_BORDER: List<Pair<Double, Double>> = listOf(
        -117.12 to 32.53, -114.72 to 32.72, -114.63 to 35.00, -120.00 to 39.00,
        -120.00 to 42.00, -124.21 to 42.00
    )
}
