package io.github.genesjc.microclimatefinder.domain

import kotlin.math.roundToInt

/**
 * The one place that turns an elevation into words. The data stays in metres; everything the user reads is in
 * feet, with a plain-language band (Foothills, Mountains and so on).
 */
object Elevation {
    private const val FEET_PER_METER = 3.28084

    fun feet(meters: Int): Int = (meters * FEET_PER_METER).roundToInt()

    /** "1,115 ft" */
    fun formatFeet(meters: Int): String = "%,d ft".format(feet(meters))

    /** "1,115 ft · Foothills" */
    fun describe(meters: Int): String = "${formatFeet(meters)} · ${band(meters)}"

    /** "66–9,678 ft" */
    fun rangeFeet(minMeters: Int, maxMeters: Int): String =
        "%,d–%,d ft".format(feet(minMeters), feet(maxMeters))

    /** The plain-language band for a height. Bands are by feet: 100, 500, 1,500, 3,500 and 6,000. */
    fun band(meters: Int): String = when (val ft = feet(meters)) {
        in Int.MIN_VALUE until 100 -> "Near sea level"
        in 100 until 500 -> "Lowlands"
        in 500 until 1_500 -> "Foothills"
        in 1_500 until 3_500 -> "Hills"
        in 3_500 until 6_000 -> "Mountains"
        else -> "High country"
    }
}
