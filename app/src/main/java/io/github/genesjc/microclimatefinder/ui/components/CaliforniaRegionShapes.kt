package io.github.genesjc.microclimatefinder.ui.components

/**
 * The six regions of the California picker as (lon, lat) polygons that share their boundary points,
 * so the small icon on the picker button can be drawn the same way as the picker's map. The picker
 * map draws the same boundaries.
 */
internal val CALIFORNIA_REGION_SHAPES: Map<String, List<Pair<Float, Float>>> = run {
    val tahoe = -120.00f to 39.00f
    val northValleyMid = -120.80f to 39.15f
    val northBayValley = -121.60f to 39.20f
    val northBayMid = -122.80f to 38.95f
    val coastMendoSonoma = -123.65f to 38.80f

    val bayValley1 = -121.20f to 38.50f
    val bayValley2 = -121.15f to 37.95f
    val bayValley3 = -121.30f to 37.50f
    val bayValleyCoast = -121.50f to 37.00f
    val bayCoastMid = -121.80f to 36.95f
    val coastMontereyBay = -121.85f to 36.80f

    val coastValley1 = -120.70f to 36.20f
    val coastValley2 = -120.10f to 35.50f
    val coastValley3 = -119.50f to 35.00f
    val coastValleyLa = -119.00f to 34.80f
    val coastLaMid = -119.30f to 34.45f
    val coastVentura = -119.10f to 34.10f

    val valleyLa1 = -118.45f to 35.05f
    val valleyLa2 = -117.80f to 35.35f
    val valleyLa3 = -117.20f to 35.80f
    val nevadaValleyLa = -115.80f to 36.00f

    val coastSanOnofre = -117.60f to 33.38f
    val laSd1 = -117.10f to 33.45f
    val laSd2 = -116.30f to 33.35f
    val laSd3 = -115.50f to 33.20f
    val coloradoRiverMid = -114.60f to 33.30f

    mapOf(
        "northern_california" to listOf(
            coastMendoSonoma, -124.41f to 40.44f, -124.21f to 42.00f, -120.00f to 42.00f,
            tahoe, northValleyMid, northBayValley, northBayMid
        ),
        "bay_area_sacramento" to listOf(
            coastMendoSonoma, northBayMid, northBayValley, bayValley1, bayValley2, bayValley3,
            bayValleyCoast, bayCoastMid, coastMontereyBay, -122.20f to 37.00f, -122.51f to 37.77f, -123.00f to 38.00f
        ),
        "central_valley" to listOf(
            tahoe, northValleyMid, northBayValley, bayValley1, bayValley2, bayValley3, bayValleyCoast,
            coastValley1, coastValley2, coastValley3, coastValleyLa, valleyLa1, valleyLa2, valleyLa3,
            nevadaValleyLa, -117.20f to 37.00f, -118.60f to 38.00f
        ),
        "central_coast" to listOf(
            coastMontereyBay, bayCoastMid, bayValleyCoast, coastValley1, coastValley2, coastValley3,
            coastValleyLa, coastLaMid, coastVentura, -119.70f to 34.40f, -120.47f to 34.45f,
            -120.85f to 35.30f, -121.90f to 36.30f
        ),
        "los_angeles" to listOf(
            coastVentura, coastLaMid, coastValleyLa, valleyLa1, valleyLa2, valleyLa3, nevadaValleyLa,
            -114.63f to 35.00f, coloradoRiverMid, laSd3, laSd2, laSd1, coastSanOnofre,
            -118.40f to 33.74f, -118.85f to 34.02f
        ),
        "san_diego" to listOf(
            coastSanOnofre, laSd1, laSd2, laSd3, coloradoRiverMid, -114.72f to 32.72f,
            -117.12f to 32.53f, -117.25f to 32.72f
        )
    )
}
