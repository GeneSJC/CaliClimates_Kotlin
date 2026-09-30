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

    /** A well-known place and its height in feet (approximate). */
    private class Landmark(val name: String, val feet: Int)

    // Approximate heights, for comparisons in plain words
    private val LANDMARKS: Map<String, List<Landmark>> = mapOf(
        "bay_area_sacramento" to listOf(
            Landmark("Twin Peaks", 925), Landmark("Mt. Tamalpais", 2_570), Landmark("Mt. Diablo", 3_850),
            Landmark("Mt. Hamilton", 4_210), Landmark("Lake Tahoe", 6_225)
        ),
        "los_angeles" to listOf(
            Landmark("Griffith Observatory", 1_130), Landmark("the Hollywood Sign", 1_600),
            Landmark("Mt. Wilson", 5_710), Landmark("Big Bear Lake", 6_750), Landmark("Mt. Baldy", 10_060)
        ),
        "san_diego" to listOf(
            Landmark("Mt. Soledad", 820), Landmark("Julian", 4_200), Landmark("Cuyamaca Peak", 6_510)
        ),
        "central_coast" to listOf(
            Landmark("Morro Rock", 580), Landmark("the Pinnacles High Peaks", 3_300),
            Landmark("Junipero Serra Peak", 5_860)
        ),
        "central_valley" to listOf(
            Landmark("Fresno", 300), Landmark("the Yosemite Valley floor", 4_000), Landmark("Half Dome", 8_840)
        ),
        "northern_california" to listOf(
            Landmark("Redding", 500), Landmark("Lassen Peak", 10_460), Landmark("Mt. Shasta", 14_180)
        )
    )

    /** Rule of thumb: the air cools about 3.5 \u00B0F for every 1,000 ft of height. */
    fun coolerFahrenheit(meters: Int): Int = (feet(meters) / 1000.0 * 3.5).roundToInt()

    /**
     * A sentence about the height: a landmark comparison (only when a landmark is within about 20% of the
     * height, or else the highest one it is above) and the rule-of-thumb cooling.
     */
    fun note(meters: Int, regionId: String?): String {
        val ft = feet(meters)
        val landmarks = LANDMARKS[regionId].orEmpty()
        val near = landmarks.minByOrNull { kotlin.math.abs(it.feet - ft) }
            ?.takeIf { ft > 0 && kotlin.math.abs(it.feet - ft) <= 0.2 * it.feet }
        val landmarkPart = when {
            near != null -> "About as high as ${near.name} (${"%,d".format(near.feet)} ft)."
            else -> landmarks.filter { it.feet < ft }.maxByOrNull { it.feet }
                ?.let { "Higher than ${it.name} (${"%,d".format(it.feet)} ft)." }
        }
        val cooler = coolerFahrenheit(meters)
        val coolerPart = if (cooler >= 1) "Typically about $cooler\u00B0F cooler than at sea level." else "About sea level, so no cooling from height."
        return listOfNotNull(landmarkPart, coolerPart).joinToString(" ")
    }
}
